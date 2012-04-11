package org.hfoss.posit.android.functionplugin.barcode;

import android.graphics.Bitmap;
import android.os.Handler;

import org.hfoss.posit.android.functionplugin.barcode.CameraManager;


public interface IDecoderActivity {
    public ViewfinderView getViewfinder();

    public Handler getHandler();

    public CameraManager getCameraManager();

    public void handleDecode(Result rawResult, Bitmap barcode);
}
