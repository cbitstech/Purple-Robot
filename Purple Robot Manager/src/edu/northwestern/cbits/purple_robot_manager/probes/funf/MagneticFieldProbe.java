package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.northwestern.cbits.purple_robot_manager.R;
import android.content.Context;

public class MagneticFieldProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.MagneticFieldSensorProbe";
	}

	public String key()
	{
		return "magnetic_field";
	}

	protected int funfTitle()
	{
		return R.string.title_magnetic_field_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_magnetic_field_probe;
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
