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

public abstract class Continuous3DProbe extends ContinuousProbe
{
    protected static final String X_KEY = "X";
    protected static final String Y_KEY = "Y";
    protected static final String Z_KEY = "Z";

    protected static final String[] fieldNames =
    { X_KEY, Y_KEY, Z_KEY };

    public String getDisplayContent(Activity activity)
    {
        try
        {
            String template = WebkitActivity.stringForAsset(activity, "webkit/epoch_chart_line_3_values.html");

            JSONArray xSeries = new JSONArray();
            JSONArray ySeries = new JSONArray();
            JSONArray zSeries = new JSONArray();

            JSONArray data = new JSONArray();

            JSONObject xData = new JSONObject();
            xData.put("label", "X");
            xData.put("values", xSeries);

            data.put(xData);

            JSONObject yData = new JSONObject();
            yData.put("label", "Y");
            yData.put("values", ySeries);

            data.put(yData);

            JSONObject zData = new JSONObject();
            zData.put("label", "Z");
            zData.put("values", zSeries);

            data.put(zData);

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
