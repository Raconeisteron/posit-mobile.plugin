package org.hfoss.posit.android.experimental.functionplugins.sms;

import java.util.List;
import java.util.Map;

import org.hfoss.posit.android.experimental.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SmsActivity extends Activity {
	
	private static final String TAG = "SmsActivity";
	
	private List<Map.Entry<String, Object>> mEntries;
	private EditText mEditPhoneNum;
	private EditText mEditMessage;
	private Button mSendButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send_sms);
		mEditPhoneNum = (EditText) findViewById(R.id.phoneEditText);
		mEditMessage = (EditText) findViewById(R.id.smsEditText);
		mSendButton = (Button) findViewById(R.id.sendButton);
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				SmsTransmitter transmitter = new SmsTransmitter();
				transmitter.addMessage(mEditMessage.getText().toString(), mEditPhoneNum.getText().toString());
				transmitter.transmitMessages(getBaseContext());
				finish();
			}
		});
	}
}
