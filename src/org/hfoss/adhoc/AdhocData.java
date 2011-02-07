/*
 * File: AdhocData.java
 * 
 * Copyright (C) 2010 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Search and Identification Tool.
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
package org.hfoss.adhoc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.content.Context;
import android.util.Log;

/**
 * The payload for an RWG packet.
 *
 * @param <T>, for POSIT T will usually be an AdhocFind.
 */
public class AdhocData<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String PROTOCOL_RWG = "RWG";
	private static final String TAG = "Adhoc";
	
//	private String protocol = PROTOCOL_RWG;
//	private short packetLength=0;
//	private byte messageType=0;
//	private byte hops=0;
//	private short TTL=0;
//	private short groupSize=0;
//	private short sequenceNumber=0;
//	private String origin;   // MAC Addresses
//	private String target; 
//	private String sender; 
//	private short[] visited = new short[16];
//	private short[] recentVisited = new short[16];
	private T message;

	public AdhocData() {
//		packetLength = 128;
//		messageType = 0;
//		hops = 2;
//		TTL = 12;
//		groupSize = 6;
//		sequenceNumber = 1;
//		origin = new String("10:01:10:01:10:01");
//		target = new String("20:02:20:02:20:02");
//		sender = new String("30:03:30:03:30:03");
		message = (T)"Hello";
	}
	
	
	public AdhocData(Context cxt, T msg) {
		this();
		message = (T)msg;
		String mac = AdhocService.getMacAddress(cxt);
		if (mac != null) {
//			origin = mac;
//			sender = mac;
		}
	}
	
	public AdhocData(T msg) {
//		messageType = 0;
//		hops = 0;
//		TTL = 0;
//		groupSize = 0;
//		sequenceNumber = 0;
		message = msg;

	}
	
	/**
	 * Write this object to a serialized byte array
	 * @param baos
	 * @throws IOException
	 */
	public byte[] writeToBytes() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(this);
		oos.flush();
		return baos.toByteArray();
	}
	
	/**
	 * Reads an instance of AdhocData from a serialized byte stream.
	 * @param bais
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static AdhocData readFromBytes(byte[] bytes) 
					throws IOException,	ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bais);
		AdhocData data = (AdhocData)ois.readObject();

		// For development/debug 
//		Log.d(TAG, "protocol = " + data.protocol);
//		Log.d(TAG, "packetLength " + data.packetLength);
//		Log.d(TAG, "messageType = " + data.messageType);
//		Log.d(TAG, "hops = " + data.hops);
//		Log.d(TAG, "TTL = " + data.TTL);
//		Log.d(TAG, "groupSize = " + data.groupSize);
//		Log.d(TAG, "sequenceNumber = " + data.sequenceNumber);
//		Log.d(TAG, "origin = " + data.origin);
//		Log.d(TAG, "target = " + data.target);
//		Log.d(TAG, "sender = " + data.sender);
		Log.d(TAG, "message = " + data.message);
		return data;
	}	

//	public String getProtocol() {
//		return protocol;
//	}
//	public void setProtocol(String protocol) {
//		this.protocol = protocol;
//	}
//	
//	
//	public short getPacketLength() {
//		return packetLength;
//	}
//
//	public void setPacketLength(short packetLength) {
//		this.packetLength = packetLength;
//	}
//
//	public byte getMessageType() {
//		return messageType;
//	}
//
//	public void setMessageType(byte messageType) {
//		this.messageType = messageType;
//	}
//
//	public byte getHops() {
//		return hops;
//	}
//
//	public void setHops(byte hops) {
//		this.hops = hops;
//	}
//
//	public short getGroupSize() {
//		return groupSize;
//	}
//
//	public void setGroupSize(short groupSize) {
//		this.groupSize = groupSize;
//	}
//
//	public short[] getVisited() {
//		return visited;
//	}
//
//	public void setVisited(short[] visited) {
//		this.visited = visited;
//	}
//
//	public short[] getRecentVisited() {
//		return recentVisited;
//	}
//
//	public void setRecentVisited(short[] recentVisited) {
//		this.recentVisited = recentVisited;
//	}
//
//	public String getOrigin() {
//		return origin;
//	}
//
//	public void setOrigin(String origin) {
//		this.origin = origin;
//	}
//
//	public String getTarget() {
//		return target;
//	}
//
//	public void setTarget(String target) {
//		this.target = target;
//	}
//
//	public String getSender() {
//		return sender;
//	}
//
//	public void setSender(String sender) {
//		this.sender = sender;
//	}
//
//
//	public short getTTL() {
//		return TTL;
//	}
//
//	public void setTTL(short tTL) {
//		TTL = tTL;
//	}
//
//	public short getSequenceNumber() {
//		return sequenceNumber;
//	}
//
//	public void setSequenceNumber(short sequenceNumber) {
//		this.sequenceNumber = sequenceNumber;
//	}

	public T getMessage() {
		return message;
	}

	public void setMessage(T message) {
		this.message = message;
	}

	@Override
	public String toString() {
		String result = "null";
		try {
			//result = String.format("%d %d %d %d %d %s %s %s %s %s",
			result = String.format("%s %s",
//					messageType, hops, TTL, groupSize, sequenceNumber, 
//					origin.toString(), 
//					target.toString(), 
//					sender.toString(),
					message.toString().getBytes().length, 
					message.toString());
		} catch (NullPointerException ne) {
			Log.e(TAG,
					"NullPointerException when creating string for AdhocData "
							+ ne.getMessage());
			ne.printStackTrace();
		}
		return result;
	}
}
