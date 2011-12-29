/*
 * File: MapFindsActivity.java
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

package org.hfoss.posit.android.experimental.api.activity;


import java.util.List;

import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.database.DbManager;
import org.hfoss.posit.android.experimental.plugin.csv.CsvListFindsActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

/**
 *  This class retrieves Finds from the Db or from a Csv List and
 *  displays them as an overlay on a Google map. When clicked, 
 *  the finds start a FindActivity. Allowing them to be edited.
 *
 */
public class MapFindsActivity extends OrmLiteBaseMapActivity<DbManager>  {

	private static final String TAG = "MapFindsActivity";
	private MapView mMapView;
	private MapController mapController;
	private MyLocationOverlay myLocationOverlay;
	private static int mapZoomLevel = 0;
//	private LinearLayout linearLayout;
	private List<Overlay> mapOverlays;
	private Drawable drawable;
	private boolean zoomFirst = true;
//	private LinearLayout searchLayout;
//	private Button search_first_Btn;
//	private Button search_next_Btn;
//	private Button search_prev_Btn;
//	private Button search_last_Btn;
	
	private static List<? extends Find> finds = null;
	

	/* (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.map_finds);
		
		// Check if this is a CSV Finds case
		Intent intent = getIntent();
		if (intent != null) {
			String action = intent.getAction();
			if (action != null && action.equals(CsvListFindsActivity.ACTION_CSV_FINDS)) {
				finds = CsvListFindsActivity.getFinds();
			}
		}
		
		mMapView = (MapView) findViewById(R.id.mapView);
		mMapView.setBuiltInZoomControls(true);
		
		// Create a mylocation overlay, add it and refresh it
	    myLocationOverlay = new MyLocationOverlay(this, mMapView);
	    mMapView.getOverlays().add(myLocationOverlay);
	    mMapView.postInvalidate();
	    		
//		searchLayout = (LinearLayout) findViewById(R.id.search_finds_layout);
//		search_first_Btn = (Button) findViewById(R.id.search_finds_first);
//		search_next_Btn = (Button) findViewById(R.id.search_finds_next);
//		search_prev_Btn = (Button) findViewById(R.id.search_finds_previous);
//		search_last_Btn = (Button) findViewById(R.id.search_finds_last);
		
//		search_first_Btn.setOnClickListener(search_first_lstn);
//		search_next_Btn.setOnClickListener(search_next_lstn);
//		search_prev_Btn.setOnClickListener(search_prev_lstn);
//		search_last_Btn.setOnClickListener(search_last_lstn);		
	}
    
	/** 
	 * This method is called when the activity is ready to start 
	 *  interacting with the user. It is at the top of the Activity
	 *  stack.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		
		// Enable my location
	    myLocationOverlay.enableMyLocation();
		myLocationOverlay.enableCompass();	
		mapFinds();
		
//		if (zoomFirst) {
//			// Zoom into current position
//			Log.d("MapFindsActivity:onResume", "Zoom into currrent location");
//			int latitude = 0;
//			int longitude = 0;
//						
//			if (loc != null) {
//				latitude = (int) (loc.getLatitude()*1E6);
//				longitude = (int) (loc.getLongitude()*1E6);
//				mapController.setCenter(new GeoPoint(latitude, longitude));
//				mapController.setZoom(14);
//				zoomFirst = false;
//			} else {
//				// Move to first find
//				if (!mCursor.moveToFirst()) {
//					// No Finds
//					Toast.makeText(this,"No finds.", 10000);
//					mapController.setZoom(1);
//				} else {
//					// Get first "Find"
//					Toast.makeText(this,"Could not retrieve GPS position\nMoving to first find. ", 1000);
//					//latitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LATITUDE))*1E6);
//					//longitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LONGITUDE))*1E6);
//					mapController.setCenter(new GeoPoint(latitude, longitude));
//					mapController.setZoom(14);
//					zoomFirst = false;
//				}
//			}
//		}
		
//		searchLayout.setVisibility(LinearLayout.INVISIBLE);
	}
		
	/**
	 * Called when the system is about to resume some other activity.
	 *  It can be used to save state, if necessary.  In this case
	 *  we close the cursor to the DB to prevent memory leaks.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onPause(){
		super.onPause();
		myLocationOverlay.disableMyLocation();
	}

	/**
	 * Gets the Finds to be mapped.  These may have been passed in from
	 * Csv plugin, in which case the finds list is non-null.  Otherwise
	 * get the Finds from the Db.
	 */
	private void mapFinds() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		int pid = sp.getInt(getString(R.string.projectPref), 0);
		
