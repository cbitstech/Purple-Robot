package edu.northwestern.cbits.purple_robot_manager.probes.devices;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.northwestern.cbits.purple_robot_manager.AndroidWearService;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ContinuousProbe;

public class AndroidWearProbe extends Probe implements DataApi.DataListener
{
    private static final String ENABLED = "config_probe_android_wear_enabled";
    private static final boolean DEFAULT_ENABLED = false;
    public static final String URI_READING_PREFIX = "/purple-robot-reading";

    public static final String ACCELEROMETER_ENABLED = "config_probe_android_wear_accelerometer_enabled";
    public static final String GYROSCOPE_ENABLED = "config_probe_android_wear_gyroscope_enabled";
    public static final String MAGNETOMETER_ENABLED = "config_probe_android_wear_magnetometer_enabled";
    public static final String LIGHT_METER_ENABLED = "config_probe_android_wear_light_meter_enabled";
    public static final String HEART_METER_ENABLED = "config_probe_android_wear_heart_meter_enabled";

    public static final boolean ACCELEROMETER_DEFAULT_ENABLED = true;
    public static final boolean GYROSCOPE_DEFAULT_ENABLED = false;
    public static final boolean MAGNETOMETER_DEFAULT_ENABLED = false;
    public static final boolean LIGHT_METER_DEFAULT_ENABLED = false;
    public static final boolean HEART_METER_DEFAULT_ENABLED = false;

    public static final String ACCELEROMETER_FREQUENCY = "config_probe_android_wear_accelerometer_frequency";
    public static final String GYROSCOPE_FREQUENCY = "config_probe_android_wear_gyroscope_frequency";
    public static final String MAGNETOMETER_FREQUENCY = "config_probe_android_wear_magnetometer_frequency";
    public static final String LIGHT_METER_FREQUENCY = "config_probe_android_wear_light_meter_frequency";
    public static final String HEART_METER_FREQUENCY = "config_probe_android_wear_heart_meter_frequency";

