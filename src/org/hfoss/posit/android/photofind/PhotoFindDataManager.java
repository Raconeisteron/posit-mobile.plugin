package org.hfoss.posit.android.photofind;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import org.hfoss.posit.android.Log;
import org.hfoss.posit.android.api.FindDataManager;
import org.hfoss.posit.android.provider.PositDbHelper;
import org.hfoss.posit.android.utilities.Utils;
import org.hfoss.third.Base64Coder;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Images.Media;

public class PhotoFindDataManager extends FindDataManager{

	private static PhotoFindDataManager sInstance = null;
	private static String TAG = "PhotoFindDataManager";
	public static final int THUMBNAIL_TARGET_SIZE = 320;
	
	private PhotoFindDataManager(){}
	
	public static PhotoFindDataManager getInstance(){
		if(sInstance == null){
			sInstance = new PhotoFindDataManager();
		}
		return sInstance;
	}
	
	@Override
	public String getBase64StringFromUri(Uri uri, Context context) {
		ByteArrayOutputStream imageByteStream = new ByteArrayOutputStream();
		byte[] imageByteArray = null;
		Bitmap bitmap = null;

		try {
			bitmap = android.provider.MediaStore.Images.Media.getBitmap(
					context.getContentResolver(), uri);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (bitmap == null) {
			Log.d(TAG, "No bitmap");
		}
		// Compress bmp to jpg, write to the byte output stream
		bitmap.compress(Bitmap.CompressFormat.JPEG, 80, imageByteStream);
		// Turn the byte stream into a byte array
		imageByteArray = imageByteStream.toByteArray();
		char[] base64 = Base64Coder.encode(imageByteArray);
		String base64String = new String(base64);
		return base64String;
	}
	
	@Override
	public ContentValues saveBase64StringAsUri(String base64string, Context context) {

		// convert data to bitmap
		byte[] data = Base64Coder.decode(base64string);
		Bitmap image_bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		
		return saveBitmapAsUri(image_bitmap, context);
	}

	public ContentValues saveBitmapAsUri(Bitmap image_bitmap, Context context) {
		ContentValues result = new ContentValues();
		
		// store bitmap in a ContentProvider along with additional metadata
		ContentValues values = new ContentValues();
		
		values.put(MediaColumns.TITLE, "posit image");
		values.put(ImageColumns.BUCKET_DISPLAY_NAME, "posit");
		values.put(ImageColumns.IS_PRIVATE, 0);
		values.put(MediaColumns.MIME_TYPE, "image/jpeg");
		
		Uri imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		
		OutputStream outstream;
		try {
			outstream = context.getContentResolver().openOutputStream(imageUri);
			image_bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outstream);
			outstream.close();
		} catch (Exception e) {
				Log.i(TAG, "Exception writing image file " + e.getMessage());
				e.printStackTrace();
		}
		if(Utils.debug) {
			Log.i(TAG, "Saved image file, uri = " + imageUri.toString());
		}

		// Now create a thumbnail and save it
		int width = image_bitmap.getWidth();
		int height = image_bitmap.getHeight();
		int newWidth = THUMBNAIL_TARGET_SIZE;
		int newHeight = THUMBNAIL_TARGET_SIZE;

		float scaleWidth = ((float)newWidth)/width;
		float scaleHeight = ((float)newHeight)/height;

		Matrix matrix = new Matrix();
		matrix.setScale(scaleWidth, scaleHeight);
		Bitmap thumbnailImage = Bitmap.createBitmap(image_bitmap, 0, 0,width,height,matrix,true);

		int imageId = Integer.parseInt(imageUri.toString().substring(Media.EXTERNAL_CONTENT_URI.toString().length()+1));	

		values = new ContentValues(4);
		values.put(Images.Thumbnails.KIND, Images.Thumbnails.MINI_KIND);
		values.put(Images.Thumbnails.IMAGE_ID, imageId);
		values.put(Images.Thumbnails.HEIGHT, height);
		values.put(Images.Thumbnails.WIDTH, width);
		
		Uri thumbnailUri = context.getContentResolver().insert(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, values);
		
		try {
			outstream = context.getContentResolver().openOutputStream(thumbnailUri);
			thumbnailImage.compress(Bitmap.CompressFormat.JPEG, 70, outstream);
			outstream.close();
		} catch (Exception e) {
				Log.i(TAG, "Exception writing thumbnail file " + e.getMessage());
				e.printStackTrace();
		}
		if(Utils.debug) {
			Log.i(TAG, "Saved image file, uri = " + imageUri.toString());
		}

		// finally insert image and thumbnail Uri's into result
		result.put(PositDbHelper.PHOTOS_IMAGE_URI, imageUri.toString());
		result.put(PositDbHelper.PHOTOS_THUMBNAIL_URI, thumbnailUri.toString());
		
		return result;

	}
}
