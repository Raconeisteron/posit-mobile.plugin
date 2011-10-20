/*
 * File: AcdiVocaDbHelper.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of the ACDI/VOCA plugin for POSIT, Portable Open Search 
 * and Identification Tool.
 *
 * This plugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (LGPL) as published 
 * by the Free Software Foundation; either version 3.0 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU LGPL along with this program; 
 * if not visit http://www.gnu.org/licenses/lgpl.html.
 * 
 */

package org.hfoss.posit.android.experimental.api.database;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.FindHistory;
import org.hfoss.posit.android.experimental.api.SyncHistory;
import org.hfoss.posit.android.experimental.api.User;
import org.hfoss.posit.android.experimental.plugin.FindPluginManager;
import org.hfoss.posit.android.experimental.plugin.acdivoca.AcdiVocaFind;
import org.hfoss.posit.android.experimental.plugin.acdivoca.AcdiVocaMessage;
import org.hfoss.posit.android.experimental.plugin.acdivoca.AcdiVocaUser;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.DateFormat;
import android.util.Log;

/**
 * The class is the interface with the Database. It controls all Db access and
 * directly handles all Db queries.
 */
public class DbManager extends OrmLiteSqliteOpenHelper {

	private static final String TAG = "DbHelper";

	protected static final String DATABASE_NAME = "posit";
	public static final int DATABASE_VERSION = 2;
	public static final String FIND_TABLE_NAME = "find"; // All find extensions should use this table name

	public static final int DELETE_FIND = 1;
	public static final int UNDELETE_FIND = 0;
	public static final String WHERE_NOT_DELETED = " " + AcdiVocaFind.DELETED + " != " + DELETE_FIND + " ";
	public static final String DATETIME_NOW = "`datetime('now')`";

	public static final String FINDS_HISTORY_TABLE = "acdi_voca_finds_history";
	public static final String HISTORY_ID = "_id";

	// DAO objects used to access the Db tables
	private Dao<User, Integer> userDao = null;
	private Dao<Find, Integer> findDao = null;
	private Dao<FindHistory, Integer> findHistoryDao = null;
	private Dao<SyncHistory, Integer> syncHistoryDao = null;

