/**
 * 
 */
package org.hfoss.posit.android.experimental.plugin.outsidein;

import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.activity.FindActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.CheckBox;


/**
 * FindActivity subclass for Outside In plugin.
 * 
 */
public class OutsideInFindActivity extends FindActivity {

	private static final String TAG = "OutsideInFindActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
		super.onCreate(savedInstanceState);

	}

	@Override
	protected void initializeListeners() {
		super.initializeListeners();
	}

	@Override
	protected Find retrieveContentFromView() {
		OutsideInFind find =  (OutsideInFind)super.retrieveContentFromView();
		String value; //used to get the string from the textbox
		
		EditText eText = (EditText) findViewById(R.id.syringesInEditText);
		//If no value is supplied, set it to 0.
		if(eText.getText().toString().equals("")){
			value = "0";
		}
		else{
			value = eText.getText().toString();
		}
		find.setSyringesIn(Integer.parseInt(value));

		eText = (EditText) findViewById(R.id.syringesOutEditText);
		if(eText.getText().toString().equals("")){
			value = "0";
		}
		else{
			value = eText.getText().toString();
		}
		find.setSyringesOut(Integer.parseInt(value));

		CheckBox checkBox = (CheckBox) findViewById(R.id.isNewCheckBox);
		find.setNew(checkBox.isChecked());

		return find;
	}

	@Override
	protected void displayContentInView(Find find) {
		OutsideInFind oiFind = (OutsideInFind)find;
		EditText et = (EditText)findViewById(R.id.guidEditText);
		et.setText(oiFind.getGuid());
		
		et = (EditText)findViewById(R.id.syringesInEditText);
		et.setText(Integer.toString(oiFind.getSyringesIn()));
		
		et = (EditText)findViewById(R.id.syringesOutEditText);
		et.setText(Integer.toString(oiFind.getSyringesOut()));
		
		CheckBox cb = (CheckBox)findViewById(R.id.isNewCheckBox);
		cb.setChecked(oiFind.isNew());
		
	}

	public void onClick(View v) {
		super.onClick(v);
	}

}
