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
	
//
	public static final String ATTR_VAL_SEPARATOR = "=";
	public static final String PAIRS_SEPARATOR = ",";
	
	public static final int MAX_MESSAGE_LENGTH = 140;
	public static final int MAX_PHONE_NUMBER_LENGTH = 10;
	public static final int MIN_PHONE_NUMBER_LENGTH = 5;
	
	
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

		String message = acdiVocaMessage.ACDI_VOCA_PREFIX + ATTR_VAL_SEPARATOR +
			+ acdiVocaMessage.getBeneficiaryId() +   PAIRS_SEPARATOR
			+ acdiVocaMessage.getSmsMessage();
		
		if (checkPhoneNumber(phoneNumber)
				&& message.length() > 0 
				&& message.length() <= MAX_MESSAGE_LENGTH) {
			try {
				SmsManager sms = SmsManager.getDefault();
				//sms.sendTextMessage(phoneNumber, null, message, sentIntent, deliveryIntent);    
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
		} else if (attr.equals(AcdiVocaDbHelper.MESSAGE_TEXT)) {
			attrAbbrev="t";
			valAbbrev = "[" + val + "]";
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
		} else if (attr.equals(AcdiVocaDbHelper.MESSAGE_BENEFICIARY_ID)) {
			attrAbbrev = "#";
			valAbbrev = val;
		} else if (attr.equals(AcdiVocaDbHelper.MESSAGE_CREATED_AT)) {
			attrAbbrev = "t1";
			valAbbrev = val;
		} else if (attr.equals(AcdiVocaDbHelper.MESSAGE_SENT_AT)) {
			attrAbbrev = "t2";
			valAbbrev = val;
		} else if (attr.equals(AcdiVocaDbHelper.MESSAGE_ACK_AT)) {
			attrAbbrev = "t3";
			valAbbrev = val;
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
