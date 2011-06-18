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
import org.hfoss.posit.android.Log;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.FindActivityProvider;
import org.hfoss.posit.android.api.SettingsActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
	public static final int DONE = 0;



	private ArrayAdapter<String> adapter;
	private String items[] = new String[100];
	private ProgressDialog mProgressDialog;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		

	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume()");
		super.onResume();
		
		AcdiVocaLocaleManager.setDefaultLocale(this);  // Locale Manager should be in API

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
			Intent intent = new Intent();
			Class<Activity> loginActivity = FindActivityProvider.getLoginActivityClass();
			if (loginActivity != null) {
				intent.setClass(this, loginActivity);
				intent.putExtra(AcdiVocaDbHelper.USER_TYPE_STRING, AcdiVocaDbHelper.UserType.SUPER.ordinal());
				this.startActivityForResult(intent, LoginActivity.ACTION_LOGIN);
			}
			
			
			//startActivity(new Intent(this, AboutActivity.class));
			break;
		}
		
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG,"onActivityResult = " + resultCode);
		switch (requestCode) {
		case LoginActivity.ACTION_LOGIN:
			if (resultCode == RESULT_OK) {
				//Toast.makeText(this, "Thank you", Toast.LENGTH_LONG).show();
				//findViewById(R.id.fileload_progressbar).setVisibility(View.VISIBLE);
				
				mProgressDialog = ProgressDialog.show(this, "Loading data",
						"Please wait.", true, true);
				
				ImportDataThread thread = new ImportDataThread(this, new ImportThreadHandler());
				thread.start();
				
				
				//findViewById(R.id.fileload_progressbar).setVisibility(View.INVISIBLE);
				break;
			} else {
				Toast.makeText(this, "Sorry. Incorrect username or password.", Toast.LENGTH_LONG).show();
				finish();
			} 
		
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	
	/**
	 * Reads data from a text file into the Db.
	 */
	private void importBeneficiaryDataToDb() {
		AcdiVocaDbHelper db = new AcdiVocaDbHelper(this);
		ContentValues values = new ContentValues();
		
		items = loadBeneficiaryData();
		long nImports = db.addUpdateBeneficiaries(items, AcdiVocaDbHelper.FINDS_STATUS_UPDATE);
		Log.i(TAG, "Imported " + nImports + " Beneficiaries");	
//		Toast.makeText(this, "Imported " +  + nImports + " Beneficiaries", Toast.LENGTH_SHORT);
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
			//while (line != null && k < 1000)  {
			while (line != null)  {
				//Log.i(TAG, line);
				if (line.length() > 0 && line.charAt(0) != '*')  {
					data[k] = line;
					k++;
				}
				line = br.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (StringIndexOutOfBoundsException e) {
			Log.e(TAG, "Bad line?  " + line);
			e.printStackTrace();
		}
		String[] dossiers = new String[k];  // Create the actual size of array
		for (int i= 0; i < k; i++)
			dossiers[i] = data[i];
		return dossiers;
	}
	
	class ImportThreadHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == DONE) {
				mProgressDialog.dismiss();
			}
		}
	}
	
	class ImportDataThread extends Thread {
		private Context mContext;
		private Handler mHandler;
		
		public ImportDataThread(Context context, Handler handler) {
			mHandler = handler;
			mContext = context;
		}
	
		@Override
		public void run() {
			importBeneficiaryDataToDb();
			mHandler.sendEmptyMessage(AcdiVocaAdminActivity.DONE);
		}
	}
}

