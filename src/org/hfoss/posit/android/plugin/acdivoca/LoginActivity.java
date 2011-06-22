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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.FindPluginManager;
import org.hfoss.posit.android.api.SettingsActivity;
import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaDbHelper.UserType;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * Handles Login for ACDI/VOCA application.
 * 
 */
public class LoginActivity extends Activity implements OnClickListener {
	public static final String TAG = "AcdiVocaLookupActivity";
	public static final int ACTION_LOGIN = 0;
	public static final int INVALID_LOGIN = 1;
	public static final int VALID_LOGIN = 2;
	
	private static final int CONFIRM_EXIT = 0;
	
	private UserType userType;
	private int mUserTypeOrdinal;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 Log.i(TAG, "onCreate");
		 
		 Intent intent = this.getIntent();
		 Bundle extras = intent.getExtras();
			if (extras == null) {
				return;
			}
			mUserTypeOrdinal = extras.getInt(AcdiVocaDbHelper.USER_TYPE_STRING);
			if (mUserTypeOrdinal == UserType.USER.ordinal()) {
				userType = UserType.USER;
			} else if (mUserTypeOrdinal == UserType.SUPER.ordinal()) {
				userType = UserType.SUPER;
			} else if (mUserTypeOrdinal == UserType.ADMIN.ordinal()) {
				userType = UserType.ADMIN;
			}
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
		Log.i(TAG, "onResume");
		
		AcdiVocaLocaleManager.setDefaultLocale(this);  // Locale Manager should be in API

		setContentView(R.layout.acdivoca_login);  // Should be done after locale configuration
		
		((Button)findViewById(R.id.login_button)).setOnClickListener(this);
		((Button)findViewById(R.id.cancel_login_button)).setOnClickListener(this);
	}

	/**
	 * Required as part of OnClickListener interface. Handles button clicks.
	 */
	public void onClick(View v) {
		Log.i(TAG, "onClick");
	    Intent returnIntent = new Intent();
	
		if (v.getId() == R.id.login_button) {
			EditText etext = ((EditText)findViewById(R.id.usernameEdit));
			String username = etext.getText().toString();
			etext = ((EditText)findViewById(R.id.passwordEdit));
			String password = etext.getText().toString();
			int userTypeOrdinal = authenticateUser(username, password);
			if (userTypeOrdinal != -1) {
				setResult(RESULT_OK,returnIntent);
				
				// Remember what type of user this is -- for controlling menus, etc.
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
				Editor editor = sp.edit();
				editor.putInt(AcdiVocaDbHelper.USER_TYPE_KEY, userTypeOrdinal);
				editor.commit();
				Log.i(TAG, "User Type Key = "  +  sp.getInt(AcdiVocaDbHelper.USER_TYPE_KEY,-1));
				
				SettingsActivity settings = 
					SettingsActivity.getInstance(this, FindPluginManager.mPreferences);
				settings.disablePreferences(this, userTypeOrdinal);
				

				finish();
			} else {
				showDialog(INVALID_LOGIN);
			}
		} else {
			setResult(Activity.RESULT_CANCELED, returnIntent);
			finish();
		}
	    //finish();
	}
	
	/**
	 * Creates a dialog to confirm that the user wants to exit POSIT.
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case INVALID_LOGIN:
			return new AlertDialog.Builder(this).setIcon(
					R.drawable.alert_dialog_icon).setTitle(R.string.password_alert_message)
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// User clicked OK so do nothing

						}
					}
					).create();

		default:
			return null;
		}
	}
	
	private int authenticateUser(String username, String password) {
		AcdiVocaDbHelper db = new AcdiVocaDbHelper(this);
		return db.authenicateUser(username, password, userType);
	}
}