package edu.northwestern.cbits.purple_robot_manager.plugins;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.StartActivity;
import edu.northwestern.cbits.purple_robot_manager.models.Model;
import edu.northwestern.cbits.purple_robot_manager.models.ModelManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class AppDisplayPlugin extends OutputPlugin
{
	public String[] respondsTo()
	{
		String[] activeActions = { Probe.PROBE_READING, OutputPlugin.LOG_EVENT, AppDisplayPlugin.DISPLAY_MESSAGE };

		return activeActions;
	}

	public void processIntent(Intent intent)
	{
		Bundle extras = intent.getExtras();

		if (AppDisplayPlugin.DISPLAY_MESSAGE.equals(intent.getAction()))
		{
			Intent displayIntent = new Intent(StartActivity.UPDATE_MESSAGE);
			displayIntent.putExtra(StartActivity.DISPLAY_MESSAGE, extras.getString("MESSAGE"));

			LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this.getContext());
			manager.sendBroadcast(displayIntent);
		}
		else
		{
			Intent displayIntent = new Intent(StartActivity.UPDATE_DISPLAY);
			
			if (extras.containsKey("FROM_MODEL"))
			{
				Model m = ModelManager.getInstance(this.getContext()).fetchModelByTitle(this.getContext(), extras.getString("PROBE"));
				displayIntent.putExtra(StartActivity.DISPLAY_PROBE_NAME, m.name(this.getContext()));
			}
			else
				displayIntent.putExtra(StartActivity.DISPLAY_PROBE_NAME, extras.getString("PROBE"));

			displayIntent.putExtra(StartActivity.DISPLAY_PROBE_VALUE, extras);
			
			LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this.getContext());
			manager.sendBroadcast(displayIntent);
		}
	}
}
