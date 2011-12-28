/*
 * File: FileViewActivity.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool. 
 *
 * This code is free software; you can redistribute it and/or modify
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

package org.hfoss.posit.android.experimental.functionplugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.database.DbManager;
import org.hfoss.posit.android.experimental.functionplugins.sms.ObjectCoder;
import org.hfoss.posit.android.experimental.functionplugins.tracker.TrackerSettings;
import org.hfoss.posit.android.experimental.plugin.FindActivityProvider;
import org.hfoss.posit.android.experimental.plugin.FindPlugin;
import org.hfoss.posit.android.experimental.plugin.FindPluginManager;
import org.hfoss.posit.android.experimental.plugin.FunctionPlugin;
import org.hfoss.posit.android.experimental.plugin.csv.CsvListFindsActivity;

import com.j256.ormlite.android.apptools.OrmLiteBaseListActivity;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter.ViewBinder;

public class FileViewActivity extends OrmLiteBaseListActivity<DbManager> {
//implements ViewBinder {

	public static final String TAG = "FileViewActivity";
	private static final String HOME_DIRECTORY = "log";
//	List<Find> finds;
//	protected static FileViewListAdapter mAdapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_finds);

		// Create /sdcard/HOME_DIRECTORY if it doesn't exist already
		File dir = new File(Environment.getExternalStorageDirectory() + "/"
				+ HOME_DIRECTORY);
		if (!dir.exists()) {
			if (dir.mkdir()) {
				Log.i(TAG, "Created directory " + dir);
			}
		}
		// Start file picker activity using /sdcard/HOME_DIRECTORY as the home
		// directory
		Intent intent = new Intent();
		intent.putExtra("home", Environment.getExternalStorageDirectory() + "/"
				+ HOME_DIRECTORY);
		intent.setClass(this, FilePickerActivity.class);
		this.startActivityForResult(intent, FilePickerActivity.ACTION_CHOOSER);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case FilePickerActivity.ACTION_CHOOSER:
			if (resultCode == FilePickerActivity.RESULT_OK) {
				
				// A filename (absolute path) was returned.  Try to display it.
				String filename = data.getStringExtra(Intent.ACTION_CHOOSER);
//				File file = new File(Environment.getExternalStorageDirectory()
//						+ "/" + HOME_DIRECTORY, filename);
				File file = new File(filename);		
				
				// CSV files are parsed as Finds
				if (filename.endsWith(".csv")) {
					Intent intent = new Intent();
					intent.setClass(this, CsvListFindsActivity.class);
					intent.putExtra(CsvListFindsActivity.FILENAME_TAG, filename);
					this.startActivity(intent);
					
//					setContentView(R.layout.list_finds);
//					finds = readFindsFromFile(file);
//					if (finds != null) {
//						mAdapter = (FileViewListAdapter) setUpAdapter(finds);
//						fillList(mAdapter);
//					} else {
//						Log.i(TAG, "Cannot parse " + filename);
//						Toast.makeText(this, "Sorry, cannot parse " + filename,
//								Toast.LENGTH_SHORT).show();
//					}
					
				// Text files are just displayed	
				} else if (filename.endsWith(".txt")) {
					setContentView(R.layout.list_finds);
					String text = readFileAsText(file, "\n-----\n");
					TextView tv = (TextView) findViewById(R.id.emptyText);
					tv.setTextSize(11);
					tv.setText(text);	
				} else if (filename.endsWith(".xml")) {
						setContentView(R.layout.list_finds);
						String text = readFileAsText(file, "\n");
						TextView tv = (TextView) findViewById(R.id.emptyText);
						tv.setTextSize(11);
						tv.setText(text);						
				// Punt on all other types of files	
				} else {
					Log.i(TAG, "Don't know what to do with " + filename);
					Toast.makeText(this, "Sorry, don't know what to do with " + filename,
							Toast.LENGTH_SHORT).show();
				}

			} else {
				// Result not good, do something about it
				Toast.makeText(this, "Error occurred in File Picker",
						Toast.LENGTH_LONG).show();
				finish();
			}
			break;
		default:
			// Shouldn't happen
			Log.e(TAG, "Request code on activity result not recognized");
			finish();
		}
	}
	
	/**
	 * Reads a text file and return it as a String
	 * @param file, should be a text file
	 * @return
	 */
	protected String readFileAsText(File file, String lineseparator) {
		StringBuilder text = new StringBuilder();

		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			while ((line = br.readLine()) != null) {
				text.append(line);
				text.append(lineseparator);
			}
		} catch (IOException e) {
			Log.e(TAG, "IO Exception reading from file "
					+ e.getMessage());
			e.printStackTrace();
			Toast.makeText(this, "Error occurred reading from file",
					Toast.LENGTH_LONG).show();
			finish();
		}
		return text.toString();

	}

