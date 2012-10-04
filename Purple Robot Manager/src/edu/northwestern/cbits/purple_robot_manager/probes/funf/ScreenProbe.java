package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class ScreenProbe extends Probe
{
	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_interaction_category);
	}

	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		@SuppressWarnings("deprecation")
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = ProbeManager.inflatePreferenceScreenFromResource(activity, R.layout.layout_settings_probe_screen, manager);

		return screen;
	}

	public Bundle[] dataRequestBundles(Context context)
	{
		Bundle bundle = new Bundle();
		return new Bundle[] { bundle };
	}

	public String name(Context context)
	{
		return "edu.mit.media.funf.probe.builtin.WifiProbe";
	}

	public String title(Context context)
	{
		return context.getResources().getString(R.string.title_contact_probe);
	}
}
