/*
 * File: PositDbHelper.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Search and Identification Tool.
 *
 * POSIT is free software; you can redistribute it and/or modify
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

package org.hfoss.posit.android.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.Utils;


import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * The class is the interface with the Database. It controls all Db access 
 *  and directly handles all Db queries.
 */
public class PositDbHelper extends SQLiteOpenHelper {
	/*
	 * Add new tables here.
	 */
    private static final boolean DBG = true;
	private static final String DBName ="posit";
	public static final int DBVersion = 2;
	private static final String TAG = "PositDbHelper";
	
	private static PositDbHelper sInstance = null;

	/**
	 *  The primary table
	 */
	public static final String FINDS_TABLE = "finds";
	public static final String FINDS_ID = "_id";
	public static final String FINDS_PROJECT_ID = "project_id";
	public static final String FINDS_NAME = "name";
	public static final String FINDS_GUID = "guid";    // Globally unique ID
	public static final String FINDS_DESCRIPTION = "description";
	public static final String FINDS_LATITUDE = "latitude";
	public static final String FINDS_LONGITUDE = "longitude";
	public static final String FINDS_TIME = "timestamp";
	public static final String FINDS_MODIFY_TIME = "modify_time";
	public static final String FINDS_SYNCED = "synced";
	public static final String FINDS_REVISION = "revision";
	public static final String FINDS_IS_ADHOC = "is_adhoc";
	public static final String FINDS_ACTION = "action";
	public static final String FINDS_DELETED = "deleted";
	public static final int FIND_IS_SYNCED = 1;
	public static final int FIND_NOT_SYNCED = 0;

	public static final int DELETE_FIND = 1;
	public static final int UNDELETE_FIND = 0;
	public static final String WHERE_NOT_DELETED = " " + FINDS_DELETED + " != " + DELETE_FIND + " ";
	public static final String DATETIME_NOW = "`datetime('now')`";


	/**
	 * Table and Fields for the photos table
	 */
	public static final String PHOTOS_TABLE = "photos";
	public static final String PHOTOS_ID = "_id";
	public static final String PHOTOS_IMAGE_URI = "image_uri";
	public static final String PHOTOS_THUMBNAIL_URI = "thumbnail_uri";
	public static final String PHOTOS_IDENTIFIER = "identifier";
	public static final String PHOTOS_FIND_ID = "find_id";
	public static final String PHOTOS_PROJECT_ID = "project_id";
	public static final String PHOTOS_MIME_TYPE = "mime_type";
	public static final String PHOTOS_DATA_FULL = "data_full";
	public static final String PHOTOS_DATA_THUMBNAIL = "data_thumbnail";



	public static final String SYNC_HISTORY_TABLE = "sync_history";
	public static final String FINDS_HISTORY_TABLE = "finds_history";
	public static final String SYNC_COLUMN_SERVER = "server";
	public static final String SYNC_ID = "_id";
	public static final String HISTORY_ID = "_id" ;
	
	public static final String EXPEDITION_TABLE = "expeditions";
	public static final String EXPEDITION_ROW_ID = "_id";
	public static final String EXPEDITION_NUM = "expedition_number";
	public static final String EXPEDITION_PROJECT_ID = "project_id"; 
	public static final String EXPEDITION_POINTS = "expedition_points";
	public static final String EXPEDITION_SYNCED = "expedition_synced";
	public static final String EXPEDITION_REGISTERED = "expedition_registered";
	public static final int EXPEDITION_NOT_REGISTERED = 0;
	public static final int EXPEDITION_IS_REGISTERED = 1;


	
	public static final String EXPEDITION_GPS_POINTS_TABLE = "points";
	public static final String EXPEDITION = "expedition";
	public static final String EXPEDITION_GPS_POINT_ROW_ID = "_id";
	public static final String GPS_POINT_LATITUDE = "latitude";
	public static final String GPS_POINT_LONGITUDE = "longitude";
	public static final String GPS_POINT_ALTITUDE = "altitude";
	public static final String GPS_POINT_SWATH = "swath";
	public static final String GPS_TIME = "time";
	public static final String GPS_SYNCED = "synced";
	
	// The following two arrays go together to form a <DB value, UI View> pair
	// except for the first DB value, which is just a filler.
	//	 GUID commented out so that in the list of finds the ID is no longer displayed
	//	 in an attempt to deal with the length of the new UUIDs
	//   -->UPDATED UUID is now truncated in display 
	public static final String[] list_row_data = { 
		FINDS_ID,
		FINDS_GUID,  
		FINDS_NAME,
		FINDS_DESCRIPTION,
		FINDS_LATITUDE,
		FINDS_LONGITUDE,
		FINDS_SYNCED, //,
		FINDS_GUID,  // Bogus but you need some field in the table to go with Thumbnail
		FINDS_GUID   //  Bogus, but SimpleCursorAdapter needs it
	};

	public static final int[] list_row_views = {
		R.id.row_id,		    
		R.id.idNumberText,
		R.id.name_id, 
		R.id.description_id,
		R.id.latitude_id,
		R.id.longitude_id,
		R.id.status, //,
		R.id.num_photos,
		R.id.find_image     // Thumbnail in ListPhotoFindsActivity
	};
	
	/*
	 * Array of column names used by TrackerListActivity to display a list of Expeditions.
	 */
	public static final String[] track_data = {
		EXPEDITION_NUM, 
		EXPEDITION_PROJECT_ID 
	};
	

