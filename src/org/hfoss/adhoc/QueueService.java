package org.hfoss.adhoc;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class QueueService extends Service {

	protected static final String TAG = "QueueService";
	protected boolean stopped;

	@Override
	public void onCreate() {
		Thread th = new Thread() {

			@Override
			public void run() {
				Looper.prepare();
				while (!stopped) {
					try {
						AdhocData<AdhocFind> adhocData = Queues.inputQueue.take();
						new SaveTask().execute(adhocData.getMessage());
					} catch (InterruptedException e) {
						Log.e(TAG, "Cannot read from the queue");
					}
				}
			}
		};

		th.start();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	private class SaveTask extends AsyncTask<AdhocFind, Void, Void> {

		@Override
		protected Void doInBackground(AdhocFind... finds) {
			//finds[0].saveToDB();
			return null;

		}

	}
}
