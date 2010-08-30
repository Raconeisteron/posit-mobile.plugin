/*
 * File: TrackerBackgroundService.java
 * 
 * Copyright (C) 2010 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Search and Identification Tool.
 *
 * POSIT is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (LGPL) as published 
 * by the Free Software Foundation; either version 3.0 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU LGPL along with this program; 
 * if not visit http://www.gnu.org/licenses/lgpl.html.
 * 
 */
package org.hfoss.posit.android;

  
import org.hfoss.posit.android.provider.PositDbHelper;
import org.hfoss.posit.android.utilities.Utils;
import org.hfoss.posit.android.web.Communicator;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.maps.GeoPoint;


/**
 * This service manages the tracking of the device in the background.  It uses a
 * Async Tasks to manage communication with the POSIT server.  It listens for 
 * Location updates and uses a call back method to update the View in TrackerActivity.
 * 
 * @author rmorelli
 *
 */
public class TrackerBackgroundService extends Service /*implements LocationListener*/ {

	private static final String TAG = "PositTracker";
	private static final int START_STICKY = 1;
	
	// The static variable allows us to send the Activity a reference to the service through the
	// 
	// See http://stackoverflow.com/questions/2463175/how-to-have-android-service-communicate-with-activity
	private static TrackerBackgroundService serviceInstance; 
	
	private static TrackerActivity TRACKER_ACTIVITY;  // The UI
	public static ServiceUpdateUIListener UI_UPDATE_LISTENER; // The Listener for the UI
	
	private Communicator mCommunicator;
	private ConnectivityManager mConnectivityMgr;
	private PositDbHelper mDbHelper;

//	private LocationManager mLocationManager;
	private Location mLocation  = null;
	private TrackerState mState;
	
	private long mRowId;
	
	/**
	 * These next two STATIC methods allow data to pass between Service and Activity (UI)
	 * @param activity
	 */
	public static void setMainActivity(TrackerActivity activity) {
	  TRACKER_ACTIVITY = activity;
	}

	public static void setUpdateListener(ServiceUpdateUIListener l) {
	  UI_UPDATE_LISTENER = l;
	}
	
	/**
	 * This method is called by the TrackerActivity to get a reference to the Service.
	 * The value of serviceInstance is set in onCreate().
	 * 
	 * See http://stackoverflow.com/questions/2463175/how-to-have-android-service-communicate-with-activity
	 * @return
	 */
	public static TrackerBackgroundService getInstance() {
		return serviceInstance;
	}
	
	/**
	 * This method is called repeatedly by the Activity to get
	 * data about the current Track (expedition) that is then
	 * displayed in the UI.
	 * @return
	 */
	public TrackerState getTrackerState() {
		return mState;
	}
	
	public void stopListening() {
		mState.isRunning = false;
	}
	
	  
	public void onCreate() {
		Log.d(TAG, "TrackerService, onCreate()");
		// Set up the Service so it can communicate with the Activity (UI)
		serviceInstance = this;      // Reference to this Service object used by the UI
		
		// Create a network manager
		mConnectivityMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		// Create a database helper
		mDbHelper = new PositDbHelper(this);
		
		// Let the UI know about this Tracker service.
		if (TRACKER_ACTIVITY != null)
			TrackerActivity.setTrackerService(this);
	}

