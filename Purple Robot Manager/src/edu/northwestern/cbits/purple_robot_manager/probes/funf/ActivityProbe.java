package edu.northwestern.cbits.purple_robot_manager.probes.funf;

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
		return R.string.title_accelerometer_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_accelerometer_probe;
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
}
