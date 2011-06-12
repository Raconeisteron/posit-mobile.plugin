/*
 * File: AcdiVocaSmsManager.java
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
import org.hfoss.posit.android.api.FindPluginManager;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class AcdiVocaSmsManager extends BroadcastReceiver {
	
	public static final String TAG = "AcdiVocaSmsManager";
	private static AcdiVocaSmsManager sInstance = null; 

	public static final String SENT = "SMS_SENT";
	public static final String DELIVERED = "SMS_DELIVERED";
	
	public static final String INCOMING_PREFIX = "AV=";

	
	public static final int MAX_MESSAGE_LENGTH = 140;
	public static final int MAX_PHONE_NUMBER_LENGTH = 10;
	public static final int MIN_PHONE_NUMBER_LENGTH = 5;
	
	public int msgId = 0;
	private Activity mMainActivity = null;
	private Handler mHandler;
	
	public AcdiVocaSmsManager()  {
	}
	
	private AcdiVocaSmsManager(Activity activity){
		mMainActivity = activity;
		String url = "content://sms/"; 
        Uri uri = Uri.parse(url); 
        mMainActivity.getContentResolver().registerContentObserver(uri, true, new SmsContentObserver(mHandler));                    
	}
	
	public static AcdiVocaSmsManager initInstance(Activity activity){
		sInstance = new AcdiVocaSmsManager(activity);
		return sInstance;
	}
	
	public static AcdiVocaSmsManager getInstance(){
		assert(sInstance != null);
		return sInstance;
	}

	@Override
	public void onReceive(Context arg0, Intent intent) {
		Log.i(TAG, "Intent action = " + intent.getAction());

		Bundle bundle = intent.getExtras();

		ArrayList<SmsMessage> messages = new ArrayList<SmsMessage>();

		if (bundle != null) {
			Object[] pdus = (Object[]) bundle.get("pdus");

			for (Object pdu : pdus) {
				SmsMessage message = SmsMessage.createFromPdu((byte[]) pdu);
				messages.add(message);
				String body = message.getMessageBody();

				String incomingMsg = message.getMessageBody();
				String originatingNumber = message.getOriginatingAddress();

				Log.i(TAG, "FROM: " + originatingNumber);
				Log.i(TAG, "MESSAGE: " + incomingMsg);
				int[] msgLen = SmsMessage.calculateLength(message.getMessageBody(), true);
				Log.i(TAG, "" + msgLen[0]  + " " + msgLen[1] + " " + msgLen[2] + " " + msgLen[3]);
				msgLen = SmsMessage.calculateLength(message.getMessageBody(), false);
				Log.i(TAG, "" + msgLen[0]  + " " + msgLen[1] + " " + msgLen[2] + " " + msgLen[3]);

				Log.i(TAG, "Protocol = " + message.getProtocolIdentifier());
				
				Log.i(TAG, " String length = " + incomingMsg.length());
				if (incomingMsg.startsWith(INCOMING_PREFIX)) {
					msgId = getMsgIdFromIncomingMsg(incomingMsg);
				}
			}
		}
	}

	/**
	 * Get's the message id from the incoming message.
	 * @param msg
	 * @return
	 */
	private int getMsgIdFromIncomingMsg (String msg) {
		int id = -1;
		try {
			id = Integer.parseInt(msg.substring(
					msg.indexOf(INCOMING_PREFIX) + INCOMING_PREFIX.length(), 
					msg.indexOf(AttributeManager.PAIRS_SEPARATOR)));
		} catch (NumberFormatException e){
			e.printStackTrace();
			return -1;
		}
		return id;
	}
	
	@Override
	public IBinder peekService(Context myContext, Intent service) {
		// TODO Auto-generated method stub
		return super.peekService(myContext, service);
	}

	
	/**
	 * Checks for a validly-formatted phone number, which 
	 * takes the form: [+]1234567890
	 * @param number
	 * @return
	 */
	private static boolean checkPhoneNumber(String number) {
		if (number.length() < MIN_PHONE_NUMBER_LENGTH
				|| number.length() > MAX_PHONE_NUMBER_LENGTH)
			return false;
		
		// Check for valid digits
		for(int i = 0; i < number.length(); i++) {
			if(number.charAt(i)<'0'|| number.charAt(i)>'9')
				if(!(i==0&&number.charAt(i)=='+'))
					return false;
		}
		return true;
	}
	
	/**
	 * Adds the ACDI/VOCA Prefix and sends the message, returning false if an error occurs.
	 * @param context
	 * @param beneficiary_id
	 * @param message
	 * @param phoneNumber
	 * @return
	 */
	//public static boolean sendMessage(Context context, int beneficiary_id, String message, String phoneNumber) {
	public static boolean sendMessage(Context context, int beneficiary_id, AcdiVocaMessage acdiVocaMessage, String phoneNumber) {
		if (phoneNumber==null)
			phoneNumber = PreferenceManager.getDefaultSharedPreferences(context).getString("smsPhone", "");

		String message = acdiVocaMessage.ACDI_VOCA_PREFIX + AttributeManager.ATTR_VAL_SEPARATOR +
			+ acdiVocaMessage.getBeneficiaryId() +   AttributeManager.PAIRS_SEPARATOR
			+ acdiVocaMessage.getSmsMessage();
		
		PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0,new Intent(SENT), 0);
		PendingIntent deliveryIntent = PendingIntent.getBroadcast(context, 0,new Intent(DELIVERED), 0);
		
		if (checkPhoneNumber(phoneNumber)
				&& message.length() > 0 
				&& message.length() <= MAX_MESSAGE_LENGTH) {
			try {
				SmsManager sms = SmsManager.getDefault();
//				sms.sendTextMessage(phoneNumber, null, message, sentIntent, deliveryIntent);    
				Toast.makeText(context, "SMS Sent!\n"+message + " to " + phoneNumber, Toast.LENGTH_LONG).show();
				Log.i(TAG,"SMS Sent: " + message);
			}catch(Exception e) {
				Log.i(TAG,e.toString());
				e.printStackTrace();
				return false;
			}
			return true;
		}
		else {
			Toast.makeText(context, "SMS Failed\nCheck phone number or length of message", Toast.LENGTH_LONG).show();
			return false;
		}
	}
	
	public static String formatAcdiVocaMessage(int id, String rawMessage) {
		String msg = "";
		//msg = ACDI_VOCA_PREFIX + "=" + id + "," + rawMessage;
		return msg;
	}
	
	
