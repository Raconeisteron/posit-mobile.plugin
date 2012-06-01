/*
 * File: SyncServer.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool. 
 *
 * This code is free software; you can redistribute it and/or modify
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
package org.hfoss.posit.android.sync;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.hfoss.posit.android.Constants;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.FindHistory;
import org.hfoss.posit.android.api.SyncHistory;
import org.hfoss.posit.android.api.database.DbHelper;
import org.hfoss.posit.android.functionplugin.camera.Camera;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Synchronization class used to for sync processes with the server. The
 * core functionality of this class is to receive finds and send finds.
 * 
 * The server uses a different format for Finds in string format, thus it
 * is implemented differently here. In addition, this class can be used to
 * change the current project and retrieve the list of projects from
 * the server.
 * 
 * @author Andrew Matsusaka
 *
 */
public class SyncServer extends SyncMedium{
	public final int CONNECTION_TIMEOUT = 3000;
	public final int SOCKET_TIMEOUT = 5000;
	
	public final String RESULT_FAIL = "false";
	
	private final String SERVER_PREF = "serverKey";
	private final String PROJECT_PREF = "projectKey";
	private static final String TAG = "SyncServer";
	private static final String COLUMN_IMEI = "imei";
	
	//Additional columns for photo table
	private static final String COLUMN_GUID = "guid"; 				//guid of the find
	private static final String COLUMN_IDENTIFIER = "identifier"; 	//does not seem to be useful
	private static final String COLUMN_PROJECT_ID = "project_id"; 	//project id of the find
	private static final String COLUMN_MIME_TYPE = "mime_type"; 	//data type, in this case, "image/jpeg"
	private static final String COLUMN_DATA_FULL = "data_full"; 	//data for the image, takes Base64 string of image
	private static final String COLUMN_DATA_THUMBNAIL = "data_thumbnail"; //data for the image, take Base 64 string of image
	
	private String mServer;
	private String mImei;
	
	/**
	 * Constructor that accepts the Context and initializes all the necessary
	 * settings
	 * @param context - Context to be used for all sync purposes
	 */
	public SyncServer(Context context){
		mContext = context;
		initSettings();
	}
	
	/**
	 * Initializes the settings needed to sync with the server
	 */
	private void initSettings(){
		initPreferences();
		initTelephony();
		initAuthKey();
	}
	
	/**
	 * Initializes the server string and project ID
	 */
	private void initPreferences(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mServer = prefs.getString(SERVER_PREF, "");
		mProjectId = prefs.getInt(PROJECT_PREF, 0);
	}
	
	/**
	 * Initializes the imei id
	 */
	private void initTelephony(){
		TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		mImei = telephonyManager.getDeviceId();
	}
	
	/**
	 * Initializes the authorization key
	 */
	private void initAuthKey(){
		mAuthKey = Communicator.getAuthKey( mContext );
	}
	
	/**
	 * Sends a query to the server for a list of the current users projects.
	 * Returns the result in the form of a List of HashMaps
	 * @return List of HashMaps containing the project data
	 */
	public List<HashMap<String,Object>> getProjects(){
		ArrayList<HashMap<String, Object>> list = null;

		String url = mServer + "/api/listMyProjects?authKey=" + mAuthKey;

		String responseString = Communicator.doHTTPGET(url);
		Log.i(TAG, responseString);

		if (!responseString.contains("Error")) {
			try {
				list = (ArrayList<HashMap<String, Object>>) (new ResponseParser(responseString).parseList());
			} catch (JSONException e) {
				Log.i(TAG, "getProjects JSON exception " + e.getMessage());
				list = null;
			} catch (Exception e) {
				Log.i(TAG, "getProjects Exception " + e.getMessage());
				list = null;
			}
		}
		
		return list;
	}
	
	/**
	 * Extracts the list of project names from the List of HashMap project data
	 * @param projects - List of HashMap project data to extract data from
	 * @return List of strings of project names
	 */
	public List<String> getProjectStrings(List<HashMap<String,Object>> projects) {
		List<HashMap<String,Object>> projectList = new ArrayList<HashMap<String,Object>>();
		Iterator<HashMap<String, Object>> it 	 = projectList.iterator();
		ArrayList<String> projList 				 = new ArrayList<String>();
		
		while( it.hasNext() ) {
			HashMap<String,Object> next = it.next();
			projList.add((String)(next.get("name")));
		}
		return projList;
	}
	
