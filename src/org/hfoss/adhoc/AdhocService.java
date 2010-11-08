package org.hfoss.adhoc;

import org.hfoss.posit.android.ListFindsActivity;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.TrackerActivity;
import org.hfoss.posit.android.TrackerState;
import org.hfoss.posit.android.utilities.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class AdhocService extends Service {
	protected static final String TAG = "AdhocService";
	public static final String MAC_ADDRESS = "Mac Address";

	public static final int ADHOC_NOTIFICATION = 1;
	public static final int NEWFIND_NOTIFICATION = 2;
	
	private static final int START_STICKY = 1;

	public static AdhocService adhocInstance = null;

	private NotificationManager mNotificationManager;
	private Notification mNotification;
	
	private int mPort = 4959;
	private Adhoc adhoc;
	private boolean stopped = false;
	private String mMacAddress = "";
	
	public static MacAddress getMacAddress(Context cxt){
		WifiManager wifi = (WifiManager) cxt.getSystemService(Context.WIFI_SERVICE);
		if (!wifi.isWifiEnabled()){
			return null;
		}
		WifiInfo info = wifi.getConnectionInfo();
		return new MacAddress(info.getMacAddress());
		
	}

//	private final IAdhocService.Stub mBinder = new IAdhocService.Stub() {
//
//		public int getPort() {
//			return mPort;
//		}
//	};
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		adhoc = Adhoc.getInstance(this);
		adhocInstance = this;
		
		Thread listenerThread = new Thread(){
			@Override
			public void run() {
				Looper.prepare();
				Log.i(TAG, "Starting the listening thread");
				adhoc.listen();
			}
		};
		listenerThread.start();


		Thread senderThread = new Thread(){
			public void run() {
				Looper.prepare();
				Log.i(TAG, "Starting the sending thread");
				adhoc.sendData();
			};
		};
		senderThread.start();
	}
	
	// This is the old onStart method that will be called on the pre-2.0
	// platform.  On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
	    handleCommand(intent);
		Log.i(TAG, "AdhocService,  Started, id " + startId);
	}

	/**
	 * Replaces onStart(Intent, int) in pre 2.2 versions.
	 * @param intent
	 * @param flags
	 * @param startId
	 * @return
	 */
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleCommand(intent);
		Log.i(TAG, "AdhocService,  Started, id " + startId);

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}
	
	/**
	 * Handles the start up of the service for both 2.2 and pre-2.2 verisons.
	 * @param intent
	 */
	private void handleCommand(Intent intent) {
		if (intent != null) {
			mMacAddress = intent.getStringExtra(AdhocService.MAC_ADDRESS);
			Log.d(TAG, "handleCommand(), macAddress = " + mMacAddress);
		} else  {
			Log.e(TAG, "handleCommand(), found a null intent");
		}
		if (adhoc != null)
			adhoc.setMacAddress(mMacAddress);
		notifyAdhocOn();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		adhoc.stopListening();
		adhocInstance = null;
		mNotificationManager.cancel(ADHOC_NOTIFICATION);  // Remove notifications
    	mNotificationManager.cancel(AdhocService.NEWFIND_NOTIFICATION);
		Log.d(TAG,"Destroyed Adhoc service");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
//		if (IAdhocService.class.getName().equals(intent.getAction())) {
//			return mBinder;
//		}
		return null;
	}
 
	/**
	 * Posts a message and an icon in the status bar.
	 */
	public void notifyAdhocOn() {
		//int icon = R.drawable.notification_icon;        // icon from resources
		CharSequence tickerText = "Ad-hoc Mode On";              // ticker-text
		long when = System.currentTimeMillis();         // notification time
		Context context = getApplicationContext();      // application Context
		CharSequence contentTitle = "Ad-hoc mode";  // expanded message title

		Intent notificationIntent = new Intent(this, ListFindsActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		// the next two lines initialize the Notification, using the configurations above
		mNotification = new Notification(R.drawable.ic_menu_share, tickerText, when);
		mNotification.setLatestEventInfo(context, contentTitle, "Adhoc is running", contentIntent);
		mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
		mNotification.flags |= Notification.FLAG_NO_CLEAR;
		mNotificationManager.notify(ADHOC_NOTIFICATION, mNotification);
	}

}
