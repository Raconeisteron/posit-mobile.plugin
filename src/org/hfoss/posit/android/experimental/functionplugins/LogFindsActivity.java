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
		super.onResume();
		List<? extends Find> finds = this.getHelper().getAllFinds();
		Toast.makeText(this, "Saving Finds to Log File", Toast.LENGTH_LONG).show();
		if (logFinds(finds)) {
			finish();
			Toast.makeText(
					this,
					"Finds saved to SD Card: " + DEFAULT_LOG_DIRECTORY + "/"
							+ DEFAULT_LOG_FILE, Toast.LENGTH_LONG).show();
		} else {
			finish();
			Toast.makeText(
					this,
					"Error while writing to file: " + DEFAULT_LOG_DIRECTORY + "/"
							+ DEFAULT_LOG_FILE, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Appends Finds (as strings) to a text file on the SD card.
	 * 
	 * @param finds, a list of Find records	 * 
	 * @return True if Finds were written successfully, False otherwise.
	 */
	protected boolean logFinds(List<? extends Find> finds) {
		try {
			File dir = new File(Environment.getExternalStorageDirectory()
					+ "/" + DEFAULT_LOG_DIRECTORY);
			if (!dir.exists()) {
				if (dir.mkdir()) {
					Log.i(TAG, "Created directory " + dir);
				}
			}
			if (dir.canWrite()) {
				Log.i(TAG, dir + " is writeable");
			}
            File file = new File(Environment.getExternalStorageDirectory()
                    + "/" + DEFAULT_LOG_DIRECTORY 
                    + "/"
                    + DEFAULT_LOG_FILE);
            if (!file.exists()) {
            	if (file.createNewFile()) 
            		Log.i(TAG, "Created file " + file);
            }
            
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
			return true;
		} catch (IOException e) {
			Log.e(TAG, "IO Exception writing to Log " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

}
