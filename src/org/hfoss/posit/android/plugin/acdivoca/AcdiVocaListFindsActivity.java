/*
 * File: AcdiVocaListFindsActivity.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of the ACDI/VOCA plugin for POSIT, Portable Open Search 
 * and Identification Tool.
 *
 * This plugin is free software; you can redistribute it and/or modify
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
package org.hfoss.posit.android.plugin.acdivoca;

import java.util.ArrayList;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.Utils;
import org.hfoss.posit.android.api.FindActivityProvider;
import org.hfoss.posit.android.api.FindPluginManager;
import org.hfoss.posit.android.api.ListFindsActivity;
import org.hfoss.posit.android.provider.PositDbHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
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
	public static final String MESSAGE_START_SUBSTRING = "t=";
	
	
	private int project_id;
    private static final boolean DBG = false;
	//private ArrayAdapter<String> mAdapter;
    
    private MessageListAdapter<AcdiVocaMessage> mAdapter;
	//private ArrayAdapter<AcdiVocaMessage> mAdapter;

	private int mMessageFilter = -1;   		// Set in SearchFilterActivity result
	private int mNMessagesDisplayed = 0;
	
	private boolean mMessageListDisplayed = false;

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
		
		AcdiVocaLocaleManager.setDefaultLocale(this);  // Locale Manager should be in API

//		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		project_id = 0; //sp.getInt("PROJECT_ID", 0);
		if (!mMessageListDisplayed) {
			fillData(null);
			NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			nm.cancel(Utils.NOTIFICATION_ID);
		}
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
			setContentView(R.layout.acdivoca_list_beneficiaries);
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
		
		//lookup the id and check the beneficiary type
		//based on that prepare the intent
		//Intent intent = new Intent(this, AcdiVocaFindActivity.class);
        AcdiVocaDbHelper db = new AcdiVocaDbHelper(this);
        ContentValues values = db.fetchFindDataById(id, null);
        
        Log.i(TAG, "###############################################");
        Log.i(TAG, values.toString());
        Intent intent = null;
 		if(values.getAsInteger(AcdiVocaDbHelper.FINDS_TYPE) == AcdiVocaDbHelper.FINDS_TYPE_MCHN){
 			intent = new Intent(this, AcdiVocaFindActivity.class);
 		}
 		if(values.getAsInteger(AcdiVocaDbHelper.FINDS_TYPE) == AcdiVocaDbHelper.FINDS_TYPE_AGRI){
 			intent = new Intent(this, AcdiVocaNewAgriActivity.class);
 		}
 		
 		intent.setAction(Intent.ACTION_EDIT);
		if (DBG) Log.i(TAG,"id = " + id);
		intent.putExtra(AcdiVocaDbHelper.FINDS_ID, id);

		startActivityForResult(intent, FIND_FROM_LIST);
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
	 * Prepares the menu options based on the message search filter. This
	 * is called just before the menu is displayed.
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		Log.i(TAG, "Prepare Menus, N messages = " + mNMessagesDisplayed);
        MenuItem menuItem = menu.findItem(R.id.sync_messages);
		if (mNMessagesDisplayed > 0 && 
				(mMessageFilter == SearchFilterActivity.RESULT_SELECT_NEW 
						|| mMessageFilter == SearchFilterActivity.RESULT_SELECT_PENDING
						|| mMessageFilter == SearchFilterActivity.RESULT_SELECT_UPDATE))  {
	        menuItem.setEnabled(true);		
		} else {
	        menuItem.setEnabled(false);		

		}
		return super.onPrepareOptionsMenu(menu);
	}

	/** 
	 * Starts the appropriate Activity when a MenuItem is selected.
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		        
		Intent intent;
		switch (item.getItemId()) {	
		
		// Start a SearchFilterActivity for result
		case R.id.list_messages:
			intent = new Intent();
			intent.setClass(this, SearchFilterActivity.class);
			this.startActivityForResult(intent, SearchFilterActivity.ACTION_SELECT);
			break;
			
		// This case sends all messages	(if messages are cuurently displayed)
		case R.id.sync_messages:
			if (this.mMessageListDisplayed) {
				sendMessages();
			}
			mNMessagesDisplayed = 0;
			fillData(null);
			break;
			//break;
		}
		return true;
	}

	/**
	 * Helper method to send SMS messages. 
	 */
	private void sendMessages() {
		int nMsgs = mAdapter.getCount();
		int k = 0;
		while (k < nMsgs) {
			AcdiVocaMessage acdiVocaMsg = mAdapter.getItem(k);
			int beneficiary_id = acdiVocaMsg.getBeneficiaryId();
			Log.i(TAG, "Raw Message: " + acdiVocaMsg.getRawMessage());
			Log.i(TAG, "To Send: " + acdiVocaMsg.getSmsMessage());
			
			AcdiVocaDbHelper db = new AcdiVocaDbHelper(this);
			if (AcdiVocaSmsManager.sendMessage(this, beneficiary_id, acdiVocaMsg, null)) {
				Log.i(TAG, "Message Sent--should update as SENT");
				db.updateMessageStatus(acdiVocaMsg, AcdiVocaDbHelper.MESSAGE_STATUS_SENT);
			} else {
				Log.i(TAG, "Message Not Sent -- should update as PENDING");
				db.updateMessageStatus(acdiVocaMsg, AcdiVocaDbHelper.MESSAGE_STATUS_PENDING);
			}
			++k;
		}
	}

	/**                                                                                                                                                                                       
	 * Retrieves the Beneficiary Id from the Message string.                                                                                                                                  
	 * TODO:  Probably not the best way                                                                                                                                                       
	 * to handle this.  A better way would be to have DbHelper return an array of Benefiiary                                                                                                  
	 * objects (containing the Id) and display the message field of those objects in the                                                                                                      
	 * list.  Not sure how to do this with an ArrayAdapter??                                                                                                                                  
	 * @param message                                                                                                                                                                         
	 * @return                                                                                                                                                                                
	 */
	private int getBeneficiaryId(String message) {
		return Integer.parseInt(message.substring(message.indexOf(":")+1, message.indexOf(" ")));
	}

	/**                                                                                                                                                                                       
	 * Cleans leading display data from the message as it is displayed                                                                                                                        
	 * in the list adapter.  Current format should start with "t="  for Type.                                                                                                                 
	 * TODO:  See the comment on the previous method.                                                                                                                                         
	 * @param msg                                                                                                                                                                             
	 * @return                                                                                                                                                                                
	 */
	private String cleanMessage(String msg) {
		String cleaned = "";
		cleaned = msg.substring(msg.indexOf(MESSAGE_START_SUBSTRING));
		return cleaned;
	}

	/**
	 * 
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "onActivityResult = " + resultCode);
		switch (requestCode) {
		case SearchFilterActivity.ACTION_SELECT:
			if (resultCode == RESULT_CANCELED) {
//				Toast.makeText(this, "Cancel " + resultCode, Toast.LENGTH_SHORT).show();
				break;
			} else {
				mMessageFilter = resultCode;   
//				Toast.makeText(this, "Ok " + resultCode, Toast.LENGTH_SHORT).show();
				displayMessageList(resultCode);	
			} 
		
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}


	/**
	 * Displays SMS messages, filter by status and type.
	 */
	private void displayMessageList(int filter) {
		Log.i(TAG, "Display messages for filter " + filter);
		ArrayList<AcdiVocaMessage> acdiVocaMsgs = null;
		AcdiVocaDbHelper db = new AcdiVocaDbHelper(this);
		
		if (filter == SearchFilterActivity.RESULT_SELECT_NEW 
				|| filter == SearchFilterActivity.RESULT_SELECT_UPDATE) {  // Second arg is order by
			acdiVocaMsgs = db.createMessagesForBeneficiaries(filter, null);
		} else if (filter == SearchFilterActivity.RESULT_SELECT_ALL 
				|| filter == SearchFilterActivity.RESULT_SELECT_PENDING
				|| filter == SearchFilterActivity.RESULT_SELECT_SENT
				|| filter == SearchFilterActivity.RESULT_SELECT_ACKNOWLEDGED) {
			acdiVocaMsgs = db.fetchSmsMessages(filter, null); 
		} else 
			return;
				
//		if (acdiVocaMsgs == null) {
		if (acdiVocaMsgs.size() == 0) {
			mNMessagesDisplayed = 0;
			Log.i(TAG, "display Message List, N messages = " + mNMessagesDisplayed);
			//setContentView(R.layout.acdivoca_list_messsages);
			acdiVocaMsgs.add(new AcdiVocaMessage(-1,-1,-1,"",getString(R.string.no_messages),""));

		} else {
			mNMessagesDisplayed = acdiVocaMsgs.size();
			Log.i(TAG, "display Message List, N messages = " + mNMessagesDisplayed);
	        Log.i(TAG, "Fetched " + acdiVocaMsgs.size() + " messages");
		}
		setUpMessagesList(acdiVocaMsgs);

	}
	
	/**
	 * Helper method to set up a simple list view using an ArrayAdapter.
	 * @param data
	 */
	private void setUpMessagesList(final ArrayList<AcdiVocaMessage> data) {
		if (data != null) 
			Log.i(TAG, "setUpMessagesList, size = " + data.size());
		else 
			Log.i(TAG, "setUpMessagesList, data = null");

		mMessageListDisplayed = true;

		mAdapter = new MessageListAdapter<AcdiVocaMessage>(this, R.layout.acdivoca_list_messsages, data);

		setListAdapter(mAdapter);
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String display = "";
				TextView tv = ((TextView)parent.findViewById(R.id.message_header));
				display += tv.getText();
				tv = ((TextView)parent.findViewById(R.id.message_body));
				display += "\n" + tv.getText();

				Toast.makeText(getApplicationContext(), display, Toast.LENGTH_SHORT).show();
			}
		});

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
						Toast.makeText(AcdiVocaListFindsActivity.this, R.string.deleted_from_database, Toast.LENGTH_SHORT).show();
						finish();
					} else {
						Toast.makeText(AcdiVocaListFindsActivity.this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
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


	private class MessageListAdapter<AcdiVocaMessage> extends ArrayAdapter<AcdiVocaMessage> {

        private ArrayList<AcdiVocaMessage> items;

        public MessageListAdapter(Context context, int textViewResourceId, ArrayList<AcdiVocaMessage> items) {
                super(context, textViewResourceId, items);
                this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.acdivoca_list_messages_row, null);
                }
                AcdiVocaMessage msg = items.get(position);
                if (msg != null) {
                        TextView tt = (TextView) v.findViewById(R.id.message_header);
                        TextView bt = (TextView) v.findViewById(R.id.message_body);
                        if (items.size() > 1) {
                        	if (tt != null) {
                        		tt.setTextColor(Color.RED);
                        		tt.setText(((org.hfoss.posit.android.plugin.acdivoca.AcdiVocaMessage) msg).getMsgHeader());                            
                        	}
                        	if(bt != null){
                        		bt.setText("SMS: " + ((org.hfoss.posit.android.plugin.acdivoca.AcdiVocaMessage) msg).getSmsMessage());
                        	}
                        } else {
                           	if(bt != null){
                           		bt.setTextColor(Color.RED);
                           		bt.setTextSize(24);
                        		bt.setText(((org.hfoss.posit.android.plugin.acdivoca.AcdiVocaMessage) msg).getSmsMessage());
                        	}
                       	
                        }
                }
                return v;
        }
}

}
