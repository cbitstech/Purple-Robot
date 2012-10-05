package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class CallLogProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.CallLogProbe";
	}

	public String key()
	{
		return "call_log";
	}

	protected int funfTitle()
	{
		return R.string.title_call_log_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_call_log_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_social_category);
	}
}
