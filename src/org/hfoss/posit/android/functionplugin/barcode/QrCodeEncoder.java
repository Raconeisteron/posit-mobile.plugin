/*
 * Copyright 2008 ZXing authors
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * 
 * This class receives a string entry, converts it to a quick response code and provides the bitmap through the encodeAndSave() method
 *
 */
public class QrCodeEncoder{
    
	private String info;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int BLACK = 0xFF000000;
	
    public QrCodeEncoder(String info)
    {
    	this.info = info;
    }
    
    public Bitmap encodeAndSave()
    {
	    Charset charset = Charset.forName("ISO-8859-1");
        CharsetEncoder encoder = charset.newEncoder();
        byte[] b = null;
        try {
          // Convert a string to ISO-8859-1 bytes in a ByteBuffer
          ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(info));
          b = bbuf.array();
        } catch (CharacterCodingException e) {
        System.out.println(e.getMessage());
        }
        
        String data = "null";
        try {
            data = new String(b, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            System.out.println(e.getMessage());
        }

        // get a byte matrix for the data
        BitMatrix matrix = null;
        int h = 100;
        int w = 100;
        Writer writer = new QRCodeWriter();
        try {
            matrix = writer.encode(data,
            BarcodeFormat.QR_CODE, w, h);
        } catch (WriterException e) {
            System.out.println(e.getMessage());
        }
        
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
          int offset = y * width;
          for (int x = 0; x < width; x++) {
            pixels[offset + x] = matrix.get(x, y) ? BLACK : WHITE;
          }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        
        return bitmap;
    }
}
