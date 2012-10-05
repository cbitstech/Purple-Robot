package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class BluetoothProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.BluetoothProbe";
	}

	public String key()
	{
		return "bluetooth";
	}

	protected int funfTitle()
	{
		return R.string.title_bluetooth_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_bluetooth_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_positioning_category);
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

				JSONArray scan = json.getJSONObject("extras").getJSONObject("VALUE").getJSONArray("DEVICES");

				return String.format(context.getResources().getString(R.string.summary_bluetooth_probe), scan.length());
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return super.summarizeValue(context, object);
	}
}

