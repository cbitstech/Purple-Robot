package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import edu.northwestern.cbits.purple_robot_manager.R;
import android.content.Context;

public class MagneticFieldProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.MagneticFieldSensorProbe";
	}

	public String key()
	{
		return "magnetic_field";
	}

	protected int funfTitle()
	{
		return R.string.title_magnetic_field_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_magnetic_field_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}
}
