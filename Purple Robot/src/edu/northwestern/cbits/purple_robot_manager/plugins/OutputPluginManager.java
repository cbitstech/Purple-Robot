package edu.northwestern.cbits.purple_robot_manager.plugins;

import java.util.HashMap;
import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OutputPluginManager extends BroadcastReceiver
{
	public static OutputPluginManager sharedInstance = new OutputPluginManager();

	private Map<Class<OutputPlugin>, OutputPlugin> _plugins = new HashMap<Class<OutputPlugin>, OutputPlugin>();

	public OutputPlugin pluginForClass(Context context, Class<?> c)
	{
		OutputPlugin plugin = this._plugins.get(c);
		
		if (plugin != null)
			return plugin;
		
		this.onReceive(context, null);
		
		return this._plugins.get(c);
	}

	public void onReceive(Context context, Intent intent)
	{
		for (Class<OutputPlugin> pluginClass : OutputPlugin.availablePluginClasses())
		{
			try
			{
				OutputPlugin plugin = this._plugins.get(pluginClass);

				if (plugin == null)
				{
					plugin = pluginClass.newInstance();
					this._plugins.put(pluginClass, plugin);
				}

				plugin.setContext(context);

				if (intent != null)
					plugin.process(intent);
			}
			catch (InstantiationException e)
			{
				LogManager.getInstance(context).logException(e);
			}
			catch (IllegalAccessException e)
			{
				LogManager.getInstance(context).logException(e);
			}
		}
	}
}
