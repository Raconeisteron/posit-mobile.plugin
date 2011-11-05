package org.hfoss.posit.android.experimental.api.activity;

import java.util.List;

import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.database.DbManager;
import org.hfoss.posit.android.experimental.plugin.FindPluginManager;
import org.hfoss.posit.android.experimental.sync.SyncAdapter;

import com.j256.ormlite.android.apptools.OrmLiteBaseListActivity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
		setContentView(R.layout.list_finds);
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
	 * Called in onResume() and gets all of the finds in the database and puts
	 * them in an adapter. Override for a custom adapter/layout for this
	 * Activity.
	 */
	protected ListAdapter setUpAdapter() {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int projectId = prefs.getInt(getString(R.string.projectPref), 0);
		
		List<? extends Find> list = this.getHelper().getFindsByProjectId(projectId);

		int resId = getResources().getIdentifier(
				FindPluginManager.mListFindLayout, "layout", getPackageName());

		FindsListAdapter adapter = new FindsListAdapter(this, resId, list);
		if (adapter == null) {
			Log.i(TAG, "Adapter = null");
		}
		return adapter;
	}

	/**
	 * Puts the items from the DB table into the rows of the view.
	 */
	private void fillList(ListAdapter adapter) {
		
//		if (adapter.isEmpty()) {
//			setContentView(R.layout.list_finds);
//			return;
//		}

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
	 * Creates the menus for this activity.
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_finds_menu, menu);
		return true;
	}

	/**
	 * Handles the various menu item actions.
	 * 
	 * @param featureId
	 *            is unused
	 * @param item
	 *            is the MenuItem selected by the user
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Log.i(TAG, "onMenuitemSelected()");

		Intent intent;
		switch (item.getItemId()) {
		case R.id.sync_finds_menu_item:
			Log.i(TAG, "Sync finds menu item");
			AccountManager manager = AccountManager.get(this);
			Account[] accounts = manager
					.getAccountsByType(SyncAdapter.ACCOUNT_TYPE);
			// Just pick the first account for now.. TODO: make this work for
			// multiple accounts of same type?
			Bundle extras = new Bundle();
			ContentResolver
					.requestSync(
							accounts[0],
							getResources().getString(R.string.contentAuthority),
							extras);
			break;
		// case R.id.save_find_menu_item:
		// saveFind();
		// break;
		//
		// case R.id.delete_find_menu_item:
		// showDialog(CONFIRM_DELETE_DIALOG);
		// break;
		//
		// default:
		// return false;
		}
		return true;
	} // onMenuItemSelected

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

				int resId = getResources().getIdentifier(
						FindPluginManager.mListFindLayout, "layout",
						getPackageName());
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
