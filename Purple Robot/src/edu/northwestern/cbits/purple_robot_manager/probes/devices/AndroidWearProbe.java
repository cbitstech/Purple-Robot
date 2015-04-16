package edu.northwestern.cbits.purple_robot_manager.probes.devices;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

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

import java.util.Date;
import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.AndroidWearService;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.calibration.AndroidWearCalibrationHelper;
import edu.northwestern.cbits.purple_robot_manager.calibration.PebbleCalibrationHelper;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ContinuousProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.devices.wear.WearBatteryProbe;

public class AndroidWearProbe extends Probe implements DataApi.DataListener
{
    public static final String ENABLED = "config_probe_android_wear_enabled";
    public static final boolean DEFAULT_ENABLED = false;
    public static final String URI_READING_PREFIX = "/purple-robot-reading";

    public static final String ACCELEROMETER_ENABLED = "config_probe_android_wear_accelerometer_enabled";
    public static final String GYROSCOPE_ENABLED = "config_probe_android_wear_gyroscope_enabled";
    public static final String MAGNETOMETER_ENABLED = "config_probe_android_wear_magnetometer_enabled";
    public static final String LIGHT_METER_ENABLED = "config_probe_android_wear_light_meter_enabled";
    public static final String HEART_METER_ENABLED = "config_probe_android_wear_heart_meter_enabled";
    public static final String LIVEWELL_COUNTS_ENABLED = "config_probe_android_wear_livewell_enabled";

    public static final boolean ACCELEROMETER_DEFAULT_ENABLED = true;
    public static final boolean GYROSCOPE_DEFAULT_ENABLED = false;
    public static final boolean MAGNETOMETER_DEFAULT_ENABLED = false;
    public static final boolean LIGHT_METER_DEFAULT_ENABLED = false;
    public static final boolean HEART_METER_DEFAULT_ENABLED = false;
    public static final boolean LIVEWELL_COUNTS_DEFAULT_ENABLED = false;

    public static final String ACCELEROMETER_FREQUENCY = "config_probe_android_wear_accelerometer_frequency";
    public static final String GYROSCOPE_FREQUENCY = "config_probe_android_wear_gyroscope_frequency";
    public static final String MAGNETOMETER_FREQUENCY = "config_probe_android_wear_magnetometer_frequency";
    public static final String LIGHT_METER_FREQUENCY = "config_probe_android_wear_light_meter_frequency";
    public static final String HEART_METER_FREQUENCY = "config_probe_android_wear_heart_meter_frequency";
    public static final String LIVEWELL_BIN_SIZE = "config_probe_android_wear_livewell_bin_size";

    public static final String LIVEWELL_DEFAULT_BIN_SIZE = "60";

