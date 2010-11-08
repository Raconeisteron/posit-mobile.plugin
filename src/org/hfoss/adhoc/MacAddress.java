package org.hfoss.adhoc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigInteger;

import android.util.Log;

/**
 * Convenience class for storing MacAddresses and getting byte arrays and String
 * representations
 * 
 * @author pgautam
 * 
 */
public class MacAddress implements Serializable {
	private static final String TAG = "MacAddress";
	// ONLY use ZERO_MAC for debugging/ can cause confusion
	public static final String ZERO_MAC = "00:00:00:00:00:00";
	private String macAddress = null;

	public MacAddress(String mac) {

		macAddress = mac;
	}

	/**
	 * gets the unsignedbytes and converts to hex string format that's saved
	 * 
	 * @param unsignedBytes
	 */
//	public MacAddress(UnsignedByte[] unsignedBytes) {
//		if (unsignedBytes.length < 6) {
//			throw new UnsupportedOperationException(
//					"MacAddresses are 6 bytes long. Double check your input!");
//		}
//		macAddress = "";
//		for (int i = 0; i < 6; i++) {
//			macAddress += String.format("%02x", unsignedBytes[i].toInt());
//			if (i < 5) {
//				macAddress += ":";
//			}
//		}
//	}

	private long unsignedByteToLong(byte b) {
	    return (long) b & 0xFF;
	}
	

	/**
	 * gets the long value from byte array
	 * @param addr
	 */
	private long byte2Long(byte addr[]) {
	    long address = 0;
		if (addr != null) {
		    if (addr.length == 6) {
			address = unsignedByteToLong(addr[5]);
			address |= (unsignedByteToLong(addr[4]) << 8);
			address |= (unsignedByteToLong(addr[3]) << 16);
			address |= (unsignedByteToLong(addr[2]) << 24);
			address |= (unsignedByteToLong(addr[1]) << 32);
			address |= (unsignedByteToLong(addr[0]) << 40);
		    } 
		} 
		return address;
	}	
	

	private String bytesToString(byte[] bytes,char ch){
		StringBuffer sb = new StringBuffer( 17 );
		for ( int i=44; i>=0; i-=4 ) {
			int nibble =  ((int)( byte2Long(bytes) >>> i )) & 0xf;
			char nibbleChar = (char)( nibble > 9 ? nibble + ('A'-10) : nibble + '0' );
			sb.append( nibbleChar );
			if ( (i & 0x7) == 0 && i != 0 ) {
				sb.append( ch );
			}
		}
		return sb.toString();	
	}
	
	public MacAddress(byte[] bytes) {
		if (bytes.length < 6) {
			throw new UnsupportedOperationException(
					"MacAddresses are 6 bytes long. Double check your input!");
		}
		BigInteger bi = new BigInteger(bytes);
		String s = bi.toString(16); // 120ff0 
		
//		Log.i(TAG, " BigInteger string = " + s);
		macAddress = "";
		for (int i = 0; i < 6; i++){
//			Log.i(TAG, " loop: " + ((int)bytes[i] & 0xff)+"");
			macAddress += Integer.toString( ( bytes[i] & 0xff ) + 0x100, 16).substring( 1 );
			if (i < 5) {
				macAddress += ":";
			}
//			Log.i(TAG, String.format("%02x", bytes[i]));
		}
		
		//macAddress = bytesToString(bytes, ':');

	}

	public MacAddress() {
		macAddress = ZERO_MAC;
	}

