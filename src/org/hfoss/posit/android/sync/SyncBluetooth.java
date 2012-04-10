/*
 * File: SyncBluetooth.java
 * 
 * Copyright (C) 2012 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool.
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
package org.hfoss.posit.android.sync;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbHelper;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.functionplugin.bluetooth.BluetoothFindObject;
import org.hfoss.posit.android.functionplugin.bluetooth.BluetoothSyncActivity;
import org.hfoss.posit.android.functionplugin.camera.Camera;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * 
 * This class manages and sets up Bluetooth connections with other devices. 
 * It has a thread that listens for incoming connections, a thread for 
 * connecting with a device and a thread for performing data transmissions 
 * when connected. This class extends SyncMedium and overrides the 
 * transmission functionality
 * 
 * @author Elias Adum
 *
 */
public class SyncBluetooth extends SyncMedium {
	
	// Debugging
	public static final String TAG = "SyncBluetooth";
	
	// Name for the SDP record when creating server socket
	private static final String NAME = "BluetoothSyncService";
	
	// Unique UUID for this app
	static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
	
	// Member fields
	final BluetoothAdapter mAdapter;
	final Handler mHandler;
	final BluetoothSyncActivity mActivity;
	private AcceptThread mAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	int mState;
	
	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming	connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
	public static final int STATE_CONNECTED = 3; // now connected to a remote device
	
	/**
	 * Constructor. Prepares a new BluetoothSyncService session.
	 * 
	 * @param context The UI Activity Context
	 * @param handler Handler to send messages to the UI Activity
	 */
	public SyncBluetooth(BluetoothSyncActivity activity, Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
		mActivity = activity;
	}

	/**
	 * Returns a list of all the finds in the current project.
	 */
	@Override
	public List<String> getFindsNeedingSync() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
		DbManager helper = DbHelper.getDbManager(mActivity);
		
		int projectId = prefs.getInt(mActivity.getString(R.string.projectPref), 0);
		List<Find> finds = helper.getFindsByProjectId(projectId);
		
		DbHelper.releaseDbManager();
		
		ArrayList<String> findsNeedingSync = new ArrayList<String>();
		
		for (Find find : finds) {
			findsNeedingSync.add(find.getGuid());
		}
		
