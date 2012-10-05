package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class RunningApplicationsProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.RunningApplicationsProbe";
	}

	public String key()
	{
		return "running_applications";
	}

	protected int funfTitle()
	{
		return R.string.title_running_applications_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_running_applications_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_interaction_category);
	}
}
