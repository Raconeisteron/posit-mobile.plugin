package org.hfoss.posit.android.experimental.api.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.database.DbHelper;
import org.hfoss.posit.android.experimental.api.database.DbManager;
import org.hfoss.posit.android.experimental.functionplugins.NotifyReminder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class LocationService extends Service implements LocationListener {
	
	private static final String TAG = "LocationService";
	private static final int ONE_MINUTE = 60 * 1000;
	
	private int projectID;
	// All finds with projectID that have reminders set
	private ArrayList<Find> reminderFinds;
	private LocationManager mLocationManager = null;
	private Location mCurrentLocation = null;
	
	private DbManager dbManager;
	
	private NotificationManager mNotificationManager;
		
	private MediaPlayer player;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean allowReminder = prefs.getBoolean("allowReminderKey", true);
		boolean allowGeoTag = prefs.getBoolean("geotagKey", true);
		
		if (allowReminder && allowGeoTag) {
			//Toast.makeText(this, "My Service Created!", Toast.LENGTH_SHORT).show();
			
			projectID = prefs.getInt(getString(R.string.projectPref), 0);
			
			reminderFinds = new ArrayList<Find>();
			
			mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, ONE_MINUTE, 0, this);
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, ONE_MINUTE, 0, this);
			
			Location netLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			Location gpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			
			if (gpsLocation != null) {
				mCurrentLocation = gpsLocation;
			} else {
				mCurrentLocation = netLocation;
			}
			
			String ns = Context.NOTIFICATION_SERVICE;
			mNotificationManager = (NotificationManager) getSystemService(ns);
			
			player = MediaPlayer.create(this, R.raw.braincandy);
			player.setLooping(true);
		} else {
			this.onDestroy();
		}
		
		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Toast.makeText(this, "My Service Started!", Toast.LENGTH_SHORT).show();
		
		// Start or stop this service based on preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean allowReminder = prefs.getBoolean("allowReminderKey", true);
		boolean allowGeoTag = prefs.getBoolean("geotagKey", true);
		
		if (!allowReminder || !allowGeoTag) {
			this.onDestroy();
			return START_STICKY;
		}
		
		Location netLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location gpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		if (gpsLocation != null) {
			mCurrentLocation = gpsLocation;
		} else {
			mCurrentLocation = netLocation;
		}
		
		dbManager = DbHelper.getDbManager(this);

		reminderFinds.clear();
		
		for (Find find : dbManager.getFindsByProjectId(projectID)) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			String time = dateFormat.format(find.getTime());
			if (time.substring(11).equals("00:00:00")){
				reminderFinds.add(find);
			}
		}
		
		if (reminderFinds.size() > 0) {
			player.start();
		} else {
			this.onDestroy();
			return START_STICKY;
		}

		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		//Toast.makeText(this, "My Service Stopped!", Toast.LENGTH_SHORT).show();
		
		mLocationManager.removeUpdates(this);
		
		if (dbManager != null) {
			DbHelper.releaseDbManager();
			dbManager = null;
		}
		
		mNotificationManager.cancelAll();
		
		player.stop();
		player = MediaPlayer.create(this, R.raw.braincandy);
		
	}

	public void onLocationChanged(Location location) {
		//Toast.makeText(this, "Location might have changed.", Toast.LENGTH_SHORT).show();
		
		if (isBetterLocation(location, mCurrentLocation)) {
			mCurrentLocation = location;
			//Toast.makeText(this, "A new location is used.", Toast.LENGTH_SHORT).show();
			Log.i(TAG, "Got a new location: " + mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude());
		}
		
		double currLong = mCurrentLocation.getLongitude();
		double currLat = mCurrentLocation.getLatitude();
		
		for (Find find : reminderFinds) {
			// If the reminder time is today
			Date findTime = find.getTime();
			Date currTime = new Date();
			if ((findTime.getYear() == currTime.getYear())
					&& (findTime.getMonth() == currTime.getMonth())
					&& (findTime.getDate() == currTime.getDate())) {
				// If the current location is within a radius of around 500 meters from reminder's location
				double findLong = find.getLongitude();
				double findLat = find.getLatitude();
				double longDiff = Math.abs(currLong - findLong);
				double latDiff = Math.abs(currLat - findLat);
//				Toast.makeText(this, find.getName() + " Diff = " + Double.toString(longDiff) + " "
//						+ Double.toString(latDiff), Toast.LENGTH_SHORT).show();
				if (longDiff <= 0.003 && latDiff <= 0.003) {
					// Set notification icon, name, and time
					int icon = android.R.drawable.stat_sys_warning;
					CharSequence tickerText = "Reminder: " + find.getName();
					long when = System.currentTimeMillis();
					Notification notification = new Notification(icon, tickerText, when);
					notification.defaults |= Notification.DEFAULT_SOUND;
					// Set intent and bundle
					Intent intent = new Intent(this, NotifyReminder.class);
					Bundle bundle = new Bundle();
					bundle.putInt(Find.ORM_ID, find.getId());
					intent.putExtras(bundle);
					// Set pending intent
					PendingIntent contentIntent = PendingIntent.getActivity(this, find.getId(), intent, 0);
					notification.setLatestEventInfo(this, "Reminder: " + find.getName(), find.getDescription(),
							contentIntent);
					//Toast.makeText(this, find.getName() + " Notify", Toast.LENGTH_SHORT).show();
					mNotificationManager.notify(find.getId(), notification);
				}
			}
		}
	}

	public void onProviderDisabled(String provider) {}

	public void onProviderEnabled(String provider) {}

	public void onStatusChanged(String provider, int status, Bundle extras) {}
	
	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	    if (currentBestLocation == null) {
	        // A new location is always better than no location
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > ONE_MINUTE * 2;
	    boolean isSignificantlyOlder = timeDelta < - ONE_MINUTE * 2;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
//	    	Toast.makeText(this, "The new location is very new.", Toast.LENGTH_SHORT).show();
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBestLocation.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
//	    	Toast.makeText(this, "Old accuracy: " + currentBestLocation.getAccuracy(), Toast.LENGTH_SHORT).show();
//	    	Toast.makeText(this, "New accuracy: " + location.getAccuracy(), Toast.LENGTH_SHORT).show();
//	    	Toast.makeText(this, "The new location is more accurate.", Toast.LENGTH_SHORT).show();
	        return true;
	    } else if (isNewer && !isLessAccurate) {
//	    	Toast.makeText(this, "New time - old time: " + timeDelta / ONE_MINUTE, Toast.LENGTH_SHORT).show();
//	    	Toast.makeText(this, "Old accuracy: " + currentBestLocation.getAccuracy(), Toast.LENGTH_SHORT).show();
//	    	Toast.makeText(this, "New accuracy: " + location.getAccuracy(), Toast.LENGTH_SHORT).show();
//	    	Toast.makeText(this, "The new location is new and not less accurate.", Toast.LENGTH_SHORT).show();
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
//	    	Toast.makeText(this, "A new location is new but less accurate.", Toast.LENGTH_SHORT).show();
	        return true;
	    }
	    return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
	
}