		if (finds == null) 
			finds = this.getHelper().getFindsByProjectId(pid);
		if (finds.size() <= 0) { // No finds
			Toast.makeText(this, "No finds to display", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		mapOverlays = mMapView.getOverlays();
		mapOverlays.add(mapLayoutItems(finds));	
		mapController = mMapView.getController();
		if (mapZoomLevel != 0)
			mapController.setZoom(mapZoomLevel);
		else
			mapController.setZoom(14);
	}

	/**
	 * Displays each Find in the map View.
	 * @param finds, a List of Finds
	 * @return
	 */
	private  FindOverlay mapLayoutItems(List<? extends Find> finds) {
		int latitude = 0;
		int longitude = 0;
		int id = 0;

		drawable = this.getResources().getDrawable(R.drawable.bubble);
		FindOverlay mPoints = new FindOverlay(drawable, this, true, this.getIntent().getAction());

		for(Find find  : finds) {
			latitude = (int) (find.getLatitude()*1E6);
			longitude = (int) (find.getLongitude()*1E6);

			id = find.getId();
			Intent intent = getIntent();
			if (intent != null) {
				String action = intent.getAction();
				if (action != null && action.equals(CsvListFindsActivity.ACTION_CSV_FINDS)) {
					id = Integer.parseInt(find.getGuid());
				}
			}
		
			String description = find.getGuid() + "\n" + find.getName() + "\n" + find.getDescription(); 

			Log.i(TAG, latitude + " " + longitude + " " + description);
			
			mPoints.addOverlay(new OverlayItem(new GeoPoint(latitude,longitude),String.valueOf(id),description));
		}
		return mPoints;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_I) {
			// Zoom In
			mapZoomLevel = mMapView.getZoomLevel();
			mapController.zoomIn();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_O) {
			// Zoom Out
			mapZoomLevel = mMapView.getZoomLevel();
			mapController.zoomOut();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_S) {
			// Switch on the satellite images
			mMapView.setSatellite(!mMapView.isSatellite());		
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_T) {
			// Switch on traffic overlays
			mMapView.setTraffic(!mMapView.isTraffic());
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
// 			showDialog();
		}
		
		return false;
	}
	
