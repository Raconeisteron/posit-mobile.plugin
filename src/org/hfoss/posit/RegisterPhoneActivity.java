/*
 * File: ServerRegistrationActivity.java
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

package org.hfoss.posit;

import java.util.List;

import org.hfoss.posit.utilities.Utils;
import org.hfoss.posit.web.Communicator;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import org.apache.commons.validator.EmailValidator;
import org.apache.commons.validator.UrlValidator;
import org.apache.oro.*;
import org.apache.commons.validator.routines.*;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Prompts the user to register their phone if the phone is not registered, or shows the phone's current 
 * registration status and allows the user to register their phone again with a different server.
 * 
 *
 */
public class RegisterPhoneActivity extends Activity implements OnClickListener{

	private static final int BARCODE_READER = 0;
	private static final String TAG = "ServerRegistration";
	public boolean isSandbox = false; 
	public boolean readerInstalled = false;
	private Button registerUserButton;
	private Button moreButton;
	private Button registerButton;
	private Button registerAltButton;
	/**
	 * Called when the Activity is first started.  If the phone is not registered, tells the user
	 * so and gives the user instructions on how to register the phone.  If the phone is registered, tells
	 * the user the server address that the phone is registered to in case the user would like to
	 * change it.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.registerphone);
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		
		String server = sp.getString("SERVER_ADDRESS", null);
		if(server!=null){
			((EditText)findViewById(R.id.serverName)).setText(server);
		}
		
		if(isIntentAvailable(this,"com.google.zxing.client.android.SCAN")) {
			readerInstalled=true;
		}
		
		registerUserButton = (Button)findViewById(R.id.createaccount);
		moreButton = (Button)findViewById(R.id.more);
		registerButton = (Button)findViewById(R.id.registerButton);
		registerAltButton = (Button)findViewById(R.id.registerAltButton);
		
		moreButton.setOnClickListener(this);
		registerButton.setOnClickListener(this);
		registerAltButton.setOnClickListener(this);
		registerUserButton.setOnClickListener(this);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode){
		case KeyEvent.KEYCODE_BACK:
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * This method is used to check whether or not the user has an intent
	 * available before an activity is actually started.  This is only 
	 * invoked on the register view to check whether or not the intent for
	 * the barcode scanner is available.  Since the barcode scanner requires
	 * a downloadable dependency, the user will not be allowed to click the 
	 * "Read Barcode" button unless the phone is able to do so.
	 * @param context
	 * @param action
	 * @return
	 */
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	/**
	 * Handles server registration by decoding the JSON Object that the barcode reader gets
	 * from the server site containing the server address and the authentication key.  These
	 * two pieces of information are stored as shared preferences.  The user is then prompted
	 * to choose a project from the server to work on and sync with.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);		
		if (resultCode == RESULT_CANCELED)
			return;
		switch (requestCode) {
		case BARCODE_READER:
			String value = data.getStringExtra("SCAN_RESULT");
			// Hack to remove extra escape characters from JSON text.
			StringBuffer sb = new StringBuffer("");
			for (int k = 0; k < value.length(); k++) {
				char ch = value.charAt(k);
				if (ch != '\\') {
					sb.append(ch);
				} else if (value.charAt(k+1) == '\\') {
					sb.append(ch);
				}	
			}
			value = sb.toString();  // Valid JSON encoded string
			//  End of Hack
			JSONObject object;
			JSONTokener tokener=new JSONTokener(value);

			try {
				Log.i(TAG,"JSON="+value);
				
				object = new JSONObject(value);
				String server = object.getString("server");
				String authKey = object.getString("authKey");
				if(Utils.debug)
					Log.i(TAG, "server= "+server+", authKey= "+authKey);
				TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
				String imei = manager.getDeviceId();
				Communicator communicator = new Communicator(this);				
				try{
				boolean registered = communicator.registerDevice(server, authKey, imei);
				
				if (registered == true) {
					SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
					Editor spEditor = sp.edit();
					
					spEditor.putString("SERVER_ADDRESS", server);
					spEditor.putString("AUTHKEY", authKey);
					spEditor.commit();
				}
				} catch(NullPointerException e){
					Utils.showToast(this, "Registration Error");
				}
				
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
				int projectId = sp.getInt("PROJECT_ID", 0);
				if (projectId == 0) {
					Intent intent = new Intent(this, ShowProjectsActivity.class);
					startActivity(intent);
				}
				finish();
				
			} catch (JSONException e) {
				if(Utils.debug)
					Log.e(TAG, e.toString());
			}
			break;
		}
	}

	public void onClick(View v) {
		
		Context cont = moreButton.getContext();
		String servername = (((TextView)findViewById(R.id.serverName)).getText()).toString();
		EmailValidator emV = EmailValidator.getInstance();
		UrlValidator urV = new UrlValidator();
		
		switch(v.getId()){
		
			case(R.id.more):
				Log.i("blar",""+findViewById(R.id.serverName).getVisibility());
				if(findViewById(R.id.serverName).getVisibility()!=0){
					findViewById(R.id.serverName).setVisibility(EditText.VISIBLE);
					findViewById(R.id.serverNameLabel).setVisibility(TextView.VISIBLE);
					findViewById(R.id.registerButton).setVisibility(TextView.VISIBLE);
				}else{
					findViewById(R.id.serverName).setVisibility(EditText.GONE);
					findViewById(R.id.serverNameLabel).setVisibility(TextView.GONE);
					findViewById(R.id.registerButton).setVisibility(TextView.GONE);
				}
				break;
				
			case(R.id.registerAltButton):
				String password = (((TextView)findViewById(R.id.password)).getText()).toString();
				String email = (((TextView)findViewById(R.id.email)).getText()).toString();

				if(urV.isValid(servername)!=true){
					Utils.showToast(cont, "Please enter a valid server URL");
				}
				if(password.equals("") || email.equals("")){
					Utils.showToast(cont, "Please fill in all the fields");
					break;
				}
				if(emV.isValid(email)!=true){
					Utils.showToast(cont, "Please enter a valid email address");
					break;
				}				
				Communicator com = new Communicator(cont);
				TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
				String imei = manager.getDeviceId();
				String result = com.registerDevice(servername, email, password, imei);
				if(result!=null){
					SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
					Editor spEditor = sp.edit();
					spEditor.putString("SERVER_ADDRESS", servername);
					spEditor.putString("AUTHKEY", result);
					spEditor.commit();			
				}else
					Utils.showToast(cont, "Registration Error");
				break;
				
			case(R.id.registerButton):
            	if(!readerInstalled){
            		Utils.showToast(cont,"Please install the Zxing Barcode Scanner");
            		break;
            	}
        		if(!Utils.isNetworkAvailable(cont)){
        			Utils.showToast(cont, "Registration Error:No Network Available");
        			break;
        		}
            	if (RegisterPhoneActivity.isIntentAvailable(
            			RegisterPhoneActivity.this,"com.google.zxing.client.android.SCAN"))
        		{
            		
        			Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        			try{
        				startActivityForResult(intent, BARCODE_READER);
        			}catch(ActivityNotFoundException e)
        			{
        				if(Utils.debug)
        					Log.i(TAG, e.toString());
        			}
        		}
            
				break;
				
			case(R.id.createaccount):
				
				if(urV.isValid(servername)!=true){
					Utils.showToast(cont, "Please enter a valid server URL");
					break;
				}
				Intent i = new Intent(cont,RegisterUserActivity.class);
				i.putExtra("server", (((TextView)findViewById(R.id.serverName)).getText()).toString());
				this.startActivity(i);
				break;
				
		}
		
	}
}