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

package org.hfoss.posit.android.functionplugin.tracker;

import java.util.ArrayList;
import java.util.List;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.activity.OrmLiteBaseMapActivity;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.functionplugin.tracker.TrackerState.PointAndTime;
import org.hfoss.posit.android.sync.Communicator;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.GpsStatus;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;

/**
 * This Activity tracks and maps the phone's location in real time.  The track is displayed
 * on this activity's View.  Listening for Location updates and sending points to the POSIT
 * server are handled by the TrackerBackgroundService. 
 * 
 * @see http://www.calvin.edu/~jpr5/android/tracker.html
 *
 */
public class TrackerActivity extends OrmLiteBaseMapActivity<DbManager> 
implements ServiceUpdateUIListener, View.OnClickListener, OnSharedPreferenceChangeListener,
GpsStatus.Listener, LocationListener { 

	public static final String TAG = "PositTracker";

	private static final String NO_PROVIDER = "No location service";
	private static final boolean RESUMING_SYNC = true;

	public static final int SET_MINIMUM_DISTANCE = 0;

	public static final int GET_EXPEDITION_ID = 1;

	private TextView mPointsTextView;

	private SharedPreferences mPreferences ;
	private SharedPreferences.Editor spEditor;

	private String mProvider = NO_PROVIDER;
	private ConnectivityManager mConnectivityMgr;
	private int mNetworkType;
	private NotificationManager mNotificationMgr;
	private MapController mMapController;

	private Communicator mCommunicator;

	// Location Stuff
	private LocationManager mLocationManager;

	private static TrackerBackgroundService mBackgroundService; 

	// View stuff
	private MapView mapView;
	//	private MapViewExtended mapView;
	private MyLocationOverlay myLocationOverlay;
	private TrackerOverlay mTrackerOverlay;
	private List<Overlay> mOverlays;
	private TextView mLocationTextView;
	private TextView mStatusTextView;
	private TextView mExpeditionTextView;
	private TextView mSwathTextView;
	private TextView mMinDistTextView;
	private Button mTrackerStartStopButton;
	private Button mSettingsSyncRegisterButton;
	private Button mListButton;

	// The current track
	private TrackerState mTrackerState;

	// Current GPS status
	private boolean mGpsHasFix = false;

	// Used when ACTION_VIEW intent -- i.e., for displaying existing expeditions
	private int mExpeditionIdBeingSynced;

	/** 
	 * Called when the activity is first created. Note that if the "Back" key is used while this
	 *  (or any) activity is running, the Activity will be stopped and destroyed.  So if it is started
	 *  again, onCreate() will be called. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Register as a preference listener for changes to tracker state,  
		// preferences (RUNNING, IDLE, VIEWING_MODE, SYNCING).
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mPreferences.registerOnSharedPreferenceChangeListener(this);
		spEditor = mPreferences.edit();

		// RECOVERY HACK:  If Tracker crashes and is stuck in a state, uncomment this and re-run
		//		setExecutionState(TrackerSettings.IDLE);

		//Abort the Tracker if GPS is unavailable
		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		if (!hasGpsEnabled())  {
			this.finish();
			return;
		}

		// Create a communicator for syncing
		mCommunicator = new Communicator();

		// Create a network manager
		mConnectivityMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

		// Initialize call backs so that the Tracker Service can pass data to this UI
		TrackerBackgroundService.setUpdateListener(this);   // The Listener for the Service
		TrackerBackgroundService.setMainActivity(this);

		// Create a new track
		mTrackerState = new TrackerState(this);

		// Make sure the phone is registered with a Server and has a project selected
		if (mTrackerState.mProjId == -1) {
			Toast.makeText(this, "Cannot start Tracker:\nDevice must be registered with a project.", 
					Toast.LENGTH_LONG).show();
			Log.e(TAG, "Cannot start Tracker -- device not registered with a project.");
			return;
		}

		// Get a notification manager
		mNotificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Set up the user interface, which displays the state
		setUpTheUI();

		// Syncing an existing track?
		if (getExecutionState() == TrackerSettings.SYNCING_POINTS) {
			mExpeditionIdBeingSynced = mPreferences.getInt(TrackerSettings.ROW_ID_EXPEDITION_BEING_SYNCED, -1);
			displayExistingExpedition(mExpeditionIdBeingSynced, RESUMING_SYNC); 
		}

		Log.i(TAG,"TrackerActivity,  created with state = " + 
				mPreferences.getInt(TrackerSettings.TRACKER_STATE_PREFERENCE, -1));
	}

	/**
	 * Handles updating the UI when the Activity is resumed. It also 
	 * distinguishes between this activity started from the main POSIT menu
	 * (the default) and started from the TrackerListActivity (action = ACTION_VIEW).
	 * In the latter case the track is just viewable--the Tracker is IDLE.
	 */
	@Override
	public void onResume() {
		super.onResume();
		Log.i(TAG, "onResume(), projectID = " + mTrackerState.mProjId);

		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);  		
		mLocationManager.addGpsStatusListener(this);
		
		if (getExecutionState() == TrackerSettings.IDLE)
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

		if (myLocationOverlay != null) {
			myLocationOverlay.enableMyLocation();
			myLocationOverlay.enableCompass();
		}

		if (Communicator.isServerReachable(this) == false) {
			Log.i(TrackerActivity.TAG, "TrackerActivity there is NO network connectivity");
			Toast.makeText(this, "Note: Tracker currently has NO network connectivity.", Toast.LENGTH_SHORT).show();
		}

		// See whether the Tracker service is running and if so restore the state
		int executionState = getExecutionState();
		if (executionState == TrackerSettings.RUNNING)  {
			//Toast.makeText(this, "The Tracker is RUNNING.", Toast.LENGTH_SHORT).show();
			restoreExpeditionState();
			if (mTrackerState != null)  
				updateUI(mTrackerState);
			else 
				updateViewTrackingMode();
		} else if (executionState == TrackerSettings.IDLE){
			updateViewTrackingMode();
			//Toast.makeText(this, "The Tracker is IDLE.", Toast.LENGTH_SHORT).show();
		} else if (executionState == TrackerSettings.VIEWING_MODE) {
			//Toast.makeText(this, "Viewing an existing track.", Toast.LENGTH_SHORT).show();
		}
		//		else  { // Syncing state 
		////			ArrayList<ContentValues> points = mDbHelper.fetchExpeditionPointsUnsynced(mExpId);
		//			Log.d(TrackerActivity.TAG, "TrackerActivity.onresume points= " + points.size() + " to sync");
		//
		//			if (points.size() == 0) { // There were probably a few lost points 
		//				Log.d(TrackerActivity.TAG, "TrackerActivity, Stopping sync " + points.size() + " to sync");
		//				mSettingsButton.setVisibility(View.GONE);
		//				mListButton.setEnabled(true);
		//				mListButton.setClickable(true);
		//				spEditor.putInt(TrackerSettings.TRACKER_STATE_PREFERENCE, TrackerSettings.IDLE);
		//				spEditor.commit();
		//			} else 
		//				Toast.makeText(this, "This expedition is being synced with the server.", Toast.LENGTH_SHORT).show();
		//		}

		Log.i(TAG,"TrackerActivity,  resumed in state " + executionState);
	}

	/**
	 * A utility method to set the activity's state and save it as a Sharaed Preference. 
	 * 
	 * @param newState Specifies the new state if setFromNewState is true
	 * @return The new execution state
	 */
	public void setExecutionState(int newState) {
		spEditor.putInt(TrackerSettings.TRACKER_STATE_PREFERENCE, newState);
		spEditor.commit();
	}

	/**
	 * Retrieves the current execution state from Preferences
	 */
	private int getExecutionState() {
		return mPreferences.getInt(TrackerSettings.TRACKER_STATE_PREFERENCE, TrackerSettings.IDLE);
	}

	/**
	 * Sets up the user interface for tracking mode
	 */
	private void setUpTheUI() {
		setContentView(R.layout.tracker);
		initializeTheView();
		setUpTheMap();	
	}


	/**
	 * Sets up the map and an overlay for my location. 
	 * NOTE: The points overlay is created in UpdateUI, during tracking.
	 */
	private void setUpTheMap() { 
		mapView = (MapView) findViewById(R.id.mapView);
		//		mapView = (MapViewExtended) findViewById(R.id.mapView);
		mapView.setSatellite(false);
		mMapController = mapView.getController();
		mapView.setBuiltInZoomControls(true);

		// Add an overlay for my location
		mOverlays = mapView.getOverlays();
		if (mOverlays.contains(myLocationOverlay)) {
			mOverlays.remove(myLocationOverlay);
		}
		myLocationOverlay = new MyLocationOverlay(this, mapView);
		mOverlays.add(myLocationOverlay);

		// Display the current GPS location
		Location loc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (loc != null) {
			GeoPoint point = new GeoPoint((int)(loc.getLatitude()*1E6), (int)(loc.getLongitude()*1E6));
			mMapController.animateTo(point);				
		} else {
			Toast.makeText(this, "Current GPS location is unknown", Toast.LENGTH_SHORT).show();
			Log.i(TAG, "Current GPS location is unknown");
		}
	}

	/**
	 * Sets up the View and various widgets.
	 */
	private void initializeTheView() {
		mPointsTextView = (TextView)findViewById(R.id.trackerPoints);
		mLocationTextView = (TextView)findViewById(R.id.trackerLocation);
		mStatusTextView = (TextView)findViewById(R.id.trackerStatus);
		mExpeditionTextView = (TextView)findViewById(R.id.trackerExpedition);
		mSwathTextView = (TextView)findViewById(R.id.trackerSwath);
		mMinDistTextView = (TextView)findViewById(R.id.trackerMinDistance);
		mTrackerStartStopButton = (Button)findViewById(R.id.idTrackerButton);
		mTrackerStartStopButton.setOnClickListener(this);
		mSettingsSyncRegisterButton = (Button)findViewById(R.id.idTrackerSettingsButton);
		mSettingsSyncRegisterButton.setOnClickListener(this);
		mListButton = (Button)findViewById(R.id.idTrackerListButton);
		mListButton.setOnClickListener(this);
		mListButton.setText("List");
	}

	/**
	 * Sets up the view for an existing expedition.
	 * @param expId, the expedition being displayed
	 * @param points, the number of points in the expedition.
	 */
	private void displayExpInView(Expedition exp) {
		if (exp == null)
			return;
		mExpeditionTextView.setText(""+ exp.expedition_num);
		mPointsTextView.setText("" + exp.points);
		mSwathTextView.setText("" + exp.is_synced);
		mTrackerStartStopButton.setClickable(false);
		mTrackerStartStopButton.setEnabled(false);
		mTrackerStartStopButton.setVisibility(View.GONE);
		mSettingsSyncRegisterButton.setClickable(false);
		mSettingsSyncRegisterButton.setEnabled(false);	
		mSettingsSyncRegisterButton.setVisibility(View.VISIBLE);
		mListButton.setText("Delete");
		mListButton.setClickable(true);
		mListButton.setEnabled(true);		

		((TextView)findViewById(R.id.trackerSwathLabel)).setText("Synced");
		((TextView)findViewById(R.id.trackerMinDistLabel)).setVisibility(View.GONE);
		((TextView)findViewById(R.id.trackerMinDistance)).setVisibility(View.GONE);
		((TextView)findViewById(R.id.trackerStatusLabel)).setVisibility(View.GONE);
		((TextView)findViewById(R.id.trackerStatus)).setVisibility(View.GONE);
		((TextView)findViewById(R.id.trackerLabel)).setVisibility(View.GONE);
		((TextView)findViewById(R.id.trackerLocation)).setVisibility(View.GONE);
	}

	/**
	 * Adds the track to the display.
	 * @param tracker
	 */
	public void displayTrack(TrackerState tracker) {
		if (tracker.mPoints != 0) {
			List<PointAndTime> pointsList = tracker.getPoints();
			if (pointsList.size() != 0) {
				PointAndTime aPoint = pointsList.get(pointsList.size()/2);
				GeoPoint point = aPoint.getGeoPoint();
				mMapController.animateTo(point);
			}
		}
		// Display the expedition as an overlay
		if (mapView.getOverlays().contains(mTrackerOverlay)) {
			mapView.getOverlays().remove(mTrackerOverlay);
		}
		mTrackerOverlay = new TrackerOverlay(tracker);
		mOverlays.add(mTrackerOverlay);

	}

	/*
	 * Displays a static expedition, one that was previously collected and stored in the Db. 
	 * Call this with -1 (from onCreate() or onResume() if already in VIEWING_MODE. That means
	 * the Async thread is already syncing the points.  We just want to reset the display.
	 * 
	 * @param expId Either the expedition num of the expedition to be displayed or -1 
	 * to indicate that we are already displaying the track. 
	 * NOTE: expId could be a temporary random Id
	 */
	private void displayExistingExpedition(int expId, boolean isResuming) {
		Log.d(TAG, "TrackerActivity, displayExistingExpedition(), id=" + expId + " isResuming=" + isResuming);

		Expedition expedition = null;
		if (expId != -1) { // Unless we are already in VIEWING_MODE

			// Retrieve data from this expedition

			expedition = this.getHelper().getExpeditionByExpeditionNumber(expId);
			Log.i(TAG, "Expedition to display = " + expedition);

			mTrackerState = new TrackerState();
			mTrackerState.mExpeditionNumber = expedition.expedition_num;
			mTrackerState.mPoints = expedition.getPoints();
			mTrackerState.isRegistered = (expedition.is_registered == 1);
			mTrackerState.mSynced = expedition.getIs_synced();   // Seems a misnomer
			mTrackerState.mProjId = expedition.project_id;

			Log.d(TrackerActivity.TAG, "TrackerActivity.displayExisting mExpId " 
					+ expedition.expedition_num 
					+ " mPoints=" + mTrackerState.mPoints + " mSynced=" + mTrackerState.mSynced 
					+ " mRegistered= " + mTrackerState.isRegistered);


			ArrayList<Points> points = (ArrayList<Points>) getHelper().getPointsByExpeditionId(expedition.expedition_num);
			Log.i(TAG, "Retrieved " + points.size() + " points for expedition " + expId);
			mTrackerState.setPointsFromDbValues(points);
		}

		// Display the expedition
		initializeTheView();
		displayExpInView(expedition);
		displayTrack(mTrackerState);

		if (expId != -1) {

			// Get the unsynced points
			ArrayList<Points> unsyncedPoints = (ArrayList<Points>) getHelper().getUnsyncedPointsByExpeditionId(expedition.expedition_num);

			// If there are points to sync and we have a network connection now
			if (unsyncedPoints.size() > 0 && Communicator.isServerReachable(this)) { // mConnectivityMgr.getActiveNetworkInfo() != null) {

				if (expedition.is_registered == Expedition.EXPEDITION_IS_REGISTERED) {
					mSettingsSyncRegisterButton.setText("Sync");
					mSettingsSyncRegisterButton.setVisibility(View.VISIBLE);
				}
				else  {
					mSettingsSyncRegisterButton.setText("Register");
					Toast.makeText(this, "This expedition needs to be registered with the server. " +
							"Please click the register button", Toast.LENGTH_SHORT).show();
					mSettingsSyncRegisterButton.setVisibility(View.VISIBLE);

				}
				if (isResuming) {
					mSettingsSyncRegisterButton.setClickable(false);
					mSettingsSyncRegisterButton.setEnabled(false);	
					mListButton.setClickable(false);
					mListButton.setEnabled(false);
					//Utils.showToast(this, "This expedition is still being synced.");
				} else {
					mSettingsSyncRegisterButton.setClickable(true);
					mSettingsSyncRegisterButton.setEnabled(true);	
					mSettingsSyncRegisterButton.setVisibility(View.VISIBLE);
				}
			} else {
				mSettingsSyncRegisterButton.setVisibility(View.GONE);
			}
		}
	}

	/**
	 * Checks for GPS service
	 * @return
	 */
	private boolean hasGpsEnabled() {		
		if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			mProvider = LocationManager.GPS_PROVIDER;
		} else {
			Toast.makeText(this, "Aborting Tracker: " + NO_PROVIDER
					+ "\nYou must have GPS enabled. ", Toast.LENGTH_LONG).show();
			return false;
		}
		Log.i(TAG, "TrackerActivity,  hasNecessaryServices() = true");
		return true;
	}


	/**
	 * Called by the Tracker Service to update UI.  It must run in the UI thread. 
	 */
	public void updateUI(final TrackerState tracker) {
		//		Log.i(TAG, "Updating UI");
		mTrackerState = tracker;
		// Make sure this runs in the UI thread... since it's messing with views...
		this.runOnUiThread(new Runnable() {
			public void run() {
				restoreExpeditionState(); 
				updateViewTrackingMode();
				if (tracker.mLocation != null) {
					myLocationOverlay.onLocationChanged(tracker.mLocation);
					Log.i(TAG,"TrackerActivity, updatingUI  <" + tracker.mLocation.getLatitude() + "," + tracker.mLocation.getLongitude() + ">");
				} else 
					Log.w(TAG,"TrackerActivity, updatingUI  unable to get location from TrackerState");
			}
		});
	}

	/**
	 * A helper method to restore the Activity's state and its UI when it is restarted.  The
	 * Service runs in the background independently of the foreground Activity (the UI).  We
	 * get some of the data from the Service -- namely the TrackerState object, which contains
	 * an array of all the points collected and the current lat,long,alt. 
	 */
	private void restoreExpeditionState() {
		mBackgroundService = TrackerBackgroundService.getInstance();
		if (mBackgroundService != null) {
			mTrackerState = mBackgroundService.getTrackerState();
			if (mapView.getOverlays().contains(mTrackerOverlay)) {
				mapView.getOverlays().remove(mTrackerOverlay);
			}
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
	 * A helper method to update the UI. Most of the data displayed in the View
	 * are taken from the TrackerState object,  mTrack.
	 */
	private void updateViewTrackingMode() {	
		int executionState = getExecutionState();
		Log.i(TAG, "start/stop button = " + this.mTrackerStartStopButton.getText());
		Log.i(TAG, "execution state = " + getExecutionState());

		if (executionState == TrackerSettings.VIEWING_MODE || 
				executionState == TrackerSettings.SYNCING_POINTS) {			
			return;
		}

		String s = " Idle ";

		// Manage the control buttons
		if (executionState == TrackerSettings.RUNNING) {
			s = " Running ";
			mTrackerStartStopButton.setText("Stop");
			mTrackerStartStopButton.setCompoundDrawablesWithIntrinsicBounds(
					getResources().getDrawable(R.drawable.stop_icon),null,null,null); 
		} else {
			mTrackerStartStopButton.setText("Start");
			mTrackerStartStopButton.setCompoundDrawablesWithIntrinsicBounds(
					getResources().getDrawable(R.drawable.play_icon),null,null,null);  
			if (mGpsHasFix)
				mTrackerStartStopButton.setEnabled(true);
			else 
				mTrackerStartStopButton.setEnabled(false);
		}

		// Display the current state of the GPS and Network services.
		String netStr = "none"; // Assume no network
		NetworkInfo info = mConnectivityMgr.getActiveNetworkInfo();
		if (info != null) {
			mNetworkType = info.getType();
			netStr = (mNetworkType == ConnectivityManager.TYPE_WIFI) ? " WIFI" : " MOBILE";
		}

		mStatusTextView.setText(s + " (GPS = " + mTrackerState.mProvider + 
				", Ntwk = " + netStr + ")");

		// Display the expedition's (i.e., mTrack's) current state.
		if (mTrackerState != null) {
			mPointsTextView.setText("" + mTrackerState.mPoints + " (" + mTrackerState.mSynced + ")");
			mExpeditionTextView.setText(""+ mTrackerState.mExpeditionNumber);
			mSwathTextView.setText("" + mTrackerState.mSwath);
			mMinDistTextView.setText("" + mTrackerState.mMinDistance);

			if (mTrackerState.mLocation != null) {
				// Center on my location
				Double geoLat = mTrackerState.mLocation.getLatitude()*1E6;
				Double geoLng = mTrackerState.mLocation.getLongitude()*1E6;
				GeoPoint point = new GeoPoint(geoLat.intValue(), geoLng.intValue());
				mMapController.animateTo(point);
				String lat = mTrackerState.mLocation.getLatitude() + "";
				String lon = mTrackerState.mLocation.getLongitude() + "";		
				mLocationTextView.setText("(" + lat.substring(0,Math.min(lat.length(),10)) + "...," 
						+ lon.substring(0,Math.min(lon.length(), 10)) + "...," + mTrackerState.mLocation.getAltitude() + ")");	
			} else
				Log.w(TAG, "TrackerActivity, updateView unable to get Location from TrackerState");
		}
		Log.d(TAG, "TrackerActivity,  updated view");
	}

	/**
	 * Part of the View.OnClickListener interface. Called when any button in 
	 * the Tracker View is clicked.  This also handles other bookkeeping tasks, 
	 * such as posting and canceling a Notification, displaying a Toast to the UI, and
	 * saving the changed state in the SharedPreferences. 
	 */ 
	public void onClick(View v) {
		switch (v.getId()) {

		case R.id.idTrackerButton:   // The start/stop button

			int executionState = getExecutionState();
			if (executionState == TrackerSettings.IDLE) {  // Start the tracker
				startTrackerService();
				mListButton.setClickable(false);
				mListButton.setEnabled(false);
				mLocationManager.removeUpdates(this);
			} else  { /* RUNNING */            // Stop the tracker
				stopTrackerService();
				mListButton.setClickable(true);
				mListButton.setEnabled(true);
			}

			updateViewTrackingMode();
			break;

			// This button starts the TrackerSettings activity which lets the user
			//  change the tracker parameters on the fly. Changes are commnicated
			//  through shared preferences.
		case R.id.idTrackerSettingsButton:   
			String text = (String) mSettingsSyncRegisterButton.getText();
			Log.d(TAG, "Text = " + text);
			if (text.equals("Settings")) {
				// Try to get the background service
				mBackgroundService = TrackerBackgroundService.getInstance();
				startActivity(new Intent(this, TrackerSettings.class));	

			} else if (text.equals("Sync")) {  
				// This is only reached by clicking on the Sync button in List View	
				mSettingsSyncRegisterButton.setEnabled(false);
				mSettingsSyncRegisterButton.setClickable(false);
				mSettingsSyncRegisterButton.setVisibility(View.VISIBLE);
				mListButton.setEnabled(false);
				mListButton.setClickable(false);
				syncUnsyncedPoints();
			} else if (text.equals("Register")) {
				Toast.makeText(this, "Attempting to register with server. Please wait.", Toast.LENGTH_SHORT).show();
				registerUnregisteredExpedition();
			}
			break;

			// The "list" button is used for listing and deleting tracks. The button's 
			//	text and icon are changed depending on the current state of the Activity.
		case R.id.idTrackerListButton:
			text = (String) mListButton.getText();
			if (text.equals("List")) {				
				Intent intent = new Intent(this, TrackerListActivity.class); // List tracks
				startActivityForResult(intent, GET_EXPEDITION_ID);
			} else { 						// Delete this track
				deleteExpedition();
				finish();
			}
			break;
		}
	}


	/**
	 * Returns the result of selecting an Expedition in TrackerListActivity
	 * @param requestCode The code of the subactivity
	 * @param resultCode Success or canceled
	 * @param data An Intent containing the rowId of the selected expedition
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(TrackerActivity.TAG, "onActivityResult reqcode = " + requestCode + " resultCode = " + resultCode);

		switch (requestCode) {

		case GET_EXPEDITION_ID:
			int expId = 0;
			if (resultCode == Activity.RESULT_OK) {
				expId = data.getIntExtra(Expedition.EXPEDITION_NUM, -1);
				Log.d(TrackerActivity.TAG, "onActivityResult expedition expId = " + expId);

				setExecutionState(TrackerSettings.VIEWING_MODE);
				mExpeditionIdBeingSynced = expId;
				displayExistingExpedition(expId, !RESUMING_SYNC);  // i.e., starting a new sync
			}
			break;
		}
	}

	/**
	 *  Registers an expedition that was recorded while network was unavailable. When
	 *  called mExpId equals the expeditions Id
	 */
	private void registerUnregisteredExpedition() {	
		int existingId = mTrackerState.mExpeditionNumber;

		// Get a new Id from the server
		int newId = mCommunicator.registerExpeditionId(this, mTrackerState.mProjId);
		Log.d(TrackerActivity.TAG, "Register Expedition newId = " + newId);
		if (newId != -1) {
			Log.d(TrackerActivity.TAG, "TrackerActivity.registerUnregistered Success! New Expedition Id = " + newId);
			mTrackerState.isRegistered = true;
			mTrackerState.isInLocalMode = false;

			// Update phone's Db
			// Create a record or update an existing record in Db for this expedition
			try {
				ContentValues expVals = new ContentValues();
				expVals.put(Expedition.EXPEDITION_NUM, newId);
				expVals.put(Expedition.EXPEDITION_SYNCED, Expedition.EXPEDITION_NOT_SYNCED);
				expVals.put(Expedition.EXPEDITION_REGISTERED, Expedition.EXPEDITION_IS_REGISTERED);
				int rows = this.getHelper().updateExpedition(existingId, expVals);	
				if (rows != 0) {
					Log.i(TrackerActivity.TAG, "Updated Expedition " + existingId + " to " + newId);
				} else {
					Log.i(TrackerActivity.TAG, "Failed updating Expedition " + existingId + " to " + newId);
				}

			} catch (Exception e) {
				Log.e(TrackerActivity.TAG, "TrackerService.Async, registeringExpedition " + e.getMessage());
				e.printStackTrace();
			}

			mTrackerState.mExpeditionNumber =  newId; 

			// Update each of the expedition's points in the phone's Db
			ArrayList<Points> unsyncedPoints = (ArrayList<Points>) getHelper().getUnsyncedPointsByExpeditionId(existingId);

			Log.i(TAG, "Updating the "+ unsyncedPoints.size() + " points associated with " + existingId + "(now " + newId + ")");
			if (unsyncedPoints.size() != 0) {
				ContentValues vals = new ContentValues();
				vals.put(Points.EXPEDITION, newId);      // New expedition Id

				for (Points point: unsyncedPoints) {
					int id = point.id;
					getHelper().updateGPSPoint(id, vals);
				}
			}				
		} else {
			Log.d(TrackerActivity.TAG, "TrackerActivity.registeredUnregistered FAILED TO REGISTER expedition");
			return;			
		}

		// Now let the user sync the unsynced points with the Server
		mTrackerState.mExpeditionNumber = newId;
		mExpeditionTextView.setText(""+ newId);
		mSettingsSyncRegisterButton.setText("Sync");  
		mSettingsSyncRegisterButton.setVisibility(View.VISIBLE);
	}

	/**
	 * Register the expedition on the server
	 */
	public int registerExpedition(TrackerState tracker) {
		Log.i(TAG, "Register expedition, tracker = " + tracker);

		int expId = -1;
		boolean serverIsReachable = Communicator.isServerReachable(this);			
		if (serverIsReachable) {
			expId = mCommunicator.registerExpeditionId(this, mTrackerState.mProjId);
			if (expId != -1) {
				mTrackerState.isRegistered = true;
				tracker.isRegistered = true;
				mTrackerState.isInLocalMode = false;
				tracker.isInLocalMode = false;
			}
			Log.d(TrackerActivity.TAG, "Register Expedition expId = " + expId);
		}

		// If no network or some kind of server problem, create a temporary expId
		//  and put the tracker in local mode
		if (!serverIsReachable || expId == -1) {
			expId = TrackerSettings.MIN_LOCAL_EXP_ID 
			+ (int)(Math.random() * TrackerSettings.LOCAL_EXP_ID_RANGE);  // Create a random expId
			mTrackerState.isInLocalMode = true;
			tracker.isInLocalMode = true;
		}

		// Create a record or update an existing record in Db for this expedition
		try {
			insertExpeditionToDb(expId);
		} catch (Exception e) {
			Log.e(TrackerActivity.TAG, "TrackerService.Async, registeringExpedition " + e.getMessage());
			e.printStackTrace();
		}

		mTrackerState.mExpeditionNumber =  expId; 
		tracker.mExpeditionNumber = expId;
		return expId;
	}		


	/**
	 * Helper method to insert the expedition into the db.
	 * @param expId The expedition's id.
	 */
	private synchronized void insertExpeditionToDb (int expId) {
		Log.i(TAG, "insertExpeditionToDb "  + expId);
		ContentValues values = new ContentValues();

		values.put(Expedition.EXPEDITION_NUM, expId);
		values.put(Expedition.EXPEDITION_PROJECT_ID, mTrackerState.mProjId);

		if (mTrackerState.isRegistered)
			values.put(Expedition.EXPEDITION_REGISTERED, Expedition.EXPEDITION_IS_REGISTERED);
		else 
			values.put(Expedition.EXPEDITION_REGISTERED, Expedition.EXPEDITION_NOT_REGISTERED);

		try {
			int rows = getHelper().addNewExpedition(values);

			if (rows > 0) {
				Log.i(TAG, "TrackerService, saved expedition " 
						+ expId + " projid= " + mTrackerState.mProjId);
				//			Utils.showToast(this, "Saved expedition " + mState.mExpeditionNumber);
				if (mTrackerState != null)
					mTrackerState.setSaved(true);
			} else {
				Log.i(TAG, "TrackerService, Db Error: exped=" + expId + " proj=" 
						+ mTrackerState.mProjId);
				//			Utils.showToast(this, "Oops, something went wrong when saving " + mState.mExpeditionNumber);
			}
		} catch (Exception e) {
			Log.e(TrackerActivity.TAG, "TrackerService.Async, insertExpeditionToDb " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Helper method retrieves unsycned points from Db and sends them to the server in a
	 * background thread. 
	 */
	private void syncUnsyncedPoints() {

		// Get the unsynced points
		List<Points> points = getHelper().getUnsyncedPointsByExpeditionId(mTrackerState.mExpeditionNumber);

		if (points.size() == 0) { // There were probably a few lost points 
			Log.d(TrackerActivity.TAG, "TrackerActivity, Stopping sync " + points.size() + " to sync");
			mSettingsSyncRegisterButton.setVisibility(View.GONE);
			mListButton.setEnabled(true);
			mListButton.setClickable(true);
		}

		Log.d(TrackerActivity.TAG, "TrackerActivity, Syncing " + points.size() + " unsynced points");
		Toast.makeText(this, "Syncing " + points.size() + " unsynced points", Toast.LENGTH_SHORT).show();

		ContentValues[] valuesArray = new ContentValues[points.size()];
		int k = 0;
		for (Points point: points) {
			valuesArray[k] = point.toContentValues();
			Log.i(TAG, "Point: " + valuesArray[k]);
			k++;					
		}

		setExecutionState(TrackerSettings.SYNCING_POINTS);
		spEditor.putInt(TrackerSettings.ROW_ID_EXPEDITION_BEING_SYNCED, mExpeditionIdBeingSynced);
		spEditor.commit();

		// A background task is used to sync the array of unsynced points.
		AsyncSendPointTask asyncTask = new AsyncSendPointTask(this, mTrackerState);
		asyncTask.execute(valuesArray);
	}

	/**
	 * Invoked from the AsyncThread to update the UI. This method could be called
	 * in two different states, SYNCING_POINTS, where this method is used to update
	 * the UI.  This method is not used when syncing in the RUNNING state. 
	 * 
	 * @param nSynced
	 */
	public void asyncUpdate(int nSynced) {
		Log.i(TAG, "asyncUpdate, nSynced = " + nSynced);
		
		// Update the display
		if (getExecutionState() == TrackerSettings.SYNCING_POINTS)
			this.mSwathTextView.setText("" + (mTrackerState.mSynced + nSynced));
	}

	/**
	 * This method is called when AsyncSendPointTask completes its background task.
	 * It can be called in two different states, SYNCING_POINTS, where an array of
	 * points is synced at the same time, and RUNNING, where one point at a time
	 * is synced.  
	 *  
	 * @param syncedPoints, a ContentValues list of each point that was synced to server.
	 */
	public void asyncResult(List<ContentValues> syncedPoints) {
		Log.i(TAG, "AsyncResult, #synced = " + syncedPoints.size());

		if (syncedPoints.size() == 0)
			return;

		int expNum = 0;  // We'll get the expNum from the points.
		try {
			for (ContentValues vals : syncedPoints) {
				Log.i(TAG, "AsyncResult syncedPoint = " + vals);
				int pointId = vals.getAsInteger(Points.EXPEDITION_GPS_POINT_ROW_ID);
				expNum = vals.getAsInteger(Points.EXPEDITION);   // They all have the same expNum
				ContentValues updateVals = new ContentValues();
				updateVals.put(Points.GPS_SYNCED, Points.GPS_IS_SYNCED);
				Log.i(TAG, "mTrackerState.mSynced=" + mTrackerState.mSynced); 
				if ( getHelper().updateGPSPoint(pointId, updateVals) ) {
					++mTrackerState.mSynced;
				}
			}	
			ContentValues updateVals = new ContentValues();
			updateVals.put(Expedition.EXPEDITION_SYNCED,mTrackerState.mSynced); // mSynced);
			getHelper().updateExpedition(expNum, updateVals);
		} catch(Exception e) {
			Log.e(TrackerActivity.TAG, "TrackerService.Async, updatePointAndExpedition " + e.getMessage());
			e.printStackTrace();				
		}

		// If SYNCING_POINTS if done, set the state to IDLE and refresh the UI
		if (getExecutionState() == TrackerSettings.SYNCING_POINTS) {
			setExecutionState(TrackerSettings.IDLE);
			displayExistingExpedition(expNum, false);	
		}
	}

	/**
	 * Helper method to delete the current track. Returns to TrackerListActivity
	 */
	private void deleteExpedition() {
		int mExpedNum = Integer.parseInt((String) mExpeditionTextView.getText().toString().trim());
		boolean success = getHelper().deleteExpedition(mExpedNum);
		if (success) {
			if (mTrackerState.mPoints > 0) {
				success = getHelper().deleteExpeditionPoints(mExpedNum);
				if (success) {
					Log.i(TAG, "TrackerActivity, Deleted expedition " + mExpedNum);
					Toast.makeText(this, "Deleted expedition " + mExpedNum, Toast.LENGTH_SHORT).show();
				} else {
					Log.i(TAG, "TrackerActivity, Oops, something wrong when deleting expedition " + mExpedNum);
					Toast.makeText(this, "Oops, something went wrong when deleting " + mExpedNum, Toast.LENGTH_SHORT).show();
				}
			} else {
				Log.i(TAG, "TrackerActivity, Deleted expedition " + mExpedNum);
				Toast.makeText(this, "Deleted expedition " + mExpedNum, Toast.LENGTH_SHORT).show();					
			}
		} else {
			Log.i(TAG, "TrackerActivity, Error deleting expedition " + mExpedNum);
			Toast.makeText(this, "Error deleting expedition " + mExpedNum, Toast.LENGTH_SHORT).show();			
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
		// PRoblem with this is when it is used, you can never restart POSIT
		// at the main screen.  Try: Start Posit, start tracking, stop posit,
		// Click on the "tracker" notification.  Stop the tracker. Hit the back
		// key. You will exit to Android.  Now try restarting Posit.  It 
		// always starts directly in Tracker, rather than PositMain.  
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, TrackerActivity.class), PendingIntent.FLAG_ONE_SHOT);


		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this,
				getText(R.string.local_service_label), text, contentIntent);

		// Post the notification
		mNotificationMgr.notify(R.string.local_service_label, notification);
	}

	/*
	 * Helper method to start the background tracker service.
	 */
	private void startTrackerService() {
		Log.d(TAG, "TrackerActivity, Starting Tracker Service");

		mTrackerState= new TrackerState(this);

		mBackgroundService = new TrackerBackgroundService();


		Intent intent = new Intent(this, TrackerBackgroundService.class);
		intent.putExtra(TrackerState.BUNDLE_NAME, mTrackerState.bundle());
		startService(intent);

		setExecutionState(TrackerSettings.RUNNING);
		Toast.makeText(this, "Starting background tracking.", Toast.LENGTH_SHORT).show();
		//postNotification(); 

		mTrackerStartStopButton.setText("Stop");
		mTrackerStartStopButton.setCompoundDrawablesWithIntrinsicBounds(
				getResources().getDrawable(R.drawable.stop_icon),null,null,null); 
		updateViewTrackingMode();
	}

	/*
	 * Helper method to stop background tracker service.
	 */
	private void stopTrackerService () {
		Log.d(TAG, "TrackerActivity, Stopping Tracker Service");

		if (mBackgroundService != null)
			mBackgroundService.stopListening();

		stopService(new Intent(this, TrackerBackgroundService.class));
		mNotificationMgr.cancel(R.string.local_service_label);  // Cancel the notification

		setExecutionState(TrackerSettings.IDLE);		
		myLocationOverlay.disableMyLocation();

		Toast.makeText(this, "Tracking is stopped.", Toast.LENGTH_SHORT).show();
		mTrackerStartStopButton.setText("Start");
		mTrackerStartStopButton.setCompoundDrawablesWithIntrinsicBounds(
				getResources().getDrawable(R.drawable.play_icon),null,null,null);  		
	}

	@Override
	public void onPause() {
		super.onPause();
		myLocationOverlay.disableMyLocation();
		myLocationOverlay.disableCompass();
		mLocationManager.removeGpsStatusListener(this);
		mLocationManager.removeUpdates(this);
		Log.i(TAG,"TrackerActivity, Paused in state: " + getExecutionState());
	}

	// The following methods don't change the default behavior, but they show (in the Log) the
	// life cycle of the Activity.

	@Override protected void onDestroy() {
		super.onDestroy();

		// If stopped in Viewing mode, reset the state to IDLE;. 
		// else if it stopped while syncing points, set it to IDLE if all points synced
		// otherwise leave its state unchanged

		int executionState = getExecutionState();
		if (executionState == TrackerSettings.VIEWING_MODE)
			setExecutionState(TrackerSettings.IDLE);
		else if (executionState == TrackerSettings.SYNCING_POINTS) {
			setExecutionState(TrackerSettings.IDLE);
			//				if (mSynced == mPoints) {
			//					setExecutionState(TrackerSettings.IDLE);
			//				} else {
			//					spEditor.putInt(TrackerSettings.ROW_ID_EXPEDITION_BEING_SYNCED, mExpeditionIdBeingSynced);
			//					spEditor.commit();
			//				}
		} 

		Log.i(TAG,"TrackerActivity, Destroyed in state " + getExecutionState());
		//Utils.showToast(this, " TrackerActivity Destroyed in state " + mExecutionState);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG,"TrackerActivity, Stopped");
	}

	/**
	 * Listener for changes to the Tracker Preferences.  Since the Tracker
	 * service cannot listen for changes, this method will pass changes to
	 * the service. 
	 */
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		Log.d(TAG, "TrackerActivity, Shared Preference Changed, key = " 
				+ key);

		// This is a hack.  TrackerState is not a shared preference but 
		// this is called repeatedly throughout the run with this key.
		// Don't understand why???

		if (key.equals("TrackerState")) 
			return;

		if (key != null && mBackgroundService != null) {
			try {
				mBackgroundService.changePreference(sp, key);
			} catch (Exception e) {
				Log.w(TAG, "TrackerActivity, Failed to inform Tracker of shared preference change", e);
			}
		}
		if (mTrackerState != null) {
			mTrackerState.updatePreference(sp, key);
			updateViewTrackingMode();
		}
	}

	/**
	 * Required for MapActivity
	 * @see com.google.android.maps.MapActivity#isRouteDisplayed()
	 */
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	/**
	 * GpsStatus.Listener interface.
	 */
	public void onGpsStatusChanged(int event) {
		if (event == GpsStatus.GPS_EVENT_FIRST_FIX) {
			Log.i(TAG, "Gps first fix ");
			this.mGpsHasFix = true;
			this.mTrackerStartStopButton.setEnabled(true);
		}
		else if (event == GpsStatus.GPS_EVENT_STARTED) {
			Log.i(TAG, "Gps Started");
//			this.mGpsHasFix = true;
//			this.mTrackerStartStopButton.setEnabled(true);
		}
		else if (event == GpsStatus.GPS_EVENT_STOPPED) {
			Log.i(TAG, "Gps Stopped");
		}
	}

	public void onLocationChanged(Location location) {	
		Log.i(TAG, "location changed, accuracy = " + location.getAccuracy() + " meters");
	}

	public void onProviderDisabled(String provider) {
		Log.i(TAG, "onProviderDisabled()");
		this.mTrackerStartStopButton.setEnabled(false);
		
	}

	public void onProviderEnabled(String provider) {
		Log.i(TAG, "onProviderEnsabled()");
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.i(TAG, "onProviderDisabled()");
		if (status == LocationProvider.OUT_OF_SERVICE ||
				status == LocationProvider.TEMPORARILY_UNAVAILABLE)
			this.mTrackerStartStopButton.setEnabled(false);
	}
}
