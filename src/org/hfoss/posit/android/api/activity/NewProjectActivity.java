/*
 * File: NewProjectActivity.java
 * 
 * Copyright (C) 2010 The Humanitarian FOSS Project (http://www.hfoss.org)
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

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.sync.Communicator;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Creates a new project and registers it on the server. 
 * @author rmorelli
 *
 */
public class NewProjectActivity extends Activity implements OnClickListener{

	private Button mCreateProject;
	private static final String TAG = "NewProjectActivity";
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!Communicator.isServerReachable(this)) {
		    Log.i(TAG, "Can't add new project. The server not reachable");
		    Toast.makeText(this, "Can't add new project. The server not reachable.", Toast.LENGTH_LONG).show();
		    finish();
		    return;
		}

		setContentView(R.layout.new_project);
		mCreateProject = (Button) this.findViewById(R.id.createProject);
		mCreateProject.setOnClickListener(this);
	}

	public void onClick(View v) {
		switch(v.getId()){
		case R.id.createProject:
			String projectName = (((TextView) findViewById(R.id.projectName)).getText()).toString();
			String projectDescription = (((TextView) findViewById(R.id.projectDescription)).getText()).toString(); 
			if(projectName.equals("")){
				Toast.makeText(this, "Please enter a name for your project", Toast.LENGTH_LONG).show();
				break;
			}
			SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(this);
			String authkey = Communicator.getAuthKey(this);
//			String server = prefManager.getString("SERVER_PREF", null);
			String server = prefManager.getString(getString(R.string.serverPref), null);
			if (server == null) 
				server = getString(R.string.defaultServer);
			Communicator com = new Communicator();
			String response = com.createProject(this, server, projectName, projectDescription, authkey);
			Log.i(TAG, response);
			if(response.contains("success")){
				Toast.makeText(this,response,Toast.LENGTH_SHORT).show();
				setResult(ListProjectsActivity.NEW_PROJECT);
				finish();
			}
			else
				Toast.makeText(this,response,Toast.LENGTH_SHORT).show();
			break;
		}
	}
}

