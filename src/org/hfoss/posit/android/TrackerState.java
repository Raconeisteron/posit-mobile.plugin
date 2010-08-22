/*
 * File: TrackerState.java
 * 
 * Copyright (C) 2010 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Search and Identification Tool.
 *
 * POSIT is free software; you can redistribute it and/or modify
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
package org.hfoss.posit.android;

import java.util.ArrayList;
import java.util.List;

import com.google.android.maps.GeoPoint;

import android.location.Location;

/**
 * A class to encapsulate Tracker state data
 * @author rmorelli
 *
 */
public class TrackerState {

	public static final String SHARED_STATE = "TrackerState";
	public static final String NO_PROVIDER = "No location service";
	
	private static final int DEFAULT_SWATH_WIDTH = 50;  // 50 meters
	private static final int DEFAULT_MIN_DISTANCE = 10;  // 10 meters

	private static final String PROVIDER = "gps";
	
	public static final int IDLE = 0;
	public static final int RUNNING = 1;
	public static final int PAUSED = 2;
	
	public int mProjId;
	public int mExpeditionNumber = -1;  // Indicates that an expedition number has not been set
	
	public int mPoints = 0;
	public Location mLocation;

	public int mSwath = DEFAULT_SWATH_WIDTH;
	public int mMinDistance = DEFAULT_MIN_DISTANCE;  // meters
	
	private List<PointAndTime> pointsAndTimes;

	
	public TrackerState() {
		pointsAndTimes = new ArrayList<PointAndTime>();
	}  
	
	public synchronized void addGeoPoint(GeoPoint geoPoint) {
		pointsAndTimes.add(new PointAndTime(geoPoint, System
				.currentTimeMillis()));
	}
	
	public List<PointAndTime> getPoints() {
		return pointsAndTimes;
	}
	
	public void setPoints(List<PointAndTime> points) {
		pointsAndTimes = points;
	}
	
	/**
	 * Helper class to store a geopoint and its time stamp.
	 * @author rmorelli
	 *
	 */
	public class PointAndTime {
		private GeoPoint geoPoint;
		private long time;

		public PointAndTime(GeoPoint geoPoint, long time) {
			this.geoPoint = geoPoint;
			this.time = time;
		}

		public GeoPoint getGeoPoint() {
			return geoPoint;
		}

		public long getTime() {
			return time;
		}

	}
}
