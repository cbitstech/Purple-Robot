package edu.northwestern.cbits.purple_robot_manager.probes;

import java.util.Date;

import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;

public class LocationProbe extends Probe
{
	public static PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		@SuppressWarnings("deprecation")
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = ProbesPreferenceScreenBuilder.inflatePreferenceScreenFromResource(activity, R.layout.layout_settings_probe_location, manager);

		return screen;
	}

	public LocationProbe(String name, String title, long period, long duration, Date start, Date end)
	{
		super(name, title, period, duration, start, end);
	}
}
