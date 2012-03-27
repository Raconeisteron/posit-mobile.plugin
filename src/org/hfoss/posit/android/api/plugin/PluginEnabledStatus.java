package org.hfoss.posit.android.api.plugin;

/**
 * 
 * Used to store the enabled status of a plugin. The plugin is identified by 
 * the pluginName field and its enabled status is identified by the enabled 
 * field. 
 *
 */
public class PluginEnabledStatus {
	
	/**
	 * A unique name associated with a plugin
	 */
	protected String pluginName;
	
	protected boolean enabled;
	
	public PluginEnabledStatus(String pluginName, boolean enabled)
	{
		this.pluginName = pluginName;
		this.enabled = enabled;
	}
	
	public String getPluginName()
	{
		return this.pluginName;
	}
	
	public boolean getEnabled()
	{
		return this.enabled;
	}
	
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PLUGIN_NAME=").append(this.pluginName);
		sb.append(", ").append("PLUGIN_ENABLED=").append(this.enabled);
		return sb.toString();
	}

}
