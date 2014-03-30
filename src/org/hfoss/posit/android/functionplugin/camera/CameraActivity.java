/*
 * File: CameraActivity.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
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

package org.hfoss.posit.android.functionplugin.camera;

import java.io.ByteArrayOutputStream;


import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.plugin.AddFindPluginCallback;
import org.hfoss.posit.android.api.plugin.ListFindPluginCallback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

/**
 * This class calls the camera application and returns the Base64 string representation of the image
 *
 */

public class CameraActivity extends Activity 
	implements AddFindPluginCallback, ListFindPluginCallback {

	public static final String TAG="CameraActivity";
	public static final String PHOTO_MENU = "Take Photo"; // Should match plugin preference
	
	public static final String PREFERENCES_IMAGE = "Image";
	static final int TAKE_CAMERA_REQUEST = 1000;
	private static String img_str = null; //stores base64 string of the image
	
	private ImageView photo;
//	private Uri mImageUri;
	
	private static Intent mIntent;
	private static Context mContext;
	private static Find mFind;
	private static View mView;
			
    /** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	 
	   // mImageUri = getFullSizeImage();
	    
	    if (mIntent.getAction().equals(PHOTO_MENU)) {
	    	Log.i(TAG, "Starting Camera from Intent");
	    	Intent pictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
	    	pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString());
	    	startActivityForResult(pictureIntent, TAKE_CAMERA_REQUEST);
	    } else {
	    	
	    }
	}
	
 	
	/**
	 * Handles the picture that was taken
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    Log.i(TAG, "onActivityResult, request code= " + requestCode);

		switch(requestCode) {
		case TAKE_CAMERA_REQUEST:
			
			if(resultCode == Activity.RESULT_CANCELED){
			}
			else if(resultCode == Activity.RESULT_OK){
				
				//handle photo taken
				Bitmap cameraPic = (Bitmap) data.getExtras().get("data");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				cameraPic.compress(Bitmap.CompressFormat.JPEG, 100, baos);
				byte[] b = baos.toByteArray();
				
				//encode to base64 string -- Does this compress it?
				img_str = Base64.encodeToString(b, Base64.DEFAULT);
				Log.i(TAG, "Converted image to base64 string, len = " + img_str.length());

				photo = new ImageView(this);
			    photo.setImageBitmap(cameraPic);     //display the retrieved image
			    photo.setVisibility(View.VISIBLE);

			    //pass base64 string to calling function
				Intent resultIntent=new Intent();  
				resultIntent.putExtra("Photo", img_str);
				setResult(RESULT_OK, resultIntent);
	    		finish();
			}
			break;
		}
	}

	/**
	 * Required for function plugins. Set the Finds thumbnail.
	 */
	public void listFindCallback(Context context, Find find, View view) {
		//Display the thumbnail picture beside the find
		//or a default image if there isn't one
		ImageView iv = (ImageView) view.findViewById(R.id.find_image);
		Bitmap bmp = Camera.getPhotoAsBitmap(find.getGuid(), context);
		if(bmp != null){
		    iv.setImageBitmap(bmp);
		}
		else{
		    iv.setImageResource(R.drawable.ic_menu_camera);
		}		
	}
	/**
	 * Required for function plugins. Unused here.
	 */
	public void menuItemSelectedCallback(Context context, Find find, View view,
			Intent intent) {
		mIntent = intent;
		
	}
	/**
	 * Display the image in FindActivity's view.
	 */
	public void onActivityResultCallback(Context context, Find find, View view, Intent intent) {
		Log.i(TAG, "onActivityResultCallback");
		if (intent != null) {
			// do we get an image back?
			if (intent.getStringExtra("Photo") != null) {
				ImageView photo = (ImageView) view.findViewById(R.id.photo);
				if (photo == null)
					return;
				
				String img_str = intent.getStringExtra("Photo");
				byte[] c = Base64.decode(img_str, Base64.DEFAULT);
				Bitmap bmp = BitmapFactory.decodeByteArray(c, 0, c.length);
				photo.setImageBitmap(bmp);// display the retrieved image
				photo.setVisibility(View.VISIBLE);
			}
		}
	}

	/**
	 * If this Find has a camera image, display it. The photo ImageView is
	 * an INVISIBLE view in the Basic interface
	 */
	public void displayFindInViewCallback(Context context, Find find, View view) {
		Log.i(TAG, "displayFindInViewCallback");
		Bitmap bmp = Camera.getPhotoAsBitmap(find.getGuid(), context);
		ImageView photo = (ImageView) view.findViewById(R.id.photo);

		if (bmp != null) {
			// we have a picture to display
			if (photo != null) {
				photo.setImageBitmap(bmp);
				photo.setVisibility(View.VISIBLE);
			}
		} else {
			// we don't have a picture to display. Nothing should show up, but
			// this is to make sure.
			if (photo != null)
				photo.setVisibility(View.INVISIBLE);
		}		
	}
	
	/**
	 * If this Find's camera image was clicked, display a full sized view of it.
	 * NOTE: This method is currently not being called.  See FindActivity. 
	 * It generates an error when you try to start the activity.
	 */
	public void onClickCallback (Context context, Find find, View view) {
		Log.i(TAG, "onClickCallback " + this.toString() + " " + context.toString());
		mContext = context;
		mFind = find;
		mView = view;
	}
	
//	private void displayPhoto() {
//		String path = Camera.getPhotoPath(mContext, mFind.getGuid());
//		Log.i(TAG, "Path = " + path);
//		
//		Uri.Builder builder = new Uri.Builder();
//		builder.path(path);
//		Uri uri = builder.build();
//		
//		Intent intent = new Intent(this,FullScreenImageViewer.class);
//		intent.setData(uri);
//		if (intent != null)
//			this.startActivity(intent); 			
//	}

	/**
	 * Called from FindActivity after a Find has been saved. Saves the
	 * image to a file.
	 */
	public void afterSaveCallback(Context context, Find find, View view, boolean isSaved) {
		// if the find is saved, we can save/update the picture to the phone
		if (isSaved) {
			// do we even have an image to save?
			if (img_str != null) {
				Log.i(TAG, "There is an image to save.");
				if (Camera.savePhoto(find.getGuid(), img_str, context)) {
					Log.i(TAG, "Successfully saved photo to phone with guid: "
							+ find.getGuid());
				} else {
					Log.i(TAG, "Failed to save photo to phone with guid: "
							+ find.getGuid());
				}
			}
		}
	
	}	
	
	/**
	 * Called from FindActivity once a FindActivity has been finished.
	 */
	public void finishCallback(Context context, Find find, View view) {
		img_str = null;
	}
	
	   // See http://stackoverflow.com/questions/6448856/android-camera-intent-how-to-get-full-sized-photo
//	private Uri get FullSizeImage() {
//	    File photo;
//	    try
//	    {
//	        // place where to store camera taken picture
//	        photo = this.createTemporaryFile("picture", ".jpg");
//	        photo.delete();
//	    }
//	    catch(Exception e)
//	    {
//	        Log.v(TAG, "Can't create file to take picture!");
//	        Toast.makeText(this, "Please check SD card! Image shot is impossible!", 10000);
//	        return;
//	    }
//	    mImageUri = Uri.fromFile(photo);    
//	    mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
//	    activity.startActivityForResult(this, intent, MenuShootImage);
//	}
	
//	public static File createTemporaryFile(String part, String ext) throws Exception  {
//		File tempDir= Environment.getExternalStorageDirectory();
//		tempDir=new File(tempDir.getAbsolutePath()+"/.temp/");
//		if(!tempDir.exists()) {
//			tempDir.mkdir();
//		}
//		return File.createTempFile(part, ext, tempDir);
//	}

	
//	public Bitmap grabImage(Uri uri)  {
//		Log.i(TAG, "Uri " + uri);
//	    this.getContentResolver().notifyChange(uri, null);
//	    ContentResolver cr = this.getContentResolver();
//	    Bitmap bitmap;
//	    try
//	    {
//	        bitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, uri);
//	        return bitmap;
//	    }
//	    catch (Exception e)
//	    {
//	        Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show();
//	        Log.d(TAG, "Failed to load", e);
//	    }
//	    return null;
//	}
	
	// Uncomment and call this to display image (e.g., to test quality rcvd from Camera)
//	private void displayImage(Uri imageUri) {
//		Bitmap cameraPic = grabImage(mImageUri);
//		Intent intent = new Intent(this, FullScreenImageViewer.class);
//		intent.setData(mImageUri);
//		startActivity(intent);	
//	}
	
	// Uncomment and call this to save the image (not a string) to a file
	// An image saved in this way will be viewable in the Gallery
//	private void saveImageToFile(byte[] bytes) {
//		ContentValues cv = new ContentValues();
//		cv.put(Images.Media.TITLE, "TestImage1");
//		cv.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
//		cv.put(Images.Media.MIME_TYPE, "image/jpg");				
//		Uri uri = getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, cv);
//		
//		try {
//			OutputStream os = getContentResolver().openOutputStream(uri);
//			os.write(b);
//			os.flush();
//			os.close();
//			Toast.makeText(this, "Image Saved", Toast.LENGTH_LONG);
//	
//		} catch (IOException e) {
//			Toast.makeText(this, "Error saving picture", Toast.LENGTH_LONG);
//		}		
//	}

	
	
	
}