/*
 * File: ListFindsActivity.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
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
package org.hfoss.posit.android.api.activity;

import java.util.List;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.activity.FindOverlay;

import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.GeoPoint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Extends the MapView class for maps point clustering, specifically for zoom 
 * and pan.
 *
 */
public class MapViewExtended extends MapView {
	
	private static final String TAG = "mMapView";
	
	private Context c = this.getContext();
	private Drawable emptyDrawable;	
	private FindOverlay mPoints;
	private List<Overlay> mapOverlays = getOverlays();
	private MapFindsActivity mf = new MapFindsActivity();	
	
	private int oldZoomLevel = -1;	

	public MapViewExtended(android.content.Context context, android.util.AttributeSet attrs) {
		super(context, attrs);
		emptyDrawable = c.getResources().getDrawable(R.drawable.bubble);
		mPoints = new FindOverlay(emptyDrawable, getContext(), true, null);
	}

	public MapViewExtended(android.content.Context context,
			               android.util.AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		emptyDrawable = c.getResources().getDrawable(R.drawable.bubble);
		mPoints = new FindOverlay(emptyDrawable, getContext(), true, null);
	}

	public MapViewExtended(android.content.Context context, java.lang.String apiKey) {
		super(context, apiKey);
		emptyDrawable = c.getResources().getDrawable(R.drawable.bubble);
		mPoints = new FindOverlay(emptyDrawable, getContext(), true, null);
	}	
	
	/**
	 * Update clusters upon pan
	 */
	public boolean onTouchEvent(MotionEvent ev) {
		
		if (ev.getAction()==MotionEvent.ACTION_UP) {
			Log.i(TAG, "PANNED");
			placeOverlays(mf.getFinds(), mPoints);
			mapOverlays.add(mPoints);
		}
		return super.onTouchEvent(ev);
	}

	/**
	 * Update clusters upon zoom
	 * @param canvas
	 */
	public void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);  
		if (getZoomLevel() != oldZoomLevel) {
			Log.i(TAG, "ZOOOMED");
			Log.i(TAG, "mMapView context: " + c);
			Log.i(TAG, "mMapView mf: " + mf);
			placeOverlays(mf.getFinds(), mPoints);
			mapOverlays.add(mPoints);
			oldZoomLevel = getZoomLevel();
		}        
	}

	/**
	 * Add finds as clustered overlays to mPoints, and place these overlays on the map. 
	 * @param finds
	 * @param mPoints
	 * @return
	 */
	public FindOverlay placeOverlays(List<? extends Find> finds, FindOverlay mPoints) {		
		int latitude = 0;
		int longitude = 0;
		OverlayItemExtended item;
		
		mPoints.removeAllOverlays();        
	    getOverlays().clear();
	    mapOverlays.clear(); 
	    
	    for(Find find  : finds) {
			latitude = (int) (find.getLatitude()*1E6);
			longitude = (int) (find.getLongitude()*1E6);

			//String description = find.getGuid() + "\n" + find.getName() + "\n" + find.getDescription(); 
			
			//Specific for clustering
			item = new OverlayItemExtended(new GeoPoint(latitude,longitude), Integer.toString(find.getId()), null);
			mPoints.addOverlayItemClustered(item, this);        		
		}	    
	    return mPoints;		
	}
	
}
