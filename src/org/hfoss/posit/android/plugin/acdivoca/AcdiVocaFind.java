/*
 * File: AcdiVocaFind.java
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.plugin.acdivoca.AttributeManager;
//import org.hfoss.posit.android.provider.PositDbHelper;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * Represents an AcdiVoca Beneficiary. Objects of this class are automatically
 * persisted to the Db.  
 * 
 */

public class AcdiVocaFind extends Find {
	private static final String TAG = "AcdiVocaFind";
	
	public static final String DOSSIER = AttributeManager.FINDS_DOSSIER;
	public static final String PROJECT_ID = "project_id";
	public static final String NAME = "name";
	
	public static final String TYPE = AttributeManager.FINDS_TYPE;    
	public static final int TYPE_MCHN = 0;
	public static final int TYPE_AGRI = 1;
	public static final int TYPE_BOTH = 2;
	public static final String[] FIND_TYPE_STRINGS = {"MCHN", "AGRI", "BOTH"};  // For display purpose

	public static final String STATUS = AttributeManager.FINDS_STATUS;
	public static final int STATUS_NEW = 0;      // New registration, no Dossier ID
	public static final int STATUS_UPDATE = 1;   // Update, imported from TBS, with Dossier ID
	public static final int STATUS_DONTCARE = -1;  
	public static final String[] FIND_STATUS_STRINGS = {"New", "Update"};  // For display purpose

	public static final String MESSAGE_ID = AttributeManager.FINDS_MESSAGE_ID;
	public static final String MESSAGE_STATUS = AttributeManager.FINDS_MESSAGE_STATUS;

	public static final String FIRSTNAME = AttributeManager.FINDS_FIRSTNAME;
	public static final String LASTNAME = AttributeManager.FINDS_LASTNAME;

	public static final String ADDRESS = AttributeManager.FINDS_ADDRESS;
	public static final String DOB = AttributeManager.FINDS_DOB;
	public static final String SEX = AttributeManager.FINDS_SEX;
	public static final String AGE = "age";

	public static final String BENEFICIARY_CATEGORY = AttributeManager.FINDS_BENEFICIARY_CATEGORY;
	public static final String HOUSEHOLD_SIZE = AttributeManager.FINDS_HOUSEHOLD_SIZE;

	public static final String DISTRIBUTION_POST = AttributeManager.FINDS_DISTRIBUTION_POST;
	public static final String Q_MOTHER_LEADER = AttributeManager.FINDS_Q_MOTHER_LEADER; // "mother_leader";
	public static final String Q_VISIT_MOTHER_LEADER = AttributeManager.FINDS_Q_VISIT_MOTHER_LEADER; // "visit_mother_leader";
	public static final String Q_PARTICIPATING_AGRI = AttributeManager.FINDS_Q_PARTICIPATING_AGRI; // "pariticipating_agri";
	public static final String Q_RELATIVE_AGRI = AttributeManager.FINDS_Q_RELATIVE_AGRI; // "pariticipating_agri";
	public static final String Q_PARTICIPATING_BENE = AttributeManager.FINDS_Q_PARTICIPATING_BENE; // "pariticipating_agri";
	public static final String Q_RELATIVE_BENE = AttributeManager.FINDS_Q_RELATIVE_BENE; // "pariticipating_agri";
	
	public static final String NAME_AGRI_PARTICIPANT = AttributeManager.FINDS_NAME_AGRI_PARTICIPANT; // "name_agri_paricipant";

	public static final String ZERO = "0";
	public static final String ONE = "1";

	// For the agriculture registration form
	public static final String LAND_AMOUNT = AttributeManager.FINDS_LAND_AMOUNT; // "amount_of_land";	
	public static final String IS_FARMER = AttributeManager.FINDS_IS_FARMER; //  "is_farmer";
	public static final String IS_MUSO = AttributeManager.FINDS_IS_MUSO;  // "is_MUSO";
	public static final String IS_RANCHER = AttributeManager.FINDS_IS_RANCHER;  //  "is_rancher";
	public static final String IS_STOREOWN = AttributeManager.FINDS_IS_STOREOWN; //  "is_store_owner";
	public static final String IS_FISHER = AttributeManager.FINDS_IS_FISHER;  // "is_fisher";
	public static final String IS_OTHER = AttributeManager.FINDS_IS_OTHER;  // "is_other";
	public static final String IS_ARTISAN = AttributeManager.FINDS_IS_ARTISAN; // "is_artisan";
	
