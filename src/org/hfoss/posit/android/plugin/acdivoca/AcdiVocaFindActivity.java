/* File: AcdiVocaFindActivity.java
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
import org.hfoss.posit.android.api.SettingsActivity;

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
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
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
		switch (item.getItemId()) {
		case R.id.settings_menu_item:
			startActivity(new Intent(this, SettingsActivity.class));
			break;
		}
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

//		mSaveButton = ((Button)findViewById(R.id.saveToDbButton));
//		mSaveButton.setOnClickListener(this);

		// Listen for clicks on radio buttons, edit texts, spinners, etc.
		initializeListeners();

		final Intent intent = getIntent();
		mAction = intent.getAction();
		if (mAction != null && mAction.equals(Intent.ACTION_EDIT)) {
			int type = intent.getIntExtra(AcdiVocaDbHelper.FINDS_TYPE, 0);
//			if (type == AcdiVocaDbHelper.FINDS_TYPE_MCHN){
//				findViewById(R.id.participating_agri).setVisibility(View.GONE);
//				findViewById(R.id.radio_participating_agri).setVisibility(View.GONE);
//			}
//			if (type == AcdiVocaDbHelper.FINDS_TYPE_AGRI){
//				findViewById(R.id.mchnPart).setVisibility(View.GONE);
//				findViewById(R.id.radio_same_bene).setVisibility(View.GONE);
//				findViewById(R.id.label_mchn).setVisibility(View.GONE);
//			}
//			if (type == AcdiVocaDbHelper.FINDS_TYPE_BOTH){
//				findViewById(R.id.mchnPart).setVisibility(View.VISIBLE);
//				//				findViewById(R.id.agriPart).setVisibility(View.VISIBLE);
//				findViewById(R.id.participating_agri).setVisibility(View.GONE);
//				findViewById(R.id.radio_participating_agri).setVisibility(View.GONE);
//				findViewById(R.id.participating_bene_same).setVisibility(View.GONE);
//				findViewById(R.id.radio_same_bene).setVisibility(View.GONE);
//			}

			displayAsUneditable();
			isProbablyEdited = false; // In EDIT mode, initialize after filling in data
			mSaveButton.setEnabled(false);
		}

		if (mAction != null && mAction.equals(Intent.ACTION_INSERT)){
			Log.i(TAG,"############################################");
			Log.i(TAG,"you are now in insert");
			if(intent.getExtras() != null){
				int type = intent.getIntExtra(AcdiVocaDbHelper.FINDS_TYPE, 0);
//				if (type == AcdiVocaDbHelper.FINDS_TYPE_MCHN){
//					//					findViewById(R.id.agriPart).setVisibility(View.GONE);
//				}
//				if (type == AcdiVocaDbHelper.FINDS_TYPE_AGRI){
//					findViewById(R.id.mchnPart).setVisibility(View.GONE);					
//				}
				String doing = intent.getStringExtra(AttributeManager.FINDS_RELATIVE_AGRI);
				if (doing != null){
					if (doing.equals(AcdiVocaDbHelper.FINDS_YES)){
						findViewById(R.id.agri_rel).setVisibility(View.VISIBLE);
						findViewById(R.id.participating_bene_same).setVisibility(View.GONE);
						findViewById(R.id.radio_same_bene).setVisibility(View.GONE);
					}
				}
				doing = intent.getStringExtra(AttributeManager.FINDS_RELATIVE_BENE);
				if (doing != null){
					if (doing.equals(AcdiVocaDbHelper.FINDS_YES)){
						findViewById(R.id.mchn_rel).setVisibility(View.VISIBLE);
						findViewById(R.id.participating_agri).setVisibility(View.GONE);
						findViewById(R.id.radio_participating_agri).setVisibility(View.GONE);
					}
				}
			}
		}
	}
	
	/**
	 * Helper method to create listener for radio buttons, etc.
	 */
	private void initializeListeners() {
		mSaveButton = ((Button)findViewById(R.id.saveToDbButton));
		mSaveButton.setOnClickListener(this);

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
		((RadioButton)findViewById(R.id.radio_yes_participating_agri)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.radio_no_participating_agri)).setOnClickListener(this);
		//added Jun17
		((RadioButton)findViewById(R.id.radio_yes_relative_participating_agri)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.radio_no_relative_participating_agri)).setOnClickListener(this);
		//added from agri
		//		 ((RadioButton)findViewById(R.id.radio_yes_bene)).setOnClickListener(this);
		//		 ((RadioButton)findViewById(R.id.radio_no_bene)).setOnClickListener(this);
		//		 ((RadioButton)findViewById(R.id.radio_yes_bene_same)).setOnClickListener(this);
		//		 ((RadioButton)findViewById(R.id.radio_no_bene_same)).setOnClickListener(this);


		// Listen for text changes in edit texts and set the isEdited flag
		((EditText)findViewById(R.id.firstnameEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.lastnameEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.addressEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.inhomeEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.responsibleIfChildEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.responsibleIfMotherEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.give_name)).addTextChangedListener(this);

		// Initialize the DatePicker and listen for changes
		Calendar calendar = Calendar.getInstance();

		((DatePicker)findViewById(R.id.datepicker)).init(
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH), 
				calendar.get(Calendar.DAY_OF_MONTH), this);

		((Spinner)findViewById(R.id.healthcenterSpinner)).setOnItemSelectedListener(this);
		((Spinner)findViewById(R.id.distributionSpinner)).setOnItemSelectedListener(this);
		//		 ((Spinner)findViewById(R.id.unitSpinner)).setOnItemSelectedListener(this);

	}
	
	
	
	/**
	 * Allows editing of editable data for existing finds.  For existing finds, 
	 * we retrieve the Find's data from the DB and display it in a TextView. The
	 * Find's location and time stamp are not updated.
	 */
	private void displayAsUneditable() {
		Log.i(TAG, "doEditAction");
		mFindId = (int) getIntent().getLongExtra(AcdiVocaDbHelper.FINDS_ID, 0); 
		Log.i(TAG,"Find id = " + mFindId);
		ContentValues values = AcdiVocaFindDataManager.getInstance().fetchFindDataById(this, mFindId, null);
		displayContentUneditable(values);
		//displayContentInView(values);						
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
		// ADDING TYPE
		final Intent intent = getIntent();
		int x = intent.getIntExtra(AcdiVocaDbHelper.FINDS_TYPE, 0);
		
		if (x == AcdiVocaDbHelper.FINDS_TYPE_MCHN)
			result.put(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_MCHN);
		
		if (x == AcdiVocaDbHelper.FINDS_TYPE_AGRI)
			result.put(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_AGRI);
		
		RadioButton brb = (RadioButton)findViewById(R.id.radio_yes_participating_agri);
//		RadioButton arb = (RadioButton)findViewById(R.id.radio_yes_bene_same);
//		if(brb.isChecked() || arb.isChecked()){
		if(brb.isChecked()){
			result.put(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_BOTH);
		}
		
		// ADDING FIRST NAME
		EditText eText = (EditText)findViewById(R.id.firstnameEdit);
		if (eText != null) {
			value = eText.getText().toString();
			result.put(AcdiVocaDbHelper.FINDS_FIRSTNAME, value);
		}
		// ADDING LAST NAME		
		eText = (EditText) findViewById(R.id.lastnameEdit);
		if (eText != null) {
			value = eText.getText().toString();
			result.put(AcdiVocaDbHelper.FINDS_LASTNAME, value);
			Log.i(TAG, "retrieve LAST NAME = " + value);
		}
		// ADDING ADDRESS
		eText = (EditText)findViewById(R.id.addressEdit);
		if (eText != null) {
			value = eText.getText().toString();
			result.put(AcdiVocaDbHelper.FINDS_ADDRESS, value);
		}
		// ADDING DOB
		
		//value = mMonth + "/" + mDay + "/" + mYear;
		DatePicker picker = ((DatePicker)findViewById(R.id.datepicker));
		value = picker.getYear() + "/" + picker.getMonth() + "/" + picker.getDayOfMonth();
		Log.i(TAG, "Date = " + value);
		result.put(AcdiVocaDbHelper.FINDS_DOB, value);
		
		// ADDING SEX
		String sex = "";
		RadioButton sexRB = (RadioButton)findViewById(R.id.femaleRadio);
		if (sexRB.isChecked()) 
			sex = AcdiVocaDbHelper.FINDS_FEMALE;
		sexRB = (RadioButton)findViewById(R.id.maleRadio);
		if (sexRB.isChecked()) {
			sex = AcdiVocaDbHelper.FINDS_MALE;
		}
		result.put(AcdiVocaDbHelper.FINDS_SEX, sex);   
		
		//ADDING NUMER OF PEOPLE AT HOME		
		eText = (EditText)findViewById(R.id.inhomeEdit);
		if (eText != null) {
			value = eText.getText().toString();
			result.put(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE,value);
		}
		
		// ADDING HEALTH CENTER AND DISTRIBUTION POST        
		String spinnerStr = "";
		Spinner spinner = (Spinner)findViewById(R.id.distributionSpinner);
		if (spinner != null) {
			spinnerStr = (String) spinner.getSelectedItem();
			result.put(AcdiVocaDbHelper.FINDS_DISTRIBUTION_POST, spinnerStr);
		}
		
        // ADDING BENEFICIARY CATAGORY FOR MCHN FORM AND RESPECTIVE TEXT FIELDS
        RadioButton rb = (RadioButton)findViewById(R.id.malnourishedRadio);
        RadioButton rb2 = (RadioButton)findViewById(R.id.inpreventionRadio);
        String sss = "";
        if (rb.isChecked() || rb2.isChecked()){
            eText = (EditText) findViewById(R.id.responsibleIfChildEdit);
            if (eText != null) {
                sss = eText.getText().toString();
                result.put(AcdiVocaDbHelper.FINDS_RELATIVE_1, sss);
                Log.i(TAG, "retrieve RELATIVE 1 = " + value);
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
            }

        }
		
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
        
		
        // MCHN FORM QUESTIONS
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

		Boolean acdiAgri = false;		
		RadioButton acdiAgriRB = (RadioButton)findViewById(R.id.radio_yes_participating_agri);
		if (acdiAgriRB != null && acdiAgriRB.isChecked()) {
			acdiAgri = true;
		}
		acdiAgriRB = (RadioButton)findViewById(R.id.radio_no_participating_agri);
		if (acdiAgriRB != null && acdiAgriRB.isChecked()) {
			acdiAgri = false;
		}
		result.put(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_AGRI, acdiAgri);   

		Boolean acdiAgriRelative = false;		
		RadioButton acdiAgriSameRB = (RadioButton)findViewById(R.id.radio_yes_relative_participating_agri);
		if (acdiAgriSameRB != null && acdiAgriSameRB.isChecked()) {
			acdiAgriRelative = true;
		}
		acdiAgriSameRB = (RadioButton)findViewById(R.id.radio_no_relative_participating_agri);
		if (acdiAgriSameRB != null && acdiAgriSameRB.isChecked()) {
			acdiAgriRelative = false;
		}
		result.put(AcdiVocaDbHelper.FINDS_Q_RELATIVE_AGRI, acdiAgriRelative);
		
		return result;
	}
	
	
    /**
     * Displays the content as uneditable labels -- default view.
     * @param values
     */
	private void displayContentUneditable(ContentValues values) {

		if (values != null){
			this.setContentView(R.layout.acdivoca_health_beneficiary_noedit);
			((Button)findViewById(R.id.editFind)).setOnClickListener(this);

			findViewById(R.id.unedit).setVisibility(View.VISIBLE);
//			findViewById(R.id.form).setVisibility(View.GONE);

			TextView tv = ((TextView) findViewById(R.id.first_label));
			tv.setText(getString(R.string.firstname) + ": " 
					+  values.getAsString(AcdiVocaDbHelper.FINDS_FIRSTNAME));
			tv = ((TextView) findViewById(R.id.last_label));
			tv.setText(getString(R.string.lastname) + ": " 
					+  values.getAsString(AcdiVocaDbHelper.FINDS_LASTNAME)); 
			tv = ((TextView) findViewById(R.id.address_label));
			tv.setText(getString(R.string.address) + ": " 
					+  values.getAsString(AcdiVocaDbHelper.FINDS_ADDRESS)); 
			tv = ((TextView) findViewById(R.id.dob_label));
			tv.setText(getString(R.string.dob) + ": " 
					+  values.getAsString(AcdiVocaDbHelper.FINDS_DOB));  
			tv = ((TextView) findViewById(R.id.sex_label));
			tv.setText(getString(R.string.sex) + ": " 
					+  values.getAsString(AcdiVocaDbHelper.FINDS_SEX)); 
			tv = ((TextView) findViewById(R.id.num_ppl_label));

			tv.setText(getString(R.string.Number_of_people_in_home) + ": " 
					+  values.getAsString(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE));

			// MCHN PART    	

			String mc = values.getAsString(AcdiVocaDbHelper.FINDS_DISTRIBUTION_POST);
			if (mc != null){
				tv = ((TextView) findViewById(R.id.distro_label));
				tv.setText(getString(R.string.distribution_post) + ": " 
						+  values.getAsString(AcdiVocaDbHelper.FINDS_DISTRIBUTION_POST));

				tv = ((TextView) findViewById(R.id.bene_category_label));
				tv.setText(getString(R.string.Beneficiary_Category) + ": " 
						+  values.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY));

				if (values.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY).equals(AcdiVocaDbHelper.FINDS_MALNOURISHED) || values.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY).equals(AcdiVocaDbHelper.FINDS_PREVENTION)){
					tv = ((TextView) findViewById(R.id.child_label));
					tv.setText(getString(R.string.responsible_if_child) + ": " 
							+  values.getAsString(AcdiVocaDbHelper.FINDS_RELATIVE_1));
				}
				if (values.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY).equals(AcdiVocaDbHelper.FINDS_EXPECTING) || values.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY).equals(AcdiVocaDbHelper.FINDS_NURSING)){
					tv = ((TextView) findViewById(R.id.mother_label));
					tv.setText(getString(R.string.responsible_if_mother) + ": " 
							+  values.getAsString(AcdiVocaDbHelper.FINDS_RELATIVE_1));
				}
				tv = ((TextView) findViewById(R.id.mleader_label));
				tv.setText(getString(R.string.mother_leader) + ": " 
						+  values.getAsString(AcdiVocaDbHelper.FINDS_Q_MOTHER_LEADER));

				tv = ((TextView) findViewById(R.id.visit_label));
				tv.setText(getString(R.string.visit_mother_leader) + ": " 
						+  values.getAsString(AcdiVocaDbHelper.FINDS_Q_VISIT_MOTHER_LEADER));

				tv = ((TextView) findViewById(R.id.participating_self_label));
				tv.setText(": " 
						+  values.getAsString(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_AGRI));

				tv = ((TextView) findViewById(R.id.participating_relative_label));
				tv.setText(": " 
						+  values.getAsString(AcdiVocaDbHelper.FINDS_Q_RELATIVE_AGRI));
				
				tv = ((TextView) findViewById(R.id.participating_relative_name));
				tv.setText(": " 
						+  values.getAsString(AcdiVocaDbHelper.FINDS_RELATIVE_1));


			}
		}
	}
	
	
	/**
	 * Displays values from a ContentValues in the View.
	 * @param contentValues stores <key, value> pairs
	 */
	private void displayContentInView(ContentValues contentValues) {
		Log.i(TAG, "displayContentInView");

		if (contentValues != null) {
			setContentView(R.layout.acdivoca_registration);
			initializeListeners();
						
			EditText eText = (EditText) findViewById(R.id.lastnameEdit);
			eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_LASTNAME));

			eText = (EditText) findViewById(R.id.firstnameEdit);
			eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_FIRSTNAME));
			Log.i(TAG,"display First Name = " + contentValues.getAsString(AcdiVocaDbHelper.FINDS_FIRSTNAME));

			eText = (EditText)findViewById(R.id.addressEdit);
			eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_ADDRESS));

			DatePicker dp = (DatePicker) findViewById(R.id.datepicker);
			String date = contentValues.getAsString(AcdiVocaDbHelper.FINDS_DOB);
			Log.i(TAG,"display DOB = " + date);
			int yr=0, mon=0, day=0;
			day = Integer.parseInt(date.substring(date.lastIndexOf("/")+1));
			yr = Integer.parseInt(date.substring(0,date.indexOf("/")));
			mon = Integer.parseInt(date.substring(date.indexOf("/")+1,date.lastIndexOf("/")));
			Log.i(TAG, yr + "/" + mon + "/" + day);
			try {
				if (date != null) {
					Log.i(TAG,"display DOB = " + date);
					dp.init(yr, mon, day, this);
				}
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "Illegal Argument, probably month == 12 in " + date);
				e.printStackTrace();
			}


			RadioButton sexRB = (RadioButton)findViewById(R.id.femaleRadio);
			Log.i(TAG, "sex=" + contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX));
			if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX).equals(AcdiVocaDbHelper.FINDS_FEMALE))
				sexRB.setChecked(true);
			sexRB = (RadioButton)findViewById(R.id.maleRadio);
			if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX).equals(AcdiVocaDbHelper.FINDS_MALE))
				sexRB.setChecked(true);

			// SPINNERS FOR MCHN
			Spinner spinner = (Spinner)findViewById(R.id.distributionSpinner);
			setSpinner(spinner, contentValues, AcdiVocaDbHelper.FINDS_DISTRIBUTION_POST);

			// NUMBNER OF PEOPLE IN HOME
			eText = (EditText)findViewById(R.id.inhomeEdit);
			eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE));

			// MCHN CATAGORY
			RadioButton beneRB1 = (RadioButton)findViewById(R.id.malnourishedRadio);
			if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY).equals(AcdiVocaDbHelper.FINDS_MALNOURISHED.toString())){
				beneRB1.setChecked(true);
				findViewById(R.id.relatives).setVisibility(View.VISIBLE);		
				findViewById(R.id.responsibleIfChildEdit).setVisibility(View.VISIBLE);
				findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.INVISIBLE);
			}
			RadioButton beneRB2 = (RadioButton)findViewById(R.id.inpreventionRadio);

			String val = contentValues.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY);

			if (val.equals(AcdiVocaDbHelper.FINDS_PREVENTION.toString())){
				beneRB2.setChecked(true);
				findViewById(R.id.relatives).setVisibility(View.VISIBLE);		
				findViewById(R.id.responsibleIfChildEdit).setVisibility(View.VISIBLE);
				findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.INVISIBLE);
			}
			RadioButton beneRB3 = (RadioButton)findViewById(R.id.expectingRadio);
			if (val.equals(AcdiVocaDbHelper.FINDS_EXPECTING.toString())){
				beneRB3.setChecked(true);
				findViewById(R.id.relatives).setVisibility(View.VISIBLE);		
				findViewById(R.id.responsibleIfChildEdit).setVisibility(View.INVISIBLE);
				findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.VISIBLE);
			}
			RadioButton beneRB4 = (RadioButton)findViewById(R.id.nursingRadio);
			if(val.equals(AcdiVocaDbHelper.FINDS_NURSING.toString())){
				beneRB4.setChecked(true);
				findViewById(R.id.relatives).setVisibility(View.VISIBLE);		
				findViewById(R.id.responsibleIfChildEdit).setVisibility(View.INVISIBLE);
				findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.VISIBLE);
			}

			if(beneRB1.isChecked() || beneRB2.isChecked()){
				eText = (EditText)findViewById(R.id.responsibleIfChildEdit);
				eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_RELATIVE_1));
			}


			if(beneRB3.isChecked() || beneRB4.isChecked()){
				eText = (EditText)findViewById(R.id.responsibleIfMotherEdit);
				eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_RELATIVE_1));
			}

			// MCHN QUESTIONS
			RadioButton aRadioButton = (RadioButton)findViewById(R.id.radio_motherleader_yes);
			Log.i(TAG, "motherLeader=" + contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_MOTHER_LEADER));

			Integer value = contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_Q_MOTHER_LEADER);
			if (value != null) {
				if (value.equals(1))
					aRadioButton.setChecked(true);
				else 
					aRadioButton.setChecked(false);

				aRadioButton = (RadioButton)findViewById(R.id.radio_motherleader_no);

				if (value.equals(0)){
					aRadioButton.setChecked(true);
				} else {
					aRadioButton.setChecked(false);
				}
			}

			value = contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_Q_VISIT_MOTHER_LEADER);
			aRadioButton = (RadioButton)findViewById(R.id.radio_visit_yes);
			Log.i(TAG, "motherLeaderVisit=" + contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_VISIT_MOTHER_LEADER));
			if (value != null) {
				if (value.equals(1))
					aRadioButton.setChecked(true);
				else 
					aRadioButton.setChecked(false);

				aRadioButton = (RadioButton)findViewById(R.id.radio_visit_no);

				if (value.equals(0)){
					aRadioButton.setChecked(true);
				} else {
					aRadioButton.setChecked(false);

				}
			}

			value = contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_AGRI);
			aRadioButton = (RadioButton)findViewById(R.id.radio_yes_participating_agri);
			Log.i(TAG, "acdiAgri=" + contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_AGRI));
			if (value != null) {
				if (value.equals(1))
					aRadioButton.setChecked(true);
				else 
					aRadioButton.setChecked(false);
				aRadioButton  = (RadioButton)findViewById(R.id.radio_no_participating_agri);
				if (value.equals(0)){
					aRadioButton.setChecked(true);
				} else {
					aRadioButton.setChecked(false);

				}
			}

			//added Jun 17
			value = contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_Q_RELATIVE_AGRI);
			aRadioButton = (RadioButton)findViewById(R.id.radio_yes_relative_participating_agri);
			Log.i(TAG, "acdiAgri=" + contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_Q_RELATIVE_AGRI));
			if (value != null) {
				if (value.equals(1))
					aRadioButton.setChecked(true);
				else 
					aRadioButton.setChecked(false);
				aRadioButton  = (RadioButton)findViewById(R.id.radio_no_relative_participating_agri);
				if (value.equals(0)){
					aRadioButton.setChecked(true);
				} else {
					aRadioButton.setChecked(false);

				}
			}
			
			// Disable Save button until form is edited
			isProbablyEdited = false;
			mSaveButton.setEnabled(false);	
		}
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
					Log.i(TAG, "Radio clicked");
					isProbablyEdited = true;
					mSaveButton.setEnabled(true);	
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// The Edit Button
		int id = v.getId();
		if (id == R.id.editFind){
	    	mFindId = (int) getIntent().getLongExtra(AcdiVocaDbHelper.FINDS_ID, 0); 
			ContentValues values = AcdiVocaFindDataManager.getInstance().fetchFindDataById(this, mFindId, null);
//			isProbablyEdited = false;
//			mSaveButton.setEnabled(false);	
			displayContentInView(values);	
		}
		
		if (id == R.id.datepicker) {
			isProbablyEdited = true;
			mSaveButton.setEnabled(true);	
		}
		
		// Are you participating in Agri?
		
		// If no, ask whether relative is participating.
		if (id == R.id.radio_no_participating_agri){
			Log.i(TAG, "Clicked no_acdivoca");
//			findViewById(R.id.agriPart).setVisibility(View.GONE);
			findViewById(R.id.relative_participating_agri).setVisibility(View.VISIBLE);
			findViewById(R.id.radio_relative_participating_agri).setVisibility(View.VISIBLE);
		}
		
//		// If yes, get Agri data including communal section, seeds, tools, etc..
//		
		if (id == R.id.radio_yes_participating_agri){
			Log.i(TAG, "Clicked yes_acdivoca");
			findViewById(R.id.relative_participating_agri).setVisibility(View.GONE);
			findViewById(R.id.radio_relative_participating_agri).setVisibility(View.GONE);
			RadioButton rb = (RadioButton)findViewById(R.id.radio_yes_relative_participating_agri);
			rb.setChecked(false);
//
////			findViewById(R.id.agriPart).setVisibility(View.VISIBLE);
//			findViewById(R.id.commune_sectionSpinner).setFocusable(true);
//			findViewById(R.id.commune_sectionSpinner).setFocusableInTouchMode(true);			
//			findViewById(R.id.commune_sectionSpinner).requestFocus();
//			findViewById(R.id.participating_bene_same).setVisibility(View.GONE);
//			findViewById(R.id.radio_same_bene).setVisibility(View.GONE);
		}
		
		// Is a relative participating in Agri?
		// If no, hide the Agri form 
		if (id == R.id.radio_no_relative_participating_agri){
			Log.i(TAG, "Clicked no_relative_participating_agri");
			findViewById(R.id.give_name).setVisibility(View.GONE);
			findViewById(R.id.give_name).setEnabled(false);		}
		
		// If relative participating in Agri, get the name.
		if (id == R.id.radio_yes_relative_participating_agri){
			Log.i(TAG, "Clicked yes_relative_participating_agri");
			findViewById(R.id.give_name).setVisibility(View.VISIBLE);
			findViewById(R.id.give_name).setEnabled(true);

//			findViewById(R.id.participating_bene).setVisibility(View.GONE);
//			findViewById(R.id.radio_participating_bene).setVisibility(View.GONE);
//			RadioButton rb = (RadioButton)findViewById(R.id.radio_yes_bene);
//			rb.setChecked(false);
//			
//			findViewById(R.id.mchnPart).setVisibility(View.VISIBLE);
//			findViewById(R.id.distributionSpinner).setFocusable(true);
//			findViewById(R.id.distributionSpinner).setFocusableInTouchMode(true);			
//			findViewById(R.id.distributionSpinner).requestFocus();
//			findViewById(R.id.participating_agri).setVisibility(View.GONE);
//			findViewById(R.id.radio_participating_agri).setVisibility(View.GONE);
		}		
		
		if (id == R.id.expectingRadio || id == R.id.nursingRadio) {
			findViewById(R.id.relatives).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfChildEdit).setVisibility(View.INVISIBLE);
		} 
		if (id == R.id.malnourishedRadio || id == R.id.inpreventionRadio) {
			findViewById(R.id.relatives).setVisibility(View.VISIBLE);		
			findViewById(R.id.responsibleIfChildEdit).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.INVISIBLE);
		}
		if(v.getId()==R.id.saveToDbButton) {
			boolean result = false;
			ContentValues data = this.retrieveContentFromView(); 
			Log.i(TAG,"View Content: " + data.toString());
			data.put(AcdiVocaDbHelper.FINDS_PROJECT_ID, 0);
			
			if (mAction.equals(Intent.ACTION_EDIT)) { // Editing an existing beneficiary
				result = AcdiVocaFindDataManager.getInstance().updateFind(this, mFindId, data);
//				RadioButton bene = (RadioButton)findViewById(R.id.radio_yes_bene);
//				RadioButton agri = (RadioButton)findViewById(R.id.radio_yes_relative_participating_agri);
//				if(agri.isChecked()){
//					Intent intent = new Intent(this, AcdiVocaFindActivity.class);
//					intent.setAction(Intent.ACTION_INSERT);
//					intent.putExtra(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_AGRI);
//					intent.putExtra(AttributeManager.FINDS_RELATIVE_AGRI, AcdiVocaDbHelper.FINDS_YES);
//					startActivityForResult(intent, 0);
//				}
//				if(bene.isChecked()){
//					Intent intent = new Intent(this, AcdiVocaFindActivity.class);
//					intent.setAction(Intent.ACTION_INSERT);
//					intent.putExtra(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_MCHN);
//					intent.putExtra(AttributeManager.FINDS_RELATIVE_BENE, AcdiVocaDbHelper.FINDS_YES);
//					startActivityForResult(intent, 0);
//				}
				Log.i(TAG, "Update to Db is " + result);
				
			} else { // New beneficiary
				data.put(AcdiVocaDbHelper.FINDS_STATUS, AcdiVocaDbHelper.FINDS_STATUS_NEW);
				
				if (data.getAsInteger(AcdiVocaDbHelper.FINDS_TYPE).equals(AcdiVocaDbHelper.FINDS_TYPE_MCHN)){
					data.put(AcdiVocaDbHelper.FINDS_DOSSIER, AttributeManager.FINDS_BENE_DOSSIER);
				}
				if (data.getAsInteger(AcdiVocaDbHelper.FINDS_TYPE).equals(AcdiVocaDbHelper.FINDS_TYPE_AGRI))
					data.put(AcdiVocaDbHelper.FINDS_DOSSIER, AttributeManager.FINDS_AGRI_DOSSIER);
				if (data.getAsInteger(AcdiVocaDbHelper.FINDS_TYPE).equals(AcdiVocaDbHelper.FINDS_TYPE_BOTH))
					data.put(AcdiVocaDbHelper.FINDS_DOSSIER, AttributeManager.FINDS_BOTH_DOSSIER);
				
				result = AcdiVocaFindDataManager.getInstance().addNewFind(this, data);
				//if radioAgri is checked, make intent
				RadioButton agri = (RadioButton)findViewById(R.id.radio_yes_relative_participating_agri);
//				RadioButton bene = (RadioButton)findViewById(R.id.radio_yes_bene);
////				if(agri.isChecked()){
////					Intent intent = new Intent(this, AcdiVocaFindActivity.class);
//////					intent.setAction(Intent.ACTION_INSERT);
////					intent.putExtra(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_AGRI);
////					intent.putExtra(AttributeManager.FINDS_RELATIVE_AGRI, AcdiVocaDbHelper.FINDS_YES);
////					startActivityForResult(intent, 0);
////				}
//				if(bene.isChecked()){
//					Intent intent = new Intent(this, AcdiVocaFindActivity.class);
////					intent.setAction(Intent.ACTION_INSERT);
//					intent.putExtra(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_MCHN);
//					intent.putExtra(AttributeManager.FINDS_RELATIVE_BENE, AcdiVocaDbHelper.FINDS_YES);
//					startActivityForResult(intent, 0);
//				}
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
	public static void setSpinner(Spinner spinner, ContentValues contentValues, String attribute){
		String selected = contentValues.getAsString(attribute);
		int k = 0;
		if(selected != null){
			String item = (String) spinner.getItemAtPosition(k);
			while (k < spinner.getCount()-1 && !selected.equals(item)) {
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
