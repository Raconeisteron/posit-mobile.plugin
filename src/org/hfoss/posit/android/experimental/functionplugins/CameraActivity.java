/*
 * File: CameraActivity.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Search and Identification Tool.
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

package org.hfoss.posit.android.experimental.functionplugins;

import java.io.ByteArrayOutputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * This class calls the camera application and returns the Base64 string representation of the image
 *
 */

public class CameraActivity extends Activity {

	public static final String PREFERENCES_IMAGE = "Image";
	static final int TAKE_CAMERA_REQUEST = 1000;
	private String img_str = null; //stores base64 string of the image
	
	private LinearLayout linear;
	private Button btnPic;
	private Button btnCont;
	private ImageView photo;
			
    /** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    
	    linear = new LinearLayout(this);
	    linear.setOrientation(LinearLayout.VERTICAL);
	    
	    photo = new ImageView(this);
	    photo.setVisibility(View.INVISIBLE); //this is just to change the button text
	    linear.addView(photo);
 
	    btnPic = new Button(this);
	    if(photo.getVisibility() == View.INVISIBLE){
		    btnPic.setText("Take Picture");
	    }
	    else{
		    btnPic.setText("Retake Picture");
	    }
	    linear.addView(btnPic);
	    
	    btnPic.setOnClickListener(new View.OnClickListener(){
	    	public void onClick(View v){
	    		//launch the camera
	    		Intent pictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
	    		//get the full picture
	    		pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString());
	    		//how to handle the picture taken
	    		startActivityForResult(pictureIntent, TAKE_CAMERA_REQUEST);
	    	}
	    });
	    
	    btnCont = new Button(this);
	    btnCont.setText("Continue");
	    linear.addView(btnCont);
	    
	    btnCont.setOnClickListener(new View.OnClickListener(){
	    	public void onClick(View v){
	    		//pass base64 string to calling function
				Intent intent=new Intent();  
				intent.putExtra("Photo", img_str);
				setResult(RESULT_OK, intent);
	    		finish();
	    	}
	    });
	    
	    setContentView(linear);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		switch(requestCode){
		case TAKE_CAMERA_REQUEST:
			if(resultCode == Activity.RESULT_CANCELED){
			}
			else if(resultCode == Activity.RESULT_OK){
				//handle photo taken
				Bitmap cameraPic = (Bitmap) data.getExtras().get("data");
				
				//encode to base64 string
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				cameraPic.compress(Bitmap.CompressFormat.JPEG, 100, baos);
				byte[] b = baos.toByteArray();
				img_str = Base64.encodeToString(b, Base64.DEFAULT);
/*				
				//test decoding it back into an image
				byte[] c = Base64.decode(img_str, Base64.DEFAULT);
			    Bitmap bmp = BitmapFactory.decodeByteArray(c, 0, c.length);
*/			    
			    photo.setImageBitmap(cameraPic);//display the retrieved image
			    photo.setVisibility(View.VISIBLE);
			    btnPic.setText("Retake Picture");//we now have an image	    
			}
			break;
		}
	}
}