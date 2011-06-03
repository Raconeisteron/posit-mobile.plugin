/*
 * File: AcdiVocaFindActivity.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of the ACDI/VOCA plugin for POSIT, Portable Open Search 
 * and Identification Tool.
 *
 * This plugin is free software; you can redistribute it and/or modify
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

import java.util.Calendar;
import java.util.Locale;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.FindActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Handles Finds for AcdiVoca Mobile App.
 * 
 */
public class AcdiVocaNewAgriActivity extends FindActivity implements OnDateChangedListener, 
	TextWatcher, OnItemSelectedListener { //, OnKeyListener {
	public static final String TAG = "AcdiVocaAddActivity";

	private static final int CONFIRM_EXIT = 0;

	private boolean isProbablyEdited = false;   // Set to true if user edits a datum
	private String mAction = "";
	private int mFindId = 0;
	private AcdiVocaDbHelper mDbHelper;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 Log.i(TAG, "onCreate");

		// Create DB helper
		mDbHelper = new AcdiVocaDbHelper(this);
		isProbablyEdited = false;
	}

	/**
	 * Inflates the Apps menus from a resource file.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		 Log.i(TAG, "onCreateOptionsMenu");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.acdivoca_menu_add, menu);
		return true;
	}

	/**
	 * Implements the requested action when user selects a menu item.
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Log.i(TAG, "onMenuItemSelected");
		return true;
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		super.onPause();
	}

	class DbSimulator {
		
		private String[] db = {
				"AB-100-CD,Alicia,Morelli,1/6/1982,EXPECTIONG,2",
				"AB-101-CD,Baby,Morelli,8/6/2010,PREVENTION,9",
				"AB-102-CD,Baby,Jones,1/1/2011,PREVENTION,13" };
		
		public DbSimulator() {
		}
		
		public boolean addNewFind(ContentValues values) {
			return true;
		}
		
		public ContentValues fetchFindDataById(String id, ContentValues values) {
			ContentValues result = null;
			for (int k = 0; k < db.length; k++) {
				String[] vals = db[k].split(",");
				if (vals[0].equals(id)) {
					result = new ContentValues();
					result.put(AcdiVocaDbHelper.FINDS_FIRSTNAME, vals[1]);
					result.put(AcdiVocaDbHelper.FINDS_LASTNAME, vals[2]);
					result.put(AcdiVocaDbHelper.FINDS_DOB, vals[3]);
					result.put(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY_ID, vals[4]);
					result.put("MonthsRemaining", vals[5]);
				}
			}
			return result;
		}
	}

	
	/**
	 * 
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
		String localePref = PreferenceManager.getDefaultSharedPreferences(this).getString("locale", "");
		Log.i(TAG, "Locale = " + localePref);
		Locale locale = new Locale(localePref); 
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
		getBaseContext().getResources().updateConfiguration(config, null);

		Log.i(TAG, "Before edited = " + isProbablyEdited);
		setContentView(R.layout.acdivoca_agri_registration);  // Should be done after locale configuration

		((Button)findViewById(R.id.saveToDbButton)).setOnClickListener(this);
		((Button)findViewById(R.id.sendSmsButton)).setOnClickListener(this);
		
		// Listen for clicks on radio buttons
		 ((RadioButton)findViewById(R.id.femaleRadio)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.maleRadio)).setOnClickListener(this);
		 
		// Listen for clicks on check boxes
		 ((CheckBox)findViewById(R.id.farmerCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.musoCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.rancherCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.storeOwnerCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.fisherCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.otherCheckBox)).setOnClickListener(this);

		 ((CheckBox)findViewById(R.id.vegeCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.cerealCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.tuberCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.treeCheckBox)).setOnClickListener(this);
		 
		 ((CheckBox)findViewById(R.id.houeCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.piocheCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.brouetteCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.machetteCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.serpetteCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.pelleCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.barreAMinesCheckBox)).setOnClickListener(this);

		 // Listen for text changes in edit texts and set the isEdited flag
		 ((EditText)findViewById(R.id.firstnameEdit)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.lastnameEdit)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.addressEdit)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.inhomeEdit)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.amount_of_land)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.quantityEdit)).addTextChangedListener(this);
		 		 
		 // Initialize the DatePicker and listen for changes
		 Calendar calendar = Calendar.getInstance();
		 
		 ((DatePicker)findViewById(R.id.datepicker)).init(
				 calendar.get(Calendar.YEAR),
				 calendar.get(Calendar.MONTH), 
				 calendar.get(Calendar.DAY_OF_MONTH), this);
		 
		 //Spinner listeners
		 ((Spinner)findViewById(R.id.commune_sectionSpinner)).setOnItemSelectedListener(this);
		 ((Spinner)findViewById(R.id.communeSpinner)).setOnItemSelectedListener(this);
		 ((Spinner)findViewById(R.id.unitSpinner)).setOnItemSelectedListener(this);


		final Intent intent = getIntent();
		mAction = intent.getAction();
		if (mAction.equals(Intent.ACTION_EDIT)) {
			doEditAction();
			isProbablyEdited = false; // In EDIT mode, initialize after filling in data
		}
		 Log.i(TAG, "After edited = " + isProbablyEdited);
	}
	
	
	/**
	 * Allows editing of editable data for existing finds.  For existing finds, 
	 * we retrieve the Find's data from the DB and display it in a TextView. The
	 * Find's location and time stamp are not updated.
	 */
	private void doEditAction() {
		Log.i(TAG, "doEditAction");

		mFindId = (int) getIntent().getLongExtra(AcdiVocaDbHelper.FINDS_ID, 0); 
		Log.i(TAG,"Find id = " + mFindId);

		ContentValues values = AcdiVocaFindDataManager.getInstance().fetchFindDataById(this, mFindId, null);
		displayContentInView(values);						
	}


	/**
	 * Retrieves values from the View fields and stores them as <key,value> pairs in a ContentValues.
	 * This method is invoked from the Save menu item.  It also marks the find 'unsynced'
	 * so it will be updated to the server.
	 * multiple checkBox results will be stored as the addition of the binary summation.
	 * @return The ContentValues hash table.
	 */
	private ContentValues retrieveContentFromView() {
		Log.i(TAG, "retrieveContentFromView");
		ContentValues result = new ContentValues();

		EditText eText = (EditText) findViewById(R.id.lastnameEdit);
		String value = eText.getText().toString();
		result.put(AcdiVocaDbHelper.FINDS_LASTNAME, value);
		Log.i(TAG, "retrieve LAST NAME = " + value);
		
		eText = (EditText)findViewById(R.id.firstnameEdit);
		value = eText.getText().toString();
		result.put(AcdiVocaDbHelper.FINDS_FIRSTNAME, value);
		//AMOUNT OF LAND ADDED
		eText = (EditText)findViewById(R.id.amount_of_land);
		value = eText.getText().toString();
		result.put(AcdiVocaDbHelper.FINDS_LAND_AMOUNT, value);
		
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
		
		//Add beneficiary checkbox values
		CheckBox beneCB = (CheckBox)findViewById(R.id.farmerCheckBox);
		int beneCtg  = 0;
		if (beneCB.isChecked())
			beneCtg += Math.pow(2, 0);
		
		beneCB = (CheckBox)findViewById(R.id.musoCheckBox);
		if (beneCB.isChecked())
			beneCtg += Math.pow(2, 1);
		
		beneCB = (CheckBox)findViewById(R.id.rancherCheckBox);
		if (beneCB.isChecked())
			beneCtg += Math.pow(2, 2);
		
		beneCB = (CheckBox)findViewById(R.id.storeOwnerCheckBox);
		if (beneCB.isChecked())
			beneCtg += Math.pow(2, 3);
		
		beneCB = (CheckBox)findViewById(R.id.fisherCheckBox);
		if (beneCB.isChecked())
			beneCtg += Math.pow(2, 4);
		
		beneCB = (CheckBox)findViewById(R.id.otherCheckBox);
		if (beneCB.isChecked())
			beneCtg += Math.pow(2, 5);
		result.put(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY, beneCtg);
		
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
		
		// Add Seed types
		CheckBox seedCB = (CheckBox)findViewById(R.id.vegeCheckBox);
		int seedCtg  = 0;
		if (seedCB.isChecked())
			seedCtg += Math.pow(2, 0);
		
		seedCB = (CheckBox)findViewById(R.id.cerealCheckBox);
		if (seedCB.isChecked())
			seedCtg += Math.pow(2, 1);
		
		seedCB = (CheckBox)findViewById(R.id.tuberCheckBox);
		if (seedCB.isChecked())
			seedCtg += Math.pow(2, 2);
		
		seedCB = (CheckBox)findViewById(R.id.treeCheckBox);
		if (seedCB.isChecked())
			seedCtg += Math.pow(2, 3);
		result.put(AcdiVocaDbHelper.SEED_GROUP, seedCtg);

		// ADD TOOLS
		CheckBox toolCB = (CheckBox)findViewById(R.id.houeCheckBox);
		int toolCtg  = 0;
		if (toolCB.isChecked())
			toolCtg += Math.pow(2, 0);

		toolCB = (CheckBox)findViewById(R.id.piocheCheckBox);
		if (toolCB.isChecked())
			toolCtg += Math.pow(2, 1);
		
		toolCB = (CheckBox)findViewById(R.id.brouetteCheckBox);
		if (toolCB.isChecked())
			toolCtg += Math.pow(2, 2);
		
		toolCB = (CheckBox)findViewById(R.id.machetteCheckBox);
		if (toolCB.isChecked())
			toolCtg += Math.pow(2, 3);
		
		toolCB = (CheckBox)findViewById(R.id.serpetteCheckBox);
		if (toolCB.isChecked())
			toolCtg += Math.pow(2, 4);
		
		toolCB = (CheckBox)findViewById(R.id.pelleCheckBox);
		if (toolCB.isChecked())
			toolCtg += Math.pow(2, 5);
		
		toolCB = (CheckBox)findViewById(R.id.barreAMinesCheckBox);
		if (toolCB.isChecked())
			toolCtg += Math.pow(2, 6);		
		
		result.put(AcdiVocaDbHelper.FINDS_TOOL_CATAGORY, toolCtg);
		
		Spinner spinner = (Spinner)findViewById(R.id.communeSpinner);
		String commune = (String) spinner.getSelectedItem();
		result.put(AcdiVocaDbHelper.COMMUNE_NAME, commune);

		spinner = (Spinner)findViewById(R.id.unitSpinner);
		String unit = (String) spinner.getSelectedItem();
		result.put(AcdiVocaDbHelper.FINDS_UNIT, unit);
		
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
		Log.i(TAG, "displayContentInView");
		EditText eText = (EditText) findViewById(R.id.lastnameEdit);
		eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_LASTNAME));

		eText = (EditText) findViewById(R.id.firstnameEdit);
		eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_FIRSTNAME));
		Log.i(TAG,"display First Name = " + contentValues.getAsString(AcdiVocaDbHelper.FINDS_FIRSTNAME));
		
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
				this);

		RadioButton sexRB = (RadioButton)findViewById(R.id.femaleRadio);
		Log.i(TAG, "sex=" + contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX));
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX).equals("FEMALE"))
			sexRB.setChecked(true);
		else {
			sexRB = (RadioButton)findViewById(R.id.maleRadio);
			sexRB.setChecked(true);
		}
		// HANDLE CHECKBOX NEES TO FIND A BETTER WAY
		CheckBox farmerCB = (CheckBox)findViewById(R.id.farmerCheckBox);
		CheckBox musoCB = (CheckBox)findViewById(R.id.musoCheckBox);
		CheckBox rancherCB = (CheckBox)findViewById(R.id.rancherCheckBox);
		CheckBox storeCB = (CheckBox)findViewById(R.id.storeOwnerCheckBox);
		CheckBox fisherCB = (CheckBox)findViewById(R.id.fisherCheckBox);
		CheckBox otherCB = (CheckBox)findViewById(R.id.otherCheckBox);
		Log.i(TAG, "display Beneficiary Catagory=" + contentValues.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY));
		int test = Integer.parseInt(contentValues.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY));
		if (test >= 32){
			otherCB.setChecked(true);
			test -= 32;
		}
		if (test >= 16){
			fisherCB.setChecked(true);
			test -= 16;
		}if (test >= 8){
			storeCB.setChecked(true);
			test -= 8;
		}if (test >= 4){
			rancherCB.setChecked(true);
			test -= 4;
		}if (test >= 2){
			musoCB.setChecked(true);
			test -= 2;
		}if (test >= 1){
			farmerCB.setChecked(true);
			test -= 1;
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
		Log.i(TAG, "onClick");
		// If a RadioButton was clicked, mark the form as edited.
		//Toast.makeText(this, "Clicked on a " + v.getClass().toString(), Toast.LENGTH_SHORT).show();
		try {
			if (v.getClass().equals(Class.forName("android.widget.RadioButton"))) {
					//Toast.makeText(this, "RadioClicked", Toast.LENGTH_SHORT).show();
					isProbablyEdited = true;
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int id = v.getId();
		if (id == R.id.datepicker) 
			isProbablyEdited = true;
	
		// TODO:  Edit this case
		if(v.getId()==R.id.saveToDbButton) {
			boolean result = false;
			ContentValues data = this.retrieveContentFromView(); 
			Log.i(TAG,"View Content: " + data.toString());
			data.put(AcdiVocaDbHelper.FINDS_PROJECT_ID, 0);
			if (mAction.equals(Intent.ACTION_EDIT)) {
				result = AcdiVocaFindDataManager.getInstance().updateFind(this, mFindId, data);
				Log.i(TAG, "Update to Db is " + result);
			} else {
				DbSimulator db = new DbSimulator();   // Temporary Simulator
				result = db.addNewFind(data);
				//result = AcdiVocaFindDataManager.getInstance().addNewFind(this, data);
				Log.i(TAG, "Save to Db is " + result);
			}
			if (result){
				Toast.makeText(this, data.toString(), Toast.LENGTH_LONG).show();
				Toast.makeText(this, data.toString(), Toast.LENGTH_LONG).show();
				Toast.makeText(this, data.toString(), Toast.LENGTH_LONG).show();
				Toast.makeText(this, data.toString(), Toast.LENGTH_LONG).show();
				Toast.makeText(this, data.toString(), Toast.LENGTH_LONG).show();
				Toast.makeText(this, data.toString(), Toast.LENGTH_LONG).show();
				Toast.makeText(this, data.toString(), Toast.LENGTH_LONG).show();
				Toast.makeText(this, data.toString(), Toast.LENGTH_LONG).show();
				Toast.makeText(this, data.toString(), Toast.LENGTH_LONG).show();
				Toast.makeText(this, data.toString(), Toast.LENGTH_LONG).show();

			}
			else 
				Toast.makeText(this, "Db error", Toast.LENGTH_SHORT).show();
			//this.startActivity(new Intent().setClass(this,AcdiVocaListFindsActivity.class));
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
	 * Intercepts the back key (KEYCODE_BACK) and displays a confirmation dialog
	 * when the user tries to exit POSIT.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.i(TAG, "onKeyDown keyCode = " + keyCode);
		if(keyCode==KeyEvent.KEYCODE_BACK && isProbablyEdited){
			//Toast.makeText(this, "Backkey isEdited=" +  isProbablyEdited, Toast.LENGTH_SHORT).show();
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
		Log.i(TAG, "onCreateDialog");
		switch (id) {
		case CONFIRM_EXIT:
			return new AlertDialog.Builder(this).setIcon(
					R.drawable.alert_dialog_icon).setTitle(R.string.acdivoca_exit_findactivity)
					.setPositiveButton(R.string.Yes,
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// User clicked OK so do some stuff
							finish();
						}
					}).setNegativeButton(R.string.acdivoca_cancel,
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
	

	public void onDateChanged(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		Log.i(TAG, "onDateChanged");
		isProbablyEdited = true;
	}

	//  The remaining methods are part of unused interfaces inherited from the super class.
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) { }
	public void onLocationChanged(Location location) {	}
	public void onProviderDisabled(String provider) {	}
	public void onProviderEnabled(String provider) {	}
	public void onStatusChanged(String provider, int status, Bundle extras) {	}


	/**
	 * Sets the 'edited' flag if text has been changed in an EditText
	 */
	public void afterTextChanged(Editable arg0) {
		Log.i(TAG, "afterTextChanged " + arg0.toString());
		isProbablyEdited = true;
		// TODO Auto-generated method stub
		
	}

	// Unused
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {
		// TODO Auto-generated method stub
		
	}

	// Unused
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		Log.i(TAG, "onTextChanged " + arg0.toString());		
	}

	
	
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		Log.i(TAG, "onItemSelected = " + arg2);
		//isProbablyEdited = true;
	}

	// Unused
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onNothingSelected = " + arg0);

	}
}