package org.hfoss.posit.android.photofind;

import org.hfoss.posit.android.Find;
import org.hfoss.posit.android.FindFactory;

import android.content.Context;

public class PhotoFindFactory extends FindFactory {
	private static PhotoFindFactory sInstance = null;
	
	public static PhotoFindFactory getInstance(){
		if(sInstance == null){
			initInstance();
		}
		
		return sInstance;
	}
	
	public static void initInstance(){
		assert(sInstance == null);
		
		sInstance = new PhotoFindFactory();
	}
	
	public Find createNewFind(Context context){
		return new PhotoFind(context);
	}
	
	public Find createNewFind(Context context, long id){
		return new PhotoFind(context, id);
	}
	
	public Find createNewFind(Context context, String guid){
		return new PhotoFind(context, guid);
	}
}
