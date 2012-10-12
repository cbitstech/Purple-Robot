package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;

public class GyroscopeProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.GyroscopeSensorProbe";
	}

	public String key()
	{
		return "gyroscope";
	}

	protected int funfTitle()
	{
		return R.string.title_gyroscope_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_gyroscope_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_motion_category);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		float xReading = bundle.getFloatArray("X")[0];
		float yReading = bundle.getFloatArray("Y")[0];
		float zReading = bundle.getFloatArray("Z")[0];

		return String.format(context.getResources().getString(R.string.summary_gyroscope_probe), xReading, yReading, zReading);
	}
}
