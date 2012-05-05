/*
 * File: Communicator.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
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
package org.hfoss.posit.android.sync;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.hfoss.posit.android.Constants;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.FindHistory;
import org.hfoss.posit.android.api.activity.ListProjectsActivity;
import org.hfoss.posit.android.api.authentication.AuthenticatorActivity;
import org.hfoss.posit.android.api.database.DbHelper;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.functionplugin.camera.Camera;
import org.hfoss.posit.android.R;
//import org.hfoss.posit.android.functionplugin.tracker.TrackerActivity;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/**
 * The communication module for POSIT. Handles most calls to the server to get
 * information regarding projects and finds.
 * 
 * 
 */

//public class Communicator extends OrmLiteBaseActivity<TrackerDbManager> {
public class Communicator {
	private static final String MESSAGE = "message";
	private static final String MESSAGE_CODE = "messageCode";
	private static final String ERROR_MESSAGE = "errorMessage";
	private static final String ERROR_CODE = "errorCode";
	private static final String COLUMN_IMEI = "imei";
	//Start of addition columns for photo table
	private static final String COLUMN_GUID = "guid"; 				//guid of the find
	private static final String COLUMN_IDENTIFIER = "identifier"; 	//does not seem to be useful
	private static final String COLUMN_PROJECT_ID = "project_id"; 	//project id of the find
	private static final String COLUMN_TIMESTAMP = "timestamp"; 	//if this is not set, it uses the current timestamp
	private static final String COLUMN_MIME_TYPE = "mime_type"; 	//data type, in this case, "image/jpeg"
	private static final String COLUMN_DATA_FULL = "data_full"; 	//data for the image, takes Base64 string of image
	private static final String COLUMN_DATA_THUMBNAIL = "data_thumbnail"; //data for the image, take Base 64 string of image
	//End of addition columns for photo table
	public static final int THUMBNAIL_TARGET_SIZE = 320; //width and height of thumbnail data
	public static final int CONNECTION_TIMEOUT = 3000; // millisecs
	public static final int SOCKET_TIMEOUT = 5000;
	public static final String RESULT_FAIL = "false";
	
	private static final String SERVER_PREF = "serverKey";
	private static final String PROJECT_PREF = "projectKey";

	private static String TAG = "Communicator";
	private Context mContext;
	public static long mTotalTime = 0;
	
	public static boolean isServerReachable(Context context) {
		SharedPreferences applicationPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String server = applicationPreferences.getString(SERVER_PREF, "");
		String url = server + "/api/isreachable?authKey=" + getAuthKey(context);

		HashMap<String, Object> responseMap = null;
		Log.i(TAG, "is reachable URL=" + url);

		String responseString = null;
		String responseCode = null;
		try {
			responseString = doHTTPGET(url);
			Log.i(TAG, "isreachable response = " + responseString);
			if (responseString.contains("[Error] ")) {
				Log.e(TAG, responseString);
				return false;
			} else {
				ResponseParser parser = new ResponseParser(responseString);
				responseMap = parser.parseObject();
				//responseCode = (String) responseMap.get(MESSAGE_CODE);
			}
		} catch (Exception e) {
			Log.i(TAG, "longinUser catch clause response = " + responseString);
			Toast.makeText(context, e.getMessage() + "", Toast.LENGTH_LONG).show();
			//sendAuthenticationResult(authKey, false, handler, context);
			return false;
		}
		try {
			if (responseMap.containsKey(ERROR_CODE)) {
				return false;
			} else if (responseMap.containsKey(MESSAGE_CODE)) {
				if (responseMap.get(MESSAGE_CODE).equals(Constants.AUTHN_OK)) {
					return true;
				}
			} else {
				return false;
			}
		} catch (Exception e) {
			Log.e(TAG, "loginUser " + e.getMessage() + " ");
			return false;
		}
		return false;
	}
	
