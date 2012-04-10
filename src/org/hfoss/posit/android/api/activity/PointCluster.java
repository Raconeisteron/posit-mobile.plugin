/*
 * File: PointCluster.java
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

import android.graphics.Point;

import com.google.android.maps.MapView;
import com.google.android.maps.GeoPoint;

/**
 * PointClusters are points on the Map that can be clustered. 
 * 
 */
public class PointCluster {  
	
	/**
	 * Finds the distance between two Points (Finds) on the Map
	 * 
	 * @param x1 x coordinate of the first Find
	 * @param y1 y coordinate of the first Find
	 * @param x2 x coordinate of the second Find
	 * @param x2 x coordinate of the second Find
	 * 
	 */
	private static double findDistance(float x1, float y1, float x2, float y2) {
        return Math.sqrt(((x1 - x2) * (x1 - x2)) + ((y1 - y2) * (y1 - y2)));
    }	
	
	/**
	 * Finds the distance between the projection of GeoPoints in the mapView.
	 * 
	 * @param item1
	 * @param item2
	 * @param mapView
	 * @return
	 */
    public static double getOverLayItemDistance(OverlayItemExtended item1, OverlayItemExtended item2, MapView mapView) {
        GeoPoint point = item1.getPoint();
        Point ptScreenCoord = new Point();
        mapView.getProjection().toPixels(point, ptScreenCoord);

        GeoPoint slavePoint = item2.getPoint();
        Point slavePtScreenCoord = new Point();
        mapView.getProjection().toPixels(slavePoint, slavePtScreenCoord);
        return findDistance(ptScreenCoord.x, ptScreenCoord.y, slavePtScreenCoord.x, slavePtScreenCoord.y);
    }
    
}
