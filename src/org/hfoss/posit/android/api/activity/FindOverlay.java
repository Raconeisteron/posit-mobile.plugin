/*
 * File: FindOverlay.java
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

import java.util.ArrayList;
import java.util.Stack;

import org.hfoss.posit.android.api.plugin.FindPluginManager;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

/**
 * Object for creating and adding overlays on the map, making them
 * tappable with clickable icons.
 */
public class FindOverlay extends ItemizedOverlay {
	private static final String TAG = "ItemizedOverlay";

	//mOverlays will contains all the OverlayItemExtended we want on the map
	private ArrayList<OverlayItemExtended> mOverlays 
			= new ArrayList<OverlayItemExtended>();
	
	private Context mContext;
	private boolean isTappable;
	private String action;	

	/**
	 * @param defaultMarker
	 */
	public FindOverlay(Drawable defaultMarker, Context c, boolean isTappable, 
						String action) {
		//super(defaultMarker); //so that marker is invisible for clustering
		super(boundCenterBottom(defaultMarker));
		mContext = c;
		this.isTappable = isTappable;
		this.action = action;
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.ItemizedOverlay#createItem(int)
	 */
	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.ItemizedOverlay#size()
	 */
	@Override
	public int size() {
		return mOverlays.size();
	}

	public void addOverlay(OverlayItemExtended overlay) {
		mOverlays.add(overlay);
		populate();
	}
	
	/**
	 * Clear all overlays
	 */
	public void removeAllOverlays() {
    	mOverlays.clear();
    	populate();
    }
    
    /**
     * Groups the GeoPoints if the projection of them in the mapView is close
     * enough. This is where the clustering takes place.
     * 
     * @param thisOverlay
     * @param mapView
     */
    public void addOverlayItemClustered(OverlayItemExtended thisOverlay,
    		MapView mapView) {
    	  	
    	for (OverlayItemExtended otherOverlay : mOverlays) {
    		
    		//don't cluster if thisOverlay doesn't fit within the below threshold
    		if (mapView.getZoomLevel() >= 14 && 
    			PointCluster.getOverLayItemDistance(thisOverlay, 
    												otherOverlay, mapView) > 60) {
    			mOverlays.add(thisOverlay);
    			populate();
    			return;
    		}
    		
    		//if thisOverlay is currently not clustered and within the threshold
    		//for otherOverlay note this threshold is large to allow the points 
    		//to be on opposite edges of the cluster
    		if (PointCluster.getOverLayItemDistance(thisOverlay, otherOverlay,
    				mapView) < 240 && !thisOverlay.getIsClustered()) {
    			
    			if (otherOverlay.getIsMaster()) {
    				thisOverlay.setIsMaster(false);
    				thisOverlay.setIsClustered(true);
    				otherOverlay.setIsClustered(true);
    				otherOverlay.getSlaves().push(thisOverlay);
    				thisOverlay.setParent(otherOverlay);
    			
    			// otherwise, otherOverlay is not a master
    			// check if thisOverlay is within the threshold for 
    			// otherOverlay's master
    			} else if (PointCluster.getOverLayItemDistance(thisOverlay,
    					otherOverlay.getParent(), mapView) < 240
    					&& otherOverlay.getIsClustered()) {
    				thisOverlay.setIsMaster(false);
    				thisOverlay.setIsClustered(true);
    				thisOverlay.setParent(otherOverlay.getParent());
    				otherOverlay.getParent().getSlaves().push(thisOverlay);
    			}
    		}
    	}    
    	mOverlays.add(thisOverlay);
    	populate();
    }

    /**
     * Draw the clustered points in the overlay
     */
    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
    	super.draw(canvas, mapView, shadow);
    	
    	// cycle through all overlays
    	for (OverlayItemExtended item : mOverlays) {
    		
    		GeoPoint point = item.getPoint();
    		OverlayItemExtended slave = null;
    		Point ptScreenCoord = new Point(); 
    		mapView.getProjection().toPixels(point, ptScreenCoord);
    		
    		float minX = ptScreenCoord.x;
            float maxX = ptScreenCoord.x;
            float minY = ptScreenCoord.y;
            float maxY = ptScreenCoord.y;
    		float centerX, centerY;    	
    		
    		if (item.getIsMaster()) {
    			
    			//if there are slaves, find the length between the farthest slaves  
    			if (item.getSlaves().size() > 0) {   				
            		
    				for (int i = 0; i < item.getSlaves().size(); i++) {
    					slave = item.getSlaves().get(i);
    					GeoPoint slavePoint = slave.getPoint();
    					Point slaveScreenCoord = new Point();
    					mapView.getProjection().toPixels(slavePoint, slaveScreenCoord);
    					minX = Math.min(minX, slaveScreenCoord.x);
    					maxX = Math.max(maxX, slaveScreenCoord.x);
    					minY = Math.min(minY, slaveScreenCoord.y);
    					maxY = Math.max(maxY, slaveScreenCoord.y);
    				}
    				
    				//draw a transparent red circle at the midpoint
    				centerX = (maxX + minX) / 2;
    				centerY = (maxY + minY) / 2;
    				double distance = findDistance(minX, minY, maxX, maxY);
    				Paint linePaint = new Paint();
    	            linePaint.setColor(android.graphics.Color.RED);
    	            linePaint.setStyle(Paint.Style.FILL);
    	            linePaint.setAlpha(35); 
    	            canvas.drawCircle(centerX, centerY, (float) (distance / 2) + 10,
    	                    linePaint);     

    				//draw the number of Finds in this cluster
    	            Paint paint = new Paint();
                    paint.setTextAlign(Paint.Align.CENTER);
                    paint.setTextSize(45);
                    paint.setAntiAlias(true);
                    paint.setARGB(255, 0, 0, 0);

                    Paint boxPaint = new Paint();
                    boxPaint.setColor(android.graphics.Color.WHITE);
                    boxPaint.setStyle(Paint.Style.FILL);
                    boxPaint.setAlpha(140);
                    canvas.drawCircle(centerX, centerY - (paint.getTextSize() / 2),
                            paint.getTextSize(), boxPaint);
                    canvas.drawText(item.getSlaves().size() + 1 + "", centerX, centerY,
                            paint);   				
    			}
    			
    		} //end if master
    	}
    }

