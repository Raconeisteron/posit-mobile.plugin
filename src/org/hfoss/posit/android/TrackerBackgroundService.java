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

  
//import org.hfoss.posit.android.TrackerService.SendExpeditionPointTask;
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
import android.os.IBinder;
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
public class TrackerBackgroundService extends Service implements LocationListener {

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

	private LocationManager mLocationManager;
	private Location mLocation  = null;
	private TrackerState mState;
	
	private int mPointsSent = 0;
	private int mUpdates = 0;
	
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
	
	  
	public void onCreate() {
		Log.d(TAG, "Tracker Service Created");
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

	public int onStartCommand(Intent intent, int flags, int startId) {
		
		Bundle b = intent.getBundleExtra(TrackerState.BUNDLE_NAME);
		mState = new TrackerState(b);
		
		Log.i(TAG, "Tracker Service Started, id " + startId + " minDistance: " + mState.mMinDistance);

		// Register a new expedition
		mCommunicator = new Communicator(this);
		new RegisterExpeditionTask().execute();

		// Start location update service
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE); 
		if (mLocationManager != null) {
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 
					TrackerSettings.DEFAULT_MIN_RECORDING_INTERVAL, 
					mState.mMinDistance, 
					this);
		}
		
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}
	

	/**
	 * This method is called whenever a new location update is received from the GPS. It
	 * updates the TrackerState object (mState) and sends the GeoPoint to the POSIT Server.
	 * @param newLocation
	 */
	private void setCurrentGpsLocation(Location newLocation) {
		if (mLocation == null || mLocation.distanceTo(newLocation) >= mState.mMinDistance) {
			mLocation = newLocation;
			
			// Update the TrackerState object, used to keep track of things and send data to UI
			// Each new point is recorded in the TrackerState object
			mState.mLocation = mLocation;
			double latitude = mLocation.getLatitude();
			double longitude = mLocation.getLongitude();
			String latStr = String.valueOf(latitude);
			String longStr = String.valueOf(longitude);
			//Log.i(TAG, "Lat,long as strings " + latStr + "," + longStr);
			
			// Add the point to the ArrayList (used for mapping the points)
			mState.mPoints++;
			mState.addGeoPoint(new GeoPoint((int)(latitude*1E6), 
					(int)(longitude*1E6)));
			
			// Add the point to the database
            ContentValues resultGPSPoint = new ContentValues();
            resultGPSPoint.put(PositDbHelper.EXPEDITION, mState.mExpeditionNumber); // Will be -1 if no network
            resultGPSPoint.put(PositDbHelper.GPS_POINT_LATITUDE, latStr);
            resultGPSPoint.put(PositDbHelper.GPS_POINT_LONGITUDE, longStr);
            resultGPSPoint.put(PositDbHelper.GPS_POINT_ALTITUDE, mLocation.getAltitude());
            resultGPSPoint.put(PositDbHelper.GPS_POINT_SWATH, mState.mSwath);
            resultGPSPoint.put(PositDbHelper.GPS_TIME, newLocation.getTime());
           mDbHelper.addNewGPSPoint(resultGPSPoint);
            
			// Call the UI's Listener. This will update the View if it is visible
            // Update here rather than in the Async thread so the points are displayed 
            // even when there is no network.
			if (UI_UPDATE_LISTENER != null && mLocation != null) {
				UI_UPDATE_LISTENER.updateUI(mState);
			}	
			
			// Send the point to the POSIT server using a background Async Thread
			new SendExpeditionPointTask().execute(resultGPSPoint);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mLocationManager.removeUpdates(this); 
		Log.i(TAG,"Tracker destroyed, updates = " + mUpdates + " points sent = " + mPointsSent);
	}
	
	
	
	/**
	 * Called from TrackerActivity when the user changes preferences. 
	 * @param sp
	 * @param key
	 */
	public void changePreference(SharedPreferences sp, String key) {
		Log.d(TAG, "Shared Preference Changed key = " + key);
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

	
	// ------------------------ LocationListener Methods
	/**
	 * Invoked by the location service when phone's location changes.
	 */
	public void onLocationChanged(Location newLocation) {
		setCurrentGpsLocation(newLocation);
		++mUpdates;
//		Log.i(TAG, "point found");
	}

	/**
	 * Resets the GPS location whenever the provider is enabled.
	 */
	public void onProviderEnabled(String provider) {
		setCurrentGpsLocation(null);
	}

	/**
	 * Resets the GPS location whenever the provider is disabled.
	 */
	public void onProviderDisabled(String provider) {
		// Log.i(TAG, provider + " disabled");
		setCurrentGpsLocation(null);
	}

	/**
	 * Resets the GPS location whenever the provider status changes. We don't
	 * care about the details.
	 */
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (mLocationManager != null)
			setCurrentGpsLocation(mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
	}
	
	
	/**
	 * Registers the expedition with the POSIT server. The server returns the expedition's
	 * id.  If the phone lacks network connectivity, the expeditions id = -1. 
	 * 
	 * The thread will repeatedly wait until network connectivity is obtained.
	 * @author rmorelli
	 */
	private class RegisterExpeditionTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			
			// Timed-wait until we have WIFI or MOBILE (MOBILE works best of course)	
			NetworkInfo info = mConnectivityMgr.getActiveNetworkInfo();
			while (info == null) {
				try {
					Thread.sleep(2000);
					info = mConnectivityMgr.getActiveNetworkInfo();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			// Register a new expedition
			mState.mExpeditionNumber =  mCommunicator.registerExpeditionId(mState.mProjId);
			Log.i(TAG, "Registered expedition id = " + mState.mExpeditionNumber);
			
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
	private class SendExpeditionPointTask extends AsyncTask<ContentValues, Void, Void> {


	@Override
	protected Void doInBackground(ContentValues... values) {
		
		String result;
		for (ContentValues v : values) {

			try {			
				// Wait until we have WIFI or MOBILE (MOBILE works best of course)	
				NetworkInfo info = mConnectivityMgr.getActiveNetworkInfo();
				while (info == null || mState.mExpeditionNumber == -1) {
					try {
						Thread.sleep(2000);  // Wait for 2 seconds
						info = mConnectivityMgr.getActiveNetworkInfo();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				// Send the point to the Server
				result = mCommunicator.registerExpeditionPoint(
						v.getAsDouble(PositDbHelper.GPS_POINT_LATITUDE),
						v.getAsDouble(PositDbHelper.GPS_POINT_LONGITUDE), 
						v.getAsDouble(PositDbHelper.GPS_POINT_ALTITUDE), 
						v.getAsInteger(PositDbHelper.GPS_POINT_SWATH), 
						mState.mExpeditionNumber,  //  We need to use the newly made expedition number
						v.getAsLong(PositDbHelper.GPS_TIME));
				
				++mPointsSent;
				Log.i(TAG, "Sent  point " + mPointsSent + " to server, result = " + result);

			} catch (Exception e) {
				Log.i(TAG, "Error handleMessage " + e.getMessage());
				e.printStackTrace();
				// finish();
			}
		}
		return null;
	}

}
	
	
}
