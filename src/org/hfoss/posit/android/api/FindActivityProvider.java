package org.hfoss.posit.android.api;

/**
* Convenience class to quickly get right find activity object without having to do lots of chained calls  
*/
public class FindActivityProvider {
	public static Class<FindActivity> getFindActivityClass(){
		return FindPluginManager.getInstance().getFindActivityClass();
	}

	public static Class<ListFindsActivity> getListFindsActivityClass(){
		return FindPluginManager.getInstance().getListFindsActivityClass();
	}
}
