/*
 * File: Tests.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool.
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
package org.hfoss.posit.android.test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.UUID;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.activity.FindActivity;
import org.hfoss.posit.android.api.plugin.AddFindPluginCallback;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.api.plugin.FunctionPlugin;

import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;


/**
 * Testing class
 *
 */
public class Tests extends PreferenceActivity {
	
	private static final String TAG = "Tests";
	
	private FindActivity f;
	private int projectId;
	
	public Tests (FindActivity f, int projectId) {
		this.f = f;
		this.projectId = projectId;
	}
	
	/**
	 * Randomly assigns Find information and stores it in a Find instance.
	 * This method was created primarily to test the map clustering of Finds.
	 * 
	 * @param n number of Finds to generate
	 */
	public void generateFinds(int n) {		
		
		Find find = null;
		
		int minlat = -90; 
		int maxlat = 90;
		int minlong = -180;
		int maxlong = 180;
		int latitude = 0;
		int longitude = 0;
		int rows;
		
		Random gen = new Random();		
	
		// get the appropriate find class from the plug-in
		// manager and make an instance of it
		Class<Find> findClass = FindPluginManager.mFindPlugin.getmFindClass();		
		
		for (int i=0; i < n; i++) {	
			
			rows = 0;

			try {
				find = findClass.newInstance();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			}

			// Generate a random GUID
			// NOTE: Some derived finds may not have a GUID field. But the Guid must
			// be set anyway because it is used as the Find ID by the Posit server.
			find.setGuid(UUID.randomUUID().toString()); 

			//Set Time
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yyyy/MM/dd HH:mm:ss");
			
			String initialDate = "2012/13/04 11:00:00";
			try {
				find.setTime(dateFormat.parse(initialDate));
			} catch (ParseException e) {
				e.printStackTrace();
			}

			// Set Description			
			find.setDescription("Test");
			
			// Randomly set Longitude and Latitude				
			longitude = gen.nextInt(maxlong - minlong) + minlong;	
			latitude = gen.nextInt(maxlat - minlat) + minlat;	
			
			//latitude and longitude values need to of length at least seven
			find.setLatitude(rightPad(latitude));
			find.setLongitude(rightPad(longitude));

			find.setIs_adhoc(0); 
			find.setRevision(0);
			find.setStatus(0);
			find.setSyncOperation(0);			
			
			// Set Project ID
			find.setProject_id(projectId);
		
			rows = f.getHelper().insert(find);					
			
			for (FunctionPlugin plugin : f.getmAddFindMenuPlugins()) {
				Log.i(TAG, "plugin=" + plugin);
				Class<AddFindPluginCallback> callbackClass = null;
				Object o;
				try {
					View view = ((ViewGroup)f.findViewById(android.R.id.content)).getChildAt(0);
					String className = plugin.getAddFindCallbackClass();
					if (className != null) {
						callbackClass = (Class<AddFindPluginCallback>) Class.forName(className);
						o = (AddFindPluginCallback) callbackClass.newInstance();
						((AddFindPluginCallback) o).afterSaveCallback(
								this.getApplication(),
								find,
								view,
								rows > 0);
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				}
			}
		}
		
	
	}
	
	/**
	 * Pad value with decimal digits to the right
	 * This function is primarily to satisfy the requirement that latitude
	 * and longitude values must be at least seven digits.
	 * @param value
	 * @return s the padded double
	 */
	public static final double rightPad (int value) {
		  double s = (double) value + 0.0000001;
		  return s;
	}

}
