package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class GravityProbe extends BasicFunfProbe
{
	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_motion_category);
	}

	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.GravitySensorProbe";
	}

	public String key()
	{
		return "gravity";
	}

	protected int funfTitle()
	{
		return R.string.title_gravity_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_gravity_probe;
	}

	public String summarizeValue(Context context, Object object)
	{
		if (object instanceof String)
		{
			try
			{
				String jsonString = (String) object;

				JSONObject json = new JSONObject(jsonString);

				JSONArray x = json.getJSONObject("extras").getJSONObject("VALUE").getJSONArray("X");
				JSONArray y = json.getJSONObject("extras").getJSONObject("VALUE").getJSONArray("Y");
				JSONArray z = json.getJSONObject("extras").getJSONObject("VALUE").getJSONArray("Z");

				double xReading = Double.parseDouble(x.get(0).toString());
				double yReading = Double.parseDouble(y.get(0).toString());
				double zReading = Double.parseDouble(z.get(0).toString());

				return String.format(context.getResources().getString(R.string.summary_accelerator_probe), xReading, yReading, zReading);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return super.summarizeValue(context, object);
	}
}