	/*
	 * Finds table creation sql statement. 
	 */
	private static final String CREATE_FINDS_TABLE = "CREATE TABLE IF NOT EXISTS "
		+ FINDS_TABLE  
		+ " (" + FINDS_ID + " integer primary key autoincrement, "
		+ FINDS_GUID + " text, "        /* Globally unique Id = barcodes*/
		+ FINDS_PROJECT_ID + " integer DEFAULT 0, "
		+ FINDS_NAME + " text, "
		+ FINDS_DESCRIPTION + " text, "
		+ FINDS_LATITUDE + " double DEFAULT 0, "
		+ FINDS_LONGITUDE + " double DEFAULT 0, "
		+ FINDS_TIME + " timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," //text, "
		+ FINDS_MODIFY_TIME + " timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," // text, "
		+ FINDS_SYNCED + " integer default 0, "
		+ FINDS_REVISION + " integer default 1, "
		+ FINDS_DELETED + " integer default 0, "
		+ FINDS_IS_ADHOC + " integer default 0"
		+ ");";
	
	
	/*
	 * Creates the Photos table.
	 */
	private static final String CREATE_IMAGES_TABLE = "CREATE TABLE IF NOT EXISTS "
		+ PHOTOS_TABLE  
		+ " (" + PHOTOS_ID + " integer primary key autoincrement, "  // dB Key
		+ PHOTOS_FIND_ID + " integer DEFAULT 0, "      // Find Key
		+ FINDS_GUID + " text, "
		+ PHOTOS_PROJECT_ID + " integer DEFAULT 0, "
		+ PHOTOS_IDENTIFIER + " integer DEFAULT 0, "  // ??
		+ PHOTOS_IMAGE_URI + " text, "      // The image's URI
		+ PHOTOS_THUMBNAIL_URI + " text, "      // The thumbnail's URI
		+ PHOTOS_MIME_TYPE + " text DEFAULT 'image/jpeg', "
		+ FINDS_TIME + " timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP" //text, "
		+ ");" ;

	/*
	 * Keeps track of create, update, and delete actions on Finds.
	 */
	private static final String CREATE_FINDS_HISTORY_TABLE = 
		"CREATE TABLE IF NOT EXISTS " 
		+ FINDS_HISTORY_TABLE + "("
		+ HISTORY_ID + " integer primary key autoincrement,"
		+ FINDS_TIME + " timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,"
//		+ FINDS_PROJECT_ID + " integer DEFAULT 0,"
		+ FINDS_GUID + " varchar(50) NOT NULL,"
		+ FINDS_ACTION + " varchar(20) NOT NULL"
		+ ")";
		
	/*
	 * Keeps track of sync actions between client (phone) and server
	 */
	private static final String CREATE_SYNC_HISTORY_TABLE = 
		"CREATE TABLE IF NOT EXISTS " 
		+ SYNC_HISTORY_TABLE + "("
		+ SYNC_ID + " integer primary key autoincrement, "
		+ FINDS_TIME + " timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, "
		+ SYNC_COLUMN_SERVER + " varchar(50) NOT NULL, "
		+ FINDS_PROJECT_ID + " integer DEFAULT -1"
		+ ")";

	/*
	 * Keeps track of sync actions between client (phone) and server
	 */
	private static final String INITIALIZE_SYNC_HISTORY_TABLE = 
		"INSERT INTO " + SYNC_HISTORY_TABLE + "(" 
		+ FINDS_TIME + "," + SYNC_COLUMN_SERVER + ")" 
		+ " VALUES (datetime('now'),'noserver')";

	/*
	 * Keeps track of sync actions between client (phone) and serve
	 */
	private static final String TIMESTAMP_FIND_UPDATE = 
		"UPDATE " + FINDS_TABLE + " SET " 
		+ FINDS_MODIFY_TIME + " = " 
		+ " datetime('now') ";
	
	/*
	 * The create statement for the Expedition table.
	 */
	private static final String CREATE_EXPEDITION_TABLE =
		"CREATE TABLE IF NOT EXISTS " 
		+ EXPEDITION_TABLE + "("
		+ EXPEDITION_ROW_ID + " integer primary key autoincrement, "
		+ EXPEDITION_NUM + " integer DEFAULT 0, "
		+ EXPEDITION_PROJECT_ID + " integer DEFAULT 0, "
		+ EXPEDITION_POINTS + " integer DEFAULT 0, "
		+ EXPEDITION_SYNCED + " integer DEFAULT 0, "
		+ EXPEDITION_REGISTERED + " integer DEFAULT 0 "
		+ ")";
	
	/*
	 *  The create statement for the GPS points table. Note that we 
	 *  store the lat,long as Strings.  Had problems with precision 
	 *  between SQLite (on the phone) and MySQL (on the server) when 
	 *  storing them as doubles.  Might make sense to store them as
	 *  integer (* 1E6)?
	 */
	private static final String CREATE_EXPEDITION_GPS_POINTS_TABLE =
		"CREATE TABLE IF NOT EXISTS " 
		+ EXPEDITION_GPS_POINTS_TABLE + "("
		+ EXPEDITION + " integer DEFAULT 0, "
		+ EXPEDITION_GPS_POINT_ROW_ID + " integer primary key autoincrement, "
//	    + GPS_POINT_LATITUDE + " double DEFAULT 0, "
//	    + GPS_POINT_LONGITUDE + " double DEFAULT 0, "
	    + GPS_POINT_LATITUDE + " varchar(30) DEFAULT 0, "
	    + GPS_POINT_LONGITUDE + " varchar(30) DEFAULT 0, "	    
	    + GPS_POINT_ALTITUDE + " double DEFAULT 0, "
	    + GPS_POINT_SWATH + " integer DEFAULT 0, "
	    + GPS_TIME + " long DEFAULT 0, " 
	    + GPS_SYNCED + " integer DEFAULT 0 "
	    +")";
	
	private Context mContext;   // The Activity
	private SQLiteDatabase mDb;  // Pointer to the DB	
	
	public static void initInstance(Context context){
		assert(sInstance == null);
		sInstance = new PositDbHelper(context);
	}
	
	public static PositDbHelper getInstance(){
		assert(sInstance != null);
		return sInstance;
	}
  
