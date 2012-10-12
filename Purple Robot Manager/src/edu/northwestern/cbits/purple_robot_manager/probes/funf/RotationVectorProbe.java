package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import android.os.Bundle;
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
		return R.string.summary_rotation_vector_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_motion_category);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		float xReading = bundle.getFloatArray("X_SIN_THETA_OVER_2")[0];
		float yReading = bundle.getFloatArray("Y_SIN_THETA_OVER_2")[0];
		float zReading = bundle.getFloatArray("Z_SIN_THETA_OVER_2")[0];

		return String.format(context.getResources().getString(R.string.summary_accelerator_probe), xReading, yReading, zReading);
	}
}
