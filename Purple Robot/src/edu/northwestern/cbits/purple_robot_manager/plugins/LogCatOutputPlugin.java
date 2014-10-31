package edu.northwestern.cbits.purple_robot_manager.plugins;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class LogCatOutputPlugin extends OutputPlugin
{
    public String[] respondsTo()
    {
        String[] activeActions =
        { OutputPlugin.OUTPUT_EVENT, Probe.PROBE_READING };

        return activeActions;
    }

    public void processIntent(Intent intent)
    {
        JSONObject object = new JSONObject();

        try
        {
            object.put("intent_action", intent.getAction());

            Bundle extras = intent.getExtras();

            object.put("extras", OutputPlugin.jsonForBundle(extras));

            Log.e("PRM", "JSON OBJECT: " + object.getJSONObject("extras").getString("NAME"));
        }
        catch (JSONException e)
        {
            LogManager.getInstance(this.getContext()).logException(e);
        }
    }

}
