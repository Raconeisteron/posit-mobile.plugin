package org.hfoss.posit.android.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public interface FindInterface {

	public void setGuid(String guid);
	
	/**
	 * 	getContent() returns the Find's <attr:value> pairs in a ContentValues array. 
	 *  This method assumes the Find object has been instantiated with a context
	 *  and an id.
	 *  @return A ContentValues with <key, value> pairs
	 */
	public ContentValues getContent();
		
	public Uri getImageUriByPosition(long findId, int position);
	
	
	/**
	 * Deprecated
	 * Returns a hashmap of string key and value
	 * this is used primarily for sending and receiving over Http, which is why it 
	 * somewhat works as everything is sent as string eventually. 
	 * @return
	 */
	public HashMap<String,String> getContentMap();
	
	/**
	 * Returns a hashmap of key/value pairs represented as Strings.
	 * This is used primarily for sending and receiving over Http.
	 * @return
	 */
	public HashMap<String,String> getContentMapGuid();

	/** 
	 * Creates an entry for a Find in the DB. 
	 * Assumes that the context has been passed through a constructor.
	 * @param content contains the Find's attributes and values.  
	 * @param images list of contentvalues containing the image references to add
	 * @return whether the DB operation succeeds
	 */
	public boolean insertToDB(ContentValues content, List<ContentValues> images);
	
	/**
	 * Inserts images for this find
	 * @param images
	 * @return
	 */
	public boolean insertImagesToDB(List<ContentValues> images);

	/** 
	 * updateDB() assumes that the context and rowID has be passed through 
	 * a constructor.
	 * @param content contains the Find's attributes and values.  
	 * @return whether the DB operation succeeds
	 */
	// TODO: Confirm that this works with GUIDs
	public boolean updateToDB(ContentValues content);
	
	/**
	 * Updates a find given its guid.
	 * @param guid
	 * @param content
	 * @return
	 */
	public boolean updateToDB(String guid, ContentValues content);
	
	/**
	 * deletes the Find object form the DB, not including its photos
	 * Call deleteFindPhotos() to delete its photos.
	 * @return whether the DB operation was successful
	 */
	public boolean delete();
	
	/**
	 * deletes the photos associated with this find.
	 * @return
	 */
	public boolean deleteFindPhotos();

	/**
	 * @return the mId
	 */
	public long getId();
	
	/**
	 * @return the guId
	 */
	public String getguId();
	
	
	/**
	 * NOTE: This may cause a leak because the Cursor is not closed
	 * Get all images attached to this find
	 * @return the cursor that points to the images
	 */
	public Cursor getImages();
	
	public ArrayList<ContentValues> getImagesContentValuesList();

	
	/**
	 * @return whether or not there are images attached to this find
	 */
	public boolean hasImages();

	public boolean deleteImageByPosition(int position);
	

	/**
	 * Directly sets the Find as either Synced or not.
	 * @param status
	 */
	public void setSyncStatus(boolean status);
	
	// TODO: Test that this method works with GUIDs
	public int getRevision();
	
	/**
	 * Used for adhoc finds.  Tests whether find already exists
	 * @param guid
	 * @return
	 */
	public boolean exists(String guid);
	
	/**
	 * Tests whether the Find is synced.  This method should work with
	 * either GUIDs (Find from a server) or row iDs (Finds from the phone).
	 * If neither is set, it returns false.
	 * @return
	 */
	public boolean isSynced();

}
