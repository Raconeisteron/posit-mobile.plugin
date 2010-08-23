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

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;

/**
 * A class to encapsulate Tracker state data
 * @author rmorelli
 *
 */
public class TrackerState {

	public static final String TAG = "PositTacker";
	
	public static final String BUNDLE_NAME = "TrackerState";
	public static final String BUNDLE_PROJECT = "ProjectId";
	public static final String BUNDLE_SWATH = "Swath";
	public static final String BUNDLE_MIN_DISTANCE = "MinDistance";
	public static final String BUNDLE_EXPEDITION = "Expedition";
		
	public int mProjId = -1;			// No project id assigned 
	public int mExpeditionNumber = -1;  // No expedition number assigned yet
	public int mPoints = 0;

	public int mSwath = TrackerSettings.DEFAULT_SWATH_WIDTH;
	public int mMinDistance = TrackerSettings.DEFAULT_MIN_RECORDING_DISTANCE;  // meters
	
	private List<PointAndTime> pointsAndTimes;
	public Location mLocation;
	
	/**
	 * Default constructor
	 */
	public TrackerState() {
		pointsAndTimes = new ArrayList<PointAndTime>();
	}  
	
	/**
	 * Construct from a Bundle.  The Bundle contains only some of the elements
	 * of the track's state -- e.g., no points
	 * @param b
	 */
	public TrackerState(Bundle b) {
		mProjId = b.getInt(BUNDLE_PROJECT);
		mExpeditionNumber = b.getInt(BUNDLE_EXPEDITION);
		mSwath = b.getInt(BUNDLE_SWATH);
		mMinDistance = b.getInt(BUNDLE_MIN_DISTANCE);
		pointsAndTimes = new ArrayList<PointAndTime>();
	}
	
	/**
	 * Returns a Bundle of some of the elements of the tracker's state. Useful for
	 * making Intents.
	 * 
	 * @return
	 */
	public Bundle bundle() {
		Bundle b = new Bundle();
		b.putInt(BUNDLE_PROJECT, mProjId);
		b.putInt(BUNDLE_MIN_DISTANCE, mMinDistance);
		b.putInt(BUNDLE_EXPEDITION, mExpeditionNumber);
		b.putInt(BUNDLE_SWATH, mSwath);
		return b;
	}
	
	/**
	 * Updates attributes that are settable by the user.  This could be expanded.
	 * 
	 * @param preferenceKey the settable attribute
	 */
	public void updatePreference (SharedPreferences sp, String preferenceKey) {
		if (preferenceKey.equals(TrackerSettings.MINIMUM_DISTANCE_PREFERENCE))
			mMinDistance = Integer.parseInt(
					sp.getString(preferenceKey,
							""+TrackerSettings.DEFAULT_MIN_RECORDING_DISTANCE));
		if (preferenceKey.equals(TrackerSettings.SWATH_PREFERENCE))
			mSwath = Integer.parseInt(
					sp.getString(preferenceKey, 
							""+TrackerSettings.DEFAULT_SWATH_WIDTH));
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
