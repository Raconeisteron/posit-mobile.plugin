/*
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hfoss.posit.android.functionplugin.barcode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

/**
 * 
 * This class assumes the current context and displays the bitmap created from encoding the bar-code
 *
 */
public class BitMapView extends View {
Bitmap mBitmap = null;

public BitMapView(Context context, Bitmap bm) {
super(context);
mBitmap = bm;
}

@Override
protected void onDraw(Canvas canvas) {
// called when view is drawn
Paint paint = new Paint();
paint.setFilterBitmap(true);
// The image will be scaled so it will fill the width, and the
// height will preserve the image’s aspect ration
double aspectRatio = ((double) mBitmap.getWidth()) / mBitmap.getHeight();
Rect dest = new Rect(0, 0, this.getWidth(),(int) (this.getHeight() / aspectRatio));
canvas.drawBitmap(mBitmap, null, dest, paint);
}
}
