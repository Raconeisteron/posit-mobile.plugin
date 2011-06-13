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
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.FindActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.DatePicker.OnDateChangedListener;

/**
 * Handles Finds for AcdiVoca Mobile App.
 * 
 */
public class AcdiVocaFindActivity extends FindActivity implements OnDateChangedListener, 
	TextWatcher, OnItemSelectedListener { //, OnKeyListener {
	public static final String TAG = "AcdiVocaAddActivity";

	private static final int CONFIRM_EXIT = 0;

	private boolean isProbablyEdited = false;   // Set to true if user edits a datum
	private String mAction = "";
	private int mFindId = 0;
	private AcdiVocaDbHelper mDbHelper;
	private Button mSaveButton;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 Log.i(TAG, "onCreate");

		// Create DB helper
//		mDbHelper = new AcdiVocaDbHelper(this);
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

	/**
	 * 
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume, isProbablyEdited= " + isProbablyEdited);
		
		AcdiVocaLocaleManager.setDefaultLocale(this);  // Locale Manager should be in API

		setContentView(R.layout.acdivoca_registration);  // Should be done after locale configuration

		mSaveButton = ((Button)findViewById(R.id.saveToDbButton));
		mSaveButton.setOnClickListener(this);
		((Button)findViewById(R.id.sendSmsButton)).setOnClickListener(this);
		
		// Listen for clicks on radio buttons
		 ((RadioButton)findViewById(R.id.femaleRadio)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.maleRadio)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.malnourishedRadio)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.inpreventionRadio)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.expectingRadio)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.nursingRadio)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.radio_motherleader_yes)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.radio_motherleader_no)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.radio_visit_yes)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.radio_visit_no)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.radio_yes_acdivoca)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.radio_no_acdivoca)).setOnClickListener(this);

		 // Listen for text changes in edit texts and set the isEdited flag
		 ((EditText)findViewById(R.id.firstnameEdit)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.lastnameEdit)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.addressEdit)).addTextChangedListener(this);
//		 ((EditText)findViewById(R.id.ageEdit)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.inhomeEdit)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.responsibleIfChildEdit)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.fatherIfChildEdit)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.responsibleIfMotherEdit)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.husbandIfMotherEdit)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.give_name)).addTextChangedListener(this);

		 // Initialize the DatePicker and listen for changes
		 Calendar calendar = Calendar.getInstance();
		 
		 ((DatePicker)findViewById(R.id.datepicker)).init(
				 calendar.get(Calendar.YEAR),
				 calendar.get(Calendar.MONTH), 
				 calendar.get(Calendar.DAY_OF_MONTH), this);
		 
		 ((Spinner)findViewById(R.id.healthcenterSpinner)).setOnItemSelectedListener(this);
		 ((Spinner)findViewById(R.id.distributionSpinner)).setOnItemSelectedListener(this);

		final Intent intent = getIntent();
		mAction = intent.getAction();
		if (mAction.equals(Intent.ACTION_EDIT)) {
			doEditAction();
			isProbablyEdited = false; // In EDIT mode, initialize after filling in data
			mSaveButton.setEnabled(false);
		}
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
	 * This method is invoked from the Save menu item.  
	 * @return The ContentValues hash table.
	 */
	private ContentValues retrieveContentFromView() {
		Log.i(TAG, "retrieveContentFromView");
		ContentValues result = new ContentValues();
		String value = "";

		EditText eText = (EditText)findViewById(R.id.firstnameEdit);
		if (eText != null) {
			value = eText.getText().toString();
			result.put(AcdiVocaDbHelper.FINDS_FIRSTNAME, value);
		}
		
		
		eText = (EditText) findViewById(R.id.lastnameEdit);

		if (eText != null) {
			value = eText.getText().toString();
			result.put(AcdiVocaDbHelper.FINDS_LASTNAME, value);
			Log.i(TAG, "retrieve LAST NAME = " + value);
		}
		
		
// Eliminated because redundant and possibly inconsistent with DoB		
//		eText = (EditText)findViewById(R.id.ageEdit);
//		if (eText != null) {
//			value = eText.getText().toString();
//			result.put(AcdiVocaDbHelper.FINDS_AGE, value);
//		}
		
		//value = mMonth + "/" + mDay + "/" + mYear;
		DatePicker picker = ((DatePicker)findViewById(R.id.datepicker));
		value = picker.getYear() + "/" + picker.getMonth() + "/" + picker.getDayOfMonth();
		Log.i(TAG, "Date = " + value);
		result.put(AcdiVocaDbHelper.FINDS_DOB, value);
		
		String sex = "undefined";
		RadioButton sexRB = (RadioButton)findViewById(R.id.femaleRadio);
		if (sexRB.isChecked()) 
			sex = AcdiVocaDbHelper.FINDS_FEMALE;
		sexRB = (RadioButton)findViewById(R.id.maleRadio);
		if (sexRB.isChecked()) {
			sex = AcdiVocaDbHelper.FINDS_MALE;
		}
		result.put(AcdiVocaDbHelper.FINDS_SEX, sex);   
		
		
		eText = (EditText)findViewById(R.id.addressEdit);
		if (eText != null) {
			value = eText.getText().toString();
			result.put(AcdiVocaDbHelper.FINDS_ADDRESS, value);
		}
		
		eText = (EditText)findViewById(R.id.inhomeEdit);
		if (eText != null) {
			value = eText.getText().toString();
			result.put(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE,value);
		}
		
		
        
        RadioButton rb = (RadioButton)findViewById(R.id.malnourishedRadio);
        RadioButton rb2 = (RadioButton)findViewById(R.id.inpreventionRadio);
        String sss = "";
        if (rb.isChecked() || rb2.isChecked()){
            eText = (EditText) findViewById(R.id.responsibleIfChildEdit);
            if (eText != null) {
                sss = eText.getText().toString();
                result.put(AcdiVocaDbHelper.FINDS_RELATIVE_1, sss);
                Log.i(TAG, "retrieve RELATIVE 1 = " + value);
                //            Log.i(TAG, "retrieve LAST NAME = " + value);
            }

            eText = (EditText)findViewById(R.id.fatherIfChildEdit);
            if (eText != null) {
                sss = eText.getText().toString();
                result.put(AcdiVocaDbHelper.FINDS_RELATIVE_2, sss);
                Log.i(TAG, "retrieve RELATIVE 2 = " + value);
            }
        }
        
       rb = (RadioButton)findViewById(R.id.expectingRadio);
       rb2 = (RadioButton)findViewById(R.id.nursingRadio);
        if (rb.isChecked() || rb2.isChecked()){
            eText = (EditText) findViewById(R.id.responsibleIfMotherEdit);
            if (eText != null) {
                value = eText.getText().toString();
                result.put(AcdiVocaDbHelper.FINDS_RELATIVE_1, value);
                Log.i(TAG, "retrieve RELATIVE 1 = " + value);
                //            Log.i(TAG, "retrieve LAST NAME = " + value);
            }

            eText = (EditText)findViewById(R.id.husbandIfMotherEdit);
            if (eText != null) {
                value = eText.getText().toString();
                result.put(AcdiVocaDbHelper.FINDS_RELATIVE_2, value);
                Log.i(TAG, "retrieve RELATIVE 2 = " + value);
            }
        }
 
		
//Note: we need something similar to the below for distribution points and health centers		
		
		
//  NOTE:  These are removed because they are redundant with the
//   Distribution point or health center locations.	
//		Spinner communeSpinner = (Spinner)findViewById(R.id.communeSpinner);
//		if (communeSpinner != null) {
//			value = (String)communeSpinner.getSelectedItem();
//			result.put(AcdiVocaDbHelper.COMMUNE_NAME, value);
//		}
//
//		communeSpinner = (Spinner)findViewById(R.id.commune_sectionSpinner);
//		if (communeSpinner != null) {
//			value = (String)communeSpinner.getSelectedItem();
//			result.put(AcdiVocaDbHelper.COMMUNE_SECTION_NAME, value);
//		}
		
		
		
		
		
		// Set the Beneficiary's category (4 exclusive radio buttons)

		String category = "";
		rb = (RadioButton)findViewById(R.id.malnourishedRadio);
		if (rb.isChecked()) {
			category = AcdiVocaDbHelper.FINDS_MALNOURISHED;
		}
		rb = (RadioButton)findViewById(R.id.inpreventionRadio);
		if (rb.isChecked()){
			category = AcdiVocaDbHelper.FINDS_PREVENTION;
		}
		rb = (RadioButton)findViewById(R.id.expectingRadio);
		if (rb.isChecked()) {
			category = AcdiVocaDbHelper.FINDS_EXPECTING;
		}
		rb = (RadioButton)findViewById(R.id.nursingRadio);
		if (rb.isChecked()){
			category = AcdiVocaDbHelper.FINDS_NURSING;
		}
        result.put(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY, category);
        
		String spinnerStr = "";
		Spinner spinner = (Spinner)findViewById(R.id.healthcenterSpinner);
		if (spinner != null) {
			spinnerStr = (String) spinner.getSelectedItem();
			result.put(AcdiVocaDbHelper.FINDS_HEALTH_CENTER, spinnerStr);
		}
		
		if (spinner != null) {
			spinner = (Spinner)findViewById(R.id.distributionSpinner);
			spinnerStr = (String) spinner.getSelectedItem();
			result.put(AcdiVocaDbHelper.FINDS_DISTRIBUTION_POST, spinnerStr);
		}
		
//Adding stuff here - Chris		
		String motherLeader = "";
		RadioButton motherLeaderRB = (RadioButton)findViewById(R.id.radio_motherleader_yes);
		if (motherLeaderRB != null && motherLeaderRB.isChecked()) {
			motherLeader = AcdiVocaDbHelper.FINDS_YES;
		}
		motherLeaderRB = (RadioButton)findViewById(R.id.radio_motherleader_no);
		if (motherLeaderRB != null && motherLeaderRB.isChecked()) {
			motherLeader = AcdiVocaDbHelper.FINDS_NO;
		}
		result.put(AcdiVocaDbHelper.FINDS_Q_MOTHER_LEADER, motherLeader);   
		
		String visitMotherLeader = "";
		RadioButton visitMotherLeaderRB = (RadioButton)findViewById(R.id.radio_visit_yes);
		if (visitMotherLeaderRB != null && visitMotherLeaderRB.isChecked()) {
			visitMotherLeader = AcdiVocaDbHelper.FINDS_YES;
		}
		visitMotherLeaderRB = (RadioButton)findViewById(R.id.radio_visit_no);
		if (visitMotherLeaderRB != null && visitMotherLeaderRB.isChecked()) {
			visitMotherLeader = AcdiVocaDbHelper.FINDS_NO;
		}
		result.put(AcdiVocaDbHelper.FINDS_Q_VISIT_MOTHER_LEADER, visitMotherLeader); 
		Log.i(TAG, AcdiVocaDbHelper.FINDS_Q_VISIT_MOTHER_LEADER +"="+ visitMotherLeader);

		String acdiAgri = "";		
		RadioButton acdiAgriRB = (RadioButton)findViewById(R.id.radio_yes_acdivoca);
		if (acdiAgriRB != null && acdiAgriRB.isChecked()) {
			acdiAgri = AcdiVocaDbHelper.FINDS_YES;
		}
		acdiAgriRB = (RadioButton)findViewById(R.id.radio_no_acdivoca);
		if (acdiAgriRB != null && acdiAgriRB.isChecked()) {
			acdiAgri = AcdiVocaDbHelper.FINDS_NO;
		}
		result.put(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_AGRI, acdiAgri);   
			
		
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

// Removed b/c redundant with and maybe inconsistent with DoB
//		eText = (EditText)findViewById(R.id.ageEdit);
//		eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_AGE));
		
		eText = (EditText)findViewById(R.id.addressEdit);
		eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_ADDRESS));
		
		eText = (EditText)findViewById(R.id.inhomeEdit);
		eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE));
		
		DatePicker dp = (DatePicker) findViewById(R.id.datepicker);
		String date = contentValues.getAsString(AcdiVocaDbHelper.FINDS_DOB);
		Log.i(TAG,"display DOB = " + date);
		int yr=0, mon=0, day=0;
		day = Integer.parseInt(date.substring(date.lastIndexOf("/")+1));
		yr = Integer.parseInt(date.substring(0,date.indexOf("/")));
		mon = Integer.parseInt(date.substring(date.indexOf("/")+1,date.lastIndexOf("/")));
		Log.i(TAG, yr + "/" + mon + "/" + day);
