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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Locale;

import org.hfoss.posit.android.AboutActivity;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.FindActivity;
import org.hfoss.posit.android.api.SettingsActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.gsm.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.DatePicker.OnDateChangedListener;

/**
 * Handles Finds for AcdiVoca Mobile App.
 * 
 */
public class AcdiVocaLookupActivity extends Activity implements OnClickListener, TextWatcher {
	public static final String TAG = "AcdiVocaLookupActivity";

	private Spinner lookupSpinner;
	private ArrayAdapter<String> mAdapter;
	private String dossiers[] = new String[100];
	private EditText eText;
	
	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		Log.i(TAG, PreferenceManager.getDefaultSharedPreferences(this).getAll().toString());
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		super.onPause();
	}

	
	/**
	 * Creates the menu options.
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.acdi_voca_lookup_menu, menu);
		return true;
	}
	
	/**
	 * Manages the selection of menu items.
	 * 
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings_menu_item:
			startActivity(new Intent(this, SettingsActivity.class));
			break;
		}
		
		return true;
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

		setContentView(R.layout.acdivoca_lookup);  // Should be done after locale configuration

		((Button)findViewById(R.id.update_lookup_button)).setOnClickListener(this);
		((Button)findViewById(R.id.cancel_lookup_button)).setOnClickListener(this);
		lookupSpinner = ((Spinner)findViewById(R.id.lookupSpinner));

		// See http://mylifewithandroid.blogspot.com/2009/10/spinner-and-its-data-behind.html
		//final String items[] = new String[100];
		//	    for (int k = 0; k < items.length; k++) 
		//	    	items[k] = "Str" + k;

		AcdiVocaDbHelper db = new AcdiVocaDbHelper(this);
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String distrKey = this.getResources().getString(R.string.distribution_point);
		String distributionCtr = sharedPrefs.getString(distrKey, "");
		Log.i(TAG, distrKey +"="+ distributionCtr);
		
		((TextView)findViewById(R.id.distribution_label)).setText(distributionCtr);

		dossiers = db.fetchAllBeneficiaryIdsByDistributionSite(distributionCtr);
		
		if (dossiers == null) {
			Toast.makeText(this, "Sorry, there are no beneficiaries in the Db.", Toast.LENGTH_SHORT).show();
			dossiers = new String[1];
			dossiers[0] = "No beneficiaries found";
			((Button)findViewById(R.id.update_lookup_button)).setEnabled(false);
		} 
		setUpSpinnerAdapter(dossiers);
	}
	
	private void setUpSpinnerAdapter(final String[] data) {
		mAdapter = 
			new ArrayAdapter<String>(
					this,
					android.R.layout.simple_spinner_item,
					data );
		mAdapter.sort(String.CASE_INSENSITIVE_ORDER);
		mAdapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		lookupSpinner.setAdapter(mAdapter);
		lookupSpinner.setOnItemSelectedListener(
				new AdapterView.OnItemSelectedListener() {
					public void onItemSelected(
							AdapterView<?> parent, 
							View view, 
							int position, 
							long id) {
						String d = data[position];

						//eText.setText(d);
					}

					public void onNothingSelected(AdapterView<?> parent) {
					}
				}
		);
		eText = ((EditText)findViewById(R.id.dossierEdit));
		eText.addTextChangedListener(this);
		eText.setText(""); 
	}
	
	/**
	 * Required as part of OnClickListener interface. Handles button clicks.
	 */
	public void onClick(View v) {
		Log.i(TAG, "onClick");
	    Intent returnIntent = new Intent();
	
		if (v.getId() == R.id.update_lookup_button) {
			String id = (String)lookupSpinner.getSelectedItem();
//			EditText etext = ((EditText)findViewById(R.id.dossierEdit));
//			String id = etext.getText().toString();
			returnIntent.putExtra("Id",id);
			setResult(RESULT_OK,returnIntent); 
			Toast.makeText(this, "Id= " + id, Toast.LENGTH_SHORT).show();
		} else {
			setResult(this.RESULT_CANCELED, returnIntent);
		}
	    finish();
	}


	public void afterTextChanged(Editable s) {
		// TODO Auto-generated method stub
		
	}


	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub
		
	}


	public void onTextChanged(CharSequence s, int start, int before, int count) {
		int k = 0;
		String prefix = s.toString();
		Log.i(TAG, "Prefix = " + prefix);
		String item = dossiers[k];
		while (!item.startsWith(prefix.toUpperCase()) && k < dossiers.length) {
			k += 1;
			if (k < dossiers.length)
				item = dossiers[k];
		}
		Log.i(TAG, "onTextChanged " + prefix + " " + k);
		if (k < dossiers.length)
			lookupSpinner.setSelection(k);				
	}
}