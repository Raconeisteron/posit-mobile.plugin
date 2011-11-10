package org.hfoss.posit.android.experimental.plugin;

import org.hfoss.posit.android.experimental.api.activity.FindActivity;
import org.hfoss.posit.android.experimental.api.activity.ListFindsActivity;

import android.app.Activity;

/**
* Convenience class to quickly get right find activity object without having to do lots of chained calls  
*/
public class FindActivityProvider {
	private FindActivityProvider(){} // don't instantiate this class
	
	public static Class<FindActivity> getFindActivityClass(){
		return FindPluginManager.mFindPlugin.getmFindActivityClass();
	}

	public static Class<ListFindsActivity> getListFindsActivityClass(){
		return FindPluginManager.mFindPlugin.getmListFindsActivityClass();
	}
	
	public static Class<Activity> getLoginActivityClass(){
		return FindPluginManager.mFindPlugin.getmLoginActivityClass();
	}

	public static Class<Activity> getExtraActivityClass(){
		return FindPluginManager.mFindPlugin.getmExtraActivityClass();
	}
	
	public static Class<Activity> getExtraActivityClass2(){
		return FindPluginManager.mFindPlugin.getmExtraActivityClass2();
	}
}
