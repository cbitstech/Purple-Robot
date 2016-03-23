package edu.northwestern.cbits.purple_robot_manager.probes.studies;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ContinuousProbe;

@SuppressLint("SimpleDateFormat")
public class LivewellActivityCountsProbe extends Probe implements SensorEventListener
{
    public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.studies.LivewellActivityCountsProbe";
    private static final String ENABLED = "config_probe_livewell_activities_enabled";
    private static final boolean DEFAULT_ENABLED = false;
    private static final String FREQUENCY = "config_probe_livewell_activities_frequency";
    public static final String BUNDLE_DURATION = "BUNDLE_DURATION";
    public static final String BUNDLE_NUM_SAMPLES = "BUNDLE_NUM_SAMPLES";
    private static final String BUNDLE_X_DELTA = "BUNDLE_X_DELTA";
    private static final String BUNDLE_Y_DELTA = "BUNDLE_Y_DELTA";
    private static final String BUNDLE_Z_DELTA = "BUNDLE_Z_DELTA";
    public static final String BUNDLE_ALL_DELTA = "BUNDLE_ALL_DELTA";

    private static long BIN_SIZE_DEFAULT = 60000;

    private double _lastX = 0;
    private double _lastY = 0;
    private double _lastZ = 0;

    private double _xSum = 0;
    private double _ySum = 0;
    private double _zSum = 0;

    private int _lastFrequency = -1;
    private Context _context = null;
    private double _lastBinStart = 0;
    private int _numSamples = 0;

    @Override
    public String getPreferenceKey() {
        return "services_livewell_activities";
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_studies_category);
    }

    @Override
    public String name(Context context)
    {
        return LivewellActivityCountsProbe.NAME;
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_livewell_activity_counts_probe);
    }

    @Override
    public boolean isEnabled(Context context)
    {
        this._context = context.getApplicationContext();

        SharedPreferences prefs = ContinuousProbe.getPreferences(context);

        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(LivewellActivityCountsProbe.ENABLED, LivewellActivityCountsProbe.DEFAULT_ENABLED))
            {
                int frequency = Integer.parseInt(prefs.getString(LivewellActivityCountsProbe.FREQUENCY, ContinuousProbe.DEFAULT_FREQUENCY));

                if (this._lastFrequency != frequency)
                {
                    sensors.unregisterListener(this, sensor);

                    if (frequency != SensorManager.SENSOR_DELAY_FASTEST && frequency != SensorManager.SENSOR_DELAY_UI &&
                        frequency != SensorManager.SENSOR_DELAY_NORMAL)
                    {
                        frequency = SensorManager.SENSOR_DELAY_GAME;
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    {
                        sensors.registerListener(this, sensor, frequency, 0);
                    }
                    else
                    {
                        sensors.registerListener(this, sensor, frequency, null);
                    }

                    this._lastFrequency = frequency;
                }

                return true;
            }
            else
            {
                sensors.unregisterListener(this, sensor);
                this._lastFrequency = -1;
            }
        }
        else
        {
            sensors.unregisterListener(this, sensor);
            this._lastFrequency = -1;
        }

        return false;
    }

    @Override
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = super.preferenceScreen(context, manager);

        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_livewell_activity_counts_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(LivewellActivityCountsProbe.ENABLED);
        enabled.setDefaultValue(LivewellActivityCountsProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleListPreference duration = new FlexibleListPreference(context);
        duration.setKey(LivewellActivityCountsProbe.FREQUENCY);
        duration.setEntryValues(R.array.probe_continuous_frequency_values);
        duration.setEntries(R.array.probe_continuous_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);
        duration.setDefaultValue(ContinuousProbe.DEFAULT_FREQUENCY);

        screen.addPreference(duration);

        // TODO: Customizable bin sizes?

        return screen;
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_livewell_activity_counts_probe_desc);
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        final double now = (double) System.currentTimeMillis();

        synchronized (this)
        {
            if (this._lastBinStart == 0)
                this._lastBinStart = now;
            else if (now - this._lastBinStart > LivewellActivityCountsProbe.BIN_SIZE_DEFAULT)
            {
                Sensor sensor = event.sensor;

                Bundle data = new Bundle();

                Bundle sensorBundle = new Bundle();
                sensorBundle.putFloat(ContinuousProbe.SENSOR_MAXIMUM_RANGE, sensor.getMaximumRange());
                sensorBundle.putString(ContinuousProbe.SENSOR_NAME, sensor.getName());
                sensorBundle.putFloat(ContinuousProbe.SENSOR_POWER, sensor.getPower());
                sensorBundle.putFloat(ContinuousProbe.SENSOR_RESOLUTION, sensor.getResolution());
                sensorBundle.putInt(ContinuousProbe.SENSOR_TYPE, sensor.getType());
                sensorBundle.putString(ContinuousProbe.SENSOR_VENDOR, sensor.getVendor());
                sensorBundle.putInt(ContinuousProbe.SENSOR_VERSION, sensor.getVersion());

                data.putDouble(Probe.BUNDLE_TIMESTAMP, now / 1000);
                data.putString(Probe.BUNDLE_PROBE, this.name(this._context));

                data.putBundle(ContinuousProbe.BUNDLE_SENSOR, sensorBundle);

                data.putDouble(LivewellActivityCountsProbe.BUNDLE_X_DELTA, this._xSum);
                data.putDouble(LivewellActivityCountsProbe.BUNDLE_Y_DELTA, this._ySum);
                data.putDouble(LivewellActivityCountsProbe.BUNDLE_Z_DELTA, this._zSum);
                data.putDouble(LivewellActivityCountsProbe.BUNDLE_ALL_DELTA, (this._xSum + this._ySum + this._zSum) / 3);
                data.putDouble(LivewellActivityCountsProbe.BUNDLE_DURATION, now - this._lastBinStart);
                data.putDouble(LivewellActivityCountsProbe.BUNDLE_NUM_SAMPLES, this._numSamples);

                this.transmitData(this._context, data);

                this._lastBinStart = now;

                this._xSum = 0;
                this._ySum = 0;
                this._zSum = 0;
                this._numSamples = 0;
            }

            this._xSum += Math.abs(event.values[0] - this._lastX);
            this._ySum += Math.abs(event.values[1] - this._lastY);
            this._zSum += Math.abs(event.values[2] - this._lastZ);

            this._lastX = event.values[0];
            this._lastY = event.values[1];
            this._lastZ = event.values[2];

            this._numSamples += 1;
        }
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double count = bundle.getDouble(LivewellActivityCountsProbe.BUNDLE_ALL_DELTA);
        double numSamples = bundle.getDouble(LivewellActivityCountsProbe.BUNDLE_NUM_SAMPLES);
        double duration = bundle.getDouble(LivewellActivityCountsProbe.BUNDLE_DURATION);

        return String.format(context.getResources().getString(R.string.summary_livewell_pebble_probe), (duration / 1000), numSamples);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(LivewellActivityCountsProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(LivewellActivityCountsProbe.ENABLED, false);

        e.commit();
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

            String[] options = context.getResources().getStringArray(R.array.probe_continuous_frequency_values);

            for (String option : options)
            {
                values.put(Long.parseLong(option));
            }

            frequency.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_FREQUENCY, frequency);

            // TODO: configurable bin sizes?
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }
}
