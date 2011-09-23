package org.hfoss.posit.android.api.activity;

import java.util.List;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.plugin.FindPluginManager;

import com.j256.ormlite.android.apptools.OrmLiteBaseListActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ListFindsActivity extends OrmLiteBaseListActivity<DbManager> {

	private static final String TAG = "ListFindsActivity";

	/**
	 * Called when the Activity starts.
	 * 
	 * @param savedInstanceState
	 *            contains the Activity's previously frozen state. In this case
	 *            it is unused.
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	/**
	 * Called when the activity is ready to start interacting with the user. It
	 * is at the top of the Activity stack.
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume()");
		fillList(setUpAdapter());
	}
	/**
	 * Called in onResume() and gets all of the finds in the database and puts them in an
	 * adapter.  Override for a custom adapter/layout for this Activity.
	 */
	protected ListAdapter setUpAdapter() {

		List<? extends Find> list = this.getHelper().getAllFinds();

		int resId = getResources().getIdentifier(FindPluginManager.mListFindLayout,
			    "layout", getPackageName());
		
		FindsListAdapter adapter = new FindsListAdapter(this,
				resId, list);

		return adapter;
	}
	/**
	 * Puts the items from the DB table into the rows of the view.
	 */
	private void fillList(ListAdapter adapter) {

		if (adapter.isEmpty()) {
			setContentView(R.layout.list_finds);
			return;
		}

		setListAdapter(adapter);

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(parent.getContext(),
						FindPluginManager.getInstance().getFindActivityClass());
				TextView tv = (TextView) view.findViewById(R.id.id);
				int ormId = Integer.parseInt((String) tv.getText());
				intent.putExtra(Find.ORM_ID, ormId);
				intent.setAction(Intent.ACTION_EDIT);
				startActivity(intent);
			}
		});
	}

	/**
	 * Adapter for displaying finds.
	 * 
	 * @param <Find>
	 */
	protected class FindsListAdapter extends ArrayAdapter<Find> {
		protected List<? extends Find> items;

		public FindsListAdapter(Context context, int textViewResourceId,
				List list) {
			super(context, textViewResourceId, list);
			this.items = list;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				
				int resId = getResources().getIdentifier(FindPluginManager.mListFindLayout,
					    "layout", getPackageName());
				v = vi.inflate(resId, null);
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
				tv.setText(Integer.toString(find.getId()));

			}
			return v;
		}
	}

}
