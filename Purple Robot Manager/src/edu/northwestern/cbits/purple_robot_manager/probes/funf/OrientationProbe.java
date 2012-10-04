package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class OrientationProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.OrientationSensorProbe";
	}

	public String key()
	{
		return "orientation";
	}

	protected int funfTitle()
	{
		return R.string.title_orientation_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_orientation_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_motion_category);
	}
}
