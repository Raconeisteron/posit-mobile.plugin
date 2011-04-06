package org.hfoss.posit.android;

import android.content.Context;

/**
* Convenience class to quickly get right find object without having to type  
*/
public class FindProvider{
	
	public static Find createNewFind(Context context){
		return FindPluginManager.getInstance().getFindFactory().createNewFind(context);
	}
	
	public static Find createNewFind(Context context, long id){
		return FindPluginManager.getInstance().getFindFactory().createNewFind(context, id);
	}
	
	public static Find createNewFind(Context context, String guid){
		return FindPluginManager.getInstance().getFindFactory().createNewFind(context, guid);
	}
}
