package edu.northwestern.cbits.purple_robot_manager.probes.funf;

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
}
