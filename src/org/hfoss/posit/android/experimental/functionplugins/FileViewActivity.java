package org.hfoss.posit.android.experimental.functionplugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.hfoss.posit.android.experimental.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class FileViewActivity extends Activity {

	public static final String TAG = "FileViewActivity";
	private static final String HOME_DIRECTORY = "log";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file_view);

		// Create /sdcard/HOME_DIRECTORY if it doesn't exist already
		File dir = new File(Environment.getExternalStorageDirectory() + "/"
				+ HOME_DIRECTORY);
		if (!dir.exists()) {
			if (dir.mkdir()) {
				Log.i(TAG, "Created directory " + dir);
			}
		}
		// Start file picker activity using /sdcard/HOME_DIRECTORY as the home
		// directory
		Intent intent = new Intent();
		intent.putExtra("home", Environment.getExternalStorageDirectory() + "/"
				+ HOME_DIRECTORY);
		intent.setClass(this, FilePickerActivity.class);
		this.startActivityForResult(intent, FilePickerActivity.ACTION_CHOOSER);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case FilePickerActivity.ACTION_CHOOSER:
			if (resultCode == FilePickerActivity.RESULT_OK) {
				// Result good, display file
				String filename = data.getStringExtra(Intent.ACTION_CHOOSER);
				File file = new File(Environment.getExternalStorageDirectory()
						+ "/" + HOME_DIRECTORY, filename);

				// Read text from file
				StringBuilder text = new StringBuilder();

				try {
					BufferedReader br = new BufferedReader(new FileReader(file));
					String line;

					while ((line = br.readLine()) != null) {
						text.append(line);
						text.append('\n');
					}
				} catch (IOException e) {
					Log.e(TAG, "IO Exception reading from file "
							+ e.getMessage());
					e.printStackTrace();
					Toast.makeText(this, "Error occurred reading from file",
							Toast.LENGTH_LONG).show();
					finish();
				}

				// Display text
				String test = text.toString();
				TextView tv = (TextView) findViewById(R.id.fileview);
				tv.setText(text);
			} else {
				// Result not good, do something about it
				Toast.makeText(this, "Error occurred in File Picker",
						Toast.LENGTH_LONG).show();
				finish();
			}
			break;
		default:
			// Shouldn't happen
			Log.e(TAG, "Request code on activity result not recognized");
			finish();
		}
	}
}