	public PositDbHelper(Context context) {
		super(context, DBName, null, DBVersion);
//		if (DBG) Log.i(TAG, "Constructor");
		mDb = getWritableDatabase();
		onCreate(mDb);
		mDb = getWritableDatabase();
//		onUpgrade(mDb, 2, 3);
		this.mContext= context;
		mDb.close();
	}
	

	/* (non-Javadoc)
	 * @see android.database.sqlite.SQLiteOpenHelper#close()
	 */
	@Override
	public synchronized void close() {
		// TODO Auto-generated method stub
		super.close();
		mDb.close();
	}


 
	/**
	 * This method is called only when the DB is first created.
	 */
	@Override
	public void onCreate(SQLiteDatabase db) throws SQLException {
//		if (DBG) Log.i(TAG, "OnCreate");
		db.execSQL(CREATE_FINDS_TABLE);
//		if (DBG) Log.i(TAG, "Created " + FINDS_TABLE);
		db.execSQL(CREATE_IMAGES_TABLE);
//		if (DBG) Log.i(TAG, "Created " + PHOTOS_TABLE);
		db.execSQL(CREATE_FINDS_HISTORY_TABLE);
//		if (DBG) Log.i(TAG, "Created " + FINDS_HISTORY_TABLE);
		db.execSQL(CREATE_SYNC_HISTORY_TABLE);
//		if (DBG) Log.i(TAG, "Created " + SYNC_HISTORY_TABLE);
//  		db.execSQL("DROP TABLE IF EXISTS " + EXPEDITION_TABLE);
		db.execSQL(CREATE_EXPEDITION_TABLE);
//		if (DBG) Log.i(TAG, "Created " +EXPEDITION_TABLE);
//		db.execSQL("DROP TABLE IF EXISTS " + EXPEDITION_GPS_POINTS_TABLE);
		db.execSQL(CREATE_EXPEDITION_GPS_POINTS_TABLE);
//		if (DBG) Log.i(TAG, "Created " + EXPEDITION_GPS_POINTS_TABLE);
		
		// IF the column sync.project_id does not exist, add it
		try
		{
			db.rawQuery("SELECT " + FINDS_PROJECT_ID + " FROM " + SYNC_HISTORY_TABLE, null);
		}catch(SQLException e){
			Log.w(TAG, "ALTERING TABLE");
			db.execSQL("ALTER TABLE " + SYNC_HISTORY_TABLE + " ADD " + FINDS_PROJECT_ID + " integer DEFAULT -1");
		}
	}

	/**
	 * This method is called when the DB needs to be upgraded -- not
	 *   sure when that is??
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (DBG) Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
				+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + FINDS_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + PHOTOS_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + FINDS_HISTORY_TABLE );
		db.execSQL("DROP TABLE IF EXISTS " + SYNC_HISTORY_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + EXPEDITION_GPS_POINTS_TABLE);
		onCreate(db);	
		db.execSQL(INITIALIZE_SYNC_HISTORY_TABLE);
	}
	
	/**
	 * Queries the sync_find table and returns the time of the last sync
	 *  operation.
	 * @param projectId the project id to check last sync
	 * @return
	 */
	public String getTimeOfLastSync(long projectId) {
		mDb = getReadableDatabase();
		Cursor c = mDb.rawQuery("SELECT " + FINDS_TIME + 
				" FROM " + SYNC_HISTORY_TABLE + 
				" WHERE " + FINDS_PROJECT_ID + " = " + projectId +
				" ORDER BY " +  FINDS_TIME +  " DESC ", null);
		String maxtime = "No time";
		if (c.moveToFirst()) 
			maxtime = c.getString(0);  // column 0
		//Log.i(TAG, "projectId = " + projectId);
		if (DBG) Log.i(TAG, "Last sync time = " + maxtime);
		c.close();
		return maxtime;
	}

	/**
	 * Returns a list of the IDs of all Finds that have been created,
	 * updated or deleted since the last sync with the server.  Each
	 * change to a Find is recorded in find_history.
	 * 
	 * Policy: We don't send the server those finds that were created
	 *  and then deleted since the last sync.  Finds are not removed
	 *  from the Db, but just marked for deletion.
	 * @param projectId the project id
	 * @return a comma, delimited list of Find guIds
	 */
	public String getDeltaFindsIds(long projectId){
		String maxtime = getTimeOfLastSync(projectId);
		Log.i(TAG, "Last Sync = " + maxtime);
		
		mDb = getReadableDatabase();
		String[] args = new String[1];
		args[0] = maxtime;
		
		// Note this query will select finds that were created and then deleted since last sync
		// It is necessary to remove the created/updated if there's a deletion.


		Cursor c = mDb.rawQuery("SELECT DISTINCT " + FINDS_HISTORY_TABLE+"."+FINDS_GUID + "," + FINDS_HISTORY_TABLE+"."+FINDS_ACTION 
				+ " FROM " + FINDS_HISTORY_TABLE+","+FINDS_TABLE
				+ " WHERE " + FINDS_TABLE+"."+FINDS_PROJECT_ID + " = "+projectId+ " AND " + FINDS_HISTORY_TABLE+"."+FINDS_ACTION + "= 'delete' AND " + FINDS_HISTORY_TABLE+"."+FINDS_TIME + " > ? " , args);

		
		String result = "";
		c.moveToFirst();
		for (int k = 0; k < c.getCount(); k++) {
			result += c.getString(0) + ":" + c.getString(1) + ",";
			c.moveToNext();
		}
		c.close();
		c = mDb.rawQuery("SELECT DISTINCT " + FINDS_HISTORY_TABLE+"."+FINDS_GUID + "," + FINDS_HISTORY_TABLE+"."+FINDS_ACTION 
				+ " FROM " + FINDS_HISTORY_TABLE+","+FINDS_TABLE
				+ " WHERE " + FINDS_TABLE+"."+FINDS_PROJECT_ID + " = "+projectId+ " AND " + FINDS_HISTORY_TABLE+"."+FINDS_ACTION + "!= 'delete' AND " + FINDS_HISTORY_TABLE+"."+FINDS_TIME + " > ? " , args);
		c.moveToFirst();
		for (int k = 0; k < c.getCount(); k++) {
			String id = c.getString(0);
			String act = c.getString(1);
			if (result.indexOf(id) == -1) { // the result does not contain a deletion for this record
				result += id + ":" + act + ",";
			}
			c.moveToNext();
		}
		c.close();
		mDb.close();
		if (DBG) Log.i(TAG, "Find deltas = " + result);
		return result;
	}
	
