package org.hfoss.posit.android.api.plugin;

import java.util.EventListener;

public interface ActiveFuncPluginChangeEventListener extends EventListener {
	/**
	 * Handler function that is triggered when a function plugin is enabled or disabled.
	 * 
	 * @param plugin Instance of the plugin that is being enabled/disabled
	 * @param enabled True if the plugin is being enabled and false if it is 
	 * being disabled.
	 */
	 public void handleActiveFuncPluginChangeEvent(FunctionPlugin plugin, boolean enabled);
}
