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

package org.hfoss.posit.android.plugin.acdivoca;


import java.util.List;

import org.hfoss.posit.android.R;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * The class is the interface with the Database. It controls all Db access 
 *  and directly handles all Db queries.
 */
public class AcdiVocaDbHelper extends SQLiteOpenHelper {
	/*
	 * Add new tables here.
	 */
    private static final boolean DBG = true;
	private static final String DBName ="posit";
	public static final int DBVersion = 2;
	private static final String TAG = "DbHelper";

	/**
	 *  The primary table
	 */
	public static final String FINDS_TABLE = "acdi_voca_finds";
	public static final String FINDS_ID = "_id";
	public static final String FINDS_PROJECT_ID = "project_id";
	public static final String FINDS_NAME = "name";
	
	public static final String FINDS_FIRSTNAME = "firstname";
	public static final String FINDS_LASTNAME = "lastname";
	
	public static final String FINDS_ADDRESS = "address";
	public static final String FINDS_DOB = "dob";
	public static final String FINDS_SEX = "sex";
	public static final String FINDS_AGE = "age";
	
	public static final String FINDS_COMMUNE_ID = "commune_id";
	public static final String FINDS_COMMUNE_SECTION_ID = "commune_section_id";
	public static final String FINDS_BENEFICIARY_CATEGORY_ID = "beneficiary_category_id";
	public static final String FINDS_HOUSEHOLD_SIZE = "household_size";
	public static final String FINDS_INFANT_CATEGORY ="infant_category";
	public static final String FINDS_MOTHER_CATEGORY = "mother_category";

	public static final String FINDS_GUID = "guid";    // Globally unique ID
	
	
	/** Commune table */
	
	public static final String COMMUNE_TABLE = "commune";
	public static final String COMMUNE_ID = "id";
	public static final String COMMUNE_NAME = "commune";
	public static final String COMMUNE_ABBR = "comm_abbrev";
	
	/** Commune section table */
	
	public static final String COMMUNE_SECTION_TABLE = "commune_section";
	public static final String COMMUNE_SECTION_ID = "id";
	public static final String COMMUNE_SECTION_NAME = "commune_section";
	public static final String COMMUNE_SECTION_ABBR = "comm_sect_abbrev";
	public static final String COMMUNE_SECTION_COMMUNE_ID = "commune_id";
	
	
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

//	public static final String SYNC_HISTORY_TABLE = "sync_history";
	public static final String FINDS_HISTORY_TABLE = "acdi_voca_finds_history";
//	public static final String SYNC_COLUMN_SERVER = "server";
//	public static final String SYNC_ID = "_id";
	public static final String HISTORY_ID = "_id" ;

	
	// The following two arrays go together to form a <DB value, UI View> pair
	// except for the first DB value, which is just a filler.
	//	 GUID commented out so that in the list of finds the ID is no longer displayed
	//	 in an attempt to deal with the length of the new UUIDs
	//   -->UPDATED UUID is now truncated in display 
	public static final String[] list_row_data = { 
		FINDS_ID,
		//FINDS_GUID,  
		FINDS_LASTNAME,
		FINDS_FIRSTNAME,
		FINDS_DOB,
		FINDS_SEX,
		FINDS_AGE,
		FINDS_HOUSEHOLD_SIZE,
		FINDS_MOTHER_CATEGORY,
		FINDS_INFANT_CATEGORY,
		COMMUNE_NAME,
		COMMUNE_SECTION_NAME
//		FINDS_DESCRIPTION,
//		FINDS_LATITUDE,
//		FINDS_LONGITUDE,
//		FINDS_SYNCED, //,
//		FINDS_GUID,  // Bogus but you need some field in the table to go with Thumbnail
//		FINDS_GUID   //  Bogus, but SimpleCursorAdapter needs it
	};

	public static final int[] list_row_views = {
		R.id.row_id,		    
////		R.id.idNumberText,
		R.id.lastname_field, 
		R.id.firstname_field,
		R.id.datepicker,
		R.id.femaleRadio,
		R.id.ageEdit,
		R.id.inhomeEdit,
		R.id.expectingRadio,
		R.id.malnourishedRadio,
		R.id.communeSpinner,
		R.id.commune_sectionSpinner
//		R.id.description_id,
//		R.id.latitude_id,
//		R.id.longitude_id,
//		R.id.status, //,
//		R.id.num_photos,
//		R.id.find_image     // Thumbnail in ListFindsActivity
	};
	

	/*
	 * Finds table creation sql statement. 
	 */
	private static final String CREATE_FINDS_TABLE = "CREATE TABLE IF NOT EXISTS "
		+ FINDS_TABLE  
		+ " (" + FINDS_ID + " integer primary key autoincrement, "
		+ FINDS_PROJECT_ID + " integer DEFAULT 0, "
		+ FINDS_NAME + " text, "
		+ FINDS_FIRSTNAME + " text, "
		+ FINDS_LASTNAME + " text, "
		+ FINDS_ADDRESS + " text, "
		+ FINDS_DOB + " date, "
		+ FINDS_SEX + " text, "
		+ FINDS_AGE + " text, "
		+ FINDS_HOUSEHOLD_SIZE + " text, "
		+ FINDS_MOTHER_CATEGORY + " text, "
		+ FINDS_INFANT_CATEGORY + " text, "
		+ COMMUNE_NAME + " text, "
		+ COMMUNE_ABBR + " text, "
		+ COMMUNE_SECTION_NAME + " text, "
		+ COMMUNE_SECTION_ABBR + " text "
//		+ FINDS_COMMUNE_ID + " references " + COMMUNE_TABLE + "(" + COMMUNE_ID + "), "
//		+ FINDS_COMMUNE_SECTION_ID + " references " + COMMUNE_SECTION_TABLE + "(" + COMMUNE_SECTION_ID + ")" 
		+ ");";
	