	public static final String HAVE_VEGE = AttributeManager.FINDS_HAVE_VEGE; //  "have_vege";
	public static final String HAVE_CEREAL = AttributeManager.FINDS_HAVE_CEREAL;  //  "have_cereal";
	public static final String HAVE_TUBER = AttributeManager.FINDS_HAVE_TUBER;  // "have_tuber";
	public static final String HAVE_TREE = AttributeManager.FINDS_HAVE_TREE; // "have_tree";
	public static final String HAVE_GRAFTING = AttributeManager.FINDS_HAVE_GRAFTING; // "have_grafting";
	public static final String HAVE_HOUE = AttributeManager.FINDS_HAVE_HOUE;  //  "have_houe";
	public static final String HAVE_PIOCHE = AttributeManager.FINDS_HAVE_PIOCHE;  // "have_pioche";
	public static final String HAVE_BROUETTE = AttributeManager.FINDS_HAVE_BROUETTE; // "have_brouette";
	public static final String HAVE_MACHETTE = AttributeManager.FINDS_HAVE_MACHETTE; //  "have_machette";
	public static final String HAVE_SERPETTE = AttributeManager.FINDS_HAVE_SERPETTE;  // "have_serpette";
	public static final String HAVE_PELLE = AttributeManager.FINDS_HAVE_PELLE;  // "have_pelle";
	public static final String HAVE_BARREAMINES = AttributeManager.FINDS_HAVE_BARREAMINES; // "have_barreamines";
	public static final String RELATIVE_1 = AttributeManager.FINDS_RELATIVE_1;  // "relative_1";
	public static final String RELATIVE_2 = AttributeManager.FINDS_RELATIVE_2;  // "relative_2";
	public static final String HAVE_COFFEE = AttributeManager.FINDS_HAVE_COFFEE; //  "have_vege";

	public static final String PARTNER_FAO = AttributeManager.FINDS_PARTNER_FAO;// "partner_fao";
	public static final String PARTNER_SAVE = AttributeManager.FINDS_PARTNER_SAVE;// "partner_save";
	public static final String PARTNER_CROSE = AttributeManager.FINDS_PARTNER_CROSE;// "partner_crose";
	public static final String PARTNER_PLAN = AttributeManager.FINDS_PARTNER_PLAN;// "partner_plan";
	public static final String PARTNER_MARDNR = AttributeManager.FINDS_PARTNER_MARDNR;// "partner_mardnr";
	public static final String PARTNER_OTHER = AttributeManager.FINDS_PARTNER_OTHER;// "partner_other";
	
	public static final String MALNOURISHED = AttributeManager.FINDS_MALNOURISHED;  // "MALNOURISHED";
	public static final String PREVENTION = AttributeManager.FINDS_PREVENTION;     // "PREVENTION";
	public static final String EXPECTING = AttributeManager.FINDS_EXPECTING;   // "EXPECTING";
	public static final String NURSING = AttributeManager.FINDS_NURSING;      // "NURSING";
	
    public static final String MALE = AttributeManager.FINDS_MALE;          // "MALE";
    public static final String FEMALE = AttributeManager.FINDS_FEMALE;        // "FEMALE";
    public static final String YES = AttributeManager.FINDS_YES;           // "YES";
    public static final String NO = AttributeManager.FINDS_NO;            // "NO";
    public static final String TRUE = AttributeManager.FINDS_TRUE;       // "TRUE";
    public static final String FALSE = AttributeManager.FINDS_FALSE;      // "FALSE";
    public static final String COMMUNE_SECTION = AttributeManager.LONG_COMMUNE_SECTION;
    
	public static final String Q_PRESENT = AttributeManager.FINDS_Q_PRESENT;   // "Present";
	public static final String Q_TRANSFER = AttributeManager.FINDS_Q_TRANSFER;   //"Transfer";
	public static final String Q_MODIFICATION = AttributeManager.FINDS_Q_MODIFICATIONS;  // "Modifications";
	public static final String MONTHS_REMAINING = "MonthsRemaining";
	public static final String Q_CHANGE = AttributeManager.FINDS_Q_CHANGE;   //"ChangeInStatus";   // Added to incorporated changes to beneficiary type
	public static final String CHANGE_TYPE = AttributeManager.FINDS_CHANGE_TYPE;   //"ChangeType";

	
	
