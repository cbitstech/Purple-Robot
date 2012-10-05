package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import org.json.JSONException;
import org.json.JSONObject;

import edu.northwestern.cbits.purple_robot_manager.R;
import android.content.Context;

public class LightProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.LightSensorProbe";
	}

	public String key()
	{
		return "light";
	}

	protected int funfTitle()
	{
		return R.string.title_light_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_light_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public String summarizeValue(Context context, Object object)
	{
		if (object instanceof String)
		{
			try
			{
				String jsonString = (String) object;

				JSONObject json = new JSONObject(jsonString);

				double lux = json.getJSONObject("extras").getJSONObject("VALUE").getDouble("LUX");

				return String.format(context.getResources().getString(R.string.summary_light_probe), lux);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return super.summarizeValue(context, object);
	}
}