	private static final String CREATE_COMMUNE_TABLE = "CREATE TABLE IF NOT EXISTS "
		+ COMMUNE_TABLE + "(" + COMMUNE_ID + " integer primary key autoincrement, "
		+ COMMUNE_NAME + " text, "
		+ COMMUNE_ABBR + " text, "
		+ ")";
	
	private static final String CREATE_COMMUNE_SECTION_TABLE = "CREATE TABLE IF NOT EXISTS "
		+ COMMUNE_SECTION_TABLE + "(" + COMMUNE_SECTION_ID + " integer primary key autoincrement, "
		+ COMMUNE_SECTION_NAME + " text, "
		+ COMMUNE_SECTION_ABBR + " text, "
		+ COMMUNE_SECTION_COMMUNE_ID + " references " + COMMUNE_TABLE + "(" + COMMUNE_ID + ") "
		+ ")";
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
//		+ FINDS_ACTION + " varchar(20) NOT NULL"
		+ ")";
	/*
	 * Keeps track of sync actions between client (phone) and serve
	 */
	private static final String TIMESTAMP_FIND_UPDATE = 
		"UPDATE " + FINDS_TABLE + " SET " 
		+ FINDS_MODIFY_TIME + " = " 
		+ " datetime('now') ";
	
	private Context mContext;   // The Activity
	private SQLiteDatabase mDb;  // Pointer to the DB	
  
	public AcdiVocaDbHelper(Context context) {
		super(context, DBName, null, DBVersion);
		mDb = getWritableDatabase();
		onCreate(mDb);
		mDb = getWritableDatabase();
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
		db.execSQL(CREATE_FINDS_TABLE);
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
		onCreate(db);	
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
	 * This method is called from a Find object to add its data to DB.
	 * @param values contains the key/value pairs for each Db column,
	 * @return the rowId of the new insert or -1 in case of error
	 */
	public long addNewFind(ContentValues values) {
		mDb = getWritableDatabase();  // Either create the DB or open it.
		long rowId = mDb.insert(FINDS_TABLE, null, values);
		Log.i(TAG, "addNewFind, rowId=" + rowId);
		mDb.close();
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

		try {
			Log.i(TAG, "updateFind id = " + id);

			
			// Update the Finds table with the new data
			success = mDb.update(FINDS_TABLE, args, FINDS_ID + "=" + id, null) > 0;
			Log.i(TAG,"updateFind result = "+success);  
		} catch (Exception e){
			Log.i("Error in update Find transaction", e.toString());
		} finally {
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
	public boolean updateFind(String guId, ContentValues args, List<ContentValues> images ) {
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
		
		return success;
	}
	
	/**
	 * This method is called from ListActivity to delete all the finds currently
	 *  in the DB. It marks them all "deleted" and deletes their images.
	 * @return
	 */
	public boolean deleteAllFinds() {
		mDb = getWritableDatabase();
		ContentValues content = new ContentValues();
		content.put(FINDS_DELETED, DELETE_FIND);

		boolean success = mDb.update(FINDS_TABLE, content, null, null) > 0;
		mDb.close();
		if (success)
			Log.i(TAG, "deleteAllFinds marked finds deleted ... deleting photos");
		//			return deleteAllPhotos();
		return success;
	}

	/** 
	 * SUSPECT:  PositDbHelper should not return a Cursor -- causes memory leaks: 
	 * Returns a Cursor with rows for all Finds of a given project.
	 * @return
	 */
	public Cursor fetchFindsByProjectId(int project_id, String order_by) {
		mDb = getReadableDatabase(); // Either open or create the DB.
		Cursor c = mDb.query(FINDS_TABLE,null, 
				FINDS_PROJECT_ID +"="+project_id, null, null, null, order_by);
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
	
	public ContentValues fetchAllCommunes() {
		mDb = getReadableDatabase();
		Cursor c = mDb.query(COMMUNE_TABLE, null, null, null, null, null, null);
		c.moveToFirst();
		ContentValues values = null;
		if (c.getCount()!=0)
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

		String[] selectionArgs = null;
		String groupBy = null, having = null, orderBy = null;
		Cursor c = mDb.query(FINDS_TABLE, columns, 
				FINDS_ID+"="+id, selectionArgs, groupBy, having, orderBy);
				//WHERE_NOT_DELETED + " AND " + FINDS_ID+"="+id, selectionArgs, groupBy, having, orderBy);
		c.moveToFirst();
		ContentValues values = null;
		if (c.getCount() != 0)
			values = this.getContentValuesFromRow(c);
		c.close();
		mDb.close();
		return values;
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
			
				if (DBG) Log.i(TAG, "getContentValuesFromRow, Column " + column + " = " + 
					c.getString(c.getColumnIndexOrThrow(column)));
			values.put(column, c.getString(c.getColumnIndexOrThrow(column)));
		}
		return values;
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

		if (success) {
			Log.i(TAG, "deleteFind " + id + " deleted photos");
		}
		
		return success;
	}
}
