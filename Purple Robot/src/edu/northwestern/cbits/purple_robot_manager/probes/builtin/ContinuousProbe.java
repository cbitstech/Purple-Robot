package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public abstract class ContinuousProbe extends Probe
{
    public static final String PROBE_THRESHOLD = "threshold";

    protected static final boolean DEFAULT_ENABLED = false;
    protected static final String DEFAULT_FREQUENCY = "0";

    private WakeLock _wakeLock = null;
    private int _wakeLockLevel = -1;

    protected Context _context = null;

    private boolean _lastEnableResult = false;
    private long _lastEnableCheck = 0;

    @Override
    public void enable(Context context)
    {
        String key = this.getPreferenceKey();

        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean("config_probe_" + key + "_enabled", true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        String key = this.getPreferenceKey();

        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean("config_probe_" + key + "_enabled", false);

        e.commit();
    }

    protected boolean shouldProcessEvent(SensorEvent event)
    {
        long now = System.currentTimeMillis();

        if (now - this._lastEnableCheck > 5000)
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

            // TODO: Replace string keys below...

            String key = this.getPreferenceKey();
            this._lastEnableResult = prefs.getBoolean("config_probes_enabled", false);

            if (this._lastEnableResult)
                this._lastEnableResult = prefs.getBoolean("config_probe_" + key + "_enabled", ContinuousProbe.DEFAULT_ENABLED);

            this._lastEnableCheck = now;
        }

        return this._lastEnableResult;
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(PreferenceActivity activity)
    {
        PreferenceManager manager = activity.getPreferenceManager();

        PreferenceScreen screen = manager.createPreferenceScreen(activity);
        screen.setTitle(this.title(activity));
        screen.setSummary(this.summary(activity));

        String key = this.getPreferenceKey();

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey("config_probe_" + key + "_enabled");
        enabled.setDefaultValue(ContinuousProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        ListPreference duration = new ListPreference(activity);
        duration.setKey("config_probe_" + key + "_frequency");
        duration.setEntryValues(this.getResourceFrequencyValues());
        duration.setEntries(this.getResourceFrequencyLabels());
        duration.setTitle(R.string.probe_frequency_label);
        duration.setDefaultValue(ContinuousProbe.DEFAULT_FREQUENCY);

        screen.addPreference(duration);

        ListPreference wakelock = new ListPreference(activity);
        wakelock.setKey("config_probe_" + key + "_wakelock");
        wakelock.setEntryValues(R.array.wakelock_values);
        wakelock.setEntries(R.array.wakelock_labels);
        wakelock.setTitle(R.string.probe_wakelock_title);
        wakelock.setSummary(R.string.probe_wakelock_summary);
        wakelock.setDefaultValue("-1");

        screen.addPreference(wakelock);

        return screen;
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        map.put(Probe.PROBE_FREQUENCY, this.getFrequency());
        map.put(ContinuousProbe.PROBE_THRESHOLD, this.getThreshold());

        return map;
    }

    protected abstract double getThreshold();

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(Probe.PROBE_FREQUENCY))
        {
            Object frequency = params.get(Probe.PROBE_FREQUENCY);

            if (frequency instanceof Long || frequency instanceof Integer)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_frequency";

                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(key, frequency.toString());
                e.commit();
            }
            if (frequency instanceof Double)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_frequency";

                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(key, "" + ((Double) frequency).intValue());
                e.commit();
            }
            else if (frequency instanceof String)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_frequency";

                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(key, frequency.toString());
                e.commit();
            }
        }

        if (params.containsKey(ContinuousProbe.PROBE_THRESHOLD))
        {
            Object threshold = params.get(ContinuousProbe.PROBE_THRESHOLD);

            if (threshold instanceof Long || threshold instanceof Integer)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_threshold";

                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(key, "" + threshold);
                e.commit();
            }
            if (threshold instanceof Double)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_threshold";

                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(key, "" + threshold);
                e.commit();
            }
            else if (threshold instanceof String)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_threshold";

                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(key, "" + threshold);
                e.commit();
            }
        }
    }

    @Override
    public Bundle formattedBundle(Context context, Bundle bundle)
    {
        Bundle formatted = super.formattedBundle(context, bundle);

        return formatted;
    };

    public int getResourceFrequencyLabels()
    {
        return R.array.probe_continuous_frequency_labels;
    }

    public int getResourceFrequencyValues()
    {
        return R.array.probe_continuous_frequency_values;
    }

    public abstract long getFrequency();

    public abstract int getTitleResource();

    public abstract int getSummaryResource();

    public abstract String getPreferenceKey();

    protected abstract boolean passesThreshold(SensorEvent event);

    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    @Override
    public String title(Context context)
    {
        return context.getString(this.getTitleResource());
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(this.getSummaryResource());
    }

    @Override
    @SuppressLint("Wakelock")
    public boolean isEnabled(Context context)
    {
        boolean enabled = super.isEnabled(context);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String key = this.getPreferenceKey();

        int wakeLevel = Integer.parseInt(prefs.getString("config_probe_" + key + "_wakelock", "-1"));

        if (enabled)
        {
            if (wakeLevel != this._wakeLockLevel)
            {
                if (this._wakeLock != null)
                {
                    this._wakeLock.release();

                    this._wakeLock = null;
                }

                this._wakeLockLevel = wakeLevel;
            }

            if (this._wakeLockLevel != -1 && this._wakeLock == null)
            {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

                this._wakeLock = pm.newWakeLock(this._wakeLockLevel, "key");
                this._wakeLock.acquire();
            }
        }
        else
        {
            if (this._wakeLock != null)
            {
                this._wakeLock.release();
                this._wakeLock = null;
            }
        }

        return enabled;
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

            int frequencyValues = this.getResourceFrequencyValues();

            if (frequencyValues != -1)
            {
                JSONObject frequency = new JSONObject();
                frequency.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
                values = new JSONArray();

                String[] options = context.getResources().getStringArray(frequencyValues);

                for (String option : options)
                {
                    values.put(Long.parseLong(option));
                }

                frequency.put(Probe.PROBE_VALUES, values);
                settings.put(Probe.PROBE_FREQUENCY, frequency);
            }

            int thresholdValues = this.getResourceThresholdValues();

            if (thresholdValues != -1)
            {
                JSONObject threshold = new JSONObject();
                threshold.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_DOUBLE);
                values = new JSONArray();

                String[] options = context.getResources().getStringArray(thresholdValues);

                for (String option : options)
                {
                    values.put(Double.parseDouble(option));
                }

                threshold.put(Probe.PROBE_VALUES, values);
                settings.put(ContinuousProbe.PROBE_THRESHOLD, threshold);
            }
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    protected abstract int getResourceThresholdValues();
}
