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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.hfoss.posit.android.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

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
		
		@Override
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

		@Override
		public String toString() {
			return preferencesXmlFile + " table=" + preferencesList.toString();
		}

		@Override
		public boolean equals(Object o) {
			return preferencesXmlFile.equals(((PluginSettings)o).getPreferencesXmlFile());
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
		
		PluginSettings settingsObject = getKeyActivityPairs(context, prefsXmlFileName);
		if (!pluginXmlList.contains(settingsObject))
			pluginXmlList.add(0,settingsObject);		
	} 
	
	/**
	 * Utility method parses an XML preferences file pulling out domain-specific attributes
	 * that associate a Preference key with an Activity.
	 * @param context this Activity
	 * @param prefsXmlFileName  the name of the XML file
	 * @return an PluginSettings object that stores the data for a particular XML file.
	 */
	private static PluginSettings getKeyActivityPairs(Context context, String prefsXmlFileName) {
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
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return settingsObject;
	}
	
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Log.i(TAG, "onCreate()");
		
		// Add POSIT's core preferences to the Settings (if not already added) 
		
		PluginSettings settingsObject = getKeyActivityPairs(this, "posit_preferences");
		if (!pluginXmlList.contains(settingsObject))
			pluginXmlList.add(0,settingsObject);
	
		
		// For each plugin add its preferences to the Settings
		for (int k = 0; k < pluginXmlList.size(); k++) {
			Log.i(TAG,pluginXmlList.get(k).toString());
			
			// Merge its preference with POSIT core preferences.
			String pluginFile = pluginXmlList.get(k).getPreferencesXmlFile();
			int resID = getResources().getIdentifier(pluginFile, "xml", "org.hfoss.posit.android");
			this.addPreferencesFromResource(resID);
			
			// For each preference that starts an Activity set its Listener
			ArrayList<PluginSetting> settings = pluginXmlList.get(k).getPreferencesList();
			for (int j = 0; j < settings.size(); j++) {
				this.findPreference(settings.get(j).prefName).setOnPreferenceClickListener(this);
			}
		}
		// Register this activity as a preference change listener
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		sp.registerOnSharedPreferenceChangeListener(this);
		
		// Initialize the summary strings
		Map<String,?> prefs = sp.getAll();
		Iterator it = prefs.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Preference p =  findPreference(key);
			String value = sp.getString(key, null);
			if (p!= null && value != null) 
				p.setSummary(value);
		}
		
		for (int k = 0; k < prefs.size(); k++) {

		}
		
		//this.findPreference("testpositpref").setOnPreferenceClickListener(this);
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
					Log.i(TAG, "Class = " + className);
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
		return false;
	}
	
	/**
	 * Adjusts the summary string when a shared preference is changed
	 * (non-Javadoc)
	 * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged(android.content.SharedPreferences, java.lang.String)
	 */
	 public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		 Log.i(TAG, "onSharedPreferenceChanged, key= " + key +
				 " value = " + sp.getString(key, ""));
		 Log.i(TAG, "Preferences= " + sp.getAll().toString());
		 Preference p =  this.findPreference(key);
		 String value = sp.getString(key, null);
		 if (p != null && value != null)
			 p.setSummary(value);
		 
		 if (key.equals(getString(R.string.distribution_point)) && value != null) {
			 Editor ed = sp.edit();
			 ed.putString(getString(R.string.distribution_event_key), 
					 getString(R.string.import_beneficiary_file));
			 ed.commit();
		 }
	 }

}