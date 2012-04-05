/*
 * File: SyncSms.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool. 
 *
 * This code is free software; you can redistribute it and/or modify
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
package org.hfoss.posit.android.sync;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.service.SmsService;
import org.hfoss.posit.android.functionplugin.sms.SmsViewActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * This class is for the easy sending of SMS messages. Currently, there are two
 * types of SMS messages that can be sent: An entire Find, or a simple String.
 * This class encapsulates the SMS sending protocol. If someone were to want to
 * make the protocol more robust (for instance, to allow the sending of partial
 * finds), they would likely want to do it here. For the purposes of my plugin,
 * I have kept it fairly simple.
 * 
 * @author Ryan McLeod
 * 
 * Note: Some modifications to the code have been made by Andrew Matsusaka
 */
public class SyncSms extends SyncMedium {
	public static final String TAG = "SyncSms";
	public static final String FIND_PREFIX = "~_";
	
	private ArrayList<String> mMessages;
	private ArrayList<String> mPhoneNumbers;
	private Context mContext;
	
	public SyncSms(Context context) {
		mMessages = new ArrayList<String>();
		mPhoneNumbers = new ArrayList<String>();
		mContext = context;
	}

	/**
	 * Add a Find to be transmitted via SMS later.
	 * 
	 * @param find
	 *            The Find object to be transmitted.
	 * @param phoneNumber
	 *            The phone number that the Find should be transmitted to
	 * @throws IllegalArgumentException
	 */
	public void addFind(Find find, String phoneNumber)
			throws IllegalArgumentException {
		addFind( find.getDbEntries(), phoneNumber );
	}
	
	/**
	 * Adds a find in the form of a text message to the list of texts
	 * to be sent later
	 * @param bundle - Bundle containing find data
	 * @param phoneNumber - phone number to be sent to
	 * @throws IllegalArgumentException
	 */
	public void addFind(Bundle bundle, String phoneNumber)
			throws IllegalArgumentException {
		String text = convertBundleToRaw( bundle );
		StringBuilder builder = new StringBuilder( text );
		builder.insert(0, FIND_PREFIX);
		addMessage( text, phoneNumber );
	}

	/**
	 * Add a text message to be sent later.
	 * 
	 * @param text
	 *            The string contents of the SMS message
	 * @param phoneNumber
	 *            The phone number to which the message should be sent
	 */
	public void addMessage(String text, String phoneNumber) {
		mMessages.add(text);
		mPhoneNumbers.add(phoneNumber);
	}

	/**
	 * Sends all previously added messages to their respective destinations.
	 * 
	 */
	public void sendFinds() {
		Intent smsService = new Intent(mContext, SmsService.class);
		smsService.putExtra("messages", mMessages);
		smsService.putExtra("phonenumbers", mPhoneNumbers);
		smsService.setAction(Intent.ACTION_SEND);
		mContext.startService(smsService);
	}
	
	/**
	 * This isn't used because SMS sends individual messages internally
	 * using the SmsService
	 */
	public boolean sendFind(Find find){ return false; }
	
	/**
	 * SMS has no post sending tasks to complete
	 */
	public boolean postSendTasks() { return true; }

	/**
	 * Retrieves messages from the Intent and stores the originating phone
	 * number and text data
	 * @param intent - Intent containing text data
	 * @return Map of received text data
	 */
	public Map<String, String> getMessages( Intent intent ){
		Bundle bundle = intent.getExtras();
		Map<String, String> msgTexts = new LinkedHashMap<String, String>();

		Log.i(TAG, "Intent action = " + intent.getAction());
		
		if (bundle != null){
			Object[] pdus = (Object[]) bundle.get("pdus");
	
			for (Object pdu : pdus) {
				SmsMessage 	message 			= SmsMessage.createFromPdu((byte[]) pdu);
				String 		incomingMsg 		= message.getMessageBody();
				String 		originatingNumber 	= message.getOriginatingAddress();
	
				logMessageData( message );
	
				// If there are other messages from this sender, concatenate them
				String text = msgTexts.get(originatingNumber);
				if (text != null) {
					msgTexts.put(originatingNumber, text + incomingMsg);
				} else {
					msgTexts.put(originatingNumber, incomingMsg);
				}
			}
		}
		
		return msgTexts;
	}

