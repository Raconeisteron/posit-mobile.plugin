package org.hfoss.posit.android.experimental.plugin.clp;

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
public class ClpFind extends Find {

	public static final String TAG = "ClpInFind";

	public ClpFind() {
		// Necessary by ormlite
	}
	
	/**
	 * Creates the table for this class.
	 * 
	 * @param connectionSource
	 */
	public static void createTable(ConnectionSource connectionSource) {
		Log.i(TAG, "Creating OutsideinFind table");
		try {
			TableUtils.createTable(connectionSource, ClpFind.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
		return sb.toString();
	}

}
