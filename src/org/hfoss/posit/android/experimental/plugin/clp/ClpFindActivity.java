/**
 * 
 */
package org.hfoss.posit.android.experimental.plugin.clp;

import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.activity.FindActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;


/**
 * FindActivity subclass for Outside In plugin.
 * 
 */
public class ClpFindActivity extends FindActivity {

	private static final String TAG = "ClpFindActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		
		// Change prompt
		TextView tv = (TextView)findViewById(R.id.nameTextView);
		tv.setText(this.getString(R.string.namePrompt));
	}

	@Override
	protected void initializeListeners() {
		super.initializeListeners();
	}


	public void onClick(View v) {
		super.onClick(v);
	}

}
