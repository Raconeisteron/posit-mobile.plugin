package org.hfoss.posit.android.plugin;

import org.hfoss.posit.android.api.Find;

import android.content.Context;
import android.content.Intent;
import android.view.View;

public interface AddFindPluginCallback {
	
	public void menuItemSelectedCallback(Context context, Find find, View view, Intent intent);
	public void onActivityResultCallback(Context context, Find find, View view, Intent intent);

}
