package org.hfoss.posit.android.photofind;

import org.hfoss.posit.android.Find;

import android.content.Context;

public class PhotoFind extends Find{
	// TODO: this are dummy constructors until Find and PhotoFind classes are swapped names
	public PhotoFind(Context context){
		super(context);
	}
	
	public PhotoFind(Context context, long id){
		super(context, id);
	}
	
	public PhotoFind(Context context, String guid){
		super(context, guid);
	}
}
