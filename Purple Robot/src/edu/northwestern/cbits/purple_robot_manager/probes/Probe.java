package edu.northwestern.cbits.purple_robot_manager.probes;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

public abstract class Probe
{
    public static final String PROBE_READING = "edu.northwestern.cbits.purple_robot.PROBE_READING";
    public static final String PROBE_TRANSMIT_BUFFER = "edu.northwestern.cbits.purple_robot.PROBE_TRANSMIT_BUFFER";

    public static final String PROBE_CALIBRATION_NOTIFICATIONS = "enable_calibration_notifications";
    public static final String PROBE_FREQUENCY = "frequency";
    public static final String PROBE_SAMPLE_FREQUENCY = "sample_frequency";
    public static final String HASH_DATA = "hash_data";

    public static final String DEFAULT_FREQUENCY = "300000";
    public static final boolean DEFAULT_HASH_DATA = true;

    public static final String ENCRYPT_DATA = "encrypt_data";

    public static final String PROBE_ENABLED = "enabled";
    public static final String PROBE_NAME = "name";

    protected static final String BUNDLE_PROBE = "PROBE";
    protected static final String BUNDLE_TIMESTAMP = "TIMESTAMP";

    public static final String PROBE_TYPE_LONG = "long";
    public static final String PROBE_TYPE_BOOLEAN = "boolean";
    public static final String PROBE_TYPE_DOUBLE = "double";
    public static final String PROBE_TYPE_STRING = "string";
    public static final String PROBE_TYPE = "type";
    public static final String PROBE_VALUES = "values";
    public static final String PROBE_DISTANCE = "distance";
    public static final String PROBE_DURATION = "duration";
    public static final String PROBE_MEDIA_URL = "media_url";
    public static final String PROBE_MEDIA_CONTENT_TYPE = "media_content_type";
    public static final String PROBE_MEDIA_SIZE = "media_size";
    public static final String PROBE_MUTE_WARNING = "mute_warning";

    public static final String PROBE_GUID = "GUID";
    public static final String PROBE_DATA = "PROBE_DATA";
    public static final String PROBE_DISPLAY_NAME = "PROBE_DISPLAY_NAME";
    public static final String PROBE_DISPLAY_MESSAGE = "PROBE_DISPLAY_MESSAGE";
    public static final String PROBE_TRANSMIT_MODE = "PROBE_TRANSMIT_MODE";

    public static final int PROBE_TRANSMIT_MODE_NORMAL = 0;
    public static final int PROBE_TRANSMIT_MODE_PRIORITY = 1;
    public static final int PROBE_TRANSMIT_MODE_ON_DEMAND = 2;

    private static List<Class<Probe>> _probeClasses = new ArrayList<>();

    public abstract String name(Context context);

    public abstract String title(Context context);

