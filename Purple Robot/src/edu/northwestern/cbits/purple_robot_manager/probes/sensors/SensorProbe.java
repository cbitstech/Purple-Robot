package edu.northwestern.cbits.purple_robot_manager.probes.sensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.util.WakeLockManager;

public abstract class SensorProbe extends Probe implements SensorEventListener
{
    public static final String SENSOR_MAXIMUM_RANGE = "MAXIMUM_RANGE";
    public static final String SENSOR_NAME = "NAME";
    public static final String SENSOR_POWER = "POWER";
    public static final String SENSOR_TYPE = "TYPE";
    public static final String SENSOR_VENDOR = "VENDOR";
    public static final String SENSOR_VERSION = "VERSION";
    public static final String SENSOR_RESOLUTION = "RESOLUTION";
    public static final String BUNDLE_SENSOR = "SENSOR";
    public static final String SENSOR_ACCURACY = "ACCURACY";

    public static final String EVENT_TIMESTAMP = "EVENT_TIMESTAMP";
    public static final String SENSOR_TIMESTAMP = "SENSOR_TIMESTAMP";
    public static final String NORMALIZED_TIMESTAMP = "NORMALIZED_TIMESTAMP";

    public static final String X = "X";
    public static final String Y = "Y";
    public static final String Z = "Z";

    private static final String PROBE_WAKELOCK = "wakelock";

    private static final boolean DEFAULT_ENABLED = false;
    public static final String DEFAULT_SENSOR_FREQUENCY = "0";
    public static final String DEFAULT_SAMPLE_FREQUENCY = "300000";
    private static final String DEFAULT_WAKELOCK = "-1";
    private static final String DEFAULT_DURATION = "0";

    private int _lastFrequency = -1;
    private int _lastDuration = -1;

    private Handler _handler = null;
    public Context _context = null;
    private PowerManager.WakeLock _wakeLock = null;
    private int _wakeLockLevel = -1;
    private boolean _running = false;
    private long _lastRun = 0;

