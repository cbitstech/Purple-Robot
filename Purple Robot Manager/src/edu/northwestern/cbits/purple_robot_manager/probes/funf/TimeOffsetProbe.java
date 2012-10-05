package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class TimeOffsetProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.TimeOffsetProbe";
	}

	public String key()
	{
		return "hardware_info";
	}

	protected int funfTitle()
	{
		return R.string.title_time_offset_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_time_offset_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_category);
	}

	public String period()
	{
		return "21600";
	}

}
