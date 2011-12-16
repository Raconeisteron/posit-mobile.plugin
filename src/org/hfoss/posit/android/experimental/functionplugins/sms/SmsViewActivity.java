package org.hfoss.posit.android.experimental.functionplugins.sms;

import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.database.DbManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

public class SmsViewActivity extends OrmLiteBaseActivity<DbManager> {
	
	private static final String TAG = "SmsViewActivity";
	
	protected TextView mFindView;
	protected Button mSaveButton;
	protected Button mDismissButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sms_view);
		mFindView = (TextView) findViewById(R.id.smsFindView);
		mSaveButton = (Button) findViewById(R.id.smsSaveButton);
		mDismissButton = (Button) findViewById(R.id.smsDismissButton);
		
		// Get Find bundle and display in text view
		final Bundle bundle = getIntent().getBundleExtra("findbundle");
		StringBuilder builder = new StringBuilder();
		for (String key : bundle.keySet()) {
			Object val = bundle.get(key);
			if (val == null) {
				builder.append(key + ": null\n");
			} else {
				builder.append(key + ": " + val + "\n");
			}
		}
		mFindView.setText(builder.toString());
		
		// Listener for Save button click
		mSaveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Find find = new Find();
				find.updateObject(bundle);
				// Change project ID to reflect current project on this end
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				int projectId = prefs.getInt(getString(R.string.projectPref), 0);
				find.setProject_id(projectId);
				// Insert into database
				getHelper().insert(find);
				finish();
			}
		});
		// Listener for Dismiss button click
		mDismissButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// Get Find bundle and display in text view
		final Bundle bundle = getIntent().getBundleExtra("findbundle");
		StringBuilder builder = new StringBuilder();
		for (String key : bundle.keySet()) {
			Object val = bundle.get(key);
			if (val == null) {
				builder.append(key + ": null\n");
			} else {
				builder.append(key + ": " + val + "\n");
			}
		}
		mFindView.setText(builder.toString());
	}

	@Override
	protected void onNewIntent (Intent intent) {
		setIntent(intent);
	}
}
