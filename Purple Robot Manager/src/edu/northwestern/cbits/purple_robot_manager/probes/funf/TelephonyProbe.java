package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class TelephonyProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.TelephonyProbe";
	}

	public String key()
	{
		return "telephony";
	}

	protected int funfTitle()
	{
		return R.string.title_telephony_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_telephony_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_category);
	}
}
