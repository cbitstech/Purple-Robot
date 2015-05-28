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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.RealTimeProbeViewActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

@SuppressLint("SimpleDateFormat")
public class AccelerometerProbe extends Continuous3DProbe implements SensorEventListener
{
    private static int BUFFER_SIZE = 1024;

    public static final String DB_TABLE = "accelerometer_probe";

    private static final String DEFAULT_THRESHOLD = "0.5";

    public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.AccelerometerProbe";

    private static final String FREQUENCY = "config_probe_accelerometer_built_in_frequency";
    private static final String ENABLED = "config_probe_accelerometer_built_in_enabled";
    private static final String THRESHOLD = "config_probe_accelerometer_built_in_threshold";
    private static final String USE_HANDLER = "config_probe_accelerometer_built_in_handler";

    private long lastThresholdLookup = 0;
    private double lastThreshold = 0.5;

    private double _lastX = Double.MAX_VALUE;
    private double _lastY = Double.MAX_VALUE;
    private double _lastZ = Double.MAX_VALUE;

    private final float valueBuffer[][] = new float[3][BUFFER_SIZE];
    private final int accuracyBuffer[] = new int[BUFFER_SIZE];
    private final double timeBuffer[] = new double[BUFFER_SIZE];
    private final double sensorTimeBuffer[] = new double[BUFFER_SIZE];

    private Map<String, String> _schema = null;

    private int _lastFrequency = -1;

    private int bufferIndex = 0;

    private static Handler _handler = null;

