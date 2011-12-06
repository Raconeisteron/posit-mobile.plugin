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
package org.hfoss.posit.android.experimental;

import java.util.ArrayList;

import org.hfoss.posit.android.experimental.api.LocaleManager;
import org.hfoss.posit.android.experimental.api.User;
import org.hfoss.posit.android.experimental.api.activity.ListProjectsActivity;
//import org.hfoss.posit.android.experimental.api.activity.LoginActivity;
import org.hfoss.posit.android.experimental.api.activity.MapFindsActivity;
import org.hfoss.posit.android.experimental.api.activity.SettingsActivity;
import org.hfoss.posit.android.experimental.api.database.DbManager;
import org.hfoss.posit.android.experimental.api.service.LocationService;
import org.hfoss.posit.android.experimental.functionplugins.tracker.TrackerDbManager;
import org.hfoss.posit.android.experimental.plugin.FindActivityProvider;
import org.hfoss.posit.android.experimental.plugin.FindPluginManager;
import org.hfoss.posit.android.experimental.plugin.FunctionPlugin;
import org.hfoss.posit.android.experimental.plugin.acdivoca.AcdiVocaFind;
import org.hfoss.posit.android.experimental.plugin.acdivoca.AttributeManager;
import org.hfoss.posit.android.experimental.plugin.Plugin;
import org.hfoss.posit.android.experimental.sync.Communicator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

/**
 * Implements the main activity and the main screen for the POSIT application.
 */
public class PositMain extends OrmLiteBaseActivity<DbManager> implements android.view.View.OnClickListener {

	// extends Activity implements OnClickListener { //,RWGConstants {

	private static final String TAG = "PositMain";

	private static final int CONFIRM_EXIT = 0;

//	public static final int LOGIN_CANCELED = 3;
//	public static final int LOGIN_SUCCESSFUL = 4;

	private SharedPreferences mSharedPrefs;
	private Editor mSpEditor;
	
	//private boolean mMainMenuExtensionPointEnabled = false;
	private ArrayList<FunctionPlugin> mMainMenuPlugins = null;
	private FunctionPlugin mMainLoginPlugin = null;
	/* Function Button Begins */
	private ArrayList<FunctionPlugin> mMainButtonPlugins = null;
	/* Function Button Ends */
	/* All Services Begins */
	private ArrayList<Class<Service>> mServices = null;
	/* All Services Ends */
	

