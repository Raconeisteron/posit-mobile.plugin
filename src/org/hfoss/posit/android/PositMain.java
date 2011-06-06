/*
 * File: PositMain.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
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


import java.util.Locale;

import org.hfoss.posit.android.api.FindActivityProvider;
import org.hfoss.posit.android.api.FindPluginManager;
import org.hfoss.posit.android.api.SettingsActivity;
import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaAdminActivity;
import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaDbHelper;
import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaNewAgriActivity;
import org.hfoss.posit.android.plugin.acdivoca.LoginActivity;
import org.hfoss.posit.android.provider.PositDbHelper;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Implements the main activity and the main screen for the POSIT application.
 */
public class PositMain extends Activity implements OnClickListener { //,RWGConstants {

	private static final String TAG = "PositMain";

	private static final int CONFIRM_EXIT = 0;

	public static final int LOGIN_CANCELED = 3;
	public static final int LOGIN_SUCCESSFUL = 4;
	private static final int REGISTRATION_ACTIVITY = 11;


	private SharedPreferences mSharedPrefs;
	private Editor mSpEditor;

	private String mAuthKey;
	public static WifiManager wifiManager;
	public Intent rwg;
	
	NotificationManager mNotificationManager;

	/**
	 * Called when the activity is first created. Sets the UI layout, adds the
	 * buttons, checks whether the phone is registered with a POSIT server.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG,"Creating");

		// A newly installed POSIT should have no shared prefs
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mSpEditor = mSharedPrefs.edit();
		Log.i(TAG, "Preferences= " + mSharedPrefs.getAll().toString());

		// If this is a new install, we need to set up the Server
		if (mSharedPrefs.getString("SERVER_ADDRESS", "").equals("")) {
			mSpEditor.putString("SERVER_ADDRESS", getString(R.string.defaultServer));
			mSpEditor.commit();
		}

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		// initialize plugins
		FindPluginManager.initInstance(this);
		
		// Run login activity, if necessary
		
		Intent intent = new Intent();
		Class<Activity> loginActivity = FindActivityProvider.getLoginActivityClass();
		if (loginActivity != null) {
			intent.setClass(this, loginActivity);
			intent.putExtra(AcdiVocaDbHelper.USER_TYPE_STRING, AcdiVocaDbHelper.UserType.USER.ordinal());
			this.startActivityForResult(intent, LoginActivity.ACTION_LOGIN);
		}

//		// Give the user the tutorial if they haven't yet had it. 
//		if (!mSharedPrefs.getBoolean("tutorialComplete", false)) {
//			Intent i = new Intent(this, TutorialActivity.class);
//			startActivity(i);
//		} else { 
//			startPOSIT();
//		}

//		Toast.makeText(this, "Server: "  + mSharedPrefs.getString("SERVER_ADDRESS", ""), Toast.LENGTH_SHORT).show();
	}


	/**
	 * When POSIT starts it should either display a Registration View, if the 
	 * phone is not registered with a POSIT server, or it should display the 
	 * main View (ListFinds, AddFinds).  This helper method is called in various
	 * places in the Android, including in onCreate() and onRestart(). 
	 */
	private void startPOSIT() {
		// If the phone has no valid AUTH_KEY, it has to be registered with a server 

//		mAuthKey = mSharedPrefs.getString("AUTHKEY", "");
//		if (mAuthKey.equals("") || mAuthKey.equals(null)) {
//			Intent i = new Intent(this, RegisterActivity.class);
//			startActivityForResult(i, REGISTRATION_ACTIVITY);
//			
//		} else {    // Otherwise display the PositMain View
			setContentView(R.layout.main);
			
			if (FindPluginManager.mMainIcon != null) {
				final ImageView mainLogo = (ImageView) findViewById(R.id.Logo);
				int resID = getResources().getIdentifier(FindPluginManager.mMainIcon, "drawable", "org.hfoss.posit.android");
				mainLogo.setImageResource(resID);
			}
			
			if (FindPluginManager.mAddButtonLabel != null) {
				//final ImageButton addFindButton = (ImageButton) findViewById(R.id.addFindButton);
				final Button addFindButton = (Button)findViewById(R.id.addFindButton);
				int resid = this.getResources().getIdentifier(FindPluginManager.mAddButtonLabel, "string", "org.hfoss.posit.android");

				addFindButton.setText(resid);
				if (addFindButton != null)
					addFindButton.setOnClickListener(this);
			}

			if (FindPluginManager.mListButtonLabel != null) {
//				final ImageButton listFindButton = (ImageButton) findViewById(R.id.listFindButton);
				final Button listFindButton = (Button) findViewById(R.id.listFindButton);
				int resid = this.getResources().getIdentifier(FindPluginManager.mListButtonLabel, "string", "org.hfoss.posit.android");
				listFindButton.setText(resid);
				if (listFindButton != null) {
					listFindButton.setOnClickListener(this);
				}
			}
			
			if (FindPluginManager.mExtraButtonLabel != null) {
				final Button extraButton = (Button) findViewById(R.id.extraButton);
				int resid = this.getResources().getIdentifier(FindPluginManager.mExtraButtonLabel, "string", "org.hfoss.posit.android");
				extraButton.setText(resid);
				extraButton.setVisibility(View.VISIBLE);
				if (extraButton != null) {
					extraButton.setOnClickListener(this);
				}
			}

			if (FindPluginManager.mExtraButtonLabel2 != null) {
				final Button extraButton = (Button) findViewById(R.id.extraButton2);
				int resid = this.getResources().getIdentifier(FindPluginManager.mExtraButtonLabel2, "string", "org.hfoss.posit.android");
				extraButton.setText(resid);
				extraButton.setVisibility(View.VISIBLE);
				if (extraButton != null) {
					extraButton.setOnClickListener(this);
				}
			}
			
			//((Button)findViewById(R.id.extraButton2)).setOnClickListener(this);
			
			final TextView version = (TextView) findViewById(R.id.version);
			try {
				version.setText(getPackageManager().getPackageInfo("org.hfoss.posit.android", 0).versionName);
			} catch(NameNotFoundException nnfe) {
				//shouldn't happen
				Log.w(TAG, nnfe.toString(), nnfe);
				version.setVisibility(View.INVISIBLE);
			}
//		}
	}