    @Override
    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_sensor_category);
    }

    @Override
    public Map<String, String> databaseSchema()
    {
        if (this._schema == null)
        {
            this._schema = new HashMap<>();

            this._schema.put(Continuous3DProbe.X_KEY, ProbeValuesProvider.REAL_TYPE);
            this._schema.put(Continuous3DProbe.Y_KEY, ProbeValuesProvider.REAL_TYPE);
            this._schema.put(Continuous3DProbe.Z_KEY, ProbeValuesProvider.REAL_TYPE);
        }

        return this._schema;
    }

    @Override
    public Bundle formattedBundle(Context context, Bundle bundle)
    {
        Bundle formatted = super.formattedBundle(context, bundle);

        double[] eventTimes = bundle.getDoubleArray(ContinuousProbe.EVENT_TIMESTAMP);
        double[] x = bundle.getDoubleArray(Continuous3DProbe.X_KEY);
        double[] y = bundle.getDoubleArray(Continuous3DProbe.Y_KEY);
        double[] z = bundle.getDoubleArray(Continuous3DProbe.Z_KEY);

        ArrayList<String> keys = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.display_date_format));

        if (x == null || y == null || z == null)
        {

        }
        else if (eventTimes.length > 1)
        {
            Bundle readings = new Bundle();

            for (int i = 0; i < eventTimes.length; i++)
            {
                String formatString = String.format(context.getString(R.string.display_gyroscope_reading), x[i], y[i], z[i]);

                double time = eventTimes[i];

                Date d = new Date((long) time);

                String key = sdf.format(d);

                readings.putString(key, formatString);

                keys.add(key);
            }

            if (keys.size() > 0)
                readings.putStringArrayList("KEY_ORDER", keys);

            formatted.putBundle(context.getString(R.string.display_accelerometer_readings), readings);
        }
        else if (eventTimes.length > 0)
        {
            String formatString = String.format(context.getString(R.string.display_gyroscope_reading), x[0], y[0], z[0]);

            double time = eventTimes[0];

            Date d = new Date((long) time);

            formatted.putString(sdf.format(d), formatString);
        }

        return formatted;
    }

    @Override
    public long getFrequency()
    {
        SharedPreferences prefs = ContinuousProbe.getPreferences(this._context);

        return Long.parseLong(prefs.getString(AccelerometerProbe.FREQUENCY, ContinuousProbe.DEFAULT_FREQUENCY));
    }

    @Override
    public String name(Context context)
    {
        return AccelerometerProbe.NAME;
    }

    @Override
    public int getTitleResource()
    {
        return R.string.title_accelerometer_probe;
    }

    @Override
    public boolean isEnabled(Context context)
    {
        SharedPreferences prefs = ContinuousProbe.getPreferences(context);

        this._context = context.getApplicationContext();

        final SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        final Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(AccelerometerProbe.ENABLED, ContinuousProbe.DEFAULT_ENABLED))
            {
                int frequency = Integer.parseInt(prefs.getString(AccelerometerProbe.FREQUENCY, ContinuousProbe.DEFAULT_FREQUENCY));

                if (this._lastFrequency != frequency)
                {
                    sensors.unregisterListener(this, sensor);

                    if (AccelerometerProbe._handler != null)
                    {
                        Looper loop = AccelerometerProbe._handler.getLooper();
                        loop.quit();

                        AccelerometerProbe._handler = null;
                    }

                    if (frequency != SensorManager.SENSOR_DELAY_FASTEST && frequency != SensorManager.SENSOR_DELAY_UI &&
                        frequency != SensorManager.SENSOR_DELAY_NORMAL)
                    {
                        frequency = SensorManager.SENSOR_DELAY_GAME;
                    }

                    if (prefs.getBoolean(AccelerometerProbe.USE_HANDLER, ContinuousProbe.DEFAULT_USE_HANDLER))
                    {
                        final AccelerometerProbe me = this;
                        final int finalFrequency = frequency;

                        Runnable r = new Runnable()
                        {
                            public void run()
                            {
                                Looper.prepare();

                                AccelerometerProbe._handler = new Handler();

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                                    sensors.registerListener(me, sensor, finalFrequency, 0, AccelerometerProbe._handler);
                                else
                                    sensors.registerListener(me, sensor, finalFrequency, AccelerometerProbe._handler);

                                Looper.loop();
                            }
                        };

                        Thread t = new Thread(r, "accelerometer");
                        t.start();
                    }
                    else
                    {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                            sensors.registerListener(this, sensor, frequency, 0);
                        else
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

                if (AccelerometerProbe._handler != null)
                {
                    Looper loop = AccelerometerProbe._handler.getLooper();
                    loop.quit();

                    AccelerometerProbe._handler = null;
                }
            }
        }
        else
        {
            sensors.unregisterListener(this, sensor);
            this._lastFrequency = -1;

            if (AccelerometerProbe._handler != null)
            {
                Looper loop = AccelerometerProbe._handler.getLooper();
                loop.quit();

                AccelerometerProbe._handler = null;
            }
        }

        return false;
    }

    @Override
    protected boolean passesThreshold(SensorEvent event)
    {
        long now = System.currentTimeMillis();

        if (now - this.lastThresholdLookup > 5000)
        {
            SharedPreferences prefs = Probe.getPreferences(this._context);

            this.lastThreshold = Double.parseDouble(prefs.getString(AccelerometerProbe.THRESHOLD, AccelerometerProbe.DEFAULT_THRESHOLD));

            this.lastThresholdLookup = now;
        }

        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];

        boolean passes = false;

        if (Math.abs(x - this._lastX) >= this.lastThreshold)
            passes = true;
        else if (Math.abs(y - this._lastY) >= this.lastThreshold)
            passes = true;
        else if (Math.abs(z - this._lastZ) >= this.lastThreshold)
            passes = true;

        if (passes)
        {
            this._lastX = x;
            this._lastY = y;
            this._lastZ = z;
        }

        return passes;
    }

    @Override
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = super.preferenceScreen(context, manager);

        FlexibleListPreference threshold = new FlexibleListPreference(context);
        threshold.setKey(AccelerometerProbe.THRESHOLD);
        threshold.setDefaultValue(AccelerometerProbe.DEFAULT_THRESHOLD);
        threshold.setEntryValues(R.array.probe_accelerometer_threshold);
        threshold.setEntries(R.array.probe_accelerometer_threshold_labels);
        threshold.setTitle(R.string.probe_noise_threshold_label);
        threshold.setSummary(R.string.probe_noise_threshold_summary);

        screen.addPreference(threshold);

        CheckBoxPreference handler = new CheckBoxPreference(context);
        handler.setTitle(R.string.title_own_sensor_handler);
        handler.setKey(AccelerometerProbe.USE_HANDLER);
        handler.setDefaultValue(ContinuousProbe.DEFAULT_USE_HANDLER);

        screen.addPreference(handler);

        return screen;
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        final double now = (double) System.currentTimeMillis();

        if (!this.shouldProcessEvent(event))
            return;

        if (this.passesThreshold(event))
        {
            synchronized (this)
            {
                sensorTimeBuffer[bufferIndex] = event.timestamp;
                timeBuffer[bufferIndex] = now / 1000;

                accuracyBuffer[bufferIndex] = event.accuracy;

                for (int i = 0; i < event.values.length; i++)
                {
                    valueBuffer[i][bufferIndex] = event.values[i];
                }

                double[] plotValues = { timeBuffer[0] / 1000,
                                        valueBuffer[0][bufferIndex],
                                        valueBuffer[1][bufferIndex],
                                        valueBuffer[2][bufferIndex] };

                RealTimeProbeViewActivity.plotIfVisible(this.getTitleResource(), plotValues);

                bufferIndex += 1;

                if (bufferIndex >= timeBuffer.length)
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

                    double[] normalBuffer = new double[sensorTimeBuffer.length];

                    double start = sensorTimeBuffer[0];
                    double end = sensorTimeBuffer[sensorTimeBuffer.length - 1];
                    double tick = (end - start) / sensorTimeBuffer.length;

                    start = timeBuffer[0] * (1000 * 1000);

                    for (int i = 0; i < normalBuffer.length; i++)
                    {
                        normalBuffer[i] = (start + (tick * i)) / (1000 * 1000);
                    }

                    data.putDoubleArray(ContinuousProbe.EVENT_TIMESTAMP, timeBuffer);
                    data.putDoubleArray(ContinuousProbe.SENSOR_TIMESTAMP, sensorTimeBuffer);
                    data.putDoubleArray(ContinuousProbe.NORMALIZED_TIMESTAMP, normalBuffer);
                    data.putIntArray(ContinuousProbe.SENSOR_ACCURACY, accuracyBuffer);

                    for (int i = 0; i < fieldNames.length; i++)
                    {
                        data.putFloatArray(fieldNames[i], valueBuffer[i]);
                    }

                    this.transmitData(this._context, data);

                    if (timeBuffer.length > 0)
                    {
                        double x = Double.NaN;
                        double y = Double.NaN;
                        double z = Double.NaN;

                        for (int i = 0; i < fieldNames.length; i++)
                        {
                            if (fieldNames[i].equals(Continuous3DProbe.X_KEY))
                                x = valueBuffer[i][0];
                            else if (fieldNames[i].equals(Continuous3DProbe.Y_KEY))
                                y = valueBuffer[i][0];
                            else if (fieldNames[i].equals(Continuous3DProbe.Z_KEY))
                                z = valueBuffer[i][0];
                        }

                        if (!Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(z))
                        {
                            Map<String, Object> values = new HashMap<>(4);

                            values.put(Continuous3DProbe.X_KEY, x);
                            values.put(Continuous3DProbe.Y_KEY, y);
                            values.put(Continuous3DProbe.Z_KEY, z);

                            values.put(ProbeValuesProvider.TIMESTAMP, timeBuffer[0] / 1000);

                            ProbeValuesProvider.getProvider(this._context).insertValue(this._context, AccelerometerProbe.DB_TABLE, this.databaseSchema(), values);
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
        return "accelerometer_built_in";
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double xReading = bundle.getDoubleArray("X")[0];
        double yReading = bundle.getDoubleArray("Y")[0];
        double zReading = bundle.getDoubleArray("Z")[0];

        return String.format(context.getResources().getString(R.string.summary_accelerator_probe), xReading, yReading, zReading);
    }

    @Override
    public int getSummaryResource()
    {
        return R.string.summary_accelerometer_probe_desc;
    }

    @Override
    protected String tableName()
    {
        return AccelerometerProbe.DB_TABLE;
    }

    @Override
    public double getThreshold()
    {
        SharedPreferences prefs = Probe.getPreferences(this._context);
        return Double.parseDouble(prefs.getString(AccelerometerProbe.THRESHOLD, AccelerometerProbe.DEFAULT_THRESHOLD));
    }

    public void setThreshold(double threshold)
    {
        SharedPreferences prefs = Probe.getPreferences(this._context);

        Editor e = prefs.edit();
        e.putString(AccelerometerProbe.THRESHOLD, "" + threshold);
        e.commit();
    }

    @Override
    protected int getResourceThresholdValues()
    {
        return R.array.probe_accelerometer_threshold;
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = super.fetchSettings(context);

        try
        {
            JSONObject handler = new JSONObject();
            handler.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);

            JSONArray values = new JSONArray();
            values.put(true);
            values.put(false);
            handler.put(Probe.PROBE_VALUES, values);
            settings.put(ContinuousProbe.USE_THREAD, handler);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(ContinuousProbe.USE_THREAD))
        {
            Object handler = params.get(ContinuousProbe.USE_THREAD);

            if (handler instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(AccelerometerProbe.USE_HANDLER, (Boolean) handler);
                e.commit();
            }
        }
    }
}
