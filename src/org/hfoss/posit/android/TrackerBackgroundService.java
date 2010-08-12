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
import org.hfoss.posit.android.utilities.Utils;
import org.hfoss.posit.android.web.Communicator;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.maps.GeoPoint;


public class TrackerBackgroundService extends Service implements LocationListener {

	private static final String TAG = "Tracker Service";
	private static final int START_STICKY = 1;
	
	// The static variable allows us to send the Activity a reference to the service through the
	// 
	// See http://stackoverflow.com/questions/2463175/how-to-have-android-service-communicate-with-activity
	private static TrackerBackgroundService serviceInstance; 
	
	private static TrackerActivity TRACKER_ACTIVITY;  // The UI
	public static ServiceUpdateUIListener UI_UPDATE_LISTENER; // The Listener for the UI

	public static int MINIMUM_INTERVAL = 1000; // millisecs
	public static int MINIMUM_DISTANCE = 5; // Meters  
	
	private Communicator mCommunicator;
	private ConnectivityManager mConnectivityMgr;
	private SharedPreferences mPreferences;

	private LocationManager mLocationManager;
	private Location mLocation  = null;
	private TrackerState mState;
	
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
		// Set up the Service so it can communicate with the Activity (UI)
		serviceInstance = this;      // Reference to this Service object used by the UI
		mState = new TrackerState(); // Create a TrackerState object to keep track of the track.
		
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		// Make sure the phone is registered with a Server and has a project selected
		// Maybe this can be done in the Activity and sent to the Service?
		mState.mProjId = mPreferences.getInt("PROJECT_ID", 0);
		if (mState.mProjId == 0) {
			Utils.showToast(this,"Aborting Tracker:\nDevice must be registered with a project.");
			return;
		}
		Log.i(TAG,"Project id = " + mState.mProjId);
		
		// Register a new expedition
		mCommunicator = new Communicator(this);
		mState.mExpeditionNumber =  mCommunicator.registerExpeditionId(mState.mProjId);
		
		// Start location update service
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE); 
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINIMUM_INTERVAL, MINIMUM_DISTANCE, this);
		
		// Create a network manager
		mConnectivityMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		// Let the UI know about this Tracker service.
		if (TRACKER_ACTIVITY != null)
			TrackerActivity.setTrackerService(this);
	}

	/**
	 * This method is used only if the Service does inter process calls (IPCs), which it doesn't.
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Received start id " + startId + ": " + intent);
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
		Log.i(TAG, "Setting current GPS");
		if (mLocation == null || mLocation.distanceTo(newLocation) >= MINIMUM_DISTANCE) {
			mLocation = newLocation;
			
			// Update the TrackerState object, used to keep track of things and send data to UI
			// Each new point is recorded in the TrackerState object
			mState.mLocation = mLocation;
			mState.mPoints++;
			mState.addGeoPoint(new GeoPoint((int)(mLocation.getLatitude()*1E6), 
					(int)(mLocation.getLongitude()*1E6)));
			
			// Call the UI's Listener. This will update the View if it is visible
			if (UI_UPDATE_LISTENER != null && mLocation != null) {
				UI_UPDATE_LISTENER.updateUI(mLocation);
			}	
			
			// Send the point to the POSIT server in a background Thread
			new SendExpeditionPointTask().execute(mLocation);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mLocationManager.removeUpdates(this); 
		mLocationManager = null;
		Log.i(TAG,"Destroyed");
	}
	
	// ------------------------ LocationListener Methods
	/**
	 * Invoked by the location service when phone's location changes.
	 */
	public void onLocationChanged(Location newLocation) {
		setCurrentGpsLocation(newLocation);
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
		setCurrentGpsLocation(mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
	}
	
	
	/**
	 * This class creates a new Thread to handle network access. 
	 * @author rmorelli
	 *
	 */
	private class SendExpeditionPointTask extends AsyncTask<Location, Void, Void> {

	@Override
	protected Void doInBackground(Location... location) {
		
		for (Location loc : location) {
			double latitude = loc.getLatitude();
			double longitude = loc.getLongitude();
			double altitude = loc.getAltitude();

			// Try to handle a change in network connectivity
			// We may lose a few points, but try not to crash
			try {
				int networkType = mConnectivityMgr.getActiveNetworkInfo().getType();

				// Send the point to the Server
				String result = mCommunicator.registerExpeditionPoint(
									latitude, longitude, altitude, mState.mSwath, mState.mExpeditionNumber);

				Log.i(TAG, result);

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
