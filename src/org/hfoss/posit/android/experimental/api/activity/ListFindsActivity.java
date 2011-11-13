package org.hfoss.posit.android.experimental.api.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hfoss.posit.android.experimental.Constants;
import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.database.DbHelper;
import org.hfoss.posit.android.experimental.api.database.DbManager;
import org.hfoss.posit.android.experimental.plugin.FindPluginManager;
import org.hfoss.posit.android.experimental.plugin.FunctionPlugin;
import org.hfoss.posit.android.experimental.sync.SyncActivity;
import org.hfoss.posit.android.experimental.sync.SyncAdapter;

import com.j256.ormlite.android.apptools.OrmLiteBaseListActivity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
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
	private ArrayList<FunctionPlugin> mListMenuPlugins = null;

	private static final int CONFIRM_DELETE_DIALOG = 0;
	List<? extends Find> finds;
	
	protected static FindsListAdapter mAdapter = null;
	
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
		mListMenuPlugins = FindPluginManager.getFunctionPlugins(FindPluginManager.LIST_MENU_EXTENSION);
		Log.i(TAG, "# of List menu plugins = " + mListMenuPlugins.size());
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
		mAdapter = (FindsListAdapter) setUpAdapter();
		fillList(mAdapter);
	}

	public void onGetChangedFindsResult(String finds) {
		Log.i(TAG,"Got changed finds: " + finds);
	}
	/**
	 * Called in onResume() and gets all of the finds in the database and puts
	 * them in an adapter. Override for a custom adapter/layout for this
	 * Activity.
	 */
	protected ListAdapter setUpAdapter() {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int projectId = prefs.getInt(getString(R.string.projectPref), 0);
		
		finds = this.getHelper().getFindsByProjectId(projectId);

		int resId = getResources().getIdentifier(
				FindPluginManager.mFindPlugin.mListFindLayout, "layout", getPackageName());

		FindsListAdapter adapter = new FindsListAdapter(this, resId, finds);

		return adapter;
	}

	/**
	 * Puts the items from the DB table into the rows of the view.
	 */
	private void fillList(ListAdapter adapter) {
		setListAdapter(adapter);

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(parent.getContext(),
						FindPluginManager.mFindPlugin.getmFindActivityClass());
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
		if (mListMenuPlugins.size() > 0) {
			for (FunctionPlugin plugin: mListMenuPlugins) {
				MenuItem item = menu.add(plugin.getmMenuTitle());
				item.setIcon(android.R.drawable.ic_menu_mapmode);				
			}
		}
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
			startActivityForResult(new Intent(this,SyncActivity.class), 0);
//			Log.i(TAG, "Sync finds menu item");
//			AccountManager manager = AccountManager.get(this);
//			Account[] accounts = manager
//					.getAccountsByType(SyncAdapter.ACCOUNT_TYPE);
//			
//			// Just pick the first account for now.. TODO: make this work for
//			// multiple accounts of same type?
//			Bundle extras = new Bundle();
//			
//			// Avoids index-out-of-bounds error if no such account
//			// Must be a better way to do this?
//			if (accounts.length != 0) {
//				Log.i(TAG, "Requesting sync");
//				if (!ContentResolver.getSyncAutomatically(accounts[0],getResources().getString(R.string.contentAuthority))) {
//					Log.i(TAG, "Sync not requested. " + SyncAdapter.ACCOUNT_TYPE + " is not ON");
//					Toast.makeText(this, "Sync not requested: " + SyncAdapter.ACCOUNT_TYPE + " is not ON", Toast.LENGTH_LONG).show();
//				} else {
//				ContentResolver
//				.requestSync(
//						accounts[0],
//						getResources().getString(R.string.contentAuthority),
//						extras);
////				mAdapter.notifyDataSetChanged();
//				Toast.makeText(this, "Sync requested", Toast.LENGTH_LONG).show();
//				}
//			} else {
//				Log.i(TAG, "Sync not requested. Unable to get " + SyncAdapter.ACCOUNT_TYPE);
//				Toast.makeText(this, "Sync error: Unable to get " + SyncAdapter.ACCOUNT_TYPE, Toast.LENGTH_LONG).show();
//			}
			break;
		case R.id.map_finds_menu_item:
			Log.i(TAG, "Map finds menu item");
			startActivity(new Intent(this, MapFindsActivity.class));
			break;

		case R.id.delete_finds_menu_item:
			Log.i(TAG, "Delete all finds menu item"); 
			showDialog(CONFIRM_DELETE_DIALOG);
			break;
			
		default:
			if (mListMenuPlugins.size() > 0){
				for (FunctionPlugin plugin: mListMenuPlugins) {
					if (item.getTitle().equals(plugin.getmMenuTitle()))
						startActivity(new Intent(this, plugin.getmMenuActivity()));
				}
			}

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
	
//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		// TODO Auto-generated method stub
//		Log.i(TAG, "Notify data set changed");
//		this.mAdapter.notifyDataSetChanged();
//		super.onActivityResult(requestCode, resultCode, data);
//	}
	
	public static void syncCallback() {
		Log.i(TAG, "Notified sync callback");
		mAdapter.notifyDataSetChanged();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONFIRM_DELETE_DIALOG:
			return new AlertDialog.Builder(this).setIcon(R.drawable.alert_dialog_icon)
					.setTitle(R.string.confirm_delete)
					.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// User clicked OK so do some stuff
							if (deleteAllFind()) {
								Toast.makeText(ListFindsActivity.this, R.string.deleted_from_database, Toast.LENGTH_SHORT)
										.show();
								finish();
							} else
								Toast.makeText(ListFindsActivity.this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
						}
					}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// User clicked cancel so do nothing
						}
					}).create();
		default:
			return null;
		}
	}
	
	protected boolean deleteAllFind() {
		int rows = 0;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int projectId = prefs.getInt(getString(R.string.projectPref), 0);
		rows = getHelper().deleteAll(projectId);
		return rows >= 0;

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
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged();
		}


		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				int resId = getResources().getIdentifier(
						FindPluginManager.mFindPlugin.mListFindLayout, "layout",
						getPackageName());
				v = vi.inflate(resId, null);
			}
			Find find = items.get(position);
			if (find != null) {
				TextView tv = (TextView) v.findViewById(R.id.name);
				tv.setText(find.getName());
				tv = (TextView) v.findViewById(R.id.latitude);
				tv.setText(getText(R.string.latitude) + " " + String.valueOf(find.getLatitude()));
				tv = (TextView) v.findViewById(R.id.longitude);
				tv.setText(getText(R.string.longitude) + " " + String.valueOf(find.getLongitude()));
				tv = (TextView) v.findViewById(R.id.id);
				tv.setText(Integer.toString(find.getId()));
				tv = (TextView) v.findViewById(R.id.time);
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				tv.setText(getText(R.string.timeLabel)+ " " + dateFormat.format(find.getTime()));
				tv = (TextView) v.findViewById(R.id.status);
				tv.setText(find.getStatusAsString());
				tv = (TextView) v.findViewById(R.id.description_id);
				String description = find.getDescription();
				if (description.length() <= 50) {
					tv.setText(description);
				} else {
					tv.setText(description.substring(0,49)+" ...");
				}
			}
			return v;
		}
	}

}
