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

	public static final String IS_PLUGIN = "isPlugin"; // used in subclasses to
														// indicate
	// that in onCreate() we are in a plugin.. not sure

	private ArrayList<Plugin> plugins = new ArrayList<Plugin>();

	// NOTE: When multiple plugins are implemented these should disappear
	// from here and possibly move to Plugin.java??
	private Activity mMainActivity = null;
	private FindFactory mFindFactory = null;

	private DbManager mDbManager = null;
	private Class<Find> mFindClass;
	private Class<FindActivity> mFindActivityClass = null;
	private Class<ListFindsActivity> mListFindsActivityClass = null;
	private Class<Activity> mExtraActivityClass = null;
	private Class<Activity> mExtraActivityClass2 = null;
	private Class<Activity> mLoginActivityClass = null;
 
	// Extension point in PositMain
	public static String mExtensionPoint = null;
	public static Class<Activity> mMenuActivity;
	public static String mMenuIcon;
	public static String mMenuTitle;
	
	// Extension point in ListFinds
	public static String mListFindsMenuExtensionPoint = null;
	public static Class<Activity> mListFindsMenuActivity;
	public static String mListFindsMenuIcon;
	public static String mListFindsMenuTitle;
	
	

	public static String mPreferences = null; // Shared preferences XML for
												// Settings

	public static String mAddFindLayout = null;
	public static String mListFindLayout = null;
	public static String mMainIcon = null;
	public static String mAddButtonLabel = null;
	public static String mListButtonLabel = null;
	public static String mExtraButtonLabel = null;
	public static String mExtraButtonLabel2 = null;

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

	@SuppressWarnings("unchecked")
	public void initFromResource(Context context, int plugins_xml){		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try{
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream istream = context.getResources().openRawResource(plugins_xml);
			Document document = builder.parse(istream);
			XPath xpath = XPathFactory.newInstance().newXPath();
			NodeList plugin_nodes = (NodeList)xpath.evaluate("PluginsPreferences/FindPlugins/Plugin", document, XPathConstants.NODESET);
			for(int ii = 0; ii < plugin_nodes.getLength(); ++ii){
				
				if(plugin_nodes.item(ii).getAttributes().getNamedItem("active").getTextContent().compareTo("true") == 0){

					// Function plugin fields -- menu function TODO: make this a separate class
					if (plugin_nodes.item(ii).getAttributes().getNamedItem("type").getTextContent().equals("function") 
							&& plugin_nodes.item(ii).getAttributes().getNamedItem("extensionPoint").getTextContent().equals("mainMenu")) {
						mExtensionPoint = plugin_nodes.item(ii).getAttributes().getNamedItem("extensionPoint")
								.getTextContent();
						mMenuActivity = (Class<Activity>) Class.forName(plugin_nodes.item(ii).getAttributes()
								.getNamedItem("menuActivity").getTextContent());
						mMenuIcon = plugin_nodes.item(ii).getAttributes().getNamedItem("menuIcon").getTextContent();
						mMenuTitle = plugin_nodes.item(ii).getAttributes().getNamedItem("menuTitle").getTextContent();
					} else if (plugin_nodes.item(ii).getAttributes().getNamedItem("type").getTextContent().equals("function") 
							&& plugin_nodes.item(ii).getAttributes().getNamedItem("extensionPoint").getTextContent().equals("listMenu")) {
						mListFindsMenuExtensionPoint = plugin_nodes.item(ii).getAttributes().getNamedItem("extensionPoint")
								.getTextContent();
						mListFindsMenuActivity = (Class<Activity>) Class.forName(plugin_nodes.item(ii).getAttributes()
								.getNamedItem("menuActivity").getTextContent());
						mListFindsMenuIcon = plugin_nodes.item(ii).getAttributes().getNamedItem("menuIcon").getTextContent();
						mListFindsMenuTitle = plugin_nodes.item(ii).getAttributes().getNamedItem("menuTitle").getTextContent();
					}
					else {
					String package_name = plugin_nodes.item(ii).getAttributes().getNamedItem("package").getTextContent();
					String find_factory_name = plugin_nodes.item(ii).getAttributes().getNamedItem("find_factory").getTextContent();
					String db_manager_name = plugin_nodes.item(ii).getAttributes().getNamedItem("find_data_manager").getTextContent();
					String findclass_name = plugin_nodes.item(ii).getAttributes().getNamedItem("find_class").getTextContent();
					String findactivity_name = plugin_nodes.item(ii).getAttributes().getNamedItem("find_activity_class").getTextContent();
					String listfindsactivity_name = plugin_nodes.item(ii).getAttributes().getNamedItem("list_finds_activity_class").getTextContent();
					String extra_activity_name = plugin_nodes.item(ii).getAttributes().getNamedItem("extra_activity_class").getTextContent();
					String extra_activity_name2 = plugin_nodes.item(ii).getAttributes().getNamedItem("extra_activity_class2").getTextContent();
					
					Node node = plugin_nodes.item(ii).getAttributes().getNamedItem("login_activity_class");
					String login_activity_name = "";
					if (node != null) 
						login_activity_name = node.getTextContent();
					//String login_activity_name = plugin_nodes.item(ii).getAttributes().getNamedItem("login_activity_class").getTextContent();

					mMainIcon = plugin_nodes.item(ii).getAttributes().getNamedItem("main_icon").getTextContent();
					mAddButtonLabel = plugin_nodes.item(ii).getAttributes().getNamedItem("main_add_button_label").getTextContent();
					mListButtonLabel = plugin_nodes.item(ii).getAttributes().getNamedItem("main_list_button_label").getTextContent();
					mExtraButtonLabel = plugin_nodes.item(ii).getAttributes().getNamedItem("main_extra_button_label").getTextContent();
					mExtraButtonLabel2 = plugin_nodes.item(ii).getAttributes().getNamedItem("main_extra_button_label2").getTextContent();
					mPreferences = plugin_nodes.item(ii).getAttributes().getNamedItem("preferences_xml").getTextContent();
					mAddFindLayout = plugin_nodes.item(ii).getAttributes().getNamedItem("add_find_layout").getTextContent();
					mListFindLayout = plugin_nodes.item(ii).getAttributes().getNamedItem("list_find_layout").getTextContent();
					
					
					@SuppressWarnings({ "rawtypes" })
					Class new_class = Class.forName(find_factory_name);
					//mFindFactory = (FindFactory)new_class.getMethod("getInstance", null).invoke(null, null);
					
					new_class = Class.forName(db_manager_name);
					//mDbManager = (DbManager)new_class.getMethod("getInstance", null).invoke(null, null);

					mFindClass = (Class<Find>)Class.forName(findclass_name);
					mFindActivityClass = (Class<FindActivity>)Class.forName(findactivity_name);
					mListFindsActivityClass = (Class<ListFindsActivity>)Class.forName(listfindsactivity_name);
					if (!login_activity_name.equals(""))
						mLoginActivityClass = (Class<Activity>)Class.forName(login_activity_name); // Changed
					if (!extra_activity_name.equals(""))	
						mExtraActivityClass = (Class<Activity>) Class
								.forName(package_name + "."	+ extra_activity_name);
					if (!extra_activity_name2.equals(""))
						mExtraActivityClass2 = (Class<Activity>) Class
								.forName(package_name + "."	+ extra_activity_name2);
						
					Log.i(TAG,"Loading preferences for Settings Activity");
					SettingsActivity.loadPluginPreferences(mMainActivity, mPreferences);

					// Remove break to load more than one plugin
					//break;
					}
				}
			}
		} catch (ParserConfigurationException ex)

		{
			Log.i(TAG, "Failed to load plugin");
			Log.i(TAG, "reason: " + ex);
			Log.i(TAG, "stack trace: " + ex.getStackTrace().toString());
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();

		} catch (SAXException ex)

		{
			Log.i(TAG, "Failed to load plugin");
			Log.i(TAG, "reason: " + ex);
			Log.i(TAG, "stack trace: " + ex.getStackTrace().toString());
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();

		} catch (IOException ex)

		{
			Log.i(TAG, "Failed to load plugin");
			Log.i(TAG, "reason: " + ex);
			Log.i(TAG, "stack trace: " + ex.getStackTrace().toString());
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();

		} catch (XPathExpressionException ex)

		{
			Log.i(TAG, "Failed to load plugin");
			Log.i(TAG, "reason: " + ex);
			Log.i(TAG, "stack trace: " + ex.getStackTrace().toString());
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();

		}

		catch (Exception ex) {
			Log.i(TAG, "Failed to load plugin");
			Log.i(TAG, "reason: " + ex);
			Log.i(TAG, "stack trace: " + ex.getStackTrace().toString());
			Toast.makeText(mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
					Toast.LENGTH_LONG).show();
			mMainActivity.finish();
		}
	}

	public FindFactory getFindFactory() {
		return mFindFactory;
	}

	// public FindDataManager getFindDataManager(){
	// return mFindDataManager;
	// }
	//	

	public Class<FindActivity> getFindActivityClass() {
		return mFindActivityClass;
	}

	public Class<Find> getFindClass() {
		return mFindClass;
	}

	public Class<ListFindsActivity> getListFindsActivityClass() {
		return mListFindsActivityClass;
	}

	public Class<Activity> getExtraActivityClass() {
		return mExtraActivityClass;
	}

	public void setExtraActivityClass(Class<Activity> extraActivityClass) {
		mExtraActivityClass = extraActivityClass;
	}

	public Class<Activity> getExtraActivityClass2() {
		return mExtraActivityClass2;
	}

	public void setExtraActivityClass2(Class<Activity> extraActivityClass2) {
		mExtraActivityClass2 = extraActivityClass2;
	}

	public Class<Activity> getLoginActivityClass() {
		return mLoginActivityClass;
	}

	public void setLoginActivityClass(Class<Activity> loginActivityClass) {
		mLoginActivityClass = loginActivityClass;
	}

	public ArrayList<Plugin> getPlugins() {
		return plugins;
	}

}
