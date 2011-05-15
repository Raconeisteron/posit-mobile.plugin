package org.hfoss.posit.android.acdivocafind;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.FindFactory;

import android.content.Context;

public class AcdiVocaFindFactory extends FindFactory {
	private static AcdiVocaFindFactory sInstance = null;
	
	public static AcdiVocaFindFactory getInstance(){
		if(sInstance == null){
			initInstance();
		}
		
		return sInstance;
	}
	
	public static void initInstance(){
		assert(sInstance == null);
		
		sInstance = new AcdiVocaFindFactory();
	}
	
	public Find createNewFind(Context context){
		return new AcdiVocaFind(context);
	}
	
	public Find createNewFind(Context context, long id){
		return new AcdiVocaFind(context, id);
	}
	
	public Find createNewFind(Context context, String guid){
		return new AcdiVocaFind(context, guid);
	}
}