	/**
	 * Logs the data retrieved from the messages
	 * @param message - message data to be logged
	 */
	private void logMessageData( SmsMessage message ){
		String incomingMsg 			= message.getMessageBody();
		String originatingNumber 	= message.getOriginatingAddress();
		
		Log.i(TAG, "FROM: " + originatingNumber);
		Log.i(TAG, "MESSAGE: " + incomingMsg);
		
		int[] msgLen = SmsMessage.calculateLength(message.getMessageBody(), true);
		Log.i(TAG, "" + msgLen[0] + " " + msgLen[1] + " " + msgLen[2] + " " + msgLen[3]);
		
		msgLen = SmsMessage.calculateLength(message.getMessageBody(), false);
		Log.i(TAG, "" + msgLen[0] + " " + msgLen[1] + " " + msgLen[2] + " " + msgLen[3]);

		// Log.i(TAG, "Protocol = " + message.getProtocolIdentifier());
		Log.i(TAG, "LENGTH: " + incomingMsg.length());
	}
	
	/**
	 * Processes Map of text data. Once processed a notification is created.
	 * @param msgTexts - Map of text data
	 * @param notificationId - id of notification
	 */
	public void processMessages( Map<String, String> msgTexts, int notificationId ){
		for (Entry<String, String> entry : msgTexts.entrySet()) {
			Log.i(TAG, "Processing message: " + entry.getValue());
			
			if( isEntryPrefixValid(entry) ){
				Find find = convertRawToFind(entry.getValue().substring(2));
				if (find == null) {
					Log.e(TAG, "SMS message could not be parsed as a Find");
				} else {
					Log.i(TAG, "SMS message parsed as a Find successfully!");
					
					// So now we need to notify the user
					String ns = Context.NOTIFICATION_SERVICE;
					NotificationManager notificationMgr = (NotificationManager) m_context
							.getSystemService(ns);
					
					Notification notification = buildNotification( entry, find, notificationId );
					
					notificationMgr.notify(notificationId++, notification);
				}
			}
		}
	}
	
	/**
	 * Checks whether or not the prefix of the message matches the valid
	 * SMS prefix
	 * @param entry - Entry containing message
	 * @return whether or not the message prefix matched the expected SMS prefix
	 */
	private boolean isEntryPrefixValid( Entry<String, String> entry ){
		boolean valid = false;
		
		if (entry.getValue().substring(0, 2).equals(FIND_PREFIX)) {
			Log.i(TAG, "Prefix of message matches Find prefix. Attempting to parse.");
			valid = true;
		} else {
			Log.i(TAG, "Prefix of message does not match Find prefix. Ignoring.");
			valid = false;
		}
		
		return valid;
	}
	
	/**
	 * Creates a notification using the message, Find and notification id
	 * @param entry - Entry of the text message
	 * @param find - Find extracted from message
	 * @param notificationId - id of notification
	 * @return Notification created from passed in data
	 */
	private Notification buildNotification( Entry<String, String> entry, Find find, int notificationId ){
		Notification notification = initNotification();
		Intent notificationIntent = buildNotificationIntent( entry, find, notificationId );
		PendingIntent contentIntent = PendingIntent.getActivity(
				m_context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		CharSequence contentTitle = "SMS received";
		CharSequence contentText = "from " + entry.getKey();
		
		notification.contentIntent = contentIntent;
		notification.setLatestEventInfo(m_context, contentTitle,
				contentText, contentIntent);
		
		return notification;
	}
	
	/**
	 * Initializes a new notification with the icon, text and creation time
	 * @return initialized notification
	 */
	private Notification initNotification(){
		int 		 icon 		= R.drawable.notification_icon;
		CharSequence tickerText = "SMS Find received!";
		long 		 when 		= System.currentTimeMillis();
		
		return new Notification(icon, tickerText, when);
	}
	
	/**
	 * Creates an Intent containing the find data, sender and notification id
	 * @param entry - Entry of the text message
	 * @param find - Find extracted from message
	 * @param notificationId - id of notification
	 * @return Intent filled with passed in data
	 */
	private Intent buildNotificationIntent( Entry<String, String> entry, Find find, int notificationId ){
		Context appContext = m_context.getApplicationContext();

		Intent notificationIntent = new Intent(appContext,
				SmsViewActivity.class);
		notificationIntent.putExtra("findbundle", find.getDbEntries());
		notificationIntent.putExtra("sender", entry.getKey());
		notificationIntent.putExtra("notificationid", notificationId);
		
		return notificationIntent;
	}
	
	/**
	 * SMS does not retrieve a list of Find guids during its sync process
	 */
	public List<String> getFindsNeedingSync() { return null; }

	/**
	 * SMS does not request Find data based on guid
	 */
	public String retrieveRawFind(String guid) { return null; }

}
