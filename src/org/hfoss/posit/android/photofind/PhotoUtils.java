package org.hfoss.posit.android.photofind;

import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.hfoss.posit.android.Log;
import org.hfoss.posit.android.provider.PositDbHelper;
import org.hfoss.posit.android.utilities.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Images.Media;

public class PhotoUtils {
	public static final int THUMBNAIL_TARGET_SIZE = 320;
	private static final String TAG = "PhotoUtils";
	
	/**
	 * Saves the camera images and associated bitmaps to Media storage and
	 *  save's their respective Uri's in aFind, which will save them to Db.
	 * @param aFind  the current Find we are creating or editing
	 * @param bm the bitmap from the camera
	 */
	public static List<ContentValues> 
		saveImagesAndUris(Context context, List<Bitmap> bitmaps) {
		if (bitmaps.size() == 0) {
			Log.i(TAG, "No camera images to save ...exiting ");
			return null;
		}

		List<Uri> imageUris = new LinkedList<Uri>();
		List<Uri> thumbUris = new LinkedList<Uri>();

		ListIterator<Bitmap> it = bitmaps.listIterator();
		while (it.hasNext()) { 
			Bitmap bm = it.next();

			ContentValues values = new ContentValues();
			values.put(MediaColumns.TITLE, "posit image");
			values.put(ImageColumns.BUCKET_DISPLAY_NAME,"posit");
			
			values.put(ImageColumns.IS_PRIVATE, 0);
			values.put(MediaColumns.MIME_TYPE, "image/jpeg");
			Uri imageUri = context.getContentResolver()
			.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
			OutputStream outstream;
			try {
				outstream = context.getContentResolver().openOutputStream(imageUri);
				bm.compress(Bitmap.CompressFormat.JPEG, 70, outstream);
				outstream.close();
			} catch (Exception e) {
					Log.i(TAG, "Exception writing image file " + e.getMessage());
					e.printStackTrace();
			}
			if(Utils.debug) {
				Log.i(TAG, "Saved image file, uri = " + imageUri.toString());
			}

			// Now create a thumbnail and save it
			int width = bm.getWidth();
			int height = bm.getHeight();
			int newWidth = THUMBNAIL_TARGET_SIZE;
			int newHeight = THUMBNAIL_TARGET_SIZE;

			float scaleWidth = ((float)newWidth)/width;
			float scaleHeight = ((float)newHeight)/height;

			Matrix matrix = new Matrix();
			matrix.setScale(scaleWidth, scaleHeight);
			Bitmap thumbnailImage = Bitmap.createBitmap(bm, 0, 0,width,height,matrix,true);

			int imageId = Integer.parseInt(imageUri.toString()
					.substring(Media.EXTERNAL_CONTENT_URI.toString().length()+1));	

			values = new ContentValues(4);
			values.put(Images.Thumbnails.KIND, Images.Thumbnails.MINI_KIND);
			values.put(Images.Thumbnails.IMAGE_ID, imageId);
			values.put(Images.Thumbnails.HEIGHT, height);
			values.put(Images.Thumbnails.WIDTH, width);
			Uri thumbnailUri = context.getContentResolver()
			.insert(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, values);
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

			imageUris.add(imageUri);
			thumbUris.add(thumbnailUri);
		}
		return retrieveImagesFromUris(imageUris, thumbUris);
	}

	/**
	 * Retrieves images and thumbnails from their uris stores them as <key,value> pairs in a ContentValues,
	 * one for each image.  Each ContentValues is then stored in a list to carry all the images
	 * @return the list of images stored as ContentValues
	 */
	public static List<ContentValues> retrieveImagesFromUris(List<Uri> images, List<Uri> thumbs) {
		List<ContentValues> values = new LinkedList<ContentValues>();
		ListIterator<Uri> imageIt = images.listIterator();
		ListIterator<Uri> thumbnailIt = thumbs.listIterator();

		while (imageIt.hasNext() && thumbnailIt.hasNext()) {
			Uri imageUri = imageIt.next();
			Uri thumbnailUri = thumbnailIt.next();

			ContentValues result = new ContentValues();
			String value = "";
			if (imageUri != null) {
				value = imageUri.toString();
				result.put(PositDbHelper.PHOTOS_IMAGE_URI, value);
				value = thumbnailUri.toString();
				result.put(PositDbHelper.PHOTOS_THUMBNAIL_URI, value);
			}
			values.add(result);
		}
		return values;
	}

}