    private GoogleApiClient _apiClient = null;
    private Context _context = null;
    private long _lastRequest = 0;

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.devices.AndroidWearProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_android_wear_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_other_devices_category);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_android_wear_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(AndroidWearProbe.ENABLED);
        enabled.setDefaultValue(AndroidWearProbe.DEFAULT_ENABLED);
        screen.addPreference(enabled);

        PreferenceScreen accel = manager.createPreferenceScreen(context);
        accel.setTitle(R.string.title_android_wear_probe_accel_screen);

        CheckBoxPreference accelEnabled = new CheckBoxPreference(context);
        accelEnabled.setTitle(R.string.title_sensor_enable_sensor);
        accelEnabled.setKey(AndroidWearProbe.ACCELEROMETER_ENABLED);
        accelEnabled.setDefaultValue(AndroidWearProbe.ACCELEROMETER_DEFAULT_ENABLED);
        accel.addPreference(accelEnabled);

        FlexibleListPreference accelFrequency = new FlexibleListPreference(context);
        accelFrequency.setKey(AndroidWearProbe.ACCELEROMETER_FREQUENCY);
        accelFrequency.setEntryValues(R.array.probe_continuous_frequency_values);
        accelFrequency.setEntries(R.array.probe_continuous_frequency_labels);
        accelFrequency.setTitle(R.string.probe_frequency_label);
        accelFrequency.setDefaultValue(ContinuousProbe.DEFAULT_FREQUENCY);
        accel.addPreference(accelFrequency);

        screen.addPreference(accel);

        PreferenceScreen gyro = manager.createPreferenceScreen(context);
        gyro.setTitle(R.string.title_android_wear_probe_gyro_screen);

        CheckBoxPreference gyroEnabled = new CheckBoxPreference(context);
        gyroEnabled.setTitle(R.string.title_sensor_enable_sensor);
        gyroEnabled.setKey(AndroidWearProbe.GYROSCOPE_ENABLED);
        gyroEnabled.setDefaultValue(AndroidWearProbe.GYROSCOPE_DEFAULT_ENABLED);
        gyro.addPreference(gyroEnabled);

        FlexibleListPreference gyroFrequency = new FlexibleListPreference(context);
        gyroFrequency.setKey(AndroidWearProbe.GYROSCOPE_FREQUENCY);
        gyroFrequency.setEntryValues(R.array.probe_continuous_frequency_values);
        gyroFrequency.setEntries(R.array.probe_continuous_frequency_labels);
        gyroFrequency.setTitle(R.string.probe_frequency_label);
        gyroFrequency.setDefaultValue(ContinuousProbe.DEFAULT_FREQUENCY);
        gyro.addPreference(gyroFrequency);

        screen.addPreference(gyro);

        PreferenceScreen magnet = manager.createPreferenceScreen(context);
        magnet.setTitle(R.string.title_android_wear_probe_magnet_screen);

        CheckBoxPreference magnetEnabled = new CheckBoxPreference(context);
        magnetEnabled.setTitle(R.string.title_sensor_enable_sensor);
        magnetEnabled.setKey(AndroidWearProbe.MAGNETOMETER_ENABLED);
        magnetEnabled.setDefaultValue(AndroidWearProbe.MAGNETOMETER_DEFAULT_ENABLED);
        magnet.addPreference(magnetEnabled);

        FlexibleListPreference magnetFrequency = new FlexibleListPreference(context);
        magnetFrequency.setKey(AndroidWearProbe.MAGNETOMETER_FREQUENCY);
        magnetFrequency.setEntryValues(R.array.probe_continuous_frequency_values);
        magnetFrequency.setEntries(R.array.probe_continuous_frequency_labels);
        magnetFrequency.setTitle(R.string.probe_frequency_label);
        magnetFrequency.setDefaultValue(ContinuousProbe.DEFAULT_FREQUENCY);
        magnet.addPreference(magnetFrequency);

        screen.addPreference(magnet);

        PreferenceScreen light = manager.createPreferenceScreen(context);
        light.setTitle(R.string.title_android_wear_probe_light_screen);

        CheckBoxPreference lightEnabled = new CheckBoxPreference(context);
        lightEnabled.setTitle(R.string.title_sensor_enable_sensor);
        lightEnabled.setKey(AndroidWearProbe.LIGHT_METER_ENABLED);
        lightEnabled.setDefaultValue(AndroidWearProbe.LIGHT_METER_DEFAULT_ENABLED);
        light.addPreference(lightEnabled);

        FlexibleListPreference lightFrequency = new FlexibleListPreference(context);
        lightFrequency.setKey(AndroidWearProbe.LIGHT_METER_FREQUENCY);
        lightFrequency.setEntryValues(R.array.probe_continuous_frequency_values);
        lightFrequency.setEntries(R.array.probe_continuous_frequency_labels);
        lightFrequency.setTitle(R.string.probe_frequency_label);
        lightFrequency.setDefaultValue(ContinuousProbe.DEFAULT_FREQUENCY);
        light.addPreference(lightFrequency);

        screen.addPreference(light);

        PreferenceScreen heart = manager.createPreferenceScreen(context);
        heart.setTitle(R.string.title_android_wear_probe_heart_screen);

        CheckBoxPreference heartEnabled = new CheckBoxPreference(context);
        heartEnabled.setTitle(R.string.title_sensor_enable_sensor);
        heartEnabled.setKey(AndroidWearProbe.HEART_METER_ENABLED);
        heartEnabled.setDefaultValue(AndroidWearProbe.HEART_METER_DEFAULT_ENABLED);
        heart.addPreference(heartEnabled);

        FlexibleListPreference heartFrequency = new FlexibleListPreference(context);
        heartFrequency.setKey(AndroidWearProbe.HEART_METER_FREQUENCY);
        heartFrequency.setEntryValues(R.array.probe_continuous_frequency_values);
        heartFrequency.setEntries(R.array.probe_continuous_frequency_labels);
        heartFrequency.setTitle(R.string.probe_frequency_label);
        heartFrequency.setDefaultValue(ContinuousProbe.DEFAULT_FREQUENCY);
        heart.addPreference(heartFrequency);

        screen.addPreference(heart);

        return screen;
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_android_wear_probe_desc);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(AndroidWearProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(AndroidWearProbe.ENABLED, false);

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
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    @Override
    public boolean isEnabled(Context context)
    {
        this._context = context.getApplicationContext();
        SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            Log.e("PR", "WEAR ENABLED: " + prefs.getBoolean(AndroidWearProbe.ENABLED, AndroidWearProbe.DEFAULT_ENABLED));

            if (prefs.getBoolean(AndroidWearProbe.ENABLED, AndroidWearProbe.DEFAULT_ENABLED))
            {
                long now = System.currentTimeMillis();

                if (now - this._lastRequest > 1000 * 60)
                {
                    Log.e("PR", "WEAR REQUEST");

                    this._lastRequest = now;

                    AndroidWearService.requestDataFromDevices(context);
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents)
    {
        for (DataEvent event : dataEvents)
        {
            if (event.getType() == DataEvent.TYPE_CHANGED)
            {
                DataItem item = event.getDataItem();

                if (item.getUri().getPath().compareTo(AndroidWearProbe.URI_READING_PREFIX) == 0)
                {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                    this.transmitData(this._context, dataMap.toBundle());

                    Wearable.DataApi.deleteDataItems(this._apiClient, item.getUri());
                }
            }
            else if (event.getType() == DataEvent.TYPE_DELETED)
            {

            }
        }
    }
}
