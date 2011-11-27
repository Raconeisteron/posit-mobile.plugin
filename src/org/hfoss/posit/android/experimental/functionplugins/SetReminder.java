package org.hfoss.posit.android.experimental.functionplugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.database.DbManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

public class SetReminder extends OrmLiteBaseActivity<DbManager> {
	
	private static final String TAG = "SetReminder";
	
	// Dialog IDs
	private static final int DATE_PICKER_DIALOG_ID = 0;
	private static final int ADDRESS_PICKER_DIALOG_ID = 1;
	private static final int ADDRESS_ENTER_DIALOG_ID = 2;
	private static final int ADDRESS_CONFIRM_DIALOG_ID = 3;
	
	// Dialogs
	private DatePickerDialog datePickerDialog;
	private AlertDialog addrPickerDialog;
	private AlertDialog addrEnterDialog;
	private AlertDialog addrConfirmDialog;
	private ProgressDialog progressDialog;
	
	// Set which dialog we are currently at
	private int currentDialog;
	// Back Key counter
	private int backKeyCounter = 0;
	
	// Variables passed between intents 
	private String date, year, month, day;
	private Double currentLongitude, currentLatitude;
	private Double findsLongitude, findsLatitude;
	
	// Determine if the Set button on Date Picker Dialog is pressed
	private boolean dateSetPressed;
	
	// EditText filed in Address Enter Dialog
	private EditText addressET;
	// addressURL because the text from addressET can be only retrieved once
	private String addressURL;

