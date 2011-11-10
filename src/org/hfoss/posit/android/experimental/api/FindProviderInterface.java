package org.hfoss.posit.android.experimental.api;

import android.content.Context;

public interface FindProviderInterface {
	public Find createNewFind(Context context);
	
	public Find createNewFind(Context context, long id);
	
	public Find createNewFind(Context context, String guid);
}
