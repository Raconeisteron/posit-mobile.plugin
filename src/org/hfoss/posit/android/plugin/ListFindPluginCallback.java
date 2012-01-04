package org.hfoss.posit.android.plugin;

import org.hfoss.posit.android.api.Find;

import android.content.Context;
import android.view.View;

public interface ListFindPluginCallback {
	
	public void listFindCallback(Context context, Find find, View view);

}
