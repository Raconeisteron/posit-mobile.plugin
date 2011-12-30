package org.hfoss.posit.android.api.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.hfoss.posit.android.Constants;
import org.hfoss.posit.android.api.Camera;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.service.LocationService;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.plugin.FindPluginManager;
import org.hfoss.posit.android.plugin.FunctionPlugin;
import org.hfoss.posit.android.sync.SyncActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OrmLiteBaseListActivity;

public class ListFindsActivity extends OrmLiteBaseListActivity<DbManager> {

	private static final String TAG = "ListFindsActivity";
	protected ArrayList<FunctionPlugin> mListMenuPlugins = null;

	private static final int CONFIRM_DELETE_DIALOG = 0;
	public static final String ACTION_LIST_FINDS = "list_finds";

	private static List<? extends Find> finds;
	
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
				int id = getResources().getIdentifier(
						plugin.getmMenuIcon(), "drawable", "org.hfoss.posit.android");
				Log.i(TAG, "icon =  " + plugin.getmMenuIcon() + " id =" + id);
				item.setIcon(id);
				//item.setIcon(android.R.drawable.ic_menu_mapmode);				
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
			startActivityForResult(new Intent(this, SyncActivity.class), 0);
			break;
		case R.id.map_finds_menu_item:
			Log.i(TAG, "Map finds menu item");
			intent = new Intent();
			intent.setAction(ACTION_LIST_FINDS);
			intent.setClass(this, MapFindsActivity.class);			
			startActivity(intent);
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
		}
		return true;
	} // onMenuItemSelected
	
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
								finish();
							}
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
	
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int projectId = prefs.getInt(getString(R.string.projectPref), 0);
		boolean success = getHelper().deleteAll(projectId);
		if (success) {
			Toast.makeText(ListFindsActivity.this, R.string.deleted_from_database, Toast.LENGTH_SHORT).show();
			/* To-Do Begins */	
			this.startService(new Intent(this, LocationService.class));
			/* To-Do Ends */
		} else {
			Toast.makeText(ListFindsActivity.this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
		}
		return success;
	}

	/**
	 * Returns a list of Finds.
	 */
	public static List<? extends Find> getFinds() {
		return finds;
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
			
			/* To-Do Begins */
			// Initialize the alarm clock icon here so
			// it can be referenced in the entire function
			// and be set for each list row item
			ImageView alarmIcon = new ImageView(parent.getContext());
			/* To-Do Ends */
			
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				int resId = getResources().getIdentifier(
						FindPluginManager.mFindPlugin.mListFindLayout, "layout",
						getPackageName());
				v = vi.inflate(resId, null);
				
				/* To-Do Begins */
				// Add Reminder alarm clock icon in every list row,
				// and set its visibility based on if a find has a
				// reminder attached or not
				alarmIcon.setImageResource(R.drawable.reminder_alarm);
				RelativeLayout rl = (RelativeLayout) v.findViewById(R.id.list_row_rl);
				RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(25, 25);
				lp.addRule(RelativeLayout.BELOW, R.id.status);
				lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				lp.setMargins(0, 6, 0, 0);
				rl.addView(alarmIcon, lp);
				/* To-Do Ends */
			}
			Find find = items.get(position);
			if (find != null) {
				TextView tv = (TextView) v.findViewById(R.id.name);
				tv.setText(find.getName());
				tv = (TextView) v.findViewById(R.id.latitude);
				String latitude = String.valueOf(find.getLatitude());
				if (!latitude.equals("0.0")) {
					latitude = latitude.substring(0, 7);
				}
				tv.setText(getText(R.string.latitude) + " " + latitude);
				tv = (TextView) v.findViewById(R.id.longitude);
				String longitude = String.valueOf(find.getLongitude());
				if (!longitude.equals("0.0")) {
					longitude = longitude.substring(0, 7);
				}
				tv.setText(getText(R.string.longitude) + " " + longitude);
				tv = (TextView) v.findViewById(R.id.id);
				tv.setText(Integer.toString(find.getId()));
				tv = (TextView) v.findViewById(R.id.time);
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				String time = dateFormat.format(find.getTime());
				/* To-Do Begins */
				if (time.substring(11).equals("00:00:00")) {
					// When the time has not hour/minute/second value, it is set through SetReminder
					// class, the find has a reminder attached, the alarm clock icon must be visible
					tv.setText(getText(R.string.remindertimeLabel)+ " " + time.substring(0, 10));
					alarmIcon.setVisibility(ImageView.VISIBLE);
				} else {
					// Otherwise, the find has no reminder attached,
					// the alarm clock icon must be invisible
					tv.setText(getText(R.string.timeLabel) + " " + time);
					alarmIcon.setVisibility(ImageView.INVISIBLE);
				}
				/* To-Do Ends */
				tv = (TextView) v.findViewById(R.id.status);
				tv.setText(find.getStatusAsString());
				tv = (TextView) v.findViewById(R.id.description_id);
				String description = find.getDescription();
				if (description.length() <= 50) {
					tv.setText(description);
				} else {
					tv.setText(description.substring(0,49)+" ...");
				}

				//Display the thumbnail picture beside the find
				//or a default image if there isn't one
				ImageView iv = (ImageView) v.findViewById(R.id.find_image);
				Bitmap bmp = Camera.getPhotoAsBitmap(find.getGuid(), ListFindsActivity.this);
				if(bmp != null){
				    iv.setImageBitmap(bmp);
				}
				else{
				    iv.setImageResource(R.drawable.ic_menu_camera);
				}
			}
			return v;
		}
	}

}
