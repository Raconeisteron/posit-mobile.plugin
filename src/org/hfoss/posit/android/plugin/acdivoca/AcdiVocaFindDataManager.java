package org.hfoss.posit.android.plugin.acdivoca;

import org.hfoss.posit.android.api.FindDataManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;


public class AcdiVocaFindDataManager extends FindDataManager{

	private static AcdiVocaFindDataManager sInstance = null;
	private static String TAG = "AcdiVocaFindDataManager";
	
	private AcdiVocaFindDataManager(){}
	
	public static AcdiVocaFindDataManager getInstance(){
		if(sInstance == null){
			sInstance = new AcdiVocaFindDataManager();
		}
		return sInstance;
	}
	
	/**
	 * Saves updated finds data to db
	 * @param id
	 * @param data
	 */
	public boolean updateFind(Context context, int id, ContentValues data) {
		if (data == null)
			return false;
		AcdiVocaDbHelper dbHelper = new AcdiVocaDbHelper(context); 
		return dbHelper.updateFind(id, data);
	}
	
	/**
	 * Saves new finds data to db
	 * @param id
	 * @param data
	 */
	public boolean addNewFind(Context context, ContentValues data) {
		AcdiVocaDbHelper dbHelper = new AcdiVocaDbHelper(context); 
		return dbHelper.addNewFind(data) != -1;
	}	
	
	/**
	 * Fetches all finds for a given project.
	 * NOTE: Currently ACDI VOCA project_id = 0.
	 * @param context
	 * @param project_id
	 * @param order_by
	 * @return
	 */
	public Cursor fetchFindsByProjectId(Context context, int project_id, String order_by) {
		AcdiVocaDbHelper dbHelper = new AcdiVocaDbHelper(context); 
		return dbHelper.fetchFindsByProjectId(project_id, order_by);
	}
	
	/**
	 * Looks up a pre-existing find by its row id.
	 * @param context
	 * @param id
	 * @param columns
	 * @return
	 */
	public ContentValues fetchFindDataById(Context context, int id, String[] columns) {
		AcdiVocaDbHelper dbHelper = new AcdiVocaDbHelper(context); 
		return dbHelper.fetchFindDataById(id, columns);
	}
	
	/**
	 * The following methods are required by FindDataManagerInterface. 
	 */
	public String getBase64StringFromUri(Uri uri, Context context) { return null;}
	public ContentValues saveBase64StringAsUri(String base64string, Context context) {return null;}
	public ContentValues saveBitmapAsUri(Bitmap image_bitmap, Context context) {return null;}
}
