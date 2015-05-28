package edu.northwestern.cbits.purple_robot_manager.charts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitActivity;

public class LineChart extends Chart
{
    protected Map<String, List<Double>> _series = new HashMap<>();
    protected List<Double> _times = new ArrayList<>();

    public void addSeries(String key, List<Double> series)
    {
        this._series.put(key, series);
    }

    public void addTime(String string, ArrayList<Double> times)
    {
        this._times = times;
    }

    public JSONObject dataJson(Activity activity) throws JSONException, IOException
    {
        JSONObject chartJson = (JSONObject) new JSONTokener(WebkitActivity.stringForAsset(activity,
                "webkit/js/placeholder_line.js")).nextValue();

        JSONArray series = chartJson.getJSONArray("series");

        for (String key : this._series.keySet())
        {
            JSONObject seriesObject = new JSONObject();

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

                    sample.put(this._times.get(i));
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
