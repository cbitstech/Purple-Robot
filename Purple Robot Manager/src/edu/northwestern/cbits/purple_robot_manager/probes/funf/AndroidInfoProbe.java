package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class AndroidInfoProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.AndroidInfoProbe";
	}

	public String key()
	{
		return "android_info";
	}

	protected int funfTitle()
	{
		return R.string.title_android_info_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_android_info_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_category);
	}
}
