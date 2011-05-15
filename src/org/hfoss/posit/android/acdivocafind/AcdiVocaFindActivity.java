/*
 * File: AcdiVocaFindActivity.java
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
package org.hfoss.posit.android.acdivocafind;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.FindActivity;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.gsm.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

/**
 * Handles Finds for AcdiVoca Mobile App.
 * 
 */
public class AcdiVocaFindActivity extends FindActivity{
	public static final String TAG = "AcdiVocaAddActivity";

	public static boolean SAVE_CHECK = true;

	private String mAction = "";
	private int mFindId = 0;
	private AcdiVocaDbHelper mDbHelper;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Create DB helper
		mDbHelper = new AcdiVocaDbHelper(this);

		setContentView(R.layout.acdivoca_add);
		((Button)findViewById(R.id.saveToDB)).setOnClickListener(this);
		((Button)findViewById(R.id.sendSMS)).setOnClickListener(this);

		final Intent intent = getIntent();
		mAction = intent.getAction();
		if (mAction.equals(Intent.ACTION_EDIT)) {
			doEditAction();
		}
	}

	/**
	 * Inflates the Apps menus from a resource file.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.acdivoca_menu_add, menu);
		return true;
	}

	/**
	 * Implements the requested action when user selects a menu item.
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		//		if(item.getItemId()==R.id.refresh) {
		//			((EditText)findViewById(R.id.beneficiary)).setText("");
		//			((EditText)findViewById(R.id.firstname)).setText("");
		//			((DatePicker)findViewById(R.id.datepicker)).init(2011, 1, 1, (OnDateChangedListener) this);
		//			((RadioButton)findViewById(R.id.female)).setChecked(true);
		//			((RadioButton)findViewById(R.id.male)).setChecked(false);
		//		}
		//		if (item.getItemId()==R.id.delete_finds_menu_item) {
		//			Log.i(TAG,"Deleting a find");
		//			showDialog(CONFIRM_DELETE_DIALOG);
		//		}
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	/**
	 * Allows editing of editable data for existing finds.  For existing finds, 
	 * we retrieve the Find's data from the DB and display it in a TextView. The
	 * Find's location and time stamp are not updated.
	 */
	private void doEditAction() {

		mFindId = (int) getIntent().getLongExtra(AcdiVocaDbHelper.FINDS_ID, 0); 
		Log.i(TAG,"Find id = " + mFindId);

		ContentValues values = AcdiVocaFindDataManager.getInstance().fetchFindDataById(this, mFindId, null);
		displayContentInView(values);						
	}

	private boolean checkNumber(String number) {
		for(int i = 0; i < number.length(); i++) {
			if(number.charAt(i)<'0'|| number.charAt(i)>'9')
				if(!(i==0&&number.charAt(i)=='+'))
					return false;
		}
		return true;
	}

	
	public void sendMessage(String type, String message) {
		// setCurrentGpsLocation(null);
		//String phoneNumber = PreferenceManager.getDefaultSharedPreferences(this).getString("smsPhone", "");
		String phoneNumber = "8608748128";
		Toast.makeText(this, "SMS Phone Target = " + phoneNumber, Toast.LENGTH_SHORT).show();		
		message = "Haiti ACDI/VOCA"  + "|" + type + "|" + message;

		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";        
		PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0,
				new Intent(SENT), 0);

		PendingIntent deliveryIntent = PendingIntent.getBroadcast(this, 0,
				new Intent(DELIVERED), 0);

		if(phoneNumber.length()>0 && message.length()>0 && message.length()<=160 && checkNumber(phoneNumber)) {
			try {
				SmsManager sms = SmsManager.getDefault();
				sms.sendTextMessage(phoneNumber, null, message, sentIntent, deliveryIntent);    
				Toast.makeText(this, "SMS Sent!\n"+message + " to " + phoneNumber, Toast.LENGTH_LONG).show();
				Log.i(TAG,"SMS Sent: " + message);
			}catch(Exception e){Log.i("TEST",e.toString());}
		}
		else
			Toast.makeText(this, "SMS Failed\nCheck phone number or length of message", Toast.LENGTH_LONG).show();
	}

	/**
	 * Retrieves values from the View fields and stores them as <key,value> pairs in a ContentValues.
	 * This method is invoked from the Save menu item.  It also marks the find 'unsynced'
	 * so it will be updated to the server.
	 * @return The ContentValues hash table.
	 */
	private ContentValues retrieveContentFromView() {
		ContentValues result = new ContentValues();

		EditText eText = (EditText) findViewById(R.id.beneficiary);
		String value = eText.getText().toString();
		result.put(AcdiVocaDbHelper.FINDS_NAME, value);
		//Log.i(TAG, "retrieve NAME = " + value);
		
		eText = (EditText)findViewById(R.id.firstname);
		value = eText.getText().toString();
		result.put(AcdiVocaDbHelper.FINDS_FIRSTNAME, value);
////		value = mMonth + "/" + mDay + "/" + mYear;
//		value = ((DatePicker)findViewById(R.id.datepicker)).getMonth() + "/" +
//			((DatePicker)findViewById(R.id.datepicker)).getDayOfMonth() + "/" +
//			((DatePicker)findViewById(R.id.datepicker)).getYear();
//		Log.i(TAG, "retrieve DOB=" + value);
//		result.put(DbHelper.FINDS_DOB, value);
//
//		RadioButton sexRB = (RadioButton)findViewById(R.id.female);
//		String sex = "";
//		if (sexRB.isChecked()) 
//			sex = "F";
//		else 
//			sex = "M";
//
//		result.put(DbHelper.FINDS_SEX, sex);         
//
//		result.put(DbHelper.FINDS_PROJECT_ID, PROJECT_ID); // All finds have id=0
		return result;
	}

	/**
	 * Displays values from a ContentValues in the View.
	 * @param contentValues stores <key, value> pairs
	 */
	private void displayContentInView(ContentValues contentValues) {
		EditText eText = (EditText) findViewById(R.id.beneficiary);
		eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_NAME));

		eText = (EditText) findViewById(R.id.firstname);
		eText.setText(contentValues.getAsString(AcdiVocaDbHelper.FINDS_FIRSTNAME));
		Log.i(TAG,"display First Name = " + contentValues.getAsString(AcdiVocaDbHelper.FINDS_FIRSTNAME));

