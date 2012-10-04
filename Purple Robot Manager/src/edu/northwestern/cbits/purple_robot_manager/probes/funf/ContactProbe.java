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

public class ContactProbe extends Probe
{
	private static String USE_FULL = "FULL";

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_social_category);
	}

	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		@SuppressWarnings("deprecation")
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = ProbeManager.inflatePreferenceScreenFromResource(activity, R.layout.layout_settings_probe_contact, manager);

		return screen;
	}

	public Bundle[] dataRequestBundles(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		Bundle bundle = new Bundle();
		bundle.putLong(Probe.PERIOD, Long.parseLong(prefs.getString("config_probe_contact_period", "3600")));
		bundle.putBoolean(ContactProbe.USE_FULL, "true".equals(prefs.getString("config_probe_contact_full_list", "false")));

		return new Bundle[] { bundle };
	}

	public String name(Context context)
	{
		return "edu.mit.media.funf.probe.builtin.ContactProbe";
	}

	public String title(Context context)
	{
		return context.getResources().getString(R.string.title_contact_probe);
	}
}
