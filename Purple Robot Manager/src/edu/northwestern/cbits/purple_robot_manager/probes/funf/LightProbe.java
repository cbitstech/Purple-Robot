package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.StartActivity;

public class LightProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.LightSensorProbe";
	}

	public String key()
	{
		return "light";
	}

	protected int funfTitle()
	{
		return R.string.title_light_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_light_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		float lux = bundle.getFloatArray("LUX")[0];

		return String.format(context.getResources().getString(R.string.summary_light_probe), lux);
	}
}