    private static final String INTERVAL = "config_probe_android_wear_interval";
    private static final String DEFAULT_INTERVAL = "300000";

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
    public PreferenceScreen preferenceScreen(final Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_android_wear_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(AndroidWearProbe.ENABLED);
        enabled.setDefaultValue(AndroidWearProbe.DEFAULT_ENABLED);
        screen.addPreference(enabled);

        FlexibleListPreference interval = new FlexibleListPreference(context);
        interval.setKey(AndroidWearProbe.INTERVAL);
        interval.setEntryValues(R.array.probe_satellite_frequency_values);
        interval.setEntries(R.array.probe_satellite_frequency_labels);
        interval.setTitle(R.string.probe_frequency_label);
        interval.setDefaultValue(AndroidWearProbe.DEFAULT_INTERVAL);
        screen.addPreference(interval);

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

        PreferenceScreen studyScreen = manager.createPreferenceScreen(context);
        studyScreen.setTitle(R.string.title_studies_options);

        PreferenceScreen livewellScreen = manager.createPreferenceScreen(context);
        livewellScreen.setTitle(R.string.title_studies_livewell_options);

        CheckBoxPreference livewellEnabled = new CheckBoxPreference(context);
        livewellEnabled.setTitle(R.string.title_livewell_wear_enable_sensor);
        livewellEnabled.setKey(AndroidWearProbe.LIVEWELL_COUNTS_ENABLED);
        livewellEnabled.setDefaultValue(AndroidWearProbe.LIVEWELL_COUNTS_DEFAULT_ENABLED);
        livewellScreen.addPreference(livewellEnabled);

        FlexibleListPreference livewellBins = new FlexibleListPreference(context);
        livewellBins.setKey(AndroidWearProbe.LIVEWELL_BIN_SIZE);
        livewellBins.setEntryValues(R.array.probe_livewell_wear_bin_values);
        livewellBins.setEntries(R.array.probe_livewell_wear_bin_labels);
        livewellBins.setTitle(R.string.title_livewell_wear_bin_size);
        livewellBins.setDefaultValue(AndroidWearProbe.LIVEWELL_DEFAULT_BIN_SIZE);
        livewellScreen.addPreference(livewellBins);

        studyScreen.addPreference(livewellScreen);
        screen.addPreference(studyScreen);

        Preference fetchNow = new Preference(context);
        fetchNow.setTitle(R.string.action_request_data);
        fetchNow.setSummary(R.string.action_desc_request_data);

        final AndroidWearProbe me = this;

        fetchNow.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                me._lastRequest = 0;

                return true;
            }
        });

        screen.addPreference(fetchNow);

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
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        double interval = Double.parseDouble(prefs.getString(AndroidWearProbe.INTERVAL, AndroidWearProbe.DEFAULT_INTERVAL));
        map.put(AndroidWearProbe.INTERVAL, interval);

        boolean accelEnabled = prefs.getBoolean(AndroidWearProbe.ACCELEROMETER_ENABLED, AndroidWearProbe.ACCELEROMETER_DEFAULT_ENABLED);
        map.put(AndroidWearProbe.ACCELEROMETER_ENABLED, accelEnabled);

        double accelFreq = Double.parseDouble(prefs.getString(AndroidWearProbe.ACCELEROMETER_FREQUENCY, ContinuousProbe.DEFAULT_FREQUENCY));
        map.put(AndroidWearProbe.ACCELEROMETER_FREQUENCY, accelFreq);

        boolean gyroEnabled = prefs.getBoolean(AndroidWearProbe.GYROSCOPE_ENABLED, AndroidWearProbe.GYROSCOPE_DEFAULT_ENABLED);
        map.put(AndroidWearProbe.GYROSCOPE_ENABLED, gyroEnabled);

        double gyroFreq = Double.parseDouble(prefs.getString(AndroidWearProbe.GYROSCOPE_FREQUENCY, ContinuousProbe.DEFAULT_FREQUENCY));
        map.put(AndroidWearProbe.GYROSCOPE_FREQUENCY, gyroFreq);

        boolean magnetEnabled = prefs.getBoolean(AndroidWearProbe.MAGNETOMETER_ENABLED, AndroidWearProbe.MAGNETOMETER_DEFAULT_ENABLED);
        map.put(AndroidWearProbe.MAGNETOMETER_ENABLED, magnetEnabled);

        double magnetFreq = Double.parseDouble(prefs.getString(AndroidWearProbe.MAGNETOMETER_FREQUENCY, ContinuousProbe.DEFAULT_FREQUENCY));
        map.put(AndroidWearProbe.MAGNETOMETER_FREQUENCY, magnetFreq);

        boolean lightEnabled = prefs.getBoolean(AndroidWearProbe.LIGHT_METER_ENABLED, AndroidWearProbe.LIGHT_METER_DEFAULT_ENABLED);
        map.put(AndroidWearProbe.LIGHT_METER_ENABLED, lightEnabled);

        double lightFreq = Double.parseDouble(prefs.getString(AndroidWearProbe.LIGHT_METER_FREQUENCY, ContinuousProbe.DEFAULT_FREQUENCY));
        map.put(AndroidWearProbe.LIGHT_METER_FREQUENCY, lightFreq);

        boolean heartEnabled = prefs.getBoolean(AndroidWearProbe.HEART_METER_ENABLED, AndroidWearProbe.HEART_METER_DEFAULT_ENABLED);
        map.put(AndroidWearProbe.HEART_METER_ENABLED, heartEnabled);

        double heartFreq = Double.parseDouble(prefs.getString(AndroidWearProbe.HEART_METER_FREQUENCY, ContinuousProbe.DEFAULT_FREQUENCY));
        map.put(AndroidWearProbe.HEART_METER_FREQUENCY, heartFreq);

        boolean livewellEnabled = prefs.getBoolean(AndroidWearProbe.LIVEWELL_COUNTS_ENABLED, AndroidWearProbe.LIVEWELL_COUNTS_DEFAULT_ENABLED);
        map.put(AndroidWearProbe.LIVEWELL_COUNTS_ENABLED, livewellEnabled);

        double livewellBinSize = Double.parseDouble(prefs.getString(AndroidWearProbe.LIVEWELL_BIN_SIZE, AndroidWearProbe.LIVEWELL_DEFAULT_BIN_SIZE));
        map.put(AndroidWearProbe.LIVEWELL_BIN_SIZE, livewellBinSize);

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(AndroidWearProbe.INTERVAL))
        {
            Object frequency = params.get(AndroidWearProbe.INTERVAL);

            if (frequency instanceof Double)
            {
                frequency = Long.valueOf(((Double) frequency).longValue());
            }

            if (frequency instanceof Long)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.INTERVAL, frequency.toString());
                e.commit();
            }
        }

        if (params.containsKey(AndroidWearProbe.ACCELEROMETER_ENABLED))
        {
            Object enabled = params.get(AndroidWearProbe.ACCELEROMETER_ENABLED);

            if (enabled instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(AndroidWearProbe.ACCELEROMETER_ENABLED, ((Boolean) enabled));
                e.commit();
            }
        }

        if (params.containsKey(AndroidWearProbe.GYROSCOPE_ENABLED))
        {
            Object enabled = params.get(AndroidWearProbe.GYROSCOPE_ENABLED);

            if (enabled instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(AndroidWearProbe.GYROSCOPE_ENABLED, ((Boolean) enabled));
                e.commit();
            }
        }


        if (params.containsKey(AndroidWearProbe.MAGNETOMETER_ENABLED))
        {
            Object enabled = params.get(AndroidWearProbe.MAGNETOMETER_ENABLED);

            if (enabled instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(AndroidWearProbe.MAGNETOMETER_ENABLED, ((Boolean) enabled));
                e.commit();
            }
        }

        if (params.containsKey(AndroidWearProbe.LIGHT_METER_ENABLED))
        {
            Object enabled = params.get(AndroidWearProbe.LIGHT_METER_ENABLED);

            if (enabled instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(AndroidWearProbe.LIGHT_METER_ENABLED, ((Boolean) enabled));
                e.commit();
            }
        }

        if (params.containsKey(AndroidWearProbe.HEART_METER_ENABLED))
        {
            Object enabled = params.get(AndroidWearProbe.HEART_METER_ENABLED);

            if (enabled instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(AndroidWearProbe.HEART_METER_ENABLED, ((Boolean) enabled));
                e.commit();
            }
        }

        if (params.containsKey(AndroidWearProbe.LIVEWELL_COUNTS_ENABLED))
        {
            Object enabled = params.get(AndroidWearProbe.LIVEWELL_COUNTS_ENABLED);

            if (enabled instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(AndroidWearProbe.LIVEWELL_COUNTS_ENABLED, ((Boolean) enabled));
                e.commit();
            }
        }

        if (params.containsKey(AndroidWearProbe.LIVEWELL_BIN_SIZE))
        {
            Object frequency = params.get(Probe.PROBE_FREQUENCY);

            if (frequency instanceof Long || frequency instanceof Integer)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.LIVEWELL_BIN_SIZE, frequency.toString());
                e.commit();
            }
            if (frequency instanceof Double)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.LIVEWELL_BIN_SIZE, "" + ((Double) frequency).intValue());
                e.commit();
            }
            else if (frequency instanceof String)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.LIVEWELL_BIN_SIZE, frequency.toString());
                e.commit();
            }
        }

        if (params.containsKey(AndroidWearProbe.HEART_METER_FREQUENCY))
        {
            Object frequency = params.get(AndroidWearProbe.HEART_METER_FREQUENCY);

            if (frequency instanceof Long || frequency instanceof Integer)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.HEART_METER_FREQUENCY, frequency.toString());
                e.commit();
            }
            if (frequency instanceof Double)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.HEART_METER_FREQUENCY, "" + ((Double) frequency).intValue());
                e.commit();
            }
            else if (frequency instanceof String)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.HEART_METER_FREQUENCY, frequency.toString());
                e.commit();
            }
        }

        if (params.containsKey(AndroidWearProbe.LIGHT_METER_FREQUENCY))
        {
            Object frequency = params.get(AndroidWearProbe.LIGHT_METER_FREQUENCY);

            if (frequency instanceof Long || frequency instanceof Integer)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.LIGHT_METER_FREQUENCY, frequency.toString());
                e.commit();
            }
            if (frequency instanceof Double)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.LIGHT_METER_FREQUENCY, "" + ((Double) frequency).intValue());
                e.commit();
            }
            else if (frequency instanceof String)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.LIGHT_METER_FREQUENCY, frequency.toString());
                e.commit();
            }
        }

        if (params.containsKey(AndroidWearProbe.MAGNETOMETER_FREQUENCY))
        {
            Object frequency = params.get(AndroidWearProbe.MAGNETOMETER_FREQUENCY);

            if (frequency instanceof Long || frequency instanceof Integer)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.MAGNETOMETER_FREQUENCY, frequency.toString());
                e.commit();
            }
            if (frequency instanceof Double)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.MAGNETOMETER_FREQUENCY, "" + ((Double) frequency).intValue());
                e.commit();
            }
            else if (frequency instanceof String)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.MAGNETOMETER_FREQUENCY, frequency.toString());
                e.commit();
            }
        }

        if (params.containsKey(AndroidWearProbe.GYROSCOPE_FREQUENCY))
        {
            Object frequency = params.get(AndroidWearProbe.GYROSCOPE_FREQUENCY);

            if (frequency instanceof Long || frequency instanceof Integer)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.GYROSCOPE_FREQUENCY, frequency.toString());
                e.commit();
            }
            if (frequency instanceof Double)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.GYROSCOPE_FREQUENCY, "" + ((Double) frequency).intValue());
                e.commit();
            }
            else if (frequency instanceof String)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.GYROSCOPE_FREQUENCY, frequency.toString());
                e.commit();
            }
        }

        if (params.containsKey(AndroidWearProbe.ACCELEROMETER_FREQUENCY))
        {
            Object frequency = params.get(AndroidWearProbe.ACCELEROMETER_FREQUENCY);

            if (frequency instanceof Long || frequency instanceof Integer)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.ACCELEROMETER_FREQUENCY, frequency.toString());
                e.commit();
            }
            if (frequency instanceof Double)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.ACCELEROMETER_FREQUENCY, "" + ((Double) frequency).intValue());
                e.commit();
            }
            else if (frequency instanceof String)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(AndroidWearProbe.ACCELEROMETER_FREQUENCY, frequency.toString());
                e.commit();
            }
        }
    }

    @Override
    public boolean isEnabled(Context context)
    {
        this._context = context.getApplicationContext();
        SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(AndroidWearProbe.ENABLED, AndroidWearProbe.DEFAULT_ENABLED))
            {
                AndroidWearCalibrationHelper.check(context, true);

                long now = System.currentTimeMillis();

                long interval = Long.parseLong(prefs.getString(AndroidWearProbe.INTERVAL, AndroidWearProbe.DEFAULT_INTERVAL));

                if (now - this._lastRequest > interval)
                {
                    this._lastRequest = now;

                    AndroidWearService.requestDataFromDevices(context);
                }

                return true;
            }
        }

        AndroidWearCalibrationHelper.check(context, false);

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

                    String probeName = dataMap.getString("PROBE", "");

                    if (probeName.equals(WearBatteryProbe.NAME))
                    {
                        SanityManager sanity = SanityManager.getInstance(this._context);
                        String name = this._context.getString(R.string.name_sanity_wear_battery);

                        int level = dataMap.getInt("BATTERY_LEVEL", Integer.MAX_VALUE);

                        if (level < 30)
                        {
                            String message = this._context.getString(R.string.name_sanity_wear_battery_warning);

                            sanity.addAlert(SanityCheck.WARNING, name, message, null);
                        }
                        else
                            sanity.clearAlert(name);
                    }

                    Wearable.DataApi.deleteDataItems(this._apiClient, item.getUri());
                }
            }
            else if (event.getType() == DataEvent.TYPE_DELETED)
            {

            }
        }
    }
}
