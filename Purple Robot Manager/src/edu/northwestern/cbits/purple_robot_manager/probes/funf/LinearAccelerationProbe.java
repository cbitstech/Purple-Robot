package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class LinearAccelerationProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.LinearAccelerationProbe";
	}

	public String key()
	{
		return "linear_acceleration";
	}

	protected int funfTitle()
	{
		return R.string.title_linear_acceleration_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_linear_acceleration_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_motion_category);
	}
}
