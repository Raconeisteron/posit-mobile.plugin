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
package org.hfoss.posit.android.experimental.sync;

import java.io.ByteArrayOutputStream;
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
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.authentication.AuthenticatorActivity;
//import org.hfoss.posit.android.experimental.api.authentication.NetworkUtilities;
import org.hfoss.posit.android.experimental.api.database.DbHelper;
import org.hfoss.posit.android.experimental.api.activity.ListProjectsActivity;

import android.util.Log;
import android.widget.Toast;

import org.hfoss.posit.android.experimental.Constants;
import org.hfoss.posit.android.experimental.R;
//import org.hfoss.posit.android.TrackerActivity;
//import org.hfoss.posit.android.provider.PositDbHelper;
//import org.hfoss.posit.android.utilities.Utils;
//import org.hfoss.third.Base64Coder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

/**
 * The communication module for POSIT. Handles most calls to the server to get
 * information regarding projects and finds.
 * 
 * 
 */
public class Communicator {
	private static final String MESSAGE = "message";
	private static final String MESSAGE_CODE = "messageCode";
	private static final String ERROR_MESSAGE = "errorMessage";
	private static final String ERROR_CODE = "errorCode";
	private static final String COLUMN_IMEI = "imei";
	public static final int CONNECTION_TIMEOUT = 3000; // millisecs
	public static final int SOCKET_TIMEOUT = 5000;
	public static final String RESULT_FAIL = "false";

	private static final String SERVER_PREF = "serverKey";
	private static final String PROJECT_PREF = "projectKey";

	private static String TAG = "Communicator";

	// /**
	// * Attempts to get the auth token. Apparently this might have to perform a
	// * network request, so you're supposed to use a thread.
	// */
	// public static Thread getAuthToken(final Context context) {
	//
	// final Runnable runnable = new Runnable() {
	// public void run() {
	// AccountManager mAccountManager = AccountManager.get(context);
	//
	// // TODO: again just picking the first account here.. how are you
	// // supposed to handle this?
	// Account[] accounts =
	// mAccountManager.getAccountsByType(SyncAdapter.ACCOUNT_TYPE);
	//
	// try {
	// String authKey = mAccountManager
	// .blockingGetAuthToken(accounts[0],
	// SyncAdapter.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
	// Log.i(TAG, "AUTH TOKEN: " + authKey);
	// } catch (OperationCanceledException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (AuthenticatorException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	// };
	// // run on background thread.
	// return performOnBackgroundThread(runnable);
	// }

