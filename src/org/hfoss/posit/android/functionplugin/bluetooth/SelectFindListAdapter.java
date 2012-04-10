/*
 * File: SelectFindListAdapter.java
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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * This class extends BaseAdapter to create the list of finds available to sync
 * via Bluetooth
 * 
 * @author Elias Adum
 *
 */
public class SelectFindListAdapter extends BaseAdapter {

	private Context mContext;

	private List<SelectFind> mItems = new ArrayList<SelectFind>();

	/**
	 * Constructor.
	 * 
	 * @param context Application context
	 */
	public SelectFindListAdapter(Context context) {
		mContext = context;
	}

	/**
	 * Adds a find to the list
	 * 
	 * @param item Find object to add
	 */
	public void addItem(SelectFind item) {
		mItems.add(item);
	}

	/**
	 * Adds a list of finds to the list
	 * 
	 * @param loItems List of find objects
	 */
	public void setListItems(List<SelectFind> loItems) {
		mItems = loItems;
	}

	/**
	 * Return total number of Finds in the list
	 */
	public int getCount() {
		return mItems.size();
	}

	/**
	 * Returns a specific find.
	 * 
	 * @param position index of the find to return
	 */
	public Object getItem(int position) {
		return mItems.get(position);
	}

	/**
	 * Change the state for the given item
	 * 
	 * @param state	new state
	 * @param position array index
	 */
	public void setState(boolean state, int position) {
		mItems.get(position).setState(state);
		// Redraw
		this.notifyDataSetChanged();
	}

	/**
	 * Toggle the state for the given item
	 * 
	 * @param position array index
	 */
	public void toggleState(int position) {
		mItems.get(position).toggleState();
		// Redraw
		this.notifyDataSetChanged();
	}

	/**
	 * selects the checkbox for every item
	 */
	public void selectAll() {
		for (SelectFind item : mItems) {
			item.setState(true);
		}
		// Redraw
		this.notifyDataSetChanged();
	}

	/**
	 * De-selects the checkbox for every item
	 */
	public void deselectAll() {
		for (SelectFind item : mItems) {
			item.setState(false);
		}
		// Redraw
		this.notifyDataSetChanged();
	}

	
	/**
	 * Returns the item ID.
	 * This method is a required method for the BaseAdapter class. 
	 */
	public long getItemId(int position) {
		// Item position is its id
		return position;
	}
	
	/**
	 * Get the guids of all the finds selected to be synced
	 * 
	 * @return String array of the guids
	 */
	public String[] getSelectedGuids() {	
		ArrayList<String> guids = new ArrayList<String>();
		
		for (SelectFind find : mItems) {
			if (find.getState()) guids.add(find.getGuid());
		}

		return guids.toArray(new String[0]);
	}

	/**
	 * Returns the view of the item at the given position
	 * 
	 * @param position array index
	 */
	public View getView(int position, View convertView, ViewGroup parent) {
		return new SelectFindView(mContext, mItems.get(position));
	}
}
