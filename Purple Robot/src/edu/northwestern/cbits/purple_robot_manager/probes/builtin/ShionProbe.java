package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class ShionProbe extends Probe
{
    public static final String FETCH_INTENT = "fetch_shion_devices";

    private static final String TYPE = "type";
    // private static final String LOCATION = "location";
    // private static final String ADDRESS = "address";
    // private static final String MODEL = "model";
    private static final String LEVEL = "level";
    // private static final String PLATFORM = "platform";
    // private static final String NAME = "name";
    protected static final String LEVEL_VALUE = "level_value";

    private static final String DEVICES = "devices";

    private static final boolean DEFAULT_ENABLED = false;

    private BroadcastReceiver _receiver = null;

    private long _lastCheck = 0;

    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.ShionProbe";
    }

    public String title(Context context)
    {
        return context.getString(R.string.title_shion_probe);
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

            if (prefs.getBoolean("config_probe_shion_enabled", ShionProbe.DEFAULT_ENABLED))
            {
                if (this._receiver == null)
                {
                    final ShionProbe me = this;

                    this._receiver = new BroadcastReceiver()
                    {
                        public void onReceive(Context context, Intent intent)
                        {
                            Parcelable[] devices = intent.getParcelableArrayExtra(ShionProbe.DEVICES);

                            Bundle controller = null;

                            ArrayList<Bundle> bundleDevices = new ArrayList<Bundle>();

                            for (Parcelable parcelable : devices)
                            {
                                if (parcelable instanceof Bundle)
                                {
                                    Bundle device = (Bundle) parcelable;

                                    if (device.containsKey(ShionProbe.LEVEL))
                                    {
                                        String numberValue = device.getString(ShionProbe.LEVEL);

                                        if (numberValue.startsWith("0"))
                                            device.putDouble(ShionProbe.LEVEL_VALUE, 0);
                                        else
                                            device.putDouble(ShionProbe.LEVEL_VALUE, Double.parseDouble(numberValue));
                                    }

                                    if ("Controller".equalsIgnoreCase(device.getString(ShionProbe.TYPE)))
                                        controller = device;

                                    bundleDevices.add(device);
                                }
                            }

                            Bundle bundle = new Bundle();

                            bundle.putString("PROBE", me.name(context));
                            bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
                            bundle.putInt("DEVICE_COUNT", bundleDevices.size());
                            bundle.putParcelableArrayList("DEVICES", bundleDevices);

                            if (controller != null)
                                bundle.putBundle("CONTROLLER", controller);

                            me.transmitData(context, bundle);
                        }
                    };

                    IntentFilter filter = new IntentFilter();
                    filter.addAction(ShionProbe.FETCH_INTENT);

                    context.registerReceiver(this._receiver, filter);
                }

                final long now = System.currentTimeMillis();

                synchronized (this)
                {
                    long freq = Long
                            .parseLong(prefs.getString("config_probe_shion_frequency", Probe.DEFAULT_FREQUENCY));

                    if (now - this._lastCheck > freq)
                    {
                        this._lastCheck = now;

                        Intent fetchIntent = new Intent(ShionProbe.FETCH_INTENT);

                        context.startService(fetchIntent);
                    }
                }

                return true;
            }
        }

        if (this._receiver != null)
        {
            context.unregisterReceiver(this._receiver);
            this._receiver = null;
        }

        return false;
    }

    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean("config_probe_shion_enabled", true);

        e.commit();
    }

    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean("config_probe_shion_enabled", false);

        e.commit();
    }

    public String summarizeValue(Context context, Bundle bundle)
    {
        int count = (int) bundle.getDouble("DEVICE_COUNT");

        if (bundle.containsKey("CONTROLLER"))
        {
            Bundle controller = bundle.getBundle("CONTROLLER");

            double level = controller.getDouble(ShionProbe.LEVEL_VALUE) / 2.55;

            return context.getString(R.string.summary_shion_probe_controller, level, count);
        }

        return context.getString(R.string.summary_shion_probe, count);
    }

    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString("config_probe_shion_frequency", Probe.DEFAULT_FREQUENCY));

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

                e.putString("config_probe_shion_frequency", frequency.toString());
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
        screen.setSummary(R.string.summary_shion_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey("config_probe_shion_enabled");
        enabled.setDefaultValue(ShionProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        ListPreference duration = new ListPreference(activity);
        duration.setKey("config_probe_shion_frequency");
        duration.setEntryValues(R.array.probe_satellite_frequency_values);
        duration.setEntries(R.array.probe_satellite_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

        // TODO: Add username, password, server, site fields...

        screen.addPreference(duration);

        return screen;
    }

    public String summary(Context context)
    {
        return context.getString(R.string.summary_shion_probe_desc);
    }
}
