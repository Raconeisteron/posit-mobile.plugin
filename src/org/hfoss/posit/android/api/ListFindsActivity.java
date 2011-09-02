package org.hfoss.posit.android.api;

import java.sql.SQLException;
import java.util.List;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaDbManager;
import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaFind;
import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaLocaleManager;
import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaMessage;
import org.hfoss.posit.android.plugin.acdivoca.AppControlManager;
import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaUser.UserType;

import com.j256.ormlite.android.apptools.OrmLiteBaseListActivity;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public abstract class ListFindsActivity extends OrmLiteBaseListActivity<DbManager> { 

	private static final String TAG = "ListFindsActivity";
	
	/** 
	 * Called when the Activity starts.
	 *  @param savedInstanceState contains the Activity's previously
	 *   frozen state.  In this case it is unused.
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	/** 
	 * Called when the activity is ready to start 
	 *  interacting with the user. It is at the top of the Activity
	 *  stack.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG,"onResume()");
		fillList();	
	}
	
	/**
	 * Puts the items from the DB table into the rows of the view. Note that
	 *  once you start managing a Cursor, you cannot close the DB without 
	 *  causing an error.
	 */
	private void fillList() {

		List<Find> list = this.getHelper().getAllFinds();
	
		if (list.size() == 0) {
			setContentView(R.layout.list_finds);
			return;			
		}
		
		FindsListAdapter adapter = new FindsListAdapter(this, R.layout.list_row, list);
		setListAdapter(adapter); 
	}
	
	/**
	 * Adapter for displaying beneficiaries. 
	 *
	 * @param <AcdiVocaFind>
	 */
	private class FindsListAdapter extends ArrayAdapter<Find> {
		private List<Find> items;
		
		public FindsListAdapter(Context context, int textViewResourceId, List<Find> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}
		
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.list_row, null);
                }
                Find find = items.get(position);
                if (find != null) {
                        TextView tv = (TextView) v.findViewById(R.id.name);
                        tv.setText(find.getName());
                        tv = (TextView) v.findViewById(R.id.latitude);
                        tv.setText(String.valueOf(find.getLatitude()));
                        tv = (TextView) v.findViewById(R.id.longitude);
                        tv.setText(String.valueOf(find.getLongitude()));
                        tv = (TextView) v.findViewById(R.id.id);
                        tv.setText(find.getId());
                }
                return v;
        }
	}
	
}
