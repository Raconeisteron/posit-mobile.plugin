package org.hfoss.posit.android.api;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.hfoss.posit.android.R;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

public class FindPluginManager {
	private static FindPluginManager sInstance = null; 
	
	private static final String TAG = "FindPluginManager";
	
	private Activity mMainActivity = null;
	
	private FindFactory mFindFactory = null;
	private FindDataManager mFindDataManager = null;
	private Class<FindActivity> mFindActivityClass = null;
	private Class<ListFindsActivity> mListFindsActivityClass = null;
	
	public static String mMainIcon = null;
	
	private FindPluginManager(Activity activity){
		mMainActivity = activity;
	}
	
	public static FindPluginManager initInstance(Activity activity){
		sInstance = new FindPluginManager(activity);
		sInstance.initFromResource(activity, R.raw.plugins_preferences);
		return sInstance;
	}
	
	public static FindPluginManager getInstance(){
		assert(sInstance != null);
		
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
					String package_name = plugin_nodes.item(ii).getAttributes().getNamedItem("package").getTextContent();
					String find_factory_name = plugin_nodes.item(ii).getAttributes().getNamedItem("find_factory").getTextContent();
					String find_data_manager_name = plugin_nodes.item(ii).getAttributes().getNamedItem("find_data_manager").getTextContent();
					String findactivity_name = plugin_nodes.item(ii).getAttributes().getNamedItem("findactivity_class").getTextContent();
					String listfindsactivity_name = plugin_nodes.item(ii).getAttributes().getNamedItem("listfindsactivity_class").getTextContent();
					mMainIcon = plugin_nodes.item(ii).getAttributes().getNamedItem("main_icon").getTextContent();

					@SuppressWarnings({ "rawtypes" })
					Class new_class = Class.forName(package_name + "." + find_factory_name);
					mFindFactory = (FindFactory)new_class.getMethod("getInstance", null).invoke(null, null);
					
					new_class = Class.forName(package_name + "." + find_data_manager_name);
					mFindDataManager = (FindDataManager)new_class.getMethod("getInstance", null).invoke(null, null);

					mFindActivityClass = (Class<FindActivity>)Class.forName(package_name + "." + findactivity_name);
					mListFindsActivityClass = (Class<ListFindsActivity>)Class.forName(package_name + "." + listfindsactivity_name);
					
					break;
				}
			}
		}catch(Exception ex)
		{
			Log.i(TAG, "Failed to load plugin");
			Log.i(TAG, "reason: " + ex.getMessage());
			
			mMainActivity.finish();
		}
	}
	
	public FindFactory getFindFactory(){
		return mFindFactory;
	}
	
	public FindDataManager getFindDataManager(){
		return mFindDataManager;
	}
	
	public Class<FindActivity> getFindActivityClass(){
		return mFindActivityClass;
	}
	
	public Class<ListFindsActivity> getListFindsActivityClass(){
		return mListFindsActivityClass;
	}
}
