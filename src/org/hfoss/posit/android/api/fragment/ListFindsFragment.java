package org.hfoss.posit.android.api.fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.activity.MapFindsActivity;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.plugin.FindPlugin;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.api.plugin.FunctionPlugin;
import org.hfoss.posit.android.api.plugin.ListFindPluginCallback;
import org.hfoss.posit.android.sync.SyncActivity;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ListFindsFragment extends OrmLiteListFragment<DbManager> {
	private static final String TAG = "ListFindsFragment";
	protected ArrayList<FunctionPlugin> mListMenuPlugins = null;

	private static final int CONFIRM_DELETE_DIALOG = 0;
	public static final String ACTION_LIST_FINDS = "list_finds";

	private static List<? extends Find> finds;
	
	protected static FindsListAdapter mAdapter = null;
	
	private boolean mIsDualPane;
	private int mCurrCheckPosition;
	
	/*
	 * Called when the Activity starts.
	 * 
	 * @param savedInstanceState
	 *            contains the Activity's previously frozen state. In this case
	 *            it is unused.
	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mListMenuPlugins = FindPluginManager.getFunctionPlugins(FindPluginManager.LIST_MENU_EXTENSION);
		Log.i(TAG, "# of List menu plugins = " + mListMenuPlugins.size());
		
		setHasOptionsMenu(true);
		
		if (savedInstanceState != null) {
			mCurrCheckPosition = savedInstanceState.getInt("currChoice", 0);
		}
		
		if (mIsDualPane) {
			getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			//display selected find
		}
	}
	
	/* Called when the activity is ready to start interacting with the user. It
	 * is at the top of the Activity stack.
	 * 
	 * @see android.support.v4.app.Fragment#onResume()
	 */
	@Override
	public void onResume() {
		super.onResume();
		Log.i(TAG, "onResume()");
		mAdapter = (FindsListAdapter) setUpAdapter();
		fillList(mAdapter);
	}
	
	/* Called when the Activity is paused.
	 * 
	 * @see android.support.v4.app.Fragment#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currChoice", mCurrCheckPosition);
    }
	
	public void onGetChangedFindsResult(String finds) {
		Log.i(TAG, "Got changed finds: " + finds);
	}
	
	protected ListAdapter setUpAdapter() {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		int projectId = prefs.getInt(getString(R.string.projectPref), 0);
		
		finds = this.getHelper().getFindsByProjectId(projectId);
		
		int resId = getResources().getIdentifier(
				FindPlugin.mListFindLayout, "layout", getActivity().getPackageName());

		FindsListAdapter adapter = new FindsListAdapter(getActivity(), resId, finds);

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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (mListMenuPlugins.size() > 0) {
			for (FunctionPlugin plugin: mListMenuPlugins) {
				MenuItem item = menu.add(plugin.getmMenuTitle());
				int id = getResources().getIdentifier(
						plugin.getmMenuIcon(), "drawable", "org.hfoss.posit.android");
//				Log.i(TAG, "icon =  " + plugin.getmMenuIcon() + " id =" + id);
				item.setIcon(id);
				//item.setIcon(android.R.drawable.ic_menu_mapmode);				
			}
		}
		inflater.inflate(R.menu.list_finds_menu, menu);
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
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "onMenuitemSelected()");

		Intent intent;
		switch (item.getItemId()) {
		case R.id.sync_finds_menu_item:
			Log.i(TAG, "Sync finds menu item");
			startActivityForResult(new Intent(getActivity(), SyncActivity.class), 0);
			break;
		case R.id.map_finds_menu_item:
			Log.i(TAG, "Map finds menu item");
			intent = new Intent();
			intent.setAction(ACTION_LIST_FINDS);
			intent.setClass(getActivity(), MapFindsActivity.class);			
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
						startActivity(new Intent(getActivity(), plugin.getmMenuActivity()));
				}
			}
			break;
		}
		return true;
	}
	
	public static void syncCallback() {
		Log.i(TAG, "Notified sync callback");
		mAdapter.notifyDataSetChanged();
	}
	
	public void showDialog(int num) {
		
		// DialogFragment.show() will take care of adding the fragment
	    // in a transaction.  We also want to remove any currently showing
	    // dialog, so make our own transaction and take care of that here.
	    FragmentTransaction ft = getFragmentManager().beginTransaction();
	    Fragment prev = getFragmentManager().findFragmentByTag("dialog");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);

	    // Create and show the dialog.
	    ListFindsDialogFragment newFragment = ListFindsDialogFragment.newInstance(num);
	    newFragment.show(ft, "dialog");
	}
//	@Override
//	protected Dialog onCreateDialog(int id) {
//		switch (id) {
//		case CONFIRM_DELETE_DIALOG:
//			return new AlertDialog.Builder(this).setIcon(R.drawable.alert_dialog_icon)
//					.setTitle(R.string.confirm_delete)
//					.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
//						public void onClick(DialogInterface dialog, int whichButton) {
//							// User clicked OK so do some stuff
//							if (deleteAllFind()) {
//								finish();
//							}
//						}
//					}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
//						public void onClick(DialogInterface dialog, int whichButton) {
//							// User clicked cancel so do nothing
//						}
//					}).create();
//		default:
//			return null;
//		}
//	}

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
		Context context;

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public FindsListAdapter(Context context, int textViewResourceId, List list) {
			super(context, textViewResourceId, list);
			this.items = list;
			this.context = context;
		}

		
		@Override
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged();
		}


		@SuppressWarnings("unchecked")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				int resId = getResources().getIdentifier(
						FindPlugin.mListFindLayout, "layout",
						getActivity().getPackageName());
				v = vi.inflate(resId, null);
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
    			tv.setText(getText(R.string.timeLabel) + " " + time);
				
				tv = (TextView) v.findViewById(R.id.status);
				tv.setText(find.getStatusAsString());
				tv = (TextView) v.findViewById(R.id.description_id);
				String description = find.getDescription();
				if (description.length() <= 50) {
					tv.setText(description);
				} else {
					tv.setText(description.substring(0,49)+" ...");
				}

				ArrayList<FunctionPlugin> plugins = FindPluginManager.getFunctionPlugins();
				
				// Call each plugin's callback method to update view
				for (FunctionPlugin plugin: plugins) {
//					Log.i(TAG, "Call back for plugin=" + plugin);
					Class<ListFindPluginCallback> callbackClass = null;
					Object o;
					try {
						String className = plugin.getListFindCallbackClass();
						if (className != null) {
							callbackClass = (Class<ListFindPluginCallback>) Class.forName(className);
							o = (ListFindPluginCallback) callbackClass.newInstance();
							((ListFindPluginCallback) o).listFindCallback(context,find,v);
						}
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (java.lang.InstantiationException e) {
						e.printStackTrace();
					}
				}
			}
			return v;
		}
	}
}
