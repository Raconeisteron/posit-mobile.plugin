package org.hfoss.posit.rwg;

public interface Packet {
		
	public byte[] toBytes();
	
	public String toString();
	
	//public void parseBytes(byte[] rawPdu) throws BadPduFormatException;

	public String getDestinationAddress(); // MAC Address
}
