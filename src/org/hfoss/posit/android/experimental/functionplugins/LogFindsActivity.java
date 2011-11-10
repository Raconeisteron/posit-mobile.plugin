package org.hfoss.posit.android.experimental.functionplugins;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.database.DbManager;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

public class LogFindsActivity extends OrmLiteBaseActivity<DbManager> {
	
	public static final String TAG = "LogFindsActivity";
	private static final String DEFAULT_LOG_DIRECTORY = "oi";
	private static final String DEFAULT_LOG_FILE = "oi_log.txt";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		List<? extends Find> finds = this.getHelper().getAllFinds();
		Toast.makeText(this, "Saving Finds to Log File", Toast.LENGTH_LONG).show();
		logMessages(finds);
		finish();
		Toast.makeText(this, "Finds saved to SD Card: " + DEFAULT_LOG_DIRECTORY +"/" + DEFAULT_LOG_FILE, Toast.LENGTH_LONG).show();
	}
	
	
    /**
     * Appends Finds (as strings) to a text file on the SD card.
     * 
     * @param sFileName
     * @param msg
     */
    protected void logMessages(List<? extends Find> finds) {
            try {
                    File file = new File(Environment.getExternalStorageDirectory()
                                    + "/" + DEFAULT_LOG_DIRECTORY + "/"
                                    + DEFAULT_LOG_FILE);

                    // FileWriter writer = new FileWriter(file);
                    PrintWriter writer = new PrintWriter(new BufferedWriter(
                                    new FileWriter(file, true)));

                    Iterator<? extends Find> it = finds.iterator();
                    while (it.hasNext()) {
                            Find find = it.next();
                            writer.println(new Date() + ": " + find);
                            Log.i(TAG, "Wrote to file: " + find);
                    }
                    writer.flush();
                    writer.close();
            } catch (IOException e) {
                    Log.e(TAG, "IO Exception writing to Log " + e.getMessage());
                    e.printStackTrace();
            }
    }

//    protected static void logMessage(String msg) {
//            try {
//                    File file = new File(Environment.getExternalStorageDirectory()
//                                    + "/" + DEFAULT_LOG_DIRECTORY + "/"
//                                    + DEFAULT_LOG_FILE);
//
//                    // FileWriter writer = new FileWriter(file);
//                    PrintWriter writer = new PrintWriter(new BufferedWriter(
//                                    new FileWriter(file, true)));
//                    writer.println(new Date() + ": " + msg);
//                    Log.i(TAG, "Wrote to file: " + msg);
//                    writer.flush();
//                    writer.close();
//            } catch (IOException e) {
//                    Log.e(TAG, "IO Exception writing to Log " + e.getMessage());
//                    e.printStackTrace();
//            }
//    }




	
	
	
	
}
