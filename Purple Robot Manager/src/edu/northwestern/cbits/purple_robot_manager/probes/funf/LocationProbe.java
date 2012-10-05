package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class LocationProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.LocationProbe";
	}

	public String key()
	{
		return "location";
	}

	protected int funfTitle()
	{
		return R.string.title_location_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_location_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_positioning_category);
	}

	public String period()
	{
		return "1800";
	}

	public String duration()
	{
		return "120";
	}

	public String summarizeValue(Context context, Object object)
	{
		if (object instanceof String)
		{
			try
			{
				String jsonString = (String) object;

				JSONObject json = new JSONObject(jsonString);

				JSONObject location = json.getJSONObject("extras").getJSONObject("VALUE").getJSONObject("LOCATION");

				double longitude = location.getDouble("mLongitude");
				double latitude = location.getDouble("mLatitude");

				return String.format(context.getResources().getString(R.string.summary_location_probe), latitude, longitude);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return super.summarizeValue(context, object);
	}
}
