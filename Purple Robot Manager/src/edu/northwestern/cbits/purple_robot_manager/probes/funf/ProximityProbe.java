package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;

public class ProximityProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.ProximitySensorProbe";
	}

	public String key()
	{
		return "proximity";
	}

	protected int funfTitle()
	{
		return R.string.title_proximity_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_proximity_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		float proximity = bundle.getFloatArray("DISTANCE")[0];

		return String.format(context.getResources().getString(R.string.summary_proximity_value_probe), proximity);
	}
}
