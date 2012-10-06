package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.StartActivity;

public class PressureProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.PressureSensorProbe";
	}

	public String key()
	{
		return "pressure";
	}

	protected int funfTitle()
	{
		return R.string.title_pressure_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_pressure_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		float pressure = bundle.getFloatArray("PRESSURE")[0];

		return String.format(context.getResources().getString(R.string.summary_pressure_probe), pressure);
	}
}
