package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class WifiProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.WifiProbe";
	}

	public String key()
	{
		return "wifi";
	}

	protected int funfTitle()
	{
		return R.string.title_wifi_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_wifi_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_positioning_category);
	}

	public String period()
	{
		return "1200";
	}
}
