package org.hfoss.posit.android.functionplugin.camera;

import org.hfoss.posit.android.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

public class FullScreenImageViewer extends Activity {
	
	public static final String TAG="FullScreenImageViewer";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.full_image);

		Log.i(TAG, "onCreate()");
		Intent intent = getIntent();
		//		  Uri uri = intent.getData();
		String path = intent.getData().getPath();
		ImageView imageView = (ImageView)findViewById(R.id.fullImage);
		Bitmap bm = Camera.getBitmapFromPath(this, path);
		imageView.setImageBitmap(bm);
		imageView.setScaleType(ImageView.ScaleType.FIT_XY);
	}
 }
