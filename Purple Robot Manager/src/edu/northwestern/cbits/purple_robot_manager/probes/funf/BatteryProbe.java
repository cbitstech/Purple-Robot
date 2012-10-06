package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;

public class BatteryProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.BatteryProbe";
	}

	public String key()
	{
		return "battery";
	}

	protected int funfTitle()
	{
		return R.string.title_battery_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_battery_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_category);
	}

	public String period()
	{
		return "300";
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		int level = bundle.getInt("level");

		return String.format(context.getResources().getString(R.string.summary_battery_probe), level);
	}
}
