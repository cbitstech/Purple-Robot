package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class ActivityProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.ActivityProbe";
	}

	public String key()
	{
		return "activity";
	}

	protected int funfTitle()
	{
		return R.string.title_activity_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_activity_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_motion_category);
	}

	public String period()
	{
		return "60";
	}

	public String duration()
	{
		return "5";
	}

	public String summarizeValue(Context context, Object object)
	{
		if (object instanceof String)
		{
			try
			{
				String jsonString = (String) object;

				JSONObject json = new JSONObject(jsonString);

				int high = json.getJSONObject("extras").getJSONObject("VALUE").getInt("HIGH_ACTIVITY_INTERVALS");
				int low = json.getJSONObject("extras").getJSONObject("VALUE").getInt("LOW_ACTIVITY_INTERVALS");

				return String.format(context.getResources().getString(R.string.summary_activity_probe), high, low);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return super.summarizeValue(context, object);
	}
}
