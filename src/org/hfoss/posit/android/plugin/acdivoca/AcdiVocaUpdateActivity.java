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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.DatePicker.OnDateChangedListener;

/**
 * Handles Finds for AcdiVoca Mobile App.
 * 
 */
public class AcdiVocaUpdateActivity extends FindActivity implements OnDateChangedListener, 
    TextWatcher, OnItemSelectedListener { //, OnKeyListener {
    public static final String TAG = "AcdiVocaUpdateActivity";

    private static final int CONFIRM_EXIT = 0;

    private static final int ACTION_ID = 0;
    
    private String beneficiaryId = "unknown";

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
//        mDbHelper = new AcdiVocaDbHelper(this);
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
        Log.i(TAG, "onResume beneficiary id = " + beneficiaryId);
        
        AcdiVocaLocaleManager.setDefaultLocale(this);  // Locale Manager should be in API

        Log.i(TAG, "Before edited = " + isProbablyEdited);
        
        if (beneficiaryId == "unknown") {
            Intent lookupIntent = new Intent();
            lookupIntent.setClass(this, AcdiVocaLookupActivity.class);
            this.startActivityForResult(lookupIntent, ACTION_ID);
        } else {
            
            AcdiVocaDbHelper db = new AcdiVocaDbHelper(this);
            ContentValues values = db.fetchBeneficiaryByDossier(beneficiaryId, null);
            if (values == null) {
                Toast.makeText(this, "ERROR: No beneficiary with ID = " + beneficiaryId, Toast.LENGTH_SHORT).show();
            } else {
                setContentView(R.layout.acdivoca_update);  // Should be done after locale configuration
                displayContentInView(values);

                TextView tv = ((TextView) findViewById(R.id.dossier_label));
                tv.setText("Beneficiary Dossier: " + beneficiaryId);
                
                 // Listen for text changes in edit texts and set the isEdited flag
                ((EditText)findViewById(R.id.firstnameEdit)).addTextChangedListener(this);
                ((EditText)findViewById(R.id.lastnameEdit)).addTextChangedListener(this);
                ((EditText)findViewById(R.id.monthsInProgramEdit)).addTextChangedListener(this);
                
                 // Initialize the DatePicker and listen for changes
                 Calendar calendar = Calendar.getInstance();
                 
                 ((DatePicker)findViewById(R.id.datepicker)).init(
                         calendar.get(Calendar.YEAR),
                         calendar.get(Calendar.MONTH), 
                         calendar.get(Calendar.DAY_OF_MONTH), this);
                
                // Listen for clicks on radio buttons
                ((RadioButton)findViewById(R.id.femaleRadio)).setOnClickListener(this);
                ((RadioButton)findViewById(R.id.maleRadio)).setOnClickListener(this);
                ((RadioButton)findViewById(R.id.malnourishedRadio)).setOnClickListener(this);
                ((RadioButton)findViewById(R.id.inpreventionRadio)).setOnClickListener(this);
                ((RadioButton)findViewById(R.id.expectingRadio)).setOnClickListener(this);
                ((RadioButton)findViewById(R.id.nursingRadio)).setOnClickListener(this);
                
                
                ((Button)findViewById(R.id.update_to_db_button)).setOnClickListener(this);
                
// New Listeners - 6/17/11
                
                ((RadioButton)findViewById(R.id.radio_present_yes)).setOnClickListener(this);
                ((RadioButton)findViewById(R.id.radio_present_no)).setOnClickListener(this);
                ((RadioButton)findViewById(R.id.radio_change_in_status_yes)).setOnClickListener(this);
                ((RadioButton)findViewById(R.id.radio_change_in_status_no)).setOnClickListener(this);
                ((Spinner)findViewById(R.id.statuschangeSpinner)).setOnItemSelectedListener(this);
                
        

//        ((Button)findViewById(R.id.saveToDbButton)).setOnClickListener(this);
//        ((Button)findViewById(R.id.sendSmsButton)).setOnClickListener(this);
//        

//         ((RadioButton)findViewById(R.id.radio_motherleader_yes)).setOnClickListener(this);
//         ((RadioButton)findViewById(R.id.radio_motherleader_no)).setOnClickListener(this);
//         ((RadioButton)findViewById(R.id.radio_visit_yes)).setOnClickListener(this);
//         ((RadioButton)findViewById(R.id.radio_visit_no)).setOnClickListener(this);
//         ((RadioButton)findViewById(R.id.radio_yes_acdivoca)).setOnClickListener(this);
//         ((RadioButton)findViewById(R.id.radio_no_acdivoca)).setOnClickListener(this);
//
//         ((EditText)findViewById(R.id.addressEdit)).addTextChangedListener(this);
//         ((EditText)findViewById(R.id.ageEdit)).addTextChangedListener(this);
//         ((EditText)findViewById(R.id.inhomeEdit)).addTextChangedListener(this);
//         ((EditText)findViewById(R.id.responsibleIfChildEdit)).addTextChangedListener(this);
//         ((EditText)findViewById(R.id.fatherIfChildEdit)).addTextChangedListener(this);
//         ((EditText)findViewById(R.id.responsibleIfMotherEdit)).addTextChangedListener(this);
//         ((EditText)findViewById(R.id.husbandIfMotherEdit)).addTextChangedListener(this);
//         ((EditText)findViewById(R.id.give_name)).addTextChangedListener(this);
//         
//         
//         // These don't work
//         ((Spinner)findViewById(R.id.commune_sectionSpinner)).setOnItemSelectedListener(this);
//         ((Spinner)findViewById(R.id.communeSpinner)).setOnItemSelectedListener(this);
//         ((Spinner)findViewById(R.id.healthcenterSpinner)).setOnItemSelectedListener(this);
//         ((Spinner)findViewById(R.id.distributionSpinner)).setOnItemSelectedListener(this);
//
        final Intent intent = getIntent();
        mAction = intent.getAction();
        Log.i(TAG, "mAction = " + mAction);
        if (mAction.equals(Intent.ACTION_EDIT)) {
            doEditAction();
            isProbablyEdited = false; // In EDIT mode, initialize after filling in data
        }
         Log.i(TAG, "After edited = " + isProbablyEdited);
    }
}
    }
    
    
    /**
     * Returns the result of the Lookup Activity, which gets the beneficiary Id.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult");
        switch(requestCode) {
        case ACTION_ID:
            if (resultCode == RESULT_OK) {
                beneficiaryId = data.getStringExtra("Id");
                Toast.makeText(this, "Beneficiary Id = " + " " + beneficiaryId, Toast.LENGTH_LONG).show();
                break;
            } else {
                finish();
            }
        }   
    }
    
    /**
     * Allows editing of editable data for existing finds.  For existing finds, 
     * we retrieve the Find's data from the DB and display it in a TextView. The
     * Find's location and time stamp are not updated.
     */
