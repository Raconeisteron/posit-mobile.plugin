/*
 * File: SearchFilterActivity.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.Utils;

import android.app.Activity;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Handles Login for ACDI/VOCA application.
 * 
 */
public class AcdiVocaSendSmsActivity extends ListActivity implements OnClickListener {
	public static final String TAG = "AcdiVocaLookupActivity";
	public static final int ACTION_SELECT = 0;
	
	// NOTE: Activity_RESULT_CANCELED = 1
	public static final int RESULT_SELECT_ALL = 2;
	public static final int RESULT_SELECT_NEW = 3;
	public static final int RESULT_SELECT_UPDATE = 4;
	public static final int RESULT_BULK_UPDATE = 8;
	public static final int RESULT_SELECT_PENDING = 5;
	public static final int RESULT_SELECT_SENT = 6;
	public static final int RESULT_SELECT_ACKNOWLEDGED = 7;
	public static final String[] MESSAGE_STATUS_STRINGS = {"","","","NEW", "UPDATE", "PENDING", "SENT","ACKNOWLEDGED"};
 
	private MessageListAdapter<AcdiVocaMessage> mAdapter;
	
	private boolean mMessageListDisplayed = false;
	private int mNMessagesDisplayed = 0;
	private int mResultCode = -1;
	private int mMessageFilter = -1;   		// Set in SearchFilterActivity result

	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 Log.i(TAG, "onCreate");		
	}


	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		super.onPause();
	}

	/**
	 * 
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
		
		AcdiVocaLocaleManager.setDefaultLocale(this);  // Locale Manager should be in API

		setContentView(R.layout.acdivoca_list_messsages);  // Should be done after locale configuration

//		
//		((Button)findViewById(R.id.search_filter_select_button)).setOnClickListener(this);
//		((Button)findViewById(R.id.cancel_select_filter_button)).setOnClickListener(this);
//
//		// Listen for clicks on radio buttons
//		 ((RadioButton)findViewById(R.id.all_messages)).setOnClickListener(this);
//		 ((RadioButton)findViewById(R.id.new_messages)).setOnClickListener(this);
//		 ((RadioButton)findViewById(R.id.update_messages)).setOnClickListener(this);
//		 ((RadioButton)findViewById(R.id.update_bulk_messages)).setOnClickListener(this);
//		 ((RadioButton)findViewById(R.id.pending_messages)).setOnClickListener(this);
//		 ((RadioButton)findViewById(R.id.sent_messages)).setOnClickListener(this);
//		 ((RadioButton)findViewById(R.id.acknowledged_messages)).setOnClickListener(this);
		 
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
		Log.i(TAG, "Prepare Menus, N messages = " + mNMessagesDisplayed);
        MenuItem menuItem = menu.findItem(R.id.sync_messages);
		if (mNMessagesDisplayed > 0 && 
				(mMessageFilter == SearchFilterActivity.RESULT_SELECT_NEW 
						|| mMessageFilter == SearchFilterActivity.RESULT_SELECT_PENDING
						|| mMessageFilter == SearchFilterActivity.RESULT_SELECT_UPDATE
						|| mMessageFilter == SearchFilterActivity.RESULT_BULK_UPDATE))  {
	        menuItem.setEnabled(true);		
		} else {
	        menuItem.setEnabled(false);		
		}
		menuItem = menu.findItem(R.id.delete_messages_menu);
		if (mMessageFilter == SearchFilterActivity.RESULT_SELECT_ACKNOWLEDGED
				&& mNMessagesDisplayed > 0) {
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
            intent.putExtra("user_mode", "USER");
			this.startActivityForResult(intent, SearchFilterActivity.ACTION_SELECT);
			break;
			
		// This case sends all messages	(if messages are cuurently displayed)
		case R.id.sync_messages:
			if (this.mMessageListDisplayed) {
//				sendMessages();
			}
			mNMessagesDisplayed = 0;
//			fillData(null);
			break;
			
		case R.id.delete_messages_menu:
//			showDialog(CONFIRM_DELETE_DIALOG);
			break;				
		}
				
		return true;
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
	 * Required as part of OnClickListener interface. Handles button clicks.
	 */
	public void onClick(View v) {
		Log.i(TAG, "onClick");
	    Intent returnIntent = new Intent();
	    
		try {
			if (v.getClass().equals(Class.forName("android.widget.RadioButton"))) {
					//Toast.makeText(this, "RadioClicked", Toast.LENGTH_SHORT).show();
				((Button)findViewById(R.id.search_filter_select_button)).setEnabled(true);
				return;
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	
		if (v.getId() == R.id.search_filter_select_button) {
			displayMessageList(mResultCode);
		}
	}
	
	/**
	 * Returns the user's selection.
	 * @return
	 */
	private int selectRadioResult() {
		mResultCode = Activity.RESULT_OK;
		RadioButton rb = (RadioButton)findViewById(R.id.all_messages);
		if (rb.isChecked()) 
			mResultCode = RESULT_SELECT_ALL;
		rb = (RadioButton)findViewById(R.id.new_messages);
		if (rb.isChecked())
			mResultCode = RESULT_SELECT_NEW;
		rb = (RadioButton)findViewById(R.id.update_messages);
		if (rb.isChecked())
			mResultCode = RESULT_SELECT_UPDATE;
		rb = (RadioButton)findViewById(R.id.update_bulk_messages);
		if (rb.isChecked())
			mResultCode = RESULT_BULK_UPDATE;		
		rb = (RadioButton)findViewById(R.id.sent_messages);
		if (rb.isChecked())
			mResultCode = RESULT_SELECT_SENT;
		rb = (RadioButton)findViewById(R.id.pending_messages);
		if (rb.isChecked()) 
			mResultCode = RESULT_SELECT_PENDING;
		rb = (RadioButton)findViewById(R.id.acknowledged_messages);
		if (rb.isChecked())
			mResultCode = RESULT_SELECT_ACKNOWLEDGED;
		return mResultCode;

	}
	
	/**
	 * Displays SMS messages, filter by status and type.
	 */
	private void displayMessageList(int filter) {
		Log.i(TAG, "Display messages for filter " + filter);
		ArrayList<AcdiVocaMessage> acdiVocaMsgs = null;
		AcdiVocaDbHelper db = new AcdiVocaDbHelper(this);
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String distrKey = this.getResources().getString(R.string.distribution_point);
		String distributionCtr = sharedPrefs.getString(distrKey, "");
		Log.i(TAG, distrKey +"="+ distributionCtr);
		
		//dossiers = db.fetchAllBeneficiaryIdsByDistributionSite(distributionCtr);
		
		if (filter == SearchFilterActivity.RESULT_SELECT_NEW 
				|| filter == SearchFilterActivity.RESULT_SELECT_UPDATE) {  // Second arg is order by
			acdiVocaMsgs = db.createMessagesForBeneficiaries(filter, null, distributionCtr);
		} else if (filter == SearchFilterActivity.RESULT_SELECT_ALL 
				|| filter == SearchFilterActivity.RESULT_SELECT_PENDING
				|| filter == SearchFilterActivity.RESULT_SELECT_SENT
				|| filter == SearchFilterActivity.RESULT_SELECT_ACKNOWLEDGED) {
			acdiVocaMsgs = db.fetchSmsMessages(filter, AcdiVocaDbHelper.FINDS_STATUS_DONTCARE,  null); 
		} else if (filter == SearchFilterActivity.RESULT_BULK_UPDATE) {
			acdiVocaMsgs = db.createBulkUpdateMessages(distributionCtr);
		} else
			return;
				
		if (acdiVocaMsgs.size() == 0) {
			mNMessagesDisplayed = 0;
			Log.i(TAG, "display Message List, N messages = " + mNMessagesDisplayed);
			acdiVocaMsgs.add(new AcdiVocaMessage(AcdiVocaDbHelper.UNKNOWN_ID,
					AcdiVocaDbHelper.UNKNOWN_ID,
					-1,"",
					getString(R.string.no_messages),"",
					!AcdiVocaMessage.EXISTING));
		}
		else {
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
                        
                		String s = ((org.hfoss.posit.android.plugin.acdivoca.AcdiVocaMessage) msg).getSmsMessage();
                 		if (s.equals(getString(R.string.no_messages))) {
                 			bt.setTextColor(Color.RED);
                 			bt.setTextSize(24);
                 			bt.setText(((org.hfoss.posit.android.plugin.acdivoca.AcdiVocaMessage) msg).getSmsMessage());
                 		} else {  // This case handles a real message
                           	if (tt != null) {
                        		tt.setTextColor(Color.RED);
                        		tt.setText(((org.hfoss.posit.android.plugin.acdivoca.AcdiVocaMessage) msg).getMsgHeader());                            
                        	}
                        	if(bt != null){
                        		bt.setText(((org.hfoss.posit.android.plugin.acdivoca.AcdiVocaMessage) msg).getSmsMessage());
                        	}		
                 		}
                }
                return v;
        }
}

	
	
	
}