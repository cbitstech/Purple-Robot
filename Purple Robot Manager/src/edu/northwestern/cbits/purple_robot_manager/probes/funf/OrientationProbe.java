package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class OrientationProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.OrientationSensorProbe";
	}

	public String key()
	{
		return "orientation";
	}

	protected int funfTitle()
	{
		return R.string.title_orientation_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_orientation_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_motion_category);
	}

	public String summarizeValue(Context context, Object object)
	{
		if (object instanceof String)
		{
			try
			{
				String jsonString = (String) object;

				JSONObject json = new JSONObject(jsonString);

				JSONArray x = json.getJSONObject("extras").getJSONObject("VALUE").getJSONArray("AZIMUTH");
				JSONArray y = json.getJSONObject("extras").getJSONObject("VALUE").getJSONArray("PITCH");
				JSONArray z = json.getJSONObject("extras").getJSONObject("VALUE").getJSONArray("ROLL");

				double azimuth = Double.parseDouble(x.get(0).toString());
				double pitch = Double.parseDouble(y.get(0).toString());
				double roll = Double.parseDouble(z.get(0).toString());

				return String.format(context.getResources().getString(R.string.summary_orientation_probe), azimuth, pitch, roll);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return super.summarizeValue(context, object);
	}
}