// Deprecated -- To be Deleted	
//	public static String formatSmsMessage(ContentValues values) {
//		String message = 
//		"m" + AttributeManager.ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_TYPE) + AttributeManager.PAIRS_SEPARATOR
//		    +  "f" + AttributeManager.ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_FIRSTNAME) + AttributeManager.PAIRS_SEPARATOR
//			+  "l" + AttributeManager.ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_LASTNAME) + AttributeManager.PAIRS_SEPARATOR
//			+ "b" + AttributeManager.ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_DOB)  + AttributeManager.PAIRS_SEPARATOR
//			+ "s" + AttributeManager.ATTR_VAL_SEPARATOR  + values.get(AcdiVocaDbHelper.FINDS_SEX) + AttributeManager.PAIRS_SEPARATOR
//			+ "a" + AttributeManager.ATTR_VAL_SEPARATOR  + values.get(AcdiVocaDbHelper.FINDS_ADDRESS) + AttributeManager.PAIRS_SEPARATOR
//			+ "c" + AttributeManager.ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY) + AttributeManager.PAIRS_SEPARATOR
//			+ "n" + AttributeManager.ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE);
//		
//		return message;
//		
//	}
	
	
	class SmsContentObserver extends ContentObserver {
		
		
		public SmsContentObserver(Handler handler) {
			super(handler);
			mHandler = handler;
			// TODO Auto-generated constructor stub
		}
		

		@Override public boolean deliverSelfNotifications() { 
		    return false; 
		    }

		@Override public void onChange(boolean arg0) { 
		    super.onChange(arg0);

		     Log.i("SmsContentObserver", "Notification on SMS observer"); 

//		    Message msg = new Message(); 
//		    msg.obj = "xxxxxxxxxx";
//
//		    mHandler.sendMessage(msg);

		    Uri uriSMSURI = Uri.parse("content://sms/");
		    Cursor cur = mMainActivity.getContentResolver().query(uriSMSURI, null, null,
		                 null, null);
		    Log.i(TAG, "Cursor size " + cur.getCount());
		    String[] columns = cur.getColumnNames();
		    for (int k = 0; k < columns.length; k++)
		    	Log.i(TAG, columns[k]);
		    cur.moveToNext();
		    String protocol = cur.getString(cur.getColumnIndex("protocol"));
		    if(protocol == null){
		           Log.d("SmsContentObserver", "SMS SEND"); 
		           int threadId = cur.getInt(cur.getColumnIndex("thread_id"));

		           Log.d("SmsContentObserver", "SMS SEND ID = " + threadId); 
		           Cursor c = mMainActivity.getContentResolver().query(Uri.parse("content://sms/outbox/" + threadId), null, null,
		                   null, null);
		           c.moveToNext();
		           int p = cur.getInt(cur.getColumnIndex("person"));
		           Log.d("SmsContentObserver", "SMS SEND person= " + p); 
		           //getContentResolver().delete(Uri.parse("content://sms/conversations/" + threadId), null, null);

		    }
		    else{
		        Log.d("SmsContentObserver", "SMS RECIEVE");  
		         int threadIdIn = cur.getInt(cur.getColumnIndex("thread_id"));
		         int msgId = cur.getInt(cur.getColumnIndex("_id"));
			  //   mMainActivity.getContentResolver().delete(Uri.parse("content://sms/conversations/" + msgId), null, null);

		      //  mMainActivity.getContentResolver().delete(Uri.parse("content://sms/conversations/" + threadIdIn), null, null);
		        Log.i("SmsContentObserver", "Deleted??");
		    }

		 }
		

	}
	
	
}
