package org.hfoss.posit.android.sync;

import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

/**
 * This class is used to handle sync requests with the server.
 * @author ericenns
 *
 */
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
			} 
			else {
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
