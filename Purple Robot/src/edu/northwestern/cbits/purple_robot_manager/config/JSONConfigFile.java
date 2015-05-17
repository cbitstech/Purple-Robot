package edu.northwestern.cbits.purple_robot_manager.config;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

import android.content.Context;

public class JSONConfigFile
{
    private Context _context;

    public JSONConfigFile(Context context)
    {
        this._context = context;
    }

    public String toString()
    {
        try
        {
            JSONObject config = JSONConfigFile.jsonFromMap(PurpleRobotApplication.configuration(this._context));

            config.put(
                    "triggers",
                    JSONConfigFile.jsonFromList(TriggerManager.getInstance(this._context).triggerConfigurations(
                            this._context)));
            config.put("probes", JSONConfigFile.jsonFromList(ProbeManager.probeConfigurations(this._context)));

            return config.toString();
        }
        catch (JSONException e)
        {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    public static JSONObject jsonFromMap(Map<String, Object> map) throws JSONException
    {
        JSONObject json = new JSONObject();

        for (String key : map.keySet())
        {
            Object value = map.get(key);

            if (value instanceof List<?>)
                value = JSONConfigFile.jsonFromList((List<Map<String, Object>>) value);
            else if (value instanceof Map<?, ?>)
                value = JSONConfigFile.jsonFromMap((Map<String, Object>) value);

            json.put(key, value);
        }

        return json;

    }

    private static JSONArray jsonFromList(List<Map<String, Object>> list) throws JSONException
    {
        JSONArray array = new JSONArray();

        for (Map<String, Object> map : list)
        {
            array.put(JSONConfigFile.jsonFromMap(map));
        }

        return array;
    }
}
