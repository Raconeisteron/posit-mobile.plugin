/*
 * File: SmsService.java
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

package org.hfoss.posit.android.experimental.api.service;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsService extends Service {
	public static final String TAG = "SmsManager";
	
	private static final String SENT = "SMS_SENT";
	
	private int nMsgsSent = 0;
	private int nMsgsPending = 0;
	private int mBroadcastsOutstanding = 0;
	private String mErrorMsg = ""; // Set to last error by BroadcastReceiver, not currently used

	private SendMessagesTask sendMessagesTask;

	private List<String> mMessages;
	private List<String> mPhoneNumbers;
	
	/**
	 * This service is not accepting bindings.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
	}

	/**
	 * Starts the service.  Creates a AsyncTask to perform the actual
	 * sending of messages in a background task.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			mMessages = intent.getStringArrayListExtra("messages");
			mPhoneNumbers = intent.getStringArrayListExtra("phonenumbers");
			Log.i(TAG, "Started background service, " +
					" nMessages = " + mMessages.size());

			sendMessagesTask = new SendMessagesTask();
			sendMessagesTask.execute(this);
		}
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	private synchronized void handleSentMessage(Context context, BroadcastReceiver receiver,
			int resultCode, Intent intent, String seq, String smsMsg) {
		switch (resultCode) {
		case Activity.RESULT_OK:
			Log.i(TAG, "Received OK, seq = " + seq + " msg:" + smsMsg);
			++nMsgsSent;
			break;
		case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
			Log.e(TAG, "Received generic failure, seq =  " + seq + " msg:"
					+ smsMsg);
			++nMsgsPending;
			break;
		case SmsManager.RESULT_ERROR_NO_SERVICE:
			Log.e(TAG, "Received no service error, seq =  " + seq + " msg:"
					+ smsMsg);
			++nMsgsPending;
			break;
		case SmsManager.RESULT_ERROR_NULL_PDU:
			Log.e(TAG, "Received null PDU error, seq =  " + seq + " msg:"
					+ smsMsg);
			++nMsgsPending;
			break;
		case SmsManager.RESULT_ERROR_RADIO_OFF:
			Log.e(TAG, "Received radio off error, seq =  " + seq + " msg:"
					+ smsMsg);
			++nMsgsPending;
			break;
		}
	}

//	/**
//	 * A broadcast receiver receives an intent about a sent message and
//	 * reports whether it was sent successfully or failed. 
//	 * @param receiver that was created for an individual message
//	 * @param resultCode whether the message succeeded in being sent or not
//	 * @param intent the msgid is the ACTION of the intent
//	 */
//	private synchronized void handleSentMessage (BroadcastReceiver receiver, 
//			int resultCode, Intent intent, String smsMsg)  {
//		String avIdStr = intent.getAction();  //   arg1.getStringExtra("msgid")
//		int avIdInt = Integer.parseInt(avIdStr);
//		
//		Log.i(TAG, "Rcvd broadcast for msg: " + smsMsg);
//		// REFACTOR?  Why create a AcdiVocaMessage and below call static update rather than retrieving 
//		// the message and updating it.
//		AcdiVocaMessage avMsg = new AcdiVocaMessage(smsMsg);    // Create an AcdiVocaMsg from smsMsg
//		AcdiVocaDbManager db =  new AcdiVocaDbManager(this);
//		Dao<AcdiVocaFind, Integer> daoFind = null;
//		Dao<AcdiVocaMessage, Integer> daoMsg = null;
//		
//		int beneId = avMsg.getBeneficiaryId();
//		int msgId = avMsg.getMessageId();
//		
//		try {
//			daoFind = db.getAcdiVocaFindDao();
//			daoMsg = db.getAcdiVocaMessageDao();
//
//			switch (resultCode)  {
//			case Activity.RESULT_OK:
//				Log.d (TAG, "Received OK, avId = " + avIdStr + " msg:" + avMsg.getSmsMessage());
//				if (avIdInt < 0) {
//					AcdiVocaMessage.updateStatus(daoMsg, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_SENT);
//					AcdiVocaFind.updateMessageStatusForBulkMsg(daoFind, avMsg, msgId, AcdiVocaMessage.MESSAGE_STATUS_SENT);
////					db.updateMessageStatusForBulkMsg(avMsg, AcdiVocaMessage.MESSAGE_STATUS_SENT);
//				}
//				else {
//					// Call static updateStatus, which retrieves the message and then updates it. Refactor?
//					AcdiVocaMessage.updateStatus(daoMsg, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_SENT);
//					AcdiVocaFind.updateMessageStatus(daoFind, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_SENT);
//					//					db.updateMessageStatusForNonBulkMessage(avMsg, AcdiVocaMessage.MESSAGE_STATUS_SENT);
//				}
//				++nMsgsSent;
//				break;
//			case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
//				Log.e(TAG, "Received  generic failure, avId =  " + avIdStr + " msg:" + avMsg.getSmsMessage());
//				if (avIdInt < 0) {
//					AcdiVocaMessage.updateStatus(daoMsg, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//					AcdiVocaFind.updateMessageStatus(daoFind, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
////					db.updateMessageStatusForBulkMsg(avMsg, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//				}
//				else {
//					AcdiVocaMessage.updateStatus(daoMsg, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//					AcdiVocaFind.updateMessageStatus(daoFind, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
////					db.updateMessageStatusForNonBulkMessage(avMsg, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//				}
//				++nMsgsPending;
//				mErrorMsg = "Generic Failure";
//				break;
//			case SmsManager.RESULT_ERROR_NO_SERVICE:
//				Log.e(TAG, "Received  No service, avId =  " + avIdStr + " msg:" + avMsg.getSmsMessage());
//				if (avIdInt < 0) {
//					AcdiVocaMessage.updateStatus(daoMsg, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//					AcdiVocaFind.updateMessageStatus(daoFind, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
////					db.updateMessageStatusForBulkMsg(avMsg, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//				}
//				else {
//					AcdiVocaMessage.updateStatus(daoMsg, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//					AcdiVocaFind.updateMessageStatus(daoFind, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
////					db.updateMessageStatusForNonBulkMessage(avMsg, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//				}
//				++nMsgsPending;
//				mErrorMsg = "No cellular service";
//				break;
//			case SmsManager.RESULT_ERROR_NULL_PDU:
//				Log.e(TAG, "Received Null PDU, avId =  " + avIdStr + " msg:" + avMsg.getSmsMessage());
//				if (avIdInt < 0) {
//					AcdiVocaMessage.updateStatus(daoMsg, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//					AcdiVocaFind.updateMessageStatus(daoFind, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
////					db.updateMessageStatusForBulkMsg(avMsg, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//				}
//				else {
//					AcdiVocaMessage.updateStatus(daoMsg, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//					AcdiVocaFind.updateMessageStatus(daoFind, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
////					db.updateMessageStatusForNonBulkMessage(avMsg, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//				}
//				++nMsgsPending;
//				mErrorMsg = "Null PDU error";
//				break;
//			case SmsManager.RESULT_ERROR_RADIO_OFF:
//				Log.e(TAG, "Received  Radio off, avId =  " + avIdStr + " msg:" + avMsg.getSmsMessage());
//				if (avIdInt < 0) {
//					AcdiVocaMessage.updateStatus(daoMsg, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//					AcdiVocaFind.updateMessageStatus(daoFind, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
////					db.updateMessageStatusForBulkMsg(avMsg, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//				}
//				else {
//					AcdiVocaMessage.updateStatus(daoMsg, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//					AcdiVocaFind.updateMessageStatus(daoFind, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
////					db.updateMessageStatusForNonBulkMessage(avMsg, AcdiVocaMessage.MESSAGE_STATUS_PENDING);
//				}
//				++nMsgsPending;
//				mErrorMsg = "Texting is off";
//				break;
//			}
//		} catch (java.sql.SQLException e) {
//			e.printStackTrace();
//		}
//	}
	
	
	/**
	 * Appends Sms Messages to a text file on the SD card.
	 * @param sFileName
	 * @param msg
	 */
