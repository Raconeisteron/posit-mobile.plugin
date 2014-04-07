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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.Authenticator;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
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
import org.apache.http.protocol.HTTP;
import org.hfoss.posit.android.Constants;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.activity.ListProjectsActivity;
import org.hfoss.posit.android.api.authentication.AuthenticatorActivity;
import org.hfoss.posit.android.functionplugin.tracker.Points;
import org.json.JSONException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;


/**
 * Helper class to enable us to run the blockingGetAuthToken() method on
 * a separate thread.
 * 
 * According to the documentation, blockingGetAuthToken() is synchronous,
 * so we should be able to use it without complications like a background
 * process.  It can block, however.
 * 
 * The way to use this class is as follows:
 * 
 * 		AuthKeyGetter getter = new AuthKeyGetter(context); // Create an instance
 *		Thread thread = new Thread (getter);               // Create a new thread
 *		thread.run();                                      // Run the thread
 *		String authKey = getter.getAuthKey();              // Get the authkey
 * 
 * NOTE: This probably requires some refinement to respond if it does block.
 *  
 * @author rmorelli
 *
 */
class AuthKeyGetter implements Runnable {
	public static final String TAG = "AuthKeyGetter";
	private Context context;
	
	public AuthKeyGetter (Context context) {
		Log.i(TAG, "AuthKeyGetter constructor, context = " + context);
		this.context = context;
	}

	String authKey = null;

	/**
	 * Return the auth key. Call this after calling run.
	 * @return
	 */
	public String getAuthKey () {
		return authKey;
	}
	
	/**
	 * This method can run in a separate thread.
	 */
	public void run() {
		final AccountManager accountManager = AccountManager.get(context);
		Account[] accounts = accountManager.getAccountsByType(SyncAdapter.ACCOUNT_TYPE);

		if (accounts.length == 0)
			return;
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
	}
}


/**
 * The communication module for POSIT. Handles most calls to the server to get
 * information regarding projects and finds.
 * 
 */
public class Communicator {
	public static final int SUCCESS = 1;
	public static final int FAILURE = 0;
	
	// Constants needed for managing authKey
	public static final int AUTH_AUTHENTICATE = 0;
	public static final int AUTH_GET_PROJECTS = 1;
	public static final int AUTH_REG_EXPEDITION = 2;
	public static final int AUTH_REG_GPS_POINT = 3;
	
	
	private static final String MESSAGE = "message";
	private static final String MESSAGE_CODE = "messageCode";
	private static final String ERROR_MESSAGE = "errorMessage";
	private static final String ERROR_CODE = "errorCode";
	private static final String COLUMN_IMEI = "imei";
	//Start of addition columns for photo table
//	private static final String COLUMN_GUID = "guid"; 				//guid of the find
//	private static final String COLUMN_IDENTIFIER = "identifier"; 	//does not seem to be useful
//	private static final String COLUMN_PROJECT_ID = "project_id"; 	//project id of the find
//	private static final String COLUMN_TIMESTAMP = "timestamp"; 	//if this is not set, it uses the current timestamp
//	private static final String COLUMN_MIME_TYPE = "mime_type"; 	//data type, in this case, "image/jpeg"
//	private static final String COLUMN_DATA_FULL = "data_full"; 	//data for the image, takes Base64 string of image
//	private static final String COLUMN_DATA_THUMBNAIL = "data_thumbnail"; //data for the image, take Base 64 string of image
	//End of addition columns for photo table
	public static final int THUMBNAIL_TARGET_SIZE = 320; //width and height of thumbnail data
	public static final int CONNECTION_TIMEOUT = 3000; // millisecs
	public static final int SOCKET_TIMEOUT = 5000;
	public static final String RESULT_FAIL = "false";
	
	private static final String SERVER_PREF = "serverKey";
//	private static final String PROJECT_PREF = "projectKey";

	private static String TAG = "Communicator";
//	private Context mContext;
	public static long mTotalTime = 0;
	
	private static String sAuthKey = null;
	

