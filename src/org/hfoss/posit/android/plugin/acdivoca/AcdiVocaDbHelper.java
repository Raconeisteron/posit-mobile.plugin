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


import java.util.ArrayList;
import java.sql.Date;
import java.sql.Time;
import org.hfoss.posit.android.R;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

/**
 * The class is the interface with the Database. 
 *  It controls all Db access 
 *  and directly handles all Db queries.
 */
public class AcdiVocaDbHelper {


	private static final String TAG = "DbHelper";

	private static final boolean DBG = false;
	private static final String DATABASE_NAME ="posit";
	public static final int DATABASE_VERSION = 2;
	public enum UserType {SUPER, ADMIN, OWNER, USER};

	/**
	 * Private helper class for managing Db operations.
	 * @see http://www.screaming-penguin.com/node/7742
	 */
	private static class OpenHelper extends SQLiteOpenHelper {

		OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/*
		 * Add new tables here. This version creates the default SUPER
		 * user and a default non-super user.
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.i(TAG, "Creating database tables: " 
					+ FINDS_TABLE + "," + USER_TABLE + "," + MESSAGE_TABLE);
			db.execSQL(CREATE_FINDS_TABLE);
			db.execSQL(CREATE_USER_TABLE);	
			db.execSQL(CREATE_MESSAGE_TABLE);

			ContentValues values = new ContentValues();

			values.put(USER_USERNAME, SUPER_USER_NAME);
			values.put(USER_PASSWORD, SUPER_USER_PASSWORD);
			values.put(USER_TYPE_STRING, UserType.SUPER.ordinal());
			if (!addUser(db, values, UserType.SUPER))
				Log.e(TAG, "Error adding user = " + SUPER_USER_NAME);

			values.put(USER_USERNAME, ADMIN_USER_NAME);
			values.put(USER_PASSWORD, ADMIN_USER_PASSWORD);
			values.put(USER_TYPE_STRING, UserType.ADMIN.ordinal());
			if (!addUser(db, values, UserType.ADMIN)) 
				Log.e(TAG, "Error adding user = " + ADMIN_USER_NAME);

			values.put(USER_USERNAME, USER_DEFAULT_NAME);
			values.put(USER_PASSWORD, USER_DEFAULT_PASSWORD);
			values.put(USER_TYPE_STRING, UserType.USER.ordinal());
			if (addUser(db, values, UserType.USER)) 
				Log.e(TAG, "Error adding user = " + USER_DEFAULT_NAME);
		}

		/**
		 * Called when the Database requires upgrading to a new version. Not
		 * sure how it works.
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(TAG, "Upgrading database, this will drop tables and recreate.");
			//db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}
	}


	/**
	 * The user table
	 */
	public static final String USER_TABLE = "acdi_voca_users";
	public static final String USER_ID = "_id";
	public static final String USER_USERNAME = "username";
	public static final String USER_PASSWORD = "password";
	public static final String USER_DEFAULT_NAME = "b";      // For testing purposes
	public static final String USER_DEFAULT_PASSWORD = "b";
	public static final String ADMIN_USER_NAME = "r";
	public static final String ADMIN_USER_PASSWORD = "a";
	public static final String SUPER_USER_NAME = "s";
	public static final String SUPER_USER_PASSWORD = "a";	
	public static final String USER_TYPE_STRING = "UserType";
	public static final String USER_TYPE_KEY= "UserLoginType";


	private static final String CREATE_USER_TABLE = "CREATE TABLE IF NOT EXISTS "
		+ USER_TABLE + "(" + USER_ID + " integer primary key autoincrement, "
		+ USER_USERNAME + " text, "
		+ USER_PASSWORD + " text, "
		+ USER_TYPE_STRING + " integer "
		+ ")";

	public static final String MESSAGE_TABLE = "sms_message_log";
	public static final String MESSAGE_ID = "_id";
	public static final int UNKNOWN_ID = -9999;
	public static final String MESSAGE_BENEFICIARY_ID = AttributeManager.MESSAGE_BENEFICIARY_ID;  // Row Id in Beneficiary table
	public static final String MESSAGE_TEXT = AttributeManager.MESSAGE_TEXT;
	public static final String MESSAGE_STATUS = AttributeManager.FINDS_MESSAGE_STATUS; 
	public static final String[] MESSAGE_STATUS_STRINGS = {"Unsent", "Pending", "Sent", "Acknowledged", "Deleted"};
	public static final int MESSAGE_STATUS_UNSENT = 0;
	public static final int MESSAGE_STATUS_PENDING = 1;
	public static final int MESSAGE_STATUS_SENT = 2;
	public static final int MESSAGE_STATUS_ACK = 3;
	public static final int MESSAGE_STATUS_DEL = 4;
	public static final String MESSAGE_CREATED_AT = AttributeManager.MESSAGE_CREATED_AT;
	public static final String MESSAGE_SENT_AT = AttributeManager.MESSAGE_SENT_AT;
	public static final String MESSAGE_ACK_AT = AttributeManager.MESSAGE_ACK_AT;
	
	private static final String CREATE_MESSAGE_TABLE = "CREATE TABLE IF NOT EXISTS "
		+ MESSAGE_TABLE + "(" 
		+ MESSAGE_ID + " integer primary key autoincrement, "
		+ MESSAGE_BENEFICIARY_ID + " integer DEFAULT 0, "  // Beneficiary's Row_id
		+ MESSAGE_TEXT + " text, "
		+ MESSAGE_STATUS + " integer DEFAULT " + MESSAGE_STATUS_UNSENT + ", "
		+ MESSAGE_CREATED_AT + " timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, "
		+ MESSAGE_SENT_AT + " timestamp, "
		+ MESSAGE_ACK_AT + " timestamp"
		+ ")";
	
	/**
	 *  The Beneficiary table
	 */
	public static final String FINDS_TABLE = "acdi_voca_finds";
	public static final String FINDS_ID = "_id";
	public static final String FINDS_DOSSIER = AttributeManager.FINDS_DOSSIER;
	public static final String FINDS_PROJECT_ID = "project_id";
	public static final String FINDS_NAME = "name";
	
	public static final String FINDS_TYPE = AttributeManager.FINDS_TYPE;    
	public static final int FINDS_TYPE_MCHN = 0;
	public static final int FINDS_TYPE_AGRI = 1;
	public static final int FINDS_TYPE_BOTH = 2;
	public static final String[] FIND_TYPE_STRINGS = {"MCHN", "AGRI", "BOTH"};  // For display purpose

	public static final String FINDS_STATUS = AttributeManager.FINDS_STATUS;
	public static final int FINDS_STATUS_NEW = 0;      // New registration, no Dossier ID
	public static final int FINDS_STATUS_UPDATE = 1;   // Update, imported from TBS, with Dossier ID
	public static final int FINDS_STATUS_DONTCARE = -1;  
	public static final String[] FIND_STATUS_STRINGS = {"New", "Update"};  // For display purpose

//	public static final String FINDS_MESSAGE_ID = MESSAGE_STATUS;
	public static final String FINDS_MESSAGE_ID = AttributeManager.FINDS_MESSAGE_ID;
	public static final String FINDS_MESSAGE_STATUS = MESSAGE_STATUS;

	public static final String FINDS_FIRSTNAME = AttributeManager.FINDS_FIRSTNAME;
	public static final String FINDS_LASTNAME = AttributeManager.FINDS_LASTNAME;

	public static final String FINDS_ADDRESS = AttributeManager.FINDS_ADDRESS;
	public static final String FINDS_DOB = AttributeManager.FINDS_DOB;
	public static final String FINDS_SEX = AttributeManager.FINDS_SEX;
	public static final String FINDS_AGE = "age";

	public static final String FINDS_BENEFICIARY_CATEGORY = AttributeManager.FINDS_BENEFICIARY_CATEGORY;
	public static final String FINDS_HOUSEHOLD_SIZE = AttributeManager.FINDS_HOUSEHOLD_SIZE;

