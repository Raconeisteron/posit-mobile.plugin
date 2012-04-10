/*
 * File: OverlayItemExtended.java
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

import java.util.Stack;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;


/**
 * Extends the OverlayItem class for maps point clustering. 
 *
 */
public class OverlayItemExtended extends OverlayItem {
    private boolean isClustered = false;
    private boolean isMaster = true;
    private OverlayItemExtended parent;
    private Stack<OverlayItemExtended> slaves = new Stack<OverlayItemExtended>();
	
    public OverlayItemExtended(GeoPoint point, String title, String snippet) {
        super(point, title, snippet);
    } 
    
    /**
     * Set this OverlayItemExtended to clustered if value is true, otherwise not clustered.
     * @param value
     */
    public void setIsClustered(boolean value) {
    	this.isClustered = value;
    }
    
    /**
     * Return true if this OverlayItemExtended is clustered.
     * @return isClustered
     */
    public boolean getIsClustered() {
    	return isClustered;
    }
    
    /**
     * Set this OverlayItemExtended to the master if value is true, otherwise not the master.
     * @param value
     */
    public void setIsMaster(boolean value) {
    	this.isMaster = value;
    }
    
    /**
     * Return true if this OverlayItemExtended is the master of its cluster.
     * @return
     */
    public boolean getIsMaster() {
    	return isMaster;
    } 
        
    /**
     * Set item to be the parent of this OverlayItemExtended
     * @param item
     */
    public void setParent(OverlayItemExtended item) {
    	this.parent = item;
    }    
    
    /**
     * Return the parent of this OverlayItemExtended.
     * @return
     */
    public OverlayItemExtended getParent() {
    	return parent;
    }

    /**
	 * Set this OverlayItemExtended to have the slaves slaves
	 * @param slaves
	 */
	public void setSlaves(Stack<OverlayItemExtended> slaves) {
		this.slaves = slaves;
	}
	
	/**
	 * Return the slaves of this OverlayItemExtended. 
	 * Note only a master OverlayItemExtended will have slaves.	 * 
	 * @return
	 */
	public Stack<OverlayItemExtended> getSlaves() {
		return slaves;
	}
       
}
