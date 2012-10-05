package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class InstalledApplicationsProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.ApplicationsProbe";
	}

	public String key()
	{
		return "installed_applications";
	}

	protected int funfTitle()
	{
		return R.string.title_installed_applications_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_installed_applications_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_interaction_category);
	}
}
