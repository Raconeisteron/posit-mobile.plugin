/*
 * File: AcdiVocaFindActivity.java
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
package org.hfoss.posit.android.plugin.acdivoca;

import java.util.Locale;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.FindActivity;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.gsm.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.DatePicker.OnDateChangedListener;

/**
 * Handles Finds for AcdiVoca Mobile App.
 * 
 */
public class AcdiVocaFindActivity extends FindActivity implements OnDateChangedListener {
	public static final String TAG = "AcdiVocaAddActivity";

	public static boolean SAVE_CHECK = true;

	private String mAction = "";
	private int mFindId = 0;
	private AcdiVocaDbHelper mDbHelper;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Create DB helper
		mDbHelper = new AcdiVocaDbHelper(this);

		//ContentValues communes = mDbHelper.fetchAllCommunes();
		
		//SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_dropdown_item, cursor_Names, columns, to);
		//Spinner communeSpinner = (Spinner) findViewById(R.id.commune);
		//communeSpinner.setAdapter(mAdapter);

		
//		setContentView(R.layout.acdivoca_add);
//		setContentView(R.layout.acdivoca_add_full);
//		((Button)findViewById(R.id.saveToDB)).setOnClickListener(this);
//		((Button)findViewById(R.id.sendSMS)).setOnClickListener(this);

//		final Intent intent = getIntent();
//		mAction = intent.getAction();
//		if (mAction.equals(Intent.ACTION_EDIT)) {
//			doEditAction();
//		}
	}

	/**
	 * Inflates the Apps menus from a resource file.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.acdivoca_menu_add, menu);
		return true;
	}

	/**
	 * Implements the requested action when user selects a menu item.
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		//		if(item.getItemId()==R.id.refresh) {
		//			((EditText)findViewById(R.id.beneficiary)).setText("");
		//			((EditText)findViewById(R.id.firstname)).setText("");
		//			((DatePicker)findViewById(R.id.datepicker)).init(2011, 1, 1, (OnDateChangedListener) this);
		//			((RadioButton)findViewById(R.id.female)).setChecked(true);
		//			((RadioButton)findViewById(R.id.male)).setChecked(false);
		//		}
		//		if (item.getItemId()==R.id.delete_finds_menu_item) {
		//			Log.i(TAG,"Deleting a find");
		//			showDialog(CONFIRM_DELETE_DIALOG);
		//		}
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	/**
	 * 
	 */
	@Override
	protected void onResume() {
		super.onResume();
		String localePref = PreferenceManager.getDefaultSharedPreferences(this).getString("locale", "");
		Log.i(TAG, "Locale = " + localePref);
		Locale locale = new Locale(localePref); 
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
		getBaseContext().getResources().updateConfiguration(config, null);
//		setContentView(R.layout.acdivoca_add);  // Should be done after configuration

		setContentView(R.layout.acdivoca_add_full);  // Should be done after configuration
		((Button)findViewById(R.id.saveToDbButton)).setOnClickListener(this);
		((Button)findViewById(R.id.sendSmsButton)).setOnClickListener(this);
		
		final Intent intent = getIntent();
		mAction = intent.getAction();
		if (mAction.equals(Intent.ACTION_EDIT)) {
			doEditAction();
		}
	}

	/**
	 * Allows editing of editable data for existing finds.  For existing finds, 
	 * we retrieve the Find's data from the DB and display it in a TextView. The
	 * Find's location and time stamp are not updated.
	 */
	private void doEditAction() {

		mFindId = (int) getIntent().getLongExtra(AcdiVocaDbHelper.FINDS_ID, 0); 
		Log.i(TAG,"Find id = " + mFindId);

		ContentValues values = AcdiVocaFindDataManager.getInstance().fetchFindDataById(this, mFindId, null);
		displayContentInView(values);						
	}


	/**
	 * Retrieves values from the View fields and stores them as <key,value> pairs in a ContentValues.
	 * This method is invoked from the Save menu item.  It also marks the find 'unsynced'
	 * so it will be updated to the server.
	 * @return The ContentValues hash table.
	 */
	private ContentValues retrieveContentFromView() {
		ContentValues result = new ContentValues();

		EditText eText = (EditText) findViewById(R.id.lastnameEdit);
		String value = eText.getText().toString();
		result.put(AcdiVocaDbHelper.FINDS_LASTNAME, value);
		Log.i(TAG, "retrieve LAST NAME = " + value);
		
		eText = (EditText)findViewById(R.id.firstnameEdit);
		value = eText.getText().toString();
		result.put(AcdiVocaDbHelper.FINDS_FIRSTNAME, value);
		
		eText = (EditText)findViewById(R.id.ageEdit);
		value = eText.getText().toString();
		result.put(AcdiVocaDbHelper.FINDS_AGE, value);
		
		//value = mMonth + "/" + mDay + "/" + mYear;
		value = ((DatePicker)findViewById(R.id.datepicker)).getMonth() + "/" +
			((DatePicker)findViewById(R.id.datepicker)).getDayOfMonth() + "/" +
			((DatePicker)findViewById(R.id.datepicker)).getYear();
		//Log.i(TAG, "retrieve DOB=" + value);
		result.put(AcdiVocaDbHelper.FINDS_DOB, value);

		RadioButton sexRB = (RadioButton)findViewById(R.id.femaleRadio);
		String sex = "";
		if (sexRB.isChecked()) 
			sex = "FEMALE";
		else 
			sex = "MALE";
		result.put(AcdiVocaDbHelper.FINDS_SEX, sex);         
		
		eText = (EditText)findViewById(R.id.addressEdit);
		value = eText.getText().toString();
		result.put(AcdiVocaDbHelper.FINDS_ADDRESS, value);
		
		eText = (EditText)findViewById(R.id.inhomeEdit);
		value = eText.getText().toString();
		result.put(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE,value);
		
		Spinner communeSpinner = (Spinner)findViewById(R.id.communeSpinner);
		value = (String)communeSpinner.getSelectedItem();
		result.put(AcdiVocaDbHelper.COMMUNE_NAME, value);
		
		communeSpinner = (Spinner)findViewById(R.id.commune_sectionSpinner);
		value = (String)communeSpinner.getSelectedItem();
		result.put(AcdiVocaDbHelper.COMMUNE_SECTION_NAME, value);
		
		RadioButton rb = (RadioButton)findViewById(R.id.malnourishedRadio);
		String infant = "";
		if (rb.isChecked()) 
			infant = "MALNOURISHED";
		else 
			infant = "PREVENTION";
		result.put(AcdiVocaDbHelper.FINDS_INFANT_CATEGORY, infant);

		rb = (RadioButton)findViewById(R.id.expectingRadio);
		String mother = "";
		if (rb.isChecked()) 
			mother = "EXPECTING";
		else 
			mother = "NURSING";
		result.put(AcdiVocaDbHelper.FINDS_MOTHER_CATEGORY, mother);
		
		Spinner spinner = (Spinner)findViewById(R.id.communeSpinner);
		String commune = (String) spinner.getSelectedItem();
		result.put(AcdiVocaDbHelper.COMMUNE_NAME, commune);
		
		spinner = (Spinner)findViewById(R.id.commune_sectionSpinner);
		String communeSection = (String) spinner.getSelectedItem();
		result.put(AcdiVocaDbHelper.COMMUNE_SECTION_NAME, communeSection);		
		return result;
	}

	/**
	 * Displays values from a ContentValues in the View.
	 * @param contentValues stores <key, value> pairs
	 */
	private void displayContentInView(ContentValues contentValues) {
		EditText eText = (EditText) findViewById(R.id.lastnameEdit);
		eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_LASTNAME));

		eText = (EditText) findViewById(R.id.firstnameEdit);
		eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_FIRSTNAME));
		Log.i(TAG,"display First Name = " + contentValues.getAsString(AcdiVocaDbHelper.FINDS_FIRSTNAME));

		eText = (EditText)findViewById(R.id.ageEdit);
		eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_AGE));
		
		eText = (EditText)findViewById(R.id.addressEdit);
		eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_ADDRESS));
		
		eText = (EditText)findViewById(R.id.inhomeEdit);
		eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE));
		
		DatePicker dp = (DatePicker) findViewById(R.id.datepicker);
		String date = contentValues.getAsString(AcdiVocaDbHelper.FINDS_DOB);
		Log.i(TAG,"display DOB = " + date);
		dp.init(Integer.parseInt(date.substring(date.lastIndexOf("/")+1)), 
				Integer.parseInt(date.substring(0,date.indexOf("/"))),
				Integer.parseInt(date.substring(date.indexOf("/")+1,date.lastIndexOf("/"))),
				(OnDateChangedListener) this);

		RadioButton sexRB = (RadioButton)findViewById(R.id.femaleRadio);
		Log.i(TAG, "sex=" + contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX));
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX).equals("FEMALE"))
			sexRB.setChecked(true);
		else {
			sexRB = (RadioButton)findViewById(R.id.maleRadio);
			sexRB.setChecked(true);
		}
		
		RadioButton motherRB = (RadioButton) findViewById(R.id.expectingRadio);
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_MOTHER_CATEGORY).equals("EXPECTING"))
			motherRB.setChecked(true);
		else {
			motherRB = (RadioButton)findViewById(R.id.nursingRadio);
			motherRB.setChecked(true);
		}

		RadioButton infantRB = (RadioButton) findViewById(R.id.malnourishedRadio);
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_INFANT_CATEGORY).equals("MALNOURISHED"))
			infantRB.setChecked(true);
		else {
			infantRB = (RadioButton)findViewById(R.id.inpreventionRadio);
			infantRB.setChecked(true);
		}
		
		Spinner spinner = (Spinner)findViewById(R.id.communeSpinner);
		String selected = contentValues.getAsString(AcdiVocaDbHelper.COMMUNE_NAME);
		int k = 0;
		String item = (String) spinner.getItemAtPosition(k);
		while (k < spinner.getCount() && !selected.equals(item)) {
			++k;
			item = (String) spinner.getItemAtPosition(k);
		}
		spinner.setSelection(k);

		spinner = (Spinner)findViewById(R.id.commune_sectionSpinner);
		selected = contentValues.getAsString(AcdiVocaDbHelper.COMMUNE_SECTION_NAME);
		k = 0;
		item = (String) spinner.getItemAtPosition(k);
		while (k < spinner.getCount() && !selected.equals(item)) {
			++k;
			item = (String) spinner.getItemAtPosition(k);
		}
		spinner.setSelection(k);
		
	}

	/**
	 * Required as part of OnClickListener interface. Handles button clicks.
	 */
	public void onClick(View v) {
		if(v.getId()==R.id.saveToDbButton) {
			boolean result = false;
			ContentValues data = this.retrieveContentFromView(); 
			Log.i(TAG,"View Content: " + data.toString());
			data.put(AcdiVocaDbHelper.FINDS_PROJECT_ID, 0);
			if (mAction.equals(Intent.ACTION_EDIT)) {
				result = AcdiVocaFindDataManager.getInstance().updateFind(this, mFindId, data);
				Log.i(TAG, "Update to Db is " + result);
			} else {
				result = AcdiVocaFindDataManager.getInstance().addNewFind(this, data);
				Log.i(TAG, "Save to Db is " + result);
			}
			if (result)
				Toast.makeText(this, "Find saved to Db", Toast.LENGTH_SHORT).show();
			else 
				Toast.makeText(this, "Db error", Toast.LENGTH_SHORT).show();
			finish();
		}
		if(v.getId()==R.id.sendSmsButton) { 
			ContentValues values = retrieveContentFromView();

	        String message = AcdiVocaSmsManager.formatSmsMessage(values);
	        
			AcdiVocaSmsManager.sendMessage(this,message,null);
			//Toast.makeText(this, "Sending: " +  message, Toast.LENGTH_SHORT).show();
			Log.i(TAG,  "Sending: " +  message);
	        finish();
		}
	}

	/**
	 * Required as part of OnItemClickListener interface. Handles clicks on, e.g., images. 
	 */
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub	
	}	

	/**
	 * The following methods are part of the OnLocationChangedListener interface.
	 */
	public void onLocationChanged(Location newLocation) {	}
	public void onProviderDisabled(String arg0) {	}
	public void onProviderEnabled(String arg0) {	}
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {	}

	public void onDateChanged(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		// TODO Auto-generated method stub
		
	}
}