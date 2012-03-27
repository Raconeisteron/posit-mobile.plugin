package org.hfoss.posit.android.api.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hfoss.posit.android.R;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Activity for enabling and disabling function plugins. This Activity is 
 * accessible from the "Plugin Manager" item in the settings menu.
 *
 */
public class FuncPluginManagerActivity extends ListActivity {

	PluginStatusListAdapter listAdapter;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    this.listAdapter = new PluginStatusListAdapter(FuncPluginManagerActivity.this);
	    setListAdapter(this.listAdapter);
	}

	/**
	 * Adapter for populating this activities list with the appropriate plugin info.
	 *
	 */
	private class PluginStatusListAdapter extends BaseAdapter
	{
		private List<PluginEnabledStatus> pluginEnabledStatusList;
		private Context context;
		
		public PluginStatusListAdapter(Context context)
		{
			this.context = context;
			
			this.pluginEnabledStatusList = new ArrayList<PluginEnabledStatus>();
			
			//Iterate through the plugin info in the shared preferences 
			//FUNC_PLUGIN_PREFS and store it in pluginEnabledStatusList.
			SharedPreferences funcPluginPrefs = context.getSharedPreferences(FindPluginManager.FUNC_PLUGIN_PREFS, MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
			Map<String, ?> funcPluginPrefsMap = funcPluginPrefs.getAll();
			for(String curFuncPluginName : funcPluginPrefsMap.keySet()){
				boolean curFuncPluginIsEnabled = funcPluginPrefs.getBoolean(curFuncPluginName, false); 
				this.pluginEnabledStatusList.add( new PluginEnabledStatus(curFuncPluginName, curFuncPluginIsEnabled));
			}
			
			//TODO: Sort list
		}
		
		public int getCount() {
			return this.pluginEnabledStatusList.size();
		}

		public Object getItem(int position) {
			return this.pluginEnabledStatusList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View pluginStatusRowView;
			
			if (convertView == null)
			{
				LayoutInflater li = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				pluginStatusRowView = li.inflate(R.layout.plugin_status, parent, false);
			}
			else
			{
				pluginStatusRowView = convertView;
			}
			
			TextView pluginNameView = (TextView)pluginStatusRowView.findViewById(R.id.plugin_status_name);
            CheckBox pluginEnabledView = (CheckBox)pluginStatusRowView.findViewById(R.id.plugin_status_enabled);

            //Set handler for enabling/disabling a plugin.
            pluginEnabledView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		
            	public void onCheckedChanged(CompoundButton button, boolean checked) {
            		LinearLayout pluginStatusRow = (LinearLayout) button.getParent();
            		TextView pluginNameView = (TextView)pluginStatusRow.findViewById(R.id.plugin_status_name);
            		String pluginName = (String) pluginNameView.getText();
					
					FindPluginManager.getInstance().UpdateFuncPluginEnabledState(pluginName, checked);
				} 
        
            });
            
            pluginNameView.setText(this.pluginEnabledStatusList.get(position).getPluginName());
            pluginEnabledView.setChecked(this.pluginEnabledStatusList.get(position).getEnabled());
			
			return pluginStatusRowView;
		}

	}
}