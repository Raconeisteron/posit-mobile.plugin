package org.hfoss.posit.android.sync;

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
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;
import org.apache.http.ParseException;
import java.io.IOException;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String TAG = "SyncAdapter";
	private final AccountManager mAccountManager;
	private final Context mContext;


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
	
	/**
	 * This is called automatically. It can be called by starting SyncActivity
	 */
	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider,
			SyncResult syncResult) {

		Log.i(TAG, "In onPerformSync()");
		String authToken = null;
		
		try {
			// use the account manager to request the credentials
			Log.i(TAG, "Trying to retrieve authToken");
			authToken = mAccountManager.blockingGetAuthToken(account, AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
			Log.i(TAG, "auth token: " + authToken);

			SyncServer syncServer = new SyncServer( mContext );
			syncServer.sync( authToken );
		
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