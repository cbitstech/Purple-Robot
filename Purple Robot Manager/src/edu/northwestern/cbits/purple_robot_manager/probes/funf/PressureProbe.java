package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import edu.northwestern.cbits.purple_robot_manager.R;
import android.content.Context;

public class PressureProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.PressureSensorProbe";
	}

	public String key()
	{
		return "pressure";
	}

	protected int funfTitle()
	{
		return R.string.title_pressure_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_pressure_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}
}
