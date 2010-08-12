/*
 * File: TrackerActivity.java
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

import java.util.List;

import org.hfoss.posit.android.utilities.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;

/**
 * Code adapted from on online tutorial.
 * @see http://www.calvin.edu/~jpr5/android/tracker.html
 *
 */
public class TrackerActivity extends MapActivity implements ServiceUpdateUIListener { // implements Runnable , LocationListener{

	private static final String TAG = "TrackerActivity";
	public static final String TRACKER_STATE = "TrackerState";
	public static final int DEFAULT_SWATH_WIDTH = 50;  // 50 meters
	private static final boolean ENABLED_ONLY = true;
	private static final String NO_PROVIDER = "No location service";

	public static final int IDLE = 0;
	public static final int RUNNING = 1;
	public static final int PAUSED = 2;  // Currently unused
	
	private int mState = IDLE;
	private int mSwath = DEFAULT_SWATH_WIDTH;
	private TextView mPointsTextView;

    private SharedPreferences mPreferences ;
    private SharedPreferences.Editor spEditor;
    
	private String mProvider = NO_PROVIDER;
	private ConnectivityManager mConnectivityMgr;
	private int mNetworkType;
	private NotificationManager mNotificationMgr;


	private static TrackerBackgroundService mBackgroundService;
	
	private MyLocationOverlay myLocationOverlay;
	private TrackerOverlay mTrackerOverlay;
	private List<Overlay> mOverlays;
	private TextView mLocationTextView;
	private TextView mStatusTextView;
	private TextView mExpeditionTextView;
	private TextView mSwathTextView;
	
	private TrackerState mTrackerState;
	private int mPoints = 0;
	private int mExpeditionNumber;
	
	/** 
	 * Called when the activity is first created. Note that if the "Back" key is used while this
	 *  (or any) activity is running, the Activity will be stopped and destroyed.  So if it is started
	 *  again, onCreate() will be called. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Abort the Tracker if either the network or GPS is unavailable
		if (!hasNecessaryServices())  {
			this.finish();
			return;
		}
		
		// Initialize call back references so that the Tracker Service can pass data to this UI
		TrackerBackgroundService.setUpdateListener(this);   // The Listener for the Service
	    TrackerBackgroundService.setMainActivity(this);
	    	    
	    // Get a notification manager
		mNotificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Set up the UI -- first the text views
		setContentView(R.layout.tracker);
		mPointsTextView = (TextView)findViewById(R.id.trackerPoints);
		mLocationTextView = (TextView)findViewById(R.id.trackerLocation);
		mStatusTextView = (TextView)findViewById(R.id.trackerStatus);
		mExpeditionTextView = (TextView)findViewById(R.id.trackerExpedition);
		mSwathTextView = (TextView)findViewById(R.id.trackerSwath);

		// Set up the UI -- now the map view and its current location overlay. The points overlay is
		//  created in updateView, after the Tracker Service is started. 
		MapView mapView = (MapView) findViewById(R.id.mapView);
		mapView.setBuiltInZoomControls(true);
		mOverlays = mapView.getOverlays();
		myLocationOverlay = new MyLocationOverlay(this, mapView);
		mOverlays.add(myLocationOverlay);
		
		// Get a reference to the shared preferences. The Tracker's state (RUNNING or IDLE) is
		//  saved as a preference.
	    mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	    spEditor = mPreferences.edit();
	    
		// See whether the Tracker service is running and if so restore the state
		mState = mPreferences.getInt(TRACKER_STATE, IDLE);
		if (mState == RUNNING)  {
			Utils.showToast(this, "The Tracker is RUNNING.");
			restoreState();
			if (mTrackerState != null)  
				updateUI(mTrackerState.mLocation);
		} else {
			Utils.showToast(this, "The Tracker is IDLE.");
		}
		Log.i(TAG,"Created with state = " + mState);
	}
	
	/**
	 * Checks for network and GPS service.
	 * 
	 * @return
	 */
	private boolean hasNecessaryServices() {
		Log.i(TAG, "hasNecessaryServices()");
		
		// First check that we have GPS enabled 
		LocationManager locationManager = 
			(LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = locationManager.getProviders(ENABLED_ONLY);
		if (providers.contains(LocationManager.GPS_PROVIDER)) {
			mProvider = LocationManager.GPS_PROVIDER;
		} else {
			Utils.showToast(this, "Aborting Tracker: " + NO_PROVIDER
					+ "\nYou must have GPS enabled. ");
			return false;
		}

		// Check that we have WIFI or MOBILE (MOBILE works best of course)	
		mConnectivityMgr = 
			(ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = mConnectivityMgr.getActiveNetworkInfo();
		if (info == null) {
			Log.e(TAG,"setNetworkType() unable to acquire CONNECTIVITY_SERVICE");
			Utils.showToast(this,
					"Aborting Tracker: No Active Network.\nYou must have WIFI or MOBILE enabled.");
			return false;
		} else {
			mNetworkType = info.getType();
		}
		return true;
	}
	
	/**
	 * This method is called by the Tracker Service.  It must run in the UI thread. 
	 */
	public void updateUI(final Location location) {
		// make sure this runs in the UI thread... since it's messing with views...
		this.runOnUiThread(new Runnable() {
            public void run() {
        		restoreState(); 
        		updateView(location);
        		myLocationOverlay.onLocationChanged(location);
        		Log.i(TAG,"updatingUI  <" + location.getLatitude() + "," + location.getLongitude() + ">");
              }
            });
//		restoreState(); 
//		updateView(location);
//		myLocationOverlay.onLocationChanged(location);
//		Log.i(TAG,"updatingUI  <" + location.getLatitude() + "," + location.getLongitude() + ">");
	}
	
	/**
	 * A helper method to restore the Activity's state and its UI when it is restarted.  The
	 * Service runs in the background independently of the foreground Activity (the UI).  We
	 * get some of the data from the Service -- namely the TrackerState object, which contains
	 * an array of all the points collected and the current lat,long,alt. 
	 */
	private void restoreState() {
		mBackgroundService = TrackerBackgroundService.getInstance();
		if (mBackgroundService != null) {
			Log.i(TAG,"Restoring state");
			mTrackerState = mBackgroundService.getTrackerState();
			mPoints =  mTrackerState.mPoints;
			mExpeditionNumber = mTrackerState.mExpeditionNumber;

			mTrackerOverlay = new TrackerOverlay(mTrackerState);
			mOverlays.add(mTrackerOverlay);
		}
	}

	/**
	 * This static method lets the Activity acquire a reference to the Service.  The
	 * reference is used to get the Tracker's state.
	 * @param service
	 */
	public static void setTrackerService(TrackerBackgroundService service) {
		mBackgroundService = service;
	}
	
	/**
	 * A helper method to update the UI. 
	 * @param location
	 */
	private void updateView(Location location) {
//		++mPoints;
		mPointsTextView.setText(" " + mPoints);
		mExpeditionTextView.setText(" "+mExpeditionNumber);
//		Log.i(TAG,"updateView(), mPoints = " + mPoints +"  " + mExpeditionNumber);
		
		String s = " Idle ";
		if (mState == RUNNING) 
			s = " Running ";
		
		String netStr = (mNetworkType == ConnectivityManager.TYPE_WIFI) ? " WIFI" : " MOBILE";
 		mStatusTextView.setText(s + " (GPS = " + LocationManager.GPS_PROVIDER 
				+ ", Ntwk = " + netStr + ")");

		mSwathTextView.setText(" " + mSwath);
		mLocationTextView.setText(location.getLatitude() + "," + location.getLongitude() 
				+ "," + location.getAltitude());
	}


	/**
	 * 
	 */
	@Override
	public void onResume() {
		super.onResume();
		myLocationOverlay.enableMyLocation();
		int state = mPreferences.getInt(TRACKER_STATE, IDLE);
		Log.i(TAG,"Resumed in state " + state);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG,"Create options menu");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tracker_menu, menu);
		return true;
	}

