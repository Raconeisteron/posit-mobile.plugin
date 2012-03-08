package org.hfoss.posit.android.plugin.clp;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.fragment.FindFragment;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class ClpFindFragment extends FindFragment {

	private static final String TAG = "ClpFindFragment";

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
		super.onActivityCreated(savedInstanceState);
		
		// Change prompt
		TextView tv = (TextView)getView().findViewById(R.id.nameTextView);
		tv.setText(this.getString(R.string.namePrompt));
	}
}