		return findsNeedingSync;
	}

	@Override
	public String retrieveRawFind(String guid) { return null; }

	/**
	 * Convert a Find to byte array and send it over Bluetooth
	 * @param find The find to send
	 * @return true on success
	 */
	@Override
	public boolean sendFind(Find find) {

		if (find == null) {
			return false;
		}
		
		String findStr = convertFindToRaw(find);
		
		// TODO: Send image over Bluetooth. For some reason when attempting
		// to send large data Posit crashes and data gets corrupted
		
		//String imageStr = Camera.getPhotoAsString(find.getGuid(), mActivity);
		
		String imageStr = null;
		BluetoothFindObject obj = new BluetoothFindObject(findStr, imageStr);		
		
		// Pack into a message and tell the BluetoothSyncService to write
		byte[] message = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(obj);
			oos.flush();
			oos.close();
			bos.close();
			message = bos.toByteArray();
		} catch (IOException e) {
			Log.e(TAG, "IOException during sync.");
			return false;
		} catch (Exception e) {
			Log.e(TAG, "Exception during sync.");
			return false;
		}

		this.write(message);

		return true;
	}
	
	/**
	 * Sends a list of finds over Bluetooth by calling sendFind for each guid
	 * 
	 * @param guids Array of guids of the finds to send
	 */
	public void sendFinds(String[] guids) {
		
		DbManager dbHelper = DbHelper.getDbManager(mActivity);
		
		for (String guid : guids) {
			
			boolean result = sendFind(dbHelper.getFindByGuid(guid));
			mHandler.obtainMessage(BluetoothSyncActivity.MESSAGE_WRITE, -1, -1,
					result).sendToTarget();
			
			// TODO: Fix this hack. If I don't wait between sending 2 finds,
			// data gets corrupted.
			try {
				Thread.sleep(400);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		DbHelper.releaseDbManager();
	}

	@Override
	public boolean postSendTasks() { return false; }
	
	/**
	 * Process, convert and store in the DB the received find read 
	 * from the Input Stream.
	 * 
	 * @param buffer the array of bytes received
	 * @return true on successs
	 */
	public boolean receiveFind(byte[] buffer) {
		BluetoothFindObject obj = null;
		Find newFind = null;
		String imageString = null;
		
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
			ObjectInputStream ois = new ObjectInputStream(bis);
			obj = (BluetoothFindObject) ois.readObject();
			newFind = convertRawToFind(obj.getFindString());
			imageString = obj.getImageString();
			
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "IO exception on receiving message.");
			return false;
		} catch (ClassNotFoundException e) {
			Log.e(TAG, "Class not found on receiving message.");
			return false;
		} catch (Exception e) {
			Log.e(TAG, "Exception on receiving message.");
			e.printStackTrace();
			return false;
		}
		
		DbManager dbHelper = DbHelper.getDbManager(mActivity);
		
		// If the find exists then update otherwise insert
		if (dbHelper.getFindByGuid(newFind.getGuid()) == null) {
			dbHelper.insert(newFind);
		} else {
			dbHelper.update(newFind);
		}
		
		// Store the image only if its not empty and the hash code is valid
		if (imageString != null && obj.verifyImageHashCode()) {
			Camera.savePhoto(newFind.getGuid(), imageString, mActivity);
		}
		
		DbHelper.releaseDbManager();
		
		return true;
	}
	
	/**
	 * Set the current state of the connection
	 * 
	 * @param state An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		mState = state;
		
		// Pass the new state to the handler so the UI activity can display changes
		mHandler.obtainMessage(BluetoothSyncActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}
	
	/**
	 * Return the current connection state.
	 */
	public synchronized int getState() {
		return mState;
	}
	
	/**
	 * Start the thread that attempts to connect to another device
	 * 
	 * @param device Device attempting to connect
	 */
	public void startConnectThread(BluetoothDevice device) {
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}
	
	/**
	 * Cancel the connect thread.
	 */
	public synchronized void cancelConnectThread() {
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
	}
	
	/**
	 * Start the thread that listens for data once both devices are connected.
	 * 
	 * @param socket BluetoothSocket where data is sent and received.
	 */
	public void startConnectedThread(BluetoothSocket socket) {
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
		
		setState(STATE_CONNECTED);
	}
	
	/**
	 * Cancel the connected thread.
	 */
	public synchronized void cancelConnectedThread() {
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
	}
	
	/**
	 * Start the thread that listens for incoming connections.
	 */
	public void startAcceptThread() {
		if (mAcceptThread == null) {
			mAcceptThread = new AcceptThread();
			mAcceptThread.start();
		}
		
		setState(STATE_LISTEN);
	}
	
	/**
	 * Cancel the Accept thread.
	 */
	public synchronized void cancelAcceptThread() {
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}
	}
	
	/**
	 * Start the sync service. Specifically the AcceptThread to begin a session
	 * in listening (server) mode. Called by the Activity onResume().
	 */
	public synchronized void start() {
		// Cancel any thread attempting to make a connection
		cancelConnectThread();
		
		// Cancel any thread currently connected.
		cancelConnectedThread();
		
		// Start the thread to listen on a BluetoothServerSocket
		startAcceptThread();
	}
	
	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * 
	 * @param device The BluetoothDevice to connect
	 */
	public synchronized void connect(BluetoothDevice device) {

		// Cancel any thread attempting to connect
		if (mState == STATE_CONNECTING ) {
			cancelConnectThread();
		}
		
		// Cancel any thread currently running a connection
		cancelConnectedThread();
		
		// Start the thread to connect with the given device
		startConnectThread(device);
	}
	
	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * 
	 * @param socket The BluetoothSocket on which the connection was made
	 * @param device The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
		// Cancel the thread that completed the connection
		cancelConnectThread();
		
		// Cancel any thread currently running a connection
		cancelConnectedThread();
		
		// Cancel the accept thread because we only want to connect to one device
		//cancelAcceptThread();
		
		// Send the name of the connected device back to the UI Activity
		Message msg = mHandler.obtainMessage(BluetoothSyncActivity.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothSyncActivity.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);
		
		// Start the thread to manage the connection and perform transmissions
		startConnectedThread(socket);
	}
	
	/**
	 * Stop all threads
	 */
	public synchronized void stop() {
		cancelConnectThread();
		cancelConnectedThread();
		cancelAcceptThread();
		
		setState(STATE_NONE);
	}
	
	/**
	 * Write to the ConnectedThread.
	 * 
	 * @param out The bytes to write
	 * @see ConnectedThread#write(byte[])
	 */
	public void write(byte[] out) {
		// Create a temp object
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)	return;
			r = mConnectedThread;
		}
		
		// Perform the write
		r.write(out);
	}
	
	/**
	 * Indicate that the connection attempt failed and notify the UI Activity
	 */
	void connectionFailed() {
		setState(STATE_LISTEN);
		
		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(BluetoothSyncActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothSyncActivity.TOAST, "Unable to connect device");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}
	
	/**
	 * Indicate that the connection was lost and notify the UI Activity
	 */
	void connectionLost() {
		setState(STATE_LISTEN);

		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(BluetoothSyncActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothSyncActivity.TOAST, "Device connection was lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}
	
	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted (or
	 * until cancelled).
	 */
	private class AcceptThread extends Thread {
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;

		/**
		 * Initialise and open the Bluetooth socket
		 */
		public AcceptThread() {
			BluetoothServerSocket tmp = null;

			// Create a new listening server socket
			try {
				tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "listen() failed");
			}
			mmServerSocket = tmp;
		}

		/**
		 * Start the thread
		 */
		public void run() {
			Log.d(TAG, "BEGIN mAcceptThread" + this);
			setName("AcceptThread");
			BluetoothSocket socket = null;

			// Listen to the server socket if we're not connected
			while (mState != STATE_CONNECTED) {
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "accept() failed");
					break;
				}

				// If a connection was accepted
				if (socket != null) {
					synchronized (SyncBluetooth.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// Situation normal. Start the connected thread.
							connected(socket, socket.getRemoteDevice());
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// Either not ready or already connected. Terminate
							// new socket.
							try {
								socket.close();
							} catch (IOException e) {
								Log.e(TAG, "Could not close unwanted socket");
							}
							break;
						}
					}
				}
			}
			
			Log.i(TAG, "END mAcceptThread");
		}

		/**
		 * Stop the thread and close the socket
		 */
		public void cancel() {
			Log.d(TAG, "cancel " + this);
			
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of server failed");
			}
		}
	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		/**
		 * Initialise and open the Bluetooth socket
		 * 
		 * @param device device connecting to
		 */
		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "create() failed");
			}
			mmSocket = tmp;
		}

		/**
		 * Start the thread and attempt the connection
		 */
		public void run() {
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			// Always cancel discovery because it will slow down a connection
			mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
			} catch (IOException e) {
				connectionFailed();
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close() socket during connection failure");
				}
				// Start the service over to restart listening mode
				SyncBluetooth.this.start();
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (SyncBluetooth.this) {
				mConnectThread = null;
			}

			// Start the connected thread
			connected(mmSocket, mmDevice);
		}

		/**
		 * Cancel the thread and close the socket
		 */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed");
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		/**
		 * Initialise the Input and Output Streams.
		 * 
		 * @param socket BluetoothSocket where we listen for and send data
		 */
		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created");
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		/**
		 * Start running the thread and listen for incoming data
		 */
		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			//byte[] buffer = new byte[1024];
			
			// Increase from 1kb to 1mb to allow for pictures
			byte[] buffer = new byte[1048576];  
			int bytes;

			// Keep listening to the InputStream while connected
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer);
					
					// Process the received find
					boolean result = receiveFind(buffer);

					// Send the obtained bytes to the UI Activity
					mHandler.obtainMessage(BluetoothSyncActivity.MESSAGE_READ,
							bytes, -1, result).sendToTarget();
				} catch (IOException e) {
					Log.e(TAG, "disconnected");
					connectionLost();
					break;
				}
			}
		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);
				mmOutStream.flush();

			} catch (IOException e) {
				Log.e(TAG, "Exception during write");
			}
		}

		/**
		 * Cancel the thread and close the socket
		 */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed");
			}
		}
	}
}
