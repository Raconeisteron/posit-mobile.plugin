package org.hfoss.posit.android;

import java.util.List;

import org.hfoss.posit.android.provider.PositDbHelper;
import org.hfoss.posit.android.utilities.MyItemizedOverlay;
import org.hfoss.posit.android.utilities.Utils;
import org.hfoss.posit.android.web.Communicator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ZoomControls;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class CoverageTrackerActivity extends MapActivity implements OnSharedPreferenceChangeListener {
	

	
	private static final String TAG = "TrackerActivity";

	public static final String SHARED_STATE = "TrackerState";
	public static final String NO_PROVIDER = "No location service";
	
	public static final int UPDATE_LOCATION = 2;
	public static final boolean ENABLED_ONLY = true;
	public static final int CONFIRM_EXIT=0;

	public static final int IDLE = 0;
	public static final int RUNNING = 1;
	public static final int PAUSED = 2;

	private static final int SLEEP_INTERVAL = 5000; // milliseconds
	private static final int UPDATES_INTERVAL = 5000; // 
	private static final int DEFAULT_SWATH_WIDTH = 50;  // 50 meters
	private static final String PROVIDER = "gps";

	private static final String GPS_POINT_LATITUDE = "latitude";
	private static final String GPS_POINT_LONGITUDE = "longitude";
	private static final String GPS_POINT_ALTITUDE = "altitude";
	private static final String GPS_POINT_SWATH = "swath";
	
	private double mLongitude = 0;
	private double mLatitude = 0;
	private double mAltitude = 0;
	private long mSwath = DEFAULT_SWATH_WIDTH;
	
	private Communicator mCommunicator;

	private TextView mLocationTextView;
	private TextView mStatusTextView;
	private TextView mExpeditionTextView;
	private TextView mPointsTextView;
	private TextView mSwathTextView;

    private SharedPreferences mPreferences ;
    private SharedPreferences.Editor spEditor;
    
	private MapView mMapView;
	private MapController mapController;
	private List<Overlay> mapOverlays;
	private ZoomControls mZoom;
	private LinearLayout linearLayout;
	MyItemizedOverlay points;
	MyItemizedOverlay reloadPoints;

	private Thread mThread;
	private LocationManager mLocationManager;
	private Location mLocation;
	private String mProvider = NO_PROVIDER;
	
	private ConnectivityManager mConnectivityMgr;
	private int mNetworkType;
	
	private NotificationManager mNotificationManager;
	public static final int NOTIFY_TRACKER_ID = 1001;

	private static final int CHANGE_SETTINGS = 0;
	
	private int mState; 
	private int mExpeditionNumber;
	private int mProjId;
	private int mPoints;
	private long mRowId;
	private PositDbHelper mDbHelper;
	private boolean hasNetworkService;
	private boolean hasGpsService;

	private SharedPreferences localPreferences;

	private boolean tracking;


	
	/**
	 * Sets up the initial state of the Tracker.  This method should only be
	 *  reached when Tracker starts up.  Tracker saves its state in SharedPreferences
	 *  when the user selects PositMain from its menu. This allows the user to perform
	 *  any POSIT actions while the Tracker is running in the background. 
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate()");
		
		mDbHelper = new PositDbHelper(getApplicationContext());
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		
			
		setContentView(R.layout.tracker);
		mLocationTextView = (TextView)findViewById(R.id.trackerLocation);
		mStatusTextView = (TextView)findViewById(R.id.trackerStatus);
		mExpeditionTextView = (TextView)findViewById(R.id.trackerExpedition);
		mPointsTextView = (TextView)findViewById(R.id.trackerPoints);
		mSwathTextView = (TextView)findViewById(R.id.trackerSwath);
		linearLayout = (LinearLayout) findViewById(R.id.zoomView);
		mMapView = (MapView) findViewById(R.id.mapView);
		mZoom = (ZoomControls) mMapView.getZoomControls();
		linearLayout.addView(mZoom);
		
		points = new MyItemizedOverlay(this.getResources().getDrawable(R.drawable.red_dot),this,false);
		reloadPoints = new MyItemizedOverlay(this.getResources().getDrawable(R.drawable.red_dot),this,false);
		mapController = mMapView.getController();
		mapController.setZoom(17);
		
		

		tracking = mPreferences.getBoolean("Tracking",false);
		mState = mPreferences.getInt("state", IDLE);

			
		
		mPoints = 0;
		if (!doSetup()) {
			finish();
			return;
		}
		else if (!checkAllServices()) {
			finish();
			return;
		}
		else if (tracking) {
			mExpeditionNumber = mPreferences.getInt("Expedition Num", -1);
			mapOverlays = mMapView.getOverlays();
			// we assume that this activity will call the service at some point instead of 
			// the service being called in Posit Main
//			ContentValues[] GPSValues = mDbHelper.fetchExpeditionDataByExpeditionNum((long)mExpeditionNumber);
		    mPoints = mPreferences.getInt("Points", 0);
//			for (int i = 0; i<GPSValues.length; i++)
//			{
//			mLatitude = GPSValues[i].getAsDouble(GPS_POINT_LATITUDE);
//			mLatitude = GPSValues[i].getAsDouble(GPS_POINT_LONGITUDE);
//				
////			int lat = (int)(mLatitude * 1E6);
////			int lon = (int)(mLongitude * 1E6);
//			
//			
////			reloadPoints.addOverlay(new OverlayItem(new GeoPoint(lat,lon),null,null));
////			mapOverlays.add(reloadPoints);
//			Log.i(TAG,"Point added " +points.size()+" "+ GPSValues.length + "  " + mPreferences.getInt("Points", 0) );
//			updateView();
//			}
			mapOverlays.add(mapLayoutPoints(mDbHelper.fetchExpeditionDataByExpeditionNumReturnCursor((long)mExpeditionNumber)));
			mapController = mMapView.getController();
			
			
			
			
	}
        updateView();
		mPreferences.registerOnSharedPreferenceChangeListener(this);

	}
	
	private  MyItemizedOverlay mapLayoutPoints(Cursor c) {
		int latitude = 0;
		int longitude = 0;

		MyItemizedOverlay mPoints1 = new MyItemizedOverlay(this.getResources().getDrawable(R.drawable.red_dot),this,false);
		c.moveToFirst();

		do {
			latitude = (int) (c.getDouble(c
					.getColumnIndex(PositDbHelper.GPS_POINT_LATITUDE))*1E6);
			longitude = (int) (c.getDouble(c
					.getColumnIndex(PositDbHelper.GPS_POINT_LONGITUDE))*1E6);


			Log.i(TAG, latitude+" "+longitude+" ");
			mPoints1.addOverlay(new OverlayItem(new GeoPoint(latitude,longitude),null,null));
		} while (c.moveToNext());
		return mPoints1;
	}
	
	//----------------menu methods---------------------
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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
		if (tracking == true) {
			menu.findItem(R.id.start_tracking_menu_item).setEnabled(false);
			menu.findItem(R.id.stop_tracking_menu_item).setEnabled(true);
		} else {
			menu.findItem(R.id.start_tracking_menu_item).setEnabled(true);
			menu.findItem(R.id.stop_tracking_menu_item).setEnabled(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
		case R.id.start_tracking_menu_item:
//			mExpeditionNumber = registerExpedition();
			//startLocationUpdateService(mProvider);
	        
	        
			mPoints = 0;
			startTracking();
			
			//loop to insure that we have the appropriate expedition number
			
//while (mPreferences.getInt("Expedition Num", -2)<-1)
//Log.i(TAG, "waiting....");
//			Thread t = new Thread(){
//				public void run() {
//					while((mPreferences.getInt("Expedition Num", -2)<-1)){
//						Log.i(TAG, "waiting...");
//						Log.i(TAG, mPreferences.getInt("Expedition Num", -2)+"");
//					}
////					  updateView();
//				};
//			};
//			
//			t.start();
		
//	        updateView();
			//Utils.showToast(this, "Expedition number " + mExpeditionNumber);
			break;
//		case R.id.back_to_main_menu_item:
//			// TODO: This should be handled in a better way, utilizing the Android lifecycle
//			// This approach appears to add another instance of PositMain to the Activity stack
//			// rather than returning to the parent.  Possible approach:  Have the Tracker save
//			// it's state and then return.
//			startActivity(new Intent(this,PositMain.class));
//			break;
			//			case R.id.new_expedition_menu_item:
			////				registerExpedition();
			//				break;
			//				
		case R.id.stop_tracking_menu_item:
			stopTracking();
			break;
		}
		return true;
	}
	
	
	
	/**
	 * Shuts down the location service and notification manager.
	 */
	private void stopTracking() {
		updateState(IDLE);
		
		
        stopService(new Intent(CoverageTrackerActivity.this,
                TrackerService.class));
        
        
        tracking =false;

	}
	
	/**
	 * Create the background thread and starts it. Note the call to Looper.prepare().
	 * This is necessary in order for the thread to invoke the Handler.  
	 *  
	 * @see http://developerlife.com/tutorials/?p=290
	 * @param backgroundTrackerActivity
	 */
	private void startTracking() {
		// Check that we have service
		if (!checkAllServices()) {
			finish();
			return;
		}

        startService(new Intent(CoverageTrackerActivity.this,
                TrackerService.class));
        
        tracking =true;
        
		updateState(RUNNING);


		Utils.showToast(this, "Tracking the phone's location");
	}
	
	//-------------------setup methods-------------
	//TODO
	/**
	 * Starts the update service unless there is no provider available.
	 */
//	private boolean startLocationUpdateService(String provider) {
//		Log.i(TAG, "startLocationUpdateService()");
//		if (provider.equals(NO_PROVIDER))
//			return false;
//
//		try {
//			mLocationManager.requestLocationUpdates(provider, UPDATES_INTERVAL, 0, this);	
//		} catch (Exception e) {
//			Log.e(TAG, "Error starting location services " + e.getMessage());
//			e.printStackTrace();
//			return false;
//		} 
//		Log.i(TAG, "startLocationUpdateService started");
//		return true;
//	}
	
	/**
	 * Establishes the initial state of the Tracker and sets up and starts the
	 * Location service. Called from onCreate().
	 * 
	 */
	private boolean doSetup() {
		Log.i(TAG, "doSetup()");
	    mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	    spEditor = mPreferences.edit();
		mProjId = mPreferences.getInt("PROJECT_ID", 0);	
		if (mProjId == 0) {
			Utils.showToast(this, "Aborting Tracker:\nDevice must be registered with a project.");
			return false;
		}

		updateState(IDLE);
		
		// TODO mpoints needs to be saved from one opening of the 
		// activity to another opening under the same service use.

		mSwath = DEFAULT_SWATH_WIDTH;
		return true;
	}
	
	// -------------------location and mapping methods----------------------
	
	/**
	 * Updates the Tracker's state and saves the state in SharedPreferences.  It is 
	 * important that this method be called with argument IDLE before exiting in order
	 *  to enable the Tracker menu when PositMain starts up again.
	 *  
	 * @param state an integer representation of the state (IDLE, PAUSED or RUNNING)
	 */
	private void updateState(int state) {
		mState = state;
	}
	
	/**
	 * Checks for network service, location provider, and that the phone's current
	 *  location can be determined, returning false if any of those fail.
	 * @return
	 */
	private boolean checkAllServices() {
		Log.i(TAG,"checkAllServices()");
		hasNetworkService = setNetworkType();
		if (!hasNetworkService)
			return false;
		else if (!(hasGpsService = setLocationProvider()))
			return false;
//		else if ((mLocation = setInitialLocation()) == null){
//			return false;
//		} 
		else {
			
			mLocation = setInitialLocation();
			return true;
		}
	}
	
	/**
	 * Sets the connectivity to WIFI or MOBILE or returns false.
	 * @return
	 */
	private boolean setNetworkType() {
		// Check for network connectivity
		mConnectivityMgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo info = mConnectivityMgr.getActiveNetworkInfo();
		if (info == null) {
			Log.e(TAG, "setNetworkType() unable to acquire CONNECTIVITY_SERVICE");
			Utils.showToast(this, "Aborting Tracker: No Active Network.\nYou must have WIFI or MOBILE enabled.");
			return false;
		}
		mNetworkType = info.getType();
		Log.i(TAG, "setNetworkType(), active network type = " + mConnectivityMgr.getActiveNetworkInfo().getTypeName());
		return true;
	}
	
	/**
	 * Sets the Find's location to the last known location and sets the
	 * provider to either GPS, if enabled, or network, if enabled.
	 */
	private boolean setLocationProvider() {
		Log.i(TAG, "setLocationProvider...()");
	
		// Check for Location service
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		List<String> providers = mLocationManager.getProviders(ENABLED_ONLY);
		mProvider = NO_PROVIDER;
		if (providers.contains(LocationManager.GPS_PROVIDER)) {
			mProvider = LocationManager.GPS_PROVIDER;
		} 
		else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
			mProvider = LocationManager.NETWORK_PROVIDER;
		}
		if (mProvider.equals(NO_PROVIDER)) {
			Utils.showToast(this, "Aborting Tracker: " +  
					NO_PROVIDER +  "\nYou must have GPS enabled. ");
			return false;
		}
		Log.i(TAG, "setLocationProvider()= " + mProvider);
		return true;
	}
	
	/**
	 * Updates the Tracker's View, including the MapView portion. 
	 */
	private void updateView() {
		mPoints = mPreferences.getInt("Points", 0);
		mExpeditionNumber = mPreferences.getInt("Expedition Num", -1);
		Log.i(TAG,"updateView(), mPoints = " + mPoints +"  " + mExpeditionNumber);
		mExpeditionTextView.setText(" "+mExpeditionNumber);
		String s = " Idle ";
		if (tracking) 
			s = " Running ";
		else
			s = " Idle ";
		String netStr = (mNetworkType == ConnectivityManager.TYPE_WIFI) ? " WIFI" : " MOBILE";
 		mStatusTextView.setText(s + " (GPS = " + mProvider 
				+ ", Ntwk = " + netStr + ")");
		mPointsTextView.setText("  " + mPoints);
		mSwathTextView.setText(" " + mSwath);
		mLocationTextView.setText(mLatitude + "," + mLongitude + "," + mAltitude);
		
		int lat = (int)(mLatitude * 1E6);
		int lon = (int)(mLongitude * 1E6);
		
		
		points.addOverlay(new OverlayItem(new GeoPoint(lat,lon),null,null));
		Log.i(TAG,"Point added");
		mapOverlays = mMapView.getOverlays();
		mapOverlays.add(points);
		
		

	
	
		mapController = mMapView.getController();
		mapController.animateTo(new GeoPoint(lat,lon));
		mapController.stopAnimation(false);

	}
	
	/**
	 * Sets the location to the phone's last known location.
	 * @return
	 */
	private Location setInitialLocation() {
		Log.i(TAG, "setInitialLocation() " + mProvider);
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		mLocation = mLocationManager.getLastKnownLocation(mProvider);
		if (mLocation != null) {
			mLongitude = mLocation.getLongitude();
			mLatitude = mLocation.getLatitude();
			mAltitude = mLocation.getAltitude();
			Log.i(TAG, "Location= " + mLatitude + "," + mLongitude);
			
			int lat = (int)(mLatitude * 1E6);
			int lon = (int)(mLongitude * 1E6);
			mapController.setCenter(new GeoPoint(lat,lon));
		} else {
			Log.e(TAG, "Location= NULL" + mLatitude + "," + mLongitude);
//			Utils.showToast(this,"Aborting Tracker:\nUnable to obtain current location");
		}
		return mLocation;
	}
	
	/**
	 * Sends a message to the update handler with either the current location or 
	 *  the last known location. This method is called with null on its first 
	 *  invocation.  
	 *  
	 * @param location is either null or the current location
	 */
	private void setCurrentGpsLocation(Location location) {
		if (location == null) {
		}
		if (Utils.debug) Log.i(TAG, "setCurrentGpsLocation , Provider = |" + mProvider + "|");

		try {
			mLongitude = mLocation.getLongitude();
			mLatitude = mLocation.getLatitude();
			mAltitude = mLocation.getAltitude();


			updateView();
		} catch (NullPointerException e) {
				Log.e(TAG, e.toString());
				e.printStackTrace();
		}
	}
	/**
	 * Sends a message to the update handler with either the current location or 
	 *  the last known location. This method is called with null on its first 
	 *  invocation.  
	 *  
	 * @param location is either null or the current location
	 */
	private void setCurrentGpsLocation() {
		if (Utils.debug) Log.i(TAG, "setCurrentGpsLocation , Provider = |" + mProvider + "|");

		try {
			mLongitude = mPreferences.getFloat("Longitude",0);
			mLatitude = mPreferences.getFloat("Latitude",0);
			mAltitude = mPreferences.getFloat("Altitude",0);


		} catch (NullPointerException e) {
				Log.e(TAG, e.toString());
				e.printStackTrace();
		}
	}
	
	@Override
	public void onDestroy()
	{
		Log.i(TAG, "tracking on destroy:" + tracking);
		mDbHelper.close();
		super.onDestroy();
	}
	
	/* (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#onResume()
	 */
	@Override
	protected void onResume() {
		Log.i(TAG,"onResume()");
		// TODO Auto-generated method stub
		super.onResume();
		updateView();
	}
	
//	// ------------------------ LocationListener Methods ---------------------------------//
//
//	/**
//	 * Invoked by the location service when phone's location changes.
//	 */
//	public void onLocationChanged(Location newLocation) {
//		setCurrentGpsLocation(newLocation);  	
//	}
//	/**
//	 * Resets the GPS location whenever the provider is enabled.
//	 */
//	public void onProviderEnabled(String provider) {
//		setCurrentGpsLocation(null);  	
//	}
//	/**
//	 * Resets the GPS location whenever the provider is disabled.
//	 */
//	public void onProviderDisabled(String provider) {
//		//		Log.i(TAG, provider + " disabled");
//		setCurrentGpsLocation(null);  	
//	}
//	/**
//	 * Resets the GPS location whenever the provider status changes. We
//	 * don't care about the details.
//	 */
//	public void onStatusChanged(String provider, int status, Bundle extras) {
//		setCurrentGpsLocation(null);  	
//	}
//
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}


	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.i(TAG, "Hey there " + key);
		if (key.equals("Expedition Num") || key.equals("Points")) {
			Log.i(TAG, "Hey there " + key);
			if (mPreferences.getInt("Expedition Num", -2) > -1) {
				setCurrentGpsLocation();
				updateView();
			}
		}

	}
}