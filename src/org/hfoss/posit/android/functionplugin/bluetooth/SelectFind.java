/*
 * File: SelectFind.java
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

/**
 * Comparable object that stores relevant Find data to be displayed in the 
 * SelectFindView.
 * 
 * @author Elias Adum
 *
 */
public class SelectFind implements Comparable<SelectFind> {

	// Data stored
	private String mGuid = "";
	private boolean mState = false;
	
	private int mId = 0;
	private String mName = "";
	private String mDesc = "";

	public SelectFind(String guid, boolean state, int i, String name, String desc) {
		mGuid = guid;
		mState = state;
		mName = name;
		mDesc = desc;
		mId = i;
	}

	public void setState(boolean value) {
		this.mState = value;
	}

	public boolean getState() {
		return this.mState;
	}
	
	public void toggleState() {
		this.mState = !this.mState;
	}

	public String getGuid() {
		return mGuid;
	}

	public void setGuid(String guid) {
		mGuid = guid;
	}
	
	public String getName() {
		return mName;
	}
	
	public String getDesc() {
		return mDesc;
	}
	
	public int getId() {
		return mId;
	}

	/**
	 * Compare Guids of 2 objects
	 * 
	 * @param other SelectFind object to compare against 
	 */
	public int compareTo(SelectFind other) {
		if (this.mGuid != null)
			return this.mGuid.compareTo(other.getGuid());
		else
			throw new IllegalArgumentException();
	}
}