    public abstract String getPreferenceKey();

    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_sensor_category);
    }

    @Override
    public void enable(Context context) {
        String key = this.getPreferenceKey();

        SharedPreferences prefs = Probe.getPreferences(context);

        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("config_probe_" + key + "_enabled", true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        String key = this.getPreferenceKey();

        SharedPreferences prefs = Probe.getPreferences(context);

        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("config_probe_" + key + "_enabled", false);

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

            int sensorFrequencyValues = this.getResourceSensorFrequencyValues();

            if (sensorFrequencyValues != -1)
            {
                JSONObject frequency = new JSONObject();
                frequency.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
                values = new JSONArray();

                String[] options = context.getResources().getStringArray(sensorFrequencyValues);

                for (String option : options)
                {
                    values.put(Long.parseLong(option));
                }

                frequency.put(Probe.PROBE_VALUES, values);
                settings.put(Probe.PROBE_FREQUENCY, frequency);
            }

            int sampleFrequencyValues = this.getResourceSampleFrequencyValues();

            if (sampleFrequencyValues != -1)
            {
                JSONObject frequency = new JSONObject();
                frequency.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
                values = new JSONArray();

                String[] options = context.getResources().getStringArray(sampleFrequencyValues);

                for (String option : options)
                {
                    values.put(Long.parseLong(option));
                }

                frequency.put(Probe.PROBE_VALUES, values);
                settings.put(Probe.PROBE_SAMPLE_FREQUENCY, frequency);
            }

            int durationValues = this.getResourceDurationValues();

            if (durationValues != -1)
            {
                JSONObject duration = new JSONObject();
                duration.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
                values = new JSONArray();

                String[] options = context.getResources().getStringArray(durationValues);

                for (String option : options)
                {
                    values.put(Long.parseLong(option));
                }

                duration.put(Probe.PROBE_VALUES, values);
                settings.put(Probe.PROBE_DURATION, duration);
            }

            JSONObject wakelock = new JSONObject();
            wakelock.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
            values = new JSONArray();

            String[] options = context.getResources().getStringArray(R.array.wakelock_values);

            for (String option : options)
            {
                values.put(Double.parseDouble(option));
            }

            wakelock.put(Probe.PROBE_VALUES, values);
            settings.put(SensorProbe.PROBE_WAKELOCK, wakelock);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    private int getResourceDurationValues()
    {
        return R.array.probe_duration_with_none_values;
    }

    private int getResourceDurationLabels()
    {
        return R.array.probe_duration_with_none_labels;
    }

    public int getResourceSensorFrequencyLabels()
    {
        return R.array.probe_continuous_frequency_labels;
    }

    public int getResourceSensorFrequencyValues()
    {
        return R.array.probe_continuous_frequency_values;
    }

    public int getResourceSampleFrequencyLabels()
    {
        return R.array.probe_sample_frequency_labels;
    }

    public int getResourceSampleFrequencyValues()
    {
        return R.array.probe_sample_frequency_values;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(Probe.PROBE_FREQUENCY))
        {
            Object frequency = params.get(Probe.PROBE_FREQUENCY);

            if (frequency instanceof Long || frequency instanceof Integer)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_sensor_frequency";

                SharedPreferences prefs = Probe.getPreferences(context);
                SharedPreferences.Editor e = prefs.edit();

                e.putString(key, frequency.toString());
                e.commit();
            }
            if (frequency instanceof Double)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_sensor_frequency";

                SharedPreferences prefs = Probe.getPreferences(context);
                SharedPreferences.Editor e = prefs.edit();

                e.putString(key, "" + ((Double) frequency).intValue());
                e.commit();
            }
            else if (frequency instanceof String)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_sensor_frequency";

                SharedPreferences prefs = Probe.getPreferences(context);
                SharedPreferences.Editor e = prefs.edit();

                e.putString(key, frequency.toString());
                e.commit();
            }
        }

        if (params.containsKey(Probe.PROBE_SAMPLE_FREQUENCY))
        {
            Object frequency = params.get(Probe.PROBE_SAMPLE_FREQUENCY);

            if (frequency instanceof Long || frequency instanceof Integer)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_sample_frequency";

                SharedPreferences prefs = Probe.getPreferences(context);
                SharedPreferences.Editor e = prefs.edit();

                e.putString(key, frequency.toString());
                e.commit();
            }
            if (frequency instanceof Double)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_sample_frequency";

                SharedPreferences prefs = Probe.getPreferences(context);
                SharedPreferences.Editor e = prefs.edit();

                e.putString(key, "" + ((Double) frequency).intValue());
                e.commit();
            }
            else if (frequency instanceof String)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_sample_frequency";

                SharedPreferences prefs = Probe.getPreferences(context);
                SharedPreferences.Editor e = prefs.edit();

                e.putString(key, frequency.toString());
                e.commit();
            }
        }

        if (params.containsKey(Probe.PROBE_DURATION))
        {
            Object duration = params.get(Probe.PROBE_DURATION);

            if (duration instanceof Long || duration instanceof Integer)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_duration";

                SharedPreferences prefs = Probe.getPreferences(context);
                SharedPreferences.Editor e = prefs.edit();

                e.putString(key, duration.toString());
                e.commit();
            }
            if (duration instanceof Double)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_duration";

                SharedPreferences prefs = Probe.getPreferences(context);
                SharedPreferences.Editor e = prefs.edit();

                e.putString(key, "" + ((Double) duration).intValue());
                e.commit();
            }
            else if (duration instanceof String)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_duration";

                SharedPreferences prefs = Probe.getPreferences(context);
                SharedPreferences.Editor e = prefs.edit();

                e.putString(key, duration.toString());
                e.commit();
            }
        }

        if (params.containsKey(SensorProbe.PROBE_WAKELOCK))
        {
            Object wakelock = params.get(SensorProbe.PROBE_WAKELOCK);

            if (wakelock instanceof Long || wakelock instanceof Integer)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_wakelock";

                SharedPreferences prefs = Probe.getPreferences(context);
                SharedPreferences.Editor e = prefs.edit();

                e.putString(key, "" + wakelock);
                e.commit();
            }
            if (wakelock instanceof Double)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_wakelock";

                SharedPreferences prefs = Probe.getPreferences(context);
                SharedPreferences.Editor e = prefs.edit();

                e.putString(key, "" + wakelock);
                e.commit();
            }
            else if (wakelock instanceof String)
            {
                String key = "config_probe_" + this.getPreferenceKey() + "_wakelock";

                SharedPreferences prefs = Probe.getPreferences(context);
                SharedPreferences.Editor e = prefs.edit();

                e.putString(key, "" + wakelock);
                e.commit();
            }
        }
    }

    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        String key = this.getPreferenceKey();

        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(this.summary(context));

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey("config_probe_" + key + "_enabled");
        enabled.setDefaultValue(SensorProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleListPreference frequency = new FlexibleListPreference(context);
        frequency.setKey("config_probe_" + key + "_sensor_frequency");
        frequency.setEntryValues(this.getResourceSensorFrequencyValues());
        frequency.setEntries(this.getResourceSensorFrequencyLabels());
        frequency.setTitle(R.string.probe_sensor_frequency_label);
        frequency.setDefaultValue(SensorProbe.DEFAULT_SENSOR_FREQUENCY);

        screen.addPreference(frequency);

        FlexibleListPreference sample = new FlexibleListPreference(context);
        sample.setKey("config_probe_" + key + "_sample_frequency");
        sample.setEntryValues(this.getResourceSampleFrequencyValues());
        sample.setEntries(this.getResourceSampleFrequencyLabels());
        sample.setTitle(R.string.probe_frequency_label);
        sample.setDefaultValue(SensorProbe.DEFAULT_SAMPLE_FREQUENCY);

        screen.addPreference(sample);

        String[] sampValues = context.getResources().getStringArray(this.getResourceSampleFrequencyValues());

        FlexibleListPreference duration = new FlexibleListPreference(context);
        duration.setKey("config_probe_" + key + "_duration");
        duration.setEntryValues(this.getResourceDurationValues());
        duration.setEntries(this.getResourceDurationLabels());
        duration.setTitle(R.string.probe_duration_label);
        duration.setDefaultValue(SensorProbe.DEFAULT_DURATION);

        screen.addPreference(duration);

        FlexibleListPreference wakelock = new FlexibleListPreference(context);
        wakelock.setKey("config_probe_" + key + "_wakelock");
        wakelock.setEntryValues(R.array.wakelock_values);
        wakelock.setEntries(R.array.wakelock_labels);
        wakelock.setTitle(R.string.probe_wakelock_title);
        wakelock.setSummary(R.string.probe_wakelock_summary);
        wakelock.setDefaultValue(SensorProbe.DEFAULT_WAKELOCK);

        screen.addPreference(wakelock);

        return screen;
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        map.put(Probe.PROBE_FREQUENCY, this.getSensorFrequency());
        map.put(Probe.PROBE_SAMPLE_FREQUENCY, this.getSampleFrequency());
        map.put(Probe.PROBE_DURATION, this.getDuration());
        map.put(SensorProbe.PROBE_WAKELOCK, this.getWakelock());

        return map;
    }

    private int getSensorFrequency()
    {
        String key = this.getPreferenceKey();

        SharedPreferences prefs = Probe.getPreferences(this._context);

        return Integer.parseInt(prefs.getString("config_probe_" + key + "_sensor_frequency", SensorProbe.DEFAULT_SENSOR_FREQUENCY));
    }

    private int getSampleFrequency()
    {
        String key = this.getPreferenceKey();

        SharedPreferences prefs = Probe.getPreferences(this._context);

        return Integer.parseInt(prefs.getString("config_probe_" + key + "_sample_frequency", SensorProbe.DEFAULT_SAMPLE_FREQUENCY));
    }

    protected int getDuration()
    {
        String key = this.getPreferenceKey();

        SharedPreferences prefs = Probe.getPreferences(this._context);

        return Integer.parseInt(prefs.getString("config_probe_" + key + "_duration", SensorProbe.DEFAULT_DURATION));
    }

    private int getWakelock()
    {
        String key = this.getPreferenceKey();

        SharedPreferences prefs = Probe.getPreferences(this._context);

        return Integer.parseInt(prefs.getString("config_probe_" + key + "_wakelock", SensorProbe.DEFAULT_WAKELOCK));
    }

    @Override
    public boolean isEnabled(Context context)
    {
        final SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        final Sensor sensor = sensors.getDefaultSensor(this.getSensorType());

        this._context = context.getApplicationContext();

        if (super.isEnabled(context))
        {
            SharedPreferences prefs = Probe.getPreferences(context);

            String key = this.getPreferenceKey();

            boolean enabled = prefs.getBoolean("config_probe_" + key + "_enabled", SensorProbe.DEFAULT_ENABLED);
            int duration = Integer.parseInt(prefs.getString("config_probe_" + key + "_duration", SensorProbe.DEFAULT_DURATION));
            int sampleFrequency = Integer.parseInt(prefs.getString("config_probe_" + key + "_sample_frequency", SensorProbe.DEFAULT_SAMPLE_FREQUENCY));

            long now = System.currentTimeMillis();

            if (enabled)
            {
                if (this._running == false)
                {
                    int wakeLevel = Integer.parseInt(prefs.getString("config_probe_" + key + "_wakelock", SensorProbe.DEFAULT_WAKELOCK));

                    if (wakeLevel != this._wakeLockLevel) {
                        if (this._wakeLock != null) {
                            WakeLockManager.getInstance(context).releaseWakeLock(this._wakeLock);

                            this._wakeLock = null;
                        }

                        this._wakeLockLevel = wakeLevel;
                    }

                    if (this._wakeLockLevel != -1 && this._wakeLock == null)
                        this._wakeLock = WakeLockManager.getInstance(context).requestWakeLock(this._wakeLockLevel, this.getClass().getCanonicalName());

                    int frequency = Integer.parseInt(prefs.getString("config_probe_" + key + "_sensor_frequency", SensorProbe.DEFAULT_SENSOR_FREQUENCY));

                    if (this._lastFrequency != frequency || this._lastDuration != duration || (now - this._lastRun > sampleFrequency && duration > 0)) {
                        this._lastRun = now;

//                        Log.e("PR-SENSOR", "STARTING SAMPLING " + new Date() + " -- " + sampleFrequency);

                        this._lastFrequency = frequency;
                        this._lastDuration = duration;

                        sensors.unregisterListener(this, sensor);
                        this.clearDataBuffers();

                        if (this._handler != null) {
                            Looper loop = this._handler.getLooper();
                            loop.quit();

                            this._handler = null;
                        }

                        if (frequency != SensorManager.SENSOR_DELAY_FASTEST && frequency != SensorManager.SENSOR_DELAY_UI &&
                                frequency != SensorManager.SENSOR_DELAY_NORMAL) {
                            frequency = SensorManager.SENSOR_DELAY_GAME;
                        }

                        final SensorProbe me = this;
                        final int finalFrequency = frequency;

                        Runnable r = new Runnable() {
                            public void run() {
                                Looper.prepare();

                                me._handler = new Handler();

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                                    sensors.registerListener(me, sensor, finalFrequency, 0, me._handler);
                                else
                                    sensors.registerListener(me, sensor, finalFrequency, me._handler);

                                Looper.loop();
                            }
                        };

                        Thread t = new Thread(r, key);
                        t.start();

                        if (this._lastDuration > 0 && this._running == false)
                        {
//                            Log.e("PR-SENSOR", "SCHEDULING STOP " + new Date() + " -- " + this._lastDuration);

                            this._running = true;

                            Runnable stopCollection = new Runnable() {
                                public void run() {
                                    try {
                                        Thread.sleep(me._lastDuration * 1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                    sensors.unregisterListener(me, sensor);

                                    me.flushData();

                                    if (me._handler != null) {
                                        Looper loop = me._handler.getLooper();
                                        loop.quit();

                                        me._handler = null;
                                    }

                                    if (me._wakeLock != null) {
                                        WakeLockManager.getInstance(me._context).releaseWakeLock(me._wakeLock);
                                        me._wakeLock = null;
                                    }

                                    me._running = false;

//                                    Log.e("PR-SENSOR", "STOPPED " + new Date());
                                }
                            };

                            Thread stopThread = new Thread(stopCollection);
                            stopThread.start();
                        }
                    }
                }

                return true;
            }
            else
            {
                sensors.unregisterListener(this, sensor);

                this.flushData();

                this._lastFrequency = -1;
                this._lastDuration = -1;

                if (this._handler != null)
                {
                    Looper loop = this._handler.getLooper();
                    loop.quit();

                    this._handler = null;
                }

                if (this._wakeLock != null)
                {
                    WakeLockManager.getInstance(context).releaseWakeLock(this._wakeLock);
                    this._wakeLock = null;
                }
            }
        }
        else
        {
            sensors.unregisterListener(this, sensor);

            this.flushData();

            this._lastFrequency = -1;
            this._lastDuration = -1;

            if (this._handler != null)
            {
                Looper loop = this._handler.getLooper();
                loop.quit();

                this._handler = null;
            }

            if (this._wakeLock != null)
            {
                WakeLockManager.getInstance(context).releaseWakeLock(this._wakeLock);
                this._wakeLock = null;
            }
        }

        return false;
    }

    protected abstract void flushData();

    protected abstract int getSensorType();

    protected abstract void clearDataBuffers();

    public abstract void createDataBuffers();

    public void transmitData(Sensor sensor, double timestamp)
    {
        Bundle data = new Bundle();

        Bundle sensorBundle = new Bundle();
        sensorBundle.putFloat(SensorProbe.SENSOR_MAXIMUM_RANGE, sensor.getMaximumRange());
        sensorBundle.putString(SensorProbe.SENSOR_NAME, sensor.getName());
        sensorBundle.putFloat(SensorProbe.SENSOR_POWER, sensor.getPower());
        sensorBundle.putFloat(SensorProbe.SENSOR_RESOLUTION, sensor.getResolution());
        sensorBundle.putInt(SensorProbe.SENSOR_TYPE, sensor.getType());
        sensorBundle.putString(SensorProbe.SENSOR_VENDOR, sensor.getVendor());
        sensorBundle.putInt(SensorProbe.SENSOR_VERSION, sensor.getVersion());

        data.putDouble(Probe.BUNDLE_TIMESTAMP, timestamp);
        data.putString(Probe.BUNDLE_PROBE, this.name(this._context));

        data.putBundle(SensorProbe.BUNDLE_SENSOR, sensorBundle);
        
        this.attachReadings(data);

        this.transmitData(this._context, data);
    }

    protected abstract void attachReadings(Bundle data);
}
