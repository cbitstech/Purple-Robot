package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.security.SecureRandom;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class RandomNoiseProbe extends Probe
{
    private static final String NOISE_VALUE = "NOISE_VALUE";
    public static final String ACTION = "purple_robot_generate_noise";

    private static final boolean DEFAULT_ENABLED = false;
    private static final boolean DEFAULT_PERSIST = false;
    private static final String ENABLED = "config_probe_random_noise_enabled";
    private static final String PERSIST = "config_probe_random_noise_persist";

    private PendingIntent _intent = null;

    public static RandomNoiseProbe instance = null;

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.RandomNoiseProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_random_noise_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_misc_category);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(RandomNoiseProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(RandomNoiseProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public boolean isEnabled(Context context)
    {
        if (RandomNoiseProbe.instance == null)
            RandomNoiseProbe.instance = this;

        SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(RandomNoiseProbe.ENABLED, RandomNoiseProbe.DEFAULT_ENABLED))
            {
                synchronized (this)
                {
                    Bundle bundle = new Bundle();
                    bundle.putString("PROBE", this.name(context));
                    bundle.putDouble("TIMESTAMP", ((double) System.currentTimeMillis()) / 1000);

                    SecureRandom random = new SecureRandom();

                    bundle.putFloat(RandomNoiseProbe.NOISE_VALUE, random.nextFloat());

                    bundle.putBoolean("TRANSMIT", prefs.getBoolean(RandomNoiseProbe.PERSIST, RandomNoiseProbe.DEFAULT_PERSIST));

                    this.transmitData(context, bundle);

                    if (this._intent == null)
                    {
                        this._intent = PendingIntent.getService(context, 0, new Intent(RandomNoiseProbe.ACTION), PendingIntent.FLAG_UPDATE_CURRENT);

                        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 250, this._intent);
                    }
                }

                return true;
            }
            else if (this._intent != null)
            {
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                am.cancel(this._intent);

                this._intent = null;
            }
        }

        return false;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double noise = bundle.getDouble(RandomNoiseProbe.NOISE_VALUE);

        return String.format(context.getResources().getString(R.string.summary_random_noise_probe), noise);
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_random_noise_probe_desc);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(final Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_random_noise_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(RandomNoiseProbe.ENABLED);
        enabled.setDefaultValue(RandomNoiseProbe.DEFAULT_ENABLED);
        screen.addPreference(enabled);

        CheckBoxPreference persist = new CheckBoxPreference(context);
        persist.setTitle(R.string.title_probe_random_noise_persist);
        persist.setSummary(R.string.summary_probe_random_noise_persist);
        persist.setKey(RandomNoiseProbe.PERSIST);
        screen.addPreference(persist);
        persist.setDefaultValue(RandomNoiseProbe.DEFAULT_PERSIST);

        return screen;
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        map.put(RandomNoiseProbe.PERSIST, prefs.getBoolean(RandomNoiseProbe.PERSIST, false));

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(RandomNoiseProbe.PERSIST))
        {
            Object retain = params.get(RandomNoiseProbe.PERSIST);

            if (retain instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(RandomNoiseProbe.PERSIST, (Boolean) retain);
                e.commit();
            }
        }
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = new JSONObject();

        try
        {
            JSONArray values = new JSONArray();
            values.put(true);
            values.put(false);

            JSONObject enabled = new JSONObject();
            enabled.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            enabled.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_ENABLED, enabled);

            JSONObject persist = new JSONObject();
            persist.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            persist.put(Probe.PROBE_VALUES, values);
            settings.put(RandomNoiseProbe.PERSIST, persist);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    public String assetPath(Context context)
    {
        return "random-noise-probe.html";
    }
}
