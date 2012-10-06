package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.StartActivity;

public class RunningApplicationsProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.RunningApplicationsProbe";
	}

	public String key()
	{
		return "running_applications";
	}

	protected int funfTitle()
	{
		return R.string.title_running_applications_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_running_applications_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_interaction_category);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		ArrayList<Object> tasks = (ArrayList<Object>) bundle.get("RUNNING_TASKS");

		return String.format(context.getResources().getString(R.string.summary_tasks_probe), tasks.size());
	}
}