	/** 
	 * Creates the menu for this activity by inflating a menu resource file.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_finds_menu, menu);
		return true;
	} // onCreateOptionsMenu()
	
	/** 
	 * Handles the various menu item actions.
	 * @param featureId is unused
	 * @param item is the MenuItem selected by the user
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.my_location_mapfind_menu_item:
			if (myLocationOverlay.isMyLocationEnabled()) {
				myLocationOverlay.disableMyLocation();
				myLocationOverlay.disableCompass();
				item.setTitle(R.string.my_location_on);
			} else {
				myLocationOverlay.enableMyLocation();
				myLocationOverlay.enableCompass();	
				mapController.setCenter(myLocationOverlay.getMyLocation());
				item.setTitle(R.string.my_location_off);
			}
			break;
//		case R.id.search_finds_mapfind_menu_item:
//			searchFinds();
//			break;
		//case R.id.toggle_tracks_mapfind_menu_item:
		//	toggleTracks();
		//	break;
//		case R.id.center_finds_mapfind_menu_item:
//			centerFinds();
//			break;
		default:
			return false;
		}
		return true;
	} // onMenuItemSelected
	
	
//	/**
//	 * Toggle arrows that will allow you to move between different finds.
//	 * Also shows a toast to reflect the find that you are currently looking at.
//	 */
//	private void searchFinds() {
//		if (searchLayout.getVisibility() == LinearLayout.VISIBLE) {
//			searchLayout.setVisibility(LinearLayout.INVISIBLE);
//		} else {
//			searchLayout.setVisibility(LinearLayout.VISIBLE);
//		}
//	} // searchFinds
	
//	private OnClickListener search_first_lstn = new OnClickListener() {
//        public void onClick(View v) {
//            // On Click for Search Finds First
//        	if (!mCursor.moveToFirst()) {
//				// No Finds
//				Utils.showToast(v.getContext(), "No finds.");
//        	} else {
//        		int latitude;
//        		int longitude;
//        		
//        		Utils.showToast(v.getContext(), "Find " + (mCursor.getPosition() + 1) + " of " + mCursor.getCount());
//        		latitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LATITUDE))*1E6);
//				longitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LONGITUDE))*1E6);
//				mapController.setCenter(new GeoPoint(latitude, longitude));
//				//mapController.setZoom(14);
//        	}
//        }
//    };
//
//    private OnClickListener search_next_lstn = new OnClickListener() {
//        public void onClick(View v) {
//            // On Click for Search Finds Next
//        	if (!mCursor.moveToNext()) {
//				// No further Finds
//        		mCursor.moveToLast();
//				Utils.showToast(v.getContext(), "No further finds.");
//        	} else {
//        		int latitude;
//        		int longitude;
//        		
//        		Utils.showToast(v.getContext(), "Find " + (mCursor.getPosition() + 1) + " of " + mCursor.getCount());
//        		latitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LATITUDE))*1E6);
//				longitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LONGITUDE))*1E6);
//				mapController.setCenter(new GeoPoint(latitude, longitude));
//				//mapController.setZoom(14);
//        	}
//        }
//    };
//    
//    private OnClickListener search_prev_lstn = new OnClickListener() {
//        public void onClick(View v) {
//            // On Click for Search Finds Previous
//        	if (!mCursor.moveToPrevious()) {
//				// No further Finds
//        		mCursor.moveToFirst();
//				Utils.showToast(v.getContext(), "No further finds.");
//        	} else {
//        		int latitude;
//        		int longitude;
//        		
//        		Utils.showToast(v.getContext(), "Find " + (mCursor.getPosition() + 1) + " of " + mCursor.getCount());
//        		latitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LATITUDE))*1E6);
//				longitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LONGITUDE))*1E6);
//				mapController.setCenter(new GeoPoint(latitude, longitude));
//				//mapController.setZoom(14);
//        	}
//        }
//    };
//    
//    private OnClickListener search_last_lstn = new OnClickListener() {
//        public void onClick(View v) {
//            // On Click for Search Finds Last
//        	if (!mCursor.moveToLast()) {
//				// No further Finds
//				Utils.showToast(v.getContext(), "No finds.");
//        	} else {
//        		int latitude;
//        		int longitude;
//        		
//        		Utils.showToast(v.getContext(), "Find " + (mCursor.getPosition() + 1) + " of " + mCursor.getCount());
//        		latitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LATITUDE))*1E6);
//				longitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LONGITUDE))*1E6);
//				mapController.setCenter(new GeoPoint(latitude, longitude));
//				//mapController.setZoom(14);
//        	}
//        }
//    };	
	
//	/**
//	 * Toggle the display of tracks on the map overlay.
//	 */
//	private void toggleTracks() {
//		startActivity(new Intent(this, TrackerActivity.class));
//	} // toggleTracks
	
	
//	/**
//	 * Center and scale the map to show all of the views in the current project.
//   * TODO:  This needs to be revised now that MapFinds can be invoked from 
//	 *  CsvFind which doesn't store Finds in the Db.
//	 */
//	private void centerFinds() {
//		int minLat;
//		int maxLat;
//		int minLong;
//		int maxLong;
//		
//		int latitude;
//		int longitude;
//		
//		List<? extends Find> finds = this.getHelper().getAllFinds();
//		if (finds.size() <= 0) {
//			// No finds at all
//			mapController.setZoom(1);
//		} else {
//			latitude = (int) (finds.get(0).getLatitude()*1E6);
//			minLat = latitude;
//			maxLat = latitude;
//			longitude = (int) (finds.get(0).getLongitude()*1E6);
//			minLong = longitude;
//			maxLong = longitude;
//			// Go through all finds
//			for (Find find : finds) {
//				latitude = (int) (find.getLatitude()*1E6);
//				longitude = (int) (find.getLongitude()*1E6);
//				// Find min and max for all latitudes and longitudes
//				if (latitude < minLat)
//					minLat = latitude;
//				if (latitude > maxLat)
//					maxLat = latitude;
//				if (longitude < minLong)
//					minLong = longitude;
//				if (longitude > maxLong)
//					maxLong = longitude;
//			}
//			mapController.zoomToSpan(maxLat - minLat, maxLong - minLong);
//			mapController.zoomOut(); // One extra zoom out so it doesn't cut off icons
//			mapController.setCenter(new GeoPoint((minLat + maxLat) / 2, (minLong + maxLong) / 2));
//		}
//		
//	} // centerFinds
}