	/**
	 * Called when the activity is first created. Sets the UI layout, adds the
	 * buttons, checks whether the phone is registered with a POSIT server.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "Creating");

		// Initialize plugins and managers
		FindPluginManager.initInstance(this);
		mMainMenuPlugins = FindPluginManager.getFunctionPlugins(FindPluginManager.MAIN_MENU_EXTENSION);
		Log.i(TAG, "# main menu plugins = " + mMainMenuPlugins.size());
		mMainLoginPlugin = FindPluginManager.getFunctionPlugin(FindPluginManager.MAIN_LOGIN_EXTENSION);	

		// NOTE: This is AcdiVoca stuff and should be put in a plugin
		// AcdiVocaSmsManager.initInstance(this);
		AttributeManager.init();

		// NOTE: Not sure if this is the best way to do this -- perhaps these kinds of prefs
		//  should go in the plugins_preferences.xml
		// A newly installed POSIT should have no shared prefs. Set the default phone pref if
		// it is not already set.
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		try {
			String phone = mSharedPrefs.getString(getString(R.string.smsPhoneKey), "");
			if (phone.equals("")) {
				mSpEditor = mSharedPrefs.edit();
				mSpEditor.putString(getString(R.string.smsPhoneKey), getString(R.string.default_phone));
				mSpEditor.commit();
			}
			String server = mSharedPrefs.getString(getString(R.string.serverPref), "");
			if (server.equals("")) {
				mSpEditor = mSharedPrefs.edit();
				mSpEditor.putString(getString(R.string.serverPref), getString(R.string.defaultServer));
				mSpEditor.commit();
			}
			Log.i(TAG, "Preferences= " + mSharedPrefs.getAll().toString());
		} catch (ClassCastException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}

		
		// Login Extension Point
		// Run login plugin, if necessary
		
		if (mMainLoginPlugin != null) {
			Intent intent = new Intent();
			Class<Activity> loginActivity = mMainLoginPlugin.getActivity();
			intent.setClass(this, loginActivity);
			intent.putExtra(User.USER_TYPE_STRING, User.UserType.USER.ordinal());
			Log.i(TAG, "Starting login activity for result");
			if (mMainLoginPlugin.getActivityReturnsResult()) 
				this.startActivityForResult(intent, mMainLoginPlugin.getActivityResultAction());
			else
				this.startActivity(intent);
		}
		
		/* All Services Begins */
		mServices = FindPluginManager.getAllServices();
		for (Class<Service> s : mServices) {
			this.startService(new Intent(this, s));
		}
		/* All Services Ends */
		
	}

	/**
	 * When POSIT starts it should either display a Registration View, if the
	 * phone is not registered with a POSIT server, or it should display the
	 * main View (ListFinds, AddFinds). This helper method is called in various
	 * places in the Android, including in onCreate() and onRestart().
	 */
	private void startPOSIT() {
		setContentView(R.layout.main);

		// Change visibility of buttons based on UserType

		// Log.i(TAG, "POSIT Start, distrStage = " +
		// AppControlManager.displayDistributionStage(this));

		if (FindPluginManager.mFindPlugin.mMainIcon != null) {
			final ImageView mainLogo = (ImageView) findViewById(R.id.Logo);
			int resID = getResources().getIdentifier(FindPluginManager.mFindPlugin.mMainIcon, "drawable", this.getPackageName());
			mainLogo.setImageResource(resID);
		}

		// New Find Button
		if (FindPluginManager.mFindPlugin.mAddButtonLabel != null) {
			final Button addFindButton = (Button)findViewById(R.id.addFindButton);
			//final ImageButton addFindButton = (ImageButton) findViewById(R.id.addFindButton);
			int resid = this.getResources()
					.getIdentifier(FindPluginManager.mFindPlugin.mAddButtonLabel, "string", getPackageName());

			if (addFindButton != null) {
				addFindButton.setTag(resid);
				addFindButton.setText(resid);
				addFindButton.setOnClickListener(this);
			}
		}

		// View Finds Button
		if (FindPluginManager.mFindPlugin.mListButtonLabel != null) {
			final Button listFindButton = (Button) findViewById(R.id.listFindButton);
			//final ImageButton listFindButton = (ImageButton) findViewById(R.id.listFindButton);
			int resid = this.getResources().getIdentifier(FindPluginManager.mFindPlugin.mListButtonLabel, "string",
					getPackageName());
			if (listFindButton != null) {
				listFindButton.setTag(resid);
				listFindButton.setText(resid);
				listFindButton.setOnClickListener(this);
			}
		}
		
		// Extra function plugin buttons
		mMainButtonPlugins = FindPluginManager.getFunctionPlugins(FindPluginManager.MAIN_BUTTON_EXTENSION);
		
		for (FunctionPlugin plugin : mMainButtonPlugins) {
			int buttonID = getResources().getIdentifier(plugin.getName(), "id", getPackageName());
			Button button = (Button) findViewById(buttonID);
//			int iconID = getResources().getIdentifier(plugin.getmMenuIcon(), "drawable", getPackageName());
//			ImageButton button = (ImageButton) findViewById(buttonID);
//			button.setImageResource(iconID);
			button.setVisibility(Button.VISIBLE);
			button.setOnClickListener(this);
		}
	}

	// Lifecycle methods just generate Log entries to help debug and understand
	// flow

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "Pausing");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "Resuming");

		LocaleManager.setDefaultLocale(this); // Locale Manager should
														// be in API
		startPOSIT();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "Starting");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i(TAG, "Restarting");
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG, "Stopping");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Destroying");
	}

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "onActivityResult resultcode = " + resultCode);
		
		// Login Extension Point result
		if (mMainLoginPlugin != null && requestCode == mMainLoginPlugin.getActivityResultAction()) {
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this, getString(R.string.toast_thankyou), Toast.LENGTH_SHORT).show();
			} else {
				finish();				
			}
		} else 
			super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Handles clicks on PositMain's buttons.
	 */
	public void onClick(View view) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		
		String authKey = Communicator.getAuthKey(this);
		if (authKey == null) {
			Toast.makeText(this, "You must go to Android > Settings > Accounts & Sync to " +
					" set up an account before you use POSIT.", Toast.LENGTH_LONG).show();
			return;
		}
		
		if (sp.getString(getString(R.string.projectNamePref), "").equals("")) {
			Toast.makeText(this, "To get started, you must choose a project.", Toast.LENGTH_LONG).show();
			Intent i = new Intent(this, ListProjectsActivity.class);
			startActivity(i);
		} else {

			Intent intent = new Intent();

			switch (view.getId()) {
			case R.id.addFindButton:
				intent.setClass(this, FindActivityProvider.getFindActivityClass());
				intent.setAction(Intent.ACTION_INSERT);
				startActivity(intent);
				break;
			case R.id.listFindButton:
				intent = new Intent();
//				intent.setAction(Intent.ACTION_SEND);
				intent.setClass(this, FindActivityProvider.getListFindsActivityClass());
				startActivity(intent);
				break;
			/* Function Button Begins */
			default:
				for (FunctionPlugin plugin: mMainButtonPlugins) {
					int buttonID = getResources().getIdentifier(plugin.getName(), "id", getPackageName());
					if (view.getId() == buttonID) {
						intent = new Intent(this, plugin.getmMenuActivity());
						/* Add specific info based on each button */
						if (plugin.getmMenuTitle().equals("Extra Button")) {
							intent.setAction(Intent.ACTION_INSERT);
						} else if (plugin.getmMenuTitle().equals("Extra Button 2")) {
							intent.setAction(Intent.ACTION_SEND);
						}
						/* Start the Activity */
						if (plugin.getActivityReturnsResult())
							startActivityForResult(intent, plugin.getActivityResultAction());
						else
							startActivity(intent);
					}
				}
				break;
			}
		}
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
	 * Shows/Hides menus based on user type, SUPER, ADMIN, USER
	 * 
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		// Re-inflate to force localization.
		menu.clear();
		MenuInflater inflater = getMenuInflater();