//    private void doEditAction() {
//        Log.i(TAG, "doEditAction");
//
//        mFindId = (int) getIntent().getLongExtra(AcdiVocaDbHelper.FINDS_ID, 0); 
//        Log.i(TAG,"Find id = " + mFindId);
//
//        ContentValues values = AcdiVocaFindDataManager.getInstance().fetchFindDataById(this, mFindId, null);
//        displayContentInView(values);                        
//    }

    
    /**
     * Allows editing of editable data for existing finds.  For existing finds, 
     * we retrieve the Find's data from the DB and display it in a TextView. The
     * Find's location and time stamp are not updated.
     * 
     * 
     */    
    
    
    private void doEditAction() {
        Log.i(TAG, "doEditAction");
        AcdiVocaDbHelper db = new AcdiVocaDbHelper(this);
        ContentValues values = db.fetchBeneficiaryByDossier(beneficiaryId, null);
        
        Log.i(TAG, "################  Value of FindsID " + values.getAsString(AcdiVocaDbHelper.FINDS_ID));
//        mFindId = (int) getIntent().getLongExtra(values.getAsString(AcdiVocaDbHelper.FINDS_ID), 0);   
        mFindId = values.getAsInteger(AcdiVocaDbHelper.FINDS_ID);   
//        mFindId = Integer.parseInt(values.getAsString(AcdiVocaDbHelper.FINDS_ID));
 
        Log.i(TAG, "################  If the id is 0 something is wrong: " + mFindId);
//        Log.i(TAG,"FINDS_ID = " + AcdiVocaDbHelper.FINDS_ID);
//        mFindId = (int) getIntent().getLongExtra(AcdiVocaDbHelper.FINDS_ID, 0); //Getting default from here  
//        Integer.parseInt(db.AcdiVocaDbHelper.FINDS_ID);
//        mFindId = ben;
//        mFindId = Integer.parseInt(AcdiVocaDbHelper.FINDS_ID.substring(AcdiVocaDbHelper.FINDS_ID.indexOf(","))); //WARNING: USED TO BE LONG CAST INTO INT ,AcdiVocaDbHelper.FINDS_ID.lastIndexOf(" ")
//        Log.i(TAG,"Find id = " + mFindId);
//        AcdiVocaDbHelper db = new AcdiVocaDbHelper(this);
//        Log.i(TAG, db.fetchFindDataById(mFindId, null).toString()); //Need correct ID.
//        ContentValues values = db.fetchFindDataById(mFindId, null);
        
//        ContentValues values = (AcdiVocaFindDataManager.getInstance()).fetchFindDataById(this, mFindId, null);
        Log.i(TAG,"ContentValues has become");
        Log.i(TAG,values.toString());
        displayContentInView(values);                        
    }

    /**
     * Retrieves values from the View fields and stores them as <key,value> pairs in a ContentValues.
     * This method is invoked from the Save menu item.  It also marks the find 'unsynced'
     * so it will be updated to the server.
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
        
        eText = (EditText)findViewById(R.id.monthsInProgramEdit);
        value = eText.getText().toString();
        result.put(AcdiVocaDbHelper.FINDS_MONTHS_REMAINING, value);
        
//        
//        eText = (EditText)findViewById(R.id.ageEdit);
//        value = eText.getText().toString();
//        result.put(AcdiVocaDbHelper.FINDS_AGE, value);
        
        //value = mMonth + "/" + mDay + "/" + mYear;
        DatePicker picker = ((DatePicker)findViewById(R.id.datepicker));
        value = picker.getYear() + "/" + picker.getMonth() + "/" + picker.getDayOfMonth();
        Log.i(TAG, "Date = " + value);
        result.put(AcdiVocaDbHelper.FINDS_DOB, value);

//        value = ((DatePicker)findViewById(R.id.datepicker)).getMonth() + "/" +
//            ((DatePicker)findViewById(R.id.datepicker)).getDayOfMonth() + "/" +
//            ((DatePicker)findViewById(R.id.datepicker)).getYear();
        //Log.i(TAG, "retrieve DOB=" + value);

        RadioButton sexRB = (RadioButton)findViewById(R.id.femaleRadio);
        String sex = "";
        if (sexRB.isChecked()) 
            sex = AcdiVocaDbHelper.FINDS_FEMALE;
        sexRB = (RadioButton)findViewById(R.id.maleRadio);
        if (sexRB.isChecked()) 
            sex = AcdiVocaDbHelper.FINDS_MALE;
        result.put(AcdiVocaDbHelper.FINDS_SEX, sex);     
        
//        
//        eText = (EditText)findViewById(R.id.addressEdit);
//        value = eText.getText().toString();
//        result.put(AcdiVocaDbHelper.FINDS_ADDRESS, value);
//        
//        eText = (EditText)findViewById(R.id.inhomeEdit);
//        value = eText.getText().toString();
//        result.put(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE,value);
//        
//        Spinner communeSpinner = (Spinner)findViewById(R.id.communeSpinner);
//        value = (String)communeSpinner.getSelectedItem();
//        result.put(AcdiVocaDbHelper.COMMUNE_NAME, value);
//        
//        communeSpinner = (Spinner)findViewById(R.id.commune_sectionSpinner);
//        value = (String)communeSpinner.getSelectedItem();
//        result.put(AcdiVocaDbHelper.COMMUNE_SECTION_NAME, value);
//        

        // Set the Beneficiary's category (4 exclusive radio buttons)
        String category = "";
        RadioButton rb = (RadioButton)findViewById(R.id.malnourishedRadio);
        if (rb.isChecked()) 
            category = AcdiVocaDbHelper.FINDS_MALNOURISHED;
        rb = (RadioButton)findViewById(R.id.inpreventionRadio);
        if (rb.isChecked())
            category = AcdiVocaDbHelper.FINDS_PREVENTION;

        rb = (RadioButton)findViewById(R.id.expectingRadio);
        if (rb.isChecked()) 
            category = AcdiVocaDbHelper.FINDS_EXPECTING;
        rb = (RadioButton)findViewById(R.id.nursingRadio);
        if (rb.isChecked())
            category = AcdiVocaDbHelper.FINDS_NURSING;
        result.put(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY, category);

//        
//        Spinner spinner = (Spinner)findViewById(R.id.communeSpinner);
//        String commune = (String) spinner.getSelectedItem();
//        result.put(AcdiVocaDbHelper.COMMUNE_NAME, commune);
//        
//        spinner = (Spinner)findViewById(R.id.commune_sectionSpinner);
//        String communeSection = (String) spinner.getSelectedItem();
//        result.put(AcdiVocaDbHelper.COMMUNE_SECTION_NAME, communeSection);    
        
        
        RadioButton presentRB = (RadioButton)findViewById(R.id.radio_present_yes);
        String present = "";
        if (presentRB.isChecked()) 
            present = AcdiVocaDbHelper.FINDS_TRUE;
        presentRB = (RadioButton)findViewById(R.id.radio_present_no);
        if (presentRB.isChecked()) 
            present = AcdiVocaDbHelper.FINDS_FALSE;
        result.put(AcdiVocaDbHelper.FINDS_Q_PRESENT, present); 

// New button - 6/17/11        
        
        RadioButton changeRB = (RadioButton)findViewById(R.id.radio_change_in_status_yes);
        String change = "";
        if (changeRB.isChecked()) 
            change = AcdiVocaDbHelper.FINDS_TRUE;
        changeRB = (RadioButton)findViewById(R.id.radio_change_in_status_no);
        if (changeRB.isChecked()) 
            change = AcdiVocaDbHelper.FINDS_FALSE;
        result.put(AcdiVocaDbHelper.FINDS_Q_CHANGE, change); 
        
		String spinnerStr = "";
		Spinner spinner = (Spinner)findViewById(R.id.statuschangeSpinner);
		if (spinner != null) {
			spinnerStr = (String) spinner.getSelectedItem();
			result.put(AcdiVocaDbHelper.FINDS_CHANGE_TYPE, spinnerStr);
		}
        

//    	Old buttons - not in use - 6/17/11         
        
//        RadioButton transferRB = (RadioButton)findViewById(R.id.radio_transfer_yes);
//        String transfer = "";
//        if (transferRB.isChecked()) 
//            transfer = AcdiVocaDbHelper.FINDS_TRUE;
//        transferRB = (RadioButton)findViewById(R.id.radio_transfer_no);
//        if (transferRB.isChecked()) 
//            transfer = AcdiVocaDbHelper.FINDS_FALSE;
//        result.put(AcdiVocaDbHelper.FINDS_Q_TRANSFER, transfer); 
//        
//        RadioButton modRB = (RadioButton)findViewById(R.id.radio_modifications_yes);
//        String mod = "";
//        if (modRB.isChecked()) 
//            mod = AcdiVocaDbHelper.FINDS_TRUE;
//        modRB = (RadioButton)findViewById(R.id.radio_modifications_no);
//        if (modRB.isChecked()) 
//            mod = AcdiVocaDbHelper.FINDS_FALSE;
//        result.put(AcdiVocaDbHelper.FINDS_Q_MODIFICATION, mod); 
        
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

//        eText = (EditText)findViewById(R.id.ageEdit);
//        eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_AGE));
//        
//        eText = (EditText)findViewById(R.id.addressEdit);
//        eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_ADDRESS));
//        
//        eText = (EditText)findViewById(R.id.inhomeEdit);
//        eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE));
//        
        DatePicker dp = (DatePicker) findViewById(R.id.datepicker);
        String date = contentValues.getAsString(AcdiVocaDbHelper.FINDS_DOB);
        int yr=0, mon=0, day=0;
		day = Integer.parseInt(date.substring(date.lastIndexOf("/")+1));
		yr = Integer.parseInt(date.substring(0,date.indexOf("/")));
		mon = Integer.parseInt(date.substring(date.indexOf("/")+1,date.lastIndexOf("/")));
        
        Log.i(TAG, yr + "/" + mon + "/" + day);
//        mon = mon + 1;  // Months are number 0..11
//        day = day - 1;
        if (date != null) {
            Log.i(TAG,"display DOB = " + date);
            dp.init(yr, mon, day, this);
        }

        eText = (EditText)findViewById(R.id.monthsInProgramEdit);
        eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_MONTHS_REMAINING));

        
        // Chris - 6/9/11 - Filling the form            
        
        RadioButton sexRB = (RadioButton)findViewById(R.id.maleRadio);
        Log.i(TAG, "sex=" + contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX));
        if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX).equals(AcdiVocaDbHelper.FINDS_MALE.toString()))
            sexRB.setChecked(true);
        sexRB = (RadioButton)findViewById(R.id.femaleRadio);
        if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_SEX).equals(AcdiVocaDbHelper.FINDS_FEMALE.toString())){
            sexRB.setChecked(true);
        }
        
        RadioButton motherRB = (RadioButton) findViewById(R.id.expectingRadio);
        String cat = contentValues.getAsString(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY);
        if (cat != null){
        	if (cat.equals(AcdiVocaDbHelper.FINDS_EXPECTING.toString()))
        		motherRB.setChecked(true);
        	else
        		motherRB.setChecked(false);
        	motherRB = (RadioButton)findViewById(R.id.nursingRadio);
        	if (cat.equals(AcdiVocaDbHelper.FINDS_NURSING.toString()))
        		motherRB.setChecked(true);
        	else
        		motherRB.setChecked(false);
        	RadioButton infantRB = (RadioButton) findViewById(R.id.malnourishedRadio);
        	if (cat.equals(AcdiVocaDbHelper.FINDS_MALNOURISHED.toString()))
        		infantRB.setChecked(true);
        	else
        		motherRB.setChecked(false);
        	infantRB = (RadioButton)findViewById(R.id.inpreventionRadio);
        	if (cat.equals(AcdiVocaDbHelper.FINDS_PREVENTION.toString()))
        		infantRB.setChecked(true);
        	else
        		motherRB.setChecked(false);
        }
        else{
        	motherRB.setChecked(false);
        	motherRB = (RadioButton)findViewById(R.id.nursingRadio);
        	motherRB.setChecked(false);
        	motherRB = (RadioButton)findViewById(R.id.malnourishedRadio);
        	motherRB.setChecked(false);
        	motherRB = (RadioButton)findViewById(R.id.inpreventionRadio);
        	motherRB.setChecked(false);
        }
        
        RadioButton presentRB = (RadioButton) findViewById(R.id.radio_present_yes);
        
        if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_PRESENT) != null){
        
        if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_PRESENT).equals(AcdiVocaDbHelper.FINDS_TRUE.toString()))
            presentRB.setChecked(true);
        presentRB = (RadioButton) findViewById(R.id.radio_present_no);
        if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_PRESENT).equals(AcdiVocaDbHelper.FINDS_FALSE.toString()))
            presentRB.setChecked(true);
        }
 
    //  New button - 6/17/11          
        
        
        Spinner spinner = (Spinner)findViewById(R.id.statuschangeSpinner);
		AcdiVocaFindActivity.spinnerSetter(spinner, contentValues, AcdiVocaDbHelper.FINDS_CHANGE_TYPE);
        RadioButton changeRB = (RadioButton) findViewById(R.id.radio_change_in_status_yes);
        
		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_CHANGE) != null){
        
        if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_CHANGE).equals(AcdiVocaDbHelper.FINDS_TRUE.toString())){
			findViewById(R.id.statuschange).setVisibility(View.VISIBLE);
			findViewById(R.id.statuschangeSpinner).setVisibility(View.VISIBLE);
            changeRB.setChecked(true);
        }
        changeRB = (RadioButton) findViewById(R.id.radio_change_in_status_no);
        if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_CHANGE).equals(AcdiVocaDbHelper.FINDS_FALSE.toString())){
			findViewById(R.id.statuschange).setVisibility(View.GONE);
			findViewById(R.id.statuschangeSpinner).setVisibility(View.GONE);
            changeRB.setChecked(true);
        }
        }
		
		//Spinner spinner = (Spinner)findViewById(R.id.statuschangeSpinner);		
		
//		Spinner spinner = (Spinner)findViewById(R.id.statuschangeSpinner);
//		AcdiVocaFindActivity.spinnerSetter(spinner, contentValues, AcdiVocaDbHelper.FINDS_CHANGE_TYPE);
//		if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_CHANGE).equals(AcdiVocaDbHelper.FINDS_FALSE.toString())){
//			findViewById(R.id.statuschange).setVisibility(View.VISIBLE);
//			findViewById(R.id.statuschangeSpinner).setVisibility(View.VISIBLE);
//		}
        
    //  Old buttons - not in use - 6/17/11         
        
//      if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_TRANSFER) != null){
//      RadioButton transferRB = (RadioButton) findViewById(R.id.radio_transfer_yes);
//      if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_TRANSFER).equals(AcdiVocaDbHelper.FINDS_TRUE.toString()))
//          transferRB.setChecked(true);
//      transferRB = (RadioButton) findViewById(R.id.radio_transfer_no);
//      if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_TRANSFER).equals(AcdiVocaDbHelper.FINDS_FALSE.toString()))
//          transferRB.setChecked(true);
//      }
//      
//      if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_MODIFICATION) != null){
//      RadioButton modificationRB = (RadioButton) findViewById(R.id.radio_modifications_yes);
//      if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_MODIFICATION).equals(AcdiVocaDbHelper.FINDS_TRUE.toString()))
//          modificationRB.setChecked(true);
//      modificationRB = (RadioButton) findViewById(R.id.radio_modifications_no);
//      if (contentValues.getAsString(AcdiVocaDbHelper.FINDS_Q_MODIFICATION).equals(AcdiVocaDbHelper.FINDS_FALSE.toString()))
//          modificationRB.setChecked(true);
//      }
//        
//        Spinner spinner = (Spinner)findViewById(R.id.communeSpinner);
//        String selected = contentValues.getAsString(AcdiVocaDbHelper.COMMUNE_NAME);
//        int k = 0;
//        String item = (String) spinner.getItemAtPosition(k);
//        while (k < spinner.getCount() && !selected.equals(item)) {
//            ++k;
//            item = (String) spinner.getItemAtPosition(k);
//        }
//        spinner.setSelection(k);
//
//        spinner = (Spinner)findViewById(R.id.commune_sectionSpinner);
//        selected = contentValues.getAsString(AcdiVocaDbHelper.COMMUNE_SECTION_NAME);
//        k = 0;
//        item = (String) spinner.getItemAtPosition(k);
//        while (k < spinner.getCount() && !selected.equals(item)) {
//            ++k;
//            item = (String) spinner.getItemAtPosition(k);
//        }
//        spinner.setSelection(k);
        
        
        
            
        
        
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
        
//        if (id == R.id.expectingRadio || id == R.id.nursingRadio) {
//            RadioButton rb = (RadioButton) v;
//            findViewById(R.id.mchm).setVisibility(View.VISIBLE);
//            findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.VISIBLE);
//            findViewById(R.id.husbandIfMotherEdit).setVisibility(View.VISIBLE);
//            findViewById(R.id.responsibleIfChildEdit).setVisibility(View.INVISIBLE);
//            findViewById(R.id.fatherIfChildEdit).setVisibility(View.INVISIBLE);            
//            //Toast.makeText(AcdiVocaFindActivity.this, rb.getText(), Toast.LENGTH_SHORT).show();
//        } else if (id == R.id.malnourishedRadio || id == R.id.inpreventionRadio) {
//            RadioButton rb = (RadioButton) v;
//            findViewById(R.id.mchm).setVisibility(View.VISIBLE);
//            findViewById(R.id.responsibleIfChildEdit).setVisibility(View.VISIBLE);
//            findViewById(R.id.fatherIfChildEdit).setVisibility(View.VISIBLE);
//            findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.INVISIBLE);
//            findViewById(R.id.husbandIfMotherEdit).setVisibility(View.INVISIBLE);            
//            //Toast.makeText(AcdiVocaFindActivity.this, rb.getText(), Toast.LENGTH_SHORT).show();
//        } 
        
//New code for the spinner - 9/17/11        
        
        if (id == R.id.radio_change_in_status_yes){
        	findViewById(R.id.statuschange).setVisibility(View.VISIBLE);
			findViewById(R.id.statuschangeSpinner).setVisibility(View.VISIBLE);	
        }
        
        if (id == R.id.radio_change_in_status_no){
        	findViewById(R.id.statuschange).setVisibility(View.GONE);
			findViewById(R.id.statuschangeSpinner).setVisibility(View.GONE);	
        }
        
        
        if(v.getId()==R.id.update_to_db_button) {
            boolean result = false;
//            Toast.makeText(this, "SIMULATION: Saving update to DB", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "Saving update to DB", Toast.LENGTH_SHORT).show();

            ContentValues data = this.retrieveContentFromView(); 
            Log.i(TAG, "Retrieved = " + data.toString());
//Lines below originally commented out            
            
//            Log.i(TAG,"View Content: " + data.toString());
            data.put(AcdiVocaDbHelper.FINDS_PROJECT_ID, 0);
            Log.i(TAG, "################################ /n mAction = " + mAction);
            if (mAction.equals(Intent.ACTION_EDIT)) {
                result = AcdiVocaFindDataManager.getInstance().updateFind(this, mFindId, data);
                Log.i(TAG, "Update to Db is " + result);
            } else {
                result = AcdiVocaFindDataManager.getInstance().addNewFind(this, data);
                Log.i(TAG, "Save to Db is " + result);
            }
            
//Lines above originally commented out                
            
            if (result){
                Toast.makeText(this, "Find saved to Db " + data.toString(), Toast.LENGTH_SHORT).show();
            
            
            
//Testing with burnt toast            
//            Toast.makeText(this, "Find saved to Db " + data.toString(), Toast.LENGTH_LONG).show();    
//            Toast.makeText(this, "Find saved to Db " + data.toString(), Toast.LENGTH_LONG).show();    
//            Toast.makeText(this, "Find saved to Db " + data.toString(), Toast.LENGTH_LONG).show();    
//            Toast.makeText(this, "Find saved to Db " + data.toString(), Toast.LENGTH_LONG).show();    
            }
            
            else 
                Toast.makeText(this, "Db error", Toast.LENGTH_SHORT).show();

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
        if(keyCode==KeyEvent.KEYCODE_BACK && isProbablyEdited){  // 
            Toast.makeText(this, "Backkey isEdited=" +  isProbablyEdited, Toast.LENGTH_SHORT).show();
            

            
            
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
    public void onLocationChanged(Location location) {    }
    public void onProviderDisabled(String provider) {    }
    public void onProviderEnabled(String provider) {    }
    public void onStatusChanged(String provider, int status, Bundle extras) {    }


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
