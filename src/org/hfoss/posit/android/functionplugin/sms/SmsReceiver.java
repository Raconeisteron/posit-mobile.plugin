/*
 * File: SmsReceiver.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool.
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

package org.hfoss.posit.android.functionplugin.sms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.plugin.FindPlugin;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.api.plugin.FunctionPlugin;
import org.hfoss.posit.android.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {
	private static final String TAG = "SmsReceiver";
	private static int mNextNotificationId = 1;

	/**
	 * Helper function that checks if the SMS Plugin is turned on.
	 * 
	 * @return true if the SMS Plugin is on, false otherwise.
	 */
	private Boolean smsPluginOn() {
		List<FunctionPlugin> plugins = FindPluginManager
				.getFunctionPlugins(FindPluginManager.ADD_FIND_MENU_EXTENSION);
		for (FunctionPlugin plugin : plugins)
			if (plugin.getName().equals("sendsms"))
				return true;
		return false;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!smsPluginOn()) {
			Log.i(TAG, "Received text message, but SMS plugin is disabled.");
			
			return;
		}

		Log.i(TAG, "Intent action = " + intent.getAction());
		Bundle bundle = intent.getExtras();

		if (bundle == null)
			return;

		Object[] pdus = (Object[]) bundle.get("pdus");
		Map<String, String> msgTexts = new LinkedHashMap<String, String>();

		// Grab messages from PDUs, and concatenate multi-part messages
		for (Object pdu : pdus) {
			// Get the message
			SmsMessage message = SmsMessage.createFromPdu((byte[]) pdu);

			String incomingMsg = message.getMessageBody();
			String originatingNumber = message.getOriginatingAddress();

			Log.i(TAG, "FROM: " + originatingNumber);
			Log.i(TAG, "MESSAGE: " + incomingMsg);
			int[] msgLen = SmsMessage.calculateLength(message.getMessageBody(),
					true);
			Log.i(TAG, "" + msgLen[0] + " " + msgLen[1] + " " + msgLen[2] + " "
					+ msgLen[3]);
			msgLen = SmsMessage
					.calculateLength(message.getMessageBody(), false);
			Log.i(TAG, "" + msgLen[0] + " " + msgLen[1] + " " + msgLen[2] + " "
					+ msgLen[3]);

			// Log.i(TAG, "Protocol = " + message.getProtocolIdentifier());
			Log.i(TAG, "LENGTH: " + incomingMsg.length());

			// If there are other messages from this sender, concatenate them
			String text = msgTexts.get(originatingNumber);
			if (text != null) {
				// Concatenate
				msgTexts.put(originatingNumber, text + incomingMsg);
			} else {
				msgTexts.put(originatingNumber, incomingMsg);
			}
		}

		// Process messages
		for (Entry<String, String> entry : msgTexts.entrySet()) {
			Log.i(TAG, "Processing message: " + entry.getValue());
			// Check prefix
			if (entry.getValue().substring(0, 2).equals(
					SmsTransmitter.FIND_PREFIX)) {
				Log
						.i(TAG,
								"Prefix of message matches Find prefix. Attempting to parse.");
			} else {
				Log
						.i(TAG,
								"Prefix of message does not match Find prefix. Ignoring.");
				continue;
			}

			// Try to parse the message as a find
			Find find = parseFindMessage(entry.getValue().substring(2));
			if (find == null) {
				Log.e(TAG, "SMS message could not be parsed as a Find");
			} else {
				Log.i(TAG, "SMS message parsed as a Find successfully!");
				// So now we need to notify the user
				String ns = Context.NOTIFICATION_SERVICE;
				NotificationManager notificationMgr = (NotificationManager) context
						.getSystemService(ns);
				int icon = R.drawable.notification_icon;
				CharSequence tickerText = "SMS Find received!";
				long when = System.currentTimeMillis();
				Notification notification = new Notification(icon, tickerText,
						when);
				Context appContext = context.getApplicationContext();
				CharSequence contentTitle = "SMS received";
				CharSequence contentText = "from " + entry.getKey();
				// When the user selects the Notification it should open
				// SmsViewActivity
				Intent notificationIntent = new Intent(appContext,
						SmsViewActivity.class);
				notificationIntent.putExtra("findbundle", find.getDbEntries());
				notificationIntent.putExtra("sender", entry.getKey());
				notificationIntent.putExtra("notificationid", mNextNotificationId);
				PendingIntent contentIntent = PendingIntent.getActivity(
						context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				notification.contentIntent = contentIntent;
				notification.setLatestEventInfo(context, contentTitle,
						contentText, contentIntent);
				notificationMgr.notify(mNextNotificationId++, notification);
			}
		}

	}

	/**
	 * Attempts to parse a String message as a Find object.
	 * 
	 * @param message
	 *            A String that may or may not actually correspond to a Find
	 *            object
	 * @return A Find object, or null if it couldn't be parsed.
	 */
	@SuppressWarnings("unchecked")
	private Find parseFindMessage(String message) {
		// Separate message into values, making sure to take escape
		// characters into account as we go
		List<String> values = new ArrayList<String>();
		StringBuilder current = new StringBuilder();
		for (int i = 0; i < message.length(); i++) {
			char c = message.charAt(i);
			if (c == ObjectCoder.ESCAPE_CHAR) {
				// Add this character and the next character to the current
				// string without checking for delimiters
				current.append(c);
				if (i + 1 < message.length())
					c = message.charAt(++i);
				current.append(c);
			} else if (c == ',') {
				// Delimiter. Finish up string and start new one.
				values.add(current.toString());
				current = new StringBuilder();
			} else {
				current.append(c);
			}
		}
		values.add(current.toString());

		// Attempt to construct a Find
		Find find;
		try {
			FindPlugin plugin = FindPluginManager.mFindPlugin;
			if (plugin == null) {
				Log.e(TAG, "Could not retrieve Find Plugin.");
				return null;
			}
			find = plugin.getmFindClass().newInstance();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		}
		// Need to get attributes for the Find
		Bundle bundle = find.getDbEntries();
		List<String> keys = new ArrayList<String>(bundle.keySet());
		// Important to sort so that we process attributes in the same order on
		// both ends
		Collections.sort(keys);
		// Now try to put values with attributes
		if (values.size() != keys.size()) {
			Log.e(TAG,
					"Received value set does not have expected size. values = "
							+ values.size() + ", keys = " + keys.size());
			return null;
		}
		for (int i = 0; i < values.size(); i++) {
			String key = keys.get(i);
			// Get type of this entry
			Class<Object> type;
			try {
				type = find.getType(key);
			} catch (NoSuchFieldException e) {
				// No such field. This shouldn't happen, since we're pulling the
				// keys from our own find
				Log.e(TAG, "Encountered no such field exception on field: "
						+ key);
				e.printStackTrace();
				return null;
			}
			// See if we can decode this value. If not, then we can't make a
			// Find.
			Serializable obj;
			try {
				obj = (Serializable) ObjectCoder.decode(values.get(i), type);
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "Failed to decode value for attribute \"" + key
						+ "\", string was \"" + values.get(i) + "\"");
				return null;
			}
			// Decode successful!
			bundle.putSerializable(key, obj);
		}
		// Make Find
		find.updateObject(bundle);
		return find;
	}
}