	/**
	 * Records the time stamp for the current sync operation. 
	 * @param values a map of key/value pairs, in this case just 
	 *  the server/url
	 */
	public boolean recordSync(ContentValues values) {
		mDb = getWritableDatabase();
		long result = mDb.insert(SYNC_HISTORY_TABLE, null, values);
		Log.i(TAG, "recordSync result = " + result);
		mDb.close();
		return result != -1;
	}
	
	/**
	 * Invoked to records an entry in the Finds history log, each time a 
	 * Find is created, updated or deleted
	 * @param guid the Find's globally unique Id
	 * @param action the action taken--update, delete, create
	 * @return true if the insertion was successful
	 */
	public boolean logFindHistory (String guId, String action) {
		mDb = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(FINDS_GUID, guId);
		values.put(FINDS_ACTION, action);   
		long result = -1;
		result = mDb.insert(FINDS_HISTORY_TABLE, null, values);
		Log.i(TAG, "logFindHistory result = " + result + " for guid=" + guId + " " + action);
		mDb.close();
		return result != -1;
	}


	/**
	 * Looks up a Find by its guId
	 * @param guId the find's globally unique Id
	 * @return
	 */
	public boolean containsFind(String guId) {
		mDb = getWritableDatabase();  // Either create or open the DB.
		Cursor c = mDb.rawQuery("Select * from " + FINDS_TABLE + 
				" where " + FINDS_GUID + " = \"" + guId +"\" AND " + WHERE_NOT_DELETED, null);
		boolean result = c.getCount() > 0;
		c.close();
		mDb.close();
		return result;
	}
	
	
	/**
	 * Adds a new Find to the Db taking its column values from a ContentValues.
	 * If the insertion is successful, a timestamped record of the insertion is
	 * made in the find_history table. 
	 * 
	 * The GuID is a globally unique identifier that can be used on both the 
	 * client and the server.
	 * @param values -- an array of attribute/value pairs
	 * @param images -- a list of images, may be null
	 * @return true if both the insertion and the timestamping are successful
	 */
	public boolean addNewFind(ContentValues values, List<ContentValues> find_data_entries) {
		mDb = getWritableDatabase();  // Either create the DB or open it.
		long rowId = mDb.insert(FINDS_TABLE, null, values);
		boolean result = false;
		// If successful, timestamp this action in FindsHistory table
		
		
		if (rowId != -1) {
			String guId = getGuIdFromRowId(rowId);

			result = logFindHistory(guId, "create");    // Timestamp the insertion
			if (find_data_entries != null){
				return addFindData(rowId, guId, find_data_entries);
			} else 
				return result;
		}
		mDb.close();
		return result;
	}
	
	/**
	 * This method is called from a Find object to add its data to DB.
	 * @param values contains the key/value pairs for each Db column,
	 * @return the rowId of the new insert or -1 in case of error
	 */
	public long addNewFind(ContentValues values) {
		mDb = getWritableDatabase();  // Either create the DB or open it.
		long rowId = mDb.insert(FINDS_TABLE, null, values);

		// If successful, timestamp this action in Finds history table
		if (rowId != -1) {
			String guId = getGuIdFromRowId(rowId);

			if (logFindHistory(guId, "create")) // Timestamp the insertion
				return rowId;  
			else
				return -1;
		}
		return rowId;
	}
	


	
	/**
	 * Updates a Find using it primary key, id. This should increment its
	 *  revision number.
	 * @param rowId  The Find's primary key.
	 * @param args   The key/value pairs for each column of the table.
	 * @return
	 */
	public boolean updateFind(long id, ContentValues args) {
		boolean success = false;
		if (args == null)
			return false;
		mDb = getWritableDatabase();  // Either create or open the DB.
//		mDb.beginTransaction();
		try {
			if (DBG) Log.i(TAG, "updateFind id = " + id);
			
			// Select the revision number and increment it
			Cursor c = mDb.rawQuery("SELECT " + FINDS_REVISION + " FROM " + FINDS_TABLE
					+ " WHERE " + FINDS_ID + "=" + id, null);
			c.moveToFirst();
			int revision = c.getInt(c.getColumnIndex(FINDS_REVISION));
			++revision;
			c.close();

			args.put(FINDS_REVISION, revision);
			
			// Update the Finds table with the new data
			success = mDb.update(FINDS_TABLE, args, FINDS_ID + "=" + id, null) > 0;
			if (DBG) Log.i(TAG,"updateFind result = "+success);
			
			// Timestamp the time_modify field in the Find table (by default)
			mDb.execSQL(TIMESTAMP_FIND_UPDATE + " WHERE " + FINDS_ID + " = " + id); 

			// Timestamp the update action in the find_history table
			String guId = getGuIdFromRowId(id);
			success = logFindHistory(guId, "update");    
		} catch (Exception e){
			Log.i("Error in update Find transaction", e.toString());
		} finally {
//			mDb.endTransaction();
			mDb.close();
		}
		return success;
	}
	
