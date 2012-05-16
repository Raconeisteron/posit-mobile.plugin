/*
 * File: AsyncSendPointTask.java
 * 
 * Copyright (C) 2012 The Humanitarian FOSS Project (http://www.hfoss.org)
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

package org.hfoss.posit.android.functionplugin.tracker;

import java.util.ArrayList;
import java.util.List;

import org.hfoss.posit.android.sync.Communicator;

import android.app.Activity;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.util.Log;

/**
 * This class creates a new Thread to handle sending GPS points to the POSIT server.
 * 
 * The Task's parameter is an array of ContentValues containing data on the point(s)
 * to be synced to the server.  
 * 
 * The return result is a List of ContentValues containing the points that were
 * successfully synced.
 *  
 * @author rmorelli
 */
public class AsyncSendPointTask extends AsyncTask<ContentValues, Integer, List<ContentValues>>{

	public static final String TAG = "AsyncTask";
	private Communicator mCommunicator;
	private Activity mActivity;
	private int mExpNum;
	private TrackerState mTracker;
	
	/**
	 * Constructor sets up the environment.
	 * 
	 * @param activity,  the Activity that created this AsyncTask holds a reference
	 *  to the callback method called when the task completes.
	 *  
	 * @param tracker, records the tracker's state.  It is updated by the AsyncTask.
	 */
	public AsyncSendPointTask(Activity activity, TrackerState tracker) {
		mActivity = activity;
		mCommunicator = new Communicator();
		mTracker = tracker;
		mExpNum = tracker.mExpeditionNumber;
	}

	/**
	 * This is the background task. It takes an array of ContentValues
	 * and sends each point to the Posit server via the Communicator.
	 * 
	 * @param values, an array of ContentValues whose points will be sent
	 * @param a list of ContentValues containing those points that were synced
	 */
	@Override
	protected List<ContentValues> doInBackground(ContentValues... values) {

		String result;
		List<ContentValues> list = new ArrayList<ContentValues>();
		Log.i(TAG, "Starting Async task for exp = " + mExpNum + " nPoints = " + values.length);

		for (ContentValues vals : values) {

			// Send the point to the Server
			result = mCommunicator.registerExpeditionPoint(mActivity,
					vals.getAsDouble(Points.GPS_POINT_LATITUDE),
					vals.getAsDouble(Points.GPS_POINT_LONGITUDE), 
					vals.getAsDouble(Points.GPS_POINT_ALTITUDE), 
					vals.getAsInteger(Points.GPS_POINT_SWATH), 
					vals.getAsInteger(Points.EXPEDITION),
					vals.getAsLong(Points.GPS_TIME));

			// Successful result has the form mmm,nnnn where mmm = expediton_id
			if (result.indexOf(",") != -1 && result.substring(0, result.indexOf(",")).equals(""+ mExpNum)) {
				list.add(vals);
				mTracker.mSent++;
				Log.i(TAG, "nSent = " + list.size());
	            publishProgress(list.size());

			}
		}
		return list;
	}
	
	

	@Override
	protected void onCancelled() {
		super.onCancelled();
		((TrackerActivity)mActivity).setExecutionState(TrackerSettings.IDLE);	
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		((TrackerActivity)mActivity).asyncUpdate(values[0]);
	}

	/**
	 * When the AsyncTask completes, this method will pass thes result
	 * to the Activity's call back method.
	 */
	@Override
	protected void onPostExecute(List<ContentValues> result) {
		super.onPostExecute(result);
		((TrackerActivity)mActivity).asyncResult(result);
	}
}