//		if (mMainMenuExtensionPointEnabled){
		if (mMainMenuPlugins.size() > 0) {
			for (FunctionPlugin plugin: mMainMenuPlugins) {
				MenuItem item = menu.add(plugin.getmMenuTitle());
				int id = getResources().getIdentifier(
						plugin.getmMenuIcon(), "drawable", "org.hfoss.posit.android.experimental");
				item.setIcon(id);
			}
		}
		inflater.inflate(R.menu.positmain_menu, menu);

		MenuItem adminMenu = menu.findItem(R.id.admin_menu_item);

		// Log.i(TAG, "UserType = " + AppControlManager.getUserType());
		// Log.i(TAG, "distribution stage = " +
		// AppControlManager.getDistributionStage());
		// // Hide the ADMIN menu from regular users
		// if (AppControlManager.isRegularUser() ||
		// AppControlManager.isAgriUser())
		// adminMenu.setVisible(false);
		// else
		// adminMenu.setVisible(true);

		return super.onPrepareOptionsMenu(menu);

	}

	/**
	 * Manages the selection of menu items.
	 * 
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Log.i(TAG, "onMenuItemSelected " + item.toString());
		
		String authKey = Communicator.getAuthKey(this);
		if (authKey == null) {
			Toast.makeText(this, "You must go to Android > Settings > Accounts & Sync to " +
					" set up an account before you use POSIT.", Toast.LENGTH_LONG).show();
			return false;
		}

		switch (item.getItemId()) {
		case R.id.settings_menu_item:
			startActivity(new Intent(this, SettingsActivity.class));
			break;
		case R.id.map_finds_menu_item:
			startActivity(new Intent(this, MapFindsActivity.class));
			break;
		// case R.id.admin_menu_item:
		// startActivity(new Intent(this, AcdiVocaAdminActivity.class));
		// break;
		case R.id.about_menu_item:
			startActivity(new Intent(this, AboutActivity.class));
			break;
			
		default:
			if (mMainMenuPlugins.size() > 0){
				for (FunctionPlugin plugin: mMainMenuPlugins) {
					if (item.getTitle().equals(plugin.getmMenuTitle()))
						startActivity(new Intent(this, plugin.getmMenuActivity()));
				}
			
			}

			break;

		}

		return true;
	}

	/**
	 * Intercepts the back key (KEYCODE_BACK) and displays a confirmation dialog
	 * when the user tries to exit POSIT.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showDialog(CONFIRM_EXIT);
			return true;
		}
		Log.i("code", keyCode + "");
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Creates a dialog to confirm that the user wants to exit POSIT.
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONFIRM_EXIT:
			return new AlertDialog.Builder(this).setIcon(R.drawable.alert_dialog_icon).setTitle(R.string.exit)
					.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// User clicked OK so do some stuff
							finish();
						}
					}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							/* User clicked Cancel so do nothing */
						}
					}).create();

		default:
			return null;
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		AlertDialog d = (AlertDialog) dialog;
		Button needsabutton;
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		switch (id) {
		case CONFIRM_EXIT:
			d.setTitle(R.string.exit);
			// d.setButton(DialogInterface.BUTTON_POSITIVE,
			// getString(R.string.alert_dialog_ok), new
			// DialogInterface.OnClickListener() {
			// public void onClick(DialogInterface dialog,
			// int whichButton) {
			// // User clicked OK so do some stuff
			// finish();
			// }
			// } );
			// d.setButton(DialogInterface.BUTTON_NEGATIVE,
			// getString(R.string.alert_dialog_cancel), new
			// DialogInterface.OnClickListener() {
			// public void onClick(DialogInterface dialog,
			// int whichButton) {
			// /* User clicked Cancel so do nothing */
			// }
			// } );
			needsabutton = d.getButton(DialogInterface.BUTTON_POSITIVE);
			needsabutton.setText(R.string.alert_dialog_ok);
			needsabutton.invalidate();

			needsabutton = d.getButton(DialogInterface.BUTTON_NEGATIVE);
			needsabutton.setText(R.string.alert_dialog_cancel);
			needsabutton.invalidate();

			break;
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