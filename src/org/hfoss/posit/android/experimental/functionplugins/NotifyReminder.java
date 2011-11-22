package org.hfoss.posit.android.experimental.functionplugins;

import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.database.DbManager;
import org.hfoss.posit.android.experimental.api.service.LocationService;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

public class NotifyReminder extends OrmLiteBaseActivity<DbManager> {
	
	private static final String TAG = "NotifyReminder";
	private static final int NOTIFICATION_ID = 0;
	
	private Find find;
	
	private NotificationManager mNotificationManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.blank_screen);
		
		String ns = Context.NOTIFICATION_SERVICE;
		mNotificationManager = (NotificationManager) getSystemService(ns);
		
		// Initialize variables
		Bundle bundle = getIntent().getExtras();
		find = getHelper().getFindById(bundle.getInt(Find.ORM_ID));
		Log.i(TAG, Integer.toString(bundle.getInt(Find.ORM_ID)));

		showDialog(NOTIFICATION_ID);
		
	}
	
	// Creates the Date Picker and Alert Dialogs
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
	        
		case NOTIFICATION_ID:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Reminder: " + find.getName());
			builder.setMessage(find.getDescription());
			builder.setPositiveButton("Keep the find", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			builder.setNegativeButton("Discard the find", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					getHelper().delete(find);
					getApplicationContext().startService(new Intent(getApplicationContext(), LocationService.class));
				}
			});
			AlertDialog notification = builder.create();
			notification.setOnDismissListener(new DialogInterface.OnDismissListener() {
				public void onDismiss(DialogInterface dialog) {
					mNotificationManager.cancel(find.getId());
					finish();
				}
			});
	        return notification;
	    
		default:
			return null;
		
		}
	}
	
}
