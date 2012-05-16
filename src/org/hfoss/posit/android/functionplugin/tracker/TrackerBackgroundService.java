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
package org.hfoss.posit.android.functionplugin.tracker;

import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.R;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;

import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.j256.ormlite.android.apptools.OrmLiteBaseService;


/**
 * This service manages the tracking of the device in the background.  It uses a
 * Async Tasks to manage communication with the POSIT server.  It listens for 
 * Location updates and uses a call back method to update the View in TrackerActivity.
 * 
 * For a nice example of how to set up a Service that uses an Async thread, see the src in
 * @ http://github.com/commonsguy/cw-android/tree/master/Service/WeatherPlus/
 * @author rmorelli
 *
 */ 
public class TrackerBackgroundService extends OrmLiteBaseService<DbManager> implements LocationListener {

	private static final String TAG = "PositTracker";
	private static final int START_STICKY = 1;

	// Provides a reference to this service.
	private static TrackerBackgroundService serviceInstance; 

	private static TrackerActivity TRACKER_ACTIVITY;  // The UI
	public static ServiceUpdateUIListener UI_UPDATE_LISTENER; // The Listener for the UI

	private String mProvider;
	private ConnectivityManager mConnectivityMgr;

	private LocationManager mLocationManager;
	private Location mLocation  = null;
	
	/**
	 * Maintains the state of the tracker.
	 */
	private TrackerState mTrackerState;