	/**
	 * Updates the Tracker Start/stop menus based on whether Tracker is running or not.
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.i(TAG,"Prepare options menu");
		if (mState == RUNNING) {
			menu.findItem(R.id.start_tracking_menu_item).setEnabled(false);
			menu.findItem(R.id.stop_tracking_menu_item).setEnabled(true);
		} else {
			menu.findItem(R.id.start_tracking_menu_item).setEnabled(true);
			menu.findItem(R.id.stop_tracking_menu_item).setEnabled(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	/** 
	 * Here is where we start and stop the Tracker Service, plus handle other book keeping 
	 * tasks, such as posting and canceling a Notification, displaying a Toast to the UI, and
	 * saving the changed state in the SharedPreferences. 
	 * 
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
		case R.id.start_tracking_menu_item:
			startService(new Intent(this, TrackerBackgroundService.class));
			mState = RUNNING;
			spEditor.putInt(TRACKER_STATE, RUNNING);
			spEditor.commit();
			Utils.showToast(this, "Tracking the phone's location.");
			postNotification();   
			break;	
		case R.id.stop_tracking_menu_item:
			stopService(new Intent(this, TrackerBackgroundService.class));
			mNotificationMgr.cancel(R.string.local_service_label);  // Cancel the notification
			mState = IDLE;
			spEditor.putInt(TRACKER_STATE, IDLE);
			spEditor.commit();
			if (mTrackerState != null)
				updateView(mTrackerState.mLocation); // Null Pointer if stopped before Service starts?
			Utils.showToast(this, "Tracking is stopped.");
			break;
		}
		return true;
	}
	
	/**
	 * Post a notification in the status bar while this service is running.
	 */
	private void postNotification() {  
		// The text that shows when the notification is posted.
		CharSequence text = getText(R.string.local_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.radar, text,
				System.currentTimeMillis());

		// Launch TrackerActivity when the user clicks on this notification.
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, TrackerActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this,
				getText(R.string.local_service_label), text, contentIntent);

		// Post the notification
		mNotificationMgr.notify(R.string.local_service_label, notification);
	}
	
	
	@Override
	public void onPause() {
		super.onPause();
		myLocationOverlay.disableMyLocation();
		spEditor.putInt(TRACKER_STATE, mState);
		spEditor.commit();
		Log.i(TAG,"Paused in state: " + mState);
	}
	
	// The following methods don't change the default behavior, but they show (in the Log) the
	// life cycle of the Activity.

	@Override protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG,"Destroyed");
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		Log.i(TAG,"Stopped");
	}

	/**
	 * Required for MapActivity
	 * @see com.google.android.maps.MapActivity#isRouteDisplayed()
	 */
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}


}
