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
	protected Boolean activityReturnsResult = false;
	protected int activityResultAction = 0;
	
	public FunctionPlugin (Activity activity, Node node) throws DOMException, ClassNotFoundException {
		mMainActivity = activity;
		
		// Perhaps this can be done more generally, rather than for each possible node
		Node aNode = null;
		aNode = node.getAttributes().getNamedItem("name");
		if (aNode != null)
			name = aNode.getTextContent();
		aNode = node.getAttributes().getNamedItem("type");
		if (aNode != null)
			type = aNode.getTextContent();
		aNode = node.getAttributes().getNamedItem("activity");
		if (aNode != null) 
			this.activity = (Class<Activity>) Class.forName(aNode.getTextContent());
		aNode = node.getAttributes().getNamedItem("extensionPoint");
		if (aNode != null)
			mExtensionPoint = aNode.getTextContent();
		aNode = node.getAttributes().getNamedItem("menuActivity");
		if (aNode != null)
			mMenuActivity = (Class<Activity>) Class.forName(aNode.getTextContent());
		aNode = node.getAttributes().getNamedItem("menuIcon");
		if (aNode != null)
			mMenuIcon = aNode.getTextContent();
		aNode = node.getAttributes().getNamedItem("menuTitle");
		if (aNode != null)
			mMenuTitle = aNode.getTextContent();
		aNode = node.getAttributes().getNamedItem("activity_returns_result");
		if (aNode != null) 
			activityReturnsResult = Boolean.valueOf(aNode.getTextContent());
		aNode = node.getAttributes().getNamedItem("activity_result_action");
		if (aNode != null)
			activityResultAction = Integer.parseInt(aNode.getTextContent());
	}
	
	public Boolean getActivityReturnsResult() {
		return activityReturnsResult;
	}

	public void setActivityReturnsResult(Boolean activityReturnsResult) {
		this.activityReturnsResult = activityReturnsResult;
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
	
	public int getActivityResultAction() {
		return activityResultAction;
	}

	public void setActivityResultAction(int activityResultAction) {
		this.activityResultAction = activityResultAction;
	}

	public String toString() {
		return super.toString() + " " + mExtensionPoint;
	}

}
