package org.hfoss.posit.android.experimental.api.activity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.database.DbManager;
import org.hfoss.posit.android.experimental.plugin.FindPluginManager;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class FindActivity extends OrmLiteBaseActivity<DbManager> // Activity
		implements OnClickListener, OnItemClickListener, LocationListener {

	private static final String TAG = "FindActivity";
	private static final int CONFIRM_DELETE_DIALOG = 0;

	private LocationManager mLocationManager;
	private Location mCurrentLocation;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get the custom add find layout from the plugin settings, if there is
		// one.
		int resId = getResources().getIdentifier(FindPluginManager.mAddFindLayout, "layout", getPackageName());

		setContentView(resId);
		initializeListeners();
		Bundle extras = getIntent().getExtras();

		// Check for a new location every ten seconds while we're adding a new
		// find.
		mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
		String provider = mLocationManager.getBestProvider(criteria, true);
		if (provider != null)
			mLocationManager.requestLocationUpdates(provider, 10000, 0, this);
		else {
			Toast.makeText(this, "Unable to get a location via Wifi or GPS.  Are they enabled?", Toast.LENGTH_LONG)
					.show();
			Log.i(TAG, "Cannot request location updates, wifi or GPS might not be enabled/need a view of the sky");
		}

		if (extras != null) {
			if (getIntent().getAction().equals(Intent.ACTION_EDIT)) {
				Find find = getHelper().getFindById(extras.getInt(Find.ORM_ID));
				displayContentInView(find);
			}
		}
	}

	protected void onResume() {
		super.onResume();

		Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (lastKnownLocation == null)
			Toast.makeText(this, "Unable to retrieve last known location.", Toast.LENGTH_LONG).show();
		else
			mCurrentLocation = lastKnownLocation;
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
	 * Sets the location text views. Might be removing this..
	 * 
	 * @param location
	 */
	protected void setLocationTextViews(Location location) {
		TextView tView = (TextView) findViewById(R.id.longitudeValueTextView);
		tView.setText(String.valueOf(location.getLongitude()));
		tView = (TextView) findViewById(R.id.latitudeValueTextView);
		tView.setText(String.valueOf(location.getLatitude()));
	}

	/**
	 * Creates the menu for this activity by inflating a menu resource file.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.add_finds_menu, menu);
		if (getIntent().getAction().equals(Intent.ACTION_INSERT))
			menu.removeItem(R.id.delete_find_menu_item);
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
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.save_find_menu_item:
			saveFind();
			break;

		case R.id.delete_find_menu_item:
			showDialog(CONFIRM_DELETE_DIALOG);
			break;

		default:
			return false;
		}
		return true;
	} // onMenuItemSelected

	/**
	 * Retrieves values from the View fields and stores them in a Find instance.
	 * This method is invoked from the Save menu item. It also marks the find
	 * 'unsynced' so it will be updated to the server.
	 * 
	 * @return a new Find object with data from the view.
	 */
	protected Find retrieveContentFromView() {
		// Get the appropriate find class from the plugin manager and
		// make an instance of it.
		Class<Find> findClass = FindPluginManager.getInstance().getFindClass();
		Find find = null;
		String value = "";

		try {
			find = findClass.newInstance();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}

		EditText eText = (EditText) findViewById(R.id.guidEditText);
		if (eText != null) {
			value = eText.getText().toString();
			find.setGuid(value);
		}

		eText = (EditText) findViewById(R.id.nameEditText);
		if (eText != null) {
			value = eText.getText().toString();
			find.setName(value);
		}

		eText = (EditText) findViewById(R.id.descriptionEditText);
		if (eText != null) {
			value = eText.getText().toString();
			find.setDescription(value);
		}
		eText = (EditText) findViewById(R.id.guidEditText);
		if (eText != null) {
			value = eText.getText().toString();
			find.setGuid(value);
		}

		// Removing for now.. using "currentLocation" variable instead
		// TextView tView = (TextView) findViewById(R.id.latitudeValueTextView);
		// if (eText != null) {
		// value = tView.getText().toString();
		// find.setLatitude(Double.parseDouble(value));
		// }
		//
		// tView = (TextView) findViewById(R.id.longitudeValueTextView);
		// if (eText != null) {
		// value = tView.getText().toString();
		// find.setLongitude(Double.parseDouble(value));
		// }

		if (mCurrentLocation != null) {
			find.setLatitude(mCurrentLocation.getLatitude());
			find.setLongitude(mCurrentLocation.getLongitude());
		} else {
			find.setLatitude(0);
			find.setLongitude(0);
		}

		DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker);
		if (datePicker != null) {
			value = datePicker.getMonth() + "/" + datePicker.getDayOfMonth() + "/" + datePicker.getYear();
			SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
			Date date = null;
			try {
				date = (Date) formatter.parse(value);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			if (date != null)
				find.setTime(date);
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int projectId = prefs.getInt(getString(R.string.projectPref), 0);
		find.setProject_id(projectId);
		
		// Mark the find unsynced TODO: Do we need this?
		// find.setSynced(Find.NOT_SYNCED);

		return find;
	}

	/**
	 * Retrieves values from a Find object and puts them in the View.
	 * 
	 * @param a
	 *            Find object
	 */
	protected void displayContentInView(Find find) {
		EditText eText = (EditText) findViewById(R.id.nameEditText);
		eText.setText(find.getName());
		eText = (EditText) findViewById(R.id.descriptionEditText);
		eText.setText(find.getDescription());
		eText = (EditText) findViewById(R.id.guidEditText);
		eText.setText(find.getGuid());

		DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker);

		datePicker.init(find.getTime().getYear() + 1900, find.getTime().getMonth(), find.getTime().getDate(), null);

		TextView tView = (TextView) findViewById(R.id.longitudeValueTextView);
		tView.setText(String.valueOf(find.getLongitude()));

		tView = (TextView) findViewById(R.id.latitudeValueTextView);
		tView.setText(String.valueOf(find.getLatitude()));
	}

	/**
	 * When we get a fresh location, update our class variable..
	 */
	public void onLocationChanged(Location location) {
		mCurrentLocation = location;
		Log.i(TAG, "Got a new location: " + mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude());

	}

	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub

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
			return new AlertDialog.Builder(this).setIcon(R.drawable.alert_dialog_icon)
					.setTitle(R.string.alert_dialog_2)
					.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// User clicked OK so do some stuff
							if (deleteFind()) {
								Toast.makeText(FindActivity.this, R.string.deleted_from_database, Toast.LENGTH_SHORT)
										.show();
								finish();
							} else
								Toast.makeText(FindActivity.this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
						}
					}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
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

	protected boolean saveFind() {
		int rows = 0;
		Find find = retrieveContentFromView();
		// SharedPref
		// find.setProject_id()
		if (getIntent().getAction().equals(Intent.ACTION_INSERT))
			rows = getHelper().insert(find);
		else if (getIntent().getAction().equals(Intent.ACTION_EDIT)) {
			find.setId(getIntent().getExtras().getInt(Find.ORM_ID));
			rows = getHelper().update(find);
		} else
			rows = 0; // Something wrong with intent
		if (rows > 0) {
			Log.i(TAG, "Find inserted successfully: " + find);
		} else
			Log.e(TAG, "Find not inserted: " + find);
		return rows > 0;
	}

	protected boolean deleteFind() {
		int rows = 0;

		// Get the appropriate find class from the plugin manager and
		// make an instance of it.
		Class<Find> findClass = FindPluginManager.getInstance().getFindClass();
		Find find = null;

		try {
			find = findClass.newInstance();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}

		find.setId(getIntent().getExtras().getInt(Find.ORM_ID));
		rows = getHelper().delete(find);
		return rows > 0;

	}

	@Override
	public void finish() {
		Log.i(TAG, "onFinish()");
		mLocationManager.removeUpdates(this);
		mLocationManager = null;
		mCurrentLocation = null;
		super.finish();
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
			saveFind();
			finish();
			break;

		}

	}
}