	/**
	 * Static method to retrieve the authkey, which is necessary for
	 * basically all server operations.  This method launches a 
	 * separate thread to retrieve the authkey.
	 * 
	 * @param context
	 * @return
	 */
	public static String getAuthKey(Context context){
    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
    	String authkey = sp.getString(context.getString(R.string.authKey), "");
		if (authkey != "") {
			Log.i(TAG, "getAuthKey(), Returning saved authkey = " + authkey);
			return authkey;
		} 
		
		if (sAuthKey != null) {
			Log.i(TAG, "getAuthKey(), Returning cached authkey = " + sAuthKey);
			return sAuthKey;
		}
		
		Log.i(TAG, "getAuthKey, retrieving authkey from AccountManager");
		AuthKeyGetter getter = new AuthKeyGetter(context);
		Thread thread = new Thread (getter);
		thread.run();
		String authKey = getter.getAuthKey();
		Log.i(TAG, "authKey = " + authKey);
		setAuthKey(authKey);
		return authKey;
	}
	
	public static void setAuthKey(String authkey) {
		sAuthKey = authkey;
	}
	
	/**
	 * Checks to see if Posit server is reachable.  NOTE that this does not require an authkey.
	 * 
	 * @param context
	 * @return  true if the server responds and false otherwise
	 */
	public static boolean isServerReachable(Context context) {
		SharedPreferences applicationPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String server = applicationPreferences.getString(SERVER_PREF, "");
				
		String url = server + "/api/isreachable";

		HashMap<String, Object> responseMap = null;
		Log.i(TAG, "is reachable URL=" + url);

		// Try to reach the server and parse the response
		String responseString = null;
		try {
			responseString = doHTTPGET(url);
			Log.i(TAG, "isreachable response = " + responseString);
			if (responseString.contains("[Error] ")) {
				return false;
			} else {
				Log.i(TAG, "isreachable response = " + responseString);
				ResponseParser parser = new ResponseParser(responseString);
				responseMap = parser.parseObject();
			}
		} catch (Exception e) {
			Log.i(TAG, "isServerReachable catch clause response = " + responseString);
			Toast.makeText(context, e.getMessage() + "", Toast.LENGTH_LONG).show();
			return false;
		}
		
		// Return true or false based on whether the response contains an error code
		try {
			Log.i(TAG, "responseMap " + responseMap.toString());
			if (responseMap.containsKey(ERROR_CODE)) {
				return false;
			} else if (responseMap.containsKey(MESSAGE_CODE)  
					&& responseMap.get(MESSAGE_CODE).equals(Constants.AUTHN_OK)) {
				return true;
			} else {
				return false;  
			}
		} catch (Exception e) {
			Log.e(TAG, "isServerReachable " + e.getMessage() + " ");
			return false;
		}
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
	public static void attemptAuth(final String email, final String password, final String imei,
			final Handler handler, final Context context) {
		Log.i(TAG, "Attempt Auth");

		final Runnable runnable = new Runnable() {
			public void run() {
				loginUser(email, password, imei, handler, context);
			}
		};
		new Thread(runnable).run();  		// run on background thread.
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
	public static ArrayList<HashMap<String, Object>> getProjects(Context context) {
		Log.i(TAG, "getProjects()");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String server = prefs.getString(SERVER_PREF, "");
		
		String authKey = getAuthKey(context);  // Set to null to test error handling

		String url = server + "/api/listMyProjects?authKey=" + authKey;

		ArrayList<HashMap<String, Object>> list = null;
		String responseString = doHTTPGET(url);
		Log.i(TAG, responseString);

		//responseString = null;  // Test of error handling

		try {
			list = (ArrayList<HashMap<String, Object>>) (new ResponseParser(responseString).parseList());
//			Message msg = handler.obtainMessage(SUCCESS, list);
//			handler.sendMessage(msg);
		} catch (JSONException e) {
			Log.i(TAG, "getProjects JSON exception " + e.getMessage());
//			Message msg = handler.obtainMessage(FAILURE, e.getMessage());
//			handler.sendMessage(msg);
//			return;
		} catch (Exception e) {
			Log.i(TAG, "getProjects Exception " + e.getMessage());
//			Message msg = handler.obtainMessage(FAILURE, "Exception " + e.getMessage());
//			handler.sendMessage(msg);
//			return;				
		}
		return list;
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
	 * Registers the phone being used with the given server address, email, password and imei.
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
			Log.i(TAG, "loginUser response = " + responseString);
			if (responseString.contains("[Error] ")) {
				Log.e(TAG, responseString);
				return null;
			} else {
				ResponseParser parser = new ResponseParser(responseString);
				responseMap = parser.parseObject();
				authKey = (String) responseMap.get(MESSAGE);
				Log.i(TAG, "authKey = " + authKey);
			}
		} catch (Exception e) {
			Log.i(TAG, "loginUser catch clause response = " + responseString);
			e.printStackTrace();
			Toast.makeText(context, e.getMessage() + "", Toast.LENGTH_LONG).show();
			Message msg = handler.obtainMessage(FAILURE, e.getMessage());
			handler.sendMessage(msg);
			//			sendAuthenticationResult(authKey, false, handler, context);
			return null;
		}
		try {
			if (responseMap.containsKey(ERROR_CODE)) {
				Message msg = handler.obtainMessage(FAILURE, responseMap.toString());
				handler.sendMessage(msg);			
				return null;
			} else if (responseMap.containsKey(MESSAGE_CODE)) {
				if (responseMap.get(MESSAGE_CODE).equals(Constants.AUTHN_OK)) {
					Log.i(TAG, "message code = ok ");
					if (handler == null) {
						Log.i(TAG, "handler is null ");
					}
					Message msg = handler.obtainMessage(SUCCESS, authKey);
					handler.sendMessage(msg);							
					Log.i(TAG, "message code = ok ");
					return authKey;
				}
			} else {
				Log.i(TAG, "Authentication error");
				Message msg = handler.obtainMessage(FAILURE, "Authentication error");
				handler.sendMessage(msg);							
				return null;
			}
		} catch (Exception e) {
			Log.e(TAG, "loginUser exception " + e.getMessage() + " ");
			e.printStackTrace();
			Message msg = handler.obtainMessage(FAILURE, e.getMessage());
			handler.sendMessage(msg);							
			return null;
		}
		Message msg = handler.obtainMessage(FAILURE, "Authentication error");
		handler.sendMessage(msg);							
		return null;
	}
	
//	/**
//	 * Executes the network requests on a separate thread.
//	 * 
//	 * @param runnable
//	 *            The runnable instance containing network mOperations to be
//	 *            executed.
//	 */
//	public static Thread performOnBackgroundThread(final Runnable runnable) {
//		final Thread t = new Thread() {
//			@Override
//			public void run() {
//				try {
//					runnable.run();
//				} finally {
//				}
//			}
//		};
//		t.start();
//		return t;
//	}
	
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
	 * A wrapper(does some cleanup too) for sending HTTP GET requests to the URI.
	 * If the get throws an exception, this returns a string containing "[Error]".
	 * 
	 * @param Uri
	 * @return the request from the remote server or [Error] message.
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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String server = prefs.getString(SERVER_PREF, "");

		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = telephonyManager.getDeviceId();
		
		HashMap<String, String> sendMap = new HashMap<String, String>();
		addRemoteIdentificationInfo(context, sendMap);
		
		String authkey = getAuthKey(context); 
		
		String addExpeditionUrl = server + "/api/addExpedition?authKey="  + authkey;

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
	
	private void addRemoteIdentificationInfo(Context context, HashMap<String, String> sendMap) {
		// sendMap.put(COLUMN_APP_KEY, appKey);
		sendMap.put(COLUMN_IMEI, getIMEI(context));
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

		Log.i(TAG, "Communicator, registerExpeditionPoint " + lat + " " + lng + " " + time);
		HashMap<String, String> sendMap = new HashMap<String, String>();
		addRemoteIdentificationInfo(context, sendMap);
		Log.i(TAG, "Sendmap= " + sendMap.toString());
		
		String authKey = getAuthKey(context);
		
		String addExpeditionUrl = server + "/api/addExpeditionPoint?authKey=" + authKey;

		sendMap.put(Points.GPS_POINT_LATITUDE, "" + lat);
		sendMap.put(Points.GPS_POINT_LONGITUDE, lng + "");
		sendMap.put(Points.GPS_POINT_ALTITUDE, "" + alt);
		sendMap.put(Points.GPS_POINT_SWATH, "" + swath);
		sendMap.put(Points.EXPEDITION, expedition + "");
		sendMap.put(Points.GPS_TIME, time + "");
		Log.i(TAG, "Sendmap= " + sendMap.toString());
		
		String response = doHTTPPost(addExpeditionUrl, sendMap);
		Log.i(TAG, "Communicator, registerExpeditionPoint, response: " + response);
		return response;
	}
}