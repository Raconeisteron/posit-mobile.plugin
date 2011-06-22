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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

import org.hfoss.posit.android.Log;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.FindActivityProvider;
import org.hfoss.posit.android.api.FindPluginManager;
import org.hfoss.posit.android.api.SettingsActivity;
import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaDbHelper.UserType;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;


/**
 * This class designed to make the user more comfortable with
 * posit the first time posit is opened. The user navigates through the tutorial
 * using previous, next, skip, and finish buttons.
 */

public class AcdiVocaAdminActivity extends Activity  {

	public static String TAG = "AdminActivity";
	public static final int MAX_BENEFICIARIES = 20000;  // Max readable
	
	public static final String DEFAULT_DIRECTORY = "acdivoca";
	public static final String DEFAULT_BENEFICIARY_FILE = "Beneficiare.csv";
	public static final String DEFAULT_LIVELIHOOD_FILE = "Livelihood.csv";
	public static final String COMMA= ",";
	public static final int DONE = 0;



	private ArrayAdapter<String> adapter;
	private String mBeneficiaries[] = null;
	private ProgressDialog mProgressDialog;
	private String mDistrCtr;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
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
	 * The Admin menu needs to be carefully prepared to control the management
	 * of distribution events through the various stages:  select distribution point,
	 * import beneficiary data, start, stop, send distribution report.  
	 * Here's where the various values are set:
	 * 
	 * 1) Select distribution point -- selected in settings.
	 * 2) Import beneficiary file -- set in SettingsActivity
	 * 3) Start -- set in onMenuItemSelected
	 * 4) Stop -- set in onMenuItemSelected
	 * 5) Send report -- set in SendSms
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String distrEventStage = prefs.getString(getString(R.string.distribution_event_key), "");
		String distrPoint = prefs.getString(getString(R.string.distribution_point), "");
	
		int userTypeOrdinal = prefs.getInt(AcdiVocaDbHelper.USER_TYPE_KEY, -1);
		Log.i(TAG, "UserTypeKey = " + userTypeOrdinal);
		if (userTypeOrdinal != UserType.SUPER.ordinal()) {
			menu.getItem(1).setVisible(false);
		} else {
			menu.getItem(1).setVisible(true);
		}

		
		Log.i(TAG, "onPrepareMenuOptions, distrPoint ="  + distrPoint 
				+ " distribution stage = " + distrEventStage);
		
		

		MenuItem item = null;
		for (int k = 0; k < menu.size(); k++) {
			item = menu.getItem(k);
			switch (item.getItemId()) {
			case R.id.load_beneficiary_data:
				if (distrEventStage.equals(getString(R.string.import_beneficiary_file))) 
					item.setEnabled(true);
				else 
					item.setEnabled(false);
				break;
			case R.id.start_stop_distribution:
				SubMenu sub = item.getSubMenu();
				if (distrEventStage.equals(getString(R.string.start_distribution_event))) {
					item.setEnabled(true);
					sub.getItem(0).setEnabled(true);
					sub.getItem(1).setEnabled(false);
				} else if (distrEventStage.equals(getString(R.string.stop_distribution_event))) {
					item.setEnabled(true);
					sub.getItem(0).setEnabled(false);
					sub.getItem(1).setEnabled(true);		
				} else {
					item.setEnabled(false);
				}
				break;
			case R.id.send_distribution_report:
				if (distrEventStage.equals(getString(R.string.send_distribution_report))) 
					item.setEnabled(true);
				else 
					item.setEnabled(false);
				break;
			}
		}
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Manages the selection of menu items.
	 * 
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Log.i(TAG, "Menu Item = " + item.getTitle());
		Intent intent = new Intent();

		switch (item.getItemId()) {
		case R.id.settings_menu_item:
			startActivity(new Intent(this, SettingsActivity.class));
			break;
		case R.id.admin_list_beneficiaries:
			startActivity(new Intent(this, AcdiVocaListFindsActivity.class));
			break;
//		case R.id.manage_distribution:
//			TextView tv = (TextView) findViewById(R.id.manage_event_text_view);
//			tv.setVisibility(View.VISIBLE);
//			break;
		case R.id.load_beneficiary_data:
			Class<Activity> loginActivity = FindActivityProvider.getLoginActivityClass();
			if (loginActivity != null) {
				intent.setClass(this, loginActivity);
				intent.putExtra(AcdiVocaDbHelper.USER_TYPE_STRING, AcdiVocaDbHelper.UserType.ADMIN.ordinal());
				this.startActivityForResult(intent, LoginActivity.ACTION_LOGIN);
				
				Toast.makeText(this, "Admin Login for Import Beneficiary Data.", Toast.LENGTH_LONG).show();	
			}
			break;
		case R.id.start_distribution:
			Log.i(TAG, "Start distribution event");
			item.setEnabled(false);
			setDistributionEventStage( this.getString(R.string.stop_distribution_event));		
			break;
		case R.id.stop_distribution:
			Log.i(TAG, "Start distribution event");
			item.setEnabled(false);
			setDistributionEventStage( this.getString(R.string.send_distribution_report));		
			break;
			
		case R.id.send_distribution_report:
			sendDistributionReport();
			setDistributionEventStage( this.getString(R.string.select_distr_point));		
			break;
		}
		return true;
	}
	
	/**
	 * Utility method to send messages.
	 * @param acdiVocaMsgs an ArrayList of messages.
	 */
	private void sendMessages(ArrayList<AcdiVocaMessage> acdiVocaMsgs) {
		Log.i(TAG, "Sending N messages = " + acdiVocaMsgs.size());
		AcdiVocaMessage acdiVocaMsg = null;
		Iterator<AcdiVocaMessage> it = acdiVocaMsgs.iterator();
		int nSent = 0;
		
		while (it.hasNext()) {
			acdiVocaMsg = it.next();
			int beneficiary_id = acdiVocaMsg.getBeneficiaryId();
			Log.i(TAG, "Raw Message: " + acdiVocaMsg.getRawMessage());
			Log.i(TAG, "To Send: " + acdiVocaMsg.getSmsMessage());
			
			AcdiVocaDbHelper db = new AcdiVocaDbHelper(this);
			if (AcdiVocaSmsManager.sendMessage(this, beneficiary_id, acdiVocaMsg, null)) {
				Log.i(TAG, "Message Sent--should update as SENT");
				db.updateMessageStatus(acdiVocaMsg, AcdiVocaDbHelper.MESSAGE_STATUS_SENT);
				++nSent;
			} else {
				Log.i(TAG, "Message Not Sent -- should update as PENDING");
				db.updateMessageStatus(acdiVocaMsg, AcdiVocaDbHelper.MESSAGE_STATUS_PENDING);
			}
		}
		Toast.makeText(this, "Sent " + nSent + " messages.", Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * Sends both update messages and bulk messages.
	 */
	private void sendDistributionReport() {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String distrKey = this.getResources().getString(R.string.distribution_point);
		String distributionCtr = sharedPrefs.getString(distrKey, "");
		Log.i(TAG, distrKey +"="+ distributionCtr);

		// First send update reports -- i.e., those present with changes.
		AcdiVocaDbHelper db = new AcdiVocaDbHelper(this);
		ArrayList<AcdiVocaMessage> acdiVocaMsgs = 
			db.createMessagesForBeneficiaries(SearchFilterActivity.RESULT_SELECT_UPDATE, null, distributionCtr);
		sendMessages(acdiVocaMsgs);
		
		// Now send bulk absences list
		db = new AcdiVocaDbHelper(this);
		acdiVocaMsgs = db.createBulkUpdateMessages(distributionCtr);
		sendMessages(acdiVocaMsgs);
	}
	
	/**
	 * Helper method to set the stage for distribution events.
	 * @param distrEventStage
	 */
	private void setDistributionEventStage(String distrEventStage) {
		SharedPreferences prefs = null;
		Editor editor = null;
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//distrEventStage = prefs.getString(getString(R.string.distribution_event_key), "");

		editor = prefs.edit();
		editor.putString(getString(R.string.distribution_event_key), distrEventStage);
		editor.commit();
		distrEventStage = prefs.getString(getString(R.string.distribution_event_key), "");
		Log.i(TAG, "Distribution stage = " + distrEventStage);					
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG,"onActivityResult = " + resultCode);
		switch (requestCode) {
		case LoginActivity.ACTION_LOGIN:
			if (resultCode == RESULT_OK) {
				//Toast.makeText(this, "Thank you", Toast.LENGTH_LONG).show();
				//findViewById(R.id.fileload_progressbar).setVisibility(View.VISIBLE);
				
				// Get this phone's Distribution Center
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				mDistrCtr = prefs.getString(this.getResources().getString(R.string.distribution_point), null);
				
				if (mDistrCtr == null) {
					Log.i(TAG, "Aborting loadBeneficiaryData, No distribution post selected");
					Toast.makeText(this, "No distribution post selected", Toast.LENGTH_SHORT);
					break;
				}
				
				mProgressDialog = ProgressDialog.show(this, "Loading data",
						"Please wait.", true, true);
				
				ImportDataThread thread = new ImportDataThread(this, new ImportThreadHandler());
				thread.start();				
				
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
		ContentValues values = new ContentValues();
	
		// Read all the file names on the SD Card
		File directory = new File(Environment.getExternalStorageDirectory() + "/" + DEFAULT_DIRECTORY);
		File file[] = directory.listFiles();
		
		// List files on sdcard
//	    File file[] = Environment.getExternalStorageDirectory().listFiles(); 
//	    for (int i = 0; i < file.length; i++)
//	    	Log.i(TAG, file[i].getAbsolutePath());  
		
		mBeneficiaries = loadBeneficiaryData(mDistrCtr);
		
		AcdiVocaDbHelper db = new AcdiVocaDbHelper(this);
		int rows = db.clearBeneficiaryTable();
		Log.i(TAG, "Deleted rows = " + rows);
		db = new AcdiVocaDbHelper(this);
		
		long nImports = db.addUpdateBeneficiaries(mBeneficiaries, AcdiVocaDbHelper.FINDS_STATUS_UPDATE);
		Log.i(TAG, "Imported " + nImports + " Beneficiaries");	
		
		// Move to the next stage of the distribution event process
		setDistributionEventStage( this.getString(R.string.start_distribution_event));		

//		Toast.makeText(this, "Imported " +  + nImports + " Beneficiaries", Toast.LENGTH_SHORT);
	}
	
	/**
	 * Reads beneficiary data from a text file.  Currently the
	 * file name is hard coded as "beneficiaries.txt" and it is
	 * stored in the /assets folder.
	 * @return  Returns an array of Strings, each of which represents
	 * a Beneficiary record.
	 */
	private String[] loadBeneficiaryData(String distrCtr) {
		String[] data = null;
		
		File file = new File(Environment.getExternalStorageDirectory() 
				+ "/" + DEFAULT_DIRECTORY + "/" 
				+ DEFAULT_BENEFICIARY_FILE);

		BufferedReader br = null;
		String line = null;
		int k = 0;
		
		try {
			//InputStream iStream = this.getAssets().open("beneficiaries.txt");
			FileInputStream iStream = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(iStream));
			data = new String[MAX_BENEFICIARIES];
			line = br.readLine();
			//while (line != null && k < 1000)  {
			while (line != null)  {
				//Log.i(TAG, line);
				if (line.length() > 0 && line.charAt(0) != '*')  {
					if (line.contains(distrCtr)) {
						data[k] = line;
						k++;
					}
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
	
	
	/**
	 * Thread to handle import of data from external file. 
	 * @author rmorelli
	 *
	 */
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