//	public void logMessages(ArrayList<String> msgs){
//		try
//		{
//			File file = new File(Environment.getExternalStorageDirectory() 
//					+ "/" + AcdiVocaAdminActivity.DEFAULT_LOG_DIRECTORY + "/" 
//					+ AcdiVocaAdminActivity.SMS_LOG_FILE);
//
//			//FileWriter writer = new FileWriter(file);
//			PrintWriter writer =  new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
//
//			Iterator<String> it = msgs.iterator();
//			while (it.hasNext()) {
//				String msg = it.next();
//				writer.println(msg);
//				Log.i(TAG, "Wrote to file: " + msg);
//			}
//			writer.flush();
//			writer.close();
//		}
//		catch(IOException e) {
//			Log.e(TAG, "IO Exception writing to Log " + e.getMessage());
//			e.printStackTrace();
//		}
//	}   

	/**
	 * Separately threaded task to send messages.  
	 *
	 */
	class SendMessagesTask extends AsyncTask<Context, Integer, String> {
		public static final String TAG = "AsyncSmsSendMessagesTask";
		
		private Context context;
		
		@Override
		protected String doInBackground(Context... contexts) {
			Log.i(TAG, "doInBackground");
			this.context = contexts[0];
			//logMessages(mMessages);
			transmitMessages(context);
			return null;
		}
		
		protected void transmitMessages(final Context context) {
			
			mBroadcastsOutstanding = mMessages.size();

			for (int i = 0; i < mMessages.size(); i++) {
				final String message = mMessages.get(i);
				String phoneNum = mPhoneNumbers.get(i);
				final String seq = Integer.toString(i);
				
				PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
						new Intent(seq), 0);

				// Receiver for when the SMS is sent
				BroadcastReceiver sendReceiver = new BroadcastReceiver() {
					@Override
					public synchronized void onReceive(Context arg0, Intent arg1) {
						try {
							handleSentMessage(arg0, this, getResultCode(), arg1, seq, message); 
							--mBroadcastsOutstanding;
							context.unregisterReceiver(this);
							Log.i(TAG, "Broadcasts outstanding  = " + mBroadcastsOutstanding);
						} catch (Exception e) {
							Log.e("BroadcastReceiver", "Error in onReceive for msgId " + arg1.getAction());
							Log.e("BroadcastReceiver", e.getMessage());
							e.printStackTrace();
						}
						
					}
				};
				context.registerReceiver(sendReceiver, new IntentFilter(SENT + seq));
				
				// We need to determine how many message we need to send this as.
				// The length array contains 4 result:
				// length[0]  the number of Sms messages required 
				// length[1]  the number of 7-bit code units used
				// length[2]  the number of 7-bit code units remaining
				// length[3]  an indicator of the encoding code unit size
				int[] length = null;
				length = SmsMessage.calculateLength(message, true);
				Log.i(TAG, "Length - 7 bit encoding = " + length[0] + " " + length[1] + " " + length[2] + " " + length[3]);
				length= SmsMessage.calculateLength(message, false);
				Log.i(TAG, "Length - 16 bit encoding = " + length[0] + " " + length[1] + " " + length[2] + " " + length[3]);
				
				SmsManager smsMgr = SmsManager.getDefault();
				if (length[0] == 1) {  
					// Single part message
					try {
						smsMgr.sendTextMessage(phoneNum, null, message, sentPI, null);    
						Log.i(TAG,"SMS Sent. seq = " + seq + " msg :" + message + " phone= " + phoneNum);
					} catch (IllegalArgumentException e) {
						Log.e(TAG, "IllegalArgumentException, probably phone number = " + phoneNum);
						mErrorMsg = e.getMessage();
						e.printStackTrace();
						return;
					} catch (Exception e) {
						Log.e(TAG, "Exception " +  e.getMessage());
						e.printStackTrace();
					}
				}
				else {
					// Multi-part message
 					int nMessagesNeeded = length[0];
					ArrayList<String> msgList = smsMgr.divideMessage(message);
					ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
					for (int k = 0; k < msgList.size(); k++) {
						sentIntents.add(sentPI);
					}

					smsMgr.sendMultipartTextMessage(phoneNum, null, msgList, sentIntents, null); 
					Log.i(TAG,"SMS Sent multipart message. seq = " + seq + " msg :" + msgList.toString() + " phone= " + phoneNum);
				}
			}
		}

		
