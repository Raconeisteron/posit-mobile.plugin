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

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class AcdiVocaSmsManager extends BroadcastReceiver {
	
	public static final String TAG = "AcdiVocaSmsManager";

	public static final String SENT = "SMS_SENT";
	public static final String DELIVERED = "SMS_DELIVERED";
	
	public static final String INCOMING_PREFIX = 
		AcdiVocaMessage.ACDI_VOCA_PREFIX 
		+ AttributeManager.ATTR_VAL_SEPARATOR;

	
	public static final int MAX_MESSAGE_LENGTH = 140;
	public static final int MAX_PHONE_NUMBER_LENGTH = 10;
	public static final int MIN_PHONE_NUMBER_LENGTH = 5;
	
	public int msgId = 0;
	private static Context mContext = null;
	private static Handler mHandler;
	private static AcdiVocaSmsManager mInstance = null; 
	private static String acdiVocaPhone = null;
	
	public AcdiVocaSmsManager()  {
	}
	
	public static AcdiVocaSmsManager getInstance(Activity activity){
		mInstance = new AcdiVocaSmsManager();
		mInstance.initInstance(activity);
		return mInstance;
	}
	
	public static void initInstance(Context context) {
		mContext = context;
		mInstance = new AcdiVocaSmsManager();
		String url = "content://sms/"; 
        Uri uri = Uri.parse(url); 
        acdiVocaPhone = 
			PreferenceManager.getDefaultSharedPreferences(context).getString("smsPhone", "");

 //       mContext.getContentResolver().registerContentObserver(uri, 
 //       		true, new AcdiVocaSmsManager().new SmsContentObserver(mHandler));                    
	}
	
//	public static AcdiVocaSmsManager initInstance(Activity activity){
//		sInstance = new AcdiVocaSmsManager(activity);
//		return sInstance;
//	}
//	
//	public static AcdiVocaSmsManager getInstance(){
//		assert(sInstance != null);
//		return sInstance;
//	}

	/**
	 * Invoked automatically when a message is received.  Requires Manifest:
	 * <uses-permission android:name="android.permission.SEND_SMS"></uses-permission>
     * <uses-permission android:name="android.permission.RECEIVE_SMS"></uses-permission>
     * 
 	 */
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

				String incomingMsg = message.getMessageBody();
				String originatingNumber = message.getOriginatingAddress();

				Log.i(TAG, "FROM: " + originatingNumber);
				Log.i(TAG, "MESSAGE: " + incomingMsg);
				int[] msgLen = SmsMessage.calculateLength(message.getMessageBody(), true);
				Log.i(TAG, "" + msgLen[0]  + " " + msgLen[1] + " " + msgLen[2] + " " + msgLen[3]);
				msgLen = SmsMessage.calculateLength(message.getMessageBody(), false);
				Log.i(TAG, "" + msgLen[0]  + " " + msgLen[1] + " " + msgLen[2] + " " + msgLen[3]);

				//Log.i(TAG, "Protocol = " + message.getProtocolIdentifier());
				Log.i(TAG, "LENGTH: " + incomingMsg.length());				 

				if (incomingMsg.startsWith(INCOMING_PREFIX)) {
					handleAcdiVocaIncoming(incomingMsg);
				}
			}
		}
	}

	/**
	 * Handles an incoming Sms from AcdiVoca Hdqts. 
	 * 
	 * We are interested in AcdiVoca ACK messages, which are:
	 * 
	 * AV=ACK,IDS=id1|id2|id3|...|idN,...,
	 * 
	 * The list of ids represent beneficiary row_ids (which were sent in the original
	 * message to the server.  These messages should be marked acknowledged.

	 * @param msg
	 */
	private void handleAcdiVocaIncoming(String msg) {
		Log.i(TAG, "Processing incoming SMS: " + msg);
		boolean isAck  = false;
		String attrvalPairs[] = msg.split(AttributeManager.PAIRS_SEPARATOR);

		AcdiVocaDbHelper db = null;

		// The message has the format AV=ACK,IDS=1/2/3/.../,  so just two pairs
		for (int k = 0; k < attrvalPairs.length; k++) {
			String attrval[] = attrvalPairs[k].split(AttributeManager.ATTR_VAL_SEPARATOR);
			String attr = "", val = "";
			if (attrval.length == 2) {
				attr = attrval[0];
				val = attrval[1];
			} else if (attrval.length == 1) {
				attr = attrval[0];
			}

			// If this is an ACK message,  set on the isAck flag
			if (attr.equals(AcdiVocaMessage.ACDI_VOCA_PREFIX)
					&& val.equals(AcdiVocaMessage.ACK)) {
				isAck = true;
			}
			// If this is the list of IDs,  parse the ID numbers and update the Db
			if (attr.equals(AcdiVocaMessage.IDS) && isAck) {
				Log.i(TAG, attr + "=" + val);

				// We use a tokenizer with a number parser so we can handle non-numeric 
				//  data without crashing.  It skips all non-numerics as it reads the stream.
				StreamTokenizer t = new StreamTokenizer(new StringReader(val));
				t.resetSyntax( );
				t.parseNumbers( );
				try {

					//  While not end-of-file, get the next token and extract number
					int token =  t.nextToken();
					while (token != t.TT_EOF) {
						if (token != t.TT_NUMBER )  {
							//Log.i(TAG, "Integer parser skipping token = " + token); // Skip nonnumerics
						}
						else {
							
							// Construct an SmsMessage and update the Db
							int msgId = (int)t.nval;
							Log.i(TAG, "ACKing: " + msgId);
							AcdiVocaMessage avMsg = new AcdiVocaMessage(
									msgId,
									-1,      // Beneficiary Id, unknown
									AcdiVocaDbHelper.MESSAGE_STATUS_ACK,
									attr + AttributeManager.ATTR_VAL_SEPARATOR + val, // Raw message
									"",   // SmsMessage N/A
									""    // Header  N/A
							);
							db = new AcdiVocaDbHelper(mContext);
							db.updateMessageStatus(avMsg,AcdiVocaDbHelper.MESSAGE_STATUS_ACK);
						}
						token = t.nextToken();
					}
				}
				catch ( IOException e ) {
					Log.i(TAG,"Number format exception");
					e.printStackTrace();
					continue;					
				}
			}
		}

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
	private static boolean isValidPhoneString(String number) {
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
	
	public static boolean sendMessage(Context context, int beneficiary_id, AcdiVocaMessage acdiVocaMessage, String phoneNumber) {
		if (phoneNumber==null)
			phoneNumber = PreferenceManager.getDefaultSharedPreferences(context).getString("smsPhone", "");

		String message = acdiVocaMessage.ACDI_VOCA_PREFIX + AttributeManager.ATTR_VAL_SEPARATOR +
			+ acdiVocaMessage.getBeneficiaryId() +   AttributeManager.PAIRS_SEPARATOR
			+ acdiVocaMessage.getSmsMessage();
		
		PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0,new Intent(SENT), 0);
		PendingIntent deliveryIntent = PendingIntent.getBroadcast(context, 0,new Intent(DELIVERED), 0);
		
		// The length array contains 4 result:
		// length[0]  the number of Sms messages required 
		// length[1]  the number of 7-bit code units used
		// length[2]  the number of 7-bit code units remaining
		// length[3]  an indicator of the encoding code unit size
		int[] length = SmsMessage.calculateLength(message, true);
		Log.i(TAG, "Length - 7 bit encoding = " + length[0] + " " + length[1] + " " + length[2] + " " + length[3]);
		length = SmsMessage.calculateLength(message, false);
		Log.i(TAG, "Length - 16 bit encoding = " + length[0] + " " + length[1] + " " + length[2] + " " + length[3]);
	
		if (!isValidPhoneString(phoneNumber)) {
//			Toast.makeText(context, "SMS Failed\nCheck phone number.", Toast.LENGTH_LONG).show();
			return false;			
		}
		// This can go in a single method, send it
		// TODO:  Add code to break the message into 2 or more.
		if (length[0] == 1) {  
			try {
				SmsManager sms = SmsManager.getDefault();
				sms.sendTextMessage(phoneNumber, null, message, sentIntent, deliveryIntent);    
//				Toast.makeText(context, "SMS Sent!\n"+message + " to " + phoneNumber, Toast.LENGTH_LONG).show();
				Log.i(TAG,"SMS Sent: " + message);
				return true;
			}catch(Exception e) {
				Log.i(TAG,e.toString());
				e.printStackTrace();
				return false;
			}
		} 
		return false;
	}
	
	public static String formatAcdiVocaMessage(int id, String rawMessage) {
		String msg = "";
		//msg = ACDI_VOCA_PREFIX + "=" + id + "," + rawMessage;
		return msg;
	}
	
	
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
		    Cursor cur = mContext.getContentResolver().query(uriSMSURI, null, null,
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
		           Cursor c = mContext.getContentResolver().query(Uri.parse("content://sms/outbox/" + threadId), null, null,
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