//		mon = mon + 1;  // Months are number 0..11
//		day = day - 1;

		
		RadioButton beneRB1 = (RadioButton)findViewById(R.id.malnourishedRadio);
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY).equals(AcdiVocaDbHelper.FINDS_MALNOURISHED.toString())){
			beneRB1.setChecked(true);
			findViewById(R.id.relatives).setVisibility(View.VISIBLE);		
			findViewById(R.id.mchm).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfChildEdit).setVisibility(View.VISIBLE);
			findViewById(R.id.fatherIfChildEdit).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.INVISIBLE);
			findViewById(R.id.husbandIfMotherEdit).setVisibility(View.INVISIBLE);
		}
		RadioButton beneRB2 = (RadioButton)findViewById(R.id.inpreventionRadio);
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY).equals(AcdiVocaDbHelper.FINDS_PREVENTION.toString())){
			beneRB2.setChecked(true);
			findViewById(R.id.relatives).setVisibility(View.VISIBLE);		
			findViewById(R.id.mchm).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfChildEdit).setVisibility(View.VISIBLE);
			findViewById(R.id.fatherIfChildEdit).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.INVISIBLE);
			findViewById(R.id.husbandIfMotherEdit).setVisibility(View.INVISIBLE);
		}
		RadioButton beneRB3 = (RadioButton)findViewById(R.id.expectingRadio);
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY).equals(AcdiVocaDbHelper.FINDS_EXPECTING.toString())){
			beneRB3.setChecked(true);
			findViewById(R.id.relatives).setVisibility(View.VISIBLE);		
			findViewById(R.id.mchm).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfChildEdit).setVisibility(View.INVISIBLE);
			findViewById(R.id.fatherIfChildEdit).setVisibility(View.INVISIBLE);
			findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.VISIBLE);
			findViewById(R.id.husbandIfMotherEdit).setVisibility(View.VISIBLE);
		}
		RadioButton beneRB4 = (RadioButton)findViewById(R.id.nursingRadio);
		if(contentValues.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY).equals(AcdiVocaDbHelper.FINDS_NURSING.toString())){
			beneRB4.setChecked(true);
			findViewById(R.id.relatives).setVisibility(View.VISIBLE);		
			findViewById(R.id.mchm).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfChildEdit).setVisibility(View.INVISIBLE);
			findViewById(R.id.fatherIfChildEdit).setVisibility(View.INVISIBLE);
			findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.VISIBLE);
			findViewById(R.id.husbandIfMotherEdit).setVisibility(View.VISIBLE);
		}
		
		Toast.makeText(this, contentValues.getAsString(AcdiVocaDbHelper.FINDS_RELATIVE_2), Toast.LENGTH_LONG);
		if(beneRB1.isChecked() || beneRB2.isChecked()){
            eText = (EditText)findViewById(R.id.responsibleIfChildEdit);
            eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_RELATIVE_1));
            
            eText = (EditText)findViewById(R.id.fatherIfChildEdit);
            eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_RELATIVE_2));
        }
		

        if(beneRB3.isChecked() || beneRB4.isChecked()){
            eText = (EditText)findViewById(R.id.responsibleIfMotherEdit);
            eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_RELATIVE_1));
            
            eText = (EditText)findViewById(R.id.husbandIfMotherEdit);
            eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_RELATIVE_2));
        }

