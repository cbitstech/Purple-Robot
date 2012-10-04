package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class LocationProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.LocationProbe";
	}

	public String key()
	{
		return "location";
	}

	protected int funfTitle()
	{
		return R.string.title_location_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_location_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_positioning_category);
	}

	public String period()
	{
		return "1800";
	}

	public String duration()
	{
		return "120";
	}
}
