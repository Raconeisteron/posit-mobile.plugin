package org.hfoss.posit.android.experimental.functionplugins.sms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.hfoss.posit.android.experimental.R;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SmsActivity extends Activity {

	private static final String TAG = "SmsActivity";
	private static final char[] RESERVED_CHARS = { '\\', ',' };
	private static final char ESCAPE_CHAR = '\\';

	private EditText mEditPhoneNum;
	private EditText mEditPrefix;
	private Button mSendButton;
	private List<Entry<String, Object>> mEntries;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send_sms);
		mEditPhoneNum = (EditText) findViewById(R.id.phoneEditText);
		mEditPrefix = (EditText) findViewById(R.id.messagePrefixEditText);
		mSendButton = (Button) findViewById(R.id.sendButton);
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				SmsTransmitter transmitter = new SmsTransmitter(v.getContext());
				transmitter.addMessage(getMessage(mEntries), mEditPhoneNum
						.getText().toString());
				transmitter.sendMessages();
				finish();
			}
		});
		// Get default phone # and default message prefix
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		String defaultNum = preferences.getString(
				getString(R.string.defaultPhoneNumKey), "");
		String defaultPrefix = preferences.getString(
				getString(R.string.messagePrefixKey),
				getString(R.string.messagePrefixDefault));
		mEditPhoneNum.setText(defaultNum);
		mEditPrefix.setText(defaultPrefix);

		// Retrieve DbEntries of Find from intent
		ContentValues cv = getIntent().getParcelableExtra("DbEntries");
		mEntries = new ArrayList<Entry<String, Object>>(cv.valueSet());

		// Next sort the list by attribute (string) value. It is important that
		// attribute values be transmitted and interpreted in the same order.
		// (First comes a definition of the comparator used to compare list
		// entries:)
		class sortByKey implements Comparator<Entry<String, Object>> {
			public int compare(Entry<String, Object> e1,
					Entry<String, Object> e2) {
				return e1.getKey().compareTo(e2.getKey());
			}
		}
		Collections.sort(mEntries, new sortByKey());
	}

	/**
	 * Produces the string that should be used to transmit a Find from a list of
	 * it's entries.
	 * 
	 * @param entries
	 *            A list of attribute value pairs that make up the contents of a
	 *            find
	 * @return A comma-seperated message containing the encodings of each value.
	 */
	private String getMessage(List<Entry<String, Object>> entries) {
		StringBuilder builder = new StringBuilder();
		// Add attribute values
		for (Entry<String, Object> entry : entries) {
			if (builder.length() > 0)
				builder.append(",");
			builder.append(getEncoding(entry.getValue()));
		}
		// Add message prefix
		builder.insert(0, mEditPrefix.getText().toString());
		return builder.toString();
	}

	/**
	 * Takes a particular object and outputs its String encoding for SMS. Simply
	 * calling toString() on the object is insufficient, since such a String may
	 * contain reserved characters we will later use to parse the message. We
	 * must escape these characters. It may also be desired to compress some
	 * objects, or write specific encoding mechanisms for objects that are not
	 * entirely encapsulated by toString(). Such methods should be called from
	 * here.
	 * 
	 * @param val
	 *            The object that needs to be encoded.
	 * @return The encoding of the object.
	 */
	private String getEncoding(Object val) {
		// First use toString(). Special cases where toString() is not
		// appropriate should be specified here.
		String strenc = val.toString();
		// Escape reserved characters
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < strenc.length(); i++) {
			char c = strenc.charAt(i);
			Boolean reserved = false;
			for (char d : RESERVED_CHARS) {
				if (c == d) {
					reserved = true;
					break;
				}
			}
			// The escape character itself must also be escaped
			if (c == ESCAPE_CHAR)
				reserved = true;
			if (reserved) {
				builder.append("\\" + c);
			} else {
				builder.append(c);
			}
		}
		return builder.toString();
	}
}
