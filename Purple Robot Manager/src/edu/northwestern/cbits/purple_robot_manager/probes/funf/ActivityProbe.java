package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;

public class ActivityProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.ActivityProbe";
	}

	public String key()
	{
		return "activity";
	}

	protected int funfTitle()
	{
		return R.string.title_activity_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_activity_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_motion_category);
	}

	public String period()
	{
		return "60";
	}

	public String duration()
	{
		return "5";
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		int high = bundle.getInt("HIGH_ACTIVITY_INTERVALS");
		int low = bundle.getInt("LOW_ACTIVITY_INTERVALS");

		return String.format(context.getResources().getString(R.string.summary_activity_probe), high, low);
	}
}
