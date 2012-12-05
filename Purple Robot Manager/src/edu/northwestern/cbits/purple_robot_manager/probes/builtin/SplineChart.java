package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitActivity;
import edu.northwestern.cbits.purple_robot_manager.charts.LineChart;

public class SplineChart extends LineChart
{
	public JSONObject highchartsJson(Activity activity) throws JSONException, IOException
	{
		JSONObject chartJson = (JSONObject) new JSONTokener(WebkitActivity.stringForAsset(activity, "webkit/js/highcharts_spline.js")).nextValue();

		JSONArray series = chartJson.getJSONArray("series");

		for (String key : this._series.keySet())
		{
			JSONObject seriesObject = new JSONObject();

			seriesObject.put("name", key);

			JSONArray array = new JSONArray();

			List<Double> list = this._series.get(key);

			for (Double d : list)
			{
				array.put(d.doubleValue());
			}

			seriesObject.put("data", array);

			series.put(seriesObject);
		}

		return chartJson;
	}
}
