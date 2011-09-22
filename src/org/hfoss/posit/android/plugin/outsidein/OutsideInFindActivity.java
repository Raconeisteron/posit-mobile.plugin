/**
 * 
 */
package org.hfoss.posit.android.plugin.outsidein;

import java.sql.SQLException;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.activity.FindActivity;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.plugin.FindPluginManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.Button;

/**
 * FindActivity subclass for Outside In plugin.
 * 
 */
public class OutsideInFindActivity extends FindActivity {

	private static final String TAG = "OutsideInFindActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
//		if (savedInstanceState != null)
//			savedInstanceState.putBoolean(FindPluginManager.IS_PLUGIN, true);
//		else {
//			savedInstanceState = new Bundle();
//			savedInstanceState.putBoolean(FindPluginManager.IS_PLUGIN, true);
//		}
		super.onCreate(savedInstanceState);
//		Bundle extras = getIntent().getExtras();
//		//R.layout.add_find
//		int resId = getResources().getIdentifier("R.layout.add_find",
//			    "id", getPackageName());
//		setContentView(resId);
//		if (extras != null) {
//			// if (getIntent().getAction().equals(Intent.ACTION_INSERT))
//			if (getIntent().getAction().equals(Intent.ACTION_EDIT)) {
//				Find find = this.getHelper().getFindById(extras.getInt(Find.ORM_ID));
////				OutsideInDbManager manager = (OutsideInDbManager)getHelper();
////				Find find=new Find();
////				try {
////					find = manager.getOutsideInFindDao().queryForId(extras.getInt(Find.ORM_ID));
////				} catch (SQLException e) {
////					// TODO Auto-generated catch block
////					e.printStackTrace();
////				}
//				displayContentInView(find);
//			}
//		}
//
//		initializeListeners();

	}

	@Override
	protected void initializeListeners() {
		super.initializeListeners();
	}

	@Override
	protected Find retrieveContentFromView() {
		OutsideInFind find =  (OutsideInFind)super.retrieveContentFromView();
	
		EditText eText = (EditText) findViewById(R.id.syringesInEditText);
		String value = eText.getText().toString();
		find.setSyringesIn(Integer.parseInt(value));

		eText = (EditText) findViewById(R.id.syringesOutEditText);
		value = eText.getText().toString();
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
//		switch (v.getId()) {
//		case R.id.saveButton:
//			int success = 0;
//			OutsideInFind find = (OutsideInFind)retrieveContentFromView();
//			//OutsideInDbManager helper = (OutsideInDbManager)this.getHelper();
//			//success = find.insertDumb(helper.getOutsideInFindDao());
//			success = find.insert(this.getHelper().getFindDao());
//			if (success > 0){
//				Log.i(TAG, "Find inserted successfully: " + find);
//			}
//			else
//				Log.e(TAG, "Find not inserted: " + find);
//			finish();
//			break;
//		}
	}

}
