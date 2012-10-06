package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import edu.northwestern.cbits.purple_robot_manager.R;
import android.content.Context;

public class TemperatureProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.TemperatureSensorProbe";
	}

	public String key()
	{
		return "temperature";
	}

	protected int funfTitle()
	{
		return R.string.title_temperature_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_temperature_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}
}
