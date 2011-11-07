package org.hfoss.posit.android.experimental.plugin;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

import android.app.Activity;

public class FunctionPlugin extends Plugin {
	
	// Extension point in PositMain
	public static String mExtensionPoint = null;
	public static Class<Activity> mMenuActivity;
	public static String mMenuIcon;
	public static String mMenuTitle;
	
	public FunctionPlugin (Activity activity, Node node) throws DOMException, ClassNotFoundException {
		mMainActivity = activity;
		mExtensionPoint = node.getAttributes().getNamedItem("extensionPoint")
		.getTextContent();
		mMenuActivity = (Class<Activity>) Class.forName(node.getAttributes()
				.getNamedItem("menuActivity").getTextContent());
		mMenuIcon = node.getAttributes().getNamedItem("menuIcon").getTextContent();
		mMenuTitle = node.getAttributes().getNamedItem("menuTitle").getTextContent();
	}

}
