package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class SmsProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.SMSProbe";
	}

	public String key()
	{
		return "sms";
	}

	protected int funfTitle()
	{
		return R.string.title_sms_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_sms_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.title_sms_probe);
	}
}
