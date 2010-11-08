package org.hfoss.adhoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

import org.hfoss.posit.android.Find;
import org.hfoss.posit.android.ListFindsActivity;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.provider.PositDbHelper;
import org.hfoss.posit.android.utilities.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Defines an adhoc peer-to-peer network over Datagram Sockets.
 * 
 */
public class Adhoc {
	private static final String TAG = "Adhoc";
	private static Adhoc instance = null;
	public static final String PROTOCOL_ID = "RWG";
	public static final int HEADER_SIZE = 23; // This is subject to change but the concepts will remain the same

	public static int newFindsNum = 0;
	
	public static Adhoc getInstance(Context cxt) {
		Log.d(TAG, "getInstance()");
		if (instance == null) {
			instance = new Adhoc(cxt);
		}
		return instance;
	}

	private int mPort = 4959;
	private int maxLength = 1024;
	private DatagramSocket listeningSocket = null;
	private DatagramSocket bcastSocket = null;
	private boolean stop_requested = false;
//	private boolean sending= false;
	private Context mContext;
	//private MacAddress mMacAddress;
	private String mMyMacAddress;
	
	private NotificationManager mNotificationManager;
	private Notification mNotification;

	private Adhoc(Context cxt) {
		mContext = cxt;
		mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

		try {
			Log.i(TAG, "Adhoc() trying to create listening socket");
			listeningSocket = new DatagramSocket(mPort);
			listeningSocket.setReuseAddress(true);
		} catch (SocketException e) {
			Log.e(TAG, "failed to create listening socket " + e.getMessage());
		}
		try {
			Log.i(TAG, "Adhoc() trying to create broadcast socket");
			bcastSocket = new DatagramSocket();
		} catch (SocketException e) {
			Log.e(TAG, "failed to create bcast socket " + e.getMessage());
		}
	}

	
	/**
	 * Records the Mac Address of the device starting this service instance.
	 * @param addr
	 */
	public void setMacAddress(String addr) {
		mMyMacAddress = addr;
	}
	
	/**
	 * The listening thread.
	 */
	public void listen()  {
		if (listeningSocket == null) {
			Log.e(TAG, "Listening socket is null");
			return;
		}
		try {
			InetAddress IPAddress = InetAddress.getByName("");
		} catch (UnknownHostException e1) {
			Log.e(TAG, "cannot get address");
		}
		 
		Log.i(TAG, "listen() starting the listening loop");
		while (!stop_requested) {
			byte[] buf = new byte[maxLength];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
		 	
			try {
				listeningSocket.receive(packet);
				Log.d(TAG, "listen() received a packet");
 
				byte[] rwg_payload = packet.getData();
				AdhocData<AdhocFind> data = AdhocData.readFromBytes(rwg_payload);
				if (data.getProtocol().equals(AdhocData.PROTOCOL_RWG)) {
					Log.d(TAG, "Received RWG packet, data = " + data);
					
					// Is this my own packet?
					MacAddress senderMac = data.getSender();
					Log.d(TAG, "MyMac = " + mMyMacAddress + " senderMac = " + senderMac);
					if (senderMac.toString().equalsIgnoreCase(mMyMacAddress)) {

						Log.d(TAG, "Ignoring packet -- looks like mine");
					} else {
						Log.d(TAG, "Received a packet from sender = " + senderMac);
						
						// Now do something with the RWG packet
						ContentValues values = new ContentValues();
						AdhocFind adhocFind = (AdhocFind)data.getMessage();
						values.put(PositDbHelper.FINDS_NAME, adhocFind.getName());
						values.put(PositDbHelper.FINDS_DESCRIPTION, adhocFind.getDescription());
						values.put(PositDbHelper.FINDS_LONGITUDE, adhocFind.getLongitude());
						values.put(PositDbHelper.FINDS_LATITUDE, adhocFind.getLatitude());
						values.put(PositDbHelper.FINDS_GUID, adhocFind.getId());
						values.put(PositDbHelper.FINDS_PROJECT_ID, adhocFind.getProjectId());
						values.put(PositDbHelper.FINDS_IS_ADHOC, 1);

						Find find = new Find(mContext);
						//find.setGuid(adhocFind.getId());
						if (find.exists(adhocFind.getId())) {
							Log.i(TAG, "Find already exists");
							find.updateToDB(adhocFind.getId(), values);
							Utils.showToast(mContext, "Updating existing adhoc find");
						} else {
							find.insertToDB(values, null);
							Utils.showToast(mContext, "Saving new adhoc find");
						}
						notifyNewFind(adhocFind.getName(), adhocFind.getDescription());
						Log.d(TAG, "Inserted find into POSIT Db");
					}
				} else {
					Log.d(TAG, "Ignoring packet -- not an RWG packet");
				}
			} catch (IOException e) {
				Log.e(TAG, "IOException " + e.getMessage());
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}  
		Log.i(TAG, "Exiting the listening loop, stop_requested = " + stop_requested);
		Utils.showToast(mContext, "Exiting listen loop");
	}
	
	/**
	 * The sending (broadcast) thread.
	 */
	public void sendData() {
		Log.i(TAG, "Starting sendData() queue size =" + Queues.outputQueue.size());
		while (!stop_requested) {
			try {
				AdhocData<AdhocFind> data = Queues.outputQueue.take(); // Includes RWG protocol
				Log.e(TAG,  "sendData() sending data = " + data);
//				sending = true;
				byte[] rwg_payload = data.writeToBytes();  // Serialize the data
				broadcast(rwg_payload);
//				sending = false;
			} catch (InterruptedException e) {
				Log.e(TAG, "interrupted");
				e.printStackTrace();
			} catch (IOException e) {
				Log.e(TAG, "IOException");
				e.printStackTrace();
			} catch (Exception e) {
				Log.e(TAG, "Exception " + e.getMessage());
				e.printStackTrace();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "Thread interrupted");
			}
		}
		Log.d(TAG,"Exiting the sending loop, stop_requested = " + stop_requested);
		Utils.showToast(mContext, "Exiting send loop");
	}