    public abstract String probeCategory(Context context);

    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);

        FlexibleListPreference threshold = new FlexibleListPreference(context);
        threshold.setKey("config_" + this.getPreferenceKey() + "_transmit_mode");
        threshold.setDefaultValue("" + Probe.PROBE_TRANSMIT_MODE_NORMAL);
        threshold.setEntryValues(R.array.probe_transmit_mode_values);
        threshold.setEntries(R.array.probe_transmit_mode_labels);
        threshold.setTitle(R.string.probe_transmit_mode_label);
        threshold.setSummary(R.string.probe_transmit_mode_summary);

        screen.addPreference(threshold);

        return screen;
    }

    public abstract String summary(Context context);

    private static long _lastEnabledCheck = 0;
    private static boolean _lastEnabled = false;

    private static SharedPreferences _preferences = null;

    public static SharedPreferences getPreferences(Context context)
    {
        if (Probe._preferences == null)
            Probe._preferences = PreferenceManager.getDefaultSharedPreferences(context);

        return Probe._preferences;
    }

    public void nudge(Context context)
    {
        this.isEnabled(context);
    }

    @SuppressWarnings("rawtypes")
    public static void registerProbeClass(Class probeClass)
    {
        if (!Probe._probeClasses.contains(probeClass))
            Probe._probeClasses.add(probeClass);
    }

    @SuppressWarnings("rawtypes")
    public static List<Class<Probe>> availableProbeClasses()
    {
        return Probe._probeClasses;
    }

    public static void loadProbeClasses(Context context)
    {
        String packageName = Probe.class.getPackage().getName();

        String[] probeClasses = context.getResources().getStringArray(R.array.probe_classes);

        for (String className : probeClasses)
        {
            try
            {
                Probe.registerProbeClass(Class.forName(packageName + "." + className));
            }
            catch (ClassNotFoundException e)
            {
                LogManager.getInstance(context).logException(e);
            }
        }
    }

    public static boolean probesEnabled(Context context)
    {
        long now = System.currentTimeMillis();

        if (now - Probe._lastEnabledCheck > 10000)
        {
            Probe._lastEnabledCheck = now;

            SharedPreferences prefs = Probe.getPreferences(context);

            Probe._lastEnabled = prefs.getBoolean("config_probes_enabled", false);
        }

        return Probe._lastEnabled;
    }

    public boolean isEnabled(Context context)
    {
        long now = System.currentTimeMillis();

        if (now - Probe._lastEnabledCheck > 10000)
        {
            Probe._lastEnabledCheck = now;

            SharedPreferences prefs = Probe.getPreferences(context);

            Probe._lastEnabled = prefs.getBoolean("config_probes_enabled", false);
        }

        return Probe._lastEnabled;
    }

    public String summarizeValue(Context context, Bundle bundle)
    {
        return bundle.toString();
    }

    // public abstract void updateFromJSON(Context context, JSONObject json)
    // throws JSONException;

    public Bundle formattedBundle(Context context, Bundle bundle)
    {
        Bundle formatted = new Bundle();

        if (bundle.containsKey("TIMESTAMP"))
        {
            try
            {
                double time = bundle.getDouble("TIMESTAMP");

                if (time == 0)
                    throw new ClassCastException("Catch me.");

                Date d = new Date(((long) time) * 1000);

                formatted.putString(context.getString(R.string.display_date_recorded), d.toString());
            }
            catch (ClassCastException e)
            {
                long time = bundle.getLong("TIMESTAMP");

                Date d = new Date(time * 1000);

                formatted.putString(context.getString(R.string.display_date_recorded), d.toString());
            }
        }

        return formatted;
    }

    protected void transmitData(Context context, Bundle data)
    {
        if (context != null)
        {
            UUID uuid = UUID.randomUUID();
            data.putString(Probe.PROBE_GUID, uuid.toString());

            data.putInt(Probe.PROBE_TRANSMIT_MODE, this.getTransmitMode(context));

            LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
            Intent intent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
            intent.putExtras(data);

            localManager.sendBroadcast(intent);
        }
    }

    public Intent viewIntent(Context context)
    {
        return null;
    }

    public String getDisplayContent(Activity activity)
    {
        return null;
    }

    public String contentSubtitle(Context context)
    {
        return null;
    }

    public abstract void enable(Context context);

    public abstract void disable(Context context);

    public Map<String, Object> configuration(Context context)
    {
        HashMap<String, Object> map = new HashMap<>();

        map.put("name", this.name(context));
        map.put("enabled", this.isEnabled(context));

        return map;
    }

    public void updateFromMap(Context context, Map<String, Object> params)
    {
        if (params.containsKey(Probe.PROBE_ENABLED))
        {
            Object enabled = params.get(Probe.PROBE_ENABLED);

            if (enabled instanceof Boolean)
            {
                if ((Boolean) enabled)
                    this.enable(context);
                else
                    this.disable(context);
            }

            Object transmitMode = params.get(Probe.PROBE_TRANSMIT_MODE);

            if (transmitMode instanceof Integer)
            {
                int intTransmitMode = (Integer) transmitMode;

                if (intTransmitMode < 0 || intTransmitMode > Probe.PROBE_TRANSMIT_MODE_ON_DEMAND)
                    intTransmitMode = Probe.PROBE_TRANSMIT_MODE_NORMAL;

                this.setTransmitMode(context, intTransmitMode);
            }
        }
    }

    private void setTransmitMode(Context context, int transmitMode)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor e = prefs.edit();
        e.putString("config_" + this.getPreferenceKey() + "_transmit_mode", "" + transmitMode);
        e.commit();
    }

    private int getTransmitMode(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String transmitMode = prefs.getString("config_" + this.getPreferenceKey() + "_transmit_mode", "" + Probe.PROBE_TRANSMIT_MODE_NORMAL);

        return Integer.parseInt(transmitMode);
    }

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
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    public JSONObject fetchSettingsFoo(Context context)
    {
        // TODO: Make abstract & implement across ALL probes...

        return null;
    }

    public String shortName(Context context)
    {
        String name = this.name(context);

        String[] components = name.split("\\.");

        return components[components.length - 1];
    }

    public String getMainScreenAction(Context context)
    {
        return null;
    }

    public void runMainScreenAction(Context context)
    {
        Log.e("PR", "Unimplemented main screen action for probe " + this.title(context) + "...");
    }

    public abstract String getPreferenceKey();

    // Override in subclasses as documentation is completed...

    public String assetPath(Context context)
    {
        return null;
    }
}