	public static final String FINDS_DISTRIBUTION_POST = AttributeManager.FINDS_DISTRIBUTION_POST;
//	public static final String FINDS_HEALTH_CENTER = AttributeManager.FINDS_HEALTH_CENTER;
	public static final String FINDS_Q_MOTHER_LEADER = AttributeManager.FINDS_Q_MOTHER_LEADER; // "mother_leader";
	public static final String FINDS_Q_VISIT_MOTHER_LEADER = AttributeManager.FINDS_Q_VISIT_MOTHER_LEADER; // "visit_mother_leader";
	public static final String FINDS_Q_PARTICIPATING_AGRI = AttributeManager.FINDS_Q_PARTICIPATING_AGRI; // "pariticipating_agri";
	public static final String FINDS_Q_RELATIVE_AGRI = AttributeManager.FINDS_Q_RELATIVE_AGRI; // "pariticipating_agri";
	public static final String FINDS_Q_PARTICIPATING_BENE = AttributeManager.FINDS_Q_PARTICIPATING_BENE; // "pariticipating_agri";
	public static final String FINDS_Q_RELATIVE_BENE = AttributeManager.FINDS_Q_RELATIVE_BENE; // "pariticipating_agri";
	
	public static final String FINDS_NAME_AGRI_PARTICIPANT = AttributeManager.FINDS_NAME_AGRI_PARTICIPANT; // "name_agri_paricipant";

	
	public static final String FINDS_GUID = "guid";    // Globally unique ID


	//added to handle the agriculture registration form
	public static final String FINDS_LAND_AMOUNT = AttributeManager.FINDS_LAND_AMOUNT; // "amount_of_land";	
//	public static final String FINDS_SEED_AMOUNT = "seed_amount";
//	public static final String FINDS_UNIT = "unit";
	public static final String FINDS_IS_FARMER = AttributeManager.FINDS_IS_FARMER; //  "is_farmer";
	public static final String FINDS_IS_MUSO = AttributeManager.FINDS_IS_MUSO;  // "is_MUSO";
	public static final String FINDS_IS_RANCHER = AttributeManager.FINDS_IS_RANCHER;  //  "is_rancher";
	public static final String FINDS_IS_STOREOWN = AttributeManager.FINDS_IS_STOREOWN; //  "is_store_owner";
	public static final String FINDS_IS_FISHER = AttributeManager.FINDS_IS_FISHER;  // "is_fisher";
	public static final String FINDS_IS_OTHER = AttributeManager.FINDS_IS_OTHER;  // "is_other";
	public static final String FINDS_IS_ARTISAN = AttributeManager.FINDS_IS_ARTISAN; // "is_artisan";
	
	public static final String FINDS_HAVE_VEGE = AttributeManager.FINDS_HAVE_VEGE; //  "have_vege";
	public static final String FINDS_HAVE_CEREAL = AttributeManager.FINDS_HAVE_CEREAL;  //  "have_cereal";
	public static final String FINDS_HAVE_TUBER = AttributeManager.FINDS_HAVE_TUBER;  // "have_tuber";
	public static final String FINDS_HAVE_TREE = AttributeManager.FINDS_HAVE_TREE; // "have_tree";
	public static final String FINDS_HAVE_HOUE = AttributeManager.FINDS_HAVE_HOUE;  //  "have_houe";
	public static final String FINDS_HAVE_PIOCHE = AttributeManager.FINDS_HAVE_PIOCHE;  // "have_pioche";
	public static final String FINDS_HAVE_BROUETTE = AttributeManager.FINDS_HAVE_BROUETTE; // "have_brouette";
	public static final String FINDS_HAVE_MACHETTE = AttributeManager.FINDS_HAVE_MACHETTE; //  "have_machette";
	public static final String FINDS_HAVE_SERPETTE = AttributeManager.FINDS_HAVE_SERPETTE;  // "have_serpette";
	public static final String FINDS_HAVE_PELLE = AttributeManager.FINDS_HAVE_PELLE;  // "have_pelle";
	public static final String FINDS_HAVE_BARREAMINES = AttributeManager.FINDS_HAVE_BARREAMINES; // "have_barreamines";
	public static final String FINDS_RELATIVE_1 = AttributeManager.FINDS_RELATIVE_1;  // "relative_1";
//	public static final String FINDS_RELATIVE_2 = AttributeManager.FINDS_RELATIVE_2;  // "relative_2";
	public static final String FINDS_HAVE_COFFEE = AttributeManager.FINDS_HAVE_COFFEE; //  "have_vege";

	public static final String FINDS_PARTNER_FAO = AttributeManager.FINDS_PARTNER_FAO;// "partner_fao";
	public static final String FINDS_PARTNER_SAVE = AttributeManager.FINDS_PARTNER_SAVE;// "partner_save";
	public static final String FINDS_PARTNER_CROSE = AttributeManager.FINDS_PARTNER_CROSE;// "partner_crose";
	public static final String FINDS_PARTNER_PLAN = AttributeManager.FINDS_PARTNER_PLAN;// "partner_plan";
	public static final String FINDS_PARTNER_MARDNR = AttributeManager.FINDS_PARTNER_MARDNR;// "partner_mardnr";
	public static final String FINDS_PARTNER_OTHER = AttributeManager.FINDS_PARTNER_OTHER;// "partner_other";
	
	

	public static final String FINDS_MALNOURISHED = AttributeManager.FINDS_MALNOURISHED;  // "MALNOURISHED";
	public static final String FINDS_PREVENTION = AttributeManager.FINDS_PREVENTION;     // "PREVENTION";
	public static final String FINDS_EXPECTING = AttributeManager.FINDS_EXPECTING;   // "EXPECTING";
	public static final String FINDS_NURSING = AttributeManager.FINDS_NURSING;      // "NURSING";
	
    public static final String FINDS_MALE = AttributeManager.FINDS_MALE;          // "MALE";
    public static final String FINDS_FEMALE = AttributeManager.FINDS_FEMALE;        // "FEMALE";
    public static final String FINDS_YES = AttributeManager.FINDS_YES;           // "YES";
    public static final String FINDS_NO = AttributeManager.FINDS_NO;            // "NO";
    public static final String FINDS_TRUE = AttributeManager.FINDS_TRUE;       // "TRUE";
    public static final String FINDS_FALSE = AttributeManager.FINDS_FALSE;      // "FALSE";
    public static final String FINDS_COMMUNE_SECTION = AttributeManager.LONG_COMMUNE_SECTION;
    
    
	public static final String FINDS_Q_PRESENT = AttributeManager.FINDS_Q_PRESENT;   // "Present";
	public static final String FINDS_Q_TRANSFER = AttributeManager.FINDS_Q_TRANSFER;   //"Transfer";
	public static final String FINDS_Q_MODIFICATION = AttributeManager.FINDS_Q_MODIFICATIONS;  // "Modifications";
	public static final String FINDS_MONTHS_REMAINING = "MonthsRemaining";
	public static final String FINDS_Q_CHANGE = AttributeManager.FINDS_Q_CHANGE;   //"ChangeInStatus";   // Added to incorporated changes to beneficiary type
	public static final String FINDS_CHANGE_TYPE = AttributeManager.FINDS_CHANGE_TYPE;   //"ChangeType";
	
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

	public static final String FINDS_HISTORY_TABLE = "acdi_voca_finds_history";
	public static final String HISTORY_ID = "_id" ;

