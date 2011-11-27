package org.hfoss.posit.android.experimental.plugin.outsidein;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.database.DbManager;

import android.content.ContentValues;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;
@DatabaseTable(tableName = DbManager.FIND_TABLE_NAME)
public class OutsideInFind extends Find {

	public static final String TAG = "OutsideInFind";
	public static final String SYRINGES_IN = "syringes_in";
	public static final String SYRINGES_OUT = "syringes_out";
	public static final String IS_NEW = "is_new";
	public static final String IS_LOGGED = "is_logged"
;
	@DatabaseField(columnName = SYRINGES_IN)
	protected int syringesIn;
	@DatabaseField(columnName = SYRINGES_OUT)
	protected int syringesOut;
	@DatabaseField(columnName = IS_NEW)
	protected boolean isNew;
	@DatabaseField(columnName = IS_LOGGED)
	protected boolean isLogged = false;  // Unlogged by default

	public OutsideInFind() {
		// Necessary by ormlite
	}
	
//	public OutsideInFind(ContentValues cv) {
//		super(cv);
//	}
	
	/**
	 * Creates the table for this class.
	 * 
	 * @param connectionSource
	 */
	public static void createTable(ConnectionSource connectionSource) {
		Log.i(TAG, "Creating OutsideinFind table");
		try {
			TableUtils.createTable(connectionSource, OutsideInFind.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public int getSyringesIn() {
		return syringesIn;
	}

	public void setSyringesIn(int syringesIn) {
		this.syringesIn = syringesIn;
	}

	public int getSyringesOut() {
		return syringesOut;
	}

	public void setSyringesOut(int syringesOut) {
		this.syringesOut = syringesOut;
	}

	public boolean getIsNew() {
		return isNew;
	}

	public void setIsNew(boolean isNew) {
		this.isNew = isNew;
	}
	
	public boolean getIsLogged() {
		return isLogged;
	}

	public void setIsLogged(boolean isLogged) {
		this.isLogged = isLogged;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(ORM_ID).append("=").append(id).append(",");
		sb.append(GUID).append("=").append(guid).append(",");
		sb.append(NAME).append("=").append(name).append(",");
		sb.append(PROJECT_ID).append("=").append(project_id).append(",");
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
		//sb.append(REVISION).append("=").append(revision).append(",");
		sb.append(IS_ADHOC).append("=").append(is_adhoc).append(",");
		//sb.append(ACTION).append("=").append(action).append(",");
		sb.append(DELETED).append("=").append(deleted).append(",");
		sb.append(SYRINGES_IN).append("=").append(syringesIn).append(",");
		sb.append(SYRINGES_OUT).append("=").append(syringesOut).append(",");
		sb.append(IS_NEW).append("=").append(isNew).append(",");
		sb.append(IS_LOGGED).append("=").append(isLogged).append(",");
		return sb.toString();
	}

}