	public static final String TEST_FIND = "id=0, address=null, category=null,"
		+ "changeType=null, communeSection=null, sex=null,"
		+ "distributionPost=null, dob=null, dossier=null, firstName=Ralph,"
		+ "relativeTwo=null, relativeOne=null, nameAgriParticipant=null,"
		+ "lastName=Morelli, householdSize=null, hasHoe=false,"
		+ "hasMachette=false, hasPelle=false, hasPick=false,"
		+ "hasSerpette=false, hasTree=false, hasTuber=false, hasVege=false,"
		+ "hasGrafting=false, id=0, isArtisan=false, isChanged=false,"
		+ "isDistributionPresent=false, isFarmer=false, isFisherman=false,"
		+ "isMotherLeader=false, isMuso=false, isOther=false,"
		+ "isParticipatingAgri=false, isParticipatingMchn=false,"
		+ "isPartnerCROSE=false, isPartnerFAO=false, isPartnerMARDNR=false,"
		+ "isPartnerOther=false, isPartnerPLAN=false, isPartnerSAVE=false,"
		+ "isRancher=false, isStoreOwner=false, landAmount=0,"
		+ "hasCoffee=false, messageId=0, messageStatus=0, hasCereal=false,"
		+ "projectId=0, hasBrouette=false, hasBarreamines=false,"
		+ "distributionMonthsRemaining=0, status=0, type=0,"
		+ "visitedByMotherLeader=false";
	
	// id is generated by the database and set on the object automagically
	@DatabaseField(columnName = DOSSIER) String dossier;
	@DatabaseField(columnName = TYPE) int type;
	@DatabaseField(columnName = STATUS) int status;
	@DatabaseField(columnName = MESSAGE_ID) int message_id;
	@DatabaseField(columnName = MESSAGE_STATUS) int message_status;
	@DatabaseField(columnName = FIRSTNAME) String firstname;
	@DatabaseField(columnName = LASTNAME) String lastname;
	@DatabaseField(columnName = ADDRESS) String address;
	@DatabaseField(columnName = DOB) String dob;
	@DatabaseField(columnName = SEX) String sex;
	@DatabaseField(columnName = HOUSEHOLD_SIZE) String household_size;
	@DatabaseField(columnName = BENEFICIARY_CATEGORY) String beneficiary_category;
	@DatabaseField(columnName = DISTRIBUTION_POST) String distribution_post;
	@DatabaseField(columnName = Q_MOTHER_LEADER) boolean mother_leader;
	@DatabaseField(columnName = Q_VISIT_MOTHER_LEADER) boolean visit_mother_leader;
	@DatabaseField(columnName = Q_PARTICIPATING_AGRI) boolean participating_agri;
	@DatabaseField(columnName = Q_PARTICIPATING_BENE) boolean participating_bene;
	@DatabaseField(columnName = IS_FARMER) boolean is_farmer;
	@DatabaseField(columnName = IS_MUSO) boolean is_MUSO;
	@DatabaseField(columnName = IS_RANCHER) boolean is_rancher;
	@DatabaseField(columnName = IS_STOREOWN) boolean is_store_owner;
	@DatabaseField(columnName = IS_FISHER) boolean is_fisher;
	@DatabaseField(columnName = IS_ARTISAN) boolean is_artisan;
	@DatabaseField(columnName = IS_OTHER) boolean is_other;
	@DatabaseField(columnName = LAND_AMOUNT) int amount_of_land;
	@DatabaseField(columnName = HAVE_VEGE) boolean have_vege;
	@DatabaseField(columnName = HAVE_TUBER) boolean have_tuber;
	@DatabaseField(columnName = HAVE_CEREAL) boolean have_cereal;
	@DatabaseField(columnName = HAVE_TREE) boolean have_tree;
	@DatabaseField(columnName = HAVE_GRAFTING) boolean have_grafting;
	@DatabaseField(columnName = HAVE_COFFEE) boolean have_coffee;
	@DatabaseField(columnName = PARTNER_FAO) boolean partner_fao;
	@DatabaseField(columnName = PARTNER_SAVE) boolean partner_save;
	@DatabaseField(columnName = PARTNER_CROSE) boolean partner_crose;
	@DatabaseField(columnName = PARTNER_PLAN) boolean partner_plan;
	@DatabaseField(columnName = PARTNER_MARDNR) boolean partner_mardnr;
	@DatabaseField(columnName = PARTNER_OTHER) boolean partner_other;
	@DatabaseField(columnName = COMMUNE_SECTION) String communeSection;
	@DatabaseField(columnName = HAVE_HOUE) boolean have_hoe;
	@DatabaseField(columnName = HAVE_PIOCHE) boolean have_pick;
	@DatabaseField(columnName = HAVE_BROUETTE) boolean have_wheelbarrow;
	@DatabaseField(columnName = HAVE_MACHETTE) boolean have_machete;
	@DatabaseField(columnName = HAVE_SERPETTE) boolean have_pruning_knife;
	@DatabaseField(columnName = HAVE_PELLE) boolean have_shovel;
	@DatabaseField(columnName = HAVE_BARREAMINES) boolean have_crowbar;
	@DatabaseField(columnName = RELATIVE_1) String relative_1;
	@DatabaseField(columnName = RELATIVE_2) String relative_2;
	@DatabaseField(columnName = Q_CHANGE) boolean ChangeInStatus;
	@DatabaseField(columnName = CHANGE_TYPE) String ChangeType;
	
