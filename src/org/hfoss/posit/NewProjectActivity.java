package org.hfoss.posit;

import org.hfoss.posit.utilities.Utils;
import org.hfoss.posit.web.Communicator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class NewProjectActivity extends Activity implements OnClickListener{

	private Button mCreateProject;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_project);
		mCreateProject = (Button) this.findViewById(R.id.createProject);
		mCreateProject.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.createProject:
			String projectName = (((TextView) findViewById(R.id.projectName)).getText()).toString();
			String projectDescription = (((TextView) findViewById(R.id.projectDescription)).getText()).toString(); 
			if(projectName.equals("")){
				Utils.showToast(this, "Please enter a name for your project");
				break;
			}
			SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(this);
			String authkey = prefManager.getString("AUTHKEY",null);
			String server = prefManager.getString("SERVER_ADDRESS", null);
			TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			String imei = manager.getDeviceId();
			Communicator com = new Communicator(this);
			String response = com.createProject(server, projectName, projectDescription, imei, authkey);
				if(null!=Integer.getInteger(response))
					Utils.showToast(this, "Registration Successful");
				Utils.showToast(this, response);
			break;
		}
		
	}

}