	/*
	 * Finds table creation sql statement. 
	 */
	private static final String CREATE_FINDS_TABLE = "CREATE TABLE IF NOT EXISTS "
		+ FINDS_TABLE  
		+ " (" + FINDS_ID + " integer primary key autoincrement, "
		+ FINDS_PROJECT_ID + " integer DEFAULT 0, "
		+ FINDS_DOSSIER + " text, "
		+ FINDS_TYPE + " integer DEFAULT 0, "                             // MCHN or Agri                 
		+ FINDS_STATUS + " integer , " //  DEFAULT ,"         // + FINDS_STATUS_NEW + ", "    // New or Update record
//		+ FINDS_MESSAGE + " integer DEFAULT " + MESSAGE_STATUS_UNSENT ,"                  // Reference to Message table Id
		+ FINDS_MESSAGE_ID + " integer DEFAULT " + 0 + ", "
		+ FINDS_MESSAGE_STATUS + " integer DEFAULT " + MESSAGE_STATUS_UNSENT + " ,"
//		+ FINDS_MESSAGE_TEXT + " text, "
 		+ FINDS_NAME + " text, "
		+ FINDS_FIRSTNAME + " text, "
		+ FINDS_LASTNAME + " text, "
		+ FINDS_ADDRESS + " text, "
		+ FINDS_DOB + " date, "
		+ FINDS_SEX + " text, "
//		+ FINDS_AGE + " text, "
		+ FINDS_HOUSEHOLD_SIZE + " text, "
		+ FINDS_BENEFICIARY_CATEGORY + " text, "
//		+ FINDS_HEALTH_CENTER + " text, "
		+ FINDS_DISTRIBUTION_POST + " text, "
		+ FINDS_Q_MOTHER_LEADER + " boolean, "
		+ FINDS_Q_VISIT_MOTHER_LEADER + " boolean, "
		+ FINDS_Q_PARTICIPATING_AGRI + " boolean, "
		+ FINDS_Q_RELATIVE_AGRI + " boolean, "
		+ FINDS_Q_PARTICIPATING_BENE + " boolean, "
		+ FINDS_Q_RELATIVE_BENE + " boolean, "
		+ FINDS_IS_FARMER + " boolean, "
		+ FINDS_IS_MUSO + " boolean, "
		+ FINDS_IS_RANCHER + " boolean, "
		+ FINDS_IS_STOREOWN + " boolean, "
		+ FINDS_IS_FISHER + " boolean, "
		+ FINDS_IS_OTHER + " boolean, "
		+ FINDS_IS_ARTISAN + " boolean, "
		+ FINDS_LAND_AMOUNT+ " integer DEFAULT 0, "
		+ FINDS_HAVE_VEGE+ " boolean, "
		+ FINDS_HAVE_TUBER+ " boolean, "
		+ FINDS_HAVE_CEREAL+ " boolean, "
		+ FINDS_HAVE_TREE+ " boolean, "
		+ FINDS_HAVE_COFFEE + " boolean, "
		+ FINDS_PARTNER_FAO + " boolean, "
		+ FINDS_PARTNER_SAVE + " boolean, "
		+ FINDS_PARTNER_CROSE + " boolean, "
		+ FINDS_PARTNER_PLAN + " boolean, "
		+ FINDS_PARTNER_MARDNR + " boolean, "
		+ FINDS_PARTNER_OTHER + " boolean, "		
		+ FINDS_COMMUNE_SECTION + " text, "
//		+ FINDS_SEED_AMOUNT+ " integer DEFAULT 0, "
//		+ FINDS_UNIT + " text, "
		+ FINDS_HAVE_HOUE+ " boolean, "
		+ FINDS_HAVE_PIOCHE+ " boolean, "
		+ FINDS_HAVE_BROUETTE+ " boolean, "
		+ FINDS_HAVE_MACHETTE+ " boolean, "
		+ FINDS_HAVE_SERPETTE+ " boolean, "
		+ FINDS_HAVE_PELLE+ " boolean, "
		+ FINDS_HAVE_BARREAMINES+ " boolean, "

		+ FINDS_RELATIVE_1 + " text, "
//		+ FINDS_RELATIVE_2 + " text, "
		+ FINDS_Q_CHANGE + " boolean, "
		+ FINDS_CHANGE_TYPE + " text, "
		+ FINDS_Q_PRESENT + " boolean DEFAULT FALSE ,"
//		+ FINDS_Q_TRANSFER + " boolean, "
//		+ FINDS_Q_MODIFICATION + " boolean, " 
		+ FINDS_MONTHS_REMAINING + " integer DEFAULT 0, "
		+ FINDS_NAME_AGRI_PARTICIPANT + " text "
		+ ");";

	// Fields for reading the beneficiaries.txt file. The numbers correspond to
	// the columns.  These might need to be changed.
//	*No dossier,Nom,Prenom,Section Communale,Localite beneficiaire,Date entree,Date naissance,Sexe,Categorie,Poste distribution,
//	068MP-FAT, Balthazar,Denisana,Mapou,Saint Michel,2010/08/03,1947/12/31, F,Enfant Prevention,Dispensaire Mapou,
	private static final int FIELD_DOSSIER = 0;
	private static final int FIELD_LASTNAME = 1;
	private static final int FIELD_FIRSTNAME = 2;
	private static final int FIELD_SECTION = 3;
	private static final int FIELD_LOCALITY = 4;
	private static final int FIELD_ENTRY_DATE = 5;
	private static final int FIELD_BIRTH_DATE = 6;
	private static final int FIELD_SEX = 7;
	private static final int FIELD_CATEGORY = 8;
	private static final int FIELD_DISTRIBUTION_POST = 9;
	private static final String COMMA= ",";

	private static final int AGRI_FIELD_DOSSIER = 0;
	private static final int AGRI_FIELD_LASTNAME = 1;
	private static final int AGRI_FIELD_FIRSTNAME = 2;
	private static final int AGRI_FIELD_COMMUNE = 3;
	private static final int AGRI_FIELD_SECTION = 4;
	private static final int AGRI_FIELD_LOCALITY = 5;
	private static final int AGRI_FIELD_ENTRY_DATE = 6;
	private static final int AGRI_FIELD_BIRTH_DATE = 7;
	private static final int AGRI_FIELD_SEX = 8;
	private static final int AGRI_FIELD_CATEGORY = 9;
	private static final int AGRI_FIELD_NUM_PERSONS  = 10;
	
	
	// Needed for ListFindsActivity to display a row in the list.
	// The following two arrays go together to form a <DB value, UI View> pair
	// except for the first DB value, which is just a filler.
	//	 GUID commented out so that in the list of finds the ID is no longer displayed
	//	 in an attempt to deal with the length of the new UUIDs
	//   -->UPDATED UUID is now truncated in display 
	public static final String[] list_row_data = { 
		FINDS_ID,
		FINDS_DOSSIER,  
		FINDS_LASTNAME,
		FINDS_FIRSTNAME,
		FINDS_MESSAGE_STATUS,
		FINDS_DOB,
		FINDS_SEX,
		FINDS_HOUSEHOLD_SIZE,
		FINDS_BENEFICIARY_CATEGORY,
//		FINDS_HEALTH_CENTER,
		FINDS_DISTRIBUTION_POST
	};

	public static final int[] list_row_views = {
		R.id.row_id,		    
		R.id.dossierText,
		R.id.lastname_field, 
		R.id.firstname_field,
		R.id.messageStatusText,
		R.id.datepicker,
		R.id.femaleRadio,
		R.id.inhomeEdit,
		R.id.expectingRadio,
//		R.id.healthcenterSpinner,
		R.id.distributionSpinner
	};

//	public static final String[] message_row_data = { 
//		FINDS_ID,
//		FINDS_DOSSIER, 
//		MESSAGE_TEXT
//	};
//
//	public static final int[] message_row_views = {
//		R.id.row_id,		    
//		R.id.dossierText,
//		R.id.messageText 
//	};


	private static Context mContext;   // The Activity
	private SQLiteDatabase mDb;  // Pointer to the DB	

	
	/**
	 * Constructor just saves and opens the Db. The Db
	 * is closed in the public methods.
	 * @param context
	 */
	public AcdiVocaDbHelper(Context context) {
		this.mContext= context;
		OpenHelper openHelper = new OpenHelper(this.mContext);
		mDb = openHelper.getWritableDatabase();
	}

	/**
	 * Insert a new user into the USER_TABLE.  Will not insert duplicate users.
	 * @param values the key/value pairs for each Db column.
	 * @param userType the type of user, either SUPER or USER
	 * @return true if the insertion succeeds and false otherwise
	 */
	public boolean addUser(ContentValues values, UserType userType) {
		boolean result = addUser(mDb, values, userType);
		return result;
	}


