package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class GravityProbe extends BasicFunfProbe
{
	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_motion_category);
	}

	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.GravitySensorProbe";
	}

	public String key()
	{
		return "gravity";
	}

	protected int funfTitle()
	{
		return R.string.title_gravity_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_gravity_probe;
	}
}
