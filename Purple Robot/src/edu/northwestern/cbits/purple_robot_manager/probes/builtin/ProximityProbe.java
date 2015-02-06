package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitLandscapeActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.charts.SplineChart;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

@SuppressLint("SimpleDateFormat")
public class ProximityProbe extends ContinuousProbe implements SensorEventListener
{
    private static int BUFFER_SIZE = 8;

    private static final String DB_TABLE = "proximity_probe";

    private static final String DISTANCE_KEY = "DISTANCE";

    private static final String DEFAULT_THRESHOLD = "5.0";

    public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.ProximityProbe";

    private static final String FREQUENCY = "config_probe_proximity_built_in_frequency";
    private static final String THRESHOLD = "config_probe_proximity_built_in_threshold";
    private static final String ENABLED = "config_probe_proximity_built_in_enabled";

    private static String[] fieldNames =
    { DISTANCE_KEY };

    private double _lastValue = Double.MAX_VALUE;

    private long lastThresholdLookup = 0;
    private double lastThreshold = 5.0;

    private final float valueBuffer[][] = new float[1][BUFFER_SIZE];
    private final int accuracyBuffer[] = new int[BUFFER_SIZE];
    private final double timeBuffer[] = new double[BUFFER_SIZE];
    private final double sensorTimeBuffer[] = new double[BUFFER_SIZE];

    private int bufferIndex = 0;

    private int _lastFrequency = -1;

    @Override
    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_sensor_category);
    }

    @Override
    public Intent viewIntent(Context context)
    {
        Intent i = new Intent(context, WebkitLandscapeActivity.class);

        return i;
    }

    @Override
    public String contentSubtitle(Context context)
    {
        Cursor c = ProbeValuesProvider.getProvider(context).retrieveValues(context, ProximityProbe.DB_TABLE, this.databaseSchema());

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
        HashMap<String, String> schema = new HashMap<String, String>();

        schema.put(ProximityProbe.DISTANCE_KEY, ProbeValuesProvider.REAL_TYPE);

        return schema;
    }

    @Override
    public String getDisplayContent(Activity activity)
    {
        try
        {
            String template = WebkitActivity.stringForAsset(activity, "webkit/chart_spline_full.html");

            ArrayList<Double> distance = new ArrayList<Double>();
            ArrayList<Double> time = new ArrayList<Double>();

            Cursor cursor = ProbeValuesProvider.getProvider(activity).retrieveValues(activity, ProximityProbe.DB_TABLE, this.databaseSchema());

            int count = -1;

            if (cursor != null)
            {
                count = cursor.getCount();

                while (cursor.moveToNext())
                {
                    double d = cursor.getDouble(cursor.getColumnIndex(ProximityProbe.DISTANCE_KEY));
                    double t = cursor.getDouble(cursor.getColumnIndex(ProbeValuesProvider.TIMESTAMP));

                    distance.add(d);
                    time.add(t);
                }

                cursor.close();
            }

            SplineChart c = new SplineChart();
            c.addSeries(activity.getString(R.string.proximity_label), distance);

            c.addTime("tIME", time);

            JSONObject json = c.dataJson(activity);

            template = template.replace("{{{ highchart_json }}}", json.toString());
            template = template.replace("{{{ highchart_count }}}", "" + count);

            return template;
        }
        catch (IOException e)
        {
            LogManager.getInstance(activity).logException(e);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(activity).logException(e);
        }

        return null;
    }

    @Override
    public Bundle formattedBundle(Context context, Bundle bundle)
    {
        Bundle formatted = super.formattedBundle(context, bundle);

        double[] eventTimes = bundle.getDoubleArray(ContinuousProbe.EVENT_TIMESTAMP);
        double[] distance = bundle.getDoubleArray(ProximityProbe.DISTANCE_KEY);

        ArrayList<String> keys = new ArrayList<String>();

        if (distance != null && eventTimes != null)
        {
            SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.display_date_format));

            if (eventTimes.length > 1)
            {
                Bundle readings = new Bundle();

                for (int i = 0; i < eventTimes.length; i++)
                {
                    String formatString = context.getString(R.string.proximity_formatted);

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
                String formatString = "fORMATTED " + distance[0];

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

        return Long.parseLong(prefs.getString(ProximityProbe.FREQUENCY, ContinuousProbe.DEFAULT_FREQUENCY));
    }

    @Override
    public String name(Context context)
    {
        return ProximityProbe.NAME;
    }

    @Override
    public int getTitleResource()
    {
        return R.string.title_proximity_probe;
    }

    @Override
    public boolean isEnabled(Context context)
    {
        SharedPreferences prefs = ContinuousProbe.getPreferences(context);

        this._context = context.getApplicationContext();

        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(ProximityProbe.ENABLED, ContinuousProbe.DEFAULT_ENABLED))
            {
                int frequency = Integer.parseInt(prefs.getString(ProximityProbe.FREQUENCY, ContinuousProbe.DEFAULT_FREQUENCY));

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

        FlexibleListPreference threshold = new FlexibleListPreference(context);
        threshold.setKey("config_probe_proximity_threshold");
        threshold.setDefaultValue(ProximityProbe.DEFAULT_THRESHOLD);
        threshold.setEntryValues(R.array.probe_proximity_threshold);
        threshold.setEntries(R.array.probe_proximity_threshold_labels);
        threshold.setTitle(R.string.probe_noise_threshold_label);
        threshold.setSummary(R.string.probe_noise_threshold_summary);

        screen.addPreference(threshold);

        return screen;
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

                accuracyBuffer[bufferIndex] = event.accuracy;
                valueBuffer[0][bufferIndex] = event.values[0];

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
                        data.putFloatArray(fieldNames[i], valueBuffer[i]);
                    }

                    this.transmitData(this._context, data);

                    for (int j = 0; j < timeBuffer.length; j++)
                    {
                        Double distance = null;

                        for (int i = 0; i < fieldNames.length; i++)
                        {
                            if (fieldNames[i].equals(ProximityProbe.DISTANCE_KEY))
                                distance = Double.valueOf(valueBuffer[i][j]);
                        }

                        if (distance != null)
                        {
                            Map<String, Object> values = new HashMap<String, Object>();

                            values.put(ProximityProbe.DISTANCE_KEY, distance);

                            values.put(ProbeValuesProvider.TIMESTAMP, Double.valueOf(timeBuffer[j] / 1000));

                            ProbeValuesProvider.getProvider(this._context).insertValue(this._context, ProximityProbe.DB_TABLE, this.databaseSchema(), values);
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
        return "proximity_built_in";
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double distance = bundle.getDoubleArray(ProximityProbe.DISTANCE_KEY)[0];

        return String.format(context.getResources().getString(R.string.summary_proximity_value_probe), distance);
    }

    @Override
    public int getSummaryResource()
    {
        return R.string.summary_proximity_probe_desc;
    }

    @Override
    protected double getThreshold()
    {
        SharedPreferences prefs = Probe.getPreferences(this._context);

        return Double.parseDouble(prefs.getString(ProximityProbe.THRESHOLD, ProximityProbe.DEFAULT_THRESHOLD));
    }

    @Override
    protected int getResourceThresholdValues()
    {
        return R.array.probe_proximity_threshold;
    }
}