	/**
	 * Constructor just saves and opens the Db.
	 * 
	 * @param context
	 */
	public DbManager(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/**
	 * Invoked automatically if the Database does not exist.
	 */
	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
		Log.i(TAG, "onCreate");
		Class<Find> findClass = FindPluginManager.getInstance().getFindClass();
		try {
			findClass.getMethod("createTable", ConnectionSource.class).invoke(null, connectionSource);
		} catch (Exception e) {
			e.printStackTrace();
		}
		User.createTable(connectionSource, getUserDao());
		FindHistory.createTable(connectionSource);
		SyncHistory.createTable(connectionSource);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {
			Log.i(TAG, "onUpgrade");
			TableUtils.dropTable(connectionSource, User.class, true);
			TableUtils.dropTable(connectionSource, FindHistory.class, true);
			TableUtils.dropTable(connectionSource, SyncHistory.class, true);

			Class<Find> findClass = FindPluginManager.getInstance().getFindClass();
			TableUtils.dropTable(connectionSource, findClass, true);

			// after we drop the old databases, we create the new ones
			onCreate(db, connectionSource);
		} catch (SQLException e) {
			Log.e(TAG, "Can't drop databases", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Fetches all finds currently in the database.
	 * 
	 * @return A list of all the finds.
	 */
	public List<? extends Find> getAllFinds() {
		List<Find> list = null;
		try {
			list = getFindDao().queryForAll();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;

	}

	public List<Find> getFindsByProjectId(int projectId) {
		List<Find> list = null;
		try {
			QueryBuilder<Find, Integer> builder = getFindDao().queryBuilder();
			Where<Find, Integer> where = builder.where();
			where.eq(Find.PROJECT_ID, projectId);
			PreparedQuery<Find> preparedQuery = builder.prepare();

			list = getFindDao().query(preparedQuery);
		} catch (SQLException e) {
			Log.e(TAG, "Database error getting finds: " + e.getMessage());
		}
		return list;
	}

	/**
	 * Looks up a find by its ID.
	 * 
	 * @param id
	 *            the id of the find to look up
	 * @return the find
	 */
	public Find getFindById(int id) {
		Find find = null;
		try {
			find = getFindDao().queryForId(id);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return find;
	}

	/**
	 * Inserts this find into the database.
	 * 
	 * @param dao
	 *            the DAO object provided by the ORMLite helper class.
	 * @return the number of rows inserted.
	 */

	public int insert(Find find) {
		int rows = 0;
		try {
			find.setAction(FindHistory.ACTION_CREATE);
			rows = getFindDao().create(find);
			if (rows == 1) {
				Log.i(TAG, "Inserted find:  " + this.toString());
				recordChangedFind(new FindHistory(find, FindHistory.ACTION_CREATE));
			} else {
				Log.e(TAG, "Db Error inserting find: " + this.toString());
				rows = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;
	}

	/**
	 * Updates this find in the database with the given values.
	 * 
	 * @param dao
	 *            the DAO provided by the ORMLite helper class.
	 * @param values
	 *            a ContentValues object containing all of the values to update.
	 * @return the number of rows updated.
	 */
	public int update(Find find) {
		int rows = 0;
		try {
			find.setAction(FindHistory.ACTION_UPDATE);
			rows = getFindDao().update(find);
			if (rows == 1) {
				Log.i(TAG, "Updated find:  " + this.toString());
				recordChangedFind(new FindHistory(find, FindHistory.ACTION_UPDATE));
			} else {
				Log.e(TAG, "Db Error updating find: " + this.toString());
				rows = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;
	}

	/**
	 * Deletes this find.
	 * 
	 * @param dao
	 *            the DAO provided by the ORMLite helper class.
	 * @return the number of rows deleted.
	 */
	public int delete(Find find) {
		int rows = 0;
		try {
			rows = getFindDao().delete(find);
			if (rows == 1)
				Log.i(TAG, "Deleted find:  " + this.toString());
			else {
				Log.e(TAG, "Db Error deleting find: " + this.toString());
				rows = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;

	}

	/**
	 * Updates the status of the sync--will eventually use this to restart
	 * interrupted syncs?
	 * 
	 * @param find
	 * @param status
	 *            Constants.TRANSACTING, Constants.SUCCEEDED, or
	 *            Constants.FAILED
	 * @return number of rows updated, 1 if successful
	 */
	public int updateStatus(Find find, int status) {
		int rows = 0;
		try {
			find.setStatus(status);
			rows = getFindDao().update(find);
			if (rows == 1) {
				Log.i(TAG, "Updated find status:  " + this.toString());
			} else {
				Log.e(TAG, "Db Error updating find: " + this.toString());
				rows = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;
	}

	/**
	 * Updates the sync operation in progress for this find during the last
	 * sync.
	 * 
	 * @param find
	 * @param operation Constants.POSTING, Constants.UPDATING, or Constants.DELETING
	 * @return number of rows updated, 1 if successful
	 */

	public int updateSyncOperation(Find find, int operation) {
		int rows = 0;
		try {
			find.setSyncOperation(operation);
			rows = getFindDao().update(find);
			if (rows == 1) {
				Log.i(TAG, "Updated find sync operation:  " + this.toString());
			} else {
				Log.e(TAG, "Db Error updating sync operation in find: " + this.toString());
				rows = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;
	}

	/**
	 * Gets finds changed since the last sync in a given project. TODO: Fix
	 * this, doesn't seem to get finds from the specific project id. Just
	 * returns all finds changed since the last sync.
	 * 
	 * @param projectId
	 *            the id of the project
	 * @return a list of Finds
	 */

	public List<Find> getChangedFinds(int projectId) {
		List<Find> finds = null;
		try {

			Date lastSync = getTimeOfLastSync();

			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
			String lastSyncString = formatter.format(lastSync);

			// Query taken from old POSIT DbHelper.java.. not sure it works
			// properly.
			// I don't think it's properly getting the finds from your current
			// project.
			// It seems to return all changed finds from all the projects.
			GenericRawResults<String[]> raw = getFindHistoryDao().queryRaw(
					"SELECT DISTINCT findhistory" + "." + FindHistory.FIND + ",findhistory" + "."
							+ FindHistory.FIND_ACTION + " FROM findhistory, find" + " WHERE find." + Find.PROJECT_ID
							+ " = " + projectId + " AND " + "findhistory" + "." + FindHistory.FIND_ACTION + "!= '"
							+ FindHistory.ACTION_DELETE + "' AND findhistory." + FindHistory.TIME + " > '"
							+ lastSyncString + "'");

			List<String[]> results = raw.getResults();
			finds = new ArrayList<Find>();

			for (String[] result : results) {
				int findId = Integer.parseInt(result[0]);
				Find find = getFindDao().queryForId(findId);
				finds.add(find);
			}

			Log.i(TAG, "Changed finds: " + finds);

		} catch (SQLException e) {
			Log.e(TAG, "Db error getting finds changed since last sync: " + e.getMessage());
		}
		return finds;
	}

	/**
	 * Gets the date of the last sync.
	 * 
	 * @return the date
	 */
	public Date getTimeOfLastSync() {
		Date lastSync = null;
		try {
			QueryBuilder<SyncHistory, Integer> builder = getSyncHistoryDao().queryBuilder();
			builder.orderBy(SyncHistory.TIME, false);
			PreparedQuery<SyncHistory> query = builder.prepare();
			SyncHistory syncHistory = getSyncHistoryDao().queryForFirst(query);
			if (syncHistory == null) // Never synced before
				lastSync = new Date(0); // Jan. 1st 1970 lol TODO: Better way to
										// do this?
			else
				lastSync = syncHistory.getTime();
		} catch (SQLException e) {
			Log.e(TAG, "Db error getting time of last sync: " + e.getMessage());
		}
		return lastSync;
	}

	/**
	 * Used to track syncs whenever they occur.
	 * 
	 * @param findHistory
	 * @return the number of rows changed if successful
	 */
	public int recordSync(SyncHistory syncHistory) {
		int rows = 0;
		try {
			rows = getSyncHistoryDao().create(syncHistory);
		} catch (SQLException e) {
			Log.e(TAG, "Db error recording a sync in sync_history: " + e.getMessage());
		}
		return rows;
	}

	/**
	 * Used to record a change in the find history table whenever a find is
	 * updated, deleted, or created.
	 * 
	 * @param findHistory
	 * @return the number of rows changed if successful
	 */
	private int recordChangedFind(FindHistory findHistory) {
		int rows = 0;
		try {
			rows = getFindHistoryDao().create(findHistory);
		} catch (SQLException e) {
			Log.e(TAG, "Db error inserting a find history: " + e.getMessage());
		}
		return rows;
	}

	/**
	 * Returns the Database Access Object (DAO) for the AcdiVocaUser class. It
	 * will create it or just give the cached value.
	 */
	public Dao<User, Integer> getUserDao() {
		if (userDao == null) {
			try {
				userDao = getDao(User.class);
			} catch (SQLException e) {
				Log.e(TAG, "Get user DAO failed.");
				e.printStackTrace();
			}
		}
		return userDao;
	}

	/**
	 * Returns the Database Access Object (DAO) for the Find class. It will
	 * create it or just give the cached value.
	 */
	public Dao<Find, Integer> getFindDao() {
		if (findDao == null) {
			Class<Find> findClass = FindPluginManager.getInstance().getFindClass();
			try {
				findDao = getDao(findClass);
			} catch (SQLException e) {
				Log.e(TAG, "Get find DAO failed.");
				e.printStackTrace();
			}
		}
		return findDao;
	}

	/**
	 * Returns the Database Access Object (DAO) for the FindHistory class. It
	 * will create it or just give the cached value.
	 */
	public Dao<FindHistory, Integer> getFindHistoryDao() {
		if (findHistoryDao == null) {
			try {
				findHistoryDao = getDao(FindHistory.class);
			} catch (SQLException e) {
				Log.e(TAG, "Get find DAO failed.");
				e.printStackTrace();
			}
		}
		return findHistoryDao;
	}

	/**
	 * Returns the Database Access Object (DAO) for the AcdiVocaUser class. It
	 * will create it or just give the cached value.
	 */
	public Dao<SyncHistory, Integer> getSyncHistoryDao() {
		if (syncHistoryDao == null) {
			try {
				syncHistoryDao = getDao(SyncHistory.class);
			} catch (SQLException e) {
				Log.e(TAG, "Get user DAO failed.");
				e.printStackTrace();
			}
		}
		return syncHistoryDao;
	}

	/**
	 * Close the database connections and clear any cached DAOs.
	 */
	@Override
	public void close() {
		super.close();
		userDao = null;
		findDao = null;
	}

}
