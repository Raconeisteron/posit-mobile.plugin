/*
 * File: BluetoothDeviceListActivity.java
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
package org.hfoss.posit.android.functionplugin.bluetooth;

import java.util.Set;

import org.hfoss.posit.android.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This Activity lists any paired devices and devices detected after discovery
 * When a device is chosen by the user, the MAC address of the device is sent
 * back to the parent Activity in the result Intent.
 * 
 * @author Elias Adum
 *
 */
public class BluetoothDeviceListActivity extends Activity {

	// Debugging
    private static final String TAG = "BluetoothDeviceListActivity";
    
    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    
    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);
        
        // Just in case user backs out
        setResult(Activity.RESULT_CANCELED);
        
		// Init arrays for paired devices and for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        
        // Set up the ListView for paired devices
        ListView pairedLV = (ListView) findViewById(R.id.paired_devices);
        pairedLV.setAdapter(mPairedDevicesArrayAdapter);
        pairedLV.setOnItemClickListener(mDeviceClickListener);
        
        // Set up the ListView for new devices 
        ListView newDevicesLV = (ListView) findViewById(R.id.new_devices);
        newDevicesLV.setAdapter(mNewDevicesArrayAdapter);
        newDevicesLV.setOnItemClickListener(mDeviceClickListener);
        
        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        
        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        
        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        
        // If we have paired devices add them to the corresponding ArrayAdapter
        if (pairedDevices.size() > 0) {
        	findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
        	for (BluetoothDevice device : pairedDevices) {
        		mPairedDevicesArrayAdapter.add(device.getName() + '\n' + device.getAddress());
        	}
        } else {
        	String noDevices = getString(R.string.bt_none_paired);
        	mPairedDevicesArrayAdapter.add(noDevices);
        }
	}
	
	@Override
    protected void onDestroy() {
        super.onDestroy();
        cancelDiscovery();
        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.bluetooth_connect_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.bt_scan:
			// Perform device discovery
			doDiscovery();
			return true;
		case R.id.bt_scan_cancel:
			// Cancel scan
			cancelDiscovery();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Cancel device discovery with the BluetoothAdapter
	 */
	private void cancelDiscovery() {
		
		Log.d(TAG, "Cancelling bluetooth discovery");
		
		if (mBtAdapter != null) {
			mBtAdapter.cancelDiscovery();
		}
		
		setTitle(R.string.bt_select_device);
	}
	
	/**
	 * Start device discovery with the BluetoothAdapter
	 */
	private void doDiscovery() {
		
		Log.d(TAG, "Starting bluetooth discovery");
		
		// Show in the title bar that we are scanning
		setProgressBarIndeterminateVisibility(true);
		setTitle(R.string.bt_scanning);
		
		// Turn on sub-title for new devices
		findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
		
		// Clear the list of new devices
		mNewDevicesArrayAdapter.clear();
		
		// If we are already discovering, restart
		if (mBtAdapter.isDiscovering())	cancelDiscovery();
		
		// Request discovery from BluetoothAdapter
		mBtAdapter.startDiscovery();
	}
	
	/**
	 * On-Click listener for all devices in the ListViews
	 */
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> adapter, View view, int arg2, long arg3) {
			// At this point we already know who to connect to so stop discovery
			cancelDiscovery();
			
			// Get device MAC address, which is the last 17 chars in the View
			String info = ((TextView) view).getText().toString();
			String addr = info.substring(info.length() - 17);
			
			// Create the result Intent and include the MAC address
			Intent intent = new Intent();
			intent.putExtra(EXTRA_DEVICE_ADDRESS, addr);
			
			// Set result and finish this Activity
			setResult(Activity.RESULT_OK, intent);
			finish();
		}
	};
	
	/**
	 * Listener for discovered devices.
	 * Changes title when discovery finishes
	 */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				// If it is already paired, skip it because it is listed already
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					mNewDevicesArrayAdapter.add(device.getName() + '\n' + device.getAddress());
				}
			}
			
			// When discovery is finished, change the Activity title
			if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				setProgressBarIndeterminateVisibility(false);
				setTitle(R.string.bt_select_device);
				
				// If we didn't find anything
				if (mNewDevicesArrayAdapter.getCount() == 0) {
					String noDevices = getString(R.string.bt_none_found);
					mNewDevicesArrayAdapter.add(noDevices);
				}
			}
		}
	};
}
