/*
 * File: SettingsActivity.java
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
package org.hfoss.posit.android.api;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.hfoss.posit.android.AboutActivity;
//import org.hfoss.posit.android.Log;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.RegisterActivity;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Manages preferences for core POSIT preferences and all plugins.  Plugin preferences are
 * merged with POSIT preferences. 
 * 
 * Here's how to specify a preference XML for a plugin.  
 * 
 * Preferences (EditPreference, ListPreference, etc) that simply set a 
 * preference value are handled automatically by PreferenceActivity. These can
 * be coded as usual:
 *			<ListPreference android:key="@string/s4"
 *				android:defaultValue="s5" android:summary="@string/s6"
 *				android:entries="@array/a1" android:entryValues="@array/a2" />
 *				
 * Preferences that start an Activity must include an "activity_class" attribute:
 * 
 *			<Preference android:key="testpref" 
 *				android:title="About Me"
 *				activity_class="org.hfoss.posit.android.AboutActivity"
 *				android:summary="Click me and see!" />
 *
 */
public class SettingsActivity extends PreferenceActivity implements OnPreferenceClickListener, OnSharedPreferenceChangeListener {
	private static final String TAG = "API Settings";
	protected static final int BARCODE_READER = 0;
	private String server;
	private String projectName;
	private Preference serverAddress;
	private Preference project;
	private Preference user;
	
	/**
	 * Associates preferences with activities.
	 */
	private static ArrayList<PluginSettings> pluginXmlList = new ArrayList<PluginSettings>();
	
	/**
	 * Inner class for a plugin setting, which consists of a preference name and
	 * and associated activity.  A PluginSetting is created only for those preferences
	 * that require an associated Activity, not for preferences that are handled automatically
	 * by PreferenceActivity.
	 * @author rmorelli
	 *
	 */
	static class PluginSetting {
		public String prefName;
		public String activityName;
		
		public PluginSetting(String prefName, String activityName) {
			this.prefName = prefName;
			this.activityName = activityName;
		}
		
		public String toString() {
			return prefName + "," + activityName;
		}
	}
	
	/**
	 * Innter class for PluginSettings. Stores a record for each plugin
	 * consisting of the plugin's preferences XML file (in res/xml) and
	 * key/activity pairs for each preference that requires an Activity launch.
	 * @author rmorelli
	 *
	 */
	static class PluginSettings {
		private String preferencesXmlFile;
		private ArrayList<PluginSetting> preferencesList;
				
		PluginSettings(String preferencesXmlFile) {
			this.preferencesXmlFile = preferencesXmlFile;
			preferencesList = new ArrayList<PluginSetting>();
		}

		public void put(String prefName, String activityName) {
			preferencesList.add(new PluginSetting(prefName,activityName));
		}

		public String getPreferencesXmlFile() {
			return preferencesXmlFile;
		}

		public ArrayList<PluginSetting> getPreferencesList() {
			return preferencesList;
		}

		public String toString() {
			return preferencesXmlFile + " table=" + preferencesList.toString();
		}
		
	}
 
	/**
	 * Parses the XML preferences file and loads the key/activity pairs for all
	 * preferences that require an Activity. Uses XmlPullParser. Called from PluginManager.
	 * @see http://android-er.blogspot.com/2010/04/read-xml-resources-in-android-using.html
	 * @param context
	 * @param prefsXmlFileName
	 */
	public static void loadPluginPreferences(Context context, String prefsXmlFileName) {
		Log.i(TAG,"Loading plugin preferences for Settings Activity");

		PluginSettings settingsObject = new PluginSettings(prefsXmlFileName);
		int resId = context.getResources().getIdentifier(prefsXmlFileName, "xml", "org.hfoss.posit.android");

		XmlResourceParser xpp = context.getResources().getXml(resId);
		try {
			xpp.next();
			int eventType = xpp.getEventType();

			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					if (xpp.getName().equals("Preference")) {
						String preference_name = "";
						String activity_name = "";
						for (int k = 0; k < xpp.getAttributeCount(); k++) {
							String attribute = xpp.getAttributeName(k);
							//Log.i(TAG,"Attribute = " + attribute);
							if (attribute.equals("key")) {
								preference_name = xpp.getAttributeValue(k);
							} else if (attribute.equals("activity_class")) {
								activity_name = xpp.getAttributeValue(k);
							}	
						}
						//Log.i(TAG,"Settings = " + preference_name + " " + activity_name);
						settingsObject.put(preference_name, activity_name);

					}
				}
				eventType = xpp.next();
			}
			pluginXmlList.add(settingsObject);

		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Log.i(TAG, "onCreate()");
		
