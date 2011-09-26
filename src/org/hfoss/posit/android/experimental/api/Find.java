package org.hfoss.posit.android.experimental.api;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.Date;

import org.hfoss.posit.android.experimental.plugin.acdivoca.AcdiVocaFind;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

/**
 * Represents a specific find for a project, with a unique identifier.
 * 
 */

public class Find implements FindInterface {

	public static final String TAG = "Find";

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
	@DatabaseField(columnName = ORM_ID, generatedId = true)
	protected int id;
	@DatabaseField(columnName = GUID)
	protected String guid;
	@DatabaseField(columnName = NAME)
	protected String name;
	@DatabaseField(columnName = DESCRIPTION)
	protected String description;
	@DatabaseField(columnName = LATITUDE)
	protected double latitude;
	@DatabaseField(columnName = LONGITUDE)
	protected double longitude;
	@DatabaseField(columnName = SYNCED)
	protected int synced;
	@DatabaseField(columnName = TIME, canBeNull = false)
	protected Date time = new Date();
	@DatabaseField(columnName = MODIFY_TIME)
	protected Date modify_time;
	@DatabaseField(columnName = REVISION)
	protected int revision;
	@DatabaseField(columnName = IS_ADHOC)
	protected int is_adhoc;
	@DatabaseField(columnName = ACTION)
	protected int action;
	@DatabaseField(columnName = DELETED)
	protected int deleted;

	/**
	 * Creates the table for this class.
	 * 
	 * @param connectionSource
	 */
	public static void createTable(ConnectionSource connectionSource) {
		Log.i(TAG, "Creating Finds table");
		try {
			TableUtils.createTable(connectionSource, Find.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Changed to "public"--objections?
	 */
	public Find() {
	}

	/**
	 * This constructor is used for a new Find. Its ID will be automatically
	 * created.
	 * 
	 * @param context
	 *            is the Activity
	 */
	public Find(Context context) {
	}

	/**
	 * This constructor is used for an existing Find. Its id is used to retrieve
	 * it
	 * 
	 * @param context
	 *            is the Activity
	 * @param guid
	 *            is a globally unique identifier, used by the server and other
	 *            devices
	 */
	public Find(Context context, int id) {

	}

	/**
	 * This constructor is used for an existing Find. The Find's id is
	 * automagically generated but not its GUID.
	 * 
	 * @param context
	 *            is the Activity
	 * @param guid
	 *            is a globally unique identifier, used by the server and other
	 *            devices
	 */
	public Find(Context context, String guid) {
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public int isSynced() {
		return synced;
	}

	public void setSynced(int synced) {
		this.synced = synced;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public Date getModify_time() {
		return modify_time;
	}

	public void setModify_time(Date modify_time) {
		this.modify_time = modify_time;
	}

	public int getRevision() {
		return revision;
	}

	public void setRevision(int revision) {
		this.revision = revision;
	}

	public int getIs_adhoc() {
		return is_adhoc;
	}

	public void setIs_adhoc(int is_adhoc) {
		this.is_adhoc = is_adhoc;
	}

	public int getAction() {
		return action;
	}

	public void setAction(int action) {
		this.action = action;
	}

	public int getDeleted() {
		return deleted;
	}

	public void setDeleted(int deleted) {
		this.deleted = deleted;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Inserts this find into the database.
	 * 
	 * @param dao
	 *            the DAO object provided by the ORMLite helper class.
	 * @return the number of rows inserted.
	 */

	public int insert(Dao<Find, Integer> dao) {
		int rows = 0;
		try {
			rows = dao.create(this);
			if (rows == 1)
				Log.i(TAG, "Inserted find:  " + this.toString());
			else {
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
	public int update(Dao<Find, Integer> dao) {
		int rows = 0;
		try {
			//updateObject(values);
			rows = dao.update(this);
			if (rows == 1)
				Log.i(TAG, "Updated find:  " + this.toString());
			else {
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
	public int delete(Dao<Find, Integer> dao) {
		int rows = 0;
		try {
			rows = dao.delete(this);
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

	public void sync(String protocol) {
		// TODO Auto-generated method stub

	}

	/**
	 * Uses reflection to copy data from a ContentValues object to this object.
	 * NOTE: This does not currently include any data from the superclass.
	 * Should it?
	 * 
	 * @param data
	 */
	private void updateObject(ContentValues data) {
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers())) // Skip static fields
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
				} else {
					String s = data.getAsString(fieldName);
					field.set(this, s);
					Log.i(TAG, "Set " + fieldName + "=" + s);
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassCastException e) {
				Log.e(TAG, "Class cast exception on " + fieldName + "=" + obj);
				e.printStackTrace();
			}
		}
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
			sb.append(MODIFY_TIME).append("=").append(modify_time.toString())
					.append(",");
		else
			sb.append(MODIFY_TIME).append("=").append("").append(",");
		sb.append(REVISION).append("=").append(revision).append(",");
		sb.append(IS_ADHOC).append("=").append(is_adhoc).append(",");
		sb.append(ACTION).append("=").append(action).append(",");
		sb.append(DELETED).append("=").append(deleted).append(",");
		return sb.toString();
	}

	// /**
	// * Return attr=val, ... for all non-static attributes using Reflection.
	// * @return
	// */
	// @Override
	// public String toString() {
	// StringBuilder sb = new StringBuilder(super.toString());
	// Field[] fields = this.getClass().getDeclaredFields();
	// for (Field field : fields) {
	// if (Modifier.isStatic(field.getModifiers())) // Skip static fields
	// continue;
	// try {
	// sb.append(", ").append(field.getName()).append("=").append(field.get(this));
	// } catch (IllegalArgumentException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IllegalAccessException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	// return sb.toString();
	// }

}