//		DatePicker dp = (DatePicker) findViewById(R.id.datepicker);
//		String date = contentValues.getAsString(AcdiVocaDbHelper.FINDS_DOB);
//		Log.i(TAG,"display DOB = " + date);
		if (date != null) {
			Log.i(TAG,"display DOB = " + date);
			dp.init(yr, mon, day, this);
		}
//		dp.init(Integer.parseInt(date.substring(date.lastIndexOf("/")+1)), 
//				Integer.parseInt(date.substring(0,date.indexOf("/"))),
//				Integer.parseInt(date.substring(date.indexOf("/")+1,date.lastIndexOf("/"))),
//				(OnDateChangedListener) this);

		RadioButton sexRB = (RadioButton)findViewById(R.id.femaleRadio);
		Log.i(TAG, "sex=" + contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX));
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX).equals(AcdiVocaDbHelper.FINDS_FEMALE))
			sexRB.setChecked(true);
		sexRB = (RadioButton)findViewById(R.id.maleRadio);
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX).equals(AcdiVocaDbHelper.FINDS_MALE))
			sexRB.setChecked(true);

		
		RadioButton aRadioButton = (RadioButton)findViewById(R.id.radio_motherleader_yes);
		Log.i(TAG, "motherLeader=" + contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_MOTHER_LEADER));

		String value = contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_MOTHER_LEADER);
		if (value != null) {
			if (value.equals(AcdiVocaDbHelper.FINDS_YES))
				aRadioButton.setChecked(true);
			else 
				aRadioButton.setChecked(false);

			aRadioButton = (RadioButton)findViewById(R.id.radio_motherleader_no);
		
			if (value.equals(AcdiVocaDbHelper.FINDS_NO)){
				aRadioButton.setChecked(true);
			} else {
				aRadioButton.setChecked(false);
			}
		}
		
		value = contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_VISIT_MOTHER_LEADER);
		aRadioButton = (RadioButton)findViewById(R.id.radio_visit_yes);
		Log.i(TAG, "motherLeaderVisit=" + contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_VISIT_MOTHER_LEADER));
		if (value != null) {
			if (value.equals(AcdiVocaDbHelper.FINDS_YES))
				aRadioButton.setChecked(true);
			else 
				aRadioButton.setChecked(false);

			aRadioButton = (RadioButton)findViewById(R.id.radio_visit_no);
		
			if (value.equals(AcdiVocaDbHelper.FINDS_NO)){
				aRadioButton.setChecked(true);
			} else {
				aRadioButton.setChecked(false);

			}
		}
		
		value = contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_AGRI);
		aRadioButton = (RadioButton)findViewById(R.id.radio_yes_acdivoca);
		Log.i(TAG, "acdiAgri=" + contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_AGRI));
		if (value != null) {
			if (value.equals(AcdiVocaDbHelper.FINDS_YES))
				aRadioButton.setChecked(true);
			else 
				aRadioButton.setChecked(false);
			aRadioButton  = (RadioButton)findViewById(R.id.radio_no_acdivoca);
			if (value.equals(AcdiVocaDbHelper.FINDS_NO)){
				aRadioButton.setChecked(true);
			} else {
				aRadioButton.setChecked(false);

			}
		}
	
			
		
		Spinner spinner = (Spinner)findViewById(R.id.healthcenterSpinner);