	/**
	 * Helper method to insert a new user into the USER_TABLE
	 * @param db the previously open database
	 * @param values  the attribute/value pairs to insert into the USER_TABLE
	 * @param the type of user, SUPER or USER.
	 */
	private static boolean addUser(SQLiteDatabase db, ContentValues values, UserType userType) {
		String username = values.getAsString(USER_USERNAME);
		String password = values.getAsString(USER_PASSWORD);

		// Check against duplicates
		String[] columns = null; //{ USER_PASSWORD };
		Cursor c = db.query(USER_TABLE, columns, 
				USER_USERNAME + "="+ "'" + username + "'" ,
				null, null, null, null);
		//Log.i(TAG, "Cursor size = " + c.getCount());
		if (c.getCount() == 0) {
			long rowId = db.insert(USER_TABLE, null, values);
			//mDb.close();
			Log.i(TAG, "addUser " + username + " at rowId=" + rowId);
			c.close();
			return true;
		}	
		c.close();
		return false;
	}


	/**
	 * Returns true iff a row containing username and password is found
	 * @param username
	 * @param password
	 * @param userType is an enum that defines whether this is a regular or super user.
	 * @return
	 */
	public int authenicateUser(String username, String password, UserType userType) {
		int result = 0;
		if (userType.equals(UserType.ADMIN)) {
			if (! ((username.equals(ADMIN_USER_NAME) &&  password.equals(ADMIN_USER_PASSWORD)) 
				|| (username.equals(SUPER_USER_NAME) && password.equals(SUPER_USER_PASSWORD)) )) {
				Log.i(TAG, "Sorry you must be ADMIN USER to do this.");
				Toast.makeText(mContext,mContext.getString(R.string.toast_adminuser), Toast.LENGTH_SHORT);
				result = -1;
			}
		} else if (userType.equals(UserType.SUPER)) {
			if (!username.equals(SUPER_USER_NAME) ||  !password.equals(SUPER_USER_PASSWORD)) {
				Log.i(TAG, "Sorry you must be SUPER USER to do this.");
				Toast.makeText(mContext, mContext.getString(R.string.toast_superuser), Toast.LENGTH_SHORT);
				result = -1;
			}
		} 
		if (result != -1) {
			//		String[] columns = { USER_PASSWORD, USER_TYPE_STRING };
			Cursor c = mDb.query(USER_TABLE, null, 
					USER_USERNAME + "="+ "'" + username + "'" + 
					" and " + USER_PASSWORD + "=" + "'" + password + "'" , null, null, null, null);
			c.moveToFirst();
			Log.i(TAG, "Cursor size = " + c.getCount());

			if (c.isAfterLast()) 
				result =  -1;
			else {
				result = c.getInt(c.getColumnIndex(USER_TYPE_STRING));
			}
			c.close();
		}
		mDb.close();
		//dumpUsers();
		return result;
	}

	public void dumpUsers() {
		Log.i(TAG, "Dumping user table");
		//		mDb = getReadableDatabase();
		Cursor c = mDb.query(USER_TABLE, null, null, null, null, null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			Log.i(TAG,this.getContentValuesFromRow(c).toString());
			c.moveToNext();
		}
		c.close();
		mDb.close();
	}

	/**
	 * This method is called from a Beneficiary object to add its data to DB.
	 * @param values contains the key/value pairs for each Db column,
	 * @return the rowId of the new insert or -1 in case of error
	 */
	public long addNewBeneficiary(ContentValues values) {
		//		mDb = getWritableDatabase();  // Either create the DB or open it.
		long rowId = mDb.insert(FINDS_TABLE, null, values);
		Log.i(TAG, "addNewFind, rowId=" + rowId);
		mDb.close();
		return rowId;
	}

	
	/**
	 * Inserts an array of beneficiaries read from AcdiVoca Db.
	 * NOTE:  The Android date picker stores months as 0..11, so
	 *  we have to adjust dates.
	 * @param beneficiaries
	 * @return
	 */
	public int addAgriBeneficiaries(String[] beneficiaries) {
		Log.i(TAG, "Adding " + beneficiaries.length + " AGRI beneficiaries");
		String fields[] = null;
		ContentValues values = new ContentValues();
		int count = 0;

		for (int k = 0; k < beneficiaries.length; k++) {

			fields = beneficiaries[k].split(COMMA);
//			values.put(FINDS_MESSAGE_STATUS, MESSAGE_STATUS_UNSENT);
			values.put(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_AGRI);
			values.put(AcdiVocaDbHelper.FINDS_STATUS, AcdiVocaDbHelper.FINDS_STATUS_UPDATE);
			values.put(AcdiVocaDbHelper.FINDS_DOSSIER,fields[AGRI_FIELD_DOSSIER]);
			values.put(AcdiVocaDbHelper.FINDS_LASTNAME, fields[AGRI_FIELD_LASTNAME]);
			values.put(AcdiVocaDbHelper.FINDS_FIRSTNAME, fields[AGRI_FIELD_FIRSTNAME]);
			//				values.put(AcdiVocaDbHelper.COMMUNE_SECTION_NAME, fields[FIELD_SECTION]);
			values.put(AcdiVocaDbHelper.FINDS_ADDRESS, fields[AGRI_FIELD_LOCALITY]);
			String adjustedDate = adjustDateForDatePicker(fields[AGRI_FIELD_BIRTH_DATE]);
//			Log.i(TAG, "adjusted date = " + adjustedDate);
			values.put(AcdiVocaDbHelper.FINDS_DOB, adjustedDate);
			String adjustedSex = adjustSexData(fields[AGRI_FIELD_SEX]);
			values.put(AcdiVocaDbHelper.FINDS_SEX, adjustedSex);  
			String adjustedCategory = adjustCategoryData(fields[AGRI_FIELD_CATEGORY]);
			values.put(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY, adjustedCategory);
			values.put(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE, fields[AGRI_FIELD_NUM_PERSONS]);

			//Log.i(TAG, values.toString());
			long rowId = mDb.insert(FINDS_TABLE, null, values);
			if (rowId != -1) 
				++count;

			//addNewBeneficiary(values);
		}
		mDb.close();
		Log.i(TAG, "Inserted to Db " + count + " Beneficiaries");
		return count;
	}

	
	/**
	 * Inserts an array of beneficiaries read from AcdiVoca Db.
	 * NOTE:  The Android date picker stores months as 0..11, so
	 *  we have to adjust dates.
	 * @param beneficiaries
	 * @return
	 */
	public int addUpdateBeneficiaries(String[] beneficiaries) {
		Log.i(TAG, "Adding " + beneficiaries.length + " MCHN beneficiaries");
		String fields[] = null;
		ContentValues values = new ContentValues();
		int count = 0;

		for (int k = 0; k < beneficiaries.length; k++) {

			fields = beneficiaries[k].split(COMMA);
//			values.put(FINDS_MESSAGE_STATUS, MESSAGE_STATUS_UNSENT);
			values.put(AcdiVocaDbHelper.FINDS_TYPE, AcdiVocaDbHelper.FINDS_TYPE_MCHN);
			values.put(AcdiVocaDbHelper.FINDS_STATUS, AcdiVocaDbHelper.FINDS_STATUS_UPDATE);
			values.put(AcdiVocaDbHelper.FINDS_DOSSIER,fields[FIELD_DOSSIER]);
			values.put(AcdiVocaDbHelper.FINDS_LASTNAME, fields[FIELD_LASTNAME]);
			values.put(AcdiVocaDbHelper.FINDS_FIRSTNAME, fields[FIELD_FIRSTNAME]);
			//				values.put(AcdiVocaDbHelper.COMMUNE_SECTION_NAME, fields[FIELD_SECTION]);
			values.put(AcdiVocaDbHelper.FINDS_ADDRESS, fields[FIELD_LOCALITY]);
			String adjustedDate = adjustDateForDatePicker(fields[FIELD_BIRTH_DATE]);
//			Log.i(TAG, "adjusted date = " + adjustedDate);
			values.put(AcdiVocaDbHelper.FINDS_DOB, adjustedDate);
			String adjustedSex = adjustSexData(fields[FIELD_SEX]);
			values.put(AcdiVocaDbHelper.FINDS_SEX, adjustedSex);  
			String adjustedCategory = adjustCategoryData(fields[FIELD_CATEGORY]);
			values.put(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY, adjustedCategory);
			values.put(AcdiVocaDbHelper.FINDS_DISTRIBUTION_POST, fields[FIELD_DISTRIBUTION_POST]);

			//Log.i(TAG, values.toString());
			long rowId = mDb.insert(FINDS_TABLE, null, values);
			if (rowId != -1) 
				++count;

			//addNewBeneficiary(values);
		}
		mDb.close();
		Log.i(TAG, "Inserted to Db " + count + " Beneficiaries");
		return count;
	}

