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

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class AcdiVocaSmsManager {
	
	public static final String TAG = "AcdiVocaSmsManager";
	
	public static final String ATTR_VAL_SEPARATOR = "=";
	public static final String PAIRS_SEPARATOR = "&";
	
	
	private static boolean checkNumber(String number) {
		for(int i = 0; i < number.length(); i++) {
			if(number.charAt(i)<'0'|| number.charAt(i)>'9')
				if(!(i==0&&number.charAt(i)=='+'))
					return false;
		}
		return true;
	}
	
	public static void sendMessage(Context context, String message, String phoneNumber) {
		if (phoneNumber==null)
			phoneNumber = PreferenceManager.getDefaultSharedPreferences(context).getString("smsPhone", "");
		
		//Toast.makeText(context, "SMS Phone Target = " + phoneNumber, Toast.LENGTH_SHORT).show();		
		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";        
		PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0,
				new Intent(SENT), 0);

		PendingIntent deliveryIntent = PendingIntent.getBroadcast(context, 0,
				new Intent(DELIVERED), 0);

		if(phoneNumber.length()>0 && message.length()>0 && message.length()<=160 && checkNumber(phoneNumber)) {
			try {
				SmsManager sms = SmsManager.getDefault();
				sms.sendTextMessage(phoneNumber, null, message, sentIntent, deliveryIntent);    
				Toast.makeText(context, "SMS Sent!\n"+message + " to " + phoneNumber, Toast.LENGTH_LONG).show();
				Log.i(TAG,"SMS Sent: " + message);
			}catch(Exception e){Log.i("TEST",e.toString());}
		}
		else
			Toast.makeText(context, "SMS Failed\nCheck phone number or length of message", Toast.LENGTH_LONG).show();
		
	}
	
	public static String formatSmsMessage(ContentValues values) {
		String message = "fn" + "=" + values.get(AcdiVocaDbHelper.FINDS_FIRSTNAME) + PAIRS_SEPARATOR
		+  "ln" + ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_LASTNAME) + PAIRS_SEPARATOR
		+ "dob" + ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_DOB)  + PAIRS_SEPARATOR
		+ "s" + ATTR_VAL_SEPARATOR  + values.get(AcdiVocaDbHelper.FINDS_SEX) + PAIRS_SEPARATOR
		+ "ad" + ATTR_VAL_SEPARATOR  + values.get(AcdiVocaDbHelper.FINDS_ADDRESS) + PAIRS_SEPARATOR
		+ "a" + ATTR_VAL_SEPARATOR  + values.get(AcdiVocaDbHelper.FINDS_AGE) + PAIRS_SEPARATOR
		+ "c" + ATTR_VAL_SEPARATOR  + values.get(AcdiVocaDbHelper.COMMUNE_NAME) + PAIRS_SEPARATOR
		+ "cs" + ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.COMMUNE_SECTION_NAME) + PAIRS_SEPARATOR
		+ "ic" + ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_INFANT_CATEGORY) + PAIRS_SEPARATOR
		+ "mc" + ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_MOTHER_CATEGORY) + PAIRS_SEPARATOR
		+ "nih" + ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE);
		
		return message;
		
	}
	
}
