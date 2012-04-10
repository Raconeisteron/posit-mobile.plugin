/*
 * File: SelectFindView.java
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

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.functionplugin.camera.Camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * 
 * This class extends LinearLayout to create the custom View containing the 
 * Find data.
 * 
 * @author Elias Adum
 *
 */
public class SelectFindView extends LinearLayout {

	// Field information
	private TextView mName;
	private ImageView mImage;
	private CheckBox mCheckBox;
	private SelectFind mSelectFind;

	public SelectFindView(Context context, SelectFind selectFind) {
		super(context);
		
		// Layout
		this.setOrientation(HORIZONTAL);
		mSelectFind = selectFind;
		
		String guid = selectFind.getGuid();
		
		// Add image		
		mImage = new ImageView(context);
		mImage.setAdjustViewBounds(true);
		mImage.setMaxHeight(50);
		mImage.setMaxWidth(50);
		mImage.setScaleType(ImageView.ScaleType.FIT_XY);
		mImage.setImageResource(R.drawable.person_icon);
		Bitmap bmp = Camera.getPhotoAsBitmap(guid, context);
		
		if (bmp != null) {
			mImage.setImageBitmap(bmp);
		}
		
		mImage.setPadding(0, 0, 0, 0);
		addView(mImage, new LinearLayout.LayoutParams(50, 50));
		
		// Add Checkbox
		mCheckBox = new CheckBox(context);
		mCheckBox.setPadding(0, 0, 0, 0);
		
		// Initial state of checkbox
		mCheckBox.setChecked(selectFind.getState());
		mCheckBox.setEnabled(false);
		
		// Add checkbox to self
		RelativeLayout.LayoutParams cbParams = new RelativeLayout.LayoutParams(40, 40);
		cbParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		addView(mCheckBox, cbParams);
		
		// Add Name
		mName = new TextView(context);
		mName.setText(selectFind.getName());
		mName.setSingleLine(true);
		mName.setPadding(3, 0, 70, 0);
		mName.setTextSize(20);
		RelativeLayout.LayoutParams nameParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		nameParams.addRule(RelativeLayout.RIGHT_OF, mCheckBox.getId());
		addView(mName, nameParams);

		mName.setFocusable(false);
		mImage.setFocusable(false);
		mCheckBox.setFocusable(false);
	}

	/**
	 * Sets the name
	 * @param words new name
	 */
	public void setText(String words) {
		mName.setText(words);
	}

	/**
	 * Sets the checkbox state. Checked or unchecked
	 * 
	 * @param state new state
	 */
	public void setSelectFindState(boolean state) {
		mSelectFind.setState(state);
		mCheckBox.setChecked(mSelectFind.getState());
	}
	
	/**
	 * Toggles the state between checked and unchecked
	 */
	public void toggleSelectFindState() {
		setSelectFindState(!mSelectFind.getState());
	}

}