	@DatabaseField(columnName = Q_PRESENT) boolean Present;
	@DatabaseField(columnName = MONTHS_REMAINING) int MonthsRemaining;
	@DatabaseField(columnName = NAME_AGRI_PARTICIPANT) String name_agri_participant;	
	
	public AcdiVocaFind () {
		// Necessary by ormlite
	}
		

	
	/**
	 * Intantiates a Find with known Id by retrieving from Db.
	 * @param context
	 * @param id
	 */
	public AcdiVocaFind (Context context, int id) {
		Log.i(TAG, "Fetch find = " + id);

		AcdiVocaDbHelper db = new AcdiVocaDbHelper(context);

		Dao<AcdiVocaFind, Integer> avFindDao = null;
		AcdiVocaFind avFind = null;
		try {
			avFindDao = db.getAcdiVocaFindDao();
			avFind = avFindDao.queryForId(id);
			Log.i(TAG, "Retrieved Find id = " + avFind.getId());
//			if (avFind != null)
				copyData(avFind);
		} catch (SQLException e) {
			Log.e(TAG, "SQL Exception " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Retrieves a Find object by its dossier number and instatiates from Db data.
	 * @param dossier the Find's dossier number
	 * @param columns an array of column names, can be left null
	 * @return
	 */
	public AcdiVocaFind(Context context, String dossier) {
		Log.i(TAG, "Fetching find, dossier = " + dossier);
		AcdiVocaDbHelper db = new AcdiVocaDbHelper(context);

		Dao<AcdiVocaFind, Integer> avFindDao = null;
		List<AcdiVocaFind> list = null;
		AcdiVocaFind avFind = null;
		try {
			avFindDao = db.getAcdiVocaFindDao();
			QueryBuilder<AcdiVocaFind, Integer> queryBuilder =
				avFindDao.queryBuilder();
			Where<AcdiVocaFind, Integer> where = queryBuilder.where();
			where.eq(DOSSIER, dossier);
			PreparedQuery<AcdiVocaFind> preparedQuery = queryBuilder.prepare();
			avFind = avFindDao.queryForFirst(preparedQuery);
			copyData(avFind);
		} catch (SQLException e) {
			Log.e(TAG, "SQL Exception " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Construct an instance from a collection of ContentValues.
	 * This method does not save the data to the Db.
	 * @param values
	 */
	public AcdiVocaFind(ContentValues values) {
		copyData(values);
	}
	
	
	/**
	 * Uses reflection to copy all data fields in both this from avFind to this object.
	 * Copied data include fields from this class and its superclass.
	 * @param avFind
	 */
	private void copyData(AcdiVocaFind avFind) {
		Field[] superFields = this.getClass().getSuperclass().getDeclaredFields();
		Field[] fields = this.getClass().getDeclaredFields();
		Field[] allFields = new Field[superFields.length + fields.length];
		for (int k = 0; k < superFields.length; k++)
			allFields[k] = superFields[k];
		for (int k = superFields.length; k < allFields.length; k++) 
			allFields[k] = fields[k-superFields.length];
		
		for (Field field : allFields) {
			if (Modifier.isStatic(field.getModifiers()))  // Just report non-static fields
				continue;
			try {
				field.setAccessible(true);
				field.set(this, field.get(avFind));
				Log.i(TAG, "reflect field = " + field.getName() + " = " + field.get(this));

			} catch (NullPointerException e) {   // data field may be null
				e.printStackTrace();
				
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}
	}
	
	/**
	 * Uses reflection to copy data from a ContentValues object to this object.
	 * NOTE: This does not currently include any data from the superclass.  Should it?
	 * @param data
	 */
	private void copyData(ContentValues data) {
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers()))  // Skip static fields
				continue;
			Object obj = null;
			String fieldName = null;
			try {
				fieldName = field.getName();
				obj = field.get(this);
				if (!data.containsKey(fieldName))
					continue;
				Log.i(TAG, "field: " + fieldName);
				if (obj instanceof String) {
					String s = data.getAsString(fieldName);
					field.set(this, s);
					Log.i(TAG, "Set " + fieldName + "=" + s);
				} else if (obj instanceof Boolean) {
					Log.i(TAG, "Boolean value: " + data.getAsString(fieldName));
					Boolean bVal = data.getAsBoolean(fieldName);
					boolean b = false;
					if (bVal != null)
						b = bVal;
					field.set(this, b);
					Log.i(TAG, "Set " + fieldName + "=" + b);
				} else if (obj instanceof Integer) {
					Integer iVal = data.getAsInteger(fieldName);
					int i = 0;
					if (iVal != null)
						i = iVal;
					field.set(this, i);
					Log.i(TAG, "Set " + fieldName + "=" + i);
				} else  {
					String s = data.getAsString(fieldName);
					field.set(this, s);
					Log.i(TAG, "Set " + fieldName + "=" + s);
				}
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassCastException e) {
				Log.e(TAG, "Class cast exception on " + fieldName + "="  + obj);
				e.printStackTrace();
			}
		}
	}
	
	
	public static void init(ConnectionSource connectionSource, Dao<AcdiVocaFind, Integer> acdiVocaFindDao) {
		try {
			TableUtils.createTable(connectionSource, AcdiVocaFind.class);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Inserts this Find's data into the Db.  Assumes that the Find
	 * has been instantiated with data. 
	 * @param avFind
	 * @return
	 */
	public boolean insert(Context context) {
		Log.i(TAG, "Inserting, id = " + id);
		AcdiVocaDbHelper db = new AcdiVocaDbHelper(context);
		Dao<AcdiVocaFind, Integer> avFindDao = null;
		int result = 0;
		try {
			avFindDao = db.getAcdiVocaFindDao();
			result = avFindDao.create(this);
		} catch (SQLException e) {
			Log.e(TAG, "SQL Exception " + e.getMessage());
			e.printStackTrace();
		}
		return result == 1;
	}
	
	
	/**
	 * Updates fields from ContentValues retrieved from View and 
	 * save the Data to the Db.
	 * @param values
	 */
	public boolean update(Context context, ContentValues values) {
		copyData(values);
		return update(context);
	}
	
	
	/**
	 * Updates this Find in the Db.  Assumes that the Find
	 * has been instantiated with data. 
	 * @param avFind
	 * @return
	 */
	public boolean update(Context context) {
		Log.i(TAG, "Updating, id " + id);
		AcdiVocaDbHelper db = new AcdiVocaDbHelper(context);

		Dao<AcdiVocaFind, Integer> avFindDao = null;
		int result = 0;
		try {
			avFindDao = db.getAcdiVocaFindDao();
			result = avFindDao.update(this);
		} catch (SQLException e) {
			Log.e(TAG, "SQL Exception " + e.getMessage());
			e.printStackTrace();
		}
		return result == 1;
	}
	
	
	/**
	 * Sets this object's attributes from attr/value pair string.
	 * @param attrValPairs
	 */
	public void init(String attrValPairs) {
		String[] pairs = attrValPairs.split(",");
		for (String pair:pairs) {
			String[] attr_val = pair.split("=");
			String attr = attr_val[0].trim();
			String val = attr_val[1].trim();
			Field f = null;
			try {
				f = getClass().getDeclaredField(attr);
				setField(f, val);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void setField(Field f, String val) {
		try {
			Class type = f.getType();
			Object o = f.get(this);
			if (o instanceof Boolean) 
				f.setBoolean(this, (Boolean) o);
			else if (o instanceof Integer) 
				f.setInt(this, (Integer) o);
			else if (o instanceof String) 
				f.set(this, o.toString());
			else 
				f.set(this, o);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the Beneficiary as a collection of ContentValues.
	 * @return
	 */
	public ContentValues toContentValues() {
		ContentValues values = new ContentValues();
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field field : fields) {
//			Log.i(TAG, "fieldname= " + field.getName());
			if (field.getName().equals("TEST_FIND"))
				continue;
			Object obj = null;
			String fieldName = null;
			try {
				fieldName = field.getName();
				obj = field.get(this);
				if (obj instanceof String) {
					values.put(fieldName, (String) obj);
				} else if (obj instanceof Boolean) {
					values.put(fieldName, (boolean) ((Boolean) obj));
				} else if (obj instanceof Integer) {
					values.put(fieldName, (Integer) obj);
				} else  // Ignore it, maybe an array?
//					values.put(fieldName, (String) obj);
				Log.i(TAG, "Field = " + fieldName + " value = " + obj);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassCastException e) {
				Log.e(TAG, "Class cast exception on " + fieldName + "="  + obj);
				e.printStackTrace();
			}
		}
		return values;
	}
	
	/**
	 * Returns a representation of this Find in a highly compressed
	 * Sms (Text) message.
	 * @return
	 */
	public AcdiVocaMessage toSmsMessage() {
		int msg_id = AcdiVocaDbHelper.UNKNOWN_ID;
		int beneficiary_id = id;   //AcdiVocaDbHelper.UNKNOWN_ID;
		int beneficiary_status = -1;
		int message_status = -1;
		String statusStr = "";
		
		String rawMessage = toString();
		String smsMessage = "";
		if (status == STATUS_UPDATE) 
			smsMessage = toSmsUpdateMessage();
		else if (status == STATUS_NEW && type == TYPE_MCHN)
			smsMessage = toSmsNewMchnMessage();
		else if (status == STATUS_NEW && type == TYPE_AGRI)
			smsMessage = toSmsNewAgriMessage();

		// Create a header (length and status) to message
		String msgHeader = "MsgId:" + msg_id + ", Len:" + smsMessage.length() +  ", " + statusStr 
		+ " Bid = " + beneficiary_id;

		return new AcdiVocaMessage(msg_id, 
				beneficiary_id, 
				AcdiVocaDbHelper.MESSAGE_STATUS_UNSENT,
				rawMessage, smsMessage, msgHeader,!AcdiVocaMessage.EXISTING);
	}
	
	/**
	 * Prepares the SMS for an update beneficiary (i.e., with DOSSIER number and distribution post).
	 * @return
	 */
	private String toSmsUpdateMessage() {
		StringBuilder sb = new StringBuilder();
		final String EQ = AttributeManager.ATTR_VAL_SEPARATOR;
		final String COMMA = AttributeManager.PAIRS_SEPARATOR;
		sb.append(AttributeManager.ABBREV_DOSSIER).append(EQ).append(dossier);
		sb.append(COMMA).append(AttributeManager.ABBREV_TYPE).append(EQ).append(type);
		sb.append(COMMA).append(AttributeManager.ABBREV_STATUS).append(EQ).append(status);
		sb.append(COMMA).append(AttributeManager.ABBREV_FIRST).append(EQ).append(firstname);
		sb.append(COMMA).append(AttributeManager.ABBREV_LAST).append(EQ).append(lastname);
		sb.append(COMMA).append(AttributeManager.ABBREV_LOCALITY).append(EQ).append(address);
		String adjDob = AcdiVocaDbHelper.adjustDateForSmsReader(dob);
		sb.append(COMMA).append(AttributeManager.ABBREV_DOB).append(EQ).append(adjDob);
		sb.append(COMMA).append(AttributeManager.ABBREV_SEX).append(EQ).append(AttributeManager.mapToShort(sex));
		sb.append(COMMA).append(AttributeManager.ABBREV_CATEGORY).append(EQ).append(AttributeManager.mapToShort(beneficiary_category));
		sb.append(COMMA).append(AttributeManager.ABBREV_DISTRIBUTION_POST).append(EQ).append(distribution_post);
		sb.append(COMMA).append(AttributeManager.ABBREV_CHANGE_TYPE).append(EQ).append(ChangeType);
		sb.append(COMMA).append(AttributeManager.ABBREV_Q_CHANGE).append(EQ).append(AttributeManager.mapToShort(ChangeInStatus));
		sb.append(COMMA).append(AttributeManager.ABBREV_MONTHS).append(EQ).append(MonthsRemaining);
		return sb.toString();
	}
	
	
	/**
	 * Prepares the SMS for an new MCHN beneficiary.
	 * @return
	 */
	private String toSmsNewMchnMessage() {
		StringBuilder sb = new StringBuilder();
		final String EQ = AttributeManager.ATTR_VAL_SEPARATOR;
		final String COMMA = AttributeManager.PAIRS_SEPARATOR;
//		sb.append(AttributeManager.ABBREV_DOSSIER).append(EQ).append(dossier);  // Note needed for New Mchn?
//		sb.append(COMMA).
		sb.append(AttributeManager.ABBREV_TYPE).append(EQ).append(type);
		sb.append(COMMA).append(AttributeManager.ABBREV_STATUS).append(EQ).append(status);
		sb.append(COMMA).append(AttributeManager.ABBREV_FIRST).append(EQ).append(firstname);
		sb.append(COMMA).append(AttributeManager.ABBREV_LAST).append(EQ).append(lastname);
		sb.append(COMMA).append(AttributeManager.ABBREV_LOCALITY).append(EQ).append(address);
		String adjDob = AcdiVocaDbHelper.adjustDateForSmsReader(dob);
		sb.append(COMMA).append(AttributeManager.ABBREV_DOB).append(EQ).append(adjDob);
		sb.append(COMMA).append(AttributeManager.ABBREV_SEX).append(EQ).append(AttributeManager.mapToShort(sex));
		sb.append(COMMA).append(AttributeManager.ABBREV_CATEGORY).append(EQ).append(AttributeManager.mapToShort(beneficiary_category));
		sb.append(COMMA).append(AttributeManager.ABBREV_DISTRIBUTION_POST).append(EQ).append(distribution_post);
		sb.append(COMMA).append(AttributeManager.ABBREV_NUMBER_IN_HOME).append(EQ).append(household_size);
		sb.append(COMMA).append(AttributeManager.ABBREV_IS_MOTHERLEADER).append(EQ).append(AttributeManager.mapToShort(mother_leader));
		sb.append(COMMA).append(AttributeManager.ABBREV_VISIT_MOTHERLEADER).append(EQ).append(AttributeManager.mapToShort(visit_mother_leader));
		sb.append(COMMA).append(AttributeManager.ABBREV_RELATIVE_1).append(EQ).append(relative_1);
		sb.append(COMMA).append(AttributeManager.ABBREV_PARTICIPATING_BENE).append(EQ).append(AttributeManager.mapToShort(participating_agri));
		sb.append(COMMA).append(AttributeManager.ABBREV_RELATIVE_2).append(EQ).append(relative_2);
		return sb.toString();
	}
	
	/**
	 * Prepares the SMS for an new Agri beneficiary.
	 * @return
	 */
	private String toSmsNewAgriMessage() {
		StringBuilder sb = new StringBuilder();
		final String EQ = AttributeManager.ATTR_VAL_SEPARATOR;
		final String COMMA = AttributeManager.PAIRS_SEPARATOR;
//		sb.append(AttributeManager.ABBREV_DOSSIER).append(EQ).append(dossier);
//		sb.append(COMMA).
		sb.append(AttributeManager.ABBREV_TYPE).append(EQ).append(type);
		sb.append(COMMA).append(AttributeManager.ABBREV_STATUS).append(EQ).append(status);
		sb.append(COMMA).append(AttributeManager.ABBREV_FIRST).append(EQ).append(firstname);
		sb.append(COMMA).append(AttributeManager.ABBREV_LAST).append(EQ).append(lastname);
		sb.append(COMMA).append(AttributeManager.ABBREV_LOCALITY).append(EQ).append(address);
		String adjDob = AcdiVocaDbHelper.adjustDateForSmsReader(dob);
		sb.append(COMMA).append(AttributeManager.ABBREV_DOB).append(EQ).append(adjDob);
		sb.append(COMMA).append(AttributeManager.ABBREV_SEX).append(EQ).append(AttributeManager.mapToShort(sex));
		sb.append(COMMA).append(AttributeManager.ABBREV_NUMBER_IN_HOME).append(EQ).append(household_size);
		sb.append(COMMA).append(AttributeManager.ABBREV_LAND_AMT).append(EQ).append(amount_of_land);	
//		sb.append(COMMA).append(AttributeManager.ABBREV_CATEGORY).append(EQ).append(beneficiary_category);
		sb.append(COMMA).append(AttributeManager.ABBREV_COMMUNE_SECTION).append(EQ).append(AttributeManager.mapToShort(communeSection));
		sb.append(COMMA).append(AttributeManager.ABBREV_PARTICIPATING_BENE).append(EQ).append(AttributeManager.mapToShort(participating_bene));
		sb.append(COMMA).append(AttributeManager.ABBREV_RELATIVE_2).append(EQ).append(relative_2);
		
		
		String isAFields = AttributeManager.encodeBinaryFields(getIsOrHasOrPartnerFields("is"), 
				AttributeManager.isAFields, 
				AttributeManager.ABBREV_ISA);
		String hasAFields = AttributeManager.encodeBinaryFields(getIsOrHasOrPartnerFields("have"), 
				AttributeManager.hasAFields,
				AttributeManager.ABBREV_HASA);
		sb.append(COMMA).append(isAFields);
		sb.append(COMMA).append(hasAFields);
		
		return sb.toString();
	}
	
	
	private String getIsOrHasOrPartnerFields(String prefix) {
		StringBuilder sb = new StringBuilder();
		Field[] fields = this.getClass().getDeclaredFields();
		final String COMMA = AttributeManager.PAIRS_SEPARATOR;
		final String EQ = AttributeManager.ATTR_VAL_SEPARATOR;
		
		for (Field field : fields) {
			if (field.getName().startsWith(prefix)) {
				String abbrev = AttributeManager.mapToShort(field.getName());
				try {
					sb.append(COMMA).append(abbrev).append(EQ).append(field.get(this));
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}		
			}
		}
		return sb.toString();
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

            if (!attr.equals(ORM_ID) 
                    && !attr.equals(PROJECT_ID) 
                    //&& !attr.equals(FINDS_MESSAGE_TEXT) 
                    && !val.equals("null")
                    ) {
                if(attr.equals(DOB))
                    abbrev = AttributeManager.convertAttrValPairToAbbrev(attr, AcdiVocaDbHelper.adjustDateForSmsReader(val));
                if(attr.equals(DISTRIBUTION_POST)) //new code to manage codes already in DB
                    abbrev = AttributeManager.getMapping(DISTRIBUTION_POST) + AttributeManager.ATTR_VAL_SEPARATOR + val;
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
	 * Updates Beneficiary table for processed message.
	 * @param beneficiary_id
	 * @param msg_id
	 * @param msgStatus
	 * @return
	 */
	public static boolean updateBeneficiaryMessageStatus(Context context, int beneficiary_id, int msg_id, int msgStatus) {
		Log.i(TAG, "Updating beneficiary = " + beneficiary_id + " for message " + msg_id + " status=" + msgStatus);

		AcdiVocaDbHelper db = new AcdiVocaDbHelper(context);
		Dao<AcdiVocaFind, Integer> avFindDao = null;
		AcdiVocaFind avFind = null;
		int result = 0;
		try {
			avFindDao = db.getAcdiVocaFindDao();
			avFind = avFindDao.queryForId(beneficiary_id);  // Retrieve the beneficiary
			if (avFind != null) {
				avFind.message_status = msgStatus;
				avFind.message_id = msg_id;
				result = avFindDao.update(avFind);
			} else {
				Log.e(TAG, "Unable to retrieve beneficiary id = " + beneficiary_id ); 
			}
		} catch (SQLException e) {
			Log.e(TAG, "SQL Exception " + e.getMessage());
			e.printStackTrace();
		}
		if (result == 1) 
			Log.d(TAG, "Updated beneficiary id = " + beneficiary_id + " for message " + msg_id + " status=" + msgStatus); 
		return result == 1;
	}
	
	/**
	 * Return attr=val, ... for all non-static attributes using Reflection.
	 * @return
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers()))  // Just report non-static fields
				continue;
			try {
				sb.append(", ").append(field.getName()).append("=").append(field.get(this));
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}
		return sb.toString();
	}
	
}