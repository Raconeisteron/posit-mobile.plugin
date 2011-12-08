package org.hfoss.posit.android.experimental.functionplugins.sms;

import java.util.ArrayList;

import org.hfoss.posit.android.experimental.api.service.SmsService;

import android.content.Context;
import android.content.Intent;

public class SmsTransmitter {
	public static final String TAG = "SmsTransmitter";

	private static final String SENT = "SMS_SENT";

	private ArrayList<String> mMessages;
	private ArrayList<String> mPhoneNumbers;
	private Context mContext;

	public SmsTransmitter(Context context) {
		mMessages = new ArrayList<String>();
		mPhoneNumbers = new ArrayList<String>();
		mContext = context;
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

	public void sendMessages() {
		Intent smsService = new Intent(mContext, SmsService.class);
		smsService.putExtra("messages", mMessages);
		smsService.putExtra("phonenumbers", mPhoneNumbers);
		mContext.startService(smsService);
	}
}
