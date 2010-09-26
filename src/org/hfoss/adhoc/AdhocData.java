package org.hfoss.adhoc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.opengl.Visibility;
import android.util.Log;

/**
 * The payload for an RWG packet.
 *
 * @param <T>, for POSIT T will usually be an AdhocFind.
 */
public class AdhocData<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String PROTOCOL_RWG = "RWG";

	private static final String TAG = "AdhocData";
	
	private String protocol = PROTOCOL_RWG;
	private short packetLength=0;
	private byte messageType=0;
	private byte hops=0;
	private short TTL=0;
	private short groupSize=0;
	private short sequenceNumber=0;
	private MacAddress origin = new MacAddress();
	private MacAddress target = new MacAddress();
	private MacAddress sender = new MacAddress();
	private short[] visited = new short[16];
	private short[] recentVisited = new short[16];
	private T message;

	public AdhocData() {
		packetLength = 128;
		messageType = 0;
		hops = 2;
		TTL = 12;
		groupSize = 6;
		sequenceNumber = 1;
		origin = new MacAddress("10:01:10:01:10:01");
		target = new MacAddress("20:02:20:02:20:02");
		sender = new MacAddress("30:03:30:03:30:03");
		message = (T)"Hello";
	}
	
	
	public AdhocData(Context cxt, T msg) {
		this();
		message = (T)msg;
		// getMacAddress returns null when using phone as WiFi hotspot??
		MacAddress mac = AdhocService.getMacAddress(cxt);
		if (mac != null) {
			origin = mac;
			sender = mac;
		}
	}
	
	public AdhocData(T msg) {
		messageType = 0;
		hops = 0;
		TTL = 0;
		groupSize = 0;
		sequenceNumber = 0;
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
		Log.d(TAG, "protocol = " + data.protocol);
		Log.d(TAG, "packetLength " + data.packetLength);
		Log.d(TAG, "messageType = " + data.messageType);
		Log.d(TAG, "hops = " + data.hops);
		Log.d(TAG, "TTL = " + data.TTL);
		Log.d(TAG, "groupSize = " + data.groupSize);
		Log.d(TAG, "sequenceNumber = " + data.sequenceNumber);
		Log.d(TAG, "origin = " + data.origin);
		Log.d(TAG, "target = " + data.target);
		Log.d(TAG, "sender = " + data.sender);
		Log.d(TAG, "message = " + data.message);
		return data;
	}	

//	public AdhocData(byte[] bytes) {
//		try {
//			byte[] c = new byte[6];
//			int idx = 13;
//			for (int i = idx; i < idx + 6; i++) {
//				// c[i - idx] = new UnsignedByte(pBytes[i]);
//				c[i - idx] = bytes[i];
//			}
//
//			MacAddress mc = new MacAddress(c);
//			message = (T) ""; 
//			Log.i(TAG, "Construct from bytes, mac address = " + mc);
//			// Log.i(TAG, mc.toByteString()+"");
////			int dataSize = Integer.parseInt(p.substring(0, 2));
////			Log.i(TAG, dataSize + "");
////			Log.i(TAG, p.substring(2, 2 + dataSize));
//			// this needs to be better expressed. HACK
////			message = (T) new AdhocFind(p.substring(2, 2 + dataSize));
//		} catch (Exception e) {
//			Log.e(TAG, "Malformatted data or offset error");
//
//		}
//	}
	
