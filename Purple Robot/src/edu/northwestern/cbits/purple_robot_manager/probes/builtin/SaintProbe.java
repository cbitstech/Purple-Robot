package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

public class SaintProbe extends Probe
{
    public static final String FETCH_INTENT = "edu.cornell.cs.pac.saint.helper.fetch";
    private static final String DATA = "saint_data";

    private static final boolean DEFAULT_ENABLED = false;

    private BroadcastReceiver _receiver = null;

    private long _lastCheck = 0;

    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.SaintProbe";
    }

    public String title(Context context)
    {
        return context.getString(R.string.title_saint_probe);
    }

    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_misc_category);
    }

    public boolean isEnabled(final Context context)
    {
        if (super.isEnabled(context))
        {
            SharedPreferences prefs = Probe.getPreferences(context);

            if (prefs.getBoolean("config_probe_saint_enabled", SaintProbe.DEFAULT_ENABLED))
            {
                if (this._receiver == null)
                {
                    final SaintProbe me = this;

                    this._receiver = new BroadcastReceiver()
                    {
                        public void onReceive(Context context, Intent intent)
                        {
                            Bundle bundle = intent.getBundleExtra(SaintProbe.DATA);

                            bundle.putString("PROBE", me.name(context));
                            bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                            me.transmitData(context, bundle);
                        }
                    };

                    IntentFilter filter = new IntentFilter();
                    filter.addAction(SaintProbe.FETCH_INTENT);

                    context.registerReceiver(this._receiver, filter);
                }

                final long now = System.currentTimeMillis();

                synchronized (this)
                {
                    long freq = Long
                            .parseLong(prefs.getString("config_probe_saint_frequency", Probe.DEFAULT_FREQUENCY));

                    if (now - this._lastCheck > freq)
                    {
                        this._lastCheck = now;

                        Intent fetchIntent = new Intent(SaintProbe.FETCH_INTENT);

                        context.startService(fetchIntent);
                    }
                }

                return true;
            }
        }

        if (this._receiver != null)
        {
            try
            {
                context.unregisterReceiver(this._receiver);
            }
            catch (IllegalArgumentException e)
            {
                LogManager.getInstance(context).logException(e);
            }

            this._receiver = null;
        }

        return false;
    }

    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean("config_probe_saint_enabled", true);

        e.commit();
    }

    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean("config_probe_saint_enabled", false);

        e.commit();
    }

    public String summarizeValue(Context context, Bundle bundle)
    {
        int lastSpeech = (int) bundle.getDouble("LAST_SPEECH_READING");

        int speechResource = R.string.label_speech_none;

        if (lastSpeech == 0)
            speechResource = R.string.label_speech_human;

        int lastActivity = (int) bundle.getDouble("LAST_ACTIVITY");

        int activityResource = R.string.label_activity_unknown;

        switch (lastActivity)
        {
        case 1:
            activityResource = R.string.label_activity_stationary;
            break;
        case 2:
            activityResource = R.string.label_activity_walking;
            break;
        case 3:
            activityResource = R.string.label_activity_running;
            break;
        }

        return context.getString(R.string.summary_saint_probe, context.getString(activityResource),
                context.getString(speechResource));
    }

    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString("config_probe_saint_frequency", Probe.DEFAULT_FREQUENCY));

        map.put(Probe.PROBE_FREQUENCY, freq);

        return map;
    }

    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(Probe.PROBE_FREQUENCY))
        {
            Object frequency = params.get(Probe.PROBE_FREQUENCY);

            if (frequency instanceof Long)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString("config_probe_saint_frequency", frequency.toString());
                e.commit();
            }
        }
    }

    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(PreferenceActivity activity)
    {
        PreferenceManager manager = activity.getPreferenceManager();

        PreferenceScreen screen = manager.createPreferenceScreen(activity);
        screen.setTitle(this.title(activity));
        screen.setSummary(R.string.summary_saint_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey("config_probe_saint_enabled");
        enabled.setDefaultValue(SaintProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        ListPreference duration = new ListPreference(activity);
        duration.setKey("config_probe_saint_frequency");
        duration.setEntryValues(R.array.probe_satellite_frequency_values);
        duration.setEntries(R.array.probe_satellite_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

        screen.addPreference(duration);

        return screen;
    }

    public String summary(Context context)
    {
        return context.getString(R.string.summary_saint_probe_desc);
    }
}
