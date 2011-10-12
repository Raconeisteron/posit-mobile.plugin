package org.hfoss.posit.android.experimental.sync;

/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.hfoss.posit.android.experimental.api.Find;
//import org.hfoss.posit.android.experimental.api.authentication.NetworkUtilities;
import org.hfoss.posit.android.experimental.api.database.DbHelper;
import org.json.JSONException;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String TAG = "SyncAdapter";

	private final AccountManager mAccountManager;

	private final Context mContext;

	private Communicator communicator;

	private Date mLastUpdated;

	/**
	 * Account type string.
	 */
	public static final String ACCOUNT_TYPE = "org.hfoss.posit.account";

	/**
	 * Authtoken type string.
	 */
	public static final String AUTHTOKEN_TYPE = "org.hfoss.posit.account";

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
		mAccountManager = AccountManager.get(context);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider,
			SyncResult syncResult) {

		List<? extends Find> finds;
		Log.i(TAG, "In onPerformSync() wowowpw");
		String authToken = null;
		try {
			// use the account manager to request the credentials
			// TODO: This is not the correct auth token. Its just the password.
			// We want it to use the auth token that the server generates for
			// us.
			authToken = mAccountManager.blockingGetAuthToken(account, AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
			// fetch updates from the sample service over the cloud
			// users = NetworkUtilities.fetchFriendUpdates(account, authtoken,
			// mLastUpdated);
			// update the last synced date.
			Log.i(TAG, "auth token: " + authToken);
			mLastUpdated = new Date();
			finds = DbHelper.getDbManager(mContext).getAllFinds();
			Communicator.sendFind(finds.get(0), "create", mContext, authToken);
			// update platform contacts.
			// Log.d(TAG, "Calling contactManager's sync contacts");
			// ContactManager.syncContacts(mContext, account.name, users);
			// // fetch and update status messages for all the synced users.
			// statuses = NetworkUtilities.fetchFriendStatuses(account,
			// authtoken);
			// ContactManager.insertStatuses(mContext, account.name, statuses);
		} catch (final AuthenticatorException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "AuthenticatorException", e);
		} catch (final OperationCanceledException e) {
			Log.e(TAG, "OperationCanceledExcetpion", e);
		} catch (final IOException e) {
			Log.e(TAG, "IOException", e);
			syncResult.stats.numIoExceptions++;
		} catch (final ParseException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "ParseException", e);
		}
	}
}