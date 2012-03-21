package org.hfoss.posit.android.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.service.SmsService;
import org.hfoss.posit.android.functionplugin.sms.ObjectCoder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * This class is for the easy sending of SMS messages. Currently, there are two
 * types of SMS messages that can be sent: An entire Find, or a simple String.
 * This class encapsulates the SMS sending protocol. If someone were to want to
 * make the protocol more robust (for instance, to allow the sending of partial
 * finds), they would likely want to do it here. For the purposes of my plugin,
 * I have kept it fairly simple.
 * 
 * @author Ryan McLeod
 * 
 * Note: Some modifications to the code have been made by Andrew Matsusaka
 */
public class SyncSms extends SyncMedium {
	public static final String TAG = "SyncSms";
	public static final String FIND_PREFIX = "~_";
	
	private ArrayList<String> mMessages;
	private ArrayList<String> mPhoneNumbers;
	private Context mContext;
	
	public SyncSms(Context context) {
		mMessages = new ArrayList<String>();
		mPhoneNumbers = new ArrayList<String>();
		mContext = context;
	}

	/**
	 * Add a Find to be transmitted via SMS later.
	 * 
	 * @param find
	 *            The Find object to be transmitted.
	 * @param phoneNumber
	 *            The phone number that the Find should be transmitted to
	 * @throws IllegalArgumentException
	 */
	public void addFind(Find find, String phoneNumber)
			throws IllegalArgumentException {
		addFind( find.getDbEntries(), phoneNumber );
	}
	
	public void addFind(Bundle bundle, String phoneNumber)
			throws IllegalArgumentException {
		String text = convertBundleToRaw( bundle );
		StringBuilder builder = new StringBuilder( text );
		builder.insert(0, FIND_PREFIX);
		addMessage( text, phoneNumber );
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

	/**
	 * Sends all previously added messages to their respective destinations.
	 * 
	 */
	public void sendFinds() {
		Intent smsService = new Intent(mContext, SmsService.class);
		smsService.putExtra("messages", mMessages);
		smsService.putExtra("phonenumbers", mPhoneNumbers);
		smsService.setAction(Intent.ACTION_SEND);
		mContext.startService(smsService);
	}
	
	/**
	 * This isn't used because SMS sends individual messages internally
	 * using the SmsService
	 */
	protected boolean sendFind(Find find){ return false; }

	@Override
	protected List<String> getFindsNeedingSync() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String retrieveRawFind(String guid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean postSendTasks() {
		// TODO Auto-generated method stub
		return false;
	}
}
