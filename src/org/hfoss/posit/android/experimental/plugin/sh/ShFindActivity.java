/**
 * 
 */
package org.hfoss.posit.android.experimental.plugin.sh;

import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.activity.FindActivity;
import org.hfoss.posit.android.experimental.plugin.acdivoca.AcdiVocaFind;
import org.hfoss.posit.android.experimental.plugin.outsidein.OutsideInFind;

import android.content.ContentValues;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;


/**
 * FindActivity subclass for Outside In plugin.
 * 
 */
public class ShFindActivity extends FindActivity {

	private static final String TAG = "ShFindActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
		super.onCreate(savedInstanceState);	
		
		((RadioButton)findViewById(R.id.pickupRadio)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.dropoffRadio)).setOnClickListener(this);

	}

	@Override
	protected void initializeListeners() {
		super.initializeListeners();
	}

	public void onClick(View v) {
		super.onClick(v);
	}
	
	/**
	 * Retrieves values from the ShFind fields and stores them in a Find instance. 
	 * This method is invoked from the Save menu item. It also marks the find
	 * 'unsynced' so it will be updated to the server.
	 * 
	 * @return a new Find object with data from the view.
	 */
	@Override
	protected Find retrieveContentFromView() {
		ShFind find =  (ShFind)super.retrieveContentFromView();
		
		RadioButton rb1 = (RadioButton)findViewById(R.id.pickupRadio);
		RadioButton rb2 = (RadioButton)findViewById(R.id.dropoffRadio);
		if (rb1 != null && rb1.isChecked()){
			find.setStopType(ShFind.PICKUP);
		}
		if (rb2 != null && rb2.isChecked()){
			find.setStopType(ShFind.DROPOFF);
		}

		return find;
	}

	@Override
	protected void displayContentInView(Find find) {
		super.displayContentInView(find);
		
		RadioButton rb1 = (RadioButton)findViewById(R.id.pickupRadio);
		int val = ((ShFind)find).getStopType();
		if (val == ShFind.PICKUP)
			rb1.setChecked(true);
		RadioButton rb2 = (RadioButton)findViewById(R.id.dropoffRadio);
		if (val == ShFind.DROPOFF)
			rb2.setChecked(true);		
	}

}
