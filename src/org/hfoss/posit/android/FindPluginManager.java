package org.hfoss.posit.android;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import android.content.Context;

public class FindPluginManager {
	private static FindPluginManager sInstance = null;
	
	private FindFactory mFindFactory = null;
	
	private FindPluginManager(){
	}
	
	public static FindPluginManager initInstance(Context context){
		sInstance = new FindPluginManager();
		sInstance.initFromResource(context, R.raw.plugins_preferences);
		return sInstance;
	}
	
	public static FindPluginManager getInstance(){
		assert(sInstance != null);
		
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
			for(int ii = 0; ii < plugin_nodes.getLength(); ++ii){
				if(plugin_nodes.item(ii).getAttributes().getNamedItem("active").getTextContent().compareTo("true") == 0){
					String find_factory_name = plugin_nodes.item(ii).getAttributes().getNamedItem("find_factory").getTextContent();
					
					@SuppressWarnings({ "rawtypes" })
					Class new_class = Class.forName(find_factory_name);

					mFindFactory = (FindFactory)new_class.getMethod("getInstance", null).invoke(null, null);
					
					break;
				}
			}
		}catch(Exception ex)
		{
			return;
		}
	}
	
	public FindFactory getFindFactory(){
		return mFindFactory;
	}
}
