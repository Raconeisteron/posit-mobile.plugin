/*
 * File: FindPluginManager.java
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

package org.hfoss.posit.android.api.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.hfoss.posit.android.R;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * This class reads Plugin specifications and preferences from the
 * raw XML file, plugins-preferences.xml, and constructs a list 
 * of currently active plugins. Its static methods are used by Posit
 * to access plugin activities and other plugin elements
 *
 */
public class FindPluginManager {
	private static FindPluginManager sInstance = null;

	private static final String TAG = "FindPluginManager";

	public static final String MAIN_MENU_EXTENSION = "mainMenu";
	/* Function Button Begins */
	public static final String MAIN_BUTTON_EXTENSION = "mainButton";
	/* Function Button Ends */
	public static final String LIST_MENU_EXTENSION = "listMenu";
	/* To-Do Begins */
	public static final String ADD_FIND_MENU_EXTENSION = "addFindMenu";
	/* To-Do Ends */
	public static final String MAIN_LOGIN_EXTENSION = "mainLogin"; 
	
	public static final String IS_PLUGIN = "isPlugin"; // used in subclasses to
														// indicate
	// that in onCreate() we are in a plugin.. not sure

	// Mostly for Function Plugins
	private static ArrayList<Plugin> plugins = null; // new ArrayList<Plugin>();
	
	// Our one and only (sometimes) Find Plugin
	public static FindPlugin mFindPlugin = null;
	private Activity mMainActivity = null;


	private FindPluginManager(Activity activity) {
		mMainActivity = activity;
	}

	public static FindPluginManager initInstance(Activity activity) {
		sInstance = new FindPluginManager(activity);
		sInstance.initFromResource(activity, R.raw.plugins_preferences);
		return sInstance;
	}

	public static FindPluginManager getInstance() {
		assert (sInstance != null);
		return sInstance;
	}

	/**
	 * Reads the plugins_xml file and creates plugin objects, storing
	 * them in the plugins list.
	 * @param context
	 * @param plugins_xml
	 */
	public void initFromResource(Context context, int plugins_xml){	
		plugins = new ArrayList<Plugin>();
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try{
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream istream = context.getResources().openRawResource(plugins_xml);
			Document document = builder.parse(istream);
			XPath xpath = XPathFactory.newInstance().newXPath();
			NodeList plugin_nodes = (NodeList)xpath.evaluate("PluginsPreferences/FindPlugins/Plugin", document, XPathConstants.NODESET);
			for(int k = 0; k < plugin_nodes.getLength(); ++k){

				if (plugin_nodes.item(k).getAttributes().getNamedItem("active").getTextContent().compareTo("true") == 0)  {
					Plugin p = null;
					if (plugin_nodes.item(k).getAttributes().getNamedItem("type").getTextContent().equals("find") ) {
						p = new FindPlugin(mMainActivity, plugin_nodes.item(k));
						mFindPlugin = (FindPlugin) p;
						plugins.add(mFindPlugin);	
					} else if (plugin_nodes.item(k).getAttributes().getNamedItem("type").getTextContent().equals("function") ) {
						p = new FunctionPlugin(mMainActivity, plugin_nodes.item(k));
						plugins.add(p);
					}
					else {
						// Do sth for other types in the future			
					}
					Log.i(TAG, "Plugin " + p.toString());
				}
			}
			Log.i(TAG, "# of plugins = " + plugins.size());
		} catch (ParserConfigurationException e) {
			Log.e(TAG, "Failed to load plugin");
			Log.e(TAG, "reason: " + e);
			Log.e(TAG, "stack trace: ");
			e.printStackTrace();
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();			
		} catch (SAXException e) {
			Log.e(TAG, "Failed to load plugin");
			Log.e(TAG, "reason: " + e);
			Log.e(TAG, "stack trace: ");
			e.printStackTrace();
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();				
		} catch (IOException e) {
			Log.e(TAG, "Failed to load plugin");
			Log.e(TAG, "reason: " + e);
			Log.e(TAG, "stack trace: ");
			e.printStackTrace();
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();				
		} catch (XPathExpressionException e) {
			Log.e(TAG, "Failed to load plugin");
			Log.e(TAG, "reason: " + e);
			Log.e(TAG, "stack trace: ");
			e.printStackTrace();
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();				
		} catch (DOMException e) {
			Log.e(TAG, "Failed to load plugin");
			Log.e(TAG, "reason: " + e);
			Log.e(TAG, "stack trace: ");
			e.printStackTrace();
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();				
		} catch (ClassNotFoundException e) {
			Log.e(TAG, "Failed to load plugin");
			Log.e(TAG, "reason: " + e);
			Log.e(TAG, "stack trace: ");
			e.printStackTrace();
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();				
		}
	}

	/**
	 * Returns all plugins
	 * @return
	 */
	public ArrayList<Plugin> getPlugins() {
		return plugins;
	}
	
	/**
	 * Returns a FunctionPlugin by extension point
	 * @return
	 */
	public static FunctionPlugin getFunctionPlugin(String extensionType) {
		FunctionPlugin plugin = null;
		for (Plugin p : plugins) {
			if (p instanceof FunctionPlugin) {
				Log.i(TAG, "Function plugin " + p.toString());
				if ( ((FunctionPlugin) p).mExtensionPoint.equals(extensionType))
					plugin = (FunctionPlugin) p;
			}
		}
		return plugin;
	}

	
	/**
	 * Returns FunctionPlugins by extension point
	 * @return
	 */
	public static ArrayList<FunctionPlugin> getFunctionPlugins(String extensionType) {
		ArrayList<FunctionPlugin> list = new ArrayList<FunctionPlugin>();
		if (plugins == null) return list;
		for (Plugin plugin : plugins) {
			if (plugin instanceof FunctionPlugin) {
				Log.i(TAG, "Function plugin " + plugin.toString());
				FunctionPlugin fPlugin = (FunctionPlugin) plugin;
				if (fPlugin.mExtensionPoint.equals(extensionType))
					list.add(fPlugin);
			}
		}
		return list;
	}
	
	/**
	 * Returns all FunctionPlugins
	 * @return
	 */
	public static ArrayList<FunctionPlugin> getFunctionPlugins() {
		ArrayList<FunctionPlugin> list = new ArrayList<FunctionPlugin>();
		if (plugins == null) return list;
		for (Plugin plugin : plugins) {
			if (plugin instanceof FunctionPlugin) {
				Log.i(TAG, "Function plugin " + plugin.toString());
				FunctionPlugin fPlugin = (FunctionPlugin) plugin;
				list.add(fPlugin);
			}
		}
		return list;
	}
	
	/**
	 * Returns a list of all services created by plugins.
	 * @return
	 */
	public static ArrayList<Class<Service>> getAllServices() {
		ArrayList<Class<Service>> list = (ArrayList<Class<Service>>) new ArrayList<Class<Service>>();
		for (Plugin plugin : plugins) {
			if (plugin instanceof FunctionPlugin) {
				Log.i(TAG, "Function plugin " + plugin.toString());
				FunctionPlugin fPlugin = (FunctionPlugin) plugin;
				if (fPlugin.getmServices().size() > 0) {
					list.addAll(fPlugin.getmServices());
				}
			}
		}
		return list;
	}

}