	/**
	 * Beneficiaries.txt represents categories in Haitian.  We represent them in 
	 * English.
	 * @param date
	 * @return
	 */
	private String adjustCategoryData(String category) {
		if (category.equals(AttributeManager.FINDS_MALNOURISHED_HA))
			return AttributeManager.FINDS_MALNOURISHED;
		else if (category.equals(AttributeManager.FINDS_EXPECTING_HA))
			return AttributeManager.FINDS_EXPECTING;
		else if (category.equals(AttributeManager.FINDS_NURSING_HA))
			return AttributeManager.FINDS_NURSING;		
		else if (category.equals(AttributeManager.FINDS_PREVENTION_HA))
			return AttributeManager.FINDS_PREVENTION;	
		else return category;
	}
	
	/**
	 * Beneficiaries.txt represents sex as 'M' or 'F'.  We represent them as
	 * 'FEMALE' or 'MALE'
	 * @param date
	 * @return
	 */
	private String adjustSexData(String sex) {
		if (sex.equals(AttributeManager.ABBREV_FEMALE))
			return AttributeManager.FINDS_FEMALE;
		else if (sex.equals(AttributeManager.ABBREV_MALE))
			return AttributeManager.FINDS_MALE;
		else return sex;
	}
	
	
	/**
	 * The Android date picker stores dates as 0..11.
	 * @param date
	 * @return
	 */
	@SuppressWarnings("finally")
	private String adjustDateForDatePicker(String date) {
		try {
			String[] yrmonday = date.split("/");
			date =  yrmonday[0] + "/" + (Integer.parseInt(yrmonday[1]) - 1) + "/" + yrmonday[2];
		} catch (Exception e) {
			Log.i(TAG, "Bad date = " + date + " " + e.getMessage());
			e.printStackTrace();
		} finally {
			return date;
		}
	}
	
	/**
	 * This function changes the date format from 0...11 to 1..12 format for the SMS Reader.
	 * @param date
	 * @return
	 */
	@SuppressWarnings("finally")
	private String adjustDateForSmsReader(String date) {
		try { 
			String[] yrmonday = date.split("/");
			date =  yrmonday[0] + "/" + (Integer.parseInt(yrmonday[1]) + 1) + "/" + yrmonday[2];	
		} catch (Exception e) {
			Log.i(TAG, "Bad date = " + date + " " + e.getMessage());
			e.printStackTrace();
		} finally {
			return date;
		}
	}


	/**
	 * Helper method to process ACKs for bulk update messages. Bulk updates
	 * are messages of the form AV=msgid,d1&d2&...&dN,  where d's are dossier numbers.
	 * Bulk update messages are stored in the message table. 
	 * 
	 * Uses the the message id to look up the bulk update message in the message table
	 * and updates the FINDS table with STATUS = ACK for each dossier number in the 
	 * bulk message. Finally, updates the MESSAGE table with STATUS=ACK.
	 * 
	 * @param acdiVocaMsg the message constructed by SmsManager.
	 * @return
	 */
	private boolean recordAcknowledgedBulkMessage(AcdiVocaMessage acdiVocaMsg) {
		int msgId = acdiVocaMsg.getMessageId();
		Log.i(TAG, "Recording Bulk ACK, msg_id = " + msgId);

		Cursor c = null;
			c = mDb.query(MESSAGE_TABLE, null, 
					MESSAGE_ID + "=" + msgId, 
					null, null, null, null);
			
		int rows = 0;
		
		// Should just be one row in cursor
		if (c.getCount() != 0) {
			c.moveToFirst();
			String msg = c.getString(c.getColumnIndex(MESSAGE_TEXT));
			String dossiers[] = msg.split(AttributeManager.LIST_SEPARATOR);
			ContentValues args = new ContentValues();
			args.put(FINDS_MESSAGE_STATUS, MESSAGE_STATUS_ACK);

			// For each dossier number in the message, update the FINDS table.
			for (int k = 0; k < dossiers.length; k++) {
				//Log.i(TAG, "Updating FINDS table for dossier = " + dossiers[k]);
				rows += mDb.update(FINDS_TABLE, 
						args, 
						FINDS_DOSSIER + " = " + "'" + dossiers[k] + "'",
						null); 
			}			
			Log.i(TAG, "Bulk Acknowleddge, rows = " + rows);
			
			// Finally, update the message table
			Log.i(TAG, "Updating MESSAGE table for msg = " + msgId);
			args = new ContentValues();
			args.put(MESSAGE_STATUS, MESSAGE_STATUS_ACK);
			rows += mDb.update(MESSAGE_TABLE, 
					args, 
					MESSAGE_ID + " = " + msgId,
					null); 
		} else {
			Log.i(TAG, "Failed to find message with id = " + msgId);
		}
		c.close();
		mDb.close();
		return rows > 0;
	}
	
	/**
	 * Record an incoming ACK from the modem. For normal messages, the modem 
	 * sends back the beneficiary id for the sent message.  This method looks 
	 * up the message id and updates the MESSAGE and FIND tables. 
	 * 
	 * NOTE: This method also receives ACKs for bulk messages, which it passes
	 * to a helper method. 
	 * 
	 * @param acdiVocaMsg the message constructed by SmsManager.
	 * @return
	 */
	public boolean recordAcknowledgedMessage(AcdiVocaMessage acdiVocaMsg) {
		int beneficiary_id = acdiVocaMsg.getBeneficiaryId();
		Log.i(TAG, "Recording ACK, bene_id = " + acdiVocaMsg.getBeneficiaryId());

		boolean result = false;
		
		// Beneficiary IDs are negative for  ACKs of Bulk messages.
		if (beneficiary_id < 0) {
			result = recordAcknowledgedBulkMessage(acdiVocaMsg);
			mDb.close();
			return result;
		}
		
		// Look up the beneficiary record and extract the message id
		Cursor c = null;
		c = mDb.query(FINDS_TABLE, null, 
				FINDS_ID + " = " + beneficiary_id, 
					null, null, null, null);
		
		int msg_id = UNKNOWN_ID;
		if (c.getCount() != 0) {
			Log.i(TAG, "Found beneficiary, id = " + beneficiary_id);
			c.moveToFirst();
			msg_id = c.getInt(c.getColumnIndex(FINDS_MESSAGE_ID));  // We should change the name of this.
		} else {
			Log.i(TAG, "Unable to find beneficiary, id = " + beneficiary_id);
		}
		c.close();
		
		acdiVocaMsg.setMessageId(msg_id);
		if (msg_id != UNKNOWN_ID) {
			result = updateMessageStatus(acdiVocaMsg, MESSAGE_STATUS_ACK);
		} else {
			Log.i(TAG, "Unable to find id for beneficiary id = " + beneficiary_id);
			result = false;
		}
		mDb.close();
		return result;
	}
	
