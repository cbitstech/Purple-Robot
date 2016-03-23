package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitLandscapeActivity;
import edu.northwestern.cbits.purple_robot_manager.charts.SplineChart;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class BatteryProbe extends Probe
{
    private static final String DB_TABLE = "battery_probe";

    private static final String BATTERY_KEY = "BATTERY_LEVEL";

    private static final boolean DEFAULT_ENABLED = true;

    private static final String ENABLED = "config_probe_battery_enabled";

    private boolean _isEnabled = false;

    private BroadcastReceiver _receiver = null;
    private long _lastCheck = 0;

    @Override
    public String getPreferenceKey() {
        return "built_in_battery";
    }

    @Override
    public Intent viewIntent(Context context)
    {
        Intent i = new Intent(context, WebkitLandscapeActivity.class);

        return i;
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_device_info_category);
    }

    @Override
    public String contentSubtitle(Context context)
    {
        Cursor c = ProbeValuesProvider.getProvider(context).retrieveValues(context, BatteryProbe.DB_TABLE,
                this.databaseSchema());

        int count = -1;

        if (c != null)
        {
            count = c.getCount();
            c.close();
        }

        return String.format(context.getString(R.string.display_item_count), count);
    }

    public Map<String, String> databaseSchema()
    {
        HashMap<String, String> schema = new HashMap<>();

        schema.put(BatteryProbe.BATTERY_KEY, ProbeValuesProvider.REAL_TYPE);

        return schema;
    }

    @Override
    public String getDisplayContent(Activity activity)
    {
        try
        {
            String template = WebkitActivity.stringForAsset(activity, "webkit/chart_spline_full.html");

            SplineChart c = new SplineChart();

            ArrayList<Double> battery = new ArrayList<>();
            ArrayList<Double> time = new ArrayList<>();

            Cursor cursor = ProbeValuesProvider.getProvider(activity).retrieveValues(activity, BatteryProbe.DB_TABLE,
                    this.databaseSchema());

            int count = -1;

            if (cursor != null)
            {
                count = cursor.getCount();

                while (cursor.moveToNext())
                {
                    double d = cursor.getDouble(cursor.getColumnIndex(BatteryProbe.BATTERY_KEY));
                    double t = cursor.getDouble(cursor.getColumnIndex(ProbeValuesProvider.TIMESTAMP));

                    battery.add(d);
                    time.add(t);
                }

                cursor.close();
            }

            c.addSeries(activity.getString(R.string.battery_level_label), battery);
            c.addTime(activity.getString(R.string.battery_time_label), time);

            JSONObject json = c.dataJson(activity);

            template = template.replace("{{{ highchart_json }}}", json.toString());
            template = template.replace("{{{ highchart_count }}}", "" + count);

            return template;
        }
        catch (IOException | JSONException e)
        {
            LogManager.getInstance(activity).logException(e);
        }

        return null;
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.BatteryProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_battery_probe);
    }

    @Override
    public Bundle formattedBundle(Context context, Bundle bundle)
    {
        Bundle formatted = super.formattedBundle(context, bundle);

        formatted.putString(context.getString(R.string.battery_tech_label),
                bundle.getString(BatteryManager.EXTRA_TECHNOLOGY));
        formatted.putInt(context.getString(R.string.battery_temp_label),
                (int) bundle.getDouble(BatteryManager.EXTRA_TEMPERATURE, -1));
        formatted.putInt(context.getString(R.string.battery_volt_label),
                (int) bundle.getDouble(BatteryManager.EXTRA_VOLTAGE, -1));

        int status = (int) bundle.getDouble(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);

        switch (status)
        {
        case BatteryManager.BATTERY_STATUS_CHARGING:
            formatted.putString(context.getString(R.string.battery_status_label),
                    context.getString(R.string.battery_status_charging));
            break;
        case BatteryManager.BATTERY_STATUS_DISCHARGING:
            formatted.putString(context.getString(R.string.battery_status_label),
                    context.getString(R.string.battery_status_discharging));
            break;
        case BatteryManager.BATTERY_STATUS_FULL:
            formatted.putString(context.getString(R.string.battery_status_label),
                    context.getString(R.string.battery_status_full));
            break;
        case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
            formatted.putString(context.getString(R.string.battery_status_label),
                    context.getString(R.string.battery_status_not_charging));
            break;
        default:
            formatted.putString(context.getString(R.string.battery_status_label),
                    context.getString(R.string.battery_status_unknown));
        }

        int health = (int) bundle.getDouble(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);

        switch (health)
        {
        case BatteryManager.BATTERY_HEALTH_COLD:
            formatted.putString(context.getString(R.string.battery_health_label),
                    context.getString(R.string.battery_health_cold));
            break;
        case BatteryManager.BATTERY_HEALTH_DEAD:
            formatted.putString(context.getString(R.string.battery_health_label),
                    context.getString(R.string.battery_health_dead));
            break;
        case BatteryManager.BATTERY_HEALTH_GOOD:
            formatted.putString(context.getString(R.string.battery_health_label),
                    context.getString(R.string.battery_health_good));
            break;
        case BatteryManager.BATTERY_HEALTH_OVERHEAT:
            formatted.putString(context.getString(R.string.battery_health_label),
                    context.getString(R.string.battery_health_overheat));
            break;
        case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
            formatted.putString(context.getString(R.string.battery_health_label),
                    context.getString(R.string.battery_health_over_voltage));
            break;
        case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
            formatted.putString(context.getString(R.string.battery_health_label),
                    context.getString(R.string.battery_health_failure));
            break;
        default:
            formatted.putString(context.getString(R.string.battery_health_label),
                    context.getString(R.string.battery_health_unknown));
        }

        int source = (int) bundle.getDouble(BatteryManager.EXTRA_PLUGGED, 0);

        switch (source)
        {
        case 0:
            formatted.putString(context.getString(R.string.battery_plugged_label),
                    context.getString(R.string.battery_source_none));
            break;
        case BatteryManager.BATTERY_PLUGGED_AC:
            formatted.putString(context.getString(R.string.battery_plugged_label),
                    context.getString(R.string.battery_source_ac));
            break;
        case BatteryManager.BATTERY_PLUGGED_USB:
            formatted.putString(context.getString(R.string.battery_plugged_label),
                    context.getString(R.string.battery_source_usb));
            break;
        default:
            formatted.putString(context.getString(R.string.battery_plugged_label),
                    context.getString(R.string.battery_source_other));
        }

        return formatted;
    }

    @Override
    public boolean isEnabled(Context context)
    {
        if (this._receiver == null)
        {
            final BatteryProbe me = this;

            this._receiver = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    if (me._isEnabled)
                    {
                        Bundle bundle = new Bundle();
                        bundle.putString("PROBE", me.name(context));
                        bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
                        bundle.putBoolean("PRIORITY", true);

                        bundle.putAll(intent.getExtras());

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        {
                            PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

                            bundle.putBoolean("PRIORITY", power.isPowerSaveMode());
                        }

                        me.transmitData(context, bundle);

                        Map<String, Object> values = new HashMap<>();

                        values.put(BatteryProbe.BATTERY_KEY, bundle.getInt(BatteryManager.EXTRA_LEVEL));
                        values.put(ProbeValuesProvider.TIMESTAMP, (double) bundle.getLong("TIMESTAMP"));

                        ProbeValuesProvider.getProvider(context).insertValue(context, BatteryProbe.DB_TABLE,
                                me.databaseSchema(), values);
                    }
                }
            };
        }

        long now = System.currentTimeMillis();

        if (now - this._lastCheck > 5 * 60 * 1000) {
            this._lastCheck = now;

            try {
                context.unregisterReceiver(this._receiver);
            }
            catch (IllegalArgumentException e)
            {
                // Do nothing - receiver not registered...
            }

            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

            context.registerReceiver(this._receiver, filter);
        }

        SharedPreferences prefs = Probe.getPreferences(context);

        this._isEnabled = false;

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(BatteryProbe.ENABLED, BatteryProbe.DEFAULT_ENABLED))
                this._isEnabled = true;
        }

        return this._isEnabled;
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(BatteryProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context) {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(BatteryProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        String status = this.getStatus(context, (int) bundle.getDouble(BatteryManager.EXTRA_STATUS));

        int level = (int) bundle.getDouble(BatteryManager.EXTRA_LEVEL);

        return String.format(context.getResources().getString(R.string.summary_battery_probe), level, status);
    }

    private String getStatus(Context context, int statusInt)
    {
        switch (statusInt)
        {
        case BatteryManager.BATTERY_STATUS_CHARGING:
            return context.getString(R.string.label_battery_charging);
        case BatteryManager.BATTERY_STATUS_DISCHARGING:
            return context.getString(R.string.label_battery_discharging);
        case BatteryManager.BATTERY_STATUS_FULL:
            return context.getString(R.string.label_battery_full);
        case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
            return context.getString(R.string.label_battery_not_charging);
        }

        return context.getString(R.string.label_unknown);
    }

    /*
     * 
     * public Bundle formattedBundle(Context context, Bundle bundle) { Bundle
     * formatted = super.formattedBundle(context, bundle);
     * 
     * @SuppressWarnings("unchecked") ArrayList<Bundle> array =
     * (ArrayList<Bundle>) bundle.get(HardwareInformationProbe.DEVICES); int
     * count = (int) bundle.getDouble(HardwareInformationProbe.DEVICES_COUNT);
     * 
     * Bundle devicesBundle = this.bundleForDevicesArray(context, array);
     * 
     * formatted.putBundle(String.format(context.getString(R.string.
     * display_bluetooth_devices_title), count), devicesBundle);
     * 
     * return formatted; };
     */

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_battery_probe_desc);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = super.preferenceScreen(context, manager);

        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_battery_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(BatteryProbe.ENABLED);
        enabled.setDefaultValue(BatteryProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        return screen;
    }

    public String assetPath(Context context)
    {
        return "battery-probe.html";
    }
}
