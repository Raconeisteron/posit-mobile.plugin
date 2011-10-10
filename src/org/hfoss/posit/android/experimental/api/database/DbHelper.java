package org.hfoss.posit.android.experimental.api.database;

import org.hfoss.posit.android.experimental.api.Find;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import android.content.Context;

/**
 * Class that facilitates the use of the ORMlite framework for managing the
 * database. This class should be used when you're NOT in a class that extends
 * Activity. First, call getDbManager() to get an instance of the DbManager. Do
 * some database work, then you must call releaseDbManager() to clean things up.
 * 
 * If you are within an Activity, you can extend OrmLiteBaseActivity and use
 * this.getHelper() to get an instance of DbManager, and from there, there is no
 * need to call releaseHelper(), because the superclass handles it for you.
 */
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
