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
public class LightProbe extends Continuous1DProbe implements SensorEventListener
{
    public static final String DB_TABLE = "light_probe";

    private static final String LIGHT_KEY = "LUX";

    private static final String DEFAULT_THRESHOLD = "10.0";

    public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.LightProbe";

    private static final String THRESHOLD = "config_probe_light_threshold";
    private static final String USE_HANDLER = "config_probe_light_built_in_handler";

    private static int BUFFER_SIZE = 128;

    private static String[] fieldNames = { LIGHT_KEY };

    private double _lastValue = Double.MAX_VALUE;

    private long lastThresholdLookup = 0;
    private double lastThreshold = 10.0;

    private final int accuracyBuffer[] = new int[BUFFER_SIZE];
    private final float valueBuffer[][] = new float[1][BUFFER_SIZE];
    private final double timeBuffer[] = new double[BUFFER_SIZE];
    private final double sensorTimeBuffer[] = new double[BUFFER_SIZE];

    private Map<String, String> _schema = null;

    private int bufferIndex = 0;

    private int _lastFrequency = -1;

    private static Handler _handler = null;

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
            this._schema = new HashMap<>();

            this._schema.put(LightProbe.LIGHT_KEY, ProbeValuesProvider.REAL_TYPE);
        }

        return this._schema;
    }

    @Override
    public Bundle formattedBundle(Context context, Bundle bundle)
    {
        Bundle formatted = super.formattedBundle(context, bundle);

        double[] eventTimes = bundle.getDoubleArray(ContinuousProbe.EVENT_TIMESTAMP);
        float[] lux = bundle.getFloatArray(LightProbe.LIGHT_KEY);

        ArrayList<String> keys = new ArrayList<>();

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
    }

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

        final SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        final Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean("config_probe_light_built_in_enabled", ContinuousProbe.DEFAULT_ENABLED))
            {
                int frequency = Integer.parseInt(prefs.getString("config_probe_light_built_in_frequency", ContinuousProbe.DEFAULT_FREQUENCY));

                if (this._lastFrequency != frequency)
                {
                    sensors.unregisterListener(this, sensor);

                    if (LightProbe._handler != null)
                    {
                        Looper loop = LightProbe._handler.getLooper();
                        loop.quit();

                        LightProbe._handler = null;
                    }

                    if (frequency != SensorManager.SENSOR_DELAY_FASTEST && frequency != SensorManager.SENSOR_DELAY_UI &&
                            frequency != SensorManager.SENSOR_DELAY_NORMAL)
                    {
                        frequency = SensorManager.SENSOR_DELAY_GAME;
                    }

                    if (prefs.getBoolean(LightProbe.USE_HANDLER, ContinuousProbe.DEFAULT_USE_HANDLER))
                    {
                        final LightProbe me = this;
                        final int finalFrequency = frequency;

                        Runnable r = new Runnable()
                        {
                            public void run()
                            {
                                Looper.prepare();

                                LightProbe._handler = new Handler();

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                                    sensors.registerListener(me, sensor, finalFrequency, 0, LightProbe._handler);
                                else
                                    sensors.registerListener(me, sensor, finalFrequency, LightProbe._handler);

                                Looper.loop();
                            }
                        };

                        Thread t = new Thread(r, "light");
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

                if (LightProbe._handler != null)
                {
                    Looper loop = LightProbe._handler.getLooper();
                    loop.quit();

                    LightProbe._handler = null;
                }
            }
        }
        else
        {
            sensors.unregisterListener(this, sensor);
            this._lastFrequency = -1;

            if (LightProbe._handler != null)
            {
                Looper loop = LightProbe._handler.getLooper();
                loop.quit();

                LightProbe._handler = null;
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

        if (params.containsKey(LightProbe.USE_HANDLER))
        {
            Object handler = params.get(LightProbe.USE_HANDLER);

            if (handler instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(LightProbe.USE_HANDLER, (Boolean) handler);
                e.commit();
            }
        }
    }

    @Override
    public PreferenceScreen preferenceScreen(final Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = super.preferenceScreen(context, manager);

        FlexibleListPreference threshold = new FlexibleListPreference(context);
        threshold.setKey(LightProbe.THRESHOLD);
        threshold.setDefaultValue(LightProbe.DEFAULT_THRESHOLD);
        threshold.setEntryValues(R.array.probe_light_threshold);
        threshold.setEntries(R.array.probe_light_threshold_labels);
        threshold.setTitle(R.string.probe_noise_threshold_label);
        threshold.setSummary(R.string.probe_noise_threshold_summary);

        screen.addPreference(threshold);

        CheckBoxPreference handler = new CheckBoxPreference(context);
        handler.setTitle(R.string.title_own_sensor_handler);
        handler.setKey(LightProbe.USE_HANDLER);
        handler.setDefaultValue(ContinuousProbe.DEFAULT_USE_HANDLER);

        screen.addPreference(handler);

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
                sensorTimeBuffer[bufferIndex] = event.timestamp;
                timeBuffer[bufferIndex] = now / 1000;
                valueBuffer[0][bufferIndex] = event.values[0];
                accuracyBuffer[bufferIndex] = event.accuracy;

                double[] plotValues = { timeBuffer[0] / 1000, valueBuffer[0][bufferIndex] };

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

                    data.putDoubleArray(ContinuousProbe.EVENT_TIMESTAMP, timeBuffer);
                    data.putDoubleArray(ContinuousProbe.SENSOR_TIMESTAMP, sensorTimeBuffer);
                    data.putIntArray(ContinuousProbe.SENSOR_ACCURACY, accuracyBuffer);

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
                                light = (double) valueBuffer[i][j];
                        }

                        if (light != null)
                        {
                            Map<String, Object> values = new HashMap<>();

                            values.put(LightProbe.LIGHT_KEY, light);

                            values.put(ProbeValuesProvider.TIMESTAMP, timeBuffer[j] / 1000);

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
            settings.put(LightProbe.USE_HANDLER, handler);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }
}