	private int mRowId;

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
		return mTrackerState;
	}

	public void stopListening() {
		// Shouldn't be null unless Tracker Service fails to start
		if (mTrackerState != null)
			mTrackerState.isRunning = false;
	}


	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "TrackerService, onCreate()");
		
		// Set up the Service so it can communicate with the Activity (UI)
		serviceInstance = this;      // Reference to this Service object used by the UI

		// Create a network manager
		mConnectivityMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

		// Let the UI know about this Tracker service.
		if (TRACKER_ACTIVITY != null)
			TrackerActivity.setTrackerService(this);
	}

	/**
	 * Used only if the Service does inter process calls (IPCs), which ours does not.
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
 
	/**
	 * This is the old onStart method that will be called on the pre-2.0
	 * platform.  On 2.0 or later we override onStartCommand() so this
	 * method will not be called.
	 * */
	@Override
	public void onStart(Intent intent, int startId) {
		handleStartCommand(intent);
		Log.i(TAG, "TrackerService,  Started, id " + startId + " minDistance: " + mTrackerState.mMinDistance);
	}

	/**
	 * Called when the service is started in TrackerActivity. Note that this method 
	 * sometimes executes  BEFORE onCreate(). This is important for the use of objects
	 * such as the TrackerState.
	 * 
	 * The Service is passed TrackerState object from the Activity and this object
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
		handleStartCommand(intent);
		Log.i(TAG, "TrackerService,  Started, id " + startId + " minDistance: " + mTrackerState.mMinDistance);

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	/**
	 * Handles the startup of the service.
	 * @param intent, used to pass TrackerState data
	 */
	private void handleStartCommand(Intent intent) {
		// Get the TrackerState object or create a new one from scratch
		if (intent != null) {
			Bundle b = intent.getBundleExtra(TrackerState.BUNDLE_NAME);
			mTrackerState = new TrackerState(b);
			mTrackerState.setSaved(false);
		} else  {
			mTrackerState = new TrackerState(this);
			Log.e(TrackerActivity.TAG, "TrackerBackgroundService null intent error"); // Why error?
		}

		// Request location updates through GPS provider
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);  		
		mProvider = LocationManager.GPS_PROVIDER; 
		this.mTrackerState.mProvider = mProvider;
		if (mLocationManager != null) {
			Log.i(TrackerActivity.TAG, "TrackerBackgroundService Requesting updates. Best provider = " + mProvider);

			mLocationManager.requestLocationUpdates(
					mProvider, 
					TrackerSettings.DEFAULT_MIN_RECORDING_INTERVAL, 
					mTrackerState.mMinDistance, 
					this);
		}

		// Register a new expedition and update the UI
		mTrackerState.mExpeditionNumber = TRACKER_ACTIVITY.registerExpedition(mTrackerState); // true = new registration

		// Update the state
		if (UI_UPDATE_LISTENER != null) {
			UI_UPDATE_LISTENER.updateUI(mTrackerState);
		}	
	}

	/**
	 *   Reports to the user and updates the Expedition record in the Db.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mLocationManager != null) {
			mLocationManager.removeUpdates(this);

			Toast.makeText(this, "Tracker stopped. " +
					" #pts = " + mTrackerState.mUpdates + 
					" #sent = " + mTrackerState.mSent +
					" #synced = " + mTrackerState.mSynced,  Toast.LENGTH_SHORT).show();

			Log.i(TAG,"TrackerService, Tracker destroyed, " + 
					" #pts = " + mTrackerState.mUpdates + 
					" #sent = " + mTrackerState.mSent +
					" #synced = " + mTrackerState.mSynced);
		}
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
				mTrackerState.mSwath = Integer.parseInt(
						sp.getString(key, ""+TrackerSettings.DEFAULT_SWATH_WIDTH));
				if (mTrackerState.mSwath <= 0) 
					mTrackerState.mSwath = TrackerSettings.DEFAULT_SWATH_WIDTH;
			} else if (key.equals(getString(R.string.min_recording_distance))) {
				mTrackerState.mMinDistance = Integer.parseInt(
						sp.getString(key, ""+TrackerSettings.DEFAULT_MIN_RECORDING_DISTANCE));
				if (mTrackerState.mMinDistance < 0) 
					mTrackerState.mMinDistance = 0;
			}	   
		}
	}

	/**
	 * Saves a point to the phone's Db.
	 * @param ptAsCv, the point represented as a set of ContentValues
	 */
	private synchronized void insertPtInDb(ContentValues ptAsCv) {
		try {
			// Insert the point to the phone's database and update the expedition record

			mRowId = getHelper().addNewGPSPoint(ptAsCv);
			Log.i(TAG, "New point " + ptAsCv);
			
			ptAsCv.put(Points.EXPEDITION_GPS_POINT_ROW_ID, mRowId);		

			// Update the expedition record

			ContentValues expVals = new ContentValues();
			expVals.put(Expedition.EXPEDITION_POINTS, mTrackerState.mPoints);

			int rows = getHelper().updateExpedition(mTrackerState.mExpeditionNumber, expVals);
			if (rows != 0) {
				Log.i(TrackerActivity.TAG, "Updated Expedition " + mTrackerState.mExpeditionNumber);
			} else {
				Log.i(TrackerActivity.TAG, "Failed to update Expedition " + mTrackerState.mExpeditionNumber);
			}			
		} catch (Exception e) {
			Log.e(TrackerActivity.TAG, "TrackerService, handleNewLocation " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Called whenever a new location is received.
	 * @param loc  The new location
	 */
	private synchronized void handleNewLocation(Location loc) {
		// Update the TrackerState object, used to keep track of things and send data to UI
		// Each new point is recorded in the TrackerState object

		mTrackerState.mLocation = loc;
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
		mTrackerState.mPoints++;
		mTrackerState.addGeoPointAndTime(new GeoPoint(
				(int)(latitude*1E6), 
				(int)(longitude*1E6)),
				time);

		// Create a ContentValues for the Point and save to the Db
		ContentValues resultGPSPoint = new ContentValues();
		resultGPSPoint.put(Points.EXPEDITION, mTrackerState.mExpeditionNumber); // Will be -1 if no network
		resultGPSPoint.put(Points.GPS_POINT_LATITUDE, latStr);
		resultGPSPoint.put(Points.GPS_POINT_LONGITUDE, longStr);
		resultGPSPoint.put(Points.GPS_POINT_ALTITUDE, loc.getAltitude());
		resultGPSPoint.put(Points.GPS_POINT_SWATH, mTrackerState.mSwath);
		resultGPSPoint.put(Points.GPS_TIME, time);
		
		insertPtInDb(resultGPSPoint);

		// Call the UI's Listener. This will update the View if it is visible
		if (UI_UPDATE_LISTENER != null && loc != null) {
			UI_UPDATE_LISTENER.updateUI(mTrackerState);
		}	

		// Don't start a thread to send the point unless there is network
		if (mConnectivityMgr.getActiveNetworkInfo() != null) {

			// Pass the row id to the Async thread
			resultGPSPoint.put(Points.EXPEDITION_GPS_POINT_ROW_ID, mRowId);

			ContentValues[] valuesArray = new ContentValues[1];
			valuesArray[0] = resultGPSPoint;

			// Send the point to the POSIT server using a background Async Thread
			AsyncSendPointTask asyncTask = new AsyncSendPointTask(TrackerBackgroundService.TRACKER_ACTIVITY, mTrackerState);
			asyncTask.execute(valuesArray);
		} else {
			Log.i(TrackerActivity.TAG, "Caching: no network. Not sending point to server.");
		}
	}

	// These are the location listener methods. 

	/**
	 * Called whenever a new location update is received from the GPS service. It
	 * updates the TrackerState object (mState) and sends the GeoPoint to the POSIT Server.
	 * @param location, the updated lcoation
	 */
	public void onLocationChanged(Location location) {
		if (mLocation == null || mLocation.distanceTo(location) >= mTrackerState.mMinDistance) {
			// Remember the new location and handle the change
			mLocation = location;
			++mTrackerState.mUpdates;			
			handleNewLocation(location);
		}

		Log.d(TAG, "TrackerService, point found");			
	}

	public void onProviderDisabled(String provider) {
		// Required for location listener interface. Not used
	}

	public void onProviderEnabled(String provider) {
		// Required for location listener interface. Not used
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.i(TAG, "onStatusChanged " + provider + " " + status);
		// Required for location listener interface. Not used
	}
}