//	/**
//	 * Reads a CSV file and parses the contents as Finds.
//	 * @param file, should be a comma-delimited CSV file, with no internal
//	 * commas in any of the columns. 
//	 * @return
//	 */
//	protected List<Find> readFindsFromFile(File file) {
//		// Read text from file
//		StringBuilder text = new StringBuilder();
//
//		try {
//			BufferedReader br = new BufferedReader(new FileReader(file));
//			String line;
//
//			FindPlugin plugin = FindPluginManager.mFindPlugin;
//			if (plugin == null) {
//				Log.e(TAG, "Could not retrieve Find Plugin.");
//				return null;
//			}
//
//			int project_id = PreferenceManager.getDefaultSharedPreferences(this).getInt(this.getString(R.string.projectPref), 0);
//			String formatstr = plugin.getCsvFormat();
//			String mapstr = plugin.getCsvMap();
//
//			finds = new ArrayList<Find>();
//
//			while ((line = br.readLine()) != null) {
//				if (line.contains("OBJECTID"))
//					continue;
//				Find f = parseCsvFind(line, formatstr, mapstr);
//				if (f != null) {
//					f.setProject_id(project_id);
//					finds.add(f);
//				}
//				text.append(line);
//				text.append('\n');
//			}
//		} catch (IOException e) {
//			Log.e(TAG, "IO Exception reading from file "
//					+ e.getMessage());
//			e.printStackTrace();
//			Toast.makeText(this, "Error occurred reading from file",
//					Toast.LENGTH_LONG).show();
//			finish();
//		}
//
//		return finds;
//	}



