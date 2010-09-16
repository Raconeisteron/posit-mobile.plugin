package org.hfoss.adhoc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.provider.PositDbHelper;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.util.Log;

/**
 * Holds the data for an adhoc Find -- i.e. a Find that will be sent to 
 *  other devices through an adhoc mesh network. It must be serializable 
 *  so it can easily be converted to/from an array of bytes.
 *
 */
public class AdhocFind implements Serializable {
	private static final String TAG = "AdhocFind";
	//private HashMap<String, Object> find = null;
	
	private String id;   //  = contentValues.getAsString(getString(R.string.idDB));
	private String name; // = contentValues.getAsString(getString(R.string.nameDB));
	private String description; //  = contentValues.getAsString(getString(R.string.descriptionDB));
	private String latitude; // = contentValues.getAsString(getString(R.string.latitudeDB));
	private String longitude; // = contentValues.getAsString(getString(R.string.longitudeDB));
	private String projectId; 
	
	public AdhocFind(ContentValues values) {
		name = values.getAsString(PositDbHelper.FINDS_NAME);
		description = values.getAsString(PositDbHelper.FINDS_DESCRIPTION);
		longitude = values.getAsString(PositDbHelper.FINDS_LONGITUDE);
		latitude = values.getAsString(PositDbHelper.FINDS_LATITUDE);
		id = values.getAsString(PositDbHelper.FINDS_GUID);
		projectId = values.getAsString(PositDbHelper.FINDS_PROJECT_ID); 
	}
	
	public String toString() {
		return id + " " + name + " " + description + 
			" " + latitude + " " + longitude + " " + projectId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}
	
	
	
//	public AdhocFind(HashMap<String, Object> findsMap) {
//		find = findsMap; // just hope for the best for now
//	}
//
//	public AdhocFind(String json) {
//		try {
//			JSONObject jo = new JSONObject(json);
//			find = new HashMap<String, Object>();
//			Iterator keys = jo.keys();
//			while(keys.hasNext()){
//				String key = (String)keys.next();
//				find.put(key, jo.get(key));
//				Log.i(TAG, find.toString());
//			}
//			
//		} catch (JSONException e) {
//			Log.i(TAG, "not a JSON Object");
//		}
//		
//	}
//
//	@Override
//	public String toString() {
//		if (find == null)
//			return "<no data>";
//		JSONObject jo = new JSONObject(find);
//
//		return jo.toString();
//	}
//
//	public void saveToDB() {
//		Log.e(TAG, "saveToDB "+find);
//		
//	}

}
