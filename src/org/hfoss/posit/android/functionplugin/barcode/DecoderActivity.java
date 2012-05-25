/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hfoss.posit.android.functionplugin.barcode;

import java.io.IOException;
import java.util.Collection;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.plugin.AddFindPluginCallback;
import org.hfoss.posit.android.api.plugin.ListFindPluginCallback;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.functionplugin.barcode.CameraManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;


/**
 * Example Decoder Activity.
 * 
 */
public class DecoderActivity extends Activity implements IDecoderActivity, SurfaceHolder.Callback, AddFindPluginCallback, ListFindPluginCallback {
    private static final String TAG = DecoderActivity.class.getSimpleName();

    protected DecoderActivityHandler handler = null;
    protected ViewfinderView viewfinderView = null;
    protected CameraManager cameraManager = null;
    protected boolean hasSurface = false;
    protected Collection<BarcodeFormat> decodeFormats = null;
    protected String characterSet = null;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.decoder);
        Log.v(TAG, "onCreate()");
        
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
 
        handler = null;
        hasSurface = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()");
        
        
        // CameraManager must be initialized here, not in onCreate().
        if (cameraManager==null) cameraManager = new CameraManager(getApplication());
        

        if (viewfinderView==null) {
        	setContentView(R.layout.decoder);
            viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
            //if (viewfinderView==null) return;
            viewfinderView.setCameraManager(cameraManager);
        }
       
        
        showScanner();
        
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        if(surfaceView==null)return;
        
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause()");
        
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }

        cameraManager.closeDriver();
        
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            if(surfaceView==null)return;
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_FOCUS || keyCode == KeyEvent.KEYCODE_CAMERA) {
            // Handle these events so they don't launch the Camera app
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

   
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

   
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Ignore
    }

    
    public ViewfinderView getViewfinder() {
        return viewfinderView;
    }

   
    public Handler getHandler() {
        return handler;
    }

    
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    
    public void handleDecode(Result rawResult, Bitmap barcode) {
        drawResultPoints(barcode, rawResult);
    }

    protected void drawResultPoints(Bitmap barcode, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.result_image_border));
            paint.setStrokeWidth(3.0f);
            paint.setStyle(Paint.Style.STROKE);
            Rect border = new Rect(2, 2, barcode.getWidth() - 2, barcode.getHeight() - 2);
            canvas.drawRect(border, paint);

            paint.setColor(getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1]);
            } else if (points.length == 4 && 
                          ( rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A || 
                            rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13) ) 
            {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1]);
                drawLine(canvas, paint, points[2], points[3]);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    canvas.drawPoint(point.getX(), point.getY(), paint);
                }
            }
        }
    }

    protected static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b) {
        canvas.drawLine(a.getX(), a.getY(), b.getX(), b.getY(), paint);
    }

    protected void showScanner() {
        viewfinderView.setVisibility(View.VISIBLE);
    }

    protected void initCamera(SurfaceHolder surfaceHolder) {
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) handler = new DecoderActivityHandler(this, decodeFormats, characterSet, cameraManager);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
        }
    }

	
	public void listFindCallback(Context context, Find find, View view) {
		// TODO Auto-generated method stub
		
	}

	
	public void menuItemSelectedCallback(Context context, Find find, View view,
			Intent intent) {
		// TODO Auto-generated method stub
		
	}

	
	public void onActivityResultCallback(Context context, Find find, View view,
			Intent intent) {
		// TODO Auto-generated method stub
		
	}

	
	public void displayFindInViewCallback(Context context, Find find, View view) {
		// TODO Auto-generated method stub
		
	}

	
	public void afterSaveCallback(Context context, Find find, View view,
			boolean isSaved) {
		// TODO Auto-generated method stub
		
	}

	
	public void finishCallback(Context context, Find find, View view) {
		// TODO Auto-generated method stub
		
	}

	public void onClickCallback(Context context, Find find, View view) {
		// TODO Auto-generated method stub
		
	}
	
	
}
