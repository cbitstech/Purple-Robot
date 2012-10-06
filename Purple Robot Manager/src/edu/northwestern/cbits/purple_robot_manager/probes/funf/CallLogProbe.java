package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.StartActivity;

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
		return R.string.summary_call_log_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_social_category);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		ArrayList<String> calls = (ArrayList<String>) bundle.get("CALLS");

		return String.format(context.getResources().getString(R.string.summary_call_log_probe), calls.size());
	}
}