//		//eText = (EditText) findViewById(R.id.dob);
//		
//		//eText.setText(contentValues.getAsString(DbHelper.FINDS_DOB));
//		DatePicker dp = (DatePicker) findViewById(R.id.datepicker);
//		String date = contentValues.getAsString(DbHelper.FINDS_DOB);
//		Log.i(TAG,"display DOB = " + date);
//		dp.init(Integer.parseInt(date.substring(date.lastIndexOf("/")+1)), 
//				Integer.parseInt(date.substring(0,date.indexOf("/"))),
//				Integer.parseInt(date.substring(date.indexOf("/")+1,date.lastIndexOf("/"))),
//				this);
//
//		RadioButton sexRB = (RadioButton)findViewById(R.id.female);
//		Log.i(TAG, "sex=" + contentValues.getAsString(DbHelper.FINDS_SEX));
//		if (contentValues.getAsString(DbHelper.FINDS_SEX).equals("F"))
//			sexRB.setChecked(true);
//		else {
//			sexRB = (RadioButton)findViewById(R.id.male);
//			sexRB.setChecked(true);
//		}
	}

	/**
	 * Required as part of OnClickListener interface. Handles button clicks.
	 */
	public void onClick(View v) {
		if(v.getId()==R.id.saveToDB) {
			boolean result = false;
			ContentValues data = this.retrieveContentFromView(); 
			data.put(AcdiVocaDbHelper.FINDS_PROJECT_ID, 0);
			if (mAction.equals(Intent.ACTION_EDIT)) {
				result = AcdiVocaFindDataManager.getInstance().updateFind(this, mFindId, data);
				Log.i(TAG, "Update to Db is " + result);
			} else {
				result = AcdiVocaFindDataManager.getInstance().addNewFind(this, data);
				Log.i(TAG, "Save to Db is " + result);
			}
			if (result)
				Toast.makeText(this, "Find saved to Db", Toast.LENGTH_SHORT).show();
			else 
				Toast.makeText(this, "Db error", Toast.LENGTH_SHORT).show();
			finish();
		}
		if(v.getId()==R.id.sendSMS) { 
			String name = ((EditText)findViewById(R.id.beneficiary)).getText().toString();
			name = name.trim() + "|" 
				+  ((EditText)findViewById(R.id.firstname)).getText().toString();
//			String dob = ((DatePicker)findViewById(R.id.datepicker)).getMonth() + "/" +
//				((DatePicker)findViewById(R.id.datepicker)).getDayOfMonth() + "/" +
//				((DatePicker)findViewById(R.id.datepicker)).getYear();
//			String nInHome = ((EditText)findViewById(R.id.inhome)).getText().toString();
//			
//			RadioButton sexRB = (RadioButton)findViewById(R.id.female);
//			String sex = "";
//			if (sexRB.isChecked()) 
//				sex = "F";
//			else 
//				sex = "M";
//			RadioButton infantRB = (RadioButton)findViewById(R.id.malnourished);
//			String infant = "";
//			if (infantRB.isChecked()) 
//				infant = "Malnourished";
//			else 
//				infant = "In prevention";
//	
//			RadioButton motherRB = (RadioButton)findViewById(R.id.expecting);
//			String mother = "";
//			if (motherRB.isChecked()) 
//				mother = "Expecting";
//			else 
//				mother = "Nursing";
			String message = name;
			//String message = name + "|" + dob + "|" + sex + "|" + nInHome + "|" + infant + "|" + mother;
	        sendMessage("Register New", message);
	        finish();
		}
	}

	/**
	 * Required as part of OnItemClickListener interface. Handles clicks on, e.g., images. 
	 */
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub	
	}	

	/**
	 * The following methods are part of the OnLocationChangedListener interface.
	 */
	public void onLocationChanged(Location newLocation) {	}
	public void onProviderDisabled(String arg0) {	}
	public void onProviderEnabled(String arg0) {	}
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {	}
}