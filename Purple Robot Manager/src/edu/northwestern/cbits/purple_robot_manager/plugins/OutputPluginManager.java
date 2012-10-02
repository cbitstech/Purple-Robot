package edu.northwestern.cbits.purple_robot_manager.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OutputPluginManager extends BroadcastReceiver
{
	public void onReceive(Context context, Intent intent)
	{
		for (Class<OutputPlugin> pluginClass : OutputPlugin.availablePluginClasses())
		{
			try
			{
				OutputPlugin plugin = pluginClass.newInstance();
				plugin.setContext(context);

				plugin.process(intent);
			}
			catch (InstantiationException e)
			{
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}
		}
	}
}
