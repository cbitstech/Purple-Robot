package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.Calendar;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
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
    private static final String ENABLED = "config_probe_date_calendar_enabled";
    private static final String FREQUENCY = "config_probe_date_calendar_frequency";

    private long _lastCheck = 0;

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.DateCalendarProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_date_calendar_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_personal_info_category);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(PreferenceActivity activity)
    {
        PreferenceManager manager = activity.getPreferenceManager();

        PreferenceScreen screen = manager.createPreferenceScreen(activity);
        screen.setTitle(this.title(activity));
        screen.setSummary(R.string.summary_date_calendar_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(DateCalendarProbe.ENABLED);
        enabled.setDefaultValue(DateCalendarProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        ListPreference duration = new ListPreference(activity);
        duration.setKey(DateCalendarProbe.FREQUENCY);
        duration.setEntryValues(R.array.probe_low_frequency_values);
        duration.setEntries(R.array.probe_low_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

        screen.addPreference(duration);

        return screen;
    }

    @Override
    public boolean isEnabled(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            long now = System.currentTimeMillis();

            if (prefs.getBoolean(DateCalendarProbe.ENABLED, DateCalendarProbe.DEFAULT_ENABLED))
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

                    this._lastCheck = now;
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_date_calendar_probe_desc);
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        int month = (int) bundle.getDouble(DateCalendarProbe.MONTH);
        int week = (int) bundle.getDouble(DateCalendarProbe.WEEK_OF_MONTH);
        int day = (int) bundle.getDouble(DateCalendarProbe.DAY_OF_WEEK);

        return String.format(context.getResources().getString(R.string.summary_date_calendar_probe), month, week, day);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(DateCalendarProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(DateCalendarProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = new JSONObject();

        try
        {
            JSONObject enabled = new JSONObject();
            enabled.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            JSONArray values = new JSONArray();
            values.put(true);
            values.put(false);
            enabled.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_ENABLED, enabled);

            JSONObject frequency = new JSONObject();
            frequency.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
            values = new JSONArray();

            String[] options = context.getResources().getStringArray(R.array.probe_low_frequency_values);

            for (String option : options)
            {
                values.put(Long.parseLong(option));
            }

            frequency.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_FREQUENCY, frequency);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(Probe.PROBE_FREQUENCY))
        {
            Object frequency = params.get(Probe.PROBE_FREQUENCY);

            if (frequency instanceof Double)
            {
                frequency = Long.valueOf(((Double) frequency).longValue());
            }

            if (frequency instanceof Long)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(DateCalendarProbe.FREQUENCY, frequency.toString());
                e.commit();
            }
        }
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString(DateCalendarProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

        map.put(Probe.PROBE_FREQUENCY, freq);

        return map;
    }
}