	private static String getAuthKey(Context context) {
		AccountManager accountManager = AccountManager.get(context);

		// TODO: again just picking the first account here.. how are you
		// supposed to handle this?
		Account[] accounts = accountManager.getAccountsByType(SyncAdapter.ACCOUNT_TYPE);

		String authKey = null;
		try {
			authKey = accountManager
					.blockingGetAuthToken(accounts[0], SyncAdapter.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
		} catch (OperationCanceledException e) {
			Log.i(TAG, "getAuthKey(), cancelled during request: " + e.getMessage());
			e.printStackTrace();
		} catch (AuthenticatorException e) {
			Log.e(TAG, "getAuthKey(), authentication exception: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "getAuthKey() IOException" + e.getMessage());
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

	//
	// public String createProject(String server, String projectName,
	// String projectDescription, String authKey) {
	// String url = server + "/api/newProject?authKey=" + authKey;
	// HashMap<String, String> sendMap = new HashMap<String, String>();
	// sendMap.put("name", projectName);
	// sendMap.put("description", projectDescription);
	//
	// HashMap<String, Object> responseMap = null;
	// Log.i(TAG, "Create Project URL=" + url);
	//
	// try {
	// responseString = doHTTPPost(url, sendMap);
	// Log.i(TAG, responseString);
	// if (responseString.contains("[ERROR]")) {
	// Toast.makeText(mContext, responseString, Toast.LENGTH_LONG)
	// .show();
	// return Constants.AUTHN_FAILED + ":" + "Error";
	// }
	// ResponseParser parser = new ResponseParser(responseString);
	// responseMap = parser.parseObject();
	// } catch (Exception e) {
	// Toast.makeText(mContext, e.getMessage() + "", Toast.LENGTH_LONG)
	// .show();
	// }
	// try {
	// if (responseMap.containsKey(ERROR_CODE))
	// return responseMap.get(ERROR_CODE) + ":"
	// + responseMap.get(ERROR_MESSAGE);
	// else if (responseMap.containsKey(MESSAGE_CODE)) {
	// return (String) responseMap.get(MESSAGE);
	//
	// } else {
	// return "Malformed message from server.";
	// }
	// } catch (Exception e) {
	// Log.e(TAG, "createProject " + e.getMessage());
	// return e.getMessage();
	// }
	// }
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

	/*
	 * TODO: This method is a little long and could be split up. Send one find
	 * to the server, including its images.
	 * 
	 * @param find a reference to the Find object
	 * 
	 * @param action -- either 'create' or 'update'
	 */
	public static boolean sendFind(Find find, String action, Context context, String authToken) {
		String url;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String server = prefs.getString(SERVER_PREF, "");

		url = server + "/api/createFind?authKey=" + authToken;

		Log.i(TAG, "SendFind=" + find);

		List<NameValuePair> pairs = getNameValuePairs(find);

		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = telephonyManager.getDeviceId();

		BasicNameValuePair pair = new BasicNameValuePair("imei", imei);
		pairs.add(pair);
		Log.i(TAG, "pairs: " + pairs);
		String responseString = null;

		// Send the find
		try {
			responseString = doHTTPPost(url, pairs);
			DbHelper.getDbManager(context).updateStatus(find, Constants.TRANSACTING);
			DbHelper.getDbManager(context).updateSyncOperation(find, Constants.POSTING);
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
			DbHelper.getDbManager(context).updateStatus(find, Constants.FAILED);
		}

		Log.i(TAG, "sendFind.ResponseString: " + responseString);

		// If the update failed return false
		if (responseString.indexOf("True") == -1) {
			Log.i(TAG, "sendFind result doesn't contain 'True'");
			DbHelper.getDbManager(context).updateStatus(find, Constants.FAILED);
			return false;
		} else {
			DbHelper.getDbManager(context).updateStatus(find, Constants.SUCCEEDED);
			Log.i(TAG, "sendFind() synced find id: " + find.getId());
		}

		// if (!success)
		// return false;

		// // Otherwise send the Find's images
		//
		// long id = Long.parseLong(sendMap.get(PositDbHelper.FINDS_ID));
		// PositDbHelper dbh = new PositDbHelper(mContext);
		// ArrayList<ContentValues> photosList =
		// dbh.getImagesListSinceUpdate(id);
		//
		// Log.i(TAG, "sendFind, photosList=" + photosList.toString());
		//
		// Iterator<ContentValues> it = photosList.listIterator();
		// while (it.hasNext()) {
		// ContentValues imageData = it.next();
		// Uri uri = Uri.parse(imageData
		// .getAsString(PositDbHelper.PHOTOS_IMAGE_URI));
		// String base64Data = convertUriToBase64(uri);
		// uri = Uri.parse(imageData
		// .getAsString(PositDbHelper.PHOTOS_THUMBNAIL_URI));
		// String base64Thumbnail = convertUriToBase64(uri);
		// sendMap = new HashMap<String, String>();
		// sendMap.put(COLUMN_IMEI, Utils.getIMEI(mContext));
		// sendMap.put(PositDbHelper.FINDS_GUID, guid);
		//
		// sendMap.put(PositDbHelper.PHOTOS_IDENTIFIER,
		// imageData.getAsString(PositDbHelper.PHOTOS_IDENTIFIER));
		// sendMap.put(PositDbHelper.FINDS_PROJECT_ID,
		// imageData.getAsString(PositDbHelper.FINDS_PROJECT_ID));
		// sendMap.put(PositDbHelper.FINDS_TIME,
		// imageData.getAsString(PositDbHelper.FINDS_TIME));
		// sendMap.put(PositDbHelper.PHOTOS_MIME_TYPE,
		// imageData.getAsString(PositDbHelper.PHOTOS_MIME_TYPE));
		//
		// sendMap.put("mime_type", "image/jpeg");
		//
		// sendMap.put(PositDbHelper.PHOTOS_DATA_FULL, base64Data);
		// sendMap.put(PositDbHelper.PHOTOS_DATA_THUMBNAIL, base64Thumbnail);
		// sendMedia(sendMap);
		// // it.next();
		// }

		DbHelper.releaseDbManager();
		return true;
	}

	// /**
	// * Sends an image (or sound file or video) to the server.
	// *
	// * @param identifier
	// * @param findId
	// * the guid of the associated find
	// * @param data
	// * @param mimeType
	// */
	// public void sendMedia(HashMap<String, String> sendMap) {
	// Log.i(TAG, "sendMedia, sendMap= " + sendMap);
	//
	// String url = server + "/api/attachPicture?authKey=" + authKey;
	//
	// responseString = doHTTPPost(url, sendMap);
	// if (Utils.debug)
	// Log.i(TAG, "sendImage.ResponseString: " + responseString);
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
	 * @param Uri
	 *            the URL to send to/receive from
	 * @param sendMap
	 *            the hashMap of data to send to the server as POST data
	 * @return the response from the URL
	 */
	private static String doHTTPPost(String Uri, List<NameValuePair> pairs) {
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

	// public boolean projectExists(String projectId, String server){
	// String url =
	// server+"/api/projectExists?authKey="+authKey+"&projectId="+projectId;
	// Log.i(TAG, url);
	// String response = doHTTPGET(url);
	// Log.i(TAG, "projectExists response = " + response);
	//
	// if(response.equals("true"))
	// return true;
	// if(response.equals("false"))
	// return false;
	// return false;
	// }
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
		Field[] fields = find.getClass().getDeclaredFields(); // May not get
																// methods in
																// superclass?

		List<NameValuePair> nvp = new ArrayList<NameValuePair>();
		String methodName = "";
		String value = "";

		for (Field field : fields) {
			if (!Modifier.isFinal(field.getModifiers())) {
				String key = field.getName();
				methodName = "get" + key.substring(0, 1).toUpperCase() + key.substring(1);
				value = "";

				try {
					Class returnType = find.getClass().getDeclaredMethod(methodName, null).getReturnType();
					if (returnType.equals(String.class))
						value = (String) find.getClass().getDeclaredMethod(methodName, null)
								.invoke(find, (Object[]) null);
					else if (returnType.equals(int.class))
						value = String.valueOf((Integer) find.getClass().getDeclaredMethod(methodName, null)
								.invoke(find, (Object[]) null));

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
	//
	// /**
	// * Pull the remote find from the server using the guid provided.
	// *
	// * @param guid
	// * , a globally unique identifier
	// * @return an associative list of attribute/value pairs
	// */
	// public ContentValues getRemoteFindById(String guid) {
	// String url = server + "/api/getFind?guid=" + guid + "&authKey="
	// + authKey;
	// HashMap<String, String> sendMap = new HashMap<String, String>();
	// addRemoteIdentificationInfo(sendMap);
	// sendMap.put("guid", guid + "");
	// String responseString = doHTTPPost(url, sendMap);
	// ContentValues cv = new ContentValues();
	//
	// Log.i(TAG, "getRemoteFindById = " + responseString);
	// try {
	// JSONObject jobj = new JSONObject(responseString);
	// cv.put(PositDbHelper.FINDS_GUID,
	// jobj.getString(PositDbHelper.FINDS_GUID));
	// cv.put(PositDbHelper.FINDS_PROJECT_ID,
	// jobj.getInt(PositDbHelper.FINDS_PROJECT_ID));
	// cv.put(PositDbHelper.FINDS_NAME,
	// jobj.getString(PositDbHelper.FINDS_NAME));
	// cv.put(PositDbHelper.FINDS_DESCRIPTION, jobj
	// .getString(PositDbHelper.FINDS_DESCRIPTION));
	// //FIXME add add_time and modify_time for this
	// cv.put(PositDbHelper.FINDS_TIME, jobj.getString("add_time"));
	// cv.put(PositDbHelper.FINDS_TIME, jobj.getString("modify_time"));
	// cv.put(PositDbHelper.FINDS_LATITUDE,
	// jobj.getDouble(PositDbHelper.FINDS_LATITUDE));
	// cv.put(PositDbHelper.FINDS_LONGITUDE,
	// jobj.getDouble(PositDbHelper.FINDS_LONGITUDE));
	// cv.put(PositDbHelper.FINDS_REVISION,
	// jobj.getInt(PositDbHelper.FINDS_REVISION));
	// return cv;
	// } catch (JSONException e) {
	// Log.i(TAG, e.getMessage());
	// e.printStackTrace();
	// } catch (Exception e) {
	// Log.i(TAG, e.getMessage());
	// e.printStackTrace();
	// }
	// return null;
	// }
	//
	// /**
	// * Get an image from the server using the guid as Key.
	// *
	// * @param guid
	// * the Find's globally unique Id
	// */
	// public ArrayList<HashMap<String, String>> getRemoteFindImages(String
	// guid) {
	// ArrayList<HashMap<String, String>> imagesMap = null;
	// // ArrayList<HashMap<String, String>> imagesMap = null;
	// String imageUrl = server + "/api/getPicturesByFind?findId=" + guid
	// + "&authKey=" + authKey;
	// HashMap<String, String> sendMap = new HashMap<String, String>();
	// Log.i(TAG, "getRemoteFindImages, sendMap=" + sendMap.toString());
	// sendMap.put(PositDbHelper.FINDS_GUID, guid);
	// addRemoteIdentificationInfo(sendMap);
	// try {
	// String imageResponseString = doHTTPPost(imageUrl, sendMap);
	// Log.i(TAG, "getRemoteFindImages, response=" + imageResponseString);
	//
	// if (!imageResponseString.equals(RESULT_FAIL)) {
	// JSONArray jsonArr = new JSONArray(imageResponseString);
	// imagesMap = new ArrayList<HashMap<String, String>>();
	// // imagesMap = new ArrayList<HashMap<String, String>>();
	//
	// for (int i = 0; i < jsonArr.length(); i++) {
	// JSONObject jsonObj = jsonArr.getJSONObject(i);
	// if (Utils.debug)
	// Log.i(TAG, "JSON Image Response String: "
	// + jsonObj.toString());
	// // imagesMap.add((HashMap<String, String>) jsonArr.get(i));
	// Iterator<String> iterKeys = jsonObj.keys();
	// HashMap<String, String> map = new HashMap<String, String>();
	// while (iterKeys.hasNext()) {
	// String key = iterKeys.next();
	// map.put(key, jsonObj.getString(key));
	// }
	// imagesMap.add(map);
	// }
	// }
	// } catch (Exception e) {
	// Log.i(TAG, e.getMessage());
	// e.printStackTrace();
	// }
	// if (imagesMap != null && Utils.debug)
	// Log
	// .i(TAG, "getRemoteFindImages, imagesMap="
	// + imagesMap.toString());
	// else
	// Log.i(TAG, "getRemoteFindImages, imagesMap= null");
	// return imagesMap;
	// }
	//
	// /**
	// * Checks if a given image already exists on the server. Allows for
	// quicker
	// * syncing to the server, as this allows the application to bypass
	// * converting from a bitmap to base64 to send to the server
	// *
	// * @param imageId
	// * the id of the image to query
	// * @return whether the image already exists on the server
	// */
	// public boolean imageExistsOnServer(int imageId) {
	// HashMap<String, String> sendMap = new HashMap<String, String>();
	// addRemoteIdentificationInfo(sendMap);
	// String imageUrl = server + "/api/getPicture?id=" + imageId
	// + "&authKey=" + authKey;
	// String imageResponseString = doHTTPPost(imageUrl, sendMap);
	// if (imageResponseString.equals(RESULT_FAIL))
	// return false;
	// else
	// return true;
	// }
	//
	// // public String registerExpeditionPoint(double lat, double lng, int
	// expedition) {
	// // String result = doHTTPGET(server + "/api/addExpeditionPoint?authKey="
	// // + authKey + "&lat=" + lat + "&lng=" + lng + "&expedition="
	// // + expedition);
	// // return result;
	// // }
	//
	//
	// /**
	// * Sends a GPS point and associated data to the Posit server. Called from
	// * Tracker Activity or TrackerBackgroundService.
	// */
	// public String registerExpeditionPoint(double lat, double lng, double alt,
	// int swath, int expedition, long time) {
	// //long swath, int expedition) {
	// // if (Utils.debug)
	// // Log.i(TrackerActivity.TAG, "Communicator, registerExpeditionPoint " +
	// lat + " " + lng + " " + time);
	// HashMap<String, String> sendMap = new HashMap<String, String>();
	// addRemoteIdentificationInfo(sendMap);
	// String addExpeditionUrl = server + "/api/addExpeditionPoint?authKey="
	// + authKey;
	// sendMap.put(PositDbHelper.GPS_POINT_LATITUDE, "" + lat);
	// sendMap.put(PositDbHelper.GPS_POINT_LONGITUDE, lng + "");
	// sendMap.put(PositDbHelper.GPS_POINT_ALTITUDE, "" + alt);
	// sendMap.put(PositDbHelper.GPS_POINT_SWATH, "" + swath);
	// sendMap.put(PositDbHelper.EXPEDITION, expedition + "");
	// sendMap.put(PositDbHelper.GPS_TIME, time + "");
	// String response = doHTTPPost(addExpeditionUrl, sendMap);
	// // if (Utils.debug) {
	// // Log.i(TrackerActivity.TAG,
	// "Communicator, registerExpeditionPoint, response: " +
	// addExpeditionResponseString);
	// // }
	// return response;
	// }
	//
	// /**
	// * Registers a new expedition with the server.
	// * @param projectId Posit's current project id.
	// * @return Returns the expedition number received from the server or -1 if
	// something
	// * goes wrong.
	// */
	// public int registerExpeditionId(int projectId) {
	// HashMap<String, String> sendMap = new HashMap<String, String>();
	// addRemoteIdentificationInfo(sendMap);
	// String addExpeditionUrl = server + "/api/addExpedition?authKey="
	// + authKey;
	// sendMap.put("projectId", "" + projectId);
	// String response = doHTTPPost(addExpeditionUrl, sendMap);
	// Log.d(TAG,"registerExpeditionId response = " + response);
	// // if (Utils.debug) {
	// // Log.i(TrackerActivity.TAG,
	// "Communicator, registerExpeditionId, response: "
	// // + addExpeditionResponseString);
	// // }
	// // The server should return an expedition number if everything goes ok.
	// If
	// // an error occurs, it will return an error message that cannot parse to
	// an int
	// // which will cause an exception here.
	// try {
	// Integer i = Integer.parseInt(response);
	// return i;
	// } catch (NumberFormatException e) {
	// Log.e(TrackerActivity.TAG,
	// "Communicator, registerExpeditionId, Invalid response received");
	// return -1;
	// }
	// }
}