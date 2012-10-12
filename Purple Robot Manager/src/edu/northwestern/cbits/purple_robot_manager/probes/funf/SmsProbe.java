package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
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
		return R.string.summary_sms_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.title_sms_probe);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		@SuppressWarnings("unchecked")
		ArrayList<Object> messages = (ArrayList<Object>) bundle.get("MESSAGES");

		return String.format(context.getResources().getString(R.string.summary_sms_probe), messages.size());
	}
}
