package org.hfoss.posit.android.experimental.api;

import java.sql.SQLException;
import java.util.Date;

import android.util.Log;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class FindHistory {
	
	public static final String TAG = "FindHistory";

	public static final String ID = "id";
	public static final String TIME = "time";
	public static final String FIND = "find";
	public static final String FIND_ACTION = "find_action";
	
	public static final String ACTION_CREATE = "create";
	public static final String ACTION_UPDATE = "update";
	public static final String ACTION_DELETE = "delete";

	@DatabaseField(columnName = ID, generatedId = true)
	protected int id;
	@DatabaseField(columnName = TIME)
	protected Date time;
	@DatabaseField(columnName = FIND, foreign=true)
	protected Find find;
	@DatabaseField(columnName = FIND_ACTION)
	protected String findAction;

	public FindHistory() {

	}
	
	public FindHistory(Find find, String action){
		this.find = find;
		this.findAction = action;
		time = new Date();
	}

	/**
	 * Creates the table for this class.
	 * 
	 * @param connectionSource
	 */
	public static void createTable(ConnectionSource connectionSource) {
		Log.i(TAG, "Creating FindHistory table");
		try {
			TableUtils.createTable(connectionSource, FindHistory.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public Find getFind() {
		return find;
	}

	public void setFind(Find find) {
		this.find = find;
	}

	public String getFindAction() {
		return findAction;
	}

	public void setFindAction(String findAction) {
		this.findAction = findAction;
	}

	@Override
	public String toString() {
		return "FindHistory [id=" + id + ", time=" + time + ", find=" + find + ", findAction=" + findAction
				+ "]";
	}

}
