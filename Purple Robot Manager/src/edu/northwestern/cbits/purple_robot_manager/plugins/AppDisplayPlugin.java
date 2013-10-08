package edu.northwestern.cbits.purple_robot_manager.plugins;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import edu.northwestern.cbits.purple_robot_manager.RobotContentProvider;
import edu.northwestern.cbits.purple_robot_manager.StartActivity;
import edu.northwestern.cbits.purple_robot_manager.models.Model;
import edu.northwestern.cbits.purple_robot_manager.models.ModelManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class AppDisplayPlugin extends OutputPlugin
{
	private long _lastUpdate = 0;
	
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
			ContentValues values = new ContentValues();

			Object ts = extras.get("TIMESTAMP");
			
			if (ts instanceof Long)
				values.put("recorded", ((Long) ts).longValue());
			else if (ts instanceof Double)
				values.put("recorded", ((Double) ts).longValue());
			
			values.put("source", extras.getString("PROBE"));

			if (extras.containsKey("FROM_MODEL"))
			{
				Model m = ModelManager.getInstance(this.getContext()).fetchModelByTitle(this.getContext(), extras.getString("PROBE"));
				values.put("source", m.name(this.getContext()));
			}
			
			try 
			{
				JSONObject json = OutputPlugin.jsonForBundle(extras);
				values.put("value",  json.toString());
			}
			catch (JSONException e) 
			{
				e.printStackTrace();
			}
			
			String where = "source = ?";
			String[] whereArgs = { values.getAsString("source") };
			
			this.getContext().getContentResolver().update(RobotContentProvider.RECENT_PROBE_VALUES, values, where, whereArgs);
			
			long now = System.currentTimeMillis();
			
			if (now - this._lastUpdate > 5000)
			{
				this.getContext().getContentResolver().notifyChange(RobotContentProvider.RECENT_PROBE_VALUES, null);
				
				this._lastUpdate = now;
			}
		}
	}
}