	// A list of addressed received
	private JSONArray addressArray;
	// private ArrayList<Address> addresses; --- used with Geocoder
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.blank_screen);
		
		dateSetPressed = false;
		
		// Initialize variables
		Bundle bundle = getIntent().getExtras();
		date = bundle.getString("Date");
		
		year = date.substring(0, 4);
		month = date.substring(5, 7);
		day = date.substring(8, 10);
		
		currentLongitude = bundle.getDouble("CurrentLongitude");
		currentLatitude = bundle.getDouble("CurrentLatitude");
		findsLongitude = bundle.getDouble("FindsLongitude");
		findsLatitude = bundle.getDouble("FindsLatitude");
		
		showDatePickerDialog();	
	}
	
	// Create and show Date Picker Dialog
	private void showDatePickerDialog() {
		dateSetPressed = false;
		currentDialog = DATE_PICKER_DIALOG_ID;
        int mYear = Integer.parseInt(year);
        int mMonth = Integer.parseInt(month) - 1;
        int mDay = Integer.parseInt(day);
        datePickerDialog = new myDatePickerDialog(this, mDateSetListener,
    			mYear, mMonth, mDay);
        datePickerDialog.setTitle("Step 1: Choose a date");
        datePickerDialog.setOnDismissListener(mDateDismissListener);
        datePickerDialog.setOnKeyListener(mBackKeyListener);
        datePickerDialog.show();
	}
	
	// Create and show Address Picker Dialog
	private void showAddrPickerDialog() {
		currentDialog = ADDRESS_PICKER_DIALOG_ID;
		final CharSequence[] items = {"Use Current Location", "Use Find's Location", "Enter Location Name / Landmark Address "};
		AlertDialog.Builder addrPickerBuilder = new AlertDialog.Builder(this);
		addrPickerBuilder.setTitle("Step 2: Choose an address");
		addrPickerBuilder.setItems(items, mAddrPickerOnClickListener);
		addrPickerBuilder.setOnKeyListener(mBackKeyListener);
		addrPickerBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				setResult(RESULT_OK);
	    		finish();
			}
		});
		addrPickerDialog = addrPickerBuilder.create();
		addrPickerDialog.show();
	}
	
	// Create and show Address Enter Dialog
	private void showAddrEnterDialog() {
		currentDialog = ADDRESS_ENTER_DIALOG_ID;
		AlertDialog.Builder addrEnterBuilder = new AlertDialog.Builder(this);
		addrEnterBuilder.setTitle("Step 3: Enter Location Name / Address");
		addressET = new EditText(this);
		addressET.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		addrEnterBuilder.setView(addressET);
		addrEnterBuilder.setPositiveButton("Search", mAddrEnterOnClickListner);
		addrEnterBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				setResult(RESULT_OK);
	    		finish();
			}
		});
		addrEnterBuilder.setOnKeyListener(mBackKeyListener);
		addrEnterDialog = addrEnterBuilder.create();
		addrEnterDialog.show();
	}
	
	// create and how Address Confirm Dialog
	private void showAddrConfirmDialog() {
		currentDialog = ADDRESS_CONFIRM_DIALOG_ID;
		AlertDialog.Builder addrConfirmBuilder = new AlertDialog.Builder(this);
		addrConfirmBuilder.setTitle("Step 4: Did you mean...");
		ArrayList<String> possibleAddr = new ArrayList<String>();
		for (int i = 0; i < addressArray.length(); i++) {
			try {
				String receivedAddr = addressArray.getJSONObject(i).getString("formatted_address").replace(", ", ",\n");
				possibleAddr.add(receivedAddr);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
//			for (int i = 0; i < addresses.size(); i++) {
//			returnedAddr = addresses.get(i);
//			returnedAddrStr = new StringBuilder("");
//			for (int j = 0; j < returnedAddr.getMaxAddressLineIndex(); j++) {
//				if (j == returnedAddr.getMaxAddressLineIndex() - 1 )
//					returnedAddrStr.append(returnedAddr.getAddressLine(j));
//				else
//					returnedAddrStr.append(returnedAddr.getAddressLine(j)).append("\n");
//			}
//			if (returnedAddrStr.toString() != null) {
//				possibleAddr.add(returnedAddrStr.toString());
//			}
//		}
		final CharSequence[] possibleAddrChar = possibleAddr.toArray(new CharSequence[possibleAddr.size()]);
		addrConfirmBuilder.setItems(possibleAddrChar, mAddrConfirmOnClickListener);
		addrConfirmBuilder.setOnKeyListener(mBackKeyListener);
		addrConfirmBuilder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				showAddrEnterDialog();
				Toast.makeText(getApplicationContext(), "To get better results, please type a more specific name or address "
						+ "with CITY NAME included.", Toast.LENGTH_LONG).show();
			}
		});
		addrConfirmBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				setResult(RESULT_OK);
	    		finish();
			}
		});
		addrConfirmDialog = addrConfirmBuilder.create();
		addrConfirmDialog.show();
	}

	// Listen to when the Back key is pressed
	private DialogInterface.OnKeyListener mBackKeyListener =
			new DialogInterface.OnKeyListener() {
			    public boolean onKey (DialogInterface dialog, int keyCode, KeyEvent event) {
			        if (keyCode == KeyEvent.KEYCODE_BACK) {
			        	backKeyCounter++;
			        	if (backKeyCounter % 2 == 0) {
			        		if (currentDialog == DATE_PICKER_DIALOG_ID) {
				        		datePickerDialog.dismiss();
					            setResult(RESULT_OK);
						    	finish();
				        	} else if (currentDialog == ADDRESS_PICKER_DIALOG_ID) {
				        		addrPickerDialog.dismiss();
				        		showDatePickerDialog();
				        	} else if (currentDialog == ADDRESS_ENTER_DIALOG_ID) {
				        		addrEnterDialog.dismiss();
				        		showAddrPickerDialog();
				        	}  else if (currentDialog == ADDRESS_CONFIRM_DIALOG_ID) {
				        		addrConfirmDialog.dismiss();
				        		Toast.makeText(getApplicationContext(), "To get better results, please type a more specific name or address "
										+ "with CITY NAME included.", Toast.LENGTH_LONG).show();
				        		showAddrEnterDialog();
				        	}
			        	}
			        	return true;
				    } else {
				    	return false;
				    }
			    }
			};
	
	// Customized Date Picker Dialog
	private class myDatePickerDialog extends DatePickerDialog {
		
		private myDatePickerDialog(Context context,	OnDateSetListener callBack,
				int year, int monthOfYear, int dayOfMonth) {
			super(context, callBack, year, monthOfYear, dayOfMonth);
		}
		
		public void onDateChanged(DatePicker view, int year,
	            int month, int day) {
			setTitle("Step 1: Choose a date");
	    }

	}
	
	// Listen to when the Set button on Date Picker Dialog is pressed
	private DatePickerDialog.OnDateSetListener mDateSetListener =
			new DatePickerDialog.OnDateSetListener() {
			
			    public void onDateSet(DatePicker view, int year, 
			                          int monthOfYear, int dayOfMonth) {
			    	String monthStr = (monthOfYear + 1 < 10) ?
			    			"0" + Integer.toString(monthOfYear + 1) : Integer.toString(monthOfYear + 1);
					String dayStr = (dayOfMonth < 10) ?
							"0" + Integer.toString(dayOfMonth) : Integer.toString(dayOfMonth);
					date = Integer.toString(year) + "/" + monthStr + "/" + dayStr;
					
					dateSetPressed = true;
			    }
			};
	
	// Listen to when Date Picker Dialog is dismissed
	private DatePickerDialog.OnDismissListener mDateDismissListener =
			new DatePickerDialog.OnDismissListener() {
			    public void onDismiss(DialogInterface dialog) {
			    	if (dateSetPressed) {
			    		showAddrPickerDialog();
			    	}
			    	else {
			    		setResult(RESULT_OK);
			    		finish();
			    	}	
			    }
			};
	
	// Listen to when an item on Address Picker Dialog is pressed
	private DialogInterface.OnClickListener mAddrPickerOnClickListener =
			new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	Bundle bundle = new Bundle();
			    	Intent newIntent = new Intent();
			    	if (item == 0) {
				    	bundle.putString("Date", date);
				    	bundle.putDouble("Longitude", currentLongitude);
						bundle.putDouble("Latitude", currentLatitude);
						newIntent.putExtras(bundle);
				    	setResult(RESULT_OK, newIntent);
				    	finish();
			    	} else if (item == 1) {
			    		bundle.putString("Date", date);
				    	bundle.putDouble("Longitude", findsLongitude);
						bundle.putDouble("Latitude", findsLatitude);
						newIntent.putExtras(bundle);
				    	setResult(RESULT_OK, newIntent);
				    	finish();
			    	} else if (item == 2) {
			    		showAddrEnterDialog();
			    	}
			    }
			};
	
	// Listen to when the Search button on Address Enter Dialog is pressed
	private DialogInterface.OnClickListener mAddrEnterOnClickListner =
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {				
					addressURL = addressET.getText().toString().replaceAll(" ", "%20");
					
					if (addressURL.length() == 0) {
						Toast.makeText(getApplicationContext(), "Please type a location name / address.",
								Toast.LENGTH_LONG).show();
						showAddrEnterDialog();
					} else {
						progressDialog = ProgressDialog.show(SetReminder.this, "", 
		                        "Retrieving Location Data...", true, false);
						new Thread (new Runnable() {
							public void run() {
								retrieveLocation();
								Message msg = new Message();
								msg.obj = "DISMISS PROGRESS DIALOG";
								handler.sendMessage(msg);
								}
							}).start();
					}
				}
				    
			};
	
	// Listen to when an item on Confirm Address Dialog is pressed
	private DialogInterface.OnClickListener mAddrConfirmOnClickListener =
			new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	Bundle bundle = new Bundle();
			    	Intent newIntent = new Intent();
					bundle.putString("Date", date);
					Double lng = new Double(0);
					Double lat = new Double(0);
					try {
						lng = addressArray.getJSONObject(item)
								.getJSONObject("geometry").getJSONObject("location")
								.getDouble("lng");
						lat = addressArray.getJSONObject(item)
				    			.getJSONObject("geometry").getJSONObject("location")
								.getDouble("lat");
					} catch (JSONException e) {
						e.printStackTrace();
					}
			    	bundle.putDouble("Longitude", lng);
					bundle.putDouble("Latitude", lat);
					newIntent.putExtras(bundle);
			    	setResult(RESULT_OK, newIntent);
			    	finish();
			    }
			};
	
	// Handler to dismiss the progress dialog
    private Handler handler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
    			if (msg.obj.equals("DISMISS PROGRESS DIALOG")) {
    				progressDialog.dismiss();
    			} else if (msg.obj.equals("SHOW ADDRESS ENTER DIALOG - FAILED")) {
    				Toast.makeText(getApplicationContext(), "Location retrieval " +
    						"failed. Please try again", Toast.LENGTH_LONG).show();
    				showAddrEnterDialog();
    			} else if (msg.obj.equals("SHOW ADDRESS ENTER DIALOG - NO RESULTS")) {
    				Toast.makeText(getApplicationContext(), "No results returned. " +
    						"Please type a more specific name or address with"
    						+ " CITY NAME included.", Toast.LENGTH_LONG).show();
    				showAddrEnterDialog();
    			} else if (msg.obj.equals("SHOW ADDRESS CONFIRM DIALOG")) {
    				showAddrConfirmDialog();
    			}
    		}
        };

	// Retrieve location
	private void retrieveLocation() {
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet("http://maps.google."
				+ "com/maps/api/geocode/json?address=" + addressURL
				+ "&sensor=false");
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {
				Log.e(TAG, "Failed to download file");
				Toast.makeText(getApplicationContext(), "Location retrieval failed. Please try again",
						Toast.LENGTH_LONG).show();
				Message msg = new Message();
				msg.obj = "SHOW ADDRESS ENTER DIALOG";
				handler.sendMessage(msg);
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject = new JSONObject(builder.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		try {
			addressArray = (JSONArray) jsonObject.get("results");
		} catch (JSONException e) {
			e.printStackTrace();
		}

//			Geocoder geoCoder = new Geocoder(getApplicationContext(), Locale.getDefault());
//            try {
//				addresses = (ArrayList<Address>) geoCoder.getFromLocationName(addressText, 5);
//				int maxTime = 1;
//				while (addresses.size() == 0 && maxTime < 20) {
//					addresses = (ArrayList<Address>) geoCoder.getFromLocationName(addressText, 5);
//					maxTime++;
//				}
//				if (addresses.size() == 1) {
//					Bundle bundle = new Bundle();
//			    	Intent newIntent = new Intent();
//					bundle.putString("Date", date);
//			    	bundle.putDouble("Longitude", addresses.get(0).getLongitude());
//					bundle.putDouble("Latitude", addresses.get(0).getLatitude());
//					newIntent.putExtras(bundle);
//			    	setResult(RESULT_OK, newIntent);
//			    	finish();
//				} else if (addresses.size() == 0) {
//					Toast.makeText(getApplicationContext(), "Please try again.", Toast.LENGTH_SHORT).show();
//				} else {
//					Log.i(TAG, "Address > 1");
//					showDialog(CONFIRM_ADDRESS_DIALOG_ID);
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		if (addressArray == null) {
			Message msg = new Message();
			msg.obj = "SHOW ADDRESS ENTER DIALOG - FAILED";
			handler.sendMessage(msg);
		} else if (addressArray.length() == 0) {
			Message msg = new Message();
			msg.obj = "SHOW ADDRESS ENTER DIALOG - NO RESULTS";
			handler.sendMessage(msg);
		} else if (addressArray.length() == 1) {
			Bundle bundle = new Bundle();
	    	Intent newIntent = new Intent();
			bundle.putString("Date", date);
			Double lng = new Double(0);
			Double lat = new Double(0);
			try {
				lng = addressArray.getJSONObject(0)
						.getJSONObject("geometry").getJSONObject("location")
						.getDouble("lng");
				lat = addressArray.getJSONObject(0)
		    			.getJSONObject("geometry").getJSONObject("location")
						.getDouble("lat");
			} catch (JSONException e) {
				e.printStackTrace();
			}
	    	bundle.putDouble("Longitude", lng);
			bundle.putDouble("Latitude", lat);
			newIntent.putExtras(bundle);
	    	setResult(RESULT_OK, newIntent);
	    	finish();
		} else {
			Message msg = new Message();
			msg.obj = "SHOW ADDRESS CONFIRM DIALOG";
			handler.sendMessage(msg);
		}
	}
}
