package edu.northwestern.cbits.purple_robot_manager.plugins;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import edu.northwestern.cbits.purple_robot_manager.StartActivity;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class AppDisplayPlugin extends OutputPlugin
{
	public String[] respondsTo()
	{
		String[] activeActions = { Probe.PROBE_READING };

		return activeActions;
	}

	public void processIntent(Intent intent)
	{
		JSONObject object = new JSONObject();

		try
		{
			object.put("intent_action", intent.getAction());

			Bundle extras = intent.getExtras();

			Intent displayIntent = new Intent(StartActivity.UPDATE_DISPLAY);
			displayIntent.putExtra(StartActivity.DISPLAY_PROBE_NAME, extras.getString("PROBE"));
			displayIntent.putExtra(StartActivity.DISPLAY_PROBE_VALUE, extras);

			LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this.getContext());

			manager.sendBroadcast(displayIntent);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
}