		// Add POSIT's core preferences to the Settings 
		addPreferencesFromResource(R.xml.posit_preferences);
		
		// For each plugin 
		for (int k = 0; k < pluginXmlList.size(); k++) {
			Log.i(TAG,pluginXmlList.get(k).toString());
			
			// Merge its preference with POSIT core preferences.
			String pluginFile = pluginXmlList.get(k).getPreferencesXmlFile();
			int resID = getResources().getIdentifier(pluginFile, "xml", "org.hfoss.posit.android");
			this.addPreferencesFromResource(resID);
			
			// For each preference that starts an Activity set its Listener
			ArrayList<PluginSetting> settings = pluginXmlList.get(k).getPreferencesList();
			for (int j = 0; j < settings.size(); j++) {
				this.findPreference((CharSequence) settings.get(j).prefName).setOnPreferenceClickListener((OnPreferenceClickListener) this);
			}
		}
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		sp.registerOnSharedPreferenceChangeListener(this);
		
		this.findPreference("testpositpref").setOnPreferenceClickListener((OnPreferenceClickListener) this);
	}
	

	public boolean onPreferenceClick(Preference preference) {
		Log.i(TAG, "API onPreferenceClick " + preference.toString());

		// For each plugin 
		for (int k = 0; k < pluginXmlList.size(); k++) {
			Log.i(TAG,pluginXmlList.get(k).toString());
		
			ArrayList<PluginSetting> list = pluginXmlList.get(k).getPreferencesList();
			Log.i(TAG, "list = " + list.toString());
			for (int j=0; j < list.size(); j++) {
				if (preference.getKey().equals(list.get(j).prefName)) {
					String className = list.get(j).activityName;
					try {
						Class activity = Class.forName(className);
						Log.i(TAG, "Class = " + activity);
						Intent intent = new Intent(this, activity);
						startActivity(intent);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		}

		if (preference.getTitle().toString().equals("About POSIT")){
			Intent intent = new Intent(this, AboutActivity.class);
			startActivity(intent);
		}

//		if(preference.getTitle().toString().equals("Register this device")){
//			//Intent i = new Intent(this, RegisterPhoneActivity.class);
//			Intent intent = new Intent(this, RegisterActivity.class);
//			intent.setAction(RegisterActivity.REGISTER_PHONE);
//			startActivity(intent);
//		}
//		if(preference.getTitle().toString().equals("Create an account")){
//			Intent intent = new Intent(this, RegisterActivity.class);
//			//Intent intent = new Intent(this, RegisterPhoneActivity.class);
//			//intent.setClass(this, RegisterPhoneActivity.class);
//			intent.setAction(RegisterActivity.REGISTER_USER);
//			startActivity(intent);
//		}
//		if(preference.getTitle().toString().equals("Change current server")){
//			if (preference instanceof EditTextPreference) {
//				EditTextPreference textPreference = (EditTextPreference) preference;
//				EditText eText =  textPreference.getEditText();
//				eText.setText(server);
//			}
//			Log.i(TAG, "Server = " + server);
//		}
////		if(preference.getTitle().toString().equals("Login")){
////			Intent i = new Intent(this, RegisterPhoneActivity.class);
////			startActivity(i);
////		}
//		if(preference.getTitle().toString().equals("Change current project")){
//			Intent i = new Intent(this, ShowProjectsActivity.class);
//			startActivity(i);
//		}
//		
		return false;
	}
	
	 public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		 Log.i(TAG, "onSharedPreferenceChanged");
//
//		 if (key.equals("SERVER_ADDRESS")){
//			 Log.i(TAG, "Server1 = " + server);
//			 String tempServer = sp.getString("SERVER_ADDRESS", "");
//
//			 Log.i(TAG, "Server2 = " + tempServer);
//			 if (!server.equals(tempServer)) {
//
//				 if (server != null) {
//					 serverAddress.setSummary(server); 
//				 }
//				 Editor edit = sp.edit();
//				 edit.putString("PROJECT_NAME", "");
//				 edit.putString("AUTHKEY", "");
//				 edit.putInt("PROJECT_ID", 0);
//				 edit.commit();
//				 finish();
//			 }
//			 else {
//				Toast.makeText(this, "'" + server + "' is already the current server.", Toast.LENGTH_SHORT).show();
//			 }
//
//		 }
//		 else if (key.equals("PROJECT_NAME")){
//				String projectName = sp.getString("PROJECT_NAME", "");
//				if (projectName != null) 
//					project.setSummary(projectName); 
//		 }
//		 else if (key.equals("EMAIL")){
//				String email = sp.getString("EMAIL", "");
//				if (email != null) 
//					user.setSummary(email); 
//		 }
	 }

}