    /**
     * Return the distance between two float points (x1,y1) and (x2,y2) 
     * using Pythagorean Theorem.
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    private double findDistance(float x1, float y1, float x2, float y2) {
    	return Math.sqrt(((x1 - x2) * (x1 - x2)) + ((y1 - y2) * (y1 - y2)));
    }   
    
	/**
	 * Called when the user clicks on one of the Find icons
	 *   in the map. It shows a description of the Find
	 * @param pIndex is the Find's index in the ArrayList
	 */
	@Override
	protected boolean onTap(int pIndex) {
		
		if (!isTappable)
			return false;
		
		//Begin Clustering Code
		int i=0, slaveID;
		OverlayItemExtended item;
		Stack<OverlayItemExtended> slaves = null;
		
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);	
		
		//Pass the cluster's master id into ListFindsActivity
		intent.putExtra("MasterID", Integer.parseInt(mOverlays.get(pIndex).getTitle()));
		
		//check if the item tapped is a master of its cluster and find its slaves accordingly
		if (mOverlays.get(pIndex).getParent() != null)  { 
			slaves = mOverlays.get(pIndex).getParent().getSlaves();
		}
		else { //is a master
			slaves = mOverlays.get(pIndex).getSlaves();
		}			
		
		// Pass the slavesIDs to ListFindsActivity
		while (!slaves.isEmpty()) {
			item = slaves.pop();
			slaveID = Integer.parseInt(item.getTitle());
			Log.i(TAG, "SlaveID= " + slaveID);
			intent.putExtra("SlaveID" + i, slaveID); 
			i++;
		}			
		int slavessize = i;
		intent.putExtra("SlavesSize", slavessize); //Pass the number of slaves into ListFindsActivity		
		intent.setClass(mContext, FindPluginManager.mFindPlugin.getmListFindsActivityClass());
		mContext.startActivity(intent);
		//End Clustering Code
		
//		Intent intent = new Intent();
//		int id = Integer.parseInt(mOverlays.get(pIndex).getTitle());
//		Log.i(TAG, "id= " + id);
//		
//		if (action != null && action.equals(CsvListFindsActivity.ACTION_CSV_FINDS)) {
//			intent.setAction(action);
//			intent.putExtra(action, id); // Pass the RowID to FindActivity
//			intent.setClass(mContext, CsvFindActivity.class);
//		}  else  {
//			intent.setAction(Intent.ACTION_EDIT);
//			intent.putExtra(Find.ORM_ID, id); // Pass the RowID to FindActivity
//			intent.setClass(mContext, FindPluginManager.mFindPlugin.getmFindActivityClass());
//		}
//		mContext.startActivity(intent);
		
		return true;
	}
	
	
}