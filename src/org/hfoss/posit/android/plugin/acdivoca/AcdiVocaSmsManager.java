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
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaAdminActivity.ImportDataThread;
import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaAdminActivity.ImportThreadHandler;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
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
	
	private static String mAcdiVocaPhone = null;
	private static Activity mActivity;
	
	private static ProgressDialog mProgressDialog;
	public static final int DONE = 0;
	
	//private  BroadcastReceiver[] mReceivers;
	private  BroadcastReceiver mReceiver;
	private int nRcvrs = 0;
	private int nMsgsSent = 0;
	private int nMsgsPending = 0;
	private String mErrorMsg = ""; // Set to last error by BroadcastReceiver

	private static Hashtable<String,AcdiVocaMessage> mMessagesTable;
	
	public AcdiVocaSmsManager()  {
	}
	
	public static AcdiVocaSmsManager getInstance(Activity activity){
		mActivity = activity;
		mInstance = new AcdiVocaSmsManager();
		AcdiVocaSmsManager.initInstance(activity);
		return mInstance;
	}
	
	public static void initInstance(Context context) {
		mContext = context;
		mInstance = new AcdiVocaSmsManager();
		mMessagesTable = new Hashtable<String,AcdiVocaMessage>();
		
		String url = "content://sms/"; 
        Uri uri = Uri.parse(url); 
        mAcdiVocaPhone = 
			PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.smsPhoneKey), "");
 //  Not used
 //       mContext.getContentResolver().registerContentObserver(uri, 
 //       		true, new AcdiVocaSmsManager().new SmsContentObserver(mHandler));                    
	}


	/**
	 * Invoked automatically when a message is received.  Requires Manifest:
	 * <uses-permission android:name="android.permission.SEND_SMS"></uses-permission>
     * <uses-permission android:name="android.permission.RECEIVE_SMS"></uses-permission>
     * 
 	 */
	@Override
	public void onReceive(Context context, Intent intent) {
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
					handleAcdiVocaIncoming(context, incomingMsg);
				}
			}
		}
	}

	/**
	 * Handles an incoming Sms from AcdiVoca Modem. We are interested in AcdiVoca ACK messages, 
	 * which are:
	 * 
	 * AV=ACK,IDS=id1&id2&id3&...&idN,..., 
	 * 
	 * The list of ids represent either beneficiary ids (i.e., row_ids, which were sent in 
	 * the original message) for regular messages or they represent message ids for bulk
	 * beneficiary update messages, in which case the id numbers are negative.  These 
	 * messages should be marked acknowledged.
	 * @param msg
	 */
	private void handleAcdiVocaIncoming(Context context, String msg) {
		Log.i(TAG, "Processing incoming SMS: " + msg);
		boolean isAck  = false;
		String attrvalPairs[] = msg.split(AttributeManager.PAIRS_SEPARATOR);

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
				processAckList(context, attr, val);
			}
		}
	}
	
	/**
	 * Helper method to process of list of IDs as tokens.
	 * @param val
	 */
	private void processAckList(Context context, String attr, String val) {

		// We use a tokenizer with a number parser so we can handle non-numeric 
		//  data without crashing.  It skips all non-numerics as it reads the stream.
		StreamTokenizer t = new StreamTokenizer(new StringReader(val));
		t.resetSyntax( );
		t.parseNumbers( );
		try {

			//  While not end-of-file, get the next token and extract number
			int token =  t.nextToken();
			while (token != StreamTokenizer.TT_EOF) {
				if (token != StreamTokenizer.TT_NUMBER )  {
					//Log.i(TAG, "Integer parser skipping token = " + token); // Skip nonnumerics
				}
				else {

					// Construct an SmsMessage and update the Db
					int ackId = (int)t.nval;
					Log.i(TAG, "ACKing, ackId: " + ackId);
					AcdiVocaMessage avMsg = null;
					
					// Message for bulk messages, where IDs < 0 and represent message Ids
					if (ackId < 0)  {   // Check for bulk 
						avMsg = new AcdiVocaMessage(
								ackId * -1,  // For Bulks, the ackId is MsgId
								AcdiVocaDbHelper.UNKNOWN_ID,  // Beneficiary Id
								AcdiVocaDbHelper.MESSAGE_STATUS_ACK,
								attr + AttributeManager.ATTR_VAL_SEPARATOR + val, // Raw message
								"",   // SmsMessage N/A
								"",    // Header  N/A
								!AcdiVocaMessage.EXISTING
						);
					} else {
						// Message for normal messages, where IDs > 0 and represent beneficiary IDs
						avMsg = new AcdiVocaMessage(
								AcdiVocaDbHelper.UNKNOWN_ID,  // Message Id is unknown -- Modem sends back Beneficiary Id
								ackId,  // For non-acks, ackId is Beneficiary Id
								AcdiVocaDbHelper.MESSAGE_STATUS_ACK,
								attr + AttributeManager.ATTR_VAL_SEPARATOR + val, // Raw message
								"",   // SmsMessage N/A
								"",    // Header  N/A
								!AcdiVocaMessage.EXISTING
						);
					}
					AcdiVocaDbHelper db = new AcdiVocaDbHelper(context);
					db.recordAcknowledgedMessage(avMsg);
				}
				token = t.nextToken();
			}
		}
		catch ( IOException e ) {
			Log.i(TAG,"Number format exception");
			e.printStackTrace();
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
	 * Publicly exposed method for processing Sms messages.  It starts a thread to
	 * handle the details. 
	 * @param context
	 * @param acdiVocaMsgs
	 */
	public void sendMessages(Context context, ArrayList<AcdiVocaMessage> acdiVocaMsgs) {
		mContext = context;
		Log.i(TAG, "sendMessages,  n =" + acdiVocaMsgs.size());
		
		mProgressDialog = ProgressDialog.show(context, context.getString(R.string.send_message),
				context.getString(R.string.please_wait), true, true);
		
		SendMessagesThread thread = new SendMessagesThread(context, 
				new SendMessagesThreadHandler(),
				acdiVocaMsgs);
		thread.start();	
	}
	
	
	/**
	 * Utility method to send messages. Called by the SendMessagesThread. 
	 * @param acdiVocaMsgs an ArrayList of messages.
	 */
	private synchronized void  transmitMessages(Context context, ArrayList<AcdiVocaMessage> acdiVocaMsgs) {
		Log.i(TAG, "Transmitting  messages = " + acdiVocaMsgs.size());
		
		mAcdiVocaPhone = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.smsPhoneKey), "");
		if (!isValidPhoneString(mAcdiVocaPhone)) {
			Log.e(TAG, "Invalid phone number " + mAcdiVocaPhone);
			mErrorMsg = "Invalid phone number = " + mAcdiVocaPhone;
			return;			
		}
		
		nMsgsSent = 0;
		AcdiVocaMessage acdiVocaMsg = null;
		Iterator<AcdiVocaMessage> it = acdiVocaMsgs.iterator();
		while (it.hasNext()) {
			acdiVocaMsg = it.next();
			int beneficiary_id = acdiVocaMsg.getBeneficiaryId();
			Log.i(TAG, "Raw Message: " + acdiVocaMsg.getRawMessage());
			Log.i(TAG, "To Send: " + acdiVocaMsg.getSmsMessage());
			
			if (!acdiVocaMsg.isExisting()) {
				Log.i(TAG,"This is an existing message");
				AcdiVocaDbHelper db = new AcdiVocaDbHelper(context);
				int msgId = (int)db.createNewMessageTableEntry(acdiVocaMsg,beneficiary_id,AcdiVocaDbHelper.MESSAGE_STATUS_UNSENT);
				acdiVocaMsg.setMessageId(msgId);
			}
			
			sendMessage(context, beneficiary_id, acdiVocaMsg);
		}
	}


	/**
	 * Adds the ACDI/VOCA Prefix and sends the message, returning false if an error occurs.
	 * @param context
	 * @param beneficiary_id
	 * @param message
	 * @param phoneNumber
	 * @return
	 * @throws MalformedMimeTypeException 
	 */
	private boolean sendMessage(Context context, int beneficiary_id, final AcdiVocaMessage acdiVocaMessage)  {
		Log.i(TAG, "Message for bid = " + beneficiary_id);

		String message = null;
		int msgid = 0;
		if (beneficiary_id != AcdiVocaDbHelper.UNKNOWN_ID) {
			msgid = acdiVocaMessage.getBeneficiaryId();
			message = AcdiVocaMessage.ACDI_VOCA_PREFIX 
			+ AttributeManager.ATTR_VAL_SEPARATOR 
			+ acdiVocaMessage.getBeneficiaryId() // For normal messages we use the beneficiary's row id, 1...N
			+ AttributeManager.PAIRS_SEPARATOR
			+ acdiVocaMessage.getSmsMessage();
		} else {
			msgid = acdiVocaMessage.getMessageId() * -1;
			message = AcdiVocaMessage.ACDI_VOCA_PREFIX 
			+ AttributeManager.ATTR_VAL_SEPARATOR 
			+ acdiVocaMessage.getMessageId() * -1   // For Bulk messages we use minus the message id (e.g., -123)
			+ AttributeManager.PAIRS_SEPARATOR
			+ acdiVocaMessage.getSmsMessage();
		}
		
		String actionKey = ""+msgid;
//		mMessagesTable.put(key, acdiVocaMessage);
		
		Intent sendIntent = new Intent(actionKey);
		IntentFilter intentFilter = new IntentFilter(actionKey);
		PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0, sendIntent, 0);
	
		// Not really used
		Intent delivered = new Intent (DELIVERED);
		PendingIntent deliveryIntent = PendingIntent.getBroadcast(context, 0,delivered, 0);

		
		mReceiver = new BroadcastReceiver() {
			@Override
			public synchronized void onReceive(Context arg0, Intent arg1) {
				handleSentMessage(this, getResultCode(), arg1, acdiVocaMessage);
				//notify();
				Log.i(TAG, "Notified");
				mContext.unregisterReceiver(this);
			}
		};
		context.registerReceiver(mReceiver, intentFilter);
	
		// The length array contains 4 result:
		// length[0]  the number of Sms messages required 
		// length[1]  the number of 7-bit code units used
		// length[2]  the number of 7-bit code units remaining
		// length[3]  an indicator of the encoding code unit size
		int[] length = SmsMessage.calculateLength(message, true);
		Log.i(TAG, "Length - 7 bit encoding = " + length[0] + " " + length[1] + " " + length[2] + " " + length[3]);
		length = SmsMessage.calculateLength(message, false);
		Log.i(TAG, "Length - 16 bit encoding = " + length[0] + " " + length[1] + " " + length[2] + " " + length[3]);
		
		// TODO:  Add code to break the message into 2 or more.
		if (length[0] == 1) {  
			try {
				SmsManager sms = SmsManager.getDefault();
				sms.sendTextMessage(mAcdiVocaPhone, null, message, sentIntent, deliveryIntent);    
				Log.i(TAG,"SMS Sent: " + msgid + " to " + mAcdiVocaPhone);
				try {
					Thread.sleep(1000);  // Wait for 1 seconds -- not optimal algorithm...  :(
				} catch (InterruptedException e) {
					Log.e(TAG, "Interrupted exception " + e.getMessage());
				}
				Log.i(TAG, "After wait");
			return true;
			}catch(Exception e) {
				Log.i(TAG,e.toString());
				e.printStackTrace();
				return false;
			}
		} 
		return true;
	}
	
	/**
	 * A broadcast receiver received an intent about a sent message and
	 * reports whether it was sent successfully or failed. 
	 * @param receiver one of an array of receivers, 1 per message
	 * @param resultCode whether it succeeded or not
	 * @param intent  the msgid is the ACTION of the intent
	 */
	private synchronized void handleSentMessage (BroadcastReceiver receiver, 
			int resultCode, Intent intent, AcdiVocaMessage avMsg)  {
		String msgId = intent.getAction();  //   arg1.getStringExtra("msgid")
		AcdiVocaDbHelper db =  new AcdiVocaDbHelper(mContext);
		switch (resultCode)  {
		case Activity.RESULT_OK:
			Log.i(TAG, "Received OK, msgid = " + msgId);
			db.updateMessageStatus(avMsg, AcdiVocaDbHelper.MESSAGE_STATUS_SENT);
			++nMsgsSent;
			break;
		case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
			Log.e(TAG, "Received  generic failure, msgid =  " + msgId);
			db.updateMessageStatus(avMsg, AcdiVocaDbHelper.MESSAGE_STATUS_PENDING);
			++nMsgsPending;
			mErrorMsg = "Generic Failure";
			break;
		case SmsManager.RESULT_ERROR_NO_SERVICE:
			Log.e(TAG, "Received  No service, msgid =  " + msgId);
			db.updateMessageStatus(avMsg, AcdiVocaDbHelper.MESSAGE_STATUS_PENDING);
			++nMsgsPending;
			mErrorMsg = "No cellular service";
			break;
		case SmsManager.RESULT_ERROR_NULL_PDU:
			Log.e(TAG, "Received Null PDU, msgid =  " + msgId);
			db.updateMessageStatus(avMsg, AcdiVocaDbHelper.MESSAGE_STATUS_PENDING);
			++nMsgsPending;
			mErrorMsg = "Null PDU error";
			break;
		case SmsManager.RESULT_ERROR_RADIO_OFF:
			Log.e(TAG, "Received  Radio off, msgid =  " + msgId);
			db.updateMessageStatus(avMsg, AcdiVocaDbHelper.MESSAGE_STATUS_PENDING);
			++nMsgsPending;
			mErrorMsg = "Texting is off";
			break;
		}
	}
	
	
	/**
	 * Handler for the SendMessageThread
	 *
	 */
	class SendMessagesThreadHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG,"Send SMS Message received " + msg.what);
			if (msg.what == DONE) {
				mProgressDialog.dismiss();
				Log.i(TAG, "Sent = " + nMsgsSent + " Pending = " + nMsgsPending);

				((SmsCallBack)mActivity).smsMgrCallBack(
				"Sent = " + nMsgsSent 
				+ " Pending = " + nMsgsPending
				+ " " + mErrorMsg);			}
		}
	}
	
	/**
	 * Thread to handle message sending. 
	 *
	 */
	class SendMessagesThread extends Thread {
		private Context mContext;
		private Handler mHandler;
		private ArrayList<AcdiVocaMessage> mAcdiVocaMsgs;
		
		public SendMessagesThread(Context context, Handler handler, ArrayList<AcdiVocaMessage> acdiVocaMsgs) {
			mHandler = handler;
			mContext = context;
			mAcdiVocaMsgs = acdiVocaMsgs;
		}
	
		@Override
		public void run() {
			transmitMessages(mContext, mAcdiVocaMsgs);
			mHandler.sendEmptyMessage(AcdiVocaSmsManager.DONE);
		}
	}
	
}
