/**
 * 
 */
package org.hfoss.posit.android.api.plugin.csv;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.activity.FindActivity;
import org.hfoss.posit.android.R;

import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.LinearLayout;


/**
 * FindActivity subclass for CsvFind plugin.
 */
public class CsvFindActivity extends FindActivity {

	private static final String TAG = "CsvFindActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
		super.onCreate(savedInstanceState);	
		
		if (getIntent().getAction().equals(CsvListFindsActivity.ACTION_CSV_FINDS)) {
			int id = getIntent().getIntExtra(CsvListFindsActivity.ACTION_CSV_FINDS, 0);
			displayContentInView(CsvListFindsActivity.getFind(id));
		}
	}

	@Override
	protected void initializeListeners() {
		super.initializeListeners();
	}

	public void onClick(View v) {
		super.onClick(v);
	}
	
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		//return super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	protected void displayContentInView(Find find) {
		super.displayContentInView(find);	
		
		
		this.setContentView(R.layout.csv_add_find);
		
		View v = findViewById(R.id.csvAddLinearLayout);
		
		TextView newTV = new TextView(this);
		newTV.setText((((CsvFind)find).getClosing()));
		newTV.setPadding(4, 8, 0, 0);
//		newTV.setId(5);
		newTV.setLayoutParams(new LayoutParams(
		            LayoutParams.FILL_PARENT,
		            LayoutParams.WRAP_CONTENT));
		((LinearLayout)v).addView(newTV);
		
		newTV = new TextView(this);
		newTV.setText((((CsvFind)find).getRates()));
		newTV.setPadding(4, 8, 0, 0);
//		newTV.setId(5);
		newTV.setLayoutParams(new LayoutParams(
		            LayoutParams.FILL_PARENT,
		            LayoutParams.WRAP_CONTENT));
		((LinearLayout)v).addView(newTV);
		
		newTV = new TextView(this);
		String specials = (((CsvFind)find).getSpecials()).trim();
		if (specials.length() == 0)
			newTV.setText("");
		else 
			newTV.setText("Specials: " + specials);

		newTV.setPadding(4, 8, 0, 0);
//		newTV.setId(5);
		newTV.setLayoutParams(new LayoutParams(
		            LayoutParams.FILL_PARENT,
		            LayoutParams.WRAP_CONTENT));
		((LinearLayout)v).addView(newTV);
		
		TextView tv = (TextView)findViewById(R.id.nameValueText);
		tv.setText(((CsvFind)find).getName());
		
		tv = (TextView)findViewById(R.id.descriptionValueText);
		tv.setText(((CsvFind)find).getFullAddress());
        Linkify.addLinks( tv, Linkify.MAP_ADDRESSES);	
		
		tv = (TextView)findViewById(R.id.latitudeValueTextView);
		tv.setText(""+ ((CsvFind)find).getLatitude());	

		tv = (TextView)findViewById(R.id.longitudeValueTextView);
		tv.setText(""+ ((CsvFind)find).getLongitude());
		
		tv = (TextView)findViewById(R.id.guidValueTextView);
		tv.setText(""+ ((CsvFind)find).getGuid());
		
		tv = (TextView)findViewById( R.id.urlValueTextView );
        // make sure that setText call comes BEFORE Linkify.addLinks call
        tv.setText(((CsvFind)find).getUrl());
        Linkify.addLinks( tv, Linkify.WEB_URLS );
		
		tv = (TextView)findViewById( R.id.phoneValueTextView );
        tv.setText(((CsvFind)find).getPhone());
        Linkify.addLinks( tv, Linkify.PHONE_NUMBERS );	
	}

}