//	/**
//	 * Attempts to parse a String message as a Find object.
//	 * 
//	 * @param csv
//	 *            A String that may or may not actually correspond to a Find
//	 *            object
//	 * @return A Find object, or null if it couldn't be parsed.
//	 */
//	@SuppressWarnings("unchecked")
//	private Find parseCsvFind(String csv, String keystring, String mapstring) {
//		//		keystring = "guid,latitude,longitude,name,phone,url,description,address2,city,zip";
//		//		String mapstring = "guid,latitude,longitude,name,phone,url,description,description,description,description";
//		String columns[] = keystring.split(","); 
//		String map[] = mapstring.split(",");
//
//		Log.i(TAG, csv);
//		//		if (csv.contains("OBJECTID"))
//		//			return null;
//
//		// Separate message into values, making sure to take escape
//		// characters into account as we go
//		List<String> values = new ArrayList<String>();
//		StringBuilder current = new StringBuilder();
//		for (int i = 0; i < csv.length(); i++) {
//			char c = csv.charAt(i);
//			if (c == ObjectCoder.ESCAPE_CHAR) {
//				// Add this character and the next character to the current
//				// string without checking for delimiters
//				current.append(c);
//				if (i + 1 < csv.length())
//					c = csv.charAt(++i);
//				current.append(c);
//			} else if (c == ',') {
//				// Delimiter. Finish up string and start new one.
//				values.add(current.toString());
//				current = new StringBuilder();
//			} else {
//				current.append(c);
//			}
//		}
//		values.add(current.toString());
//
//		// Attempt to construct a Find
//		Find find;
//		try {
//			FindPlugin plugin = FindPluginManager.mFindPlugin;
//			if (plugin == null) {
//				Log.e(TAG, "Could not retrieve Find Plugin.");
//				return null;
//			}
//			find = plugin.getmFindClass().newInstance();
//			Log.i(TAG,"Processing " + find.getClass());
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//			return null;
//		} catch (InstantiationException e) {
//			e.printStackTrace();
//			return null;
//		}
//		// Need to get attributes for the Find
//		Bundle bundle = find.getDbEntries();
//		List<String> keys = new ArrayList<String>(bundle.keySet());
//		// Important to sort so that we process attributes in the same order on
//		// both ends
//		Collections.sort(keys);
//		// Now try to put values with attributes
//		if (values.size() != keys.size()) {
//			Log.e(TAG,
//					"Received value set does not have expected size. values = "
//					+ values.size() + ", keys = " + keys.size());
//			//			return null;
//		}
//		Log.i(TAG,"keystring=" + keystring);
//		Log.i(TAG,"keys=" + keys);
//		Log.i(TAG,"values=" + values);
//
//		for (int i = 0; i < values.size(); i++) {
//			String key = map[i];
//			//			String key = columns[i];
//			//			String key = keys.get(i);
//			// Get type of this entry
//			Class<Object> type = null;
//			try {
//				type = find.getType(key);
//			} catch (NoSuchFieldException e) {
//				// No such field. This shouldn't happen, since we're pulling the
//				// keys from our own find
//				Log.e(TAG, "Encountered no such field exception on field: "
//						+ key);
//				e.printStackTrace();
//				continue;
//				//return null;
//			}
//			// See if we can decode this value. If not, then we can't make a
//			// Find.
//			Serializable obj;
//			try {
//				obj = (Serializable) ObjectCoder.decode(values.get(i), type);
//			} catch (IllegalArgumentException e) {
//				Log.e(TAG, "Failed to decode value for attribute \"" + key
//						+ "\", string was \"" + values.get(i) + "\"");
//				return null;
//			}
//			if (bundle.containsKey(key)) {
//				Object o = bundle.get(key); 
//				if (o != null && o.getClass().getSimpleName().equals("String") 
//						&& obj.getClass().getSimpleName().equals("String")) {
//					String s = (String) o;
//					String newS = (String) obj;
//					s = s + "," + newS;  // Concatenate
//					bundle.remove(key);
//					try {
//						obj = (Serializable) ObjectCoder.decode(s, type);
//					} catch (IllegalArgumentException e) {
//						Log.e(TAG, "Failed to decode value for attribute \"" + key
//								+ "\", string was \"" + values.get(i) + "\"");
//					}					
//				}
//			}
//
//			// Decode successful!
//			bundle.putSerializable(key, obj);
//		}
//		// Make Find
//		find.updateObject(bundle);
//		return find;
//	}



	/**
	 * Uses the ‘haversine’ formula to calculate the great-circle distance 
	 * between two points – that is, the shortest distance over the earth’s 
	 * surface – giving an ‘as-the-crow-flies’ distance between the points 
	 * (ignoring any hills, of course!).
	 * @see http://www.movable-type.co.uk/scripts/latlong.html
	 * @param mylat
	 * @param mylong
	 * @param lat
	 * @param lon
	 * @return
	 */
	private double distance(double mylat, double mylong, double lat, double lon) {
		//40.785148,-73.978828  Walde's apartment
		mylat = 40.785148;
		mylong = -73.978828;
		double R = 6371;  // radius of earth
		double dLat = Math.toRadians(mylat - lat);
		double dLon = Math.toRadians(mylong - lon);
		double lat1 = Math.toRadians(mylat);
		double lat2 = Math.toRadians(lat);
		double A = Math.sin(dLat/2) * Math.sin(dLat/2) +
		Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2); 
		double C  =  2 * Math.atan2(Math.sqrt(A), Math.sqrt(1-A)); 
		double D = R * C;
		return D;
	}

