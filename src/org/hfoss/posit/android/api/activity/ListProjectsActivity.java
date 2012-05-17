/*
 * File: ShowProjectsActivity.java
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
package org.hfoss.posit.android.api.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


import org.hfoss.posit.android.R;
import org.hfoss.posit.android.sync.Communicator;
import org.hfoss.posit.android.sync.SyncAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

/**
 * This activity shows a list of the projects on the server that the phone is registered with,
 * and allows the user to pick one from the list.  
 * 
 * When the user picks one, the phone automatically
 * syncs with the server to get all the finds from that project.
 * 
 * The Communicator object handles retrieval of the projects list in a background thread.
 */
public class ListProjectsActivity extends ListActivity implements OnClickListener{

	private static final String TAG = "ShowProjectsActivity";
	private static final int CONFIRM_PROJECT_CHANGE = 0;
	static final int NEW_PROJECT = 1;
	private int mClickedPosition = 0;

	private ArrayList<HashMap<String, Object>> mProjectList;
	
	
	/**
	 * Handles messages and results received from the background thread.
	 */
	final Handler handler = new Handler() { 
		@SuppressWarnings("unchecked")
		public void handleMessage(Message msg) { 
			if (msg.what == Communicator.SUCCESS) {
				mProjectList = (ArrayList<HashMap<String, Object>>) msg.obj;
				showProjects(mProjectList);
			} else {
				reportError((String) msg.obj);
			}
		} 
	}; 

	/**
	 * Called when the activity is first started.  Sets up the UI
	 * and invokes attemptGetProjects, which queries the server.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_proj);
		Button addProjectButton = (Button)findViewById(R.id.idAddProjButton);
		addProjectButton.setOnClickListener(this);

		Communicator.attemptGetProjects(handler, this);  // Done on background thread
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		Log.i(TAG, "onResume()");
		super.onResume();
	}

	/**
	 * Displays the projects in the View using an array list adapter.
	 * 
	 * @param projects, a list of projects.
	 */
	private void showProjects(List<HashMap<String,Object>> projectList) {
		if (projectList != null) {
			Iterator<HashMap<String, Object>> it = projectList.iterator();
			ArrayList<String> projList = new ArrayList<String>();
			for(int i = 0; it.hasNext(); i++) {
				HashMap<String,Object> next = it.next();
				projList.add((String)(next.get("name")));
			}
			setListAdapter(new ArrayAdapter<String>(this,
			          android.R.layout.simple_list_item_1, projList));
			
		} else {
			this.reportError("Null project list returned.\nMake sure your server is reachable.");
		}
	} 
	
	/**
	 * Reports as much information as it can about the error.
	 * @param str, the message to report
	 */
	private void reportError(String str) {
		Log.i(TAG, "Error: " + str);
		Toast.makeText(this, "Error: " + str, Toast.LENGTH_LONG).show();
		finish();
	}

	/**
	 * Invoked when the user selects a name off the projects list.
	 */
	public void onListItemClick(ListView lv, View v, int position, long idFull) {
		mClickedPosition = position;
		String projectId = (String) mProjectList.get(mClickedPosition).get("id");
		int id  = Integer.parseInt(projectId);
		String projectName = (String) mProjectList.get(mClickedPosition).get("name");
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		int currentProjectId = sp.getInt(getString(R.string.projectPref),0);
		
		if (id == currentProjectId){
			Toast.makeText(this, "'" + projectName + "' is already the current project.", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		Editor editor = sp.edit();

		editor.putInt(getString(R.string.projectPref), id);
		editor.putString(getString(R.string.projectNamePref), projectName);
		editor.commit();

		showDialog(CONFIRM_PROJECT_CHANGE);
	}
	
	/**
	 * Called when the user clicks the "New Project" button.  Starts
	 * the NewProject activity, which lets the user create a new project.
	 * 
	 */
	public void onClick(View v) {
		Intent i = new Intent(this, NewProjectActivity.class);;
		switch (v.getId()) {

		case R.id.idAddProjButton:
			startActivityForResult(i,NEW_PROJECT);
			break;
		}
	}
	
	/**
	 * Invoked by NewProjectActivity after the user has created a "New Project". 
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == NEW_PROJECT) {
			Communicator.attemptGetProjects(handler, this);
		}
	}

	/**
	 * Confirms with the user that they have changed their project and automatically 
	 * syncs with the server to get all the project Finds.
	 * 
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONFIRM_PROJECT_CHANGE:
			return new AlertDialog.Builder(this)
			.setIcon(R.drawable.icon)
			.setTitle("You have changed your project to: " 
					+ (String) mProjectList.get(mClickedPosition).get("name"))
					.setPositiveButton(R.string.alert_dialog_ok, 
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
									ListProjectsActivity.this);
							
							boolean syncIsOn = sp.getBoolean("SYNC_ON_OFF", true);
							if (syncIsOn) {
								AccountManager manager = AccountManager.get(getApplicationContext());
								Account[] accounts = manager.getAccountsByType(SyncAdapter.ACCOUNT_TYPE);
								// Just pick the first account for now.. TODO: make this work for multiple accounts of same type?
								Bundle extras = new Bundle();
								ContentResolver.requestSync(accounts[0], getResources().getString(R.string.contentAuthority), extras);
							}
							finish();
						}
					}).create();
		default:
			return null;
		}
	}



}