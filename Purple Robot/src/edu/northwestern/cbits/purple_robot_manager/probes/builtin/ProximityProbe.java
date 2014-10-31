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
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitLandscapeActivity;
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

    private static String[] fieldNames =
    { DISTANCE_KEY };

    private double _lastValue = Double.MAX_VALUE;

    private long lastThresholdLookup = 0;
    private double lastThreshold = 5.0;

    private float valueBuffer[][] = new float[1][BUFFER_SIZE];
    private double timeBuffer[] = new double[BUFFER_SIZE];

    private int bufferIndex = 0;

    private int _lastFrequency = -1;

    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_sensor_category);
    }

    public Intent viewIntent(Context context)
    {
        Intent i = new Intent(context, WebkitLandscapeActivity.class);

        return i;
    }

    public String contentSubtitle(Context context)
    {
        Cursor c = ProbeValuesProvider.getProvider(context).retrieveValues(context, ProximityProbe.DB_TABLE,
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
        HashMap<String, String> schema = new HashMap<String, String>();

        schema.put(ProximityProbe.DISTANCE_KEY, ProbeValuesProvider.REAL_TYPE);

        return schema;
    }

    public String getDisplayContent(Activity activity)
    {
        try
        {
            String template = WebkitActivity.stringForAsset(activity, "webkit/chart_spline_full.html");

            ArrayList<Double> distance = new ArrayList<Double>();
            ArrayList<Double> time = new ArrayList<Double>();

            Cursor cursor = ProbeValuesProvider.getProvider(activity).retrieveValues(activity, ProximityProbe.DB_TABLE,
                    this.databaseSchema());

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

    public Bundle formattedBundle(Context context, Bundle bundle)
    {
        Bundle formatted = super.formattedBundle(context, bundle);

        double[] eventTimes = bundle.getDoubleArray("EVENT_TIMESTAMP");
        double[] distance = bundle.getDoubleArray("DISTANCE");

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

    public long getFrequency()
    {
        SharedPreferences prefs = ContinuousProbe.getPreferences(this._context);

        return Long.parseLong(prefs.getString("config_probe_proximity_built_in_frequency",
                ContinuousProbe.DEFAULT_FREQUENCY));
    }

    public String name(Context context)
    {
        return ProximityProbe.NAME;
    }

    public int getTitleResource()
    {
        return R.string.title_proximity_probe;
    }

    public boolean isEnabled(Context context)
    {
        SharedPreferences prefs = ContinuousProbe.getPreferences(context);

        this._context = context.getApplicationContext();

        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean("config_probe_proximity_built_in_enabled", ContinuousProbe.DEFAULT_ENABLED))
            {
                int frequency = Integer.parseInt(prefs.getString("config_probe_proximity_built_in_frequency",
                        ContinuousProbe.DEFAULT_FREQUENCY));

                if (this._lastFrequency != frequency)
                {
                    sensors.unregisterListener(this, sensor);

                    switch (frequency)
                    {
                    case SensorManager.SENSOR_DELAY_FASTEST:
                        sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST, null);
                        break;
                    case SensorManager.SENSOR_DELAY_GAME:
                        sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME, null);
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

    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        map.put(ContinuousProbe.PROBE_THRESHOLD, this.lastThreshold);

        return map;
    }

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

                e.putString("config_probe_proximity_threshold", threshold.toString());
                e.commit();
            }
        }
    }

    public PreferenceScreen preferenceScreen(PreferenceActivity activity)
    {
        PreferenceScreen screen = super.preferenceScreen(activity);

        ListPreference threshold = new ListPreference(activity);
        threshold.setKey("config_probe_proximity_threshold");
        threshold.setDefaultValue(ProximityProbe.DEFAULT_THRESHOLD);
        threshold.setEntryValues(R.array.probe_proximity_threshold);
        threshold.setEntries(R.array.probe_proximity_threshold_labels);
        threshold.setTitle(R.string.probe_noise_threshold_label);
        threshold.setSummary(R.string.probe_noise_threshold_summary);

        screen.addPreference(threshold);

        return screen;
    }

    protected boolean passesThreshold(SensorEvent event)
    {
        long now = System.currentTimeMillis();

        if (now - this.lastThresholdLookup > 5000)
        {
            SharedPreferences prefs = Probe.getPreferences(this._context);
            this.lastThreshold = Double.parseDouble(prefs.getString("config_probe_proximity_threshold",
                    ProximityProbe.DEFAULT_THRESHOLD));

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

                            ProbeValuesProvider.getProvider(this._context).insertValue(this._context,
                                    ProximityProbe.DB_TABLE, this.databaseSchema(), values);
                        }
                    }

                    bufferIndex = 0;
                }
            }
        }
    }

    public String getPreferenceKey()
    {
        return "proximity_built_in";
    }

    public String summarizeValue(Context context, Bundle bundle)
    {
        double distance = bundle.getDoubleArray("DISTANCE")[0];

        return String.format(context.getResources().getString(R.string.summary_proximity_value_probe), distance);
    }

    public int getSummaryResource()
    {
        return R.string.summary_proximity_probe_desc;
    }
}
