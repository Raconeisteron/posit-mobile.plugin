package org.hfoss.posit.android;

import java.util.List;

import org.hfoss.posit.android.provider.PositDbHelper;
import org.hfoss.posit.android.utilities.Utils;
import org.hfoss.posit.android.web.Communicator;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class TrackerService extends Service implements LocationListener {
	private static final int START_STICKY = 2;
	private static final long DEFAULT_SWATH_WIDTH = 50;
	private static final String TAG = "Tracker Service";
	private static final int UPDATE_LOCATION = 2;
	private static final String NO_PROVIDER = "No location service";
	private static final boolean ENABLED_ONLY = true;
	private static final int UPDATES_INTERVAL = 10000;// milliseconds
	private static final int MIN_DISTANCE_TRAVELED = 4; // meters
	private NotificationManager mNM;
	private ConnectivityManager mConnectivityMgr;
	private boolean hasNetworkService;
	private boolean hasGpsService;

	private SharedPreferences mPreferences;
	private SharedPreferences.Editor spEditor;

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();
	private LocationManager mLocationManager;
	private String mProvider = NO_PROVIDER;

	private int mProjId;
	protected int mExpeditionNumber;
	private Location mLocation;
	private double mLongitude = 0;
	private double mLatitude = 0;
	private double mAltitude = 0;
	private long mSwath = DEFAULT_SWATH_WIDTH;
	protected int mPoints;

	private Long mRowId;
	private PositDbHelper mDbHelper;

	private int mNetworkType;
	private Communicator mCommunicator;

	/**
	 * Handles GPS updates. The handleMessage() method is called whenever the
	 * setCurrentGPSLocation(Location) method has been passed a new location.
	 * This method originally called post(udateDisplay) to cause the
	 * updateDisplay() method to be run in the UI thread, rather than in the
	 * background. For now the tracker service does not interact with any UI,
	 * but UI is planned for the future. This is necessary because the
	 * background thread cannot access the View. The handler's post() method
	 * puts the updateDisplay() method in the queue, giving it a turn to run.
	 * 
	 * <P>
	 * Each time it is called it sends the GPS points to the server and updates
	 * the View.
	 * 
	 * TODO: Should it also store the point in the phone's memory?? TODO: Should
	 * it be possible to download and display an Expedition?? **** BOTH of these
	 * TODO's have been implemented *****
	 * 
	 * @see http://www.androidph.com/2009/02/app-10-beer-radar.html
	 */
	final Handler updateHandler = new Handler() {

		/** Gets called on every message that is received */
		// @Override
		public void handleMessage(Message msg) {
			mLocation = mLocationManager.getLastKnownLocation(mProvider);
			if (mLocation == null) {
				Log.e(TAG, "handleMessage(), Null location returned");

				return;
			}
			
			mLatitude = mLocation.getLatitude();
			mLongitude = mLocation.getLongitude();
			mAltitude = mLocation.getAltitude();

			spEditor.putFloat("Longitude", (float) mLongitude);
			spEditor.putFloat("Latitude", (float) mLatitude);
			spEditor.putFloat("Altitude", (float) mAltitude);
			spEditor.commit();
//			Log.i(TAG, result);
//			spEditor.putInt("Points", mPoints);
//			spEditor.commit();
			new SendExpeditionPointTask().execute(mLocation);

			switch (msg.what) {
			case UPDATE_LOCATION:
				break;
			}
			super.handleMessage(msg);
		}

	};

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		TrackerService getService() {
			return TrackerService.this;
		}
	}

	public void onCreate() {
		Debug.startMethodTracing("tracking");
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mDbHelper = new PositDbHelper(this);

		if (!doSetup()) {
			return;
		}

		else if (!checkAllServices()) {
			return;
		}

		else {
			Debug.startMethodTracing("tracking");
			mExpeditionNumber = registerExpedition();
			Debug.stopMethodTracing();
		}
		spEditor.putInt("Expedition Num", mExpeditionNumber);
		spEditor.commit();
		Log.i(TAG, "expedition num updated to " + mExpeditionNumber);
		// register expedition is followed by the addNewExpedition() method
		// to put the expedition into the phone's DB
		ContentValues result = new ContentValues();

		result.put(PositDbHelper.EXPEDITION_NUM, mExpeditionNumber);
		result.put(PositDbHelper.EXPEDITION_PROJECT_ID, mProjId);
		mRowId = mDbHelper.addNewExpedition(result);

		startLocationUpdateService(mProvider);
		spEditor.putBoolean("Tracking", true);
		spEditor.putInt("Points", 0);
		spEditor.commit();

		// Display a notification about us starting. We put an icon in the
		// status bar.
		showNotification();

	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("LocalService", "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
		mNM.cancel(R.string.local_service_label);
		mDbHelper.close();
		spEditor.putBoolean("Tracking", false);
		mLocationManager.removeUpdates(this); // stop location update service
		spEditor.commit();

		// Tell the user we stopped.
		Toast
				.makeText(this, R.string.local_service_stopped,
						Toast.LENGTH_SHORT).show();
		Debug.stopMethodTracing();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.local_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.radar, text,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		// LocalServiceActivities.Controller
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, CoverageTrackerActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this,
				getText(R.string.local_service_label), text, contentIntent);

		// Send the notification.
		// We use a layout id because it is a unique number. We use it later to
		// cancel.
		mNM.notify(R.string.local_service_label, notification);
	}

	/**
	 * Attempts to communicate with the server.
	 * 
	 * @return
	 */
	private int registerExpedition() {
		// mCommunicator = new Communicator(this);
		mCommunicator = new Communicator(this);
		return mCommunicator.registerExpeditionId(mProjId);
	}

	private boolean doSetup() {
		Log.i(TAG, "doSetup()");
		// mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		spEditor = mPreferences.edit();
		mProjId = mPreferences.getInt("PROJECT_ID", 0);
		if (mProjId == 0) {
			// Utils.showToast(this,
			// "Aborting Tracker:\nDevice must be registered with a project.");
			Utils
					.showToast(this,
							"Aborting Tracker:\nDevice must be registered with a project.");
			return false;
		}

		mPoints = 0;
		spEditor.commit();
		mSwath = DEFAULT_SWATH_WIDTH;
		return true;
	}

	/**
	 * Checks for network service, location provider, and that the phone's
	 * current location can be determined, returning false if any of those fail.
	 * 
	 * @return
	 */
	private boolean checkAllServices() {
		Log.i(TAG, "checkAllServices()");
		hasNetworkService = setNetworkType();
		if (!hasNetworkService)
			return false;
		else if (!(hasGpsService = setLocationProvider()))
			return false;
		// else if ((mLocation = setInitialLocation()) == null){
		// return false;
		// }
		else {
			mLocation = setInitialLocation();
			return true;
		}
	}

	/**
	 * Sets the connectivity to WIFI or MOBILE or returns false.
	 * 
	 * @return
	 */
	private boolean setNetworkType() {
		// Check for network connectivity
		// mConnectivityMgr = (ConnectivityManager)
		// getSystemService(CONNECTIVITY_SERVICE);
		mConnectivityMgr = (ConnectivityManager) this
				.getSystemService(this.CONNECTIVITY_SERVICE);
		NetworkInfo info = mConnectivityMgr.getActiveNetworkInfo();
		if (info == null) {
			Log.e(TAG,
					"setNetworkType() unable to acquire CONNECTIVITY_SERVICE");
			// Utils.showToast(this,
			// "Aborting Tracker: No Active Network.\nYou must have WIFI or MOBILE enabled.");
			Utils
					.showToast(this,
							"Aborting Tracker: No Active Network.\nYou must have WIFI or MOBILE enabled.");
			return false;
		}
		mNetworkType = info.getType();
		Log.i(TAG, "setNetworkType(), active network type = "
				+ mConnectivityMgr.getActiveNetworkInfo().getTypeName());
		return true;
	}

	private boolean setLocationProvider() {
		Log.i(TAG, "setLocationProvider...()");

		// Check for Location service
		// mLocationManager = (LocationManager)
		// getSystemService(LOCATION_SERVICE);
		mLocationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		List<String> providers = mLocationManager.getProviders(ENABLED_ONLY);
		mProvider = NO_PROVIDER;
		if (providers.contains(LocationManager.GPS_PROVIDER)) {
			mProvider = LocationManager.GPS_PROVIDER;

		} else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
			mProvider = LocationManager.NETWORK_PROVIDER;
		}
		if (mProvider.equals(NO_PROVIDER)) {
			// Utils.showToast(this, "Aborting Tracker: " +
			Utils.showToast(this, "Aborting Tracker: " + NO_PROVIDER
					+ "\nYou must have GPS enabled. ");
			return false;
		}
		Log.i(TAG, "setLocationProvider()= " + mProvider);
		return true;
	}

	/**
	 * Starts the update service unless there is no provider available.
	 */
	private boolean startLocationUpdateService(String provider) {
		Log.i(TAG, "startLocationUpdateService()");
		if (provider.equals(NO_PROVIDER))
			return false;

		try {
			mLocationManager.requestLocationUpdates(provider, UPDATES_INTERVAL,
					MIN_DISTANCE_TRAVELED, this);
		} catch (Exception e) {
			Log.e(TAG, "Error starting location services " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		Log.i(TAG, "startLocationUpdateService started");
		return true;
	}

	/**
	 * Sets the phone's last known location.
	 * 
	 * @return
	 */
	private Location setInitialLocation() {
		Log.i(TAG, "setInitialLocation() " + mProvider);
		// mLocationManager = (LocationManager)
		// getSystemService(LOCATION_SERVICE);
		mLocationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		mLocation = mLocationManager.getLastKnownLocation(mProvider);
		if (mLocation != null) {
			mLongitude = mLocation.getLongitude();
			mLatitude = mLocation.getLatitude();
			mAltitude = mLocation.getAltitude();
			Log.i(TAG, "Location= " + mLatitude + "," + mLongitude);
		} else {
			Log.e(TAG, "Location= NULL" + mLatitude + "," + mLongitude);
			// Utils.showToast(this,"Aborting Tracker:\nUnable to obtain current location");
		}
		return mLocation;
	}

	private void setCurrentGpsLocation(Location location) {
		if (location == null) {

			// if not passed a null location then nothign should be done
			return;
		}
		Log.i(TAG, "setCurrentGpsLocation , Provider = |" + mProvider + "|");
		mLocation = location;

		try {
			mLongitude = location.getLongitude();
			mLatitude = location.getLatitude();
			mAltitude = location.getAltitude();
			Message msg = Message.obtain();
			msg.what = UPDATE_LOCATION;
			if (Utils.debug)
				Log.i(TAG, "setCurrentGpsLocation msg= " + msg);
			updateHandler.sendMessage(msg);
		} catch (NullPointerException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		}
	}

	// ------------------------ LocationListener Methods
	// ---------------------------------//

	/**
	 * Invoked by the location service when phone's location changes.
	 */
	public void onLocationChanged(Location newLocation) {
		setCurrentGpsLocation(newLocation);
		Log.i(TAG, "point found");
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
		setCurrentGpsLocation(null);
	}

	private class SendExpeditionPointTask extends
		AsyncTask<Location, Void, Void> {

		@Override
		protected Void doInBackground(Location... location) {
			
			for (Location loc : location) {
				double latitude = loc.getLatitude();
				double longitude = loc.getLongitude();
				double altitude = loc.getAltitude();

				// Try to handle a change in network connectivity
				// We may lose a few points, but try not to crash
				try {
					mNetworkType = mConnectivityMgr.getActiveNetworkInfo()
							.getType();

					String result = mCommunicator.registerExpeditionPoint(
							latitude, longitude, altitude, mSwath,
							mExpeditionNumber);

					
					Log.i(TAG, result);
					mPoints++;
//					spEditor.putFloat("Longitude", (float) mLongitude);
//					spEditor.putFloat("Latitude", (float) mLatitude);
//					spEditor.putFloat("Altitude", (float) mAltitude);
//					spEditor.commit();
					spEditor.putInt("Points", mPoints);
					spEditor.commit();

					Log.i(TAG, result);
					// if the point successfully is sent to the server it is put
					// into the database using
					// content values
					ContentValues resultGPSPoint = new ContentValues();

					resultGPSPoint.put(
							PositDbHelper.EXPEDITION_GPS_POINT_ROW_ID, mRowId);
					resultGPSPoint.put(PositDbHelper.GPS_POINT_LATITUDE,
							mLatitude);
					resultGPSPoint.put(PositDbHelper.GPS_POINT_LONGITUDE,
							mLongitude);
					resultGPSPoint.put(PositDbHelper.GPS_POINT_ALTITUDE,
							mAltitude);
					resultGPSPoint.put(PositDbHelper.GPS_POINT_SWATH, mSwath);
					mDbHelper.addNewGPSPoint(resultGPSPoint);

					// Calls updateDisplay in the main (UI) thread to update the
					// View
					// The tracking thread cannot update the view directly

					Log.i(TAG, "handleMessage() " + mLongitude + " "
							+ mLatitude);

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