	/**
	 * Updates beneficiary table for bulk dossier numbers -- i.e. n1&n2&...&nM.
	 * Bulk messages are sent to record absentees at AcdiVoca distribution events.
	 * @param acdiVocaMsg
	 * @return
	 */
	private synchronized int updateBeneficiaryTableForBulkIds(AcdiVocaMessage acdiVocaMsg, long msgId, int status) {
		String msg = acdiVocaMsg.getSmsMessage();
		Log.i(TAG, "updateBeneficiary Table, sms = " + msg);
		int rows = 0;
		String dossiers[] = msg.split(AttributeManager.LIST_SEPARATOR);
		for (int k = 0; k < dossiers.length; k++) {
			// Update the FINDS table to point to the message
			ContentValues args = new ContentValues();
			args.put(FINDS_MESSAGE_ID, msgId);
			args.put(FINDS_MESSAGE_STATUS, status);  // Both Find and Message table have message status
										
			rows += mDb.update(FINDS_TABLE, 
					args, 
					FINDS_DOSSIER + " = " + "'" + dossiers[k] + "'",
					null); 
		}
		mDb.close();
		return rows;
	}

	
	/**
	 * Updates the message status in the message table and Finds table.
	 * @param message the SMS message
	 * @param status the new status
	 * @return
	 */
	public synchronized boolean updateMessageStatus(AcdiVocaMessage acdiVocaMsg, int status) {
		Log.i(TAG, "Updating, msg_id " + acdiVocaMsg.getMessageId() + 
				" bene_id = " + acdiVocaMsg.getBeneficiaryId() + " to status = " + status);
		boolean result = false;
		long row_id;
		int rows = 0;
		int msg_id = acdiVocaMsg.getMessageId();
		String query = "";
		int beneficiary_id = acdiVocaMsg.getBeneficiaryId();
		
		rows = updateMessageTable(acdiVocaMsg, beneficiary_id, msg_id, status);

		if (rows == 0) {
			result = false;
			Log.i(TAG, "Unable to update message id= " + msg_id);  
		} else {
			result = true;
			Log.i(TAG, "Update message id= " + msg_id);  
		}

		// Update FINDS TABLE
		ContentValues args = new ContentValues();
		args.put(FINDS_MESSAGE_ID, msg_id);
		args.put(FINDS_MESSAGE_STATUS, status);  // Both Find and Message table have message status
													
		rows = mDb.update(FINDS_TABLE, 
				args, 
				FINDS_ID + " = " + beneficiary_id,
				null); 

		if (rows == 0) {
			result = false;
			Log.i(TAG, "Unable to update FINDS Table for beneficiary, id= " + beneficiary_id ); 

			rows = updateBeneficiaryTableForBulkIds(acdiVocaMsg, msg_id, status);
			Log.i(TAG, "Updated FINDS TABLE for bulk ids, rows = " + rows);

		} else {
			result = true;
			Log.i(TAG, "Updated FINDS Table for beneficiary, id= " + beneficiary_id); 

			// Update the message table with the new message
			rows = updateMessageTable(acdiVocaMsg, beneficiary_id, msg_id, status);					
		}
		mDb.close();
		return result;
	}
	
	/**
	 * Helper method to create a new entry in the message table and update the FINDS
	 * table to point to the message.
	 * @return
	 */
	public long createNewMessageTableEntry(AcdiVocaMessage acdiVocaMsg, int beneficiary_id, int status) {
		Log.i(TAG, "createNewMessage for beneficiary = " + beneficiary_id + " status= " + status);

		// Create a new message in the message table
		ContentValues args = new ContentValues();
		args.put(MESSAGE_BENEFICIARY_ID, beneficiary_id);
		args.put(MESSAGE_STATUS, status);
		args.put(MESSAGE_TEXT, acdiVocaMsg.getSmsMessage());

		long row_id = mDb.insert(MESSAGE_TABLE, null, args);

		if (row_id == -1) {
			Log.i(TAG, "Unable to insert NEW message, id= " + row_id);
		} else {
			Log.i(TAG, "Inserted NEW message, id= " + row_id + " bene_id=" + beneficiary_id); 

			int rows = 0;
			if (beneficiary_id == UNKNOWN_ID) {  // BULK Message
				rows = updateBeneficiaryTableForBulkIds(acdiVocaMsg, row_id, status);
				Log.i(TAG, "Updated FINDS TABLE for bulk ids, rows = " + rows);
			} else {

				// Update the FINDS table to point to the message
				args = new ContentValues();
				args.put(FINDS_MESSAGE_ID, row_id);
				args.put(FINDS_MESSAGE_STATUS, status);  // Both Find and Message table have message status

				rows = mDb.update(FINDS_TABLE, 
						args, 
						FINDS_ID + " = " + beneficiary_id,
						null); 

				if (rows == 0) {
					Log.i(TAG, "Unable to update FINDS Table for beneficiary, id= " + beneficiary_id ); 

				} else {
					Log.i(TAG, "Updated FINDS Table for beneficiary, id= " + beneficiary_id); 

					// Update the message table with the new message
					rows = updateMessageTable(acdiVocaMsg, beneficiary_id, row_id, status);					
				}
			}
		}
		mDb.close();
		return row_id;
	}
	
