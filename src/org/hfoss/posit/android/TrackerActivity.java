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
import org.hfoss.posit.android.web.Communicator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;

/**
 * This class tracks and maps the phone's location in real time.  The track is displayed
 * on this activity's View.  Listening for Location updates and sending points to the POSIT
 * server are handled by the TrackerBackgroundService. 
 * 
 * @see http://www.calvin.edu/~jpr5/android/tracker.html
 *
 */
public class TrackerActivity extends MapActivity 
	implements ServiceUpdateUIListener, 
		View.OnClickListener,
		OnSharedPreferenceChangeListener { 

	private static final String TAG = "PositTracker";
	private static final boolean ENABLED_ONLY = true;
	private static final String NO_PROVIDER = "No location service";

	public static final int SET_MINIMUM_DISTANCE = 0;
	
	private int mState = TrackerSettings.IDLE;
	private TextView mPointsTextView;

    private SharedPreferences mPreferences ;
    private SharedPreferences.Editor spEditor;
    
	private String mProvider = NO_PROVIDER;
	private ConnectivityManager mConnectivityMgr;
	private int mNetworkType;
	private NotificationManager mNotificationMgr;

	private static TrackerBackgroundService mBackgroundService; 
	
	// View stuff
	private MyLocationOverlay myLocationOverlay;
	private TrackerOverlay mTrackerOverlay;
	private List<Overlay> mOverlays;
	private TextView mLocationTextView;
	private TextView mStatusTextView;
	private TextView mExpeditionTextView;
	private TextView mSwathTextView;
	private TextView mMinDistTextView;
	private Button mTrackerButton;
	private Button mSettingsButton;
	
	// The current track
	private TrackerState mTrack;
	
	/** 
	 * Called when the activity is first created. Note that if the "Back" key is used while this
	 *  (or any) activity is running, the Activity will be stopped and destroyed.  So if it is started
	 *  again, onCreate() will be called. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Abort the Tracker if GPS is unavailable
		if (!hasNecessaryServices())  {
			this.finish();
			return;
		}
		 
		// Initialize call back references so that the Tracker Service can pass data to this UI
		TrackerBackgroundService.setUpdateListener(this);   // The Listener for the Service
	    TrackerBackgroundService.setMainActivity(this);
	    
	    // Get our preferences and register as a listener for changes to tracker preferences. 
	    // The Tracker's state (RUNNING, IDLE) is saved as a preference
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mPreferences.registerOnSharedPreferenceChangeListener(this);
	    spEditor = mPreferences.edit();
	    
	    // Create a new track
	    mTrack = createTrack();
	    
		// Make sure the phone is registered with a Server and has a project selected
		// Maybe this can be done in the Activity and sent to the Service?
		if (mTrack.mProjId == -1) {
			Utils.showToast(this,"Cannot start Tracker:\nDevice must be registered with a project.");
			Log.e(TAG, "Cannot start Tracker -- device not registered with a project.");
			return;
		}
		
	    // Get a notification manager
		mNotificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Set up the UI -- first the text views
		setContentView(R.layout.tracker);
		mPointsTextView = (TextView)findViewById(R.id.trackerPoints);
		mLocationTextView = (TextView)findViewById(R.id.trackerLocation);
		mStatusTextView = (TextView)findViewById(R.id.trackerStatus);
		mExpeditionTextView = (TextView)findViewById(R.id.trackerExpedition);
		mSwathTextView = (TextView)findViewById(R.id.trackerSwath);
		mMinDistTextView = (TextView)findViewById(R.id.trackerMinDistance);
	    mTrackerButton = (Button)findViewById(R.id.idTrackerButton);
	    mTrackerButton.setOnClickListener(this);
	    mSettingsButton = (Button)findViewById(R.id.idTrackerSettingsButton);
	    mSettingsButton.setOnClickListener(this);

		// Set up the UI -- now the map view and its current location overlay. 
		// The points overlay is created in updateView, after the Tracker Service is started. 
		MapView mapView = (MapView) findViewById(R.id.mapView);
		mapView.setBuiltInZoomControls(true);
		mOverlays = mapView.getOverlays();
		myLocationOverlay = new MyLocationOverlay(this, mapView);
		mOverlays.add(myLocationOverlay);
			    	    
		Log.i(TAG,"Tracker Activity created with state = " + mState);
	}
	
	/**
	 * 
	 */
	@Override
	public void onResume() {
		super.onResume();
		myLocationOverlay.enableMyLocation();
		
		// See whether the Tracker service is running and if so restore the state
		mState = mPreferences.getInt(
				TrackerSettings.TRACKER_STATE_PREFERENCE, 
				TrackerSettings.IDLE);
		if (mState == TrackerSettings.RUNNING)  {
			Utils.showToast(this, "The Tracker is RUNNING.");
			restoreState();
			if (mTrack != null)  
				updateUI(mTrack);
			else
				updateView();
		} else {
			updateView();
			Utils.showToast(this, "The Tracker is IDLE.");
		}

		Log.i(TAG,"Tracker Activity resumed in state " + mState);
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
		return true;
	}
	
	
	/**
	 * Create a current track object to store the track's points and its state.
	 * @return
	 */
	private TrackerState createTrack() {
		TrackerState ts = new TrackerState();
		try {
		ts.mMinDistance = Integer.parseInt(
				mPreferences.getString(
				TrackerSettings.MINIMUM_DISTANCE_PREFERENCE,
				""+TrackerSettings.DEFAULT_MIN_RECORDING_DISTANCE));
		ts.mSwath = Integer.parseInt(
				mPreferences.getString(
				TrackerSettings.SWATH_PREFERENCE, 
				""+TrackerSettings.DEFAULT_SWATH_WIDTH));
		} catch (Exception e) {
			Log.e(TAG, "Oops. something wrong probably Integer parse error " + e);
		}
		
		ts.mProjId = mPreferences.getInt(
				TrackerSettings.POSIT_PROJECT_PREFERENCE, 
				-1);
		return ts;
	}
	
	/**
	 * This method is called by the Tracker Service.  It must run in the UI thread. 
	 */
	public void updateUI(final TrackerState state) {
		mTrack = state;
		// make sure this runs in the UI thread... since it's messing with views...
		this.runOnUiThread(new Runnable() {
            public void run() {
        		restoreState(); 
        		updateView();
        		if (state.mLocation != null) {
        			myLocationOverlay.onLocationChanged(state.mLocation);
        			Log.i(TAG,"updatingUI  <" + state.mLocation.getLatitude() + "," + state.mLocation.getLongitude() + ">");
        		} else 
        			Log.w(TAG,"updatingUI  unable to get location from TrackerState");

              }
            });
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
			mTrack = mBackgroundService.getTrackerState();
//			mPoints =  mTrack.mPoints;
//			mExpeditionNumber = mTrack.mExpeditionNumber;
//			mMinDistance = mTrackerState.mMinDistance;
			Log.i(TAG,"Restoring state, minDistance = " + mTrack.mMinDistance);

			mTrackerOverlay = new TrackerOverlay(mTrack);
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
	private void updateView() {		 
		String s = " Idle ";
		if (mState == TrackerSettings.RUNNING) {
			s = " Running ";
			mTrackerButton.setText("Stop");
			mTrackerButton.setCompoundDrawablesWithIntrinsicBounds(
					getResources().getDrawable(R.drawable.stop_icon),null,null,null); 
		} else {
			mTrackerButton.setText("Start");
			mTrackerButton.setCompoundDrawablesWithIntrinsicBounds(
					getResources().getDrawable(R.drawable.play_icon),null,null,null);  
		}
		
		String netStr = "none"; // Assume no network
		mConnectivityMgr = 
			(ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = mConnectivityMgr.getActiveNetworkInfo();
		if (info != null) 
			netStr = (mNetworkType == ConnectivityManager.TYPE_WIFI) ? " WIFI" : " MOBILE";
 		mStatusTextView.setText(s + " (GPS = " + LocationManager.GPS_PROVIDER 
				+ ", Ntwk = " + netStr + ")");
		
 		if (mTrack != null) {
 			mPointsTextView.setText(" " + mTrack.mPoints);
 			mExpeditionTextView.setText(" "+ mTrack.mExpeditionNumber);
 			mSwathTextView.setText(" " + mTrack.mSwath);
 			mMinDistTextView.setText(" " + mTrack.mMinDistance);
 			
 			if (mTrack.mLocation != null) {
 				String lat = mTrack.mLocation.getLatitude() + "";
 				String lon = mTrack.mLocation.getLongitude() + "";		
 				mLocationTextView.setText("(" + lat.substring(0,10) + "...," 
 						+ lon.substring(0,10) + "...," + mTrack.mLocation.getAltitude() + ")");	
 			} else
 				Log.w(TAG, "updateView unable to get Location from TrackerState");
 		}
 		Log.d(TAG, "Tracker Activity updated view");
	}


	/**
	 * Part of the View.OnClickListener interface. Called when the button in 
	 * the Tracker View is clicked.  This also handles other booking tasks, 
	 * such as posting and canceling a Notification, displaying a Toast to the UI, and
	 * saving the changed state in the SharedPreferences. 
	 */
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.idTrackerButton: 

		if (mState == TrackerSettings.IDLE) {
			Log.d(TAG, "Starting Tracker Service");

			Intent intent = new Intent(this, TrackerBackgroundService.class);
			intent.putExtra(TrackerState.BUNDLE_NAME, mTrack.bundle());
			startService(intent);
			
			mState = TrackerSettings.RUNNING;
			spEditor.putInt(
					TrackerSettings.TRACKER_STATE_PREFERENCE, 
					TrackerSettings.RUNNING);
			spEditor.commit();
			Utils.showToast(this, "Starting background tracking.");
			postNotification(); 
			
			mTrackerButton.setText("Stop");
			mTrackerButton.setCompoundDrawablesWithIntrinsicBounds(
					getResources().getDrawable(R.drawable.stop_icon),null,null,null);  
		} else  { /* IDLE */ 
			Log.d(TAG, "Stopping Tracker Service");

			stopService(new Intent(this, TrackerBackgroundService.class));
			mNotificationMgr.cancel(R.string.local_service_label);  // Cancel the notification
			mState = TrackerSettings.IDLE;
			spEditor.putInt(
					TrackerSettings.TRACKER_STATE_PREFERENCE, 
					TrackerSettings.IDLE);
			spEditor.commit();
			myLocationOverlay.disableMyLocation();

			Utils.showToast(this, "Tracking is stopped.");
			mTrackerButton.setText("Start");
			mTrackerButton.setCompoundDrawablesWithIntrinsicBounds(
					getResources().getDrawable(R.drawable.play_icon),null,null,null);  
		}
		updateView();
		break;
		case R.id.idTrackerSettingsButton: 
			// Try to get the background service
			mBackgroundService = TrackerBackgroundService.getInstance();
			startActivity(new Intent(this, TrackerSettings.class));
			break;
		}
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
		spEditor.putInt(TrackerSettings.TRACKER_STATE_PREFERENCE, mState);
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
		super.onStop();
		Log.i(TAG,"Stopped");
	}

//	/*
//	 * (non-Javadoc)
//	 * @see android.app.Activity#onCreateDialog(int)
//	 * Confirms with the user that they have changed their project and automatically syncs with the server
//	 * to get all the project finds
//	 */
//	protected Dialog onCreateDialog(int id) {
//		switch (id) {
//		case SET_MINIMUM_DISTANCE:
//			final EditText input = new EditText(this); 
//			return new AlertDialog.Builder(this)
//			.setIcon(R.drawable.icon)
//			.setTitle("Set Minimum Plotting Distance")
//			.setMessage("1-2 meters is good for walking, 25 meters for biking, 100 meters for driving")
//			.setView(input)
//			.setPositiveButton("Ok", 
//					new DialogInterface.OnClickListener() {
//				public void onClick(DialogInterface dialog, int whichButton) {
//					Editable value = input.getText(); 
//					String s = value.toString();
//					if (!s.equals("")) {
//						mMinDistance = Integer.parseInt(s);
//						if (mMinDistance < 0)
//							mMinDistance = 1;  // 1 meter
//					} 
//					Log.i(TAG,"min distance = " + mMinDistance);
//					mMinDistTextView.setText(" " + mMinDistance);
//				}
//			})
//			.setNegativeButton("Cancel", 
//					new DialogInterface.OnClickListener() {
//				public void onClick(DialogInterface dialog, int whichButton) {
//					// Do nothing.
//				}
//			})
//			.create();
//		default:
//			return null;
//		}
//	}
//	
	/**
	 * Listener for changes to the Tracker Preferences.  Since the Tracker
	 * service cannot listen for changes, this method will pass changes to
	 * the tracker. 
	 */
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		Log.d(TAG, "Shared Preference Changed, key = " + key);
			
		if (key != null && mBackgroundService != null) {
			try {
				mBackgroundService.changePreference(sp, key);
			} catch (Exception e) {
		        Log.w(TAG, "Failed to inform Tracker of shared preference change", e);

			}
		}
		mTrack.updatePreference(sp, key);
		updateView();
	}
	
	/**
	 * Required for MapActivity
	 * @see com.google.android.maps.MapActivity#isRouteDisplayed()
	 */
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}




}
