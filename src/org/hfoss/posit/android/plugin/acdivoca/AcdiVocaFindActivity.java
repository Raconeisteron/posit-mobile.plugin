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
		 //added Jun17
		 ((RadioButton)findViewById(R.id.radio_yes_relative_acdivoca)).setOnClickListener(this);
		 ((RadioButton)findViewById(R.id.radio_no_relative_acdivoca)).setOnClickListener(this);
		 //added from agri
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
		 ((EditText)findViewById(R.id.responsibleIfChildEdit)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.responsibleIfMotherEdit)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.give_name)).addTextChangedListener(this);
		 //added for agri
		 ((EditText)findViewById(R.id.amount_of_land)).addTextChangedListener(this);
		 ((EditText)findViewById(R.id.quantityEdit)).addTextChangedListener(this);

		 // Initialize the DatePicker and listen for changes
		 Calendar calendar = Calendar.getInstance();
		 
		 ((DatePicker)findViewById(R.id.datepicker)).init(
				 calendar.get(Calendar.YEAR),
				 calendar.get(Calendar.MONTH), 
				 calendar.get(Calendar.DAY_OF_MONTH), this);

		 ((Spinner)findViewById(R.id.healthcenterSpinner)).setOnItemSelectedListener(this);
		 ((Spinner)findViewById(R.id.distributionSpinner)).setOnItemSelectedListener(this);
		 ((Spinner)findViewById(R.id.unitSpinner)).setOnItemSelectedListener(this);

		final Intent intent = getIntent();
		mAction = intent.getAction();
		if (mAction.equals(Intent.ACTION_EDIT)) {
			int type = intent.getIntExtra(AcdiVocaDbHelper.FINDS_TYPE, 0);
			if (type == AcdiVocaDbHelper.FINDS_TYPE_MCHN){
				findViewById(R.id.agriPart).setVisibility(View.GONE);
				findViewById(R.id.participating_acdivoca).setVisibility(View.GONE);
				findViewById(R.id.radio_participating_acdivoca).setVisibility(View.GONE);
			}
			if (type == AcdiVocaDbHelper.FINDS_TYPE_AGRI){
				findViewById(R.id.mchnPart).setVisibility(View.GONE);
				findViewById(R.id.participating_bene_same).setVisibility(View.GONE);
				findViewById(R.id.radio_same_bene).setVisibility(View.GONE);
			}
			if (type == AcdiVocaDbHelper.FINDS_TYPE_BOTH){
				findViewById(R.id.mchnPart).setVisibility(View.VISIBLE);
				findViewById(R.id.agriPart).setVisibility(View.VISIBLE);
				findViewById(R.id.participating_acdivoca).setVisibility(View.GONE);
				findViewById(R.id.radio_participating_acdivoca).setVisibility(View.GONE);
				findViewById(R.id.participating_bene_same).setVisibility(View.GONE);
				findViewById(R.id.radio_same_bene).setVisibility(View.GONE);
			}
			
			doEditAction();
			isProbablyEdited = false; // In EDIT mode, initialize after filling in data
			mSaveButton.setEnabled(false);
		}
		
		if (mAction.equals(Intent.ACTION_INSERT)){
			Log.i(TAG,"############################################");
			Log.i(TAG,"you are now in insert");
			if(intent.getExtras() != null){
				int type = intent.getIntExtra(AcdiVocaDbHelper.FINDS_TYPE, 0);
				if (type == AcdiVocaDbHelper.FINDS_TYPE_MCHN){
					findViewById(R.id.agriPart).setVisibility(View.GONE);
				}
				if (type == AcdiVocaDbHelper.FINDS_TYPE_AGRI){
					findViewById(R.id.mchnPart).setVisibility(View.GONE);					
				}
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
					findViewById(R.id.participating_acdivoca).setVisibility(View.GONE);
					findViewById(R.id.radio_participating_acdivoca).setVisibility(View.GONE);
				}
				}
			}
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
		// HIDE THE LAST QUESTION FOR EACH FORM
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
		// ADDING TYPE
		final Intent intent = getIntent();
		int x = intent.getIntExtra(AcdiVocaDbHelper.FINDS_TYPE, 0);
		
		if (x == AcdiVocaDbHelper.FINDS_TYPE_MCHN)
			result.put(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_MCHN);
		
		if (x == AcdiVocaDbHelper.FINDS_TYPE_AGRI)
			result.put(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_AGRI);
		
		RadioButton brb = (RadioButton)findViewById(R.id.radio_yes_acdivoca);
		RadioButton arb = (RadioButton)findViewById(R.id.radio_yes_bene_same);
		if(brb.isChecked() || arb.isChecked()){
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
		RadioButton acdiAgriRB = (RadioButton)findViewById(R.id.radio_yes_acdivoca);
		if (acdiAgriRB != null && acdiAgriRB.isChecked()) {
			acdiAgri = true;
		}
		acdiAgriRB = (RadioButton)findViewById(R.id.radio_no_acdivoca);
		if (acdiAgriRB != null && acdiAgriRB.isChecked()) {
			acdiAgri = false;
		}
		result.put(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_AGRI, acdiAgri);   

		Boolean acdiAgriRelative = false;		
		RadioButton acdiAgriSameRB = (RadioButton)findViewById(R.id.radio_yes_relative_acdivoca);
		if (acdiAgriSameRB != null && acdiAgriSameRB.isChecked()) {
			acdiAgriRelative = true;
		}
		acdiAgriSameRB = (RadioButton)findViewById(R.id.radio_no_relative_acdivoca);
		if (acdiAgriSameRB != null && acdiAgriSameRB.isChecked()) {
			acdiAgriRelative = false;
		}
		result.put(AcdiVocaDbHelper.FINDS_Q_RELATIVE_AGRI, acdiAgriRelative);
		
		/* *******************************
		 * *HERE BEGINS AGRI FORM STORAGE*
		 * *******************************
		 */
		
		// AGRI FORM CATAGORY
		
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
		
		// PARTNER CHECKBOXES
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
		
		
		// ADDING AGRI QUESTIONS
		Boolean bene = false;
		RadioButton beneRB = (RadioButton)findViewById(R.id.radio_yes_bene_same);
		if (beneRB != null && beneRB.isChecked()) 
			bene = true;
		beneRB = (RadioButton)findViewById(R.id.radio_no_bene_same);
		if (beneRB != null && beneRB.isChecked()) {
			bene = false;
		}
		result.put(AcdiVocaDbHelper.FINDS_Q_RELATIVE_BENE, bene); 
		
		bene = false;
		beneRB = (RadioButton)findViewById(R.id.radio_yes_bene);
		if (beneRB != null && beneRB.isChecked()) 
			bene = true;
		beneRB = (RadioButton)findViewById(R.id.radio_no_bene);
		if (beneRB != null && beneRB.isChecked()) {
			bene = false;
		}
		result.put(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_BENE, bene); 
		
		return result;
	}
	

	/**
	 * Displays values from a ContentValues in the View.
	 * @param contentValues stores <key, value> pairs
	 */
	private void displayContentInView(ContentValues contentValues) {
		Log.i(TAG, "displayContentInView");

		if (contentValues != null){
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
			spinnerSetter(spinner, contentValues, AcdiVocaDbHelper.FINDS_DISTRIBUTION_POST);

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
			aRadioButton = (RadioButton)findViewById(R.id.radio_yes_acdivoca);
			Log.i(TAG, "acdiAgri=" + contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_AGRI));
			if (value != null) {
				if (value.equals(1))
					aRadioButton.setChecked(true);
				else 
					aRadioButton.setChecked(false);
				aRadioButton  = (RadioButton)findViewById(R.id.radio_no_acdivoca);
				if (value.equals(0)){
					aRadioButton.setChecked(true);
				} else {
					aRadioButton.setChecked(false);

				}
			}

			//added Jun 17
			value = contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_Q_RELATIVE_AGRI);
			aRadioButton = (RadioButton)findViewById(R.id.radio_yes_relative_acdivoca);
			Log.i(TAG, "acdiAgri=" + contentValues.getAsInteger(AcdiVocaDbHelper.FINDS_Q_RELATIVE_AGRI));
			if (value != null) {
				if (value.equals(1))
					aRadioButton.setChecked(true);
				else 
					aRadioButton.setChecked(false);
				aRadioButton  = (RadioButton)findViewById(R.id.radio_no_relative_acdivoca);
				if (value.equals(0)){
					aRadioButton.setChecked(true);
				} else {
					aRadioButton.setChecked(false);

				}
			}


			/** *********************************
			 *  * BELOW DISPLAY AGRI FORM ITEMS *
			 *  *********************************
			 */

			// DISPLAY COMMUNE SECTION
			spinner = (Spinner)findViewById(R.id.commune_sectionSpinner);
			spinnerSetter(spinner, contentValues, AcdiVocaDbHelper.FINDS_COMMUNE_SECTION);


			RadioButton beneRB = (RadioButton)findViewById(R.id.radio_yes_bene);
			val = contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_PARTICIPATING_BENE);
			if (val != null){
				if (val.equals(AcdiVocaDbHelper.FINDS_YES))
					beneRB.setChecked(true);
				beneRB = (RadioButton)findViewById(R.id.radio_no_bene);
				if (val.equals(AcdiVocaDbHelper.FINDS_NO))
					beneRB.setChecked(true);

				beneRB = (RadioButton)findViewById(R.id.radio_yes_bene_same);
				if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_RELATIVE_BENE).equals(AcdiVocaDbHelper.FINDS_YES))
					beneRB.setChecked(true);
				beneRB = (RadioButton)findViewById(R.id.radio_no_bene_same);
				if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_RELATIVE_BENE).equals(AcdiVocaDbHelper.FINDS_NO))
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

			}


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
		
		if (id == R.id.radio_no_acdivoca){
			findViewById(R.id.agriPart).setVisibility(View.GONE);
			findViewById(R.id.relative_participating_acdivoca).setVisibility(View.VISIBLE);
			findViewById(R.id.radio_relative_acdivoca).setVisibility(View.VISIBLE);
		}
		
		if (id == R.id.radio_yes_acdivoca){
			findViewById(R.id.relative_participating_acdivoca).setVisibility(View.GONE);
			findViewById(R.id.radio_relative_acdivoca).setVisibility(View.GONE);
			RadioButton rb = (RadioButton)findViewById(R.id.radio_yes_relative_acdivoca);
			rb.setChecked(false);

			findViewById(R.id.agriPart).setVisibility(View.VISIBLE);
			findViewById(R.id.commune_sectionSpinner).setFocusable(true);
			findViewById(R.id.commune_sectionSpinner).setFocusableInTouchMode(true);			
			findViewById(R.id.commune_sectionSpinner).requestFocus();
			findViewById(R.id.participating_bene_same).setVisibility(View.GONE);
			findViewById(R.id.radio_same_bene).setVisibility(View.GONE);
		}
		
		if (id == R.id.radio_no_bene_same){
			findViewById(R.id.mchnPart).setVisibility(View.GONE);
			findViewById(R.id.participating_bene).setVisibility(View.VISIBLE);
			findViewById(R.id.radio_participating_bene).setVisibility(View.VISIBLE);
		}
		
		if (id == R.id.radio_yes_bene_same){
			findViewById(R.id.participating_bene).setVisibility(View.GONE);
			findViewById(R.id.radio_participating_bene).setVisibility(View.GONE);
			RadioButton rb = (RadioButton)findViewById(R.id.radio_yes_bene);
			rb.setChecked(false);
			findViewById(R.id.mchnPart).setVisibility(View.VISIBLE);
			findViewById(R.id.distributionSpinner).setFocusable(true);
			findViewById(R.id.distributionSpinner).setFocusableInTouchMode(true);			
			findViewById(R.id.distributionSpinner).requestFocus();
			findViewById(R.id.participating_acdivoca).setVisibility(View.GONE);
			findViewById(R.id.radio_participating_acdivoca).setVisibility(View.GONE);
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
			if (mAction.equals(Intent.ACTION_EDIT)) {
				result = AcdiVocaFindDataManager.getInstance().updateFind(this, mFindId, data);
				RadioButton bene = (RadioButton)findViewById(R.id.radio_yes_bene);
				RadioButton agri = (RadioButton)findViewById(R.id.radio_yes_relative_acdivoca);
				if(agri.isChecked()){
					Intent intent = new Intent(this, AcdiVocaFindActivity.class);
					intent.setAction(Intent.ACTION_INSERT);
					intent.putExtra(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_AGRI);
					intent.putExtra(AttributeManager.FINDS_RELATIVE_AGRI, AcdiVocaDbHelper.FINDS_YES);
					startActivityForResult(intent, 0);
				}
				if(bene.isChecked()){
					Intent intent = new Intent(this, AcdiVocaFindActivity.class);
					intent.setAction(Intent.ACTION_INSERT);
					intent.putExtra(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_MCHN);
					intent.putExtra(AttributeManager.FINDS_RELATIVE_BENE, AcdiVocaDbHelper.FINDS_YES);
					startActivityForResult(intent, 0);
				}
				Log.i(TAG, "Update to Db is " + result);
			} else {
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
				RadioButton agri = (RadioButton)findViewById(R.id.radio_yes_relative_acdivoca);
				RadioButton bene = (RadioButton)findViewById(R.id.radio_yes_bene);
				if(agri.isChecked()){
					Intent intent = new Intent(this, AcdiVocaFindActivity.class);
					intent.setAction(Intent.ACTION_INSERT);
					intent.putExtra(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_AGRI);
					intent.putExtra(AttributeManager.FINDS_RELATIVE_AGRI, AcdiVocaDbHelper.FINDS_YES);
					startActivityForResult(intent, 0);
				}
				if(bene.isChecked()){
					Intent intent = new Intent(this, AcdiVocaFindActivity.class);
					intent.setAction(Intent.ACTION_INSERT);
					intent.putExtra(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_MCHN);
					intent.putExtra(AttributeManager.FINDS_RELATIVE_BENE, AcdiVocaDbHelper.FINDS_YES);
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