	/**
	 * A special definition exists for the IP broadcast address 255.255.255.255. 
	 * It is the broadcast address of the zero network (0.0.0.0), which in 
	 * Internet Protocol standards stands for this network, i.e. the local network. 
	 * Transmission to this address is limited by definition, in that it 
	 * does not cross the routers connecting the local network to the Internet.
	 * @param bytes
	 */
	private void  broadcast (byte[] bytes){
		Log.i(TAG, "broadcast() bytes size =" + bytes.length);

		InetAddress IPAddress;
		try {
			IPAddress = InetAddress.getByName("255.255.255.255"); 
			DatagramPacket packet = new DatagramPacket(bytes, bytes.length,IPAddress, mPort);

			bcastSocket.setBroadcast(true);
			Log.i(TAG, "broadcast() sending a packet ");
			bcastSocket.send(packet);
		} catch (UnknownHostException e) {
			Log.e(TAG, "UnknownHostException");
			e.printStackTrace();
		} catch (SocketException e) {
			Log.e(TAG, "SocketException");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "IOException");
			e.printStackTrace();
		}
	}
	
	/**
	 * Notifies the user that an adhoc find has been received.
	 * @param name
	 * @param description
	 */
    public void notifyNewFind(String name, String description) {
    	newFindsNum++;
    	
    	//int icon = R.drawable.notification_icon;        // icon from resources
    	CharSequence tickerText = "New RWG Find";              // ticker-text
    	long when = System.currentTimeMillis();         // notification time
    	CharSequence contentTitle = "New RWG Find";  // expanded message title
    	CharSequence contentText= "";
    	if(newFindsNum==1) {
    		contentText = "Name: "+name+" | Description: "+description;      // expanded message text
    	}
    	else {
    		contentText = newFindsNum+" unviewed RWG Finds";
    	}
    	Intent notificationIntent = new Intent(mContext, ListFindsActivity.class);
    	PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

    	// the next two lines initialize the Notification, using the configurations above
    	Notification notification = new Notification(R.drawable.icon, tickerText, when);
    	notification.setLatestEventInfo(mContext, contentTitle, contentText, contentIntent);
    	notification.defaults |= Notification.DEFAULT_SOUND;
    	notification.defaults |= Notification.DEFAULT_VIBRATE;
    	notification.defaults |= Notification.DEFAULT_LIGHTS;
    	notification.ledARGB = 0xff0000ff;
    	notification.ledOnMS = 300;
    	notification.ledOffMS = 1000;
    	notification.flags |= Notification.FLAG_SHOW_LIGHTS;
    	notification.flags |= Notification.FLAG_AUTO_CANCEL;
    	mNotificationManager.notify(AdhocService.NEWFIND_NOTIFICATION, notification);
    }
    
	public void stopListening() {
		Log.i(TAG, "Someone called stopListening");
		stop_requested = true;
	}

}
