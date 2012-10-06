package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import android.os.Bundle;
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
		return R.string.summary_telephony_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_category);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		String network = bundle.getString("NETWORK_OPERATOR_NAME");

		return String.format(context.getResources().getString(R.string.summary_telephony_probe), network);
	}

}