	/**
	 * Updates a Find using its guId, primarily for Finds received from the
	 *  server.
	 * @param guId  the Find's globally unique Id
	 * @param args   The key/value pairs for each column of the table.
	 * @return
	 */
	public boolean updateFind(String guId, ContentValues args, List<ContentValues> find_data_entries ) {
		mDb = getWritableDatabase();  // Either create or open the DB.
		boolean success = false;
		if (args != null) {
			if (DBG) Log.i(TAG, "updateFind guId = "+guId);

			// Select the revision number and increment it
			Cursor c = mDb.rawQuery("SELECT " + FINDS_REVISION + " FROM " + FINDS_TABLE
					+ " WHERE " + FINDS_GUID + "='" + guId + "'", null);
			c.moveToFirst();
			int revision = c.getInt(c.getColumnIndex(FINDS_REVISION));
			++revision;
			c.close();

			args.put(FINDS_REVISION, revision);
			
			
			// Update the Finds table with new data
			success = mDb.update(FINDS_TABLE, args, FINDS_GUID + "=\"" + guId + "\"", null) > 0;
			if (DBG) Log.i(TAG,"updateFind success = "  + success);

			// Timestamp the time_modify field in the Find table (by default)
			mDb.execSQL(TIMESTAMP_FIND_UPDATE 
  			+ " WHERE " + FINDS_GUID + " = '" + guId + "'");     
		}

		mDb.close();
		return success;
	}

	/**
	 * Called after a Find has been successfully synced with the server.
	 * @param id
	 * @return
	 */
	public boolean markFindSynced(long id) {
		boolean success = false;
		mDb = getWritableDatabase();  // Either create or open the DB.
//		mDb.beginTransaction();
		try {
			ContentValues args = new ContentValues();
			args.put(FINDS_SYNCED, FIND_IS_SYNCED);
			long res = mDb.update(FINDS_TABLE, args, FINDS_ID + "=" + id, null);
			success = res > 0;
//			if (success)
//				mDb.setTransactionSuccessful();
			if (DBG) Log.i(TAG, "markFindSynced, find= " + id + " res = " + res);
		} catch (Exception e) {
			Log.i("Error in transaction", e.toString());
		} finally {
//			mDb.endTransaction();
			mDb.close();
			Log.i(TAG, "fetchfindmap= " + fetchFindMapById(id).toString());
		}
		return success;
	}

	/**
	 * This method is called from a Find object, passing its ID. It marks the item
	 * 'deleted' in the Finds table. (Remember to modify all Select queries to include
	 * the "where deleted != '1'" clause.)
	 *   
	 *   We first get the guId so we can log the deletion in the find_history table.
	 * @param mRowId
	 * @return
	 */
	public boolean deleteFind(long id) {
		ContentValues content = new ContentValues();
		content.put(FINDS_DELETED, DELETE_FIND);
		boolean success = updateFind (id, content);  // Just use updateFind()
		String guId = getGuIdFromRowId(id);

		// If successful, timestamp this action in FindsHistory table
		if (success) {   
			Log.i(TAG, "delete find update log, guid= " + guId);
			success = logFindHistory(guId, "delete");
		}
		
//		if (success) 
//			success = deletePhotosById(id);
//		if (success) {
//			Log.i(TAG, "deleteFind " + id + " deleted photos");
//		}
		
		return success;
	}
	
	/**
	 * Deletes find data from memory for a given find that is being deleted.
	 * @param id the Find's _id 
	 * @return
	 */
	// TODO: references to photo columns must be renamed to reflect fact that they now store generic binary data
	public boolean deleteFindDataEntriesById(long id) {
		Log.i(TAG, "Delete find data for id = " + id);
		boolean success = false;
		int rows = 0;
		mDb = getWritableDatabase();

		Cursor c = mDb.query(PHOTOS_TABLE, 
				new String[]{ PHOTOS_IMAGE_URI, PHOTOS_THUMBNAIL_URI }, 
				PHOTOS_FIND_ID + "=" + id, 
				null,null,null,null);
		if (c.getCount() == 0) {
			Log.i(TAG, "deleteFindDataEntriesById, no find data to delete");
			return true;
		}
		
		c.moveToFirst();
		while (!c.isAfterLast()) {
			Uri imgUri = Uri.parse(c.getString(c.getColumnIndexOrThrow(PHOTOS_IMAGE_URI)));		
//			Uri thumbUri = Uri.parse(c.getString(c.getColumnIndexOrThrow(PHOTOS_THUMBNAIL_URI)));
			
//			 We would like to delete the thumbnails too, but it's a problem with android
//			 @see http://code.google.com/p/android/issues/detail?id=2724
			// Try to delete the image and its thumbnail
			try {
				rows = mContext.getContentResolver().delete(imgUri, null, null);
				if (rows == 0) 
					return false;
//				rows = mContext.getContentResolver().delete(thumbUri, null, null);
			} catch (Exception e) {
				Log.i(TAG, "deleteFindDataEntriesById exception " +  e);
				e.printStackTrace();
				return false;
			}
			c.moveToNext();
		}

		// Now delete the entries in the photos table
		success = mDb.delete(PHOTOS_TABLE, PHOTOS_FIND_ID + "=" + id, null) > 0;
		c.close();
		mDb.close();
		Log.i(TAG, "deleteFindDataEntriesById " + id + " deleting find data, success = " + success);
		return success;  
	}
	
	/**
	 * This method is called from ListActivity to delete all the finds currently
	 *  in the DB. It marks them all "deleted" and deletes their find data entries.
	 * @return
	 */
	public boolean deleteAllFinds() {
		mDb = getWritableDatabase();
		ContentValues content = new ContentValues();
		content.put(FINDS_DELETED, DELETE_FIND);

		boolean success = mDb.update(FINDS_TABLE, content, null, null) > 0;
		mDb.close();
		if (success)
			Log.i(TAG, "deleteAllFinds marked finds deleted ... deleting find data");
			return deleteAllFindData();
	}
	
