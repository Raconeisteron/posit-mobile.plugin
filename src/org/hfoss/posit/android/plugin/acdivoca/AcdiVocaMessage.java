/*
 * File: AcdiVocaMessage.java
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.util.Log;

public class AcdiVocaMessage {
	
	public static final String TAG = "AcdiVocaMessage";
	
	public static final String ACDI_VOCA_PREFIX = "AV";
	public static final String ACK = "ACK";
	public static final String IDS = "IDS";
	public static final boolean EXISTING = true;

	
	private int messageId = AcdiVocaDbHelper.UNKNOWN_ID;    // row Id of message in our Db
	private int msgStatus = -1;
	private int beneficiaryId;        // row Id of beneficiary in our Db
	private String rawMessage;	     // Attr/val pairs with long attribute names
	private String smsMessage;       // abbreviated Attr/val pairs
	private String msgHeader =""; 
	private boolean existing = !EXISTING;  // Built from an existing message or, eg, a PENDING)
	private String numberSlashBatchSize;   // e.g 1/10  -- i.e., 1st of 10 messages in this batch
	
	public AcdiVocaMessage() {
		
	}
	
	public AcdiVocaMessage(int messageId, int beneficiaryId, int msgStatus,  
			String rawMessage, String smsMessage, String msgHeader, boolean existing) {
		super();
		this.messageId = messageId;
		this.beneficiaryId = beneficiaryId;
		this.msgStatus = msgStatus;
		this.rawMessage = rawMessage;
		this.smsMessage = smsMessage;
		this.msgHeader = msgHeader;
		this.existing = existing;
	}
	
	/**
	 * Construct an instance from an Sms Message. This should be the
	 * converse of the toString() method, which returns this object as
	 * an Sms message. 
	 * @param smsText a string of the form AV=msgid,....  where the ...
	 * is either a comma-separated list of attr=val pairs or the ...
	 * is an ampersand separated list of Ids.  
	 */
	public AcdiVocaMessage(String smsText) {
		Log.i(TAG, "Creating from smstext:" + smsText);
		String[] msgparts = smsText.split(AttributeManager.PAIRS_SEPARATOR);
//		for (int k = 0; k < msgparts.length; k++) {
//			Log.i(TAG, "msgpart " + k + " :" + msgparts[k]);
//		}
		String[] firstPair = msgparts[0].split(AttributeManager.ATTR_VAL_SEPARATOR);
		String msgid = firstPair[1];
		int id = Integer.parseInt(msgid);
		smsMessage = "";
		if (id < 0 && id != AcdiVocaDbHelper.UNKNOWN_ID) {
			messageId = id * -1;
			beneficiaryId =  AcdiVocaDbHelper.UNKNOWN_ID;
		} else {
			beneficiaryId = id;
			messageId =  AcdiVocaDbHelper.UNKNOWN_ID;
		} 
		
		// NOTE: We skip the first 2 pairs in constructing the actual SMS 
		// that was sent.  The first two pairs represent PREFIX information,
		// namely, AV=mid,N:m, ...
		for (int k = 2; k < msgparts.length; k++) {
//			Log.i(TAG, "msgpart " + k + " :" + msgparts[k]);
			smsMessage += msgparts[k] + AttributeManager.PAIRS_SEPARATOR;
		}
		Log.i(TAG, "Resulting sms :" + smsMessage);
	}

	public int getMessageId() {
		return messageId;
	}

	public void setMessageId(int messageId) {
		this.messageId = messageId;
	}

	public int getBeneficiaryId() {
		return beneficiaryId;
	}

	public void setBeneficiaryId(int beneficiaryId) {
		this.beneficiaryId = beneficiaryId;
	}

	
	public int getMsgStatus() {
		return msgStatus;
	}

	public void setMsgStatus(int msgStatus) {
		this.msgStatus = msgStatus;
	}

	public String getRawMessage() {
		return rawMessage;
	}

	public void setRawMessage(String rawMessage) {
		this.rawMessage = rawMessage;
	}

	public String getSmsMessage() {
		return smsMessage;
	}

	public void setSmsMessage(String smsMessage) {
		this.smsMessage = smsMessage;
	}

	public String getMsgHeader() {
		return msgHeader;
	}

	public void setMsgHeader(String msgHeader) {
		this.msgHeader = msgHeader;
	}
	
	public boolean isExisting() {
		return existing;
	}

	public void setExisting(boolean existing) {
		this.existing = existing;
	}
	
	

	public String getNumberSlashBatchSize() {
		return numberSlashBatchSize;
	}

	public void setNumberSlashBatchSize(String numberSlashBatchSize) {
		this.numberSlashBatchSize = numberSlashBatchSize;
	}

	/**
	 * Return an Sms Message.
	 */
	@Override
	public String toString() {
		String message;
		if (beneficiaryId != AcdiVocaDbHelper.UNKNOWN_ID) {
			message = 
				AcdiVocaMessage.ACDI_VOCA_PREFIX 
			+ AttributeManager.ATTR_VAL_SEPARATOR 
			+ getBeneficiaryId() // For normal messages we use the beneficiary's row id, 1...N
			+ AttributeManager.PAIRS_SEPARATOR
			+ AttributeManager.ABBREV_MSG_NUMBER_SLASH_SIZE
			+ AttributeManager.ATTR_VAL_SEPARATOR
			+ getNumberSlashBatchSize()
			+ AttributeManager.PAIRS_SEPARATOR
			+ getSmsMessage();
		} else {
			message = 
				AcdiVocaMessage.ACDI_VOCA_PREFIX 
			+ AttributeManager.ATTR_VAL_SEPARATOR 
			+ getMessageId() * -1   // For Bulk messages we use minus the message id (e.g., -123)
			+ AttributeManager.PAIRS_SEPARATOR
			+ AttributeManager.ABBREV_MSG_NUMBER_SLASH_SIZE
			+ AttributeManager.ATTR_VAL_SEPARATOR
			+ getNumberSlashBatchSize()
			+ AttributeManager.PAIRS_SEPARATOR
			+ getSmsMessage();
		}
		return message;
		//return msgHeader + AttributeManager.PAIRS_SEPARATOR + smsMessage;
	}
}
