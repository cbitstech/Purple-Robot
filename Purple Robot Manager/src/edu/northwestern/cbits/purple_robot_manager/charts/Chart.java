package edu.northwestern.cbits.purple_robot_manager.charts;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;

import edu.northwestern.cbits.purple_robot_manager.activities.WebkitActivity;

public class Chart
{
	public JSONObject highchartsJson(Activity activity) throws JSONException, IOException
	{
		JSONObject chartJson = (JSONObject) new JSONTokener(WebkitActivity.stringForAsset(activity, "webkit/js/highcharts_placeholder.js")).nextValue();
		return chartJson;
	}
}