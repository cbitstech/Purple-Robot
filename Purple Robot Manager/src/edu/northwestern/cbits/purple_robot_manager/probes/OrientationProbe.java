package edu.northwestern.cbits.purple_robot_manager.probes;

import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;

public class OrientationProbe extends Probe
{
	public static String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.OrientationSensorProbe";
	}

	public OrientationProbe(String name, String title, long period, long duration, Date start, Date end)
	{
		super(name, title, period, duration, start, end);
	}

	public static PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		@SuppressWarnings("deprecation")
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = ProbeManager.inflatePreferenceScreenFromResource(activity, R.layout.layout_settings_probe_orientation, manager);

		return screen;
	}

	public static Bundle[] dataRequestBundles(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		Bundle bundle = new Bundle();
		bundle.putLong(Probe.PERIOD, Long.parseLong(prefs.getString("config_probe_orientation_period", "180")));
		bundle.putLong(Probe.DURATION, Long.parseLong(prefs.getString("config_probe_orientation_duration", "15")));

		return new Bundle[] { bundle };
	}
}