//	/**
//	 * Creates the menus for this activity.
//	 * 
//	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
//	 */
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		MenuInflater inflater = getMenuInflater();
//		inflater.inflate(R.menu.file_view_menu, menu);
//		return true;
//	}
//
//	@Override
//	public boolean onMenuItemSelected(int featureId, MenuItem item) {
//		int count = getHelper().insertAll(finds);
//		Toast.makeText(this, "Saved " + count + " Finds", Toast.LENGTH_SHORT).show();
//		return super.onMenuItemSelected(featureId, item);
//	}


//	/**
//	 * Puts the items from the DB table into the rows of the view.
//	 */
//	private void fillList(ListAdapter adapter) {
//		setListAdapter(adapter);
//
//		ListView lv = getListView();
//		lv.setTextFilterEnabled(true);
//		lv.setOnItemClickListener(new OnItemClickListener() {
//
//			/**
//			 * Returns the expedition Id to the TrackerActivity
//			 */
//			public void onItemClick(AdapterView<?> parent, View view,
//					int position, long id) {
//				int find_id = Integer.parseInt( (String) ((TextView)view.findViewById(R.id.id)).getText());
//
//				Find find = new Find();
//				find.setName((String) ((TextView)view.findViewById(R.id.name)).getText());
//				find.setDescription((String) ((TextView)view.findViewById(R.id.description_id)).getText());
//				find.setLatitude(Double.parseDouble((String) ((TextView)view.findViewById(R.id.latitude)).getText()));
//				find.setLongitude(Double.parseDouble((String) ((TextView)view.findViewById(R.id.longitude)).getText()));
//				Intent intent = new Intent(parent.getContext(),
//						FindPluginManager.mFindPlugin.getmFindActivityClass());
//
//				find.setGuid((String) ((TextView) view.findViewById(R.id.id)).getText());
//
//				intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
//				intent.putExtra("findbundle",  find.getDbEntries());
//				startActivity(intent);
//			}
//		});
//	}

//	protected FileViewListAdapter setUpAdapter(List<Find> finds) {
//
//		int resId = getResources().getIdentifier("tracker_row", "layout", getPackageName());
//		FileViewListAdapter adapter = new FileViewListAdapter(this, resId, finds);
//
//		return adapter;
//	}
//
//	protected class FileViewListAdapter extends ArrayAdapter<Find> {
//		protected List<? extends Find> items;
//
//		public FileViewListAdapter(Context context, int textViewResourceId, List list) {
//			super(context, textViewResourceId, list);
//			Log.i(TAG, "FileViewListAdapter constructor");
//			this.items = list;
//		}
//
//		@Override
//		public View getView(int position, View convertView, ViewGroup parent) {
//			View v = convertView;
//
//			if (v == null) {
//				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//
//				int resId = getResources().getIdentifier(FindPluginManager.mFindPlugin.mListFindLayout, "layout", getPackageName());
//				v = vi.inflate(resId, null);
//
//			}
//			Find find = items.get(position);
//			if (find != null) {
//				TextView tv = (TextView) v.findViewById(R.id.name);
//				tv.setText("" + find.getName());
//				tv = (TextView) v.findViewById(R.id.description_id);
//				tv.setText("" + find.getDescription());
//				tv = (TextView) v.findViewById(R.id.latitude);
//				tv.setText("" + find.getLatitude());
//				tv = (TextView) v.findViewById(R.id.longitude);
//				tv.setText("" + find.getLongitude());
//				tv = (TextView) v.findViewById(R.id.id);
//				tv.setText(find.getGuid());
//			}
//			return v;
//		}
//
//	}
//
//	/**
//	 * Required for the ViewBinder interface.  Unused at the moment. It could
//	 * be used to modify the view as the data are being displayed.  As it stands
//	 * data from the Cursor are simply placed in their corresponding Views using
//	 * the arrays provided above to SimpleCursorAdapter.
//	 */
//	public boolean setViewValue(View v, Cursor cursor, int colIndex) {
//		return false;
//	}





}
