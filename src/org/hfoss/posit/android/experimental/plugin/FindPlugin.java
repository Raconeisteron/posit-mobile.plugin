package org.hfoss.posit.android.experimental.plugin;

import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.activity.FindActivity;
import org.hfoss.posit.android.experimental.api.activity.ListFindsActivity;
import org.hfoss.posit.android.experimental.api.activity.SettingsActivity;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

import android.app.Activity;
import android.util.Log;

public class FindPlugin extends Plugin {

	public static String mAddFindLayout = null;
	public static String mListFindLayout = null;
	public static String mMainIcon = null;
	public static String mAddButtonLabel = null;
	public static String mListButtonLabel = null;
	public static String mExtraButtonLabel = null;
	public static String mExtraButtonLabel2 = null;
	
	//protected Activity mMainActivity = null;
	protected Class<Find> mFindClass;
	protected Class<FindActivity> mFindActivityClass = null;
	protected Class<ListFindsActivity> mListFindsActivityClass = null;
	protected Class<Activity> mExtraActivityClass = null;
	protected Class<Activity> mExtraActivityClass2 = null;
	protected Class<Activity> mLoginActivityClass = null;
	
	public FindPlugin(Activity activity, Node node) throws DOMException, ClassNotFoundException {
		mMainActivity = activity;
		String package_name = node.getAttributes().getNamedItem("package").getTextContent();
		String find_factory_name = node.getAttributes().getNamedItem("find_factory").getTextContent();
		String db_manager_name = node.getAttributes().getNamedItem("find_data_manager").getTextContent();
		String findclass_name = node.getAttributes().getNamedItem("find_class").getTextContent();
		String findactivity_name = node.getAttributes().getNamedItem("find_activity_class").getTextContent();
		String listfindsactivity_name = node.getAttributes().getNamedItem("list_finds_activity_class").getTextContent();
		String extra_activity_name = node.getAttributes().getNamedItem("extra_activity_class").getTextContent();
		String extra_activity_name2 = node.getAttributes().getNamedItem("extra_activity_class2").getTextContent();
		
		Node aNode = node.getAttributes().getNamedItem("login_activity_class");
		String login_activity_name = "";
		if (aNode != null) 
			login_activity_name = node.getTextContent();
		//String login_activity_name = node.getAttributes().getNamedItem("login_activity_class").getTextContent();

		mMainIcon = node.getAttributes().getNamedItem("main_icon").getTextContent();
		mAddButtonLabel = node.getAttributes().getNamedItem("main_add_button_label").getTextContent();
		mListButtonLabel = node.getAttributes().getNamedItem("main_list_button_label").getTextContent();
		mExtraButtonLabel = node.getAttributes().getNamedItem("main_extra_button_label").getTextContent();
		mExtraButtonLabel2 = node.getAttributes().getNamedItem("main_extra_button_label2").getTextContent();
		mPreferences = node.getAttributes().getNamedItem("preferences_xml").getTextContent();
		mAddFindLayout = node.getAttributes().getNamedItem("add_find_layout").getTextContent();
		mListFindLayout = node.getAttributes().getNamedItem("list_find_layout").getTextContent();
		
		
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
