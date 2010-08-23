package org.hfoss.posit.android;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


/**
 * Lets the user adjust Tracker parameters. The tracker preferences are
 * loaded from an XML resource file.  Whenever the user changes a preference,
 * all Listeners that are registered with the PREFERENCES_NAME settings, will
 * be notified and can take action.  TrackerActivity is the only registered
 * listener.
 *  
 * @author rmorelli
 * @see http://code.google.com/p/mytracks/
 */
public class TrackerSettings extends PreferenceActivity {
	
	public static final String TAG = "PositTracker";
	
	  public static final String PREFERENCES_NAME = "TrackerSettings";

	  // Default settings -- some of these settable in shared preferences
	  public static final int DEFAULT_MIN_RECORDING_DISTANCE = 3; // meters, sp
	  public static final int DEFAULT_MIN_RECORDING_INTERVAL = 0; 
	  public static final int DEFAULT_MIN_REQUIRED_ACCURACY = 200; // Unused
	  public static final int DEFAULT_SWATH_WIDTH = 50; // sp
	  
	  public static final int IDLE = 0;
	  public static final int RUNNING = 1;
	  public static final int PAUSED = 2;  // Currently unused

	  public static final String TRACKER_STATE_PREFERENCE = "TrackerState";
	  public static final String POSIT_PROJECT_PREFERENCE = "PROJECT_ID";
	  
	  // These settable Tracker preferences have to be identical to String resources
	  public static final String SWATH_PREFERENCE = 
		  "swathWidth"; // @string/swath_width
	  public static final String MINIMUM_DISTANCE_PREFERENCE = 
		  "minDistance"; // @string/min_recording_distance

	  @Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			PreferenceManager.getDefaultSharedPreferences(this);

		    addPreferencesFromResource(R.xml.tracker_preferences);	
		}
}
