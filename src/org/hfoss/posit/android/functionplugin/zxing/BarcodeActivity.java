/*
 * File: BarcodeActivity.java
 * 
 * Copyright (C) 2012 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool.
 *
 * This is free software; you can redistribute it and/or modify
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

package org.hfoss.posit.android.functionplugin.zxing;

import java.text.SimpleDateFormat;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.plugin.AddFindPluginCallback;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class BarcodeActivity extends Activity implements AddFindPluginCallback {
	
	public static final String TAG ="BarcodeActivity";
	
	// These must be the same as the titles in plugin_preferences
	public static final String SCAN_MENU = "Scan QRcode";
	public static final String DISPLAY_MENU = "Display QRcode";
	
	private IntentIntegrator zxing;
	
	// These static variables are set by menuItemSelectedCallback.  These
	//  have to be static in order to survive re-instantiation of this Activity.
	private static Context mContext;
	private static Intent mIntent;
	private static Find mFind;
	private static View mView;
	
    /** Called when the activity is first created in FindActivity's onMenuItemSelected.
     * 
     *  NOTE that this happens after menuItemSelectedCallback is invoked. 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        // Start the barcode scanner. The result can be retrieved in onActivityResult()
        if (mIntent.getAction().equals(SCAN_MENU)) {
        	zxing = new IntentIntegrator(this);
        	zxing.setTargetApplications(IntentIntegrator.TARGET_BARCODE_SCANNER_ONLY);
        	zxing.initiateScan();
        } else if (mIntent.getAction().equals(DISPLAY_MENU)) {
        	this.displayFindAsBarcode(mFind);
        } else {
        	Log.e(TAG, "Error:  Unknown plugin menu item");
        }
    }
    
    /**
     * Displays the Find as a barcode using ZXing's IntentIntegrator.
     */
    public void displayFindAsBarcode(Find find) {
		String guid = find.getGuid();
		
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy/MM/dd HH:mm:ss");
		String time = dateFormat.format(find.getTime());
		
		String findName = find.getName();
		String description = find.getDescription();
		double latitude = find.getLatitude();
		double longitude = find.getLongitude();
		
		String findValuesToEncode = "POSITcode*" + guid + "*" + time + "*" + findName
				 + "*" + description + "*" + latitude + "*" + longitude;
		
		IntentIntegrator zxing = new IntentIntegrator(this);
		zxing.shareText(findValuesToEncode);
		finish();
    }
    
    /**
     * Invoked when the Barcode scanner finishes the scan.  It checks whether the
     * result is a POSIT scan and if so it 
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	Log.i(TAG, "onActivityResult()");
    	if (resultCode == Activity.RESULT_CANCELED) {
    		Log.i(TAG, "Scan barcode cancelled");
    	} else {
    		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
    		if (scanResult != null) {
    			String contents = scanResult.getContents();
    			Log.i(TAG, "Scan result = " + contents);
    			// Paste the barcode data into FindView
    			if (contents != null && contents.indexOf("POSITcode*") != -1) {
    				String[] values = contents.split("\\*");
    				TextView tv = (TextView) mView.findViewById(R.id.nameEditText);
    				tv.setText(values[3]);
    				tv = (TextView) mView.findViewById(R.id.guidRealValueTextView);
    				tv.setText(values[1]);
    				tv = (TextView) mView.findViewById(R.id.timeValueTextView);
    				tv.setText(values[2]);
    				tv = (TextView) mView.findViewById(R.id.descriptionEditText);
    				tv.setText(values[4]);
    				tv = (TextView) mView.findViewById(R.id.latitudeValueTextView);
    				tv.setText(values[5]);
    				tv = (TextView) mView.findViewById(R.id.longitudeValueTextView);
    				tv.setText(values[6]);  
    			} else {
    				Toast toast = Toast.makeText(this, "Not a POSIT barcode, ignoring", Toast.LENGTH_LONG);
    				toast.show();
    			}
    		} else {
    			Toast toast = Toast.makeText(this, "Scanner error", Toast.LENGTH_LONG);
    			toast.show();
    		}
    	}
    	finish();
    }

    /**
     * This method is invoked by reflection in FindActivity.  It is used
     * by the plugin to set up the environment BEFORE onCreate() is
     * invoked in FindActivity.  
     * 
     * It merely copies its parameters into instance variables
     * 
     * @param context, the Activity that started this activity
     * @param find, the current Find being processed
     * @param view, the current user interface.
     */
	public void menuItemSelectedCallback(Context context, Find find, View view,
			Intent intent) {
		Log.i(TAG, "menuItemSelectedCallback()");
		mContext = context;
		mIntent = intent;
		mView = view;
		mFind = find;
	}

	public void onActivityResultCallback(Context context, Find find, View view,
			Intent intent) {
		
	}

	public void displayFindInViewCallback(Context context, Find find, View view) {
		
	}

	public void afterSaveCallback(Context context, Find find, View view,
			boolean isSaved) {
		
	}

	public void finishCallback(Context context, Find find, View view) {
		
	}

	public void onClickCallback(Context context, Find find, View view) {
		
	}

    
}