	/** 
	 * Deletes all find data -- called from delete all finds.
	 * @return
	 */
	// TODO: references to photo columns must be renamed to reflect fact that they now store generic binary data
	private boolean deleteAllFindData() {
		mDb = getWritableDatabase();
		Cursor c = mDb.query(PHOTOS_TABLE, null, null, null,null,null,null);
		while(c.moveToNext()) {
			Uri find_data_uri = Uri.parse(c.getString(c.getColumnIndexOrThrow(PHOTOS_IMAGE_URI)));
			try {
				mContext.getContentResolver().delete(find_data_uri,null,null);
			}
			catch(Exception e) {
				Log.i(TAG,"Could not delete all find data entries");
			}
		}
		c.close();
		boolean result = mDb.delete(PHOTOS_TABLE,null,null) == 0; //deletes all rows
		mDb.close();
		return result;
	}


	/** 
	 * SUSPECT:  PositDbHelper should not return a Cursor -- causes memory leaks: 
	 * Returns a Cursor with rows for all Finds of a given project.
	 * @return
	 */
	public Cursor fetchFindsByProjectId(int project_id) {
		return fetchFindsByProjectId(project_id, null);
	}
	
	public Cursor fetchFindsByProjectId(int project_id, String order_by) {
		mDb = getReadableDatabase(); // Either open or create the DB.
		Cursor c = mDb.query(FINDS_TABLE,null, 
				WHERE_NOT_DELETED + " AND " + FINDS_PROJECT_ID +"="+project_id, null, null, null, order_by);
		Log.i(TAG,"fetchFindsByProjectId " + FINDS_PROJECT_ID + "=" + project_id + " count=" + c.getCount());

		mDb.close();
		return c;
	}
	
	/**
	 * Returns key/value pairs for selected columns with row selected by guId 
	 * @param guId the Find's globally unique Id
	 * @param columns an array of column names, can be left null
	 * @return
	 */
	public ContentValues fetchFindDataByGuId(String guId, String[] columns) {
		mDb = getReadableDatabase();  // Either open or create the DB    	
		String[] selectionArgs = null;
		String groupBy = null, having = null, orderBy = null;
		Cursor c = mDb.query(FINDS_TABLE, columns, 
				WHERE_NOT_DELETED + " AND " + FINDS_GUID+"="+guId, selectionArgs, groupBy, having, orderBy);
		c.moveToFirst();
		ContentValues values = null;
		if (c.getCount() != 0)
			values = this.getContentValuesFromRow(c);
		c.close();
		mDb.close();
		return values;
	}
	
	
	/**
	 * Returns selected columns for a find by id.
	 * @param id the Find's id
	 * @param columns an array of column names, can be left null
	 * @return
	 */
	public ContentValues fetchFindDataById(long id, String[] columns) {
		mDb = getReadableDatabase();  // Either open or create the DB    	
		//String[] columns = mContext.getResources().getStringArray(R.array.TABLE_FINDS_core_fields);

		String[] selectionArgs = null;
		String groupBy = null, having = null, orderBy = null;
		Cursor c = mDb.query(FINDS_TABLE, columns, 
				WHERE_NOT_DELETED + " AND " + FINDS_ID+"="+id, selectionArgs, groupBy, having, orderBy);
		c.moveToFirst();
		ContentValues values = null;
		if (c.getCount() != 0)
			values = this.getContentValuesFromRow(c);
		c.close();
		mDb.close();
		return values;
	}


	/**
	 * Returns a <string,string> hash map of all columns for a given Find
	 * @param id the Id primary key
	 * @return
	 */
	public HashMap<String,String> fetchFindMapById(long id) {
		mDb = getReadableDatabase();  // Either open or create the DB    	
		String[] columns = null;
		String[] selectionArgs = null;
		String groupBy = null, having = null, orderBy = null;
		Cursor cursor = mDb.query(FINDS_TABLE, columns, 
				WHERE_NOT_DELETED + " AND " + FINDS_ID + "=" + id, 
				selectionArgs, groupBy, having, orderBy);
		cursor.moveToFirst();
		HashMap<String,String> findsMap = new HashMap<String, String>();
		if (cursor.getCount() != 0) {
			findsMap = Utils.getMapFromCursor(cursor);
		}
		cursor.close();
		mDb.close();
		return findsMap;
	}
	
	/**
	 * Returns a <string,string> hash map of all columns for a given Find
	 * @param guid the Find's globally unique Id
	 * @return
	 */
	public HashMap<String,String> fetchFindMapByGuid(String guid) {
		mDb = getReadableDatabase();  // Either open or create the DB    	
		//String[] columns = mContext.getResources().getStringArray(R.array.TABLE_FINDS_core_fields);
		String[] columns = null; // Should get everything
		String[] selectionArgs = null;   
		String groupBy = null, having = null, orderBy = null;
		Cursor cursor = mDb.query(FINDS_TABLE, columns, 
				WHERE_NOT_DELETED + " AND " + FINDS_GUID + "=\"" + guid + "\"",   // Need quotes here
				selectionArgs, groupBy, having, orderBy);
		cursor.moveToFirst();
		HashMap<String,String> findsMap = new HashMap<String, String>();
		if (cursor.getCount() != 0) {
			findsMap = Utils.getMapFromCursor(cursor);
		} else {
			Log.i(TAG,"fetchFindMap ERROR on fetch for Guid = " + guid);
		}	
		if (DBG) Log.i(TAG,"fetchFindMap map=" + findsMap.toString());
		cursor.close();
		mDb.close();
		return findsMap;
	}
	

