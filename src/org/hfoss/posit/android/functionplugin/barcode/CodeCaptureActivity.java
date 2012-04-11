/*
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.activity.FindActivity;
import org.hfoss.posit.android.api.plugin.AddFindPluginCallback;
import org.hfoss.posit.android.api.plugin.ListFindPluginCallback;
import org.hfoss.posit.android.R;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ParseException;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 *  This class starts up once "Scan Barcode" on the Add Finds menu is clicked. It opens the ViewFinder, extends the
 *  DecoderActivity that deciphers the barcode, and starts the FindActivity to allow saving the Find
 *
 */
public class CodeCaptureActivity extends DecoderActivity implements AddFindPluginCallback, 
ListFindPluginCallback{
	
    private static final String TAG = CodeCaptureActivity.class.getSimpleName();
    private static final Set<ResultMetadataType> DISPLAYABLE_METADATA_TYPES =
        EnumSet.of(ResultMetadataType.ISSUE_NUMBER,
                   ResultMetadataType.SUGGESTED_PRICE,
                   ResultMetadataType.ERROR_CORRECTION_LEVEL,
                   ResultMetadataType.POSSIBLE_COUNTRY);

    private TextView statusView = null;
    private View resultView = null;
    private boolean inScanMode = false;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.capture);
        Log.v(TAG, "onCreate()");

        resultView = findViewById(R.id.result_view);
        statusView = (TextView) findViewById(R.id.status_view);
        
        inScanMode = false;
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause()");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (inScanMode) finish();
            else onResume();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void handleDecode(Result rawResult, Bitmap barcode) {
        drawResultPoints(barcode, rawResult);
        
        ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);
        handleDecodeInternally(rawResult, resultHandler, barcode);
    }

    protected void showScanner() {
        inScanMode = true;
        resultView.setVisibility(View.GONE);
        statusView.setText(R.string.msg_default_status);
        statusView.setVisibility(View.VISIBLE);
        viewfinderView.setVisibility(View.VISIBLE);
    }

    protected void showResults() {
        inScanMode = false;
        statusView.setVisibility(View.GONE);
        viewfinderView.setVisibility(View.GONE);
        resultView.setVisibility(View.VISIBLE);
    }
    
    /**
     *  This method handles the decode that gets the text in the bar-code and creates a bundle for the FindActivity
     *
     */
    private void handleDecodeInternally(Result rawResult, ResultHandler resultHandler, Bitmap barcode) {
        onPause();
        showResults();
        
		CharSequence text = resultHandler.getDisplayContents();
		
		String textValues = text.toString();
		String[] values = textValues.split("\\*");
		if(!values[0].equals("POSITcode"))
		{
			Toast toast = Toast.makeText(this, "Invalid barcode", Toast.LENGTH_LONG);
			toast.show();
			finish();
		}

		
		//A bundle is created with the text values of the Find decoded from the barcode. FindActivity is then called to handle
		Bundle b = new Bundle();
		Intent i = new Intent(this,FindActivity.class);
		i.setAction("BARCODE");
		i.putExtra("message", text.toString());
		startActivity(i);
		
		finish();
        
    }

	@Override
	public void menuItemSelectedCallback(Context context, Find find, View view,
			Intent intent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onActivityResultCallback(Context context, Find find, View view,
			Intent intent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void displayFindInViewCallback(Context context, Find find, View view) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterSaveCallback(Context context, Find find, View view,
			boolean isSaved) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void finishCallback(Context context, Find find, View view) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void listFindCallback(Context context, Find find, View view) {
		// TODO Auto-generated method stub
		
	}
}