	/**
	 * This method is used only if the Service does inter process calls (IPCs), 
	 * which ours does not.
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	/**
	 * Called when the service is started in TrackerActivity. Note that this method 
	 * sometimes executes  BEFORE onCreate(). This is important for the use of objects
	 * such as the TrackerState.
	 * 
	 * The Service is passed a TrackerState object from the Activity and this object
	 * is used during tracking to communicate data back and forth. It is used by the
	 * Activity to display data in the View. It is used by the Service to store the
	 * points that are gathered. 
	 * 
	 * @param intent Used to pass a TrackerState object to the service
	 * @param flags Unused
	 * @param startId Unused
	 * @return START_STICKY so the service persists
	 */
	public int onStartCommand(Intent intent, int flags, int startId) {
  		
		// Get the TrackerState object
		if (intent != null) {
			Bundle b = intent.getBundleExtra(TrackerState.BUNDLE_NAME);
			mState = new TrackerState(b);
			mState.setSaved(false);
		} else  {
			mState = new TrackerState(this);
			Log.e(TrackerActivity.TAG, "TrackerBackgroundService null intent error");
		}
		 			
		Log.i(TAG, "TrackerService,  Started, id " + startId + " minDistance: " + mState.mMinDistance);

		// Register a new expedition in an asynchronous thread
		mCommunicator = new Communicator(this);
		new RegisterExpeditionTask().execute();
		
		// Wait until we have a valid expedition number
		// MAX wait around 10 seconds
		while (mState.mExpeditionNumber == -1) {
			try {
				Thread.sleep(2000);  // Wait for 2 seconds
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	
		// Run the location listener in an asynchronous thread
		new GetGPSUpdatesTask().execute(); 

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}
	 
	/**
	 * Helper method to insert the expedition into the db.
	 * @param expId The expedition's id.
	 */
	private synchronized void insertExpeditionToDb (int expId) {
        ContentValues values = new ContentValues();
		values.put(PositDbHelper.EXPEDITION_NUM, expId);
		values.put(PositDbHelper.EXPEDITION_PROJECT_ID, mState.mProjId);
		if (mState.isRegistered)
			values.put(PositDbHelper.EXPEDITION_REGISTERED, PositDbHelper.EXPEDITION_IS_REGISTERED);
		else 
			values.put(PositDbHelper.EXPEDITION_REGISTERED, PositDbHelper.EXPEDITION_NOT_REGISTERED);
		
		try {
			long id = mDbHelper.addNewExpedition(values);

			if (id != -1) {
				Log.i(TAG, "TrackerService, saved expedition " 
						+ expId + " projid= " + mState.mProjId + " in row " + id);
				//			Utils.showToast(this, "Saved expedition " + mState.mExpeditionNumber);
				if (mState != null)
					mState.setSaved(true);
			} else {
				Log.i(TAG, "TrackerService, Db Error: exped=" + expId + " proj=" 
						+ mState.mProjId);
				//			Utils.showToast(this, "Oops, something went wrong when saving " + mState.mExpeditionNumber);
			}
		} catch (Exception e) {
			Log.e(TrackerActivity.TAG, "TrackerService.Async, insertExpeditionToDb " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Helper method to update the Db when a point is synced with server.  Must be syncrhonized
	 * to allow multiple threads to access Db.
	 * @param rowId  The point's rowid
	 * @param vals  Columns to update for the point
	 * @param isSynced  Synced or not
	 * @param expId  The expedition's id
	 */
	private synchronized void updatePointAndExpedition(long rowId, ContentValues vals, int isSynced, int expId) {
		// Update the point in the database
		try {
			boolean success = mDbHelper.updateGPSPoint(rowId, vals);
			if (success) {
				Log.i(TAG, "TrackerService.Async, Updated point# " + rowId + " synced = " + isSynced);

				// Update the expedition record
				ContentValues expVals = new ContentValues();
				expVals.put(PositDbHelper.EXPEDITION_SYNCED, mState.mSynced);
				mDbHelper.updateExpedition(mState.mExpeditionNumber, expVals);
			} else
				Log.i(TAG, "TrackerService.Async, Oops. Failed to update point# " + rowId + " synced = " + isSynced);
		} catch (Exception e) {
			Log.e(TrackerActivity.TAG, "TrackerService.Async, updatePointAndExpedition " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 *   Reports to the user and updates the Expedition record in the Db.
	 *   TODO:  Check if the database update is still needed. Do we now update
	 *   on every point b/c the Tracker may stop before the Async threads finish.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		Utils.showToast(this, "Tracker stopped ---- " +
				"\n#updates = " + mState.mUpdates + 
				" #sent = " + mState.mSent +
				" #synced = " + mState.mSynced);
		
	       ContentValues values = new ContentValues();
	        values.put(PositDbHelper.EXPEDITION_POINTS, mState.mPoints); 
	        values.put(PositDbHelper.EXPEDITION_SYNCED, mState.mSynced);

		boolean success = mDbHelper.updateExpedition(mState.mExpeditionNumber, values);
		if (success)
			Log.i(TAG, "TrackerService, Updated expedition " + mState.mExpeditionNumber);
		else
			Log.i(TAG, "TrackerService, Oops. Failed to update expedition# " + mState.mExpeditionNumber);
		
		Log.i(TAG,"TrackerService, Tracker destroyed, " + //updatedDb= " + success + 
				" #updates = " + mState.mUpdates + 
				" #sent = " + mState.mSent +
				" #synced = " + mState.mSynced);
	}
	
	
	/**
	 * Called from TrackerActivity when the user changes preferences. 
	 * @param sp
	 * @param key
	 */
	public void changePreference(SharedPreferences sp, String key) {
		Log.d(TAG, "TrackerService, Shared Preference Changed key = " + key);
		if (key != null) {
			if (key.equals(getString(R.string.swath_width))) {
				mState.mSwath = Integer.parseInt(
						sp.getString(key, ""+TrackerSettings.DEFAULT_SWATH_WIDTH));
				if (mState.mSwath <= 0) 
					mState.mSwath = TrackerSettings.DEFAULT_SWATH_WIDTH;
			} else if (key.equals(getString(R.string.min_recording_distance))) {
				mState.mMinDistance = Integer.parseInt(
						sp.getString(key, ""+TrackerSettings.DEFAULT_MIN_RECORDING_DISTANCE));
				if (mState.mMinDistance < 0) 
					mState.mMinDistance = 0;
			}	   
		}
	}
	
	/**
	 * Registers the expedition with the POSIT server, which returns the expedition's id.  
	 * 
	 * The thread will repeatedly wait until network connectivity is obtained.
	 * @author rmorelli
	 */
	private class RegisterExpeditionTask extends AsyncTask<Void, Void, Void> {
 
		@Override
		protected Void doInBackground(Void... params) {
			
			// Timed-wait until we have WIFI or MOBILE (MOBILE works best of course)	
			NetworkInfo info = mConnectivityMgr.getActiveNetworkInfo();
			long startTime = System.currentTimeMillis();
			long elapsedTime = 0;
			int expId = 0;

			// If no network, wait here for up to 5 seconds
			while (info == null && elapsedTime < 5000 ) {  
				try {
					Thread.sleep(500);  // Half a second
					info = mConnectivityMgr.getActiveNetworkInfo();
					elapsedTime = System.currentTimeMillis() - startTime;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if (info != null) {
				// Register a new expedition
				// TODO: Embed this in an error check or try/catch block
				expId = mCommunicator.registerExpeditionId(mState.mProjId);
				mState.isRegistered = true;
				mState.isInLocalMode = false;
				Log.i(TAG, "TrackerService.Async, Registered expedition id = " + expId);
			} else {
				expId = TrackerSettings.MIN_LOCAL_EXP_ID 
					+ (int)(Math.random() * TrackerSettings.LOCAL_EXP_ID_RANGE);  // Create a random expId
				mState.isInLocalMode = true;
			}

			try {
				// Create a record in Db for this expedition
				insertExpeditionToDb(expId);
			} catch (Exception e) {
				Log.e(TrackerActivity.TAG, "TrackerService.Async, registeringExpedition " + e.getMessage());
				e.printStackTrace();
			}

			mState.mExpeditionNumber =  expId;  // Async point threads may be waiting on this.
			
			// Call the UI's Listener. This will update the View if it is visible
			if (UI_UPDATE_LISTENER != null && mLocation != null) {
				UI_UPDATE_LISTENER.updateUI(mState);
			}
			return null;
		}
	}
	
	
	/**
	 * This class creates a new Thread to handle sending GPS points to the POSIT server.
	 * Note that the thread will wait until there is network connectivity and until
	 * the Expedition has been duly registered on the server--i.e., has an Expedition id != -1.
	 *  
	 * @author rmorelli
	 */
	public class SendExpeditionPointTask extends AsyncTask<ContentValues, Void, Void> {


	@Override
	protected Void doInBackground(ContentValues... values) {
		
		String result;
		for (ContentValues vals : values) {

			try {			
				NetworkInfo info = mConnectivityMgr.getActiveNetworkInfo();

				// Wait until we have WIFI or MOBILE (MOBILE works best of course)	
				// or it's decided that we'll operate in local mode

				while (info == null || mState.mExpeditionNumber == -1) {
					try {
						Thread.sleep(500);  // Wait for 1/2 seconds
						info = mConnectivityMgr.getActiveNetworkInfo();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				// If we're running in local mode (= no server) exit without
				//  trying to sync the point. The point is marked NOT synced by default.
				if (mState.isInLocalMode) {
					Log.i(TAG, "TrackerService.Async, In local mode, NOT sending point");
					return null;
				}
				
				// Send the point to the Server
				result = mCommunicator.registerExpeditionPoint(
						vals.getAsDouble(PositDbHelper.GPS_POINT_LATITUDE),
						vals.getAsDouble(PositDbHelper.GPS_POINT_LONGITUDE), 
						vals.getAsDouble(PositDbHelper.GPS_POINT_ALTITUDE), 
						vals.getAsInteger(PositDbHelper.GPS_POINT_SWATH), 
						mState.mExpeditionNumber,  //  We need to use the newly made expedition number
						vals.getAsLong(PositDbHelper.GPS_TIME));
				
				// Successful result has the form mmm,nnnn where mmm = expediton_id
				String s = result.substring(0, result.indexOf(","));
				 
				// Get this point's row id
				long rowId = vals.getAsLong(PositDbHelper.EXPEDITION_GPS_POINT_ROW_ID);
				
				++mState.mSent;	
				Log.i(TAG, "TrackerService.Async, Sent  point " + mState.mSent + " rowId=" + rowId + " to server, result = |" + result + "|");

				vals = new ContentValues();
				int isSynced = 0;
				if (s.equals("" + mState.mExpeditionNumber)) {
					++mState.mSynced;
					isSynced = PositDbHelper.FIND_IS_SYNCED;
					vals.put(PositDbHelper.GPS_SYNCED, isSynced);
				} else {
					isSynced = PositDbHelper.FIND_NOT_SYNCED;
					vals.put(PositDbHelper.GPS_SYNCED, isSynced);
				}
				
				// Mark the point as synced or not -- probably not necessary in the NOT case
				// because the default value for a point is NOT synced
				updatePointAndExpedition(rowId, vals, isSynced, mState.mExpeditionNumber);


			} catch (Exception e) {
				Log.i(TAG, "TrackerService.Async, Error handleMessage " + e.getMessage());
				e.printStackTrace();
				// finish();
			}
		}
		return null;
	}

}
	
	/**
	 * Called from the Async location listener whenever a new Location is received. 
	 * @param loc  The new location
	 */
    private synchronized void handleNewLocation(Location loc) {
		// Update the TrackerState object, used to keep track of things and send data to UI
		// Each new point is recorded in the TrackerState object
    	
		mState.mLocation = loc;
		if (loc == null) {
			return;
		}
		double latitude = loc.getLatitude();
		double longitude = loc.getLongitude();
		long time = loc.getTime();
		
		// The SQLite Db is happier if we store these as strings instead of doubles.
		String latStr = String.valueOf(latitude);
		String longStr = String.valueOf(longitude);
		//Log.i(TAG, "TrackerService, Lat,long as strings " + latStr + "," + longStr);
		
		// Add the point to the ArrayList (used for mapping the points)
		mState.mPoints++;
		mState.addGeoPointAndTime(new GeoPoint(
				(int)(latitude*1E6), 
				(int)(longitude*1E6)),
				time);
		
		// Create a ContentValues for the Point
        ContentValues resultGPSPoint = new ContentValues();
        resultGPSPoint.put(PositDbHelper.EXPEDITION, mState.mExpeditionNumber); // Will be -1 if no network
        resultGPSPoint.put(PositDbHelper.GPS_POINT_LATITUDE, latStr);
        resultGPSPoint.put(PositDbHelper.GPS_POINT_LONGITUDE, longStr);
        resultGPSPoint.put(PositDbHelper.GPS_POINT_ALTITUDE, loc.getAltitude());
        resultGPSPoint.put(PositDbHelper.GPS_POINT_SWATH, mState.mSwath);
        resultGPSPoint.put(PositDbHelper.GPS_TIME, time);
        
        try {
        	// Insert the point to the phone's database and update the expedition record
        	mRowId = mDbHelper.addNewGPSPoint(resultGPSPoint);
        	
        	// Update the expedition record
			ContentValues expVals = new ContentValues();
			expVals.put(PositDbHelper.EXPEDITION_POINTS, mState.mPoints);
			mDbHelper.updateExpedition(mState.mExpeditionNumber, expVals);
			
        } catch (Exception e) {
        	Log.e(TrackerActivity.TAG, "TrackerService, handleNewLocation " + e.getMessage());
        	e.printStackTrace();
        }

		// Call the UI's Listener. This will update the View if it is visible
        // Update here rather than in the Async thread so the points are displayed 
        // even when there is no network.
		if (UI_UPDATE_LISTENER != null && loc != null) {
			UI_UPDATE_LISTENER.updateUI(mState);
		}	
 
		// Don't start a thread to send the point unless there is network
		if (mConnectivityMgr.getActiveNetworkInfo() != null) {
			
			// Pass the row id to the Async thread
			resultGPSPoint.put(PositDbHelper.EXPEDITION_GPS_POINT_ROW_ID, mRowId);

			// Send the point to the POSIT server using a background Async Thread
			new SendExpeditionPointTask().execute(resultGPSPoint);
		} else {
			Log.i(TrackerActivity.TAG, "Caching: no network. Not sending point to server.");
		}
	}
	
    /**
     * This Async thread listens for location updates. 
     * @author rmorelli
     *
     */
	 private class GetGPSUpdatesTask extends AsyncTask<Void, Location, Long> implements LocationListener{
		 public static final int UPDATE_LOCATION = 1;
         long totalUpdates = 0;
         LocationManager mLocationManager;
         private Location mLocation;
         private Handler updateHandler;
         
     	/**
     	 * Handles GPS updates.  
     	 * Source: Android tutorials
     	 * @see http://www.androidph.com/2009/02/app-10-beer-radar.html
     	 * @see also: http://2009.hfoss.org/Tutorial:Hello_Mapview%2C_with_GPS
     	 */
              	
         protected Long doInBackground(Void...arg0) {
        	 
        	 // Create a handler to receive messages from the location update service.
        	 // This will loop until the Async task is stopped.
        	 Looper.prepare();
        	 updateHandler = new Handler() {

        		 /** Gets called on every message that is received */
        		 public void handleMessage(Message msg) {
        			 
        			 switch (msg.what) {
        			 case UPDATE_LOCATION: {
        				 // Causes onProgressUpdate() to be invoked and sent the new location
        				 // which is already stored in the instance variable, mLocation
        				 publishProgress(mLocation);
            			 if (!mState.isRunning)
            				 Looper.myLooper().quit();
        				 break;
        			 }
        			 }
        			 super.handleMessage(msg);	
        		 }
        	 };
         	  
	 		// Start location update service
	 		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE); 
	 		if (mLocationManager != null) {
		 		Log.i(TrackerActivity.TAG, "TrackerBackgroudnService Requesting updates");
	 			mLocationManager.requestLocationUpdates(
	 					LocationManager.GPS_PROVIDER, 
	 					TrackerSettings.DEFAULT_MIN_RECORDING_INTERVAL, 
	 					mState.mMinDistance, 
	 					this);
	 		}
	 		
	 		// Start looping -- i.e., listening for messages from the location service
	 		Looper.loop();
	 		Log.i(TrackerActivity.TAG, "TrackerBackgroudnService Starting loooper");

	 		// Unregister the listeners before stopping this thread
	 		mLocationManager.removeUpdates(this);
	 		return totalUpdates;
	     }
         
         /**
          * Invoked by publish progress.  It handles the new locaiton
          */
	     protected void onProgressUpdate(Location... loc) {
	         handleNewLocation(loc[0]);
	         ++totalUpdates;
	     }

	     /**
	      * Invoked with totalUpdates when the Async task is finished.  This would be the place
	      * to do any summary reports. 
	      */
		protected void onPostExecute(Long result) {
			mLocationManager.removeUpdates(this); 
			Log.d(TrackerActivity.TAG, "TrackerService.GetGPSUpdatesTask unregistered location listener");
			Log.d(TrackerActivity.TAG, "TrackerService.GetGPSUpdatesTask finished. Updates received = " + totalUpdates);

	     }
		 
		
		/**
		 * This method is called whenever a new location update is received from the GPS. It
		 * updates the TrackerState object (mState) and sends the GeoPoint to the POSIT Server.
		 * @param newLocation
		 */
		private void setCurrentGpsLocation(Location newLocation) {
			if (mLocation == null || mLocation.distanceTo(newLocation) >= mState.mMinDistance) {
				// Remember the new location
				mLocation = newLocation;
				
				// Send a message to the handler
				try {
					Message msg = Message.obtain();
					msg.what = UPDATE_LOCATION;
					this.updateHandler.sendMessage(msg);
				} catch (Exception e) {
					Log.i(TrackerActivity.TAG, "Location listener thread exception " + e.getMessage());
				}	
			}
		}

		// These are the location listener methods.
		public void onLocationChanged(Location location) {
			setCurrentGpsLocation(location);
			++mState.mUpdates;
//			Log.d(TAG, "TrackerService, point found");			
		}

		public void onProviderDisabled(String provider) {
			setCurrentGpsLocation(null);
		}

		public void onProviderEnabled(String provider) {
			setCurrentGpsLocation(null);
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// Do nothing
		}

	 }

	
	
}
