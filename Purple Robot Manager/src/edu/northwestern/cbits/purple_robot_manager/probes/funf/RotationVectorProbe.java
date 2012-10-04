package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class RotationVectorProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.RotationVectorSensorProbe";
	}

	public String key()
	{
		return "rotation_vector";
	}

	protected int funfTitle()
	{
		return R.string.title_rotation_vector_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_rotation_vector_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_motion_category);
	}
}