	// Lifecycle methods just generate Log entries to help debug and understand flow

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG,"Pausing");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG,"Resuming");
		String localePref = PreferenceManager.getDefaultSharedPreferences(this).getString("locale", "");
		Log.i(TAG, "Locale = " + localePref);
		Locale locale = new Locale(localePref); 
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
		getBaseContext().getResources().updateConfiguration(config, null);
		
		startPOSIT();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG,"Starting");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i(TAG,"Restarting");
//		startPOSIT();
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG,"Stopping");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG,"Destroying");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG,"onActivityResult Result from registration = " + resultCode);
		switch (requestCode) {
		//		case LOGIN_ACTIVITY:
		//			if (resultCode == LOGIN_CANCELED)
		//				finish();
		//			else if (resultCode == LOGIN_SUCCESSFUL) {
		//				Intent intent = new Intent(this, ShowProjectsActivity.class);
		//				startActivity(intent);
		//			}
		//			break;
		//		case REGISTRATION_CANCELLED:
		//			finish();
		//			break;
		case REGISTRATION_ACTIVITY:
			//			if (resultCode == RegisterActivity.CANCELLED)
			if (resultCode == LOGIN_CANCELED) {
				Log.i(TAG,"Login canceled");
				finish();
			}
		case LoginActivity.ACTION_LOGIN:
			if (resultCode == RESULT_OK) {
				Toast.makeText(this, "Thank you", Toast.LENGTH_LONG).show();
				break;
			} else {
				Toast.makeText(this, "Sorry. Incorrect username or password.", Toast.LENGTH_LONG).show();
				finish();
			} 
		
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	/**
	 * Handles clicks on PositMain's buttons.
	 */
	public void onClick(View view) {
//		// Make sure the user has chosen a project before trying to add finds
//		SharedPreferences sp = PreferenceManager
//		.getDefaultSharedPreferences(this);
//		if (sp.getString("PROJECT_NAME", "").equals("")) {
//			Toast.makeText(this, "To get started, you must choose a project.", Toast.LENGTH_SHORT).show();
//			Intent i = new Intent(this, ShowProjectsActivity.class);
//			startActivity(i);
//		} else {
			Intent intent = new Intent();

			switch (view.getId()) {
			case R.id.addFindButton:
				intent.setClass(this, FindActivityProvider.getFindActivityClass());
				intent.setAction(Intent.ACTION_INSERT);
				startActivity(intent);
				break;
			case R.id.listFindButton:
				intent.setClass(this, FindActivityProvider.getListFindsActivityClass());
				startActivity(intent);
				break;

			case R.id.extraButton:
				intent.setClass(this, FindActivityProvider.getExtraActivityClass());
				startActivity(intent);
				break;	

			case R.id.extraButton2:
				intent.setAction(Intent.ACTION_INSERT);
				intent.setClass(this, FindActivityProvider.getExtraActivityClass2());
				startActivity(intent);
				break;			
			}
//		}
	}

	/**
	 * Creates the menu options for the PositMain screen. Menu items are
	 * inflated from a resource file.
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.positmain_menu, menu);
		return true;
	}

	/**
	 * Updates the RWG Start/End menus based on whether RWG is running or not.   
	 * 
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Manages the selection of menu items.
	 * 
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings_menu_item:
			startActivity(new Intent(this, SettingsActivity.class));
			break;
		case R.id.about_menu_item:
			startActivity(new Intent(this, AboutActivity.class));
			break;
		case R.id.admin_menu_item:
			startActivity(new Intent(this, AcdiVocaAdminActivity.class));
			break;
//		case R.id.projects_menu_item:
//			startActivity(new Intent(this, ShowProjectsActivity.class));
//			break;
		}
		
		return true;
	}

	
	/**
	 * Intercepts the back key (KEYCODE_BACK) and displays a confirmation dialog
	 * when the user tries to exit POSIT.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			showDialog(CONFIRM_EXIT);
			return true;
		}
		Log.i("code", keyCode+"");
		return super.onKeyDown(keyCode, event);
	}


	/**
	 * Creates a dialog to confirm that the user wants to exit POSIT.
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONFIRM_EXIT:
			return new AlertDialog.Builder(this).setIcon(
					R.drawable.alert_dialog_icon).setTitle(R.string.exit)
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// User clicked OK so do some stuff
							finish();
						}
					}).setNegativeButton(R.string.alert_dialog_cancel,
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							/* User clicked Cancel so do nothing */
						}
					}).create();

		default:
			return null;
		}
	}
 
	/**
	 * Makes sure RWG is stopped before exiting the Activity
	 * 
	 * @see android.app.Activity#finish()
	 */
	@Override
	public void finish() {
		Log.i(TAG, "finish()");
		super.finish();
	}
}