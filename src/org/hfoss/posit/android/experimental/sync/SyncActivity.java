package org.hfoss.posit.android.experimental.sync;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.activity.ListFindsActivity;
import org.hfoss.posit.android.experimental.api.database.DbManager;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import org.hfoss.posit.android.experimental.plugin.outsidein.OutsideInFind;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

public class SyncActivity extends OrmLiteBaseActivity<DbManager> {

	public static final String TAG = "SyncActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume()");

		AccountManager manager = AccountManager.get(this);
		Account[] accounts = manager.getAccountsByType(SyncAdapter.ACCOUNT_TYPE);
		
		// Just pick the first account for now.. TODO: make this work for
		// multiple accounts of same type?
		Bundle extras = new Bundle();
		
		
		// Avoids index-out-of-bounds error if no such account
		// Must be a better way to do this?
		if (accounts.length != 0) {
		
			if (!Communicator.isServerReachable(this)) {
				Log.i(TAG, "Sync not requested. Server not reachable");
				Toast.makeText(this, "Sync not requested. Server not reachable", Toast.LENGTH_LONG).show();
				finish();
				return;
			}
			Log.i(TAG, "Requesting sync");
			if (!ContentResolver.getSyncAutomatically(accounts[0],getResources().getString(R.string.contentAuthority))) {
				Log.i(TAG, "Sync not requested. " + SyncAdapter.ACCOUNT_TYPE + " is not ON");
				Toast.makeText(this, "Sync not requested: " + SyncAdapter.ACCOUNT_TYPE + " is not ON", Toast.LENGTH_LONG).show();
			} else {
			ContentResolver
			.requestSync(
					accounts[0],
					getResources().getString(R.string.contentAuthority),
					extras);
			Toast.makeText(this, "Sync requested", Toast.LENGTH_LONG).show();
			setResult(RESULT_OK);
			}
		} else {
			Log.i(TAG, "Sync not requested. Unable to get " + SyncAdapter.ACCOUNT_TYPE);
			Toast.makeText(this, "Sync error: Unable to get " + SyncAdapter.ACCOUNT_TYPE, Toast.LENGTH_LONG).show();
		}
		finish();
	}


}
