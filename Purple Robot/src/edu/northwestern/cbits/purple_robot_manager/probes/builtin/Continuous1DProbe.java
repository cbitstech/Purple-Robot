package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.IOException;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import edu.northwestern.cbits.purple_robot_manager.activities.RealTimeProbeViewActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitActivity;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

public abstract class Continuous1DProbe extends ContinuousProbe
{
    protected static final String[] fieldNames =
    { "VALUE_KEY" };

    public String getDisplayContent(Activity activity)
    {
        try
        {
            String template = WebkitActivity.stringForAsset(activity, "webkit/epoch_chart_line_1_value.html");

            JSONArray valueSeries = new JSONArray();

            JSONArray data = new JSONArray();

            JSONObject valueData = new JSONObject();
            valueData.put("label", "VALUE");
            valueData.put("values", valueSeries);

            data.put(valueData);

            template = template.replace("{{{ data_json }}}", data.toString());

            return template;
        }
        catch (IOException | JSONException e)
        {
            LogManager.getInstance(activity).logException(e);
        }

        return null;
    }

    public Intent viewIntent(Context context)
    {
        Intent i = new Intent(context, RealTimeProbeViewActivity.class);
        i.putExtra(RealTimeProbeViewActivity.PROBE_ID, this.getTitleResource());

        return i;
    }

    protected abstract String tableName();

    protected abstract Map<String, String> databaseSchema();
}