	/**
	 * Utility method to retrieve a Find's rowId from it's guId
	 * @param guId the globally unique ID
	 * @return the _id for this Find in the Db
	 */
	public long getRowIdFromGuId(String guId) {
		mDb = getReadableDatabase();
		long id = 0;
		Cursor c = mDb.query(FINDS_TABLE, null, FINDS_GUID + "=\"" + guId+"\"", null, null, null, null);
		if ( c.getCount() != 0) {
			c.moveToFirst();
			id = (c.getLong(c.getColumnIndexOrThrow(FINDS_ID)));
		}
		c.close();
		mDb.close();
		return id;
	}
	
	/**
	 * Utility method to retrieve a find's guId from it's rowId
	 * @param rowId, the _id for this Find
	 * @return the guId, globally unique Id
	 */
	public String getGuIdFromRowId(long rowId) {
		mDb = getReadableDatabase();
		String guId = "";
		Cursor c = mDb.query(FINDS_TABLE, null, FINDS_ID + "=" + rowId, null, null, null, null);
		if ( c.getCount() != 0) {
			c.moveToFirst();
			guId =  (c.getString(c.getColumnIndexOrThrow(FINDS_GUID)));
		}
		c.close();
		mDb.close();
		return guId;
	}
	
	/**
	 * This helper method is passed a cursor, which points to a row of the DB.
	 *  It extracts the names of the columns and the values in the columns,
	 *  puts them into a ContentValues hash table, and returns the table.
	 * @param cursor is an object that manipulates DB tables. 
	 * @return
	 */
	private ContentValues getContentValuesFromRow(Cursor c) {
		ContentValues values = new ContentValues();
		c.moveToFirst();
		for (String column : c.getColumnNames()) {
			
			if(Utils.debug)
				if (DBG) Log.i(TAG, "Column " + column + " = " + 
					c.getString(c.getColumnIndexOrThrow(column)));
			values.put(column, c.getString(c.getColumnIndexOrThrow(column)));
		}
//		c.close();  // Closed by calling method should be ok for a private method
		return values;
	}

	/**
	 * Repeatedly sets up the ContentValues for an insertion into the
	 *  photo table and invokes addNewFindDataEntry() to do the insertion.
	 * @param id  the Find's id
	 * @param find_data_entries a List of find data entries represented as key/value pairs
	 */
	// TODO: references to photo columns must be renamed to reflect fact that they now store generic binary data
	public boolean addFindData(long id, String guid, List<ContentValues> find_data_entries) {
		if (find_data_entries == null)
			return false;
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
		int projId = sp.getInt("PROJECT_ID", 0);

		Log.i(TAG, "addFindData #images = " + find_data_entries.size());
		ListIterator<ContentValues> it = find_data_entries.listIterator();
		long result = -1;
		while (it.hasNext()) {
			ContentValues data_entry_values = it.next();
			data_entry_values.put(FINDS_GUID, guid);
			if (!data_entry_values.containsKey(PositDbHelper.PHOTOS_FIND_ID))
				data_entry_values.put(PositDbHelper.PHOTOS_FIND_ID, id);
			if (!data_entry_values.containsKey(PositDbHelper.PHOTOS_IDENTIFIER))
				data_entry_values.put(PositDbHelper.PHOTOS_IDENTIFIER, new Random().nextInt(999999999));
			if (!data_entry_values.containsKey(PositDbHelper.FINDS_PROJECT_ID))
				data_entry_values.put(PositDbHelper.FINDS_PROJECT_ID, projId);
			result = addNewFindDataEntry(id, data_entry_values);
			if (result == -1)
				return false;
		}
		return result != -1;
	}
	
	
	/**
	 * Adds a find data entry to DB and timestamps the data record.
	 * @param values  contains the <column_name,value> pairs for each DB field.
	 * @return
	 */  
	// TODO: references to photo columns must be renamed to reflect fact that they now store generic binary data
	public long addNewFindDataEntry(String guid, ContentValues values) {
		mDb = getWritableDatabase(); // Either create or open DB
		if (!values.containsKey(PHOTOS_IDENTIFIER))
			values.put(PHOTOS_IDENTIFIER, new Random().nextInt(999999999));
		Log.i(TAG, "addNewFindDataEntry, values=" + values.toString());
		long result = mDb.insert(PHOTOS_TABLE, null, values);
		mDb.close();
		return result;
	}

	
	/**
	 * This method is called from a Find object to add a find data entry to DB.
	 * @param values  contains the <column_name,value> pairs for each DB field.
	 * @return
	 */  
	// TODO: references to photo columns must be renamed to reflect fact that they now store generic binary data
	public long addNewFindDataEntry(long id, ContentValues values) {
		mDb = getWritableDatabase(); // Either create or open DB
		if (!values.containsKey(PHOTOS_IDENTIFIER))
			values.put(PHOTOS_IDENTIFIER, new Random().nextInt(999999999));
		Log.i(TAG, "addNewFindDataEntry, values=" + values.toString());
		long result = mDb.insert(PHOTOS_TABLE, null, values);	
		mDb.close();
		return result;
	}

	/**
	 * Retrieves a find data from the database, based on a specific find and position.
	 * @param findId the find whose find data is to be retrieved
	 * @param position the specific position [row id, list position, etc] the find data entry is located at
	 * @return Uri to the find data
	 */
	public Uri getFindDataUriByPosition(long findId, int position) {
		mDb = getWritableDatabase();
		Cursor cursor = getFindDataEntriesCursor(findId);

		if (cursor.moveToPosition(position)) {
			String s = cursor.getString(cursor.getColumnIndex(PHOTOS_IMAGE_URI));
			mDb.close();
			cursor.close();
			if (s != null) {
				return Uri.parse(s);
			}
		} else {
			Log.e(TAG, "cursor could not move to position: " + position);
		}

		return null;
	}
	
	/**
	 * Counts the number of find data entries associated with a given Find.
	 * @param id the Find's id.
	 * @return
	 */
	public int getFindDataEntriesCount(long id) {
		Cursor c = getFindDataEntriesCursor(id);
		int count = c.getCount();
		c.close();
		mDb.close();
		return count;
	}
	