	/**
	 * Removes an account. This should be called when, e.g., the user changes
	 * to a new server.
	 * @param context
	 * @param accountType
	 * @return
	 */
	public static boolean removeAccount(Context context, String accountType) {
		AccountManager am = AccountManager.get(context);
		am.invalidateAuthToken(accountType, SyncAdapter.AUTHTOKEN_TYPE);
		Account[] accounts = am.getAccountsByType(accountType);
		if (accounts.length != 0)
			am.removeAccount(accounts[0], null, null);
		String authkey = getAuthKey(context);
		return authkey == null;
	}
	
	public static String getAuthKey(Context context) {
		AccountManager accountManager = AccountManager.get(context);

		// TODO: again just picking the first account here.. how are you
		// supposed to handle this?
		Account[] accounts = accountManager.getAccountsByType(SyncAdapter.ACCOUNT_TYPE);

		if (accounts.length == 0)
			return null;

		String authKey = null;
		try {
			authKey = accountManager
					.blockingGetAuthToken(accounts[0], SyncAdapter.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
		} catch (OperationCanceledException e) {
			Log.e(TAG, "getAuthKey(), cancelled during request: " + e.getMessage());
			e.printStackTrace();
		} catch (AuthenticatorException e) {
			Log.e(TAG, "getAuthKey(), authentication exception: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "getAuthKey() IOException" + e.getMessage());
			e.printStackTrace();
		} catch (IllegalStateException e) {
			Log.e(TAG, "getAuthKey() IllegalStateException" + e.getMessage());
			e.printStackTrace();			
		}
		return authKey;
	}

	/**
	 * NOTE: Calls doHTTPGet
	 * 
	 * Get all open projects from the server. Eventually, the goal is to be able
	 * to get different types of projects depending on the privileges of the
	 * user.
	 * 
	 * @return a list of all the projects and their information, encoded as maps
	 * @throws JSONException
	 */
	public static ArrayList<HashMap<String, Object>> getProjects(Handler handler, Context context) {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String server = prefs.getString(SERVER_PREF, "");

		String authKey = getAuthKey(context);

		if (authKey != null) {

			String url = server + "/api/listMyProjects?authKey=" + authKey;

			ArrayList<HashMap<String, Object>> list;
			String responseString = doHTTPGET(url);
			Log.i(TAG, responseString);

			if (responseString.contains("Error")) {
				return null;
			}
			list = new ArrayList<HashMap<String, Object>>();
			try {
				list = (ArrayList<HashMap<String, Object>>) (new ResponseParser(responseString).parseList());
			} catch (JSONException e) {
				Log.i(TAG, "getProjects JSON exception " + e.getMessage());
				return null;
			}
			sendProjectsResult(list, true, handler, context);
			return list;
		} else {
			Log.e(TAG, "authKey is null.");
			return null;
		}
	}

	/**
	 * Registers the phone being used with the given server address, the
	 * authentication key, and the phone's imei
	 * 
	 * @param server
	 * @param authKey
	 * @param imei
	 * @return whether the registration was successful
	 */
	public String registerDevice(String server, String authKey, String imei) {
		String url = server + "/api/registerDevice?authKey=" + authKey + "&imei=" + imei;
		Log.i(TAG, "registerDevice URL=" + url);

		String responseString = null;
		try {
			responseString = doHTTPGET(url);
		} catch (Exception e) {
			// Toast.makeText(mContext, e.getMessage(),
			// Toast.LENGTH_LONG).show();
		}
		Log.i(TAG, responseString);
		if (responseString.equals(RESULT_FAIL))
			return null;
		else {
			return responseString;
		}
	}

	/**
	 * Registers the phone being used with the given server address, email,
	 * password and imei.
	 * 
	 * @param email
	 *            email/username
	 * @param password
	 *            the password
	 * @param imei
	 * @param handler
	 *            the handler instance from the UI thread
	 * @param context
	 *            the context of the calling activity
	 * @return the result
	 */
	public static String loginUser(String email, String password, String imei, Handler handler, Context context) {

		SharedPreferences applicationPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String server = applicationPreferences.getString(SERVER_PREF, "");

		String url = server + "/api/login";

		HashMap<String, Object> responseMap = null;
		Log.i(TAG, "loginUser URL=" + url);

		List<NameValuePair> sendList = new ArrayList<NameValuePair>();
		sendList.add(new BasicNameValuePair("email", email));
		sendList.add(new BasicNameValuePair("password", password));
		sendList.add(new BasicNameValuePair("imei", imei));

		String responseString = null;
		String authKey = null;
		try {
			responseString = doHTTPPost(url, sendList);
			Log.i(TAG, "longinUser response = " + responseString);
			if (responseString.contains("[Error] ")) {
				Log.e(TAG, responseString);
				return null;
			} else {
				ResponseParser parser = new ResponseParser(responseString);
				responseMap = parser.parseObject();
				authKey = (String) responseMap.get(MESSAGE);
			}
		} catch (Exception e) {
			Log.i(TAG, "longinUser catch clause response = " + responseString);
			Toast.makeText(context, e.getMessage() + "", Toast.LENGTH_LONG).show();
			sendAuthenticationResult(authKey, false, handler, context);
			return null;
		}
		try {
			if (responseMap.containsKey(ERROR_CODE)) {
				sendAuthenticationResult(authKey, false, handler, context);
				return null;
			} else if (responseMap.containsKey(MESSAGE_CODE)) {
				if (responseMap.get(MESSAGE_CODE).equals(Constants.AUTHN_OK)) {
					sendAuthenticationResult(authKey, true, handler, context);
					return authKey;
				}
			} else {
				sendAuthenticationResult(authKey, false, handler, context);
				return null;
			}
		} catch (Exception e) {
			Log.e(TAG, "loginUser " + e.getMessage() + " ");
			sendAuthenticationResult(authKey, false, handler, context);
			return null;
		}
		sendAuthenticationResult(authKey, false, handler, context);
		return null;
	}

	/**
	 * Attempts to authenticate the user credentials on the server.
	 * 
	 * @param email
	 *            The user's username
	 * @param password
	 *            The user's password to be authenticated
	 * @param imei
	 *            the phone's IMEI
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context
	 * @return Thread The thread on which the network mOperations are executed.
	 */
	public static Thread attemptAuth(final String email, final String password, final String imei,
			final Handler handler, final Context context) {

		final Runnable runnable = new Runnable() {
			public void run() {
				loginUser(email, password, imei, handler, context);
			}
		};
		// run on background thread.
		return performOnBackgroundThread(runnable);
	}

	/**
	 * Attempts to get the user's projects from the server.
	 * 
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context
	 * @return Thread The thread on which the network mOperations are executed.
	 */
	public static Thread attemptGetProjects(final Handler handler, final Context context) {

		final Runnable runnable = new Runnable() {
			ArrayList<HashMap<String, Object>> projectList;

			public void run() {
				projectList = getProjects(handler, context);
			}
		};
		// run on background thread.
		return performOnBackgroundThread(runnable);
	}

	/**
	 * Executes the network requests on a separate thread.
	 * 
	 * @param runnable
	 *            The runnable instance containing network mOperations to be
	 *            executed.
	 */
	public static Thread performOnBackgroundThread(final Runnable runnable) {
		final Thread t = new Thread() {
			@Override
			public void run() {
				try {
					runnable.run();
				} finally {
				}
			}
		};
		t.start();
		return t;
	}

	/**
	 * Sends the result of a getProjects request from server back to the caller
	 * main UI thread through its handler.
	 * 
	 * @param projects
	 *            the list of projects gotten from server
	 * @param result
	 *            The boolean holding authentication result
	 * @param authToken
	 *            The auth token returned from the server for this account.
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context.
	 */
	private static void sendProjectsResult(final ArrayList<HashMap<String, Object>> projects, final Boolean result,
			final Handler handler, final Context context) {
		if (handler == null || context == null) {
			return;
		}
		handler.post(new Runnable() {
			public void run() {
				((ListProjectsActivity) context).onShowProjectsResult(projects, result);
			}
		});
	}

	/**
	 * Sends the authentication response from server back to the caller main UI
	 * thread through its handler.
	 * 
	 * @param authKey
	 *            the auth key obtained from the server
	 * @param result
	 *            The boolean holding authentication result
	 * @param authToken
	 *            The auth token returned from the server for this account.
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context.
	 */
	private static void sendAuthenticationResult(final String authKey, final Boolean result, final Handler handler,
			final Context context) {
		if (handler == null || context == null) {
			return;
		}
		handler.post(new Runnable() {
			public void run() {
				((AuthenticatorActivity) context).onAuthenticationResult(result, authKey);
			}
		});
	}

	
	public String createProject(Context context, String server, String projectName,
			String projectDescription, String authKey) {
		String url = server + "/api/newProject?authKey=" + authKey;
		
		List<NameValuePair> nvp = new ArrayList<NameValuePair>();
		
		nvp.add(new BasicNameValuePair("name", projectName));
		nvp.add(new BasicNameValuePair("description", projectDescription));
		
		HashMap<String, Object> responseMap = null;
		Log.i(TAG, "Create Project URL=" + url);

		String responseString = null;
		
		try {
			responseString = doHTTPPost(url, nvp);
			Log.i(TAG, responseString);
			if (responseString.contains("[ERROR]")) {
				Toast.makeText(context, responseString, Toast.LENGTH_LONG).show();
				return Constants.AUTHN_FAILED + ":" + "Error";
			}
			ResponseParser parser = new ResponseParser(responseString);
			responseMap = parser.parseObject();
		} catch (Exception e) {
			Toast.makeText(context, e.getMessage() + "", Toast.LENGTH_LONG).show();
		}
		try {
			if (responseMap.containsKey(ERROR_CODE))
				return responseMap.get(ERROR_CODE) + ":"
				+ responseMap.get(ERROR_MESSAGE);
			else if (responseMap.containsKey(MESSAGE_CODE)) {
				return (String) responseMap.get(MESSAGE);

			} else {
				return "Malformed message from server.";
			}
		} catch (Exception e) {
			Log.e(TAG, "createProject " + e.getMessage());
			return e.getMessage();
		}
	}
	
	
	//
	// public String registerUser(String server, String firstname,
	// String lastname, String email, String password, String check,
	// String imei) {
	// String url = server + "/api/registerUser";
	// Log.i(TAG, "registerUser URL=" + url + "&imei=" + imei);
	// HashMap<String, String> sendMap = new HashMap<String, String>();
	// sendMap.put("email", email);
	// sendMap.put("password1", password);
	// sendMap.put("password2", check);
	// sendMap.put("firstname", firstname);
	// sendMap.put("lastname", lastname);
	// try {
	// responseString = doHTTPPost(url, sendMap);
	// Log.i(TAG, "registerUser Httpost responseString = "
	// + responseString);
	// if (responseString.contains("[ERROR]")) {
	// Toast.makeText(mContext,
	// Constants.AUTHN_FAILED + ":" + responseString,
	// Toast.LENGTH_LONG).show();
	// return Constants.AUTHN_FAILED + ":" + responseString;
	// }
	// ResponseParser parser = new ResponseParser(responseString);
	// HashMap<String, Object> responseMap = parser.parseObject();
	// if (responseMap.containsKey(ERROR_CODE))
	// return responseMap.get(ERROR_CODE) + ":"
	// + responseMap.get(ERROR_MESSAGE);
	// else if (responseMap.containsKey(MESSAGE_CODE)) {
	// if (responseMap.get(MESSAGE_CODE).equals(Constants.AUTHN_OK)) {
	// return Constants.AUTHN_OK + ":" + responseMap.get(MESSAGE);
	// }
	// } else {
	// return Constants.AUTHN_FAILED + ":"
	// + "Malformed message from the server.";
	// }
	// } catch (Exception e) {
	// Log.e(TAG, "registerUser " + e.getMessage() + " ");
	// return Constants.AUTHN_FAILED + ":" + e.getMessage();
	// }
	// return null;
	// }


	//
	// /**
	// * Converts a uri to a base64 encoded String for transmission to server.
	// *
	// * @param uri
	// * @return
	// */
	// private String convertUriToBase64(Uri uri) {
	// ByteArrayOutputStream imageByteStream = new ByteArrayOutputStream();
	// byte[] imageByteArray = null;
	// Bitmap bitmap = null;
	//
	// try {
	// bitmap = android.provider.MediaStore.Images.Media.getBitmap(
	// mContext.getContentResolver(), uri);
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	//
	// if (bitmap == null) {
	// Log.d(TAG, "No bitmap");
	// }
	// // Compress bmp to jpg, write to the byte output stream
	// bitmap.compress(Bitmap.CompressFormat.JPEG, 80, imageByteStream);
	// // Turn the byte stream into a byte array
	// imageByteArray = imageByteStream.toByteArray();
	// char[] base64 = Base64Coder.encode(imageByteArray);
	// String base64String = new String(base64);
	// return base64String;
	// }
	//
	// /**
	// * cleanup the item key,value pairs so that we can send the data.
	// *
	// * @param sendMap
	// */
	// private void cleanupOnSend(HashMap<String, String> sendMap) {
	// addRemoteIdentificationInfo(sendMap);
	// }
	//
	// /**
	// * Add the standard values to our request. We might as well use this as
	// * initializer for our requests.
	// *
	// * @param sendMap
	// */
	// private void addRemoteIdentificationInfo(HashMap<String, String> sendMap)
	// {
	// // sendMap.put(COLUMN_APP_KEY, appKey);
	// sendMap.put(COLUMN_IMEI, Utils.getIMEI(mContext));
	// }
	//
	// /**
	// * cleanup the item key,value pairs so that we can receive and save to the
	// * internal database
	// *
	// * @param rMap
	// */
	// public static void cleanupOnReceive(HashMap<String, Object> rMap) {
	// rMap.put(PositDbHelper.FINDS_SYNCED, PositDbHelper.FIND_IS_SYNCED);
	// rMap.put(PositDbHelper.FINDS_GUID, rMap.get("guid"));
	// // rMap.put(PositDbHelper.FINDS_GUID, rMap.get("guid"));
	//
	// rMap.put(PositDbHelper.FINDS_PROJECT_ID, projectId);
	// if (rMap.containsKey("add_time")) {
	// rMap.put(PositDbHelper.FINDS_TIME, rMap.get("add_time"));
	// rMap.remove("add_time");
	// }
	// if (rMap.containsKey("images")) {
	// if (Utils.debug)
	// Log.d(TAG, "contains image key");
	// rMap.put(PositDbHelper.PHOTOS_IMAGE_URI, rMap.get("images"));
	// rMap.remove("images");
	// }
	// }

	/**
	 * Sends a HttpPost request to the given URL. Any JSON
	 * 
	 * @param Uri, the URL to send to/receive from
	 * @param pairs, a list of attribute/value pairs
	 * @return the response from the URL
	 */
	public static String doHTTPPost(String Uri, List<NameValuePair> pairs) {
		BasicHttpParams mHttpParams = new BasicHttpParams();

		// Set the timeout in milliseconds until a connection is established.
		HttpConnectionParams.setConnectionTimeout(mHttpParams, CONNECTION_TIMEOUT);

		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		HttpConnectionParams.setSoTimeout(mHttpParams, SOCKET_TIMEOUT);

		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", new PlainSocketFactory(), 80));
		ThreadSafeClientConnManager mConnectionManager = new ThreadSafeClientConnManager(mHttpParams, registry);
		DefaultHttpClient mHttpClient = new DefaultHttpClient(mConnectionManager, mHttpParams);

		if (Uri == null)
			throw new NullPointerException("The URL has to be passed");
		String responseString = null;
		HttpPost post = new HttpPost();

		Log.i(TAG, "doHTTPPost() URI = " + Uri);
		try {
			post.setURI(new URI(Uri));
		} catch (URISyntaxException e) {
			Log.e(TAG, "URISyntaxException " + e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();
		}

		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		try {
			post.setEntity(new UrlEncodedFormEntity(pairs, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "UnsupportedEncodingException " + e.getMessage());
			return "[Error] " + e.getMessage();
		}

		try {
			responseString = mHttpClient.execute(post, responseHandler);
			Log.d(TAG, "doHTTPpost responseString = " + responseString);
		} catch (ClientProtocolException e) {
			Log.e(TAG, "ClientProtocolException" + e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();
		} catch (IOException e) {
			Log.e(TAG, "IOException " + e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();
		} catch (IllegalStateException e) {
			Log.e(TAG, "IllegalStateException: " + e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();
		} catch (Exception e) {
			Log.e(TAG, "Exception on HttpPost " + e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();
		}

		Log.i(TAG, "doHTTPpost response = " + responseString);

		return responseString;
	}
	
	
	/**
	 * Sends a HttpPost request to the given URL. Any JSON
	 * 
	 * @param Uri, the URL to send to/receive from
	 * @param sendMap, the hashMap of data to send to the server as POST data
	 * @return the response from the URL
	 */	
	public static String doHTTPPost(String Uri, HashMap<String, String> sendMap) {
		return doHTTPPost(Uri, getNameValuePairs(sendMap));
	}

	/**
	 * A wrapper(does some cleanup too) for sending HTTP GET requests to the URI
	 * 
	 * @param Uri
	 * @return the request from the remote server
	 */
	public static String doHTTPGET(String Uri) {
		BasicHttpParams mHttpParams = new BasicHttpParams();

		// Set the timeout in milliseconds until a connection is established.
		HttpConnectionParams.setConnectionTimeout(mHttpParams, CONNECTION_TIMEOUT);

		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		HttpConnectionParams.setSoTimeout(mHttpParams, SOCKET_TIMEOUT);

		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", new PlainSocketFactory(), 80));
		ThreadSafeClientConnManager mConnectionManager = new ThreadSafeClientConnManager(mHttpParams, registry);
		DefaultHttpClient mHttpClient = new DefaultHttpClient(mConnectionManager, mHttpParams);

		if (Uri == null)
			throw new NullPointerException("The URL has to be passed");
		String responseString = null;
		HttpGet httpGet = new HttpGet();

		try {
			httpGet.setURI(new URI(Uri));
		} catch (URISyntaxException e) {
			Log.e(TAG, "doHTTPGet " + e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();
		}

		Log.i(TAG, "doHTTPGet Uri = " + Uri);

		ResponseHandler<String> responseHandler = new BasicResponseHandler();

		try {
			responseString = mHttpClient.execute(httpGet, responseHandler);
		} catch (ClientProtocolException e) {
			Log.e(TAG, "ClientProtocolException" + e.getMessage());
			return "[Error] " + e.getMessage();
		} catch (SocketTimeoutException e) {
			Log.e(TAG, "[Error: SocketTimeoutException]" + e.getMessage());
			return "[Error] " + e.getMessage();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			return "[Error] " + e.getMessage();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage() + "what");
			return "[Error] " + e.getMessage();
		}

		Log.i(TAG, "doHTTPGet Response: " + responseString);
		return responseString;
	}

	public static List<NameValuePair> getNameValuePairs(Find find) {
		// Get fields from both class and superclass
		List<NameValuePair> pairs = null;
		if (find.getClass().getName().equals(Find.class.getName())) { // For basic POSIT
			pairs = getNameValuePairs(find, find.getClass());
		} else { // For find extensions
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
	private static List<NameValuePair> getNameValuePairs(Find find, Class clazz) {
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
	 * Registers a new expedition with the server.
	 * @param projectId  Posit's current project id.
	 * @return Returns the expedition number received from the server or -1 if something
	 * goes wrong.
	 */
	public int registerExpeditionId(Context context, int projectId) {
		mContext = context;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String server = prefs.getString(SERVER_PREF, "");

		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = telephonyManager.getDeviceId();
		
		HashMap<String, String> sendMap = new HashMap<String, String>();
		addRemoteIdentificationInfo(sendMap);
		String addExpeditionUrl = server + "/api/addExpedition?authKey="   + getAuthKey(context);
		sendMap.put("projectId", "" + projectId);
		Log.i(TAG, "URL=" + addExpeditionUrl + " projectId = " + projectId);
		String response = doHTTPPost(addExpeditionUrl, sendMap);
		Log.d(TAG,"registerExpeditionId response = " + response);

		// The server should return an expedition number if everything goes ok.  If 
		//  an error occurs, it will return an error message that cannot parse to an int
		//  which will cause an exception here.
		try {
			Integer i = Integer.parseInt(response);
			return i;
		} catch (NumberFormatException e) {
			Log.e(TAG, "Communicator, registerExpeditionId, Invalid response received");
			return -1;
		}
	}
	
	private void addRemoteIdentificationInfo(HashMap<String, String> sendMap) {
		// sendMap.put(COLUMN_APP_KEY, appKey);
		sendMap.put(COLUMN_IMEI, getIMEI(mContext));
	}
	
	/**
	 * Gets the unique IMEI code for the phone used for identification
	 * The phone should have proper permissions (READ_PHONE_STATE) to be able to get this data.
	 */
	public static String getIMEI(Context mContext) {
		TelephonyManager tm = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getDeviceId();
	}
	
	public static List<NameValuePair> getNameValuePairs (HashMap<String,String> nameValuesMap) {
		Iterator<String> iter = nameValuesMap.keySet().iterator();
		List<NameValuePair> nvp = new ArrayList<NameValuePair>();
		while (iter.hasNext()) {
			String key = iter.next();
			String value = nameValuesMap.get(key);
			nvp.add(new BasicNameValuePair(key,value));
		}
		return nvp;
	}

	/**
	 * Sends a GPS point and associated data to the Posit server. Called from 
	 *  Tracker Activity or TrackerBackgroundService.  
	 */
	public String registerExpeditionPoint(Context context, double lat, double lng, double alt,
			int swath, int expedition, long time) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String server = prefs.getString(SERVER_PREF, "");

			//long swath, int expedition) {
		Log.i(TAG, "Communicator, registerExpeditionPoint " + lat + " " + lng + " " + time);
		HashMap<String, String> sendMap = new HashMap<String, String>();
		addRemoteIdentificationInfo(sendMap);
		Log.i(TAG, "Sendmap= " + sendMap.toString());
		String addExpeditionUrl = server + "/api/addExpeditionPoint?authKey="  + this.getAuthKey(context);
		sendMap.put(DbManager.GPS_POINT_LATITUDE, "" + lat);
		sendMap.put(DbManager.GPS_POINT_LONGITUDE, lng + "");
		sendMap.put(DbManager.GPS_POINT_ALTITUDE, "" + alt);
		sendMap.put(DbManager.GPS_POINT_SWATH, "" + swath);
		sendMap.put(DbManager.EXPEDITION, expedition + "");
		sendMap.put(DbManager.GPS_TIME, time + "");
		Log.i(TAG, "Sendmap= " + sendMap.toString());
		
		String response = doHTTPPost(addExpeditionUrl, sendMap);
		Log.i(TAG, "Communicator, registerExpeditionPoint, response: " + response);
		return response;
	}
}