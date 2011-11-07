package org.hfoss.posit.android.experimental.plugin;

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

import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.FindFactory;
import org.hfoss.posit.android.experimental.api.activity.FindActivity;
import org.hfoss.posit.android.experimental.api.activity.ListFindsActivity;
import org.hfoss.posit.android.experimental.api.activity.SettingsActivity;
import org.hfoss.posit.android.experimental.api.database.DbManager;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class FindPluginManager {
	private static FindPluginManager sInstance = null;

	private static final String TAG = "FindPluginManager";

	public static final String MAIN_MENU_EXTENSION = "mainMenu";
	public static final String LIST_MENU_EXTENSION = "listMenu";
	
	public static final String IS_PLUGIN = "isPlugin"; // used in subclasses to
														// indicate
	// that in onCreate() we are in a plugin.. not sure

	// Mostly for Function Plugins
	private static ArrayList<Plugin> plugins = new ArrayList<Plugin>();
	
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

	public void initFromResource(Context context, int plugins_xml){		
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
					if (plugin_nodes.item(k).getAttributes().getNamedItem("type").getTextContent().equals("function") ) {
						p = new FunctionPlugin(mMainActivity, plugin_nodes.item(k));
						plugins.add(p);
					}
					else {
						p = new FindPlugin(mMainActivity, plugin_nodes.item(k));
						mFindPlugin = (FindPlugin) p;
						plugins.add(mFindPlugin);						
					}
					Log.i(TAG, "Plugin " + p.toString());
				}
			}
			Log.i(TAG, "# of plugins = " + plugins.size());
		} catch (ParserConfigurationException e) {
			Log.i(TAG, "Failed to load plugin");
			Log.i(TAG, "reason: " + e);
			Log.i(TAG, "stack trace: " + e.getStackTrace().toString());
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();			
		} catch (SAXException e) {
			Log.i(TAG, "Failed to load plugin");
			Log.i(TAG, "reason: " + e);
			Log.i(TAG, "stack trace: " + e.getStackTrace().toString());
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();				
		} catch (IOException e) {
			Log.i(TAG, "Failed to load plugin");
			Log.i(TAG, "reason: " + e);
			Log.i(TAG, "stack trace: " + e.getStackTrace().toString());
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();				
		} catch (XPathExpressionException e) {
			Log.i(TAG, "Failed to load plugin");
			Log.i(TAG, "reason: " + e);
			Log.i(TAG, "stack trace: " + e.getStackTrace().toString());
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();				
		} catch (DOMException e) {
			Log.i(TAG, "Failed to load plugin");
			Log.i(TAG, "reason: " + e);
			Log.i(TAG, "stack trace: " + e.getStackTrace().toString());
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();				
		} catch (ClassNotFoundException e) {
			Log.i(TAG, "Failed to load plugin");
			Log.i(TAG, "reason: " + e);
			Log.i(TAG, "stack trace: " + e.getStackTrace().toString());
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
	 * Returns FunctionPlugins by extension point
	 * @return
	 */
	public static ArrayList<FunctionPlugin> getFunctionPlugins(String extensionType) {
		ArrayList<FunctionPlugin> list = new ArrayList<FunctionPlugin>();
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

}
