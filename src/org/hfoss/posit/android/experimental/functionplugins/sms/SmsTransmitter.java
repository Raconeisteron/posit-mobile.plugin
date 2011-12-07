package org.hfoss.posit.android.experimental.functionplugins.sms;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class SmsTransmitter {
	public static final String TAG = "SmsTransmitter";

	private static final String SENT = "SMS_SENT";

	private List<String> mMessages;
	private List<String> mPhoneNumbers;

	public SmsTransmitter() {
		mMessages = new LinkedList<String>();
		mPhoneNumbers = new LinkedList<String>();
	}

	/**
	 * Add a text message to be sent later.
	 * 
	 * @param text
	 *            The string contents of the SMS message
	 * @param phoneNumber
	 *            The phone number to which the message should be sent
	 */
	public void addMessage(String text, String phoneNumber) {
		mMessages.add(text);
		mPhoneNumbers.add(phoneNumber);
	}

	private void handleSentMessage(Context context, BroadcastReceiver receiver,
			int resultCode, Intent intent, String seq, String smsMsg) {
		switch (resultCode) {
		case Activity.RESULT_OK:
			Log.i(TAG, "Received OK, seq = " + seq + " msg:" + smsMsg);
			break;
		case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
			Log.e(TAG, "Received generic failure, seq =  " + seq + " msg:"
					+ smsMsg);
			break;
		case SmsManager.RESULT_ERROR_NO_SERVICE:
			Log.e(TAG, "Received no service error, seq =  " + seq + " msg:"
					+ smsMsg);
			break;
		case SmsManager.RESULT_ERROR_NULL_PDU:
			Log.e(TAG, "Received null PDU error, seq =  " + seq + " msg:"
					+ smsMsg);
			break;
		case SmsManager.RESULT_ERROR_RADIO_OFF:
			Log.e(TAG, "Received radio off error, seq =  " + seq + " msg:"
					+ smsMsg);
			break;
		}
	}

	public void transmitMessages(final Context context) {

		for (int i = 0; i < mMessages.size(); i++) {
			final String message = mMessages.get(i);
			String phonenum = mPhoneNumbers.get(i);
			final String seq = Integer.toString(i);
			PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
					new Intent(seq), 0);

			// Receiver for when the SMS is sent
			BroadcastReceiver sendReceiver = new BroadcastReceiver() {
				@Override
				public synchronized void onReceive(Context arg0, Intent arg1) {
					handleSentMessage(arg0, this, getResultCode(), arg1, seq, message); 
					context.unregisterReceiver(this);
				}
			};
			context.registerReceiver(sendReceiver, new IntentFilter(SENT + " "
					+ seq));
			// Actually send the message
			SmsManager sms = SmsManager.getDefault();
			sms.sendTextMessage(phonenum, null, message, sentPI, null);
		}
	}
}
