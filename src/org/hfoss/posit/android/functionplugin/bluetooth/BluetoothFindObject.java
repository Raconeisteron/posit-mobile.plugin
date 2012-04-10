/*
 * File: BluetoothFindObject.java
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

import java.io.Serializable;

import org.hfoss.posit.android.functionplugin.camera.Camera;
import org.hfoss.posit.android.sync.SyncMedium;

/**
 * Serializable class containing both the Find and the image to be sent via
 * Bluetooth.
 * 
 * @author Elias Adum
 *
 */
public class BluetoothFindObject implements Serializable {

	private static final long serialVersionUID = 4866606116567160197L;
	
	private String findString = null;
	private String imageString = null;
	private int imageHashCode;
	
	public String getFindString() {
		return findString;
	}
	public void setFindString(String findString) {
		this.findString = findString;
	}
	public String getImageString() {
		return imageString;
	}
	public void setImageString(String imageString) {
		this.imageString = imageString;
		
		if (imageString != null) {
			imageHashCode = this.imageString.hashCode();
		} else {
			imageHashCode = 0;
		}
	}
	public int getImageHashCode() {
		return imageHashCode;
	}
	
	/**
	 * Verifies integrity of image data. If base 64 of the image is invalid
	 * it will crash the app 
	 * 
	 * @return True if integrity check pass
	 */
	public boolean verifyImageHashCode() {
		return (imageHashCode == imageString.hashCode());
	}
	
	/**
	 * Constructor
	 * 
	 * @param findString string representation of the find
	 * @param imageString base64 representation of the image
	 * 
	 * @see Camera#getPhotoAsString(String, android.content.Context)
	 * @see SyncMedium#convertFindToRaw(org.hfoss.posit.android.api.Find)
	 */
	public BluetoothFindObject(String findString, String imageString) {
		setFindString(findString);
		setImageString(imageString);
	}

}