	/**
	 * Accepts a HashMap of project data to set the new current project to
	 * @param newProject - HashMap of project data
	 * @return whether or not the switching of projects was successful
	 */
	public boolean setProject( HashMap<String,Object> newProject ){
		String projectId 		= (String) newProject.get("id");
		String projectName 		= (String) newProject.get("name");
		String projectPref  	= mContext.getString(R.string.projectPref);
		String projectNamePref 	= mContext.getString(R.string.projectNamePref);
		int id  				= Integer.parseInt(projectId);
		boolean success 		= true;

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
		int currentProjectId = sp.getInt(projectPref,0);
		
		if (id == currentProjectId){
			success = false;
		}
		else {
			Editor editor = sp.edit();

			editor.putInt(projectPref, id);
			editor.putString(projectNamePref, projectName);
			editor.commit();
		}
		
		return success;
	}
	
	/**
	 * Gives back a list of Find guids that are not synced with the server
	 * @return List of string guids that need to be synced
	 */
	public List<String> getFindsNeedingSync(){
		String serverFindsIds = getServerFindsNeedingSync();
		List<String> finds = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(serverFindsIds, ",");
		
		while(st.hasMoreElements()){
			finds.add(st.nextElement().toString());
		}
		
		return finds;
	}
	
	/**
	 * Gives back a String of Find guids that are not synced with the server
	 * @return String that is a comma separated list of Find guids
	 */
	private String getServerFindsNeedingSync() {
		String response = "";
		
		String url = mServer + "/api/getDeltaFindsIds?authKey=" + mAuthKey + "&imei=" + mImei + "&projectId=" + mProjectId;
		Log.i(TAG, "getDeltaFindsIds URL=" + url);

		try {
			response = Communicator.doHTTPGET(url);
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
		}
		Log.i(TAG, "serverFindsNeedingSync = " + response);

		return response;
	}
	
	/**
	 * Queries the server for the find data corresponding to the passed in guid
	 * @param guid - Guid of the find to get data for
	 * @return Find data in the form of a string
	 */
	public String retrieveRawFind( String guid ){
		String url = mServer + "/api/getFind?guid=" + guid + "&authKey=" + mAuthKey;
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("guid", guid));
		pairs.add(new BasicNameValuePair("imei", mImei));

		String responseString = Communicator.doHTTPPost(url, pairs);

		Log.i(TAG, "getRemoteFindById = " + responseString);
		