//  New code commented out - Needs to implemented instead of other spinner data - Chris	

			spinnerSetter(spinner, contentValues, AcdiVocaDbHelper.FINDS_HEALTH_CENTER);			
			spinner = (Spinner)findViewById(R.id.distributionSpinner);
			spinnerSetter(spinner, contentValues, AcdiVocaDbHelper.FINDS_DISTRIBUTION_POST);

//		String selected = contentValues.getAsString(AcdiVocaDbHelper.FINDS_HEALTH_CENTER);
//		int k = 0;
//		String item = null;
//		if (selected != null) {
//			item = (String) spinner.getItemAtPosition(k);
//			while (k < spinner.getCount() && !selected.equals(item)) {
//				++k;
//				item = (String) spinner.getItemAtPosition(k);
//			}
//			spinner.setSelection(k);
//		}

	
		
		
//		Spinner spinner = (Spinner)findViewById(R.id.communeSpinner);
//		String selected = contentValues.getAsString(AcdiVocaDbHelper.COMMUNE_NAME);
//		int k = 0;
//		String item = null;
//		if (selected != null) {
//			item = (String) spinner.getItemAtPosition(k);
//			while (k < spinner.getCount() && !selected.equals(item)) {
//				item = (String) spinner.getItemAtPosition(k);
//				++k;
//			}
//			spinner.setSelection(k);
//		}
//
//		spinner = (Spinner)findViewById(R.id.commune_sectionSpinner);
//		selected = contentValues.getAsString(AcdiVocaDbHelper.COMMUNE_SECTION_NAME);
//		if (selected != null) {
//			Log.i(TAG, "Commune section spinner count = " + spinner.getCount());
//			k = 0;
//			item = (String) spinner.getItemAtPosition(k);
//			while (k < spinner.getCount() && !selected.equals(item)) {
//				Log.i(TAG, "Commune section item = " + item + " " + k);
//				item = (String) spinner.getItemAtPosition(k);
//				++k;
//			}
//			spinner.setSelection(k);
//		}
		
		
		
		

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
					mSaveButton.setEnabled(true);	
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int id = v.getId();
		if (id == R.id.datepicker) {
			isProbablyEdited = true;
			mSaveButton.setEnabled(true);	
		}
		
		if (id == R.id.expectingRadio || id == R.id.nursingRadio) {
			RadioButton rb = (RadioButton) v;
			findViewById(R.id.relatives).setVisibility(View.VISIBLE);
			findViewById(R.id.mchm).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.VISIBLE);
			findViewById(R.id.husbandIfMotherEdit).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfChildEdit).setVisibility(View.INVISIBLE);
			findViewById(R.id.fatherIfChildEdit).setVisibility(View.INVISIBLE);	        
			//Toast.makeText(AcdiVocaFindActivity.this, rb.getText(), Toast.LENGTH_SHORT).show();
		} 
		if (id == R.id.malnourishedRadio || id == R.id.inpreventionRadio) {
			RadioButton rb = (RadioButton) v;
			findViewById(R.id.relatives).setVisibility(View.VISIBLE);		
			findViewById(R.id.mchm).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfChildEdit).setVisibility(View.VISIBLE);
			findViewById(R.id.fatherIfChildEdit).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.INVISIBLE);
			findViewById(R.id.husbandIfMotherEdit).setVisibility(View.INVISIBLE);	        
			//Toast.makeText(AcdiVocaFindActivity.this, rb.getText(), Toast.LENGTH_SHORT).show();
		}

		if(v.getId()==R.id.saveToDbButton) {
			boolean result = false;
			ContentValues data = this.retrieveContentFromView(); 
			Log.i(TAG,"View Content: " + data.toString());
			data.put(AcdiVocaDbHelper.FINDS_PROJECT_ID, 0);
			if (mAction.equals(Intent.ACTION_EDIT)) {
				result = AcdiVocaFindDataManager.getInstance().updateFind(this, mFindId, data);
				RadioButton agri = (RadioButton)findViewById(R.id.radio_yes_acdivoca);
				if(agri.isChecked()){
					Intent intent = new Intent(this, AcdiVocaNewAgriActivity.class);
					intent.setAction(Intent.ACTION_INSERT);
					startActivityForResult(intent, 0);
					Toast.makeText(this, "Please fill the form for the person who is in agricultural program.", Toast.LENGTH_LONG);
				}
				Log.i(TAG, "Update to Db is " + result);
			} else {
				data.put(AcdiVocaDbHelper.FINDS_DOSSIER, "New MCHN");
				result = AcdiVocaFindDataManager.getInstance().addNewFind(this, data);
				//if radioAgri is checked, make intent
				RadioButton agri = (RadioButton)findViewById(R.id.radio_yes_acdivoca);
				if(agri.isChecked()){
					Intent intent = new Intent(this, AcdiVocaNewAgriActivity.class);
					intent.setAction(Intent.ACTION_INSERT);
					Toast.makeText(this, "Please fill the form for the person who is in agricultural program.", Toast.LENGTH_LONG);
					startActivityForResult(intent, 0);
				}
				Log.i(TAG, "Save to Db is " + result);
			}
			if (result){
				Log.i(TAG, "Save to Db returned success");
				Toast.makeText(this, "Saved to Db", Toast.LENGTH_SHORT).show();  
				//Toast.makeText(this, data.toString(), Toast.LENGTH_SHORT).show();  
			}
			else {
				Log.i(TAG, "Save to Db returned failure");
				Toast.makeText(this, "Error on save to Db", Toast.LENGTH_SHORT).show();
			}
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
		mSaveButton.setEnabled(true);	
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
		mSaveButton.setEnabled(true);			
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

	
	
	/**
	 * Called when a spinner selection is made.
	 */
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		Log.i(TAG, "onItemSelected = " + arg2);
		//isProbablyEdited = true;
		//mSaveButton.setEnabled(true);	
	}


	// Unused
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onNothingSelected = " + arg0);

	}
	
	
//	Spinner spinner = (Spinner)findViewById(R.id.communeSpinner);
//	String selected = contentValues.getAsString(AcdiVocaDbHelper.COMMUNE_NAME);
//	int k = 0;
//	String item = null;
//	if (selected != null) {
//		item = (String) spinner.getItemAtPosition(k);
//		while (k < spinner.getCount() && !selected.equals(item)) {
//			item = (String) spinner.getItemAtPosition(k);
//			++k;
//		}
//		spinner.setSelection(k);
//	}
	
//spinner function	
	public static void spinnerSetter(Spinner spinner, ContentValues contentValues, String attribute){
		String selected = contentValues.getAsString(attribute);
		int k = 0;
		if(selected != null){
			String item = (String) spinner.getItemAtPosition(k);
			while (k < spinner.getCount() && !selected.equals(item)) {
				++k;
				item = (String) spinner.getItemAtPosition(k);				
			}
			if (k < spinner.getCount())
				spinner.setSelection(k);
			else
				spinner.setSelection(0);
		}
		else{
			spinner.setSelection(0);
		}
	}
	
}