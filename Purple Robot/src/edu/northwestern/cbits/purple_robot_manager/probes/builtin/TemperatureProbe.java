package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

@SuppressLint("SimpleDateFormat")
public class TemperatureProbe extends ContinuousProbe implements SensorEventListener
{
    private static final String DEFAULT_THRESHOLD = "1.0";

    public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.TemperatureProbe";

    private static final String THRESHOLD = "config_probe_temperature_built_in_threshold";
    private static final String FREQUENCY = "config_probe_temperature_built_in_frequency";
    private static final String ENABLED = "config_probe_temperature_built_in_enabled";

    private static int BUFFER_SIZE = 32;

    private static String[] fieldNames =
    { "TEMPERATURE" };

    private double _lastValue = Double.MAX_VALUE;

    private long lastThresholdLookup = 0;
    private double lastThreshold = 1.0;

    private final float valueBuffer[][] = new float[1][BUFFER_SIZE];
    private final double timeBuffer[] = new double[BUFFER_SIZE];

    private int bufferIndex = 0;

    private int _lastFrequency = -1;

    @Override
    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_sensor_category);
    }

    @Override
    public Bundle formattedBundle(Context context, Bundle bundle)
    {
        Bundle formatted = super.formattedBundle(context, bundle);

        double[] eventTimes = bundle.getDoubleArray("EVENT_TIMESTAMP");
        double[] temp = bundle.getDoubleArray("TEMPERATURE");

        ArrayList<String> keys = new ArrayList<String>();

        if (temp != null && eventTimes != null)
        {
            SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.display_date_format));

            if (eventTimes.length > 1)
            {
                Bundle readings = new Bundle();

                for (int i = 0; i < eventTimes.length; i++)
                {
                    String formatString = String.format(context.getString(R.string.display_temperature_reading), temp[i]);

                    double time = eventTimes[i];

                    Date d = new Date((long) time);

                    String key = sdf.format(d);

                    readings.putString(key, formatString);

                    keys.add(key);
                }

                if (keys.size() > 0)
                    readings.putStringArrayList("KEY_ORDER", keys);

                formatted.putBundle(context.getString(R.string.display_temperature_readings), readings);
            }
            else if (eventTimes.length > 0)
            {
                String formatString = String.format(context.getString(R.string.display_temperature_reading), temp[0]);

                double time = eventTimes[0];

                Date d = new Date((long) time);

                formatted.putString(sdf.format(d), formatString);
            }
        }

        return formatted;
    };

    @Override
    public long getFrequency()
    {
        SharedPreferences prefs = ContinuousProbe.getPreferences(this._context);

        return Long.parseLong(prefs.getString(TemperatureProbe.FREQUENCY, ContinuousProbe.DEFAULT_FREQUENCY));
    }

    @Override
    public String name(Context context)
    {
        return TemperatureProbe.NAME;
    }

    @Override
    public int getTitleResource()
    {
        return R.string.title_temperature_probe;
    }

    @Override
    @SuppressLint("InlinedApi")
    @SuppressWarnings("deprecation")
    public boolean isEnabled(Context context)
    {
        SharedPreferences prefs = ContinuousProbe.getPreferences(context);

        this._context = context.getApplicationContext();

        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_TEMPERATURE);

        if (Build.VERSION.SDK_INT >= 14)
            sensor = sensors.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(TemperatureProbe.ENABLED, ContinuousProbe.DEFAULT_ENABLED))
            {
                int frequency = Integer.parseInt(prefs.getString(TemperatureProbe.FREQUENCY, ContinuousProbe.DEFAULT_FREQUENCY));

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
    protected boolean passesThreshold(SensorEvent event)
    {
        long now = System.currentTimeMillis();

        if (now - this.lastThresholdLookup > 5000)
        {
            this.lastThreshold = this.getThreshold();

            this.lastThresholdLookup = now;
        }

        double value = event.values[0];

        boolean passes = false;

        if (Math.abs(value - this._lastValue) >= this.lastThreshold)
            passes = true;

        if (passes)
            this._lastValue = value;

        return passes;
    }

    @Override
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = super.preferenceScreen(context, manager);

        ListPreference threshold = new ListPreference(context);
        threshold.setKey(TemperatureProbe.THRESHOLD);
        threshold.setDefaultValue(TemperatureProbe.DEFAULT_THRESHOLD);
        threshold.setEntryValues(R.array.probe_temperature_threshold);
        threshold.setEntries(R.array.probe_temperature_threshold_labels);
        threshold.setTitle(R.string.probe_noise_threshold_label);
        threshold.setSummary(R.string.probe_noise_threshold_summary);

        screen.addPreference(threshold);

        return screen;
    }

    @Override
    @SuppressLint("NewApi")
    public void onSensorChanged(SensorEvent event)
    {
        double now = System.currentTimeMillis();

        if (this.shouldProcessEvent(event) == false)
            return;

        if (this.passesThreshold(event))
        {
            synchronized (this)
            {
                // Using wall clock instead of sensor clock so readings can be compared...
                event.timestamp = ((long) now) * 1000;

/*                double elapsed = (double) SystemClock.uptimeMillis();
                double boot = (now - elapsed) * 1000 * 1000;

                double timestamp = event.timestamp + boot;

                if (timestamp > now * (1000 * 1000) * 1.1) // Used to detect if
                                                           // sensors already
                                                           // have built-in
                                                           // times...
                    timestamp = event.timestamp;
*/
                double timestamp = event.timestamp;

                timeBuffer[bufferIndex] = timestamp / 1000000;
                valueBuffer[0][bufferIndex] = event.values[0];

                bufferIndex += 1;

                if (bufferIndex >= timeBuffer.length)
                {
                    Sensor sensor = event.sensor;

                    Bundle data = new Bundle();

                    Bundle sensorBundle = new Bundle();
                    sensorBundle.putFloat("MAXIMUM_RANGE", sensor.getMaximumRange());
                    sensorBundle.putString("NAME", sensor.getName());
                    sensorBundle.putFloat("POWER", sensor.getPower());
                    sensorBundle.putFloat("RESOLUTION", sensor.getResolution());
                    sensorBundle.putInt("TYPE", sensor.getType());
                    sensorBundle.putString("VENDOR", sensor.getVendor());
                    sensorBundle.putInt("VERSION", sensor.getVersion());

                    data.putString("PROBE", this.name(this._context));

                    data.putBundle("SENSOR", sensorBundle);
                    data.putDouble("TIMESTAMP", now / 1000);

                    data.putDoubleArray("EVENT_TIMESTAMP", timeBuffer);

                    for (int i = 0; i < fieldNames.length; i++)
                    {
                        data.putFloatArray(fieldNames[i], valueBuffer[i]);
                    }

                    this.transmitData(this._context, data);

                    bufferIndex = 0;
                }
            }
        }
    }

    @Override
    public String getPreferenceKey()
    {
        return "temperature_built_in";
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double celsius = bundle.getDoubleArray("TEMPERATURE")[0];
        double faren = (celsius * 1.8) + 32;
        double kelvin = celsius + 273.15;

        return String.format(context.getResources().getString(R.string.summary_temperature_probe), celsius, faren, kelvin);
    }

    @Override
    public int getSummaryResource()
    {
        return R.string.summary_temperature_probe_desc;
    }

    @Override
    protected double getThreshold()
    {
        SharedPreferences prefs = Probe.getPreferences(this._context);

        return Double.parseDouble(prefs.getString(TemperatureProbe.THRESHOLD, TemperatureProbe.DEFAULT_THRESHOLD));
    }

    @Override
    protected int getResourceThresholdValues()
    {
        return R.array.probe_temperature_threshold;
    }
}
