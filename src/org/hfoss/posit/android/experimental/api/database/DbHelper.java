package org.hfoss.posit.android.experimental.api.database;

import org.hfoss.posit.android.experimental.api.Find;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import android.content.Context;

public class DbHelper {

	public static DbManager dbManager;

	public static DbManager getDbManager(Context context) {
		if (dbManager == null) {
			dbManager = (DbManager) OpenHelperManager.getHelper(context);
			return dbManager;
		} else
			return dbManager;
	}

	public static void releaseDbManager() {
		dbManager = null;
		OpenHelperManager.releaseHelper();
	}
	
	public static Dao<Find, Integer> getFindDao(Context context) {
		return getDbManager(context).getFindDao();
	}

}
