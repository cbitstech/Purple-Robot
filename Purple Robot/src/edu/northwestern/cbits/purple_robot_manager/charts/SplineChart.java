package edu.northwestern.cbits.purple_robot_manager.charts;

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitActivity;

public class SplineChart extends LineChart
{
    public JSONObject dataJson(Activity activity) throws JSONException, IOException
    {
        JSONObject chartJson = (JSONObject) new JSONTokener(WebkitActivity.stringForAsset(activity,
                "webkit/vendor/spline_template.js")).nextValue();

        JSONArray series = chartJson.getJSONArray("series");

        for (String key : this._series.keySet())
        {
            JSONObject seriesObject = new JSONObject();

            seriesObject.put("name", key);

            JSONArray array = new JSONArray();

            List<Double> list = this._series.get(key);

            if (this._times.size() == 0)
            {
                for (Double d : list)
                {
                    array.put(d.doubleValue());
                }
            }
            else
            {
                for (int i = 0; i < list.size() && i < this._times.size(); i++)
                {
                    JSONArray sample = new JSONArray();

                    sample.put(this._times.get(i) * 1000);
                    sample.put(list.get(i));

                    array.put(sample);
                }
            }

            seriesObject.put("data", array);

            series.put(seriesObject);
        }

        return chartJson;
    }
}
