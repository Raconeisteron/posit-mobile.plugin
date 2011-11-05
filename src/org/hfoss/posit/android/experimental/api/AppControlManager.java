package org.hfoss.posit.android.experimental.api;



import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.plugin.acdivoca.AcdiVocaUser;
import org.hfoss.posit.android.experimental.plugin.acdivoca.AcdiVocaUser.UserType;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Class that manages user types/authentication and the visibility
 * of certain UI elements to different user types. Plugins can extend
 * and add methods to control this.
 *
 */
public class AppControlManager {
	
	
	public static final String TAG = "AppControlManager";
	private static AppControlManager sInstance = null;

	private static int sLoggedInUserTypeOrdinal;
	private static UserType sLoggedInUserType;
	
	
	
	public static AppControlManager getInstance(){
		assert(sInstance != null);
		
		return sInstance;
	}
	
	// ----------------   UserType Control Methods ------------------
	// Static utility methods used to control menus and buttons based on user type.
	
	public static void setUserType(int userTypeOrdinal) {
		sLoggedInUserTypeOrdinal = userTypeOrdinal;
	}
	
	/**
	 * Returns the type of the currently logged in user
	 */
	public static UserType getUserType() {
		if (sLoggedInUserTypeOrdinal == UserType.ADMIN.ordinal())
			return UserType.ADMIN;
		else
			return UserType.USER;
	}
	
	/**
	 * Returns the user type as an ordinal (int) values
	 */
	public static int getUserTypeOrdinal() {
		return sLoggedInUserTypeOrdinal;
	}
	
	
	public static boolean isAdminUser() {
		return sLoggedInUserTypeOrdinal == UserType.ADMIN.ordinal();
	}
	
	public static boolean isRegularUser() {
		return sLoggedInUserTypeOrdinal == UserType.USER.ordinal();
	}
	
	
}
