package org.hfoss.posit.android.api;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.hfoss.posit.android.R;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class FindActivity extends OrmLiteBaseActivity<DbManager> // Activity
		implements OnClickListener, OnItemClickListener, LocationListener {

	private static final String TAG = "FindActivity";


	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.add_find);
		initializeListeners();	
	}
	
	/**
	 * Sets listeners for various UI elements.
	 */
	private void initializeListeners() {
		ImageButton saveButton = ((ImageButton)findViewById(R.id.idSaveButton));
		saveButton.setOnClickListener(this);
	}

	/**
	 * Retrieves values from the View fields and stores them in a Find instance.
	 * This method is invoked from the Save menu item. It also marks the find
	 * 'unsynced' so it will be updated to the server.
	 * 
	 * @return a new Find object with data from the view.
	 */
	private Find retrieveContentFromView() {
		Find find = new Find();

		EditText eText = (EditText) findViewById(R.id.nameText);
		String value = eText.getText().toString();
		find.setName(value);
		eText = (EditText) findViewById(R.id.descriptionText);
		value = eText.getText().toString();
		find.setDescription(value);
		eText = (EditText) findViewById(R.id.idText);
		value = eText.getText().toString();
		find.setGuid(value);

		// Latitude/longitude disabled until decided what to do
		TextView tView = (TextView) findViewById(R.id.longitudeText);
		value = tView.getText().toString();
		//find.setLongitude(Double.parseDouble(value));
		tView = (TextView) findViewById(R.id.latitudeText);
		value = tView.getText().toString();
		//find.setLatitude(Double.parseDouble(value));
		tView = (TextView) findViewById(R.id.timeText);
		value = tView.getText().toString();

		SimpleDateFormat formatter = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss");
		Date date = null;
		try {
			date = (Date) formatter.parse(value);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (date != null)
			find.setTime(date);

		// Mark the find unsynced TODO: Do we need this?
		find.setSynced(Find.NOT_SYNCED);

		return find;
	}

	public void onLocationChanged(Location arg0) {
		// TODO Auto-generated method stub

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
		case R.id.idSaveButton:
			Find find = retrieveContentFromView();
			int success = find.insert(this.getHelper().getFindDao());
			if (success>0)
				Log.i(TAG, "Find inserted successfully: " + find);
			else
				Log.e(TAG, "Find not inserted: " + find);
			break;
		}

	}
}