		return responseString;
	}

	/**
	 * Sends a find to the server
	 * @param find - find to be sent
	 * @return whether or not the sending was successful
	 */
	public boolean sendFind( Find find ){
		boolean success = false;
		String url = createActionBasedUrl( find );
		List<NameValuePair> pairs = getNameValuePairs(find);
		
		BasicNameValuePair pair = new BasicNameValuePair("imei", mImei);
		pairs.add(pair);
		
		success = transmitFind( find, url, pairs );
		
		if( success ){
			Log.i(TAG, "transmitFind synced find id: " + find.getId());
			DbHelper.getDbManager(mContext).updateStatus(find, Constants.SUCCEEDED);
		}
		else{
			Log.i(TAG, "transmitFind failed to sync find id: " + find.getId());
			DbHelper.getDbManager(mContext).updateStatus(find, Constants.FAILED);
		}
		
		transmitImage( find );
		
		DbHelper.releaseDbManager();
		return success;
	}
	
	/**
	 * Creates a url string based on the find's action (Create or Update)
	 * @param find - Find used to determine action url
	 * @return url containing action
	 */
	private String createActionBasedUrl( Find find ){
		String url = "";
		try {
			String action = find.getAction();

			if( action.equals( FindHistory.ACTION_CREATE ) ){
				url = mServer + "/api/createFind?authKey=" + mAuthKey;
			}
			else if( action.equals( FindHistory.ACTION_UPDATE ) ){
				url = mServer + "/api/updateFind?authKey=" + mAuthKey;
			}
			else{
				Log.e(TAG, "Find object does not contain an appropriate action: " + find);
			}
		} catch(NullPointerException e) {
			Log.e(TAG, "Shouldn't happen but Find is null");
			e.printStackTrace();
			return url;
		}

		return url;
	}
	
	/**
	 * Converts the data from a Find object into NameValuePairs
	 * @param find - find to have data extracted from
	 * @return List of NameValuePairs containing Find data
	 */
	private List<NameValuePair> getNameValuePairs(Find find) {
		List<NameValuePair> pairs = null;
		if (find.getClass().getName().equals(Find.class.getName())) {
			pairs = getNameValuePairs(find, find.getClass());
		}
		else {
			String extendedDataPairs = getNameValuePairs(find, find.getClass()).toString();
			pairs = getNameValuePairs(find, find.getClass().getSuperclass());
			pairs.add(new BasicNameValuePair("data", extendedDataPairs));
		}
		return pairs;
	}
	
	/**
	 * Returns a list on name/value pairs for the Find.  Should work for Plugin Finds as
	 * well as Basic Finds.  
	 * @param find
	 * @param clazz
	 * @return
	 */
	private List<NameValuePair> getNameValuePairs(Find find, Class clazz) {
		Field[] fields = clazz.getDeclaredFields();

		List<NameValuePair> nvp = new ArrayList<NameValuePair>();
		String methodName = "";
		String value = "";

		for (Field field : fields) {
//			Log.i(TAG, "class= " + clazz + " field = " + field);
			if (!Modifier.isFinal(field.getModifiers())) {
				String key = field.getName();
				methodName = "get" + key.substring(0, 1).toUpperCase() + key.substring(1);
				value = "";

				try {
					Class returnType = clazz.getDeclaredMethod(methodName, null).getReturnType();
					if (returnType.equals(String.class))
						value = (String) clazz.getDeclaredMethod(methodName, null).invoke(find, (Object[]) null);
					else if (returnType.equals(int.class))
						value = String.valueOf((Integer) clazz.getDeclaredMethod(methodName, null).invoke(find,
								(Object[]) null));
					else if (returnType.equals(double.class))
						value = String.valueOf((Double) clazz.getDeclaredMethod(methodName, null).invoke(find,
								(Object[]) null));
					else if (returnType.equals(boolean.class))
						value = String.valueOf((Boolean) clazz.getDeclaredMethod(methodName, null).invoke(find,
								(Object[]) null));

				} catch (IllegalArgumentException e) {
					Log.e(TAG, e + ": " + e.getMessage());
				} catch (SecurityException e) {
					Log.e(TAG, e + ": " + e.getMessage());
				} catch (IllegalAccessException e) {
					Log.e(TAG, e + ": " + e.getMessage());
				} catch (InvocationTargetException e) {
					Log.e(TAG, e + ": " + e.getMessage());
				} catch (NoSuchMethodException e) {
					Log.e(TAG, e + ": " + e.getMessage());
				}
				nvp.add(new BasicNameValuePair(key, value));
			}
		}
		return nvp;
	}
	
	/**
	 * Transmits a Find to the server using the passed in find, url and NameValuePairs
	 * @param find - find to be transmitted
	 * @param url - url to be used to transmit find
	 * @param pairs - NameValuePairs containing Find data
	 * @return whether or not the transmission was successful
	 */
	private boolean transmitFind( Find find, String url, List<NameValuePair> pairs ){
		boolean success = true;
		try {
			String responseString = Communicator.doHTTPPost(url, pairs);
			success = responseString.indexOf("True") != -1;
			DbHelper.getDbManager(mContext).updateStatus(find, Constants.TRANSACTING);
			DbHelper.getDbManager(mContext).updateSyncOperation(find, Constants.POSTING);
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
			Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();
			DbHelper.getDbManager(mContext).updateStatus(find, Constants.FAILED);
			success = false;
		}
		
		return success;
	}
	
	/**
	 * Transmits an image associated to the passed in Find
	 * @param find - Find containing the image to be transmitted
	 */
	private void transmitImage( Find find ){
		if(Camera.isPhotoSynced(find, mContext) == false){
			HashMap<String, String> sendMap = createImageMap( find );
			String url = mServer + "/api/attachPicture?authKey=" + mAuthKey;
			
			Communicator.doHTTPPost(url, sendMap);
	 	}
	}
	
	/**
	 * Creates a mapping of the find's image data
	 * @param find - Find containing image
	 * @return HashMap of image data
	 */
	private HashMap<String, String> createImageMap( Find find ){
		HashMap<String, String> sendMap = new HashMap<String, String>();
		
		sendMap.put(COLUMN_IMEI, mImei);
		sendMap.put(COLUMN_GUID, find.getGuid());
		sendMap.put(COLUMN_IDENTIFIER,Integer.toString(find.getId()));
		sendMap.put(COLUMN_PROJECT_ID,Integer.toString(find.getProject_id()));
		sendMap.put(COLUMN_MIME_TYPE, "image/jpeg");
		
		String fullPicStr = Camera.getPhotoAsString(find.getGuid(), mContext);
		String thumbPicStr = Camera.getPhotoThumbAsString(find.getGuid(), mContext);
		
		sendMap.put(COLUMN_DATA_FULL, fullPicStr);
		sendMap.put(COLUMN_DATA_THUMBNAIL, thumbPicStr);
		
		return sendMap;
	}
	
	/**
	 * Records the sync on the server as well as the current device
	 * @return whether or not the recordings were successful
	 */
	public boolean postSendTasks(){
		boolean success = true;
		
		success &= recordSyncOnServer();
		success &= recordSyncOnDevice();
		
		return success;
	}
	
	/**
	 * Records the sync on the server
	 * @return whether or not the recording was successful
	 */
	private boolean recordSyncOnServer() {
		String url = mServer + "/api/recordSync?authKey=" + mAuthKey + "&imei=" + mImei + "&projectId=" + mProjectId;
		Log.i(TAG, "recordSync URL=" + url);
		String responseString = "";

		try {
			responseString = Communicator.doHTTPGET(url);
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
			e.printStackTrace();
			return false;
		}
		Log.i(TAG, "HTTPGet recordSync response = " + responseString);
		return true;
	}
	
	/**
	 * Records the sync on the current device
	 * @return whether or not the recording was successful
	 */
	private boolean recordSyncOnDevice(){
		int success = 0;
		success = DbHelper.getDbManager(mContext).recordSync(new SyncHistory("idkwhatthisissupposedtobe"));
		return success != 0;
	}
	
	/**
	 * Converts the passed in raw find data into a Find object
	 * @param rawFind - Find data in string format
	 * @return Find object filled with data
	 */
	public Find convertRawToFind( String rawFind ){
		Find newFind = new Find();
		ContentValues cv = getCvFromRaw( rawFind );
		newFind.updateObject(cv);
		
		retrieveImage( rawFind );
		
		return newFind;
	}
	
	/**
	 * Extracts the Find data from the string in the form of ContentValues
	 * @param rawFind - find data to be extracted
	 * @return ContentValues containing find data
	 */
	private ContentValues getCvFromRaw( String rawFind ){
		ContentValues cv = new ContentValues();

		Log.i(TAG, "getRemoteFindById = " + rawFind);
		try {
			JSONObject jobj = new JSONObject(rawFind);
			String findJson = jobj.getString("find");
			JSONObject find = new JSONObject(findJson);
			
			fillCvWithBasicData( cv, find );
			fillCvWithExtendedData( cv, jobj );
			
			return cv;
		} catch (JSONException e) {
			Log.i(TAG, "JSONException " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			Log.i(TAG, "Exception " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Fills ContentValues with the basic data of a find using a JSONObject
	 * @param cv - ContentValues to be filled
	 * @param find - JSONObject containing basic find data
	 */
	private void fillCvWithBasicData( ContentValues cv, JSONObject find ){
		try{
			cv.put(Find.GUID, 			find.getString(Find.GUID)		);
			cv.put(Find.PROJECT_ID, 	find.getInt(Find.PROJECT_ID)	);
			cv.put(Find.NAME, 			find.getString(Find.NAME)		);			
			cv.put(Find.DESCRIPTION, 	find.getString(Find.DESCRIPTION));
			cv.put(Find.TIME, 			find.getString("add_time")		);
			cv.put(Find.TIME, 			find.getString("modify_time")	);
			cv.put(Find.LATITUDE, 		find.getDouble(Find.LATITUDE)	);
			cv.put(Find.LONGITUDE, 		find.getDouble(Find.LONGITUDE)	);
			cv.put(Find.REVISION, 		find.getInt(Find.REVISION)		);
		} catch(JSONException e) {
			Log.i(TAG, "JSONException " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Fills ContentValues with the extended data of a find using a JSONObject
	 * @param cv - ContentValues to be filled
	 * @param find - JSONObject containing extended find data
	 */
	private void fillCvWithExtendedData( ContentValues cv, JSONObject jobj ){
		try{
			if (jobj.has(Find.EXTENSION)) {
				String extradata = jobj.getString(Find.EXTENSION);		
				Log.i(TAG, "extradata = " + extradata);
				if ( !extradata.equals("null") )
					addExtraDataToContentValues(cv, extradata);
			}
		} catch(JSONException e) {
			Log.i(TAG, "JSONException " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * The data has the form: [attr=value, ...] or 'null'
	 * @param cv
	 * @param data
	 */
	private void addExtraDataToContentValues(ContentValues cv, String data) {
		Log.i(TAG, "data = " + data  + " " + data.length());
		if (data.equals("null")) 
			return;
		data = data.trim();
		data = data.substring(1,data.length()-1);
		StringTokenizer st = new StringTokenizer(data,",");
		while (st.hasMoreElements()) {
			String attrvalpair = (String) st.nextElement();
			String attr = attrvalpair.substring(0,attrvalpair.indexOf("="));
			attr = attr.trim();
			String val = attrvalpair.substring(attrvalpair.indexOf("=")+1);
			val = val.trim();
			Log.i(TAG, "Putting " + attr + "=" + val + " into CV");
			if (Integer.getInteger(val) != null)
				cv.put(attr, Integer.parseInt(val));
			else
				cv.put(attr, val);
		}
	}
	
	/**
	 * Uses the raw find data to extract the image ids, retrieve the images
	 * from the server and store them on the current device
	 * @param rawFind - raw find data containing image ids
	 */
	private void retrieveImage( String rawFind ){
		try{
			JSONObject jobj = new JSONObject(rawFind);
			if (jobj.has("images")) {
				String imageIds = jobj.getString("images");		
				Log.i(TAG, "imageIds = " + imageIds);
				
				String imageId = parseImageIds(imageIds); 
				
				if(imageId != null){
					if(getImageOnServer(imageId)){
						Log.i(TAG, "Successfully retrieved image.");
					}
					else{
						Log.i(TAG, "Failed to retrieve image.");
					}
				}
			}
		} catch(JSONException e ) {
			Log.i(TAG, "JSONException " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			Log.i(TAG, "Exception " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * The data has the form: ["1","2", ...] or '[]'
	 * @param data
	 * the list of image ids
	 * @return the last image id in the list or null
	 */
	private String parseImageIds(String data) {
		Log.i(TAG, "imageIdData = " + data  + " " + data.length());
		if (data.equals("[]")){ 
			return null;
		}
		data = cleanImageData( data );
		StringTokenizer st = new StringTokenizer(data,","); //in the form "123"
		String imgId = null; 								//only care about one image for this version of posit
		while (st.hasMoreElements()) {
			imgId = (String) st.nextElement();
			Log.i(TAG, "Is this with quotes: " + imgId);
			imgId = imgId.substring(1, imgId.indexOf('"',1)); // removes quotes. find the second quote in the string
			Log.i(TAG, "Is this without quotes: " + imgId);
		}
		Log.i(TAG, "Planning to fetch imageId " + imgId + " for a find");
		return imgId;
	}
	
	/**
	 * Cleans the image data string by trimming brackets and white space
	 * @param data - image data to be cleaned
	 * @return String containing the cleaned data
	 */
	private String cleanImageData(String data){
		String cleaned = data.trim();
		cleaned = cleaned.substring(1, cleaned.length()-1); //removing brackets
		return cleaned;
	}
	
	 /**
	 * Retrieve the specified image id from the server and save it to the phone
	 * @param imageId
	 * the id of the image to query
	 * @param context
	 * the application context
	 * @return true if successful, false otherwise
	 */
	private boolean getImageOnServer(String imageId) throws FileNotFoundException, IOException {
		//TODO: Communicator.getAuthKey(m_context) might be returning m_authKey
		//		There are other cases where this happens to, they could be cleaned up
		
		String imageUrl = mServer + "/api/getPicture?id=" + imageId + "&authKey=" + 
			Communicator.getAuthKey(mContext);

		
		HashMap<String, String> sendMap = createSendMap();
		String imageResponseString 		= Communicator.doHTTPPost(imageUrl, sendMap);

		return saveImageData( imageResponseString );
	 }
	
	/**
	 * Creates a HashMap containing the imei
	 * @return HashMap containing the imei
	 */
	private HashMap<String, String> createSendMap(){
		HashMap<String, String> sendMap = new HashMap<String, String>();
		sendMap.put(COLUMN_IMEI, Communicator.getIMEI(mContext));
		return sendMap;
	}
	
	/**
	 * Uses the image response string from server to extract an image and save it
	 * @param imageResponseString - string containing image data
	 * @return whether or not extract and saving the image was successful
	 */
	private boolean saveImageData( String imageResponseString ){
		boolean success = true;
		
		if (imageResponseString.equals(RESULT_FAIL)){
			success = false;
		}
		else{
			Log.i(TAG, "imageResponseString = " + imageResponseString);

			try {
				JSONObject jobj = new JSONObject(imageResponseString);
				String guid = jobj.getString(Find.GUID);
				String imgData = jobj.getString("data_full");
				
				Camera.savePhoto(guid, imgData, mContext);
			} catch (JSONException e) {
				Log.i(TAG, "Unable to save image data.");
				e.printStackTrace();
				success = false;
			}
		}
		
		return success;
	}
	
}
