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

package org.hfoss.posit.android.api.authentication;

import java.util.ArrayList;
import java.util.HashMap;

import org.hfoss.posit.android.sync.Communicator;
import org.hfoss.posit.android.sync.SyncAdapter;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * This class is an implementation of AbstractAccountAuthenticator for
 * authenticating accounts in the com.example.android.samplesync domain.
 * 
 * The only method we really need to implement is addAccount, which 
 * returns an Intent that the system will use to display the login dialog 
 * to the user. 
 * 
 * This implementation below will launch our app's main launcher activity 
 * with an action of 'fm.last.android.sync.LOGIN' and an extra containing the 
 * AccountAuthenticatorResponse object we use to pass data back to 
 * the system after the user has logged in.

 */
class Authenticator extends AbstractAccountAuthenticator {

    private static final String TAG = "Authenticator";
    
	// Authentication Service context
    private final Context mContext;

    public Authenticator(Context context) {
        super(context);
        mContext = context;
    }

    /**
     *  The user has requested to add a new account to the system.  
     *  We return an intent that will launch our login screen if the 
     *  user has not logged in yet. Ootherwise our activity will just 
     *  pass the user's credentials on to the account manager.
     */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
        String authTokenType, String[] requiredFeatures, Bundle options) {

    	Log.i(TAG, "addAccount()");
        final Bundle bundle = new Bundle();

        //  The login activity to launch
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
   
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
        Bundle options) {
    	Log.i(TAG, "confirmCredentials()");

        if (options != null && options.containsKey(AccountManager.KEY_PASSWORD)) {
            final String password = options.getString(AccountManager.KEY_PASSWORD);
            final String authKey = onlineConfirmPassword(account.name, password);
            final Bundle result = new Bundle();
            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, authKey != null);
            return result;
        }
        // Launch AuthenticatorActivity to confirm credentials
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);
        intent.putExtra(AuthenticatorActivity.PARAM_CONFIRM_CREDENTIALS, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
        String authTokenType, Bundle loginOptions) {
    	Log.i(TAG, "getAuthToken()");
        if (!authTokenType.equals(SyncAdapter.AUTHTOKEN_TYPE)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }
        final AccountManager am = AccountManager.get(mContext);
        final String password = am.getPassword(account);
        if (password != null) {
            final String authKey = onlineConfirmPassword(account.name, password);
            if (authKey != null) {
                final Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, SyncAdapter.ACCOUNT_TYPE);
                result.putString(AccountManager.KEY_AUTHTOKEN, authKey);
                return result;
            }
        }
        // the password was missing or incorrect, return an Intent to an
        // Activity that will prompt the user for the password.
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);
        intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
    	Log.i(TAG, "getAuthTokenLabel)");

        if (SyncAdapter.AUTHTOKEN_TYPE.equals(authTokenType)) {
            return "I dont know what this is supposed to return";
        }
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
        String[] features) {

        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    
	/**
	 * Handles messages and results received from the background thread.
	 */
	final Handler handler = new Handler() { 
		@SuppressWarnings("unchecked")
		public void handleMessage(Message msg) { 
			if (msg.what == Communicator.SUCCESS) {
				Log.i(TAG, "Handler: login success");
			} else {
				Log.i(TAG, "Handler: login failure");
			}
		} 
	}; 
    
    /**
     * Validates user's password on the server
     * @return the authKey from the server
     */
    private String onlineConfirmPassword(String username, String password) {
    	Log.i(TAG, "onlineConfirmPassword()");
    	TelephonyManager telephonyManager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
    	String imei = telephonyManager.getDeviceId();
        return Communicator
            .loginUser(username, password, imei, handler, mContext);
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
        String authTokenType, Bundle loginOptions) {
    	Log.i(TAG, "updateCredentials()");

        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);
        intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
        intent.putExtra(AuthenticatorActivity.PARAM_CONFIRM_CREDENTIALS, false);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }
}