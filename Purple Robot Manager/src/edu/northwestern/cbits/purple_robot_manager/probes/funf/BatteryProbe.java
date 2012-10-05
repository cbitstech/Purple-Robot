package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class BatteryProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.BatteryProbe";
	}

	public String key()
	{
		return "battery";
	}

	protected int funfTitle()
	{
		return R.string.title_battery_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_battery_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_category);
	}

	public String period()
	{
		return "300";
	}

	public String summarizeValue(Context context, Object object)
	{
		if (object instanceof String)
		{
			try
			{
				String jsonString = (String) object;

				JSONObject json = new JSONObject(jsonString);

				int level = json.getJSONObject("extras").getJSONObject("VALUE").getInt("level");

				return String.format(context.getResources().getString(R.string.summary_battery_probe), level);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return super.summarizeValue(context, object);
	}
}
