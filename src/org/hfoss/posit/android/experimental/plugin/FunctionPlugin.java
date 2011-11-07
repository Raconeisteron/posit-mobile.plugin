package org.hfoss.posit.android.experimental.plugin;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

import android.app.Activity;

public class FunctionPlugin extends Plugin {
	
	// Extension point in PositMain
	protected String mExtensionPoint = null;
	protected Class<Activity> mMenuActivity;
	protected String mMenuIcon;
	protected String mMenuTitle;
	
	public FunctionPlugin (Activity activity, Node node) throws DOMException, ClassNotFoundException {
		mMainActivity = activity;
		
		name = node.getAttributes().getNamedItem("name").getTextContent();
		type = node.getAttributes().getNamedItem("type").getTextContent();
		mExtensionPoint = node.getAttributes().getNamedItem("extensionPoint").getTextContent();
		mMenuActivity = (Class<Activity>) Class.forName(node.getAttributes().getNamedItem("menuActivity").getTextContent());
		mMenuIcon = node.getAttributes().getNamedItem("menuIcon").getTextContent();
		mMenuTitle = node.getAttributes().getNamedItem("menuTitle").getTextContent();
	}
	
	public String getmExtensionPoint() {
		return mExtensionPoint;
	}




	public void setmExtensionPoint(String mExtensionPoint) {
		this.mExtensionPoint = mExtensionPoint;
	}




	public Class<Activity> getmMenuActivity() {
		return mMenuActivity;
	}




	public void setmMenuActivity(Class<Activity> mMenuActivity) {
		this.mMenuActivity = mMenuActivity;
	}




	public String getmMenuIcon() {
		return mMenuIcon;
	}




	public void setmMenuIcon(String mMenuIcon) {
		this.mMenuIcon = mMenuIcon;
	}




	public String getmMenuTitle() {
		return mMenuTitle;
	}




	public void setmMenuTitle(String mMenuTitle) {
		this.mMenuTitle = mMenuTitle;
	}




	public String toString() {
		return super.toString() + " " + mExtensionPoint;
	}

}