	/**
	 * convert to byteArray as required by functions
	 * 
	 * @return
	 */
//	public UnsignedByte[] toByteArray() {
//		if (macAddress == null) {
//			throw new NullPointerException("No MacAddress Set");
//		}
//		String[] macAddr = macAddress.toUpperCase().split(":");
//		UnsignedByte[] ub = new UnsignedByte[6];
//		for (int i = 0; i < macAddr.length; i++) {
//			// Log.i(TAG+"unsigned", macAddr[i]);
//			char[] chars = macAddr[i].toCharArray();
//			int c = 0;
//			c = (int) (Character.isDigit(chars[0]) ? (chars[0] - '0')
//					: (chars[0] - 'A' + 10));
//			c <<= 4; // left shift by 4 bits a.k.a multiply by 16
//
//			c += (int) (Character.isDigit(chars[1]) ? (chars[1] - '0')
//					: (chars[1] - 'A' + 10));
//
//			ub[i] = new UnsignedByte(c);
//			// Log.i(TAG+"unsigned", ub[i] + "");
//		}
//		return ub;
//	}

	 public byte[] tosignedByteArray() {
	 if (macAddress == null) {
	 throw new NullPointerException("No MacAddress Set");
	 }
	 String[] macAddr = macAddress.toUpperCase().split(":");
	 byte[] ub = new byte[6];
	 for (int i = 0; i < macAddr.length; i++) {
	 //Log.i(TAG+"signed", macAddr[i]);
	 char[] chars = macAddr[i].toCharArray();
	 int c = 0;
	 c = (int) (Character.isDigit(chars[0]) ? (chars[0] - '0')
	 : (chars[0] - 'A' + 10));
	 c <<= 4; // left shift by 4 bits a.k.a multiply by 16
	
	 c += (int) (Character.isDigit(chars[1]) ? (chars[1] - '0')
	 : (chars[1] - 'A' + 10));
	
	 ub[i] = (byte)c;
	// Log.i(TAG+"signed", ub[i] + "");
	 }
	 return ub;
	 }

//	public long toLong() {
//		UnsignedByte[] ubs = toByteArray();
//		long address = 0;
//		if (ubs != null) {
//			if (ubs.length == 6) {
//				address = ubs[5].toInt();
//				address |= ubs[4].toInt() << 8;
//				address |= ubs[3].toInt() << 16;
//				address |= ubs[2].toInt() << 24;
//				address |= ubs[1].toInt() << 32;
//				address |= ubs[0].toInt() << 40;
//			}
//		}
//		return address;
//	}

	/**
	 * gets a byteString to be transferred over the network. 
	 * 
	 * @return
	 */
//	public String toByteString() {
//		UnsignedByte[] ubs = toByteArray();
//
////		byte[] bs = tosignedByteArray();
//		// long address = toLong();
//		//		
//		// StringBuffer sb = new StringBuffer();
//		// for (int i = 40; i>=0; i -=8){
//		// int b = ((int) (address >>> i)) & 0xf;
//		// sb.append(b);
//		// }
//		// //
//		 byte[] bs = new byte[6];
//		 for (int i = 0; i < 6; i++){
//		 bs[i] = (byte)ubs[i].toInt() ;
//		 }

//		StringBuffer sb = new StringBuffer();
//		Log.e(TAG, String.format("%c", ubs[5].toInt()));
//		 return new String(bs);
//		return String.format("%c%c%c%c%c%c", ubs[0].toInt(), ubs[1].toInt(),
//				ubs[2].toInt(), ubs[3].toInt(), ubs[4].toInt(), ubs[5].toInt());
//		for (int i = 0; i < 6; i+=2){
//			char r = (char)ubs[i].toInt();
//			r <<= 8;
//			r += (char) ubs[i+1].toInt();
//			sb.append(String.format("%c", (char)r));
//		}
//		return sb.toString();
//		StringWriter sw = new StringWriter();
//		sw.write(bs, 0, bs.length);
//		return sw.toString();
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		try {
//			baos.write(bs);
//		} catch (IOException e) {
//			Log.e(TAG, "can't write bytearray");
//		}
//		return baos.toString();
//	}

	@Override
	public boolean equals(Object o) {
		return o.toString().equals(macAddress);
	}

	/**
	 * returns the String representation of bytes
	 */
	@Override
	public String toString() {
		return macAddress;
	}

}
