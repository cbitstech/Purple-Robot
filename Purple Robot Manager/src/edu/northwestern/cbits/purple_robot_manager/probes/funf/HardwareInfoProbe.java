package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class HardwareInfoProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.HardwareInfoProbe";
	}

	public String key()
	{
		return "hardware_info";
	}

	protected int funfTitle()
	{
		return R.string.title_hardware_info_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_hardware_info_probe;
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

				JSONObject value = json.getJSONObject("extras").getJSONObject("VALUE");

				String model = value.getString("MODEL");
				String mac = value.getString("WIFI_MAC");

				return String.format(context.getResources().getString(R.string.summary_hardware_info_probe), model, mac);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return super.summarizeValue(context, object);
	}

}
