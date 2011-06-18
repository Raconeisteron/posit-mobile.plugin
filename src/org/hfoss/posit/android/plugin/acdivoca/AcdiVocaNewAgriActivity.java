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
					result.put(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY, vals[4]);
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
		
		AcdiVocaLocaleManager.setDefaultLocale(this);  // Locale Manager should be in API

		Log.i(TAG, "Before edited = " + isProbablyEdited);
		setContentView(R.layout.acdivoca_agri_registration);  // Should be done after locale configuration

		mSaveButton = ((Button)findViewById(R.id.saveToDbButton));
		mSaveButton.setOnClickListener(this);
		((Button)findViewById(R.id.sendSmsButton)).setOnClickListener(this);
		
		// Listen for clicks on radio buttons
		 ((RadioButton)findViewById(R.id.femaleRadio)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.maleRadio)).setOnClickListener(this);
		 
		 
		 ((RadioButton)findViewById(R.id.radio_yes_bene)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.radio_no_bene)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.radio_yes_bene_same)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.radio_no_bene_same)).setOnClickListener(this);
		 
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
		 ((CheckBox)findViewById(R.id.coffeeCheckBox)).setOnClickListener(this);
		 
		 ((CheckBox)findViewById(R.id.houeCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.piocheCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.brouetteCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.machetteCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.serpetteCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.pelleCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.barreAMinesCheckBox)).setOnClickListener(this);

		 
		 ((CheckBox)findViewById(R.id.faoCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.saveCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.croseCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.mardnrCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.planCheckBox)).setOnClickListener(this);
		 ((CheckBox)findViewById(R.id.otherPartnerCheckBox)).setOnClickListener(this);	 
		 
		 
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
		 ((Spinner)findViewById(R.id.healthcenterSpinner)).setOnItemSelectedListener(this);
		 ((Spinner)findViewById(R.id.distributionSpinner)).setOnItemSelectedListener(this);
		 ((Spinner)findViewById(R.id.unitSpinner)).setOnItemSelectedListener(this);


		final Intent intent = getIntent();
		mAction = intent.getAction();
		if (mAction.equals(Intent.ACTION_EDIT)) {
			doEditAction();
			isProbablyEdited = false; // In EDIT mode, initialize after filling in data
			mSaveButton.setEnabled(false);
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
		String value = "";
		
		result.put(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_AGRI);
		
		EditText eText = (EditText) findViewById(R.id.lastnameEdit);
		value = eText.getText().toString();
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

		String sex = "";
		RadioButton sexRB = (RadioButton)findViewById(R.id.femaleRadio);
		if (sexRB != null && sexRB.isChecked()) 
			sex = AcdiVocaDbHelper.FINDS_FEMALE;
		sexRB = (RadioButton)findViewById(R.id.maleRadio);
		if (sexRB != null && sexRB.isChecked()) {
			sex = AcdiVocaDbHelper.FINDS_MALE;
		}
		result.put(AcdiVocaDbHelper.FINDS_SEX, sex); 
		
		String bene = "";
		RadioButton beneRB = (RadioButton)findViewById(R.id.radio_yes_bene_same);
		if (beneRB != null && beneRB.isChecked()) 
			bene = AcdiVocaDbHelper.FINDS_YES;
		beneRB = (RadioButton)findViewById(R.id.radio_no_bene_same);
		if (beneRB != null && beneRB.isChecked()) {
			bene = AcdiVocaDbHelper.FINDS_NO;
		}
		result.put(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_BENE_SAME, bene); 
		
		bene = "";
		beneRB = (RadioButton)findViewById(R.id.radio_yes_bene);
		if (beneRB != null && beneRB.isChecked()) 
			bene = AcdiVocaDbHelper.FINDS_YES;
		beneRB = (RadioButton)findViewById(R.id.radio_no_bene);
		if (beneRB != null && beneRB.isChecked()) {
			bene = AcdiVocaDbHelper.FINDS_NO;
		}
		result.put(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_BENE, bene); 
		
		//Add beneficiary checkbox values
		CheckBox beneCB = (CheckBox)findViewById(R.id.farmerCheckBox);
		int beneCtg  = 0;
		if (beneCB.isChecked()) {
			beneCtg += Math.pow(2, 0);
			result.put(AcdiVocaDbHelper.FINDS_IS_FARMER, true);			
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_IS_FARMER, false);			
		
		beneCB = (CheckBox)findViewById(R.id.musoCheckBox);
		if (beneCB.isChecked()){
			beneCtg += Math.pow(2, 1);
			result.put(AcdiVocaDbHelper.FINDS_IS_MUSO, true);	
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_IS_MUSO, false);

		
		beneCB = (CheckBox)findViewById(R.id.rancherCheckBox);
		if (beneCB.isChecked()){
			beneCtg += Math.pow(2, 2);
			result.put(AcdiVocaDbHelper.FINDS_IS_RANCHER, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_IS_RANCHER, false);
		
			
		beneCB = (CheckBox)findViewById(R.id.storeOwnerCheckBox);
		if (beneCB.isChecked()){
			beneCtg += Math.pow(2, 3);
			result.put(AcdiVocaDbHelper.FINDS_IS_STOREOWN, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_IS_STOREOWN, false);

		beneCB = (CheckBox)findViewById(R.id.fisherCheckBox);
		if (beneCB.isChecked()){
			beneCtg += Math.pow(2, 4);
			result.put(AcdiVocaDbHelper.FINDS_IS_FISHER, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_IS_FISHER, false);
		
		beneCB = (CheckBox)findViewById(R.id.artisanCheckBox);
		if (beneCB.isChecked()){
			beneCtg += Math.pow(2, 5);
			result.put(AcdiVocaDbHelper.FINDS_IS_ARTISAN, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_IS_ARTISAN, false);
		
		
		
		beneCB = (CheckBox)findViewById(R.id.otherCheckBox);
		if (beneCB.isChecked()){
			beneCtg += Math.pow(2, 6);
			result.put(AcdiVocaDbHelper.FINDS_IS_OTHER, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_IS_OTHER, false);
		
		
		eText = (EditText)findViewById(R.id.addressEdit);
		value = eText.getText().toString();
		result.put(AcdiVocaDbHelper.FINDS_ADDRESS, value);
		
		eText = (EditText)findViewById(R.id.inhomeEdit);
		value = eText.getText().toString();
		result.put(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE,value);
		
		// Add Seed types
		CheckBox seedCB = (CheckBox)findViewById(R.id.vegeCheckBox);
		int seedCtg  = 0;
		if (seedCB.isChecked()){
			seedCtg += Math.pow(2, 0);
			result.put(AcdiVocaDbHelper.FINDS_HAVE_VEGE, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_HAVE_VEGE, false);
		
		seedCB = (CheckBox)findViewById(R.id.cerealCheckBox);
		if (seedCB.isChecked()){
			seedCtg += Math.pow(2, 1);
			result.put(AcdiVocaDbHelper.FINDS_HAVE_CEREAL, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_HAVE_CEREAL, false);
		
		seedCB = (CheckBox)findViewById(R.id.tuberCheckBox);
		if (seedCB.isChecked()){
			seedCtg += Math.pow(2, 2);
			result.put(AcdiVocaDbHelper.FINDS_HAVE_TUBER, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_HAVE_TUBER, false);
		
		seedCB = (CheckBox)findViewById(R.id.treeCheckBox);
		if (seedCB.isChecked()){
			seedCtg += Math.pow(2, 3);
			result.put(AcdiVocaDbHelper.FINDS_HAVE_TREE, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_HAVE_TREE, false);
		
		seedCB = (CheckBox)findViewById(R.id.coffeeCheckBox);
		if (seedCB.isChecked()){
			seedCtg += Math.pow(2, 4);
			result.put(AcdiVocaDbHelper.FINDS_HAVE_COFFEE, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_HAVE_COFFEE, false);
		
		
		CheckBox partnerCB = (CheckBox)findViewById(R.id.faoCheckBox);
		int partner = 0;
		if (partnerCB.isChecked()){
			partner += Math.pow(2, 0);
			result.put(AcdiVocaDbHelper.FINDS_PARTNER_FAO, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_PARTNER_FAO, false);
		
		partnerCB = (CheckBox)findViewById(R.id.saveCheckBox);
		if (partnerCB.isChecked()){
			partner += Math.pow(2, 1);
			result.put(AcdiVocaDbHelper.FINDS_PARTNER_SAVE, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_PARTNER_SAVE, false);	
		
		partnerCB = (CheckBox)findViewById(R.id.croseCheckBox);
		if (partnerCB.isChecked()){
			partner += Math.pow(2, 2);
			result.put(AcdiVocaDbHelper.FINDS_PARTNER_CROSE, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_PARTNER_CROSE, false);	
		
		partnerCB = (CheckBox)findViewById(R.id.planCheckBox);
		if (partnerCB.isChecked()){
			partner += Math.pow(2, 3);
			result.put(AcdiVocaDbHelper.FINDS_PARTNER_PLAN, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_PARTNER_PLAN, false);	
		
		partnerCB = (CheckBox)findViewById(R.id.mardnrCheckBox);
		if (partnerCB.isChecked()){
			partner += Math.pow(2, 4);
			result.put(AcdiVocaDbHelper.FINDS_PARTNER_MARDNR, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_PARTNER_MARDNR, false);	
		
		partnerCB = (CheckBox)findViewById(R.id.otherPartnerCheckBox);
		if (partnerCB.isChecked()){
			partner += Math.pow(2, 5);
			result.put(AcdiVocaDbHelper.FINDS_PARTNER_OTHER, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_PARTNER_OTHER, false);	
		
		
		
		
		
		
		
		
		// ADD TOOLS
		int toolCtg  = 0;
		CheckBox toolCB = (CheckBox)findViewById(R.id.houeCheckBox);
		if (toolCB.isChecked()){
			toolCtg += Math.pow(2, 0);
			result.put(AcdiVocaDbHelper.FINDS_HAVE_HOUE, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_HAVE_HOUE, false);
		
		toolCB = (CheckBox)findViewById(R.id.piocheCheckBox);
		if (toolCB.isChecked()){
			toolCtg += Math.pow(2, 1);
			result.put(AcdiVocaDbHelper.FINDS_HAVE_PIOCHE, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_HAVE_PIOCHE, false);
		
		toolCB = (CheckBox)findViewById(R.id.brouetteCheckBox);
		if (toolCB.isChecked()){
			toolCtg += Math.pow(2, 2);
			result.put(AcdiVocaDbHelper.FINDS_HAVE_BROUETTE, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_HAVE_BROUETTE, false);
		
		toolCB = (CheckBox)findViewById(R.id.machetteCheckBox);
		if (toolCB.isChecked()){
			toolCtg += Math.pow(2, 3);
			result.put(AcdiVocaDbHelper.FINDS_HAVE_MACHETTE, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_HAVE_MACHETTE, false);
		
		toolCB = (CheckBox)findViewById(R.id.serpetteCheckBox);
		if (toolCB.isChecked()){
			toolCtg += Math.pow(2, 4);
			result.put(AcdiVocaDbHelper.FINDS_HAVE_SERPETTE, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_HAVE_SERPETTE, false);
		
		toolCB = (CheckBox)findViewById(R.id.pelleCheckBox);
		if (toolCB.isChecked()){
			toolCtg += Math.pow(2, 5);
			result.put(AcdiVocaDbHelper.FINDS_HAVE_PELLE, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_HAVE_PELLE, false);
		
		toolCB = (CheckBox)findViewById(R.id.barreAMinesCheckBox);
		if (toolCB.isChecked()){
			toolCtg += Math.pow(2, 6);
			result.put(AcdiVocaDbHelper.FINDS_HAVE_BARREAMINES, true);
		}
		else
			result.put(AcdiVocaDbHelper.FINDS_HAVE_BARREAMINES, false);
		
//		result.put(AcdiVocaDbHelper.FINDS_TOOL_CATAGORY, toolCtg);
		
		Spinner spinner = null;
//		String unit = "";
//		spinner = (Spinner)findViewById(R.id.unitSpinner);
//		unit = (String) spinner.getSelectedItem();
//		result.put(AcdiVocaDbHelper.FINDS_UNIT, unit);
//		
//		String health = "";
//		spinner = (Spinner)findViewById(R.id.healthcenterSpinner);
//		health = (String)spinner.getSelectedItem();
//		result.put(AcdiVocaDbHelper.FINDS_HEALTH_CENTER, health);
		
//		String distro = "";
//		spinner = (Spinner)findViewById(R.id.distributionSpinner);
//		distro = (String)spinner.getSelectedItem();
//		result.put(AcdiVocaDbHelper.FINDS_DISTRIBUTION_POST, distro);
		
		spinner = (Spinner)findViewById(R.id.commune_sectionSpinner);
		String communeSection = (String) spinner.getSelectedItem();
		result.put(AcdiVocaDbHelper.FINDS_COMMUNE_SECTION, communeSection);		
		
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
		
		eText = (EditText)findViewById(R.id.amount_of_land);
		eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_LAND_AMOUNT));
		
		eText = (EditText)findViewById(R.id.inhomeEdit);
		eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE));
		
//		eText = (EditText)findViewById(R.id.quantityEdit);
//		eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEED_AMOUNT));
		
		DatePicker dp = (DatePicker) findViewById(R.id.datepicker);
		String date = contentValues.getAsString(AcdiVocaDbHelper.FINDS_DOB);
		Log.i(TAG,"display DOB = " + date);
		dp.init(Integer.parseInt(date.substring(date.lastIndexOf("/")+1)), 
				Integer.parseInt(date.substring(0,date.indexOf("/"))),
				Integer.parseInt(date.substring(date.indexOf("/")+1,date.lastIndexOf("/"))),
				this);

		RadioButton sexRB = (RadioButton)findViewById(R.id.femaleRadio);
		Log.i(TAG, "sex=" + contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX));
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX).equals(AcdiVocaDbHelper.FINDS_FEMALE))
			sexRB.setChecked(true);
		sexRB = (RadioButton)findViewById(R.id.maleRadio);
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX).equals(AcdiVocaDbHelper.FINDS_MALE))
			sexRB.setChecked(true);
		
		RadioButton beneRB = (RadioButton)findViewById(R.id.radio_yes_bene);
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_BENE).equals(AcdiVocaDbHelper.FINDS_YES))
			beneRB.setChecked(true);
		beneRB = (RadioButton)findViewById(R.id.radio_no_bene);
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_BENE).equals(AcdiVocaDbHelper.FINDS_NO))
			beneRB.setChecked(true);
		
		beneRB = (RadioButton)findViewById(R.id.radio_yes_bene_same);
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_BENE_SAME).equals(AcdiVocaDbHelper.FINDS_YES))
			beneRB.setChecked(true);
		beneRB = (RadioButton)findViewById(R.id.radio_no_bene_same);
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_BENE_SAME).equals(AcdiVocaDbHelper.FINDS_NO))
			beneRB.setChecked(true);

		// HANDLE CHECKBOX NEEDS TO FIND A BETTER WAY
		CheckBox aCheckBox = (CheckBox)findViewById(R.id.farmerCheckBox);
        Log.i(TAG, "isFarmer = " +  contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_IS_FARMER).toString());
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_IS_FARMER) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.musoCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_IS_MUSO) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.rancherCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_IS_RANCHER) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.storeOwnerCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_IS_STOREOWN) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.fisherCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_IS_FISHER) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.artisanCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_IS_ARTISAN) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.otherCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_IS_OTHER) == 1);
		
		aCheckBox = (CheckBox)findViewById(R.id.barreAMinesCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_HAVE_BARREAMINES) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.vegeCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_HAVE_VEGE) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.cerealCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_HAVE_CEREAL) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.tuberCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_HAVE_TUBER) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.treeCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_HAVE_TREE) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.coffeeCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_HAVE_COFFEE) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.houeCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_HAVE_HOUE) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.piocheCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_HAVE_PIOCHE) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.brouetteCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_HAVE_BROUETTE) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.machetteCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_HAVE_MACHETTE) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.serpetteCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_HAVE_SERPETTE) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.pelleCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_HAVE_PELLE) == 1);
		
		aCheckBox = (CheckBox)findViewById(R.id.faoCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_PARTNER_FAO) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.saveCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_PARTNER_SAVE) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.croseCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_PARTNER_CROSE) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.planCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_PARTNER_PLAN) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.mardnrCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_PARTNER_MARDNR) == 1);
		aCheckBox = (CheckBox)findViewById(R.id.otherPartnerCheckBox);
		aCheckBox.setChecked(contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_PARTNER_OTHER) == 1);
		
		
		
		Log.i(TAG, "display Beneficiary Catagory=" + contentValues.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY));

		
		//		int test = Integer.parseInt(contentValues.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY));