//		protected void transmitMessages(Context context, ArrayList<String> messages) {
//			Iterator<String> it = messages.iterator();
//			mBroadcastsOutstanding = messages.size();
//			while (it.hasNext()) {
//				final String message = it.next();
//				String[] msgparts = message.split(AttributeManager.PAIRS_SEPARATOR);
//				String[] firstPair = msgparts[0].split(AttributeManager.ATTR_VAL_SEPARATOR);
//				String msgid = firstPair[1];
//				
//				Intent sendIntent = new Intent(msgid);
//				IntentFilter intentFilter = new IntentFilter(msgid);
//				PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0, sendIntent, 0);
//
//				// Not really used -- we are not processing DELIVERED broadcasts.
//				Intent delivered = new Intent ("SMS DELIVERED");
//				PendingIntent deliveryIntent = PendingIntent.getBroadcast(context, 0,delivered, 0);
//
//				// This receiver will be sent a result from the radio device as to whether the message was sent
//				mReceiver = new BroadcastReceiver() {
//					@Override
//					public synchronized void onReceive(Context arg0, Intent arg1) {
//						try {
//							handleSentMessage(this, getResultCode(), arg1, message);
//							--mBroadcastsOutstanding;
//
//							unregisterReceiver(this);
//							Log.i(TAG, "Broadcasts outstanding  = " + mBroadcastsOutstanding);
//						} catch (Exception e) {
//							Log.e("BroadcastReceiver", "Error in onReceive for msgId " + arg1.getAction());
//							Log.e("BroadcastReceiver", e.getMessage());
//							e.printStackTrace();
//						}
//					}
//				};
//				context.registerReceiver(mReceiver, intentFilter);
//				
//				// The length array contains 4 result:
//				// length[0]  the number of Sms messages required 
//				// length[1]  the number of 7-bit code units used
//				// length[2]  the number of 7-bit code units remaining
//				// length[3]  an indicator of the encoding code unit size
//				int[] length = null;
//				length = SmsMessage.calculateLength(message, true);
//				Log.i(TAG, "Length - 7 bit encoding = " + length[0] + " " + length[1] + " " + length[2] + " " + length[3]);
//				length= SmsMessage.calculateLength(message, false);
//				Log.i(TAG, "Length - 16 bit encoding = " + length[0] + " " + length[1] + " " + length[2] + " " + length[3]);
//
//				// TODO:  Add code to break the message into 2 or more.
//				SmsManager smsMgr = SmsManager.getDefault();
//
//				if (length[0] == 1) {  
//					try {
//						smsMgr.sendTextMessage(mPhoneNumber, null, message, sentIntent, deliveryIntent);    
//						Log.i(TAG,"SMS Sent: " + msgid + " msg :" + message + " phone= " + mPhoneNumber);
//					} catch (IllegalArgumentException e) {
//						Log.e(TAG, "IllegalArgumentException, probably phone number = " + mPhoneNumber);
//						mErrorMsg = e.getMessage();
//						e.printStackTrace();
//						return;
//					} catch (Exception e) {
//						Log.e(TAG, "Exception " +  e.getMessage());
//						e.printStackTrace();
//					}
//				}
//				else {
// 					int nMessagesNeeded = length[0];
//					ArrayList<String> msgList = smsMgr.divideMessage(message);
//					ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
////					Iterator<String> msgIt = msgList.iterator();
//					for (int k = 0; k < msgList.size(); k++) {
//						sentIntents.add(sentIntent);
//					}
//
//					smsMgr.sendMultipartTextMessage(mPhoneNumber, null, msgList, sentIntents, null);
//					//smsMgr.sendTextMessage(mPhoneNumber, null, messageFrag, sentIntent, deliveryIntent);    
//					Log.i(TAG,"SMS Sent multipart message: " + msgid + " msg :" + msgList.toString() + " phone= " + mPhoneNumber);
//				}
//			}
//		}
		
		
		@Override
		protected void onCancelled() {
			Log.i(TAG, "onCancelled");
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(String result) {
			Log.i(TAG, "onPostExecute, broadcasts outstanding = " + mBroadcastsOutstanding);
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			Log.i(TAG, "onPreExecute");
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			Log.i(TAG, "onProgressUpdate");
			super.onProgressUpdate(values);
		}
	}
}
