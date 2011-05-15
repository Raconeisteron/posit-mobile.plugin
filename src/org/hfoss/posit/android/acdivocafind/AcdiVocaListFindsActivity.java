/*
 * File: ListPhotoFindsActivity.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Search and Identification Tool.
 *
 * POSIT is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (LGPL) as published 
 * by the Free Software Foundation; either version 3.0 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU LGPL along with this program; 
 * if not visit http://www.gnu.org/licenses/lgpl.html.
 * 
 */
package org.hfoss.posit.android.acdivocafind;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.ListFindsActivity;
import org.hfoss.posit.android.provider.PositDbHelper;
import org.hfoss.posit.android.utilities.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter.ViewBinder;

/**
 * Displays a summary of Finds on this phone in a clickable list.
 *
 */
public class AcdiVocaListFindsActivity extends ListFindsActivity implements ViewBinder{

	private static final String TAG = "ListActivity";
	private Cursor mCursor;  // Used for DB accesses

	private static final int confirm_exit=1;

	private static final int CONFIRM_DELETE_DIALOG = 0;
	public static final int FIND_FROM_LIST = 0;
	private int project_id;
    private static final boolean DBG = false;

	/** 
	 * Called when the Activity starts and
	 *  when the user navigates back to ListPhotoFindsActivity
	 *  from some other app. It creates a
	 *  DBHelper and calls fillData() to fetch data from the DB.
	 *  @param savedInstanceState contains the Activity's previously
	 *   frozen state.  In this case it is unused.
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		project_id = 0; //sp.getInt("PROJECT_ID", 0);
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

//		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		project_id = 0; //sp.getInt("PROJECT_ID", 0);
		fillData(null);
		NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    	nm.cancel(Utils.NOTIFICATION_ID);
	}

	/**
	 * Called when the system is about to resume some other activity.
	 *  It can be used to save state, if necessary.  In this case
	 *  we close the cursor to the DB to prevent memory leaks.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onPause(){
		super.onPause();
		stopManagingCursor(mCursor);
		mCursor.close();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mCursor.close();
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		mCursor.close();
	}


	/**
	 * Puts the items from the DB table into the rows of the view. Note that
	 *  once you start managing a Cursor, you cannot close the DB without 
	 *  causing an error.
	 */
	private void fillData(String order_by) {
		String[] columns = AcdiVocaDbHelper.list_row_data;
		int [] views = AcdiVocaDbHelper.list_row_views;
	
		mCursor = AcdiVocaFindDataManager.getInstance().fetchFindsByProjectId(this, project_id, order_by);	
		//		Uri allFinds = Uri.parse("content://org.hfoss.provider.POSIT/finds_project/"+PROJECT_ID);
		//	    mCursor = managedQuery(allFinds, null, null, null, null);
		if (mCursor.getCount() == 0) { // No finds
			setContentView(R.layout.acdivoca_list_finds);
			mCursor.close();
			return;
		}
		startManagingCursor(mCursor); // NOTE: Can't close DB while managing cursor

		// CursorAdapter binds the data in 'columns' to the views in 'views' 
		// It repeatedly calls ViewBinder.setViewValue() (see below) for each column
		// NOTE: The columns and views are defined in MyDBHelper.  For each column
		// there must be a view and vice versa, although the column (data) doesn't
		// necessarily have to go with the view, as in the case of the thumbnail.
		// See comments in MyDBHelper.
		
		SimpleCursorAdapter adapter = 
			new SimpleCursorAdapter(this, R.layout.acdivoca_list_row, mCursor, columns, views);
		adapter.setViewBinder(this);
		setListAdapter(adapter); 
		//stopManagingCursor(mCursor);
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Invoked when the user clicks on one of the Finds in the
	 *   list. It starts the PhotoFindActivity in EDIT mode, which will read
	 *   the Find's data from the DB.
	 *   @param l is the ListView that was clicked on 
	 *   @param v is the View within the ListView
	 *   @param position is the View's position in the ListView
	 *   @param id is the Find's RowID
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent intent = new Intent(this, AcdiVocaFindActivity.class);
		intent.setAction(Intent.ACTION_EDIT);
		if (DBG) Log.i(TAG,"id = " + id);
		intent.putExtra(AcdiVocaDbHelper.FINDS_ID, id);

		startActivityForResult(intent, FIND_FROM_LIST);
		AcdiVocaFindActivity.SAVE_CHECK=false;
	}

	 
	/**
	 * Creates the menus for this activity.
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.acdi_voca_list_finds_menu, menu);
		return true;
	}

	/** 
	 * Starts the appropriate Activity when a MenuItem is selected.
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {		
		}
		return true;
	}

	/**
	 * Called automatically by the SimpleCursorAdapter.  
	 */
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		TextView tv = null; // = (TextView) view;
		long findIden = cursor.getLong(cursor.getColumnIndexOrThrow(AcdiVocaDbHelper.FINDS_ID));
		switch (view.getId()) {

		default:
			return false;
		}
	}
	/*@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if(keyCode==KeyEvent.KEYCODE_BACK){
			showDialog(confirm_exit);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}*/



	/**
	 * This method is invoked by showDialog() when a dialog window is created. It displays
	 *  the appropriate dialog box, currently a dialog to confirm that the user wants to 
	 *  delete all the finds.
	 */
	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case CONFIRM_DELETE_DIALOG:
			return new AlertDialog.Builder(this)
			.setIcon(R.drawable.alert_dialog_icon)
			.setTitle(R.string.alert_dialog)
			.setPositiveButton(R.string.alert_dialog_ok, 
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked OK so do some stuff 
					if(PositDbHelper.getInstance().deleteAllFinds()){
						Utils.showToast(AcdiVocaListFindsActivity.this, R.string.deleted_from_database);
						finish();
					} else {
						Utils.showToast(AcdiVocaListFindsActivity.this, R.string.delete_failed);
						dialog.cancel();
					}
				}
			}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					/* User clicked Cancel so do nothing */
				}
			}).create();

		} // switch

		switch (id) {
		case confirm_exit:
			return new AlertDialog.Builder(this)
			.setIcon(R.drawable.alert_dialog_icon)
			.setTitle(R.string.exit)
			.setPositiveButton(R.string.alert_dialog_ok, 
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked OK so do some stuff 
					finish();
				}
			}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					/* User clicked Cancel so do nothing */
				}
			}).create();

		default:
			return null;
		}
	}





}