	/**
	 * Helper method to update the message table for an existing message.
	 * @return
	 */
	private synchronized int updateMessageTable(AcdiVocaMessage acdiVocaMsg, int beneficiary_id, long msg_id, int status ) {
		ContentValues args = new ContentValues();
		args.put(this.MESSAGE_BENEFICIARY_ID, beneficiary_id);
		args.put(MESSAGE_STATUS, status);
		
		String now = new Date(System.currentTimeMillis()).toString()
		+ " " + new Time(System.currentTimeMillis()).toString();
		Log.i(TAG, "Time now = " + now);

		if (status == MESSAGE_STATUS_SENT) {
			args.put(MESSAGE_SENT_AT, now );
		} else if (status == MESSAGE_STATUS_ACK) 
			args.put(MESSAGE_ACK_AT, now );

		int rows = mDb.update(MESSAGE_TABLE, 
				args, 
				MESSAGE_ID + " = " + msg_id,
				null); 
		if (rows > 0) {
			Log.i(TAG, "Updated the message table for beneficiary id  = " + beneficiary_id);
		}
		//mDb.close();  Don't close -- helper method
		return rows;
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
		//		mDb = getWritableDatabase();  // Either create or open the DB.

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
	 * SUSPECT:  PositDbHelper should not return a Cursor -- causes memory leaks: 
	 * Returns a Cursor with rows for all Finds of a given project.
	 * @return
	 */
	public Cursor fetchFindsByProjectId(int project_id, String order_by) {
		//		mDb = getReadableDatabase(); // Either open or create the DB.
		Cursor c = mDb.query(FINDS_TABLE,null, 
				FINDS_PROJECT_ID +"="+project_id, null, null, null, order_by);
		Log.i(TAG,"fetchFindsByProjectId " + FINDS_PROJECT_ID + "=" + project_id + " count=" + c.getCount());
		mDb.close();
		return c;
	}
	
	/** 
	 * SUSPECT:  PositDbHelper should not return a Cursor -- causes memory leaks: 
	 * Returns a Cursor with rows for all Finds of a given project.
	 * @return
	 */
	public Cursor fetchFindsByStatus(int status) {
		Cursor c = mDb.query(FINDS_TABLE,null,
				FINDS_STATUS + " = " + status, null, null, null, null);
		Log.i(TAG,"fetchFindsByStatus " + FINDS_STATUS + "=" + status + " count=" + c.getCount());
		mDb.close();
		return c;
	}

	/**
	 * Returns key/value pairs for selected columns with row selected by guId 
	 * @param guId the Find's globally unique Id
	 * @param columns an array of column names, can be left null
	 * @return
	 */
	public ContentValues fetchBeneficiaryByDossier(String dossier, String[] columns) {
		//		mDb = getReadableDatabase();  // Either open or create the DB    	
		String[] selectionArgs = null;
		String groupBy = null, having = null, orderBy = null;
		Cursor c = mDb.query(FINDS_TABLE, columns, 
				FINDS_DOSSIER + "= '" + dossier + "'", 
				selectionArgs, groupBy, having, orderBy);
		c.moveToFirst();
		ContentValues values = null;
		if (c.getCount() != 0)
			values = this.getContentValuesFromRow(c);
		c.close();
		mDb.close();
		return values;
	}
	
	/**
	 * Helper method performs the query and returns the Cursor. Data are
	 * pulled from the MESSAGE_TABLE.
	 * @param filter
	 * @param order_by
	 * @return
	 */
	private Cursor lookupMessages(int filter, int bene_status, String order_by) {
		int msg_status = 0;
		// Map the select result to the message status
		switch (filter) {
		case SearchFilterActivity.RESULT_SELECT_PENDING:
			msg_status = MESSAGE_STATUS_PENDING;
			break;
		case SearchFilterActivity.RESULT_SELECT_SENT:
			msg_status = MESSAGE_STATUS_SENT;
			break;
		case SearchFilterActivity.RESULT_SELECT_ACKNOWLEDGED:
			msg_status = MESSAGE_STATUS_ACK;
			break;
		default:
			break;
		}
		// Construct the where clause
//		String whereClause = MESSAGE_TABLE + "." + MESSAGE_STATUS + " = " + msg_status;
//		if (bene_status != FINDS_STATUS_DONTCARE) 
//		whereClause += " AND " + MESSAGE_TABLE + "." + MESSAGE_BENEFICIARY_ID + " = " + FINDS_TABLE + "." + FINDS_ID
//		+ " AND " + FINDS_TABLE + "." + FINDS_STATUS + " = " + bene_status;
		
		String whereClause = "";
		if (bene_status == FINDS_STATUS_DONTCARE) 
			whereClause = MESSAGE_STATUS + " = " +  msg_status;
		else 
			whereClause = MESSAGE_STATUS + " = " + msg_status 
			+ " AND EXISTS (SELECT * FROM " + FINDS_TABLE + " WHERE "
			+ MESSAGE_TABLE + "." + MESSAGE_BENEFICIARY_ID + " = " + FINDS_TABLE + "." + FINDS_ID
			+ " AND " + FINDS_TABLE + "." + FINDS_STATUS + " = " + bene_status + ")";
		String fromClause = MESSAGE_TABLE + "," + FINDS_TABLE;
		Cursor c = null;

		if (filter == SearchFilterActivity.RESULT_SELECT_ALL) // All messages
			c = mDb.query(true, MESSAGE_TABLE, null, null, null, null, null, order_by,null);
		else 
			c = mDb.query(true, MESSAGE_TABLE, null, whereClause,null, null, null, order_by,null);
		return c;
	}
		
	/**
	 * Helper method performs the query and returns the Cursor. Data are
	 * pulled from the FINDS_TABLE.  This method is called by createMessages()
	 * retrieve those beneficiaries for whom SMS messages will be sent.  For
	 * NEW beneficiaries all beneficiaries whose messages are UNSENT are returned.
	 * For UPDATE beneficiaries only those for whom  there's a change in STATUS
	 * are returned.
	 * @param filter
	 * @param order_by
	 * @return
	 */
	private Cursor lookupBeneficiaryRecords(int filter, String order_by, String distrCtr) {
		Cursor c = null;
		if (filter == SearchFilterActivity.RESULT_SELECT_NEW)
			c = mDb.query(FINDS_TABLE, null, 
					FINDS_STATUS + " = " + FINDS_STATUS_NEW  
					+ " AND " 
					+ FINDS_MESSAGE_STATUS + " = " + MESSAGE_STATUS_UNSENT,
					null, null, null, order_by);
		
		// Select all UPDATE Beneficiaries whose status has changed
		else if (filter == SearchFilterActivity.RESULT_SELECT_UPDATE)
			c = mDb.query(FINDS_TABLE, null, 
					FINDS_STATUS + " = " + FINDS_STATUS_UPDATE 
					+ " AND " 
					+ FINDS_MESSAGE_STATUS + " = " + MESSAGE_STATUS_UNSENT
					+ " AND " 
					+ FINDS_DISTRIBUTION_POST + "=" + "'" + distrCtr + "'" 
					+ " AND "  
					+ FINDS_Q_CHANGE + "=" +  "'" + FINDS_TRUE + "'",
					null, null, null, order_by);
		return c;
	}
	
	
	/**
	 * Returns an array of AcdiVocaMessages for new or updated beneficiaries. 
	 * Fetches the beneficiary records from the Db and converts the column names
	 * and their respective values to abbreviated attribute-value pairs.
	 * @param filter
	 * @param order_by
	 * @return
	 */
	public ArrayList<AcdiVocaMessage> createMessagesForBeneficiaries(int filter, String order_by, String distrCtr) {
		Cursor c = lookupBeneficiaryRecords(filter, order_by, distrCtr);
		Log.i(TAG,"createMessagesForBeneficiaries " +  " count=" + c.getCount() + " filter= " + filter);

		// Construct the messages and return as a String array
		ArrayList<AcdiVocaMessage> acdiVocaMsgs = new ArrayList<AcdiVocaMessage>();
		if (c.getCount() != 0) {
			
			//Log.i(TAG, "Columns=" + c.getColumnNames().toString());
			//acdiVocaMsgs = new ArrayList<AcdiVocaMessage>();
			c.moveToFirst();
			int k = 0;
			String smsMessage = null;
			String msgHeader = null;
			int msg_id = AcdiVocaDbHelper.UNKNOWN_ID;
			int beneficiary_id = AcdiVocaDbHelper.UNKNOWN_ID;
			int beneficiary_status = -1;
			int message_status = -1;
			String columns[] = null;
			
			// For debugging
//			columns = c.getColumnNames();   // The Db columns represent the attribute names
//			for (int j = 0; j < columns.length; j++) 
//				Log.i(TAG, columns[j] + "=" + c.getString(c.getColumnIndex(columns[j])));
			
			String rawMessage = "";
			String statusStr = "";

			while (!c.isAfterLast()) {
				beneficiary_id = c.getInt(c.getColumnIndex(FINDS_ID));
				beneficiary_status = c.getInt(c.getColumnIndex(FINDS_STATUS));
				message_status = c.getInt(c.getColumnIndex(FINDS_MESSAGE_ID));
				//Log.i(TAG, "message_status = " )
				//statusStr = MESSAGE_STATUS_STRINGS[message_status];

				columns = c.getColumnNames();
				rawMessage = "";

				// Construct the raw message with full attribute names
				// For each column (attribute), put an attr=val pair in the string
				String prn = "";
				for (int i=0; i < columns.length; i++){
					prn += columns[i]+" , ";
				}
				Log.i(TAG,">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				Log.i(TAG,prn);
				String empty= "";
				for (int j = 0; j < columns.length; j++) {
					String val = c.getString(c.getColumnIndex(columns[j]));
					if (val != null){
					if ((!columns[j].equals(MESSAGE_TEXT)) && (!val.equals(empty))) {
						rawMessage += 
							columns[j] +  "=" +
							c.getString(c.getColumnIndex(columns[j])) + ",";
					}
					}
				}

				// Now abbreviate the message
				smsMessage = abbreviateBeneficiaryStringForSms(rawMessage);
				if (beneficiary_status == FINDS_STATUS_NEW)  {
					smsMessage = AttributeManager.encodeBinaryFields(smsMessage, 
							AttributeManager.isAFields, 
							AttributeManager.ABBREV_ISA);
					smsMessage = AttributeManager.encodeBinaryFields(smsMessage, 
							AttributeManager.hasAFields,
							AttributeManager.ABBREV_HASA);
				}

				// Add a header (length and status) to message
				msgHeader = "MsgId:" + msg_id + ", Len:" + smsMessage.length() +  ", " + statusStr 
				+ " Bid = " + beneficiary_id;

				acdiVocaMsgs.add(new AcdiVocaMessage(msg_id, 
						beneficiary_id, 
						MESSAGE_STATUS_UNSENT,
						rawMessage, smsMessage, msgHeader,!AcdiVocaMessage.EXISTING));
				c.moveToNext();
				++k;
			}
		}
		mDb.close();
		c.close();
		return acdiVocaMsgs;		
	}

	public int clearBeneficiaryTable() {
		int rows = mDb.delete(FINDS_TABLE, null, null);
		mDb.close();
		return rows;
	}
	
	/**
	 * Creates an array list of messages each of which consists of a list of the
	 * dossier numbers of beneficiaries who did not show up at the distribution event.
	 * Beneficiaries who did show up and who have changes are updated with individual
	 * messages.  Beneficiaries who showed up but there was no change are not processed.
	 * Their status is deduced on the server by process of elimination. 
	 * @param distrCtr
	 * @return
	 */
	public ArrayList<AcdiVocaMessage> createBulkUpdateMessages(String distrCtr) {
		ArrayList<AcdiVocaMessage> acdiVocaMsgs = new ArrayList<AcdiVocaMessage>();

		Cursor c = mDb.query(FINDS_TABLE, null, 
				FINDS_STATUS + " = " + FINDS_STATUS_UPDATE  
				+ " AND " 
				+ FINDS_MESSAGE_STATUS + " = " + MESSAGE_STATUS_UNSENT
				+ " AND " 
				+ FINDS_DISTRIBUTION_POST + " = " + "'" + distrCtr  + "'"
				+ " AND " 
				+ FINDS_Q_PRESENT + "=" +  "'" + FINDS_FALSE + "'"
				,
				null, null, null, null);

		Log.i(TAG,"fetchBulkUpdateMessages " +  " count=" + c.getCount() + " distrPost " + distrCtr);
		
		if (c.getCount() != 0) {
			
			String smsMessage = "";
			c.moveToFirst();
			
			int k = 0;

			while (!c.isAfterLast()) {
				
//				// For debugging
//				String[] columns = c.getColumnNames();   // The Db columns represent the attribute names
//				for (int j = 0; j < columns.length; j++) 
//					Log.i(TAG, columns[j] + "=" + c.getString(c.getColumnIndex(columns[j])));
				
				String dossier_no = c.getString(c.getColumnIndex(FINDS_DOSSIER));
				smsMessage += dossier_no + AttributeManager.LIST_SEPARATOR;

				if (smsMessage.length() > 120) {
					// Add a header (length and status) to message
					String msgHeader = "MsgId: bulk, Len:" + smsMessage.length();

					acdiVocaMsgs.add(new AcdiVocaMessage(UNKNOWN_ID, 
							UNKNOWN_ID, 
							MESSAGE_STATUS_UNSENT,
							"", smsMessage, msgHeader, 
							!AcdiVocaMessage.EXISTING));
					smsMessage = "";
					
				}				
				c.moveToNext();
				++k;
			}
			acdiVocaMsgs.add (new AcdiVocaMessage(UNKNOWN_ID, UNKNOWN_ID, MESSAGE_STATUS_UNSENT,
					"", smsMessage, 
					"MsgId: bulk, Len:" + smsMessage.length(), 
					!AcdiVocaMessage.EXISTING));
		}
		mDb.close();
		c.close();

		return acdiVocaMsgs;		
	}

	
	/** 
	 * Returns an array of Strings where each String represents an SMS
	 * message for a Beneficiary.
	 * @param filter a int that selects messages by status
	 * @return an array of SMS strings
	 */
	public ArrayList<AcdiVocaMessage> fetchSmsMessages(int filter, int bene_status, String order_by) {
		Cursor c = lookupMessages(filter, bene_status, order_by);
		Log.i(TAG,"fetchSmsMessages " +  " count=" + c.getCount() + " filter= " + filter);

		// Construct the messages and store in a String array
		ArrayList<AcdiVocaMessage> acdiVocaMsgs = new ArrayList<AcdiVocaMessage>();
		if (c.getCount() != 0) {
			
			//acdiVocaMsgs = new ArrayList<AcdiVocaMessage>();
			c.moveToFirst();
			
			int k = 0;

			while (!c.isAfterLast()) {
				
				// For debugging
//				String[] columns = c.getColumnNames();   // The Db columns represent the attribute names
//				for (int j = 0; j < columns.length; j++) 
//					Log.i(TAG, columns[j] + "=" + c.getString(c.getColumnIndex(columns[j])));
				
				
				int msg_id = c.getInt(c.getColumnIndex(MESSAGE_ID));
				int beneficiary_id = c.getInt(c.getColumnIndex(MESSAGE_BENEFICIARY_ID));
				int msg_status = c.getInt(c.getColumnIndex(MESSAGE_STATUS));
				String smsMessage = c.getString(c.getColumnIndex(MESSAGE_TEXT));
				String statusStr = MESSAGE_STATUS_STRINGS[msg_status];

				String msgHeader = "MsgId:" + msg_id + " Stat:" + statusStr + " Len:" + smsMessage.length()
					+ " Bid = " + beneficiary_id;
				acdiVocaMsgs.add (new AcdiVocaMessage(msg_id, beneficiary_id, 
						msg_status,
						"", smsMessage, 
						msgHeader,
						AcdiVocaMessage.EXISTING));
				c.moveToNext();
				++k;
			}
		}
		mDb.close();
		c.close();
		return acdiVocaMsgs;
	}
	
	
	/**
	 * Converts a String representing a beneficiary string into an abbreviated
	 * string. If the String already contains an SMS message in its 'message_text'
	 * field, then no need to construct it again.
	 * @param beneficiary a string of the form attr1-value1,attr2=value2...
	 * @return a String of the form a1=v1, ..., aN=vN
	 */
	private String abbreviateBeneficiaryStringForSms(String beneficiary) {
		String message = "";
		String abbrev = "";
		String[] pair = null;
		
		String[] attr_val_pairs = beneficiary.split(",");
		String attr = "";
		String val = "";
		for (int k = 0; k < attr_val_pairs.length; k++) {
			//Log.i(TAG, "Pair-" + k + " = " + attr_val_pairs[k]);
			pair = attr_val_pairs[k].split("=");
			if (pair.length == 0) {
				attr = "";
				val = "";
			} else if (pair.length == 1) {
				attr = pair[0].trim();
				val = "";
			} else {
				attr = pair[0].trim();
				val = pair[1].trim();
			}

			if (!attr.equals(FINDS_ID) 
					&& !attr.equals(FINDS_PROJECT_ID) 
					//&& !attr.equals(FINDS_MESSAGE_TEXT) 
					&& !val.equals("null")
					) {
				if(attr.equals(FINDS_DOB))
					abbrev = AttributeManager.convertAttrValPairToAbbrev(attr, adjustDateForSmsReader(val));	
				else
					abbrev = AttributeManager.convertAttrValPairToAbbrev(attr, val);
				//abbrev = AttributeManager.convertAttrValPairToAbbrev(attr, val);
				if (!abbrev.equals(""))
					message += abbrev + ",";
			}
		}
		return message;
	}
	

	/** 
	 * Returns an array of dossier numbers for all beneficiaries.
	 * @distribSite the Distribution Site of the beneficiaries
	 * @return an array of N strings or null if no beneficiaries are found
	 */
	public String[] fetchAllBeneficiaryIdsByDistributionSite(String distribSite) {
		//		mDb = getReadableDatabase(); // Either open or create the DB.
		Cursor c = mDb.query(FINDS_TABLE,null, 
				FINDS_DISTRIBUTION_POST + "=" + "'" + distribSite + "'" , null, null, null,null);
		Log.i(TAG,"fetchAllBeneficiaryIds count=" + c.getCount());

		mDb.close();
		String dossiers[] = null;
		if (c.getCount() != 0) {
			dossiers = new String[c.getCount()];
			c.moveToFirst();
			int k = 0;
			while (!c.isAfterLast()) {
				dossiers[k] = c.getString(c.getColumnIndex(AcdiVocaDbHelper.FINDS_DOSSIER));
				c.moveToNext();
				++k;
			}
		}
		c.close();
		return dossiers;
	}


	/**
	 * Returns selected columns for a find by id.
	 * @param id the Find's id
	 * @param columns an array of column names, can be left null
	 * @return
	 */
	public ContentValues fetchFindDataById(long id, String[] columns) {
		//		mDb = getReadableDatabase();  // Either open or create the DB    	

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

			if (DBG && !column.equals(USER_PASSWORD)) 
				Log.i(TAG, "getContentValuesFromRow, Column " + column + " = " + 
					c.getString(c.getColumnIndexOrThrow(column)));
			values.put(column, c.getString(c.getColumnIndexOrThrow(column)));
		}
		c.close();
		return values;
	}

}
