package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.util.WakeLockManager;

public class WakeLockInformationProbe extends Probe
{
    private static final boolean DEFAULT_ENABLED = true;
    private static final String ENABLED = "config_probe_wakelock_enabled";
    private static final String FREQUENCY = "config_probe_wakelock_frequency";

    private static final String PARTIAL_COUNT = "PARTIAL_COUNT";
    private static final String PARTIAL_LOCKS = "PARTIAL_LOCKS";

    private static final String DIM_COUNT = "DIM_COUNT";
    private static final String DIM_LOCKS = "DIM_LOCKS";

    private static final String BRIGHT_COUNT = "BRIGHT_COUNT";
    private static final String BRIGHT_LOCKS = "BRIGHT_LOCKS";

    private static final String FULL_COUNT = "FULL_COUNT";
    private static final String FULL_LOCKS = "FULL_LOCKS";

    private long _lastCheck = 0;

    @Override
    public String getPreferenceKey() {
        return "built_in_wakelock";
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.WakeLockInformationProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_wakelock_info_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_device_info_category);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(WakeLockInformationProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(WakeLockInformationProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public boolean isEnabled(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        long now = System.currentTimeMillis();

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(WakeLockInformationProbe.ENABLED, WakeLockInformationProbe.DEFAULT_ENABLED))
            {
                synchronized (this)
                {
                    long freq = Long.parseLong(prefs.getString(WakeLockInformationProbe.FREQUENCY,
                            Probe.DEFAULT_FREQUENCY));

                    if (now - this._lastCheck > freq)
                    {
                        Bundle bundle = new Bundle();
                        bundle.putString("PROBE", this.name(context));
                        bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                        WakeLockManager locks = WakeLockManager.getInstance(context);

                        ArrayList<Bundle> partialLocks = locks.partialLocks();
                        ArrayList<Bundle> dimLocks = locks.dimLocks();
                        ArrayList<Bundle> brightLocks = locks.brightLocks();
                        ArrayList<Bundle> fullLocks = locks.fullLocks();

                        bundle.putInt(WakeLockInformationProbe.PARTIAL_COUNT, partialLocks.size());
                        bundle.putParcelableArrayList(WakeLockInformationProbe.PARTIAL_LOCKS, partialLocks);

                        bundle.putInt(WakeLockInformationProbe.DIM_COUNT, dimLocks.size());
                        bundle.putParcelableArrayList(WakeLockInformationProbe.DIM_LOCKS, dimLocks);

                        bundle.putInt(WakeLockInformationProbe.BRIGHT_COUNT, brightLocks.size());
                        bundle.putParcelableArrayList(WakeLockInformationProbe.BRIGHT_LOCKS, brightLocks);

                        bundle.putInt(WakeLockInformationProbe.FULL_COUNT, fullLocks.size());
                        bundle.putParcelableArrayList(WakeLockInformationProbe.FULL_LOCKS, fullLocks);

                        this.transmitData(context, bundle);

                        this._lastCheck = now;
                    }
                }

                return true;
            }
        }

        return false;
    }

    /*
    @Override
    public Bundle formattedBundle(Context context, Bundle bundle)
    {
        Bundle formatted = super.formattedBundle(context, bundle);

        formatted.putString(context.getString(R.string.hardware_model_label),
                bundle.getString(WakeLockInformationProbe.MODEL));
        formatted.putString(context.getString(R.string.hardware_mfr_label),
                bundle.getString(WakeLockInformationProbe.MANUFACTURER));
        formatted.putString(context.getString(R.string.hardware_bluetooth_label),
                bundle.getString(WakeLockInformationProbe.BLUETOOTH_MAC));
        formatted.putString(context.getString(R.string.hardware_wifi_label),
                bundle.getString(WakeLockInformationProbe.WIFI_MAC));

        return formatted;
    }
    */

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        int partialCount = (int) bundle.getDouble(WakeLockInformationProbe.PARTIAL_COUNT);
        int dimCount =  (int) bundle.getDouble(WakeLockInformationProbe.DIM_COUNT);
        int brightCount =  (int) bundle.getDouble(WakeLockInformationProbe.BRIGHT_COUNT);
        int fullCount =  (int) bundle.getDouble(WakeLockInformationProbe.FULL_COUNT);

        return String.format(context.getResources().getString(R.string.summary_wakelock_info_probe), partialCount, dimCount, brightCount, fullCount);
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString(WakeLockInformationProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

        map.put(Probe.PROBE_FREQUENCY, freq);

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(Probe.PROBE_FREQUENCY))
        {
            Object frequency = params.get(Probe.PROBE_FREQUENCY);

            if ((frequency instanceof Double) == false)
                frequency = Double.valueOf(frequency.toString()).longValue();
            else
                frequency = ((Double) frequency).longValue();

            SharedPreferences prefs = Probe.getPreferences(context);
            Editor e = prefs.edit();

            e.putString(WakeLockInformationProbe.FREQUENCY, frequency.toString());
            e.commit();
        }
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_wakelock_info_probe_desc);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = super.preferenceScreen(context, manager);

        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_wakelock_info_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(WakeLockInformationProbe.ENABLED);
        enabled.setDefaultValue(WakeLockInformationProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleListPreference duration = new FlexibleListPreference(context);
        duration.setKey(WakeLockInformationProbe.FREQUENCY);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);
        duration.setEntryValues(R.array.probe_low_frequency_values);
        duration.setEntries(R.array.probe_low_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);

        screen.addPreference(duration);

        return screen;
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = super.fetchSettings(context);

        try
        {
            JSONObject frequency = new JSONObject();
            frequency.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
            JSONArray values = new JSONArray();

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
}