//		if (test >= 32){
//			otherCB.setChecked(true);
//			test -= 32;
//		}
//		if (test >= 16){
//			fisherCB.setChecked(true);
//			test -= 16;
//		}if (test >= 8){
//			storeCB.setChecked(true);
//			test -= 8;
//		}if (test >= 4){
//			rancherCB.setChecked(true);
//			test -= 4;
//		}if (test >= 2){
//			musoCB.setChecked(true);
//			test -= 2;
//		}if (test >= 1){
//			farmerCB.setChecked(true);
//			test -= 1;
//	}		
		Spinner spinner = (Spinner)findViewById(R.id.commune_sectionSpinner);
		AcdiVocaFindActivity.spinnerSetter(spinner, contentValues, AcdiVocaDbHelper.FINDS_COMMUNE_SECTION);
		
		
		
//	
//	Spinner spinner = (Spinner)findViewById(R.id.healthcenterSpinner);
//	AcdiVocaFindActivity.spinnerSetter(spinner, contentValues, AcdiVocaDbHelper.FINDS_HEALTH_CENTER);
//    spinner = (Spinner)findViewById(R.id.distributionSpinner);
//	AcdiVocaFindActivity.spinnerSetter(spinner, contentValues, AcdiVocaDbHelper.FINDS_DISTRIBUTION_POST);
//	spinner = (Spinner)findViewById(R.id.unitSpinner);	
//	AcdiVocaFindActivity.spinnerSetter(spinner, contentValues, AcdiVocaDbHelper.FINDS_UNIT);
	
