/*
 * File: FindActivity.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool.
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
package org.hfoss.posit.android.api.activity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.LocaleManager;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.plugin.AddFindPluginCallback;
import org.hfoss.posit.android.api.plugin.FindPlugin;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.api.plugin.FunctionPlugin;
import org.hfoss.posit.android.api.plugin.csv.CsvListFindsActivity;
import org.hfoss.posit.android.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

public class FindActivity extends OrmLiteBaseActivity<DbManager> // Activity
		implements OnClickListener, OnItemClickListener, LocationListener {

	private static final String TAG = "FindActivity";
	private static final int CONFIRM_DELETE_DIALOG = 0;

	private boolean mGeoTagEnabled;
	private LocationManager mLocationManager = null;
	protected Location mCurrentLocation = null;

	// UI Elements
	private EditText mNameET = null;
	private EditText mDescriptionET = null;
	private TextView mGuidTV = null;
	private TextView mGuidRealTV = null;
	private TextView mTimeTV = null;
	private TextView mLatTV = null;
	private TextView mLatitudeTV = null;
	private TextView mLongTV = null;
	private TextView mLongitudeTV = null;
	private TextView mAdhocTV = null;


	private ArrayList<FunctionPlugin> mAddFindMenuPlugins = null;

	/**
	 * This may be invoked by a FindActivity subclass, which may or may not have
	 * latitude and longitude fields.
	 */
	@SuppressWarnings("static-access")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// Get the custom add find layout from the plugin settings, if there us one
		int resId  = getResources().getIdentifier(
				FindPluginManager.mFindPlugin.mAddFindLayout, "layout",
				getPackageName());
		setContentView(resId);
		
		// Sets listeners for various UI elements
		initializeListeners();

		mAddFindMenuPlugins = FindPluginManager
				.getFunctionPlugins(FindPluginManager.ADD_FIND_MENU_EXTENSION);

		// Initialize all UI elements for later uses
		mNameET = (EditText) findViewById(R.id.nameEditText);
		mDescriptionET = (EditText) findViewById(R.id.descriptionEditText);
		mGuidTV = (TextView) findViewById(R.id.guidValueTextView);
		mGuidRealTV = (TextView) findViewById(R.id.guidRealValueTextView);
		mTimeTV = (TextView) findViewById(R.id.timeValueTextView);
		mLatTV = (TextView) findViewById(R.id.latitudeTextView);
		mLatitudeTV = (TextView) findViewById(R.id.latitudeValueTextView);
		mLongTV = (TextView) findViewById(R.id.longitudeTextView);
		mLongitudeTV = (TextView) findViewById(R.id.longitudeValueTextView);
		mAdhocTV = (TextView) findViewById(R.id.isAdhocTextView);

		// Check if settings allow Geotagging
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		mGeoTagEnabled = prefs.getBoolean("geotagKey", true);

		// If enabled, get location manager and provider
		if (mGeoTagEnabled) {
			mLocationManager = (LocationManager) this
					.getSystemService(Context.LOCATION_SERVICE);
		}

		// Set the content of UI elements, either auto-generated or retrieved
		// from a Find
		Bundle extras = getIntent().getExtras();

		if (extras != null) {
			// Existing Find
			if (getIntent().getAction().equals(Intent.ACTION_EDIT)) { 
				int id = extras.getInt(Find.ORM_ID);
				Log.i(TAG, "ORM_id = " + id);
				Find find = getHelper().getFindById(id);
				displayContentInView(find);
			} else 
				
			// Bundled Find
			if (getIntent().getAction().equals(Intent.ACTION_INSERT_OR_EDIT)) {
				// Pull a Bundle corresponding to a Find from the Intent and put
				// that in the view
				Bundle findBundle = extras.getBundle("findbundle");
				Find find;
				try {
					FindPlugin plugin = FindPluginManager.mFindPlugin;
					if (plugin == null) {
						Log.e(TAG, "Could not retrieve Find Plugin.");
						Toast.makeText(this, "A fatal error occurred while trying to start FindActivity", 
								Toast.LENGTH_LONG).show();
						finish();
						return;
					}
					find = plugin.getmFindClass().newInstance();
				} catch (IllegalAccessException e) {
					Toast.makeText(this, "A fatal error occurred while trying to start FindActivity", 
							Toast.LENGTH_LONG).show();
					finish();
					return;
				} catch (InstantiationException e) {
					Toast.makeText(this, "A fatal error occurred while trying to start FindActivity", 
							Toast.LENGTH_LONG).show();
					finish();
					return;
				}
				find.updateObject(findBundle);
				displayContentInView(find);
			} else 
				
			// CSV Find	
			if (getIntent().getAction().equals(CsvListFindsActivity.ACTION_CSV_FINDS)) {
				
			}
		// New Find
		} else {  
			// Set real GUID
			if (mGuidRealTV != null)
				mGuidRealTV.setText(UUID.randomUUID().toString());
			// Set displayed GUID
			if (mGuidTV != null)
				mGuidTV.setText(mGuidRealTV.getText().toString()
						.substring(0, 8)
						+ "...");
			// Set Time
			if (mTimeTV != null) {
				SimpleDateFormat dateFormat = new SimpleDateFormat(
						"yyyy/MM/dd HH:mm:ss");
				Date date = new Date();
				mTimeTV.setText(dateFormat.format(date));
			}

			if (mGeoTagEnabled) {
				// Set Longitude and Latitude
				mLocationManager.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER, 60000, 0, this);
				mLocationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, 60000, 0, this);

				Location netLocation = mLocationManager
						.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				Location gpsLocation = mLocationManager
						.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

				if (gpsLocation != null) {
					mCurrentLocation = gpsLocation;
				} else {
					mCurrentLocation = netLocation;
				}

				if (mCurrentLocation == null) {
					Log.i(TAG, "Location issue, mCurrentLocation = "
							+ mCurrentLocation);
					if (mLongitudeTV != null)
						mLongitudeTV.setText("0.0");
					if (mLatitudeTV != null)
						mLatitudeTV.setText("0.0");
					// Toast.makeText(this, "Unable to retrieve GPS info." +
					// " Please make sure your Data or Wi-Fi is enabled.",
					// Toast.LENGTH_SHORT).show();
					// Log.i(TAG,
					// "Cannot request location updates; Data or Wifi might not be enabled.");
				} else {
					if (mLongitudeTV != null)
						mLongitudeTV.setText(String.valueOf(mCurrentLocation
								.getLongitude()));
					if (mLatitudeTV != null)
						mLatitudeTV.setText(String.valueOf(mCurrentLocation
								.getLatitude()));
				}
			} else {
				if (mLongitudeTV != null && mLongTV != null) {
					mLongitudeTV.setVisibility(TextView.INVISIBLE);
					mLongTV.setVisibility(TextView.INVISIBLE);
				}
				if (mLatitudeTV != null && mLatTV != null) {
					mLatitudeTV.setVisibility(TextView.INVISIBLE);
					mLatTV.setVisibility(TextView.INVISIBLE);
				}
			}
		}
	}
	
	
	/**
	 * Request Updates whenever the activity is paused.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		
		LocaleManager.setDefaultLocale(this); // Locale Manager should
		
		if (mGeoTagEnabled) { 
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 0, this);
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 0, this);
			
			Location netLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			Location gpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			
			if (gpsLocation != null) {
				mCurrentLocation = gpsLocation;
			} else {
				mCurrentLocation = netLocation;
			}

			if (mCurrentLocation == null) {
				Toast
						.makeText(
								this,
								"Unable to retrieve GPS info."
										+ " Please make sure your Data or Wi-Fi is enabled.",
								Toast.LENGTH_SHORT).show();
				Log
						.i(TAG,
								"Cannot request location updates; Data or Wifi might not be enabled.");
			}
		}

	}

	/**
	 * Remove Updates whenever the activity is paused.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		if (mLocationManager != null)
			mLocationManager.removeUpdates(this);
	}

	/**
	 * Remove Updates whenever the activity is finished.
	 */
	@Override
	public void finish() {
		Log.i(TAG, "onFinish()");
		if (mGeoTagEnabled)
			mLocationManager.removeUpdates(this);
		mLocationManager = null;
		mCurrentLocation = null;
		super.finish();
	}

	/**
	 * Sets listeners for various UI elements.
	 */
	protected void initializeListeners() {
		Button saveButton = ((Button) findViewById(R.id.saveButton));
		if (saveButton != null)
			saveButton.setOnClickListener(this);
	}

	/**
	 * Typical onClick stuff--shouldn't need to override anything here for the
	 * most basic functionality, but you can! (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.saveButton:
			if (saveFind()) {
				finish();
			}
			break;
		}
	}

	/**
	 * Creates the menu for this activity by inflating a menu resource file.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.add_finds_menu, menu);

		if (getIntent().getAction().equals(Intent.ACTION_INSERT)) {
			menu.removeItem(R.id.delete_find_menu_item);
		}

		// Add menu options based on function plug-ins
		for (FunctionPlugin plugin : mAddFindMenuPlugins) {
				// For all other function plug-ins
				MenuItem item = menu.add(plugin.getmMenuTitle());
				int resId = getResources().getIdentifier(plugin.getmMenuIcon(),
						"drawable", getPackageName());
				item.setIcon(resId);
		}

		return true;

	}

	/**
	 * Handles the various menu item actions.
	 * 
	 * @param featureId
	 *            is unused
	 * @param item
	 *            is the MenuItem selected by the user
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.save_find_menu_item:
			if (saveFind()) {
				finish();
			}
			break;

		case R.id.delete_find_menu_item:
			showDialog(CONFIRM_DELETE_DIALOG);
			break;

		default:
			for (FunctionPlugin plugin : mAddFindMenuPlugins) {
				if (item.getTitle().equals(plugin.getmMenuTitle())) {

					Intent intent = new Intent(this, plugin.getmMenuActivity());

					Log.i(TAG, "plugin=" + plugin);
					Class<AddFindPluginCallback> callbackClass = null;
					Object o;
					try {
						Find find = retrieveContentFromView();
						View view = ((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
						String className = plugin.getAddFindCallbackClass();
						if (className != null) {
							callbackClass = (Class<AddFindPluginCallback>) Class.forName(className);
							o = (AddFindPluginCallback) callbackClass.newInstance();
							((AddFindPluginCallback) o).menuItemSelectedCallback(
									this.getApplication(),
									find,
									view,
									intent);
						}
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InstantiationException e) {
						e.printStackTrace();
					}

					// Put Find information in Intent so that it may be
					// utilized by the plugin.
					// This is done by creating a find and then extracting
					// the ContentValues object from it
					// because I want to make sure that we have the same
					// behaviour as retrieveContentFromView()
					// without having to duplicate code.
					Find find = retrieveContentFromView();
					Bundle bundle = find.getDbEntries();
					intent.putExtra("DbEntries", bundle);
					if (plugin.getActivityReturnsResult())
						startActivityForResult(intent, plugin
								.getActivityResultAction());
					else
						startActivity(intent);
				}
			}
			break;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		// Go through all Function Plug-in to find the
		// one that matches the intent request code
		for (FunctionPlugin plugin : mAddFindMenuPlugins) {
			if (requestCode == plugin.getActivityResultAction()) {
				Log.i(TAG, "plugin=" + plugin);
				Class<AddFindPluginCallback> callbackClass = null;
				Object o;
				try {
					Find find = retrieveContentFromView();
					View view = ((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
					String className = plugin.getAddFindCallbackClass();
					if (className != null) {
						callbackClass = (Class<AddFindPluginCallback>) Class.forName(className);
						o = (AddFindPluginCallback) callbackClass.newInstance();
						((AddFindPluginCallback) o).onActivityResultCallback(
								this.getApplication(),
								find,
								view,
								intent);
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				}

//				if (plugin.getmMenuTitle().equals("Capture Media")) {
//					if (intent != null) {
//						// do we get an image back?
//						if (intent.getStringExtra("Photo") != null) {
//							img_str = intent.getStringExtra("Photo");
//							byte[] c = Base64.decode(img_str, Base64.DEFAULT);
//							Bitmap bmp = BitmapFactory.decodeByteArray(c, 0,
//									c.length);
//							photo.setImageBitmap(bmp);// display the retrieved
//							// image
//							photo.setVisibility(View.VISIBLE);
//						}
//					}
//				} 
//				else {
//					// Do something specific for other function plug-ins
//				}
			}
		}
	}

	/**
	 * Retrieves values from the View fields and stores them in a Find instance.
	 * This method is invoked from the Save menu item. It also marks the find
	 * 'unsynced' so it will be updated to the server.
	 * 
	 * @return a new Find object with data from the view.
	 */
	protected Find retrieveContentFromView() {

		// Get the appropriate find class from the plug-in
		// manager and make an instance of it
		Class<Find> findClass = FindPluginManager.mFindPlugin.getmFindClass();
		Find find = null;

		try {
			find = findClass.newInstance();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}

		// Set GUID
		// NOTE: Some derived finds may not have a GUID field. But the Guid must
		// be
		// set anyway because it used as the Find ID by the Posit server.
		if (mGuidRealTV != null) {
			find.setGuid(mGuidRealTV.getText().toString());
		} else {
			find.setGuid(UUID.randomUUID().toString());
		}

		// Set Time
		if (mTimeTV != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yyyy/MM/dd HH:mm:ss");
			String value = mTimeTV.getText().toString();
			if (value.length() == 10) {
				dateFormat = new SimpleDateFormat("yyyy/MM/dd");
			}
			try {
				find.setTime(dateFormat.parse(value));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		// Set Name
		if (mNameET != null) {
			find.setName(mNameET.getText().toString());
		}

		// Set Description
		if (mDescriptionET != null) {
			find.setDescription(mDescriptionET.getText().toString());
		}

		// Set Longitude and Latitude
		if (mLatitudeTV != null && mLongitudeTV != null) {
			if (mGeoTagEnabled) {
				find.setLatitude(Double.parseDouble(mLatitudeTV.getText()
						.toString()));
				find.setLongitude(Double.parseDouble(mLongitudeTV.getText()
						.toString()));
			} else {
				find.setLatitude(0);
				find.setLongitude(0);
			}
		}

		if (mAdhocTV != null) {
			find.setIs_adhoc(Integer.parseInt((String) mAdhocTV.getText()));
		}
		// Set Project ID
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		int projectId = prefs.getInt(getString(R.string.projectPref), 0);
		find.setProject_id(projectId);

		// Mark the find unsynced
		// find.setSynced(Find.NOT_SYNCED);

		return find;
	}

	/**
	 * Retrieves values from a Find object and puts them in the View.
	 * 
	 * @param a
	 *            Find object
	 */
	@SuppressWarnings("unchecked")
	protected void displayContentInView(Find find) {

		// Set real GUID
		if (mGuidRealTV != null) {
			mGuidRealTV.setText(find.getGuid());
		}

		// Set displayed GUID
		if (mGuidTV != null) {
			String id = mGuidRealTV.getText().toString();
			mGuidTV.setText(id.substring(0, Math.min(8, id.length())) + "...");
		}

		// Set Time
		if (mTimeTV != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yyyy/MM/dd HH:mm:ss");
			String time = dateFormat.format(find.getTime());
			mTimeTV.setText(time);
		}

		// Set Name
		if (mNameET != null) {
			mNameET.setText(find.getName());
		}

		// Set Description
		if (mDescriptionET != null) {
			mDescriptionET.setText(find.getDescription());
		}

		// Set Longitude and Latitude
		if (mLongitudeTV != null && mLatitudeTV != null) {
			mLongitudeTV.setText(String.valueOf(find.getLongitude()));
			mLatitudeTV.setText(String.valueOf(find.getLatitude()));
		}

		/**
		 * For each plugin, call its displayFindInViewCallback.
		 */
		for (FunctionPlugin plugin : mAddFindMenuPlugins) {
			Log.i(TAG, "plugin=" + plugin);
			Class<AddFindPluginCallback> callbackClass = null;
			Object o;
			try {
				View view = ((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
				String className = plugin.getAddFindCallbackClass();
				if (className != null) {
					callbackClass = (Class<AddFindPluginCallback>) Class.forName(className);
					o = (AddFindPluginCallback) callbackClass.newInstance();
					((AddFindPluginCallback) o).displayFindInViewCallback(
							this.getApplication(),
							find,
							view);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * When we get a fresh location, update our class variable..
	 */
	public void onLocationChanged(Location location) {
		if (isBetterLocation(location, mCurrentLocation)) {
			mCurrentLocation = location;
			Log.i(TAG, "Got a new location: " + mCurrentLocation.getLatitude()
					+ "," + mCurrentLocation.getLongitude());
		}
	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
	}

	/**
	 * This method is invoked by showDialog() when a dialog window is created.
	 * It displays the appropriate dialog box, currently a dialog to confirm
	 * that the user wants to delete this find and a dialog to warn user that a
	 * barcode has already been entered into the system
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONFIRM_DELETE_DIALOG:
			return new AlertDialog.Builder(this).setIcon(
					R.drawable.alert_dialog_icon).setTitle(
					R.string.alert_dialog_2).setPositiveButton(
					R.string.alert_dialog_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// User clicked OK so do some stuff
							if (deleteFind()) {
								finish();
							}
						}
					}).setNegativeButton(R.string.alert_dialog_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// User clicked cancel so do nothing
						}
					}).create();

			// case CONFIRM_EXIT:
			// Log.i(TAG, "CONFIRM_EXIT dialog");
			// return new AlertDialog.Builder(this)
			// .setIcon(R.drawable.alert_dialog_icon)
			// .setTitle(R.string.check_saving)
			// .setPositiveButton(R.string.save, new
			// DialogInterface.OnClickListener() {
			// public void onClick(DialogInterface dialog, int whichButton) {
			// Log.i(TAG, "CONFIRM_EXIT setOK onClick");
			// // User clicked OK so do some stuff
			// ContentValues contentValues = retrieveContentFromView();
			// doSave(contentValues);
			// }
			// })
			// .setNeutralButton(R.string.closing, new
			// DialogInterface.OnClickListener() {
			// public void onClick(DialogInterface dialog, int whichButton) {
			// Log.i(TAG, "CONFIRM_EXIT setNeutral onClick");
			// finish();
			// }
			// })
			// .setNegativeButton(R.string.alert_dialog_cancel, new
			// DialogInterface.OnClickListener() {
			// public void onClick(DialogInterface dialog, int whichButton) {
			// Log.i(TAG, "CONFIRM_EXIT setCancel onClick");
			// /* User clicked Cancel so do nothing */
			// }
			// })
			// .create();
		default:
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	protected boolean saveFind() {
		int rows = 0;
		Find find = retrieveContentFromView();
		prepareForSave(find);

		// A valid GUID is required
		if (!isValidGuid(find.getGuid())) {
			Toast.makeText(this, "You must provide a valid Id for this Find.",
					Toast.LENGTH_LONG).show();
			return false;
		}

		// A name is not always required in derived classes
		String name = find.getName();
		if (name != null && name.equals("")) {
			// if (find.getName().equals("")){
			Toast.makeText(this, "You must provide a name for this Find.",
					Toast.LENGTH_LONG).show();
			return false;
		}

		// Either create a new Find or update the existing Find
		if (getIntent().getAction().equals(Intent.ACTION_INSERT))
			rows = getHelper().insert(find);
		else if (getIntent().getAction().equals(Intent.ACTION_EDIT)) {
			find.setId(getIntent().getExtras().getInt(Find.ORM_ID));
			rows = getHelper().update(find);
		} else if (getIntent().getAction().equals(Intent.ACTION_INSERT_OR_EDIT)) {
			// Check if a Find with the same GUID already exists and update it if so
			Find sameguid = getHelper().getFindByGuid(find.getGuid());
			if (sameguid == null) {
				rows = getHelper().insert(find);
			} else {
				find.setId(sameguid.getId());
				rows = getHelper().update(find);
			}
		} else
			rows = 0; // Something wrong with intent
		
		if (rows > 0) {
			Log.i(TAG, "Find " + getIntent().getAction() + " successful: " + find);
		} else
			Log.e(TAG, "Find " + getIntent().getAction() + " not successful: " + find);

		/**
		 * For each plugin, call its displayFindInViewCallback.
		 */
		for (FunctionPlugin plugin : mAddFindMenuPlugins) {
			Log.i(TAG, "plugin=" + plugin);
			Class<AddFindPluginCallback> callbackClass = null;
			Object o;
			try {
				View view = ((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
				String className = plugin.getAddFindCallbackClass();
				if (className != null) {
					callbackClass = (Class<AddFindPluginCallback>) Class.forName(className);
					o = (AddFindPluginCallback) callbackClass.newInstance();
					((AddFindPluginCallback) o).afterSaveCallback(
							this.getApplication(),
							find,
							view,
							rows > 0);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			}
		}
		return rows > 0;
	}

	protected void prepareForSave(Find find) {
		// Stub : meant to be overridden in subclass
	}

	/**
	 * By default a Guid must not be the empty string. This method can be
	 * overridden in the plugin extension.
	 * 
	 * @param guid
	 * @return
	 */
	protected boolean isValidGuid(String guid) {
		return guid.length() != 0;
	}

	protected boolean deleteFind() {
		int rows = 0;
		String guid = null;
		// Get the appropriate find class from the plugin manager and
		// make an instance of it.
		Class<Find> findClass = FindPluginManager.mFindPlugin.getmFindClass();
		Find find = null;

		try {
			find = findClass.newInstance();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}

		find.setId(getIntent().getExtras().getInt(Find.ORM_ID));

		// store the guid of this find so that I can delete photos on phone
		find = getHelper().getFindById(find.getId());
		guid = find.getGuid();

		rows = getHelper().delete(find);

		if (rows > 0) {
			Toast.makeText(FindActivity.this, R.string.deleted_from_database,
					Toast.LENGTH_SHORT).show();

			// delete photo if it exists
			if (FindActivity.this.deleteFile(guid)) {
				Log.i(TAG, "Image with guid: " + guid + " deleted.");
			}

//			this.startService(new Intent(this, ToDoReminderService.class));
		} else {
			Toast.makeText(FindActivity.this, R.string.delete_failed,
					Toast.LENGTH_SHORT).show();
		}

		return rows > 0;

	}

	/*
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location The new Location that you want to evaluate
	 * 
	 * @param currentBestLocation The current Location fix, to which you want to
	 * compare the new one
	 */
	protected boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > 1000 * 60 * 2;
		boolean isSignificantlyOlder = timeDelta < -1000 * 60 * 2;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/* Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

}
