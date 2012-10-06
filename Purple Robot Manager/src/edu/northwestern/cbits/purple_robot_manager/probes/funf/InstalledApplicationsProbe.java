package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.StartActivity;

public class InstalledApplicationsProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.ApplicationsProbe";
	}

	public String key()
	{
		return "installed_applications";
	}

	protected int funfTitle()
	{
		return R.string.title_installed_applications_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_installed_applications_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_interaction_category);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		ArrayList<Object> apps = (ArrayList<Object>) bundle.get("INSTALLED_APPLICATIONS");

		return String.format(context.getResources().getString(R.string.summary_installed_apps_probe), apps.size());
	}
}
