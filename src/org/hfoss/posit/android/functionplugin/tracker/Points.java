package org.hfoss.posit.android.functionplugin.tracker;

/*
 * File: AcdiVocaDbUser.java
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

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.R;
//import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaUser;
//import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaUser.UserType;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class Points {

	/**
	 * The User object for creating and persisting data for the user table in
	 * the database.
	 */

	public static final String TAG = "Points";
	
	public static final String EXPEDITION_GPS_POINTS_TABLE = "points";
	public static final String EXPEDITION = "expedition";
	public static final String EXPEDITION_GPS_POINT_ROW_ID = "id";
	public static final String GPS_POINT_LATITUDE = "latitude";
	public static final String GPS_POINT_LONGITUDE = "longitude";
	public static final String GPS_POINT_ALTITUDE = "altitude";
	public static final String GPS_POINT_SWATH = "swath";
	public static final String GPS_TIME = "time";
	public static final String GPS_SYNCED = "synced";


	/**
	 * The fields annotated with @DatabaseField are persisted to the Db.
	 */
	// id is generated by the database and set on the object automatically
	@DatabaseField(generatedId = true)
	int id;

	@DatabaseField(columnName = EXPEDITION)
	int expedition;
	@DatabaseField(columnName = GPS_POINT_LATITUDE)
	String latitude;
	@DatabaseField(columnName = GPS_POINT_LONGITUDE)
	String longitude;	
	@DatabaseField(columnName = GPS_POINT_ALTITUDE)
	String altitude;
	@DatabaseField(columnName = GPS_POINT_SWATH)
	int swath;	
	@DatabaseField(columnName = GPS_TIME)
	long time;
	@DatabaseField(columnName = GPS_SYNCED)
	int synced;

	Points() {
		// needed by ormlite
	}

	public Points(int expedition, String latitude, String longitude, String altitude, int swath, long time, int synced) {
		this.expedition = expedition;
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.swath = swath;
		this.time = time;
		this.synced = synced;
	}
	
	public Points(ContentValues values) {
		expedition = values.getAsInteger(EXPEDITION);
		latitude = values.getAsString(GPS_POINT_LATITUDE);
		longitude = values.getAsString(GPS_POINT_LONGITUDE);
		altitude = values.getAsString(GPS_POINT_ALTITUDE);
		swath = values.getAsInteger(GPS_POINT_SWATH);
		time = values.getAsLong(GPS_TIME);
	}

	/**
	 * Creates the table associated with this object. And creates the default
	 * users. The table's name is 'user', same as the class name.
	 * 
	 * @param connectionSource
	 */
	public static void createTable(ConnectionSource connectionSource,
			Dao<Points, Integer> dao) {
		try {
			TableUtils.createTable(connectionSource, Points.class);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getSynced() {
		return synced;
	}

	public void setSynced(int synced) {
		this.synced = synced;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("id=").append(id);
		sb.append(", ").append("expedition=").append(expedition);
		sb.append(", ").append("latitude").append(latitude);
		sb.append(", ").append("longitude").append(longitude);
		sb.append(", ").append("altitude").append(altitude);
		sb.append(", ").append("swath=").append(swath);
		sb.append(", ").append("time=").append(time);
		sb.append(", ").append("synced=").append(synced);
		return sb.toString();
	}
}
