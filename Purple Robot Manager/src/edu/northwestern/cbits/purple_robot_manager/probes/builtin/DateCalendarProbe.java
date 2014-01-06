package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class DateCalendarProbe extends Probe 
{
	private static final boolean DEFAULT_ENABLED = true;
	private static final String DAY_OF_WEEK = "DAY_OF_WEEK";
	private static final String DAY_OF_MONTH = "DAY_OF_MONTH";
	private static final String DAY_OF_YEAR = "DAY_OF_YEAR";
	private static final String WEEK_OF_MONTH = "WEEK_OF_MONTH";
	private static final String MONTH = "MONTH";
	private static final String MINUTE = "MINUTE";
	private static final String DST_OFFSET = "DST_OFFSET";
	private static final String HOUR_OF_DAY = "HOUR_OF_DAY";
	private static final String DAY_OF_WEEK_IN_MONTH = "DAY_OF_WEEK_IN_MONTH";
	private static final String WEEK_OF_YEAR = "WEEK_OF_YEAR";

	private long _lastCheck = 0;

	public String name(Context context) 
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.DateCalendarProbe";
	}

	public String title(Context context) 
	{
		return context.getString(R.string.title_date_calendar_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_personal_info_category);
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity) 
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_date_calendar_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_date_calendar_enabled");
		enabled.setDefaultValue(DateCalendarProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_date_calendar_frequency");
		duration.setEntryValues(R.array.probe_low_frequency_values);
		duration.setEntries(R.array.probe_low_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);
		duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

		screen.addPreference(duration);

		return screen;
	}

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);

		if (super.isEnabled(context))
		{
			long now = System.currentTimeMillis();

			if (prefs.getBoolean("config_probe_date_calendar_enabled", DateCalendarProbe.DEFAULT_ENABLED))
			{
				if (now - this._lastCheck > Long.parseLong(prefs.getString("config_probe_date_calendar_frequency", Probe.DEFAULT_FREQUENCY)))
				{
					Bundle bundle = new Bundle();
					bundle.putString("PROBE", this.name(context));
					bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
					
					Calendar calendar = Calendar.getInstance();

					bundle.putInt(DateCalendarProbe.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH));
					bundle.putInt(DateCalendarProbe.DAY_OF_WEEK, calendar.get(Calendar.DAY_OF_WEEK));
					bundle.putInt(DateCalendarProbe.DAY_OF_WEEK_IN_MONTH, calendar.get(Calendar.DAY_OF_WEEK_IN_MONTH));
					bundle.putInt(DateCalendarProbe.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR));
					bundle.putInt(DateCalendarProbe.DST_OFFSET, calendar.get(Calendar.DST_OFFSET));
					bundle.putInt(DateCalendarProbe.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
					bundle.putInt(DateCalendarProbe.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH));
					bundle.putInt(DateCalendarProbe.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR));
					bundle.putInt(DateCalendarProbe.MONTH, calendar.get(Calendar.MONTH) + 1);
					bundle.putInt(DateCalendarProbe.MINUTE, calendar.get(Calendar.MINUTE));

					this.transmitData(context, bundle);

					this._lastCheck  = now;
				}

				return true;
			}
		}

		return false;
	}
	
 
	{

	}
	
	public String summarizeValue(Context context, Bundle bundle)
	{
		int month = (int) bundle.getDouble(DateCalendarProbe.MONTH);
		int week = (int) bundle.getDouble(DateCalendarProbe.WEEK_OF_MONTH);
		int day = (int) bundle.getDouble(DateCalendarProbe.DAY_OF_WEEK);

		return String.format(context.getResources().getString(R.string.summary_date_calendar_probe), month, week, day);
	}

	public void enable(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_date_calendar_enabled", true);
		
		e.commit();
	}

	public void disable(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_date_calendar_enabled", false);
		
		e.commit();
	}
}
