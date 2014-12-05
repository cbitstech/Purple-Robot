package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.RealTimeProbeViewActivity;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

@SuppressLint("SimpleDateFormat")
public class LightProbe extends Continuous1DProbe implements SensorEventListener
{
    public static final String DB_TABLE = "light_probe";

    private static final String LIGHT_KEY = "LUX";

    private static final String DEFAULT_THRESHOLD = "10.0";

    public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.LightProbe";

    private static final String THRESHOLD = "config_probe_light_threshold";

    private static int BUFFER_SIZE = 128;

    private static String[] fieldNames =
    { LIGHT_KEY };

    private double _lastValue = Double.MAX_VALUE;

    private long lastThresholdLookup = 0;
    private double lastThreshold = 10.0;

    private final float valueBuffer[][] = new float[1][BUFFER_SIZE];
    private final double timeBuffer[] = new double[BUFFER_SIZE];

    private Map<String, String> _schema = null;

    private int bufferIndex = 0;

    private int _lastFrequency = -1;

    @Override
    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_sensor_category);
    }

    @Override
    public String contentSubtitle(Context context)
    {
        Cursor c = ProbeValuesProvider.getProvider(context).retrieveValues(context, LightProbe.DB_TABLE, this.databaseSchema());

        int count = -1;

        if (c != null)
        {
            count = c.getCount();
            c.close();
        }

        return String.format(context.getString(R.string.display_item_count), count);
    }

    @Override
    public Map<String, String> databaseSchema()
    {
        if (this._schema == null)
        {
            this._schema = new HashMap<String, String>();

            this._schema.put(LightProbe.LIGHT_KEY, ProbeValuesProvider.REAL_TYPE);
        }

        return this._schema;
    }

    @Override
    public Bundle formattedBundle(Context context, Bundle bundle)
    {
        Bundle formatted = super.formattedBundle(context, bundle);

        double[] eventTimes = bundle.getDoubleArray("EVENT_TIMESTAMP");
        float[] lux = bundle.getFloatArray("LUX");

        ArrayList<String> keys = new ArrayList<String>();

        if (lux != null && eventTimes != null)
        {
            SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.display_date_format));

            if (eventTimes.length > 1)
            {
                Bundle readings = new Bundle();

                for (int i = 0; i < eventTimes.length; i++)
                {
                    String formatString = String.format(context.getString(R.string.display_light_reading), lux[i]);

                    double time = eventTimes[i];

                    Date d = new Date((long) time);

                    String key = sdf.format(d);

                    readings.putString(key, formatString);

                    keys.add(key);
                }

                if (keys.size() > 0)
                    readings.putStringArrayList("KEY_ORDER", keys);

                formatted.putBundle(context.getString(R.string.display_light_readings), readings);
            }
            else if (eventTimes.length > 0)
            {
                String formatString = String.format(context.getString(R.string.display_light_reading), lux[0]);

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

        return Long.parseLong(prefs.getString("config_probe_light_built_in_frequency", ContinuousProbe.DEFAULT_FREQUENCY));
    }

    @Override
    public String name(Context context)
    {
        return LightProbe.NAME;
    }

    @Override
    public int getTitleResource()
    {
        return R.string.title_light_probe;
    }

    @Override
    public boolean isEnabled(Context context)
    {
        SharedPreferences prefs = ContinuousProbe.getPreferences(context);

        this._context = context.getApplicationContext();

        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean("config_probe_light_built_in_enabled", ContinuousProbe.DEFAULT_ENABLED))
            {
                int frequency = Integer.parseInt(prefs.getString("config_probe_light_built_in_frequency", ContinuousProbe.DEFAULT_FREQUENCY));

                if (this._lastFrequency != frequency)
                {
                    sensors.unregisterListener(this, sensor);

                    switch (frequency)
                    {
                    case SensorManager.SENSOR_DELAY_FASTEST:
                        sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST, null);
                        break;
                    case SensorManager.SENSOR_DELAY_UI:
                        sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, null);
                        break;
                    case SensorManager.SENSOR_DELAY_NORMAL:
                        sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL, null);
                        break;
                    default:
                        sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME, null);
                        break;
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
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(ContinuousProbe.PROBE_THRESHOLD))
        {
            Object threshold = params.get(ContinuousProbe.PROBE_THRESHOLD);

            if (threshold instanceof Double)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(LightProbe.THRESHOLD, threshold.toString());
                e.commit();
            }
        }
    }

    @Override
    public PreferenceScreen preferenceScreen(PreferenceActivity activity)
    {
        PreferenceScreen screen = super.preferenceScreen(activity);

        ListPreference threshold = new ListPreference(activity);
        threshold.setKey(LightProbe.THRESHOLD);
        threshold.setDefaultValue(LightProbe.DEFAULT_THRESHOLD);
        threshold.setEntryValues(R.array.probe_light_threshold);
        threshold.setEntries(R.array.probe_light_threshold_labels);
        threshold.setTitle(R.string.probe_noise_threshold_label);
        threshold.setSummary(R.string.probe_noise_threshold_summary);

        screen.addPreference(threshold);

        return screen;
    }

    @Override
    @SuppressLint("NewApi")
    public void onSensorChanged(SensorEvent event)
    {
        if (this.shouldProcessEvent(event) == false)
            return;

        double now = System.currentTimeMillis();

        if (this.passesThreshold(event))
        {
            synchronized (this)
            {
                double elapsed = SystemClock.uptimeMillis();
                double boot = (now - elapsed) * 1000 * 1000;

                double timestamp = event.timestamp + boot;

                if (timestamp > now * (1000 * 1000) * 1.1) // Used to detect if
                                                           // sensors already
                                                           // have built-in
                                                           // times...
                    timestamp = event.timestamp;

                timeBuffer[bufferIndex] = timestamp / 1000000;
                valueBuffer[0][bufferIndex] = event.values[0];

                double[] plotValues =
                { timeBuffer[0] / 1000, valueBuffer[0][bufferIndex] };
                RealTimeProbeViewActivity.plotIfVisible(this.getTitleResource(), plotValues);

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
                        double[] values = new double[valueBuffer[i].length];

                        for (int j = 0; j < values.length; j++)
                            values[j] = valueBuffer[i][j];

                        data.putDoubleArray(fieldNames[i], values);
                    }

                    this.transmitData(this._context, data);

                    for (int j = 0; j < timeBuffer.length; j++)
                    {
                        Double light = null;

                        for (int i = 0; i < fieldNames.length; i++)
                        {
                            if (fieldNames[i].equals(LightProbe.LIGHT_KEY))
                                light = Double.valueOf(valueBuffer[i][j]);
                        }

                        if (light != null)
                        {
                            Map<String, Object> values = new HashMap<String, Object>();

                            values.put(LightProbe.LIGHT_KEY, light);

                            values.put(ProbeValuesProvider.TIMESTAMP, Double.valueOf(timeBuffer[j] / 1000));

                            ProbeValuesProvider.getProvider(this._context).insertValue(this._context, LightProbe.DB_TABLE, this.databaseSchema(), values);
                        }
                    }

                    bufferIndex = 0;
                }
            }
        }
    }

    @Override
    public String getPreferenceKey()
    {
        return "light_built_in";
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double lux = bundle.getDoubleArray(LIGHT_KEY)[0];

        return String.format(context.getResources().getString(R.string.summary_light_probe), lux);
    }

    @Override
    public int getSummaryResource()
    {
        return R.string.summary_light_probe_desc;
    }

    @Override
    protected String tableName()
    {
        return LightProbe.DB_TABLE;
    }

    @Override
    protected double getThreshold()
    {
        SharedPreferences prefs = Probe.getPreferences(this._context);

        return Double.parseDouble(prefs.getString(LightProbe.THRESHOLD, LightProbe.DEFAULT_THRESHOLD));
    }

    @Override
    protected int getResourceThresholdValues()
    {
        return R.array.probe_light_threshold;
    }
}
