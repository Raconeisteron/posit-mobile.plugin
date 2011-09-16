package org.hfoss.posit.android.api.activity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.plugin.FindPluginManager;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
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

	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.add_find);
		initializeListeners();
		Bundle extras = getIntent().getExtras();
		
		// Check for a new location once a minute.
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
		
		if (extras != null) {
			if (getIntent().getAction().equals(Intent.ACTION_EDIT)) {
				Find find = getHelper().getFindById(extras.getInt(Find.GUID));
				displayContentInView(find);
			}
		}
	}

	protected void onResume() {
		super.onResume();
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

		DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker);
		if (datePicker != null) {
			value = datePicker.getMonth() + "/" + datePicker.getDayOfMonth()
					+ "/" + datePicker.getYear();
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

		// Mark the find unsynced TODO: Do we need this?
		find.setSynced(Find.NOT_SYNCED);

		return find;
	}

	/**
	 * Retrieves values from a Find objectand puts them in the View.
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
		datePicker.init(find.getTime().getYear(), find.getTime().getMonth(),
				find.getTime().getDay(), null);

		TextView tView = (TextView) findViewById(R.id.longitudeValueTextView);
		tView.setText(String.valueOf(find.getLongitude()));

		tView = (TextView) findViewById(R.id.latitudeValueTextView);
		tView.setText(String.valueOf(find.getLatitude()));
	}

	/**
	 * When we get a fresh location, throw that data in some text fields.
	 */
	public void onLocationChanged(Location location) {
		Toast.makeText(this, "Got a location!" + location, 10000);
		TextView tView = (TextView) findViewById(R.id.longitudeValueTextView);
		tView.setText(String.valueOf(location.getLongitude()));
		tView = (TextView) findViewById(R.id.latitudeValueTextView);
		tView.setText(String.valueOf(location.getLatitude()));
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

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.saveButton:
			int success = 0;
			Find find = retrieveContentFromView();
			success = find.insert(this.getHelper().getFindDao());
			if (success > 0)
				Log.i(TAG, "Find inserted successfully: " + find);
			else
				Log.e(TAG, "Find not inserted: " + find);
			break;
		}

	}
}
