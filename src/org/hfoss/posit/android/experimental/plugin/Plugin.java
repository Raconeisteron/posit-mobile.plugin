package org.hfoss.posit.android.experimental.plugin;

import org.hfoss.posit.android.experimental.api.FindFactory;
import org.hfoss.posit.android.experimental.api.activity.FindActivity;
import org.hfoss.posit.android.experimental.api.activity.ListFindsActivity;
import org.hfoss.posit.android.experimental.api.activity.SettingsActivity;

import android.app.Activity;

public class Plugin {

	protected static final String TAG = "Plugin";
	
	public static String mPreferences = null;
	protected String name;
	protected String type;
	protected Class<Activity> activity;
	
	protected Activity mMainActivity;
	public static String getmPreferences() {
		return mPreferences;
	}
	public static void setmPreferences(String mPreferences) {
		Plugin.mPreferences = mPreferences;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Activity getmMainActivity() {
		return mMainActivity;
	}
	public void setmMainActivity(Activity mMainActivity) {
		this.mMainActivity = mMainActivity;
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}

	public Class<Activity> getActivity() {
		return activity;
	}
	public void setActivity(Class<Activity> activity) {
		this.activity = activity;
	}
	public String toString() {
		return name + " " + type;
	}
}
