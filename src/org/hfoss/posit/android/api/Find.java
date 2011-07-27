package org.hfoss.posit.android.api;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;

import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaDbHelper;

import com.j256.ormlite.field.DatabaseField;

import android.content.Context;

/**
 * Represents a specific find for a project, with a unique identifier
 * 
 */
public class Find implements FindInterface {
	
	// Db Column names
	public static final String ORM_ID = "id";
	public static final String GUID = "guid";
	public static final String NAME = "name";
	
	public static final String DESCRIPTION = "description";
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
	public static final String TIME = "timestamp";
	public static final String MODIFY_TIME = "modify_time";
	public static final String SYNCED = "synced";
	public static final String REVISION = "revision";
	public static final String IS_ADHOC = "is_adhoc";
	public static final String ACTION = "action";
	public static final String DELETED = "deleted";
	public static final int IS_SYNCED = 1;
	public static final int NOT_SYNCED = 0;
	
	// Instance variables, automatically mapped to DB columns
	@DatabaseField(columnName = ORM_ID, generatedId = true)		protected int id;
	@DatabaseField(columnName = GUID)  							protected String guid;
	@DatabaseField(columnName = NAME)  							protected String name;
	@DatabaseField(columnName = LATITUDE)  						protected double latitude;
	@DatabaseField(columnName = LONGITUDE)  					protected double longitude;
	@DatabaseField(columnName = TIME, canBeNull=false)  		protected Date time = new Date();
	@DatabaseField(columnName = MODIFY_TIME)  					protected Date modify_time;
	@DatabaseField(columnName = REVISION)  						protected int revision;
	@DatabaseField(columnName = IS_ADHOC)  						protected int is_adhoc;
	@DatabaseField(columnName = ACTION)  						protected int action;
	@DatabaseField(columnName = DELETED)  						protected int deleted;
	
	protected Find(){}
	
	/**
	 * This constructor is used for a new Find. Its ID will be automatically created.
	 * @param context is the Activity
	 */
	public Find (Context context){}

	/**
	 * This constructor is used for an existing Find. Its id is used to retrieve it
	 * @param context is the Activity
	 * @param guid is a globally unique identifier, used by the server
	 *   and other devices
	 */
	public Find (Context context, int id) {
		
	}
	
	
	
	/**
	 * This constructor is used for an existing Find. The Find's id is automagically generated
	 * but not its GUID.
	 * @param context is the Activity
	 * @param guid is a globally unique identifier, used by the server
	 *   and other devices
	 */
	public Find (Context context, String guid){}

	public int getId() {
		return id;
	}

	public String getguId() {
		return guid;
	}

	public int insert() {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean update() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean delete() {
		// TODO Auto-generated method stub
		return false;
	}

	public void sync(String protocol) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(ORM_ID).append("=").append(id).append(",");
		sb.append(GUID).append("=").append(guid).append(",");
		sb.append(NAME).append("=").append(name).append(",");
		sb.append(LATITUDE).append("=").append(latitude).append(",");
		sb.append(LONGITUDE).append("=").append(longitude).append(",");
		if (time != null)
			sb.append(TIME).append("=").append(time.toString()).append(",");
		else 
			sb.append(TIME).append("=").append("").append(",");
		if (modify_time != null)
			sb.append(MODIFY_TIME).append("=").append(modify_time.toString()).append(",");
		else 
			sb.append(MODIFY_TIME).append("=").append("").append(",");	
		sb.append(REVISION).append("=").append(revision).append(",");
		sb.append(IS_ADHOC).append("=").append(is_adhoc).append(",");
		sb.append(ACTION).append("=").append(action).append(",");
		sb.append(DELETED).append("=").append(deleted).append(",");
		return sb.toString();
	}

	
//	/**
//	 * Return attr=val, ... for all non-static attributes using Reflection.
//	 * @return
//	 */
//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder(super.toString());
//		Field[] fields = this.getClass().getDeclaredFields();
//		for (Field field : fields) {
//			if (Modifier.isStatic(field.getModifiers()))  //  Skip static fields
//				continue;
//			try {
//				sb.append(", ").append(field.getName()).append("=").append(field.get(this));
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}		
//		}
//		return sb.toString();
//	}
	
}