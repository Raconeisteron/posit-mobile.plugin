package org.hfoss.posit.android.experimental.api;

import java.sql.SQLException;
import java.util.Date;

import android.util.Log;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class SyncHistory {
	
	public static final String TAG = "SyncHistory";

	public static final String ID = "id";
	public static final String TIME = "time";
	public static final String SERVER = "server";


	@DatabaseField(columnName = ID, generatedId = true)
	protected int id;
	@DatabaseField(columnName = TIME)
	protected Date time;
	@DatabaseField(columnName = SERVER)
	protected String server;
	
	public SyncHistory() {
		
	}
	
	public SyncHistory(String server) {
		time = new Date();
		this.server = server;
	}
	
	/**
	 * Creates the table for this class.
	 * 
	 * @param connectionSource
	 */
	public static void createTable(ConnectionSource connectionSource) {
		Log.i(TAG, "Creating SyncHistory table");
		try {
			TableUtils.createTable(connectionSource, SyncHistory.class);
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
	public String getServer() {
		return server;
	}
	public void setServer(String server) {
		this.server = server;
	}

	@Override
	public String toString() {
		return "SyncHistory [id=" + id + ", time=" + time + ", server=" + server + "]";
	}
}