//	/*
//	 * This is for initializing from the received packet string
//	 */
//	public AdhocData(String packet) {
//		String p = packet.substring(Adhoc.HEADER_SIZE);
//
//		Log.i(TAG, "Construct from string, packet= " + p);
//		try {
//			byte[] pBytes = packet.getBytes();
//
//			// UnsignedByte[] c = new UnsignedByte[6];
//			byte[] c = new byte[6];
//
//			int idx = 10;
//			for (int i = idx; i < idx + 6; i++) {
//
//				// c[i - idx] = new UnsignedByte(pBytes[i]);
//				c[i - idx] = pBytes[i];
//			}
//
//			MacAddress mc = new MacAddress(c);
//			Log.i(TAG, "Construct from string, mac address = " + mc);
//			// Log.i(TAG, mc.toByteString()+"");
//			int dataSize = Integer.parseInt(p.substring(0, 2));
//			Log.i(TAG, "Construct from String, datasize = " + dataSize + "");
//			Log.i(TAG, "Construct from String, payload = " + p.substring(2, 2 + dataSize));
//			// this needs to be better expressed. HACK
//			message = (T) new AdhocFind(p.substring(2, 2 + dataSize));
//		} catch (Exception e) {
//			Log.e(TAG, "Malformatted data or offset error");
//
//		}
//
//	}
//	
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	
	public short getPacketLength() {
		return packetLength;
	}

	public void setPacketLength(short packetLength) {
		this.packetLength = packetLength;
	}

	public byte getMessageType() {
		return messageType;
	}

	public void setMessageType(byte messageType) {
		this.messageType = messageType;
	}

	public byte getHops() {
		return hops;
	}

	public void setHops(byte hops) {
		this.hops = hops;
	}

	public short getGroupSize() {
		return groupSize;
	}

	public void setGroupSize(short groupSize) {
		this.groupSize = groupSize;
	}

	public short[] getVisited() {
		return visited;
	}

	public void setVisited(short[] visited) {
		this.visited = visited;
	}

	public short[] getRecentVisited() {
		return recentVisited;
	}

	public void setRecentVisited(short[] recentVisited) {
		this.recentVisited = recentVisited;
	}

	public MacAddress getOrigin() {
		return origin;
	}

	public void setOrigin(MacAddress origin) {
		this.origin = origin;
	}

	public MacAddress getTarget() {
		return target;
	}

//	public void setTarget(UnsignedByte[] targetBytes) {
//		target = new MacAddress(targetBytes);
//	}

	public void setTarget(MacAddress target) {
		this.target = target;
	}

	public MacAddress getSender() {
		return sender;
	}

	public void setSender(MacAddress sender) {
		this.sender = sender;
	}


	public short getTTL() {
		return TTL;
	}

	public void setTTL(short tTL) {
		TTL = tTL;
	}

	public short getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(short sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

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
			result = String.format("%d %d %d %d %d %s %s %s %s %s",
					messageType, hops, TTL, groupSize, sequenceNumber, 
					origin.toString(), 
					target.toString(), 
					sender.toString(),
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

//	public byte[] visitedNodesToBytes(short[] visited) throws IOException {
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		for (short v : visited) {
//			baos.write(AdhocUtils.shortToBytes(v));
//		}
//		return baos.toByteArray();
//	}

//	public byte[] toBytes() {
//		ByteArrayOutputStream baos0 = new ByteArrayOutputStream();
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		try {
//
//			baos0.write(messageType);
//			baos0.write(hops);
//			baos0.write(AdhocUtils.shortToBytes(TTL));
//			baos0.write(AdhocUtils.shortToBytes(groupSize));
//			baos0.write(AdhocUtils.shortToBytes(sequenceNumber));
//			baos0.write(origin.tosignedByteArray());
//			baos0.write(target.tosignedByteArray());
//			baos0.write(sender.tosignedByteArray());
//			baos0.write(visitedNodesToBytes(visited));
//			baos0.write(visitedNodesToBytes(recentVisited));
//			short messageLength = (short) (message.toString().getBytes().length);
//			baos0.write(AdhocUtils.shortToBytes(messageLength));
//			baos0.write(message.toString().getBytes());
//			// find the packet length and write it to the byteArray
//			packetLength = (short) baos0.toByteArray().length;
//			baos.write(AdhocUtils.shortToBytes(packetLength));
//			// write the bytearray with all the data
//			baos.write(baos0.toByteArray());
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			Log.e(TAG, "cannot write");
//		}
//		return baos.toByteArray();
//	}

}