	/**
	 * A utility method to get the data entries for a given Find.
	 * 
	 * @param id the Find's id
	 * @return a Cursor with a row for each find data entry associated with the given Find.
	 */
	public Cursor getFindDataEntriesCursor(long id) {
		mDb = getReadableDatabase();
		if (DBG) Log.i(TAG, "id = " + id+"");
		String[] selectionArgs = null;
		String groupBy = null, having = null, orderBy = null;
		Cursor cursor = mDb.query(PHOTOS_TABLE, null, PHOTOS_FIND_ID + "=" + id, selectionArgs, groupBy, having, orderBy);
		// mDb.close();  // You cannot close the Db w/o invalidating the cursor?
		//	    mDb.close();
		return cursor;
	}

	public ContentValues getFindDataEntries(long id) {
		mDb = getReadableDatabase();
		Cursor cursor = getFindDataEntriesCursor(id);
		cursor.moveToFirst();
		ContentValues values = new ContentValues();
		//if(Utils.debug)
		if (DBG) 	Log.i(TAG, "Find data entries count = " + cursor.getCount() + " for _id = " + id);
		if (cursor.getCount() != 0)
			values = getContentValuesFromRow(cursor);
		cursor.close();
		mDb.close();
		return values;
	}

	/**
	 * Adds find data Uris to existing ContentValues
	 * @param id  is the Key of the Find whose data entries are sought
	 * @param values is an existing ContentValues with Find's <key, value> pairs
	 */
	public void getFindDataEntries(long id, ContentValues values) {
		Cursor cursor = getFindDataEntriesCursor(id);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			for (String column : cursor.getColumnNames()) {
				String entry = cursor.getString(cursor.getColumnIndexOrThrow(column));
				if (entry != null)
					values.put(column, cursor.getString(cursor.getColumnIndexOrThrow(column)));
			}

		}
		cursor.close();
		mDb.close();
	}
	
	/**
	 * Creates a list of find data for a given find since last update where
	 * the date is provided in the timestamp.  The find data is stored in ContentValues.
	 * So each list element is a ContentsValues containing info about find data.
	 * @param id  is the Key of the Find whose data entries are sought
	 * @return an ArrayList of data for each data entry associated with a Find
	 */
	public ArrayList<ContentValues> getFindDataEntriesListSinceUpdate(long id, long projectId) {
		String whereClause = PHOTOS_FIND_ID + "=" + id 
			+ " AND " + FINDS_TIME + " > ? ";
		String[] args = new String[1];
		args[0] = getTimeOfLastSync(projectId);
		Log.i(TAG, "getFindDataEntriesListSinceUpdate, whereClause= " + whereClause + " ?=" + args[0]);
		return getFindDataEntriesListHelper(id, whereClause, args);
	}

	
	/**
	 * Creates a list of find data.  The find data is stored in ContentValues.
	 * So each list element is a ContentsValues containing info about find data.
	 * @param id  is the Key of the Find whose find data entries are sought
	 * @return an ArrayList of data for each data entry associated with a Find
	 */
	public ArrayList<ContentValues> getFindDataEntriesList(long id) {
		String whereClause = PHOTOS_FIND_ID + "=" + id;
		return getFindDataEntriesListHelper(id, whereClause, null);
		
	}

	/**
	 * Helper method that constructs the find data entries list based on various where clauses.
	 * @param id
	 * @param whereClause
	 * @return
	 */
	private ArrayList<ContentValues> getFindDataEntriesListHelper(long id, String whereClause, String[] args) {
		ArrayList<ContentValues> list = new ArrayList<ContentValues>();
		mDb = getReadableDatabase();
		Cursor c = null;
//		mDb.beginTransaction();
		try {
			if (DBG) Log.i(TAG, "id = " + id+"");
			String groupBy = null, having = null, orderBy = null;
			c = mDb.query(PHOTOS_TABLE, null, whereClause,  args, groupBy, having, orderBy);

			// 	    Log.i(TAG, "getImagesList, cursor count= " + c.getCount());
			c.moveToFirst();
			while (c.isAfterLast() == false) {
				ContentValues values = new ContentValues();
				for (String column : c.getColumnNames()) {
					String entry = c.getString(c.getColumnIndexOrThrow(column));
					if (entry != null)
						values.put(column, c.getString(c.getColumnIndexOrThrow(column)));
				}
				c.moveToNext();
				list.add(values);
				Log.i(TAG, "getFindDataEntriesListHelper, values=" + values.toString());
			}
			Log.i(TAG, "getFindDataEntriesListHelper, list size= " + list.size());
		} catch (Exception e) {
			Log.e("Error in transaction", e.toString());
		} finally {
//			mDb.endTransaction();
			c.close();
			mDb.close();
		}
		return list;
	}


	/**
	 * This method is called from a Find object, passing its ID. It marks the item
	 * 'deleted' in the Finds table. (Remember to modify all Select queries to include
	 * the "where deleted != '1'" clause.)
	 *   
	 *   We first get the guId so we can log the deletion in the find_history table.
	 * @param mRowId
	 * @return
	 */
	public boolean deleteFind(String guid) {
		ContentValues content = new ContentValues();
		content.put(FINDS_DELETED, DELETE_FIND);
		long id = getRowIdFromGuId(guid); 
		boolean success = updateFind (id, content);  // Just use updateFind()
		

		// If successful, timestamp this action in FindsHistory table
		if (success) {   
			Log.i(TAG, "delete find update log, guid= \"" + guid+"\"");
			success = logFindHistory(guid, "delete");
		}
		if (success) 
			success = deleteFindDataEntriesById(id);
		if (success) {
			Log.i(TAG, "deleteFind " + id + " deleted find data entries");
		}
		
		return success;
	}
}