//		String selected = contentValues.getAsString(AcdiVocaDbHelper.COMMUNE_NAME);
//		int k = 0;
//		String item = (String) spinner.getItemAtPosition(k);
//		while (k < spinner.getCount() && !selected.equals(item)) {
//			++k;
//			item = (String) spinner.getItemAtPosition(k);
//		}
//		spinner.setSelection(k);
//
//		spinner = (Spinner)findViewById(R.id.commune_sectionSpinner);
//		selected = contentValues.getAsString(AcdiVocaDbHelper.COMMUNE_SECTION_NAME);
//		k = 0;
//		item = (String) spinner.getItemAtPosition(k);
//		while (k < spinner.getCount() && !selected.equals(item)) {
//			++k;
//			item = (String) spinner.getItemAtPosition(k);
//		}
//		spinner.setSelection(k);
		
	}

	/**
	 * Required as part of OnClickListener interface. Handles button clicks.
	 */
	public void onClick(View v) {
		Log.i(TAG, "onClick");
		// If a RadioButton was clicked, mark the form as edited.
		//Toast.makeText(this, "Clicked on a " + v.getClass().toString(), Toast.LENGTH_SHORT).show();
		try {
			if (v.getClass().equals(Class.forName("android.widget.CheckBox"))) {
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
		// TODO:  Edit this case
		if(v.getId()==R.id.saveToDbButton) {
			boolean result = false;
			ContentValues data = this.retrieveContentFromView(); 
			Log.i(TAG,"View Content: " + data.toString());
			data.put(AcdiVocaDbHelper.FINDS_PROJECT_ID, 0);
			if (mAction.equals(Intent.ACTION_EDIT)) {
				result = AcdiVocaFindDataManager.getInstance().updateFind(this, mFindId, data);
				Log.i(TAG, "Update to Db is " + result);
				RadioButton bene = (RadioButton)findViewById(R.id.radio_yes_bene);
				RadioButton same = (RadioButton)findViewById(R.id.radio_yes_bene_same);
				if(same.isChecked()){
					Intent intent = new Intent(this, AcdiVocaFindActivity.class);
					intent.setAction(Intent.ACTION_INSERT);
					startActivityForResult(intent, 0);
				}
				if(bene.isChecked()){
					Intent intent = new Intent(this, AcdiVocaNewAgriActivity.class);
					intent.setAction(Intent.ACTION_INSERT);
					startActivityForResult(intent, 0);
				}
			} else {
				data.put(AcdiVocaDbHelper.FINDS_DOSSIER, "New Agri");
				result = AcdiVocaFindDataManager.getInstance().addNewFind(this, data);
				RadioButton bene = (RadioButton)findViewById(R.id.radio_yes_bene);
				RadioButton same = (RadioButton)findViewById(R.id.radio_yes_bene_same);
				if(same.isChecked()){
					Intent intent = new Intent(this, AcdiVocaFindActivity.class);
					intent.setAction(Intent.ACTION_INSERT);
					startActivityForResult(intent, 0);
				}
				if(bene.isChecked()){
					Intent intent = new Intent(this, AcdiVocaNewAgriActivity.class);
					intent.setAction(Intent.ACTION_INSERT);
					startActivityForResult(intent, 0);
				}
				Log.i(TAG, "Save to Db is " + result);
			}
			if (result){
				Toast.makeText(this, "Saved to Db", Toast.LENGTH_LONG).show();
			}
			else 
				Toast.makeText(this, "Db error", Toast.LENGTH_SHORT).show();
			//this.startActivity(new Intent().setClass(this,AcdiVocaListFindsActivity.class));
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