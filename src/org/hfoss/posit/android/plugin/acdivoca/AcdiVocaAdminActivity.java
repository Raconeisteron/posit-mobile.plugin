/*
 * File: TutorialActivity.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import org.hfoss.posit.android.AboutActivity;
import org.hfoss.posit.android.Log;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.SettingsActivity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;


/**
 * This class designed to make the user more comfortable with
 * posit the first time posit is opened. The user navigates through the tutorial
 * using previous, next, skip, and finish buttons.
 */

public class AcdiVocaAdminActivity extends Activity  {

	public static String TAG = "AdminActivity";
	public static final int MAX_BENEFICIARIES = 20000;  // Max readable
	public static final String COMMA= ",";
	
	private static final int FIELD_DOSSIER = 0;
	private static final int FIELD_LASTNAME = 1;
	private static final int FIELD_FIRSTNAME = 2;
	private static final int FIELD_SECTION = 3;
	private static final int FIELD_LOCALITY = 4;
	private static final int FIELD_ENTRY_DATE = 5;
	private static final int FIELD_BIRTH_DATE = 6;
	private static final int FIELD_SEX = 7;
	private static final int FIELD_CATEGORY = 8;
	private static final int FIELD_DISTRIBUTION_POST = 9;


	private ArrayAdapter<String> adapter;
	private String items[] = new String[100];

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		

	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume()");
		super.onResume();
		
		String localePref = PreferenceManager.getDefaultSharedPreferences(this).getString("locale", "");
		Log.i(TAG, "Locale = " + localePref);
		Locale locale = new Locale(localePref); 
		Locale.setDefault(locale);
		Configuration config = new Configuration();
		config.locale = locale;
		getBaseContext().getResources().updateConfiguration(config, null);

		setContentView(R.layout.acdivoca_admin);
	}	
	
	@Override
	protected void onPause() {
		Log.i(TAG, "onPause()");
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
		inflater.inflate(R.menu.acdi_voca_admin_menu, menu);
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
		case R.id.load_beneficiary_data:
			importBeneficiaryDataToDb();
			startActivity(new Intent(this, AboutActivity.class));
			break;
		}
		
		return true;
	}
	
	/**
	 * Reads data from a text file into the Db.
	 */
	private void importBeneficiaryDataToDb() {
		AcdiVocaDbHelper db = new AcdiVocaDbHelper(this);
		ContentValues values = new ContentValues();
		
		items = loadBeneficiaryData();
		String fields[] = null;
		Log.i(TAG, "Importing " + items.length + " Beneficiaries");
		Toast.makeText(this, "Importing " + items.length + " Beneficiaries", Toast.LENGTH_SHORT).show();
		for (int k = 0; k < items.length; k++) {
		//for (int k = 0; k < 5; k++) {
	
			fields = items[k].split(COMMA);
			values.put(AcdiVocaDbHelper.FINDS_DOSSIER,fields[FIELD_DOSSIER]);
			values.put(AcdiVocaDbHelper.FINDS_LASTNAME, fields[FIELD_LASTNAME]);
			values.put(AcdiVocaDbHelper.FINDS_FIRSTNAME, fields[FIELD_FIRSTNAME]);
			values.put(AcdiVocaDbHelper.FINDS_SEX, fields[FIELD_SEX]);         
			values.put(AcdiVocaDbHelper.FINDS_ADDRESS, fields[FIELD_LOCALITY]);
			values.put(AcdiVocaDbHelper.COMMUNE_SECTION_NAME, fields[FIELD_SECTION]);
			values.put(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY, fields[FIELD_CATEGORY]);
			values.put(AcdiVocaDbHelper.FINDS_DISTRIBUTION_POST, fields[FIELD_DISTRIBUTION_POST]);

			db.addNewFind(values);
		}
		Toast.makeText(this, "Imported " + items.length + " Beneficiaries", Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * Reads beneficiary data from a text file.  Currently the
	 * file name is hard coded as "beneficiaries.txt" and it is
	 * stored in the /assets folder.
	 * @return  Returns an array of Strings, each of which represents
	 * a Beneficiary record.
	 */
	private String[] loadBeneficiaryData() {
		String[] data = null;
		
		BufferedReader br = null;
		String line = null;
		int k = 0;
		
		try {
			InputStream iStream = this.getAssets().open("beneficiaries.txt");
			br = new BufferedReader(new InputStreamReader(iStream));
			data = new String[MAX_BENEFICIARIES];
			line = br.readLine();
			while (line != null)  {
				//Log.i(TAG, line);
				if (line.charAt(0) != '*')  {
					//data[k] = line.substring(0,line.indexOf(",")).trim();
					data[k] = line;
					k++;
				}
				line = br.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] dossiers = new String[k];  // Create the actual size of array
		for (int i= 0; i < k; i++)
			dossiers[i] = data[i];
		return dossiers;
	}



}
