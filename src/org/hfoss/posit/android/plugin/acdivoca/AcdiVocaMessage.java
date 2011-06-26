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

public class AcdiVocaMessage {
	
	public static final String TAG = "AcdiVocaMessage";
	
	public static final String ACDI_VOCA_PREFIX = "AV";
	public static final String ACK = "ACK";
	public static final String IDS = "IDS";
	public static final boolean EXISTING = true;

	
	private int messageId = -1;       // row Id of message in our Db
	private int msgStatus = -1;
	private int beneficiaryId;        // row Id of beneficiary in our Db
	private String rawMessage;	     // Attr/val pairs with long attribute names
	private String smsMessage;       // abbreviated Attr/val pairs
	private String msgHeader =""; 
	private boolean existing = !EXISTING;  // Built from an existing message or, eg, a PENDING)
	private int broadcastResult;           // Result broadcast by SMS radio
	
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

	public int getBroadcastResult() {
		return broadcastResult;
	}

	public void setBroadcastResult(int broadcastResult) {
		this.broadcastResult = broadcastResult;
	}

	@Override
	public String toString() {
		return msgHeader + AttributeManager.PAIRS_SEPARATOR + smsMessage;
	}
}
