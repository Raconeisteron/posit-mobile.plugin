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
	
	public static boolean sendMessage(Context context, String message, String phoneNumber) {
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
			}catch(Exception e) {
				Log.i(TAG,e.toString());
				e.printStackTrace();
			}
			return true;
		}
		else {
			Toast.makeText(context, "SMS Failed\nCheck phone number or length of message", Toast.LENGTH_LONG).show();
			return false;
		}
	}
	
	public static String formatAcdiVocaMessage(String rawMessage) {
		String msg = "";
		
		return msg;
	}
	
	
	
	/**
	 * Converts attribute-value pairs to abbreviated attribute-value pairs.
	 * It currently uses hard code. It should treat the attributes and some
	 * of the values as data.
	 * @param attr
	 * @param val
	 * @return  a string of the form a1=v1,a2=b2, ..., aN=vN
	 */
	public static String convertAttrValToAbbrev(String attr, String val) {
		String attrAbbrev = "";
		String valAbbrev = "";
		//Log.i(TAG, attr + "=" + val);
		if (attr.equals(AcdiVocaDbHelper.FINDS_DOSSIER)) {
			attrAbbrev = "i";
			valAbbrev = val;
		} else if (attr.equals(AcdiVocaDbHelper.FINDS_TYPE)) {
			attrAbbrev="t";
			valAbbrev = val;
		} else if (attr.equals(AcdiVocaDbHelper.FINDS_MESSAGE_TEXT)) {
			return "";
		} else if (attr.equals(AcdiVocaDbHelper.FINDS_MESSAGE_STATUS)) {
			return "";
//			attrAbbrev="m";
//			valAbbrev = val;
		} else if (attr.equals(AcdiVocaDbHelper.FINDS_FIRSTNAME)) {
			attrAbbrev =  "f";
			valAbbrev = val;
		}
		else if (attr.equals(AcdiVocaDbHelper.FINDS_LASTNAME)) {
			attrAbbrev = "l";
			valAbbrev = val;
		}
		else if (attr.equals(AcdiVocaDbHelper.FINDS_ADDRESS)) {
			attrAbbrev = "a";
			valAbbrev = val;
		} else if (attr.equals(AcdiVocaDbHelper.FINDS_DOB)) {
			attrAbbrev = "b";
			valAbbrev = val;
		}
		else if (attr.equals(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE)) {
			attrAbbrev = "n";
			valAbbrev = val;
		}
		else if (attr.equals(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY))  {
			attrAbbrev = "c";
			valAbbrev = convertValToAbbrev(val);
		}
		else if (attr.equals(AcdiVocaDbHelper.FINDS_SEX)) {
			attrAbbrev = "s";
			valAbbrev = convertValToAbbrev(val);
		}
		else if (attr.equals(AcdiVocaDbHelper.FINDS_HEALTH_CENTER)) {
			attrAbbrev = "h";
			valAbbrev = ""+ (int)(Math.random() * 30); //convertValToAbbrev(val);
		}
		else if (attr.equals(AcdiVocaDbHelper.FINDS_DISTRIBUTION_POST)) {
			attrAbbrev = "d";
			valAbbrev = ""+ (int)(Math.random() * 30); // convertValToAbbrev(val);
		}
		else {
			attrAbbrev = attr;
			valAbbrev = val;
		}
		return attrAbbrev + ATTR_VAL_SEPARATOR + valAbbrev;
	}
	
	/**
	 * Convert a a value to an abbreviated value. This is mostly
	 * used for the names of health centers and distribution posts.
	 * @param val a String of the form "Name of Some Health Center"
	 * @return a String of the form "1" representing that health center
	 */
	public static String convertValToAbbrev(String val) {
		if (val.equals("FEMALE"))
			return "F";
		else if (val.equals("MALE"))
			return "M";
		else if (val.equals("EXPECTING") || val.equals("Femme Enceinte")) 
			return "E";
		else if (val.equals("NURSING") || val.equals("Femme Allaitante")) 
			return "N";
		else if (val.equals("PREVENTION") || val.equals("Enfant Prevention"))
			return "P";
		else if (val.equals("MALNOURISHED") || val.startsWith("Enfant Mal"))
			return "M";
		else
			return val;
	}

	
	public static String formatSmsMessage(ContentValues values) {
		String message = 
		"m" + ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_TYPE) + PAIRS_SEPARATOR
		    +  "f" + ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_FIRSTNAME) + PAIRS_SEPARATOR
			+  "l" + ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_LASTNAME) + PAIRS_SEPARATOR
			+ "b" + ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_DOB)  + PAIRS_SEPARATOR
			+ "s" + ATTR_VAL_SEPARATOR  + values.get(AcdiVocaDbHelper.FINDS_SEX) + PAIRS_SEPARATOR
			+ "a" + ATTR_VAL_SEPARATOR  + values.get(AcdiVocaDbHelper.FINDS_ADDRESS) + PAIRS_SEPARATOR
			+ "c" + ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_BENEFICIARY_CATEGORY) + PAIRS_SEPARATOR
			+ "n" + ATTR_VAL_SEPARATOR + values.get(AcdiVocaDbHelper.FINDS_HOUSEHOLD_SIZE);
		
		return message;
		
	}
	
}
