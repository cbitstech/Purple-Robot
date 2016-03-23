package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
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
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitLandscapeActivity;
import edu.northwestern.cbits.purple_robot_manager.charts.SplineChart;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class StepCounterProbe extends Probe implements SensorEventListener
{
    private static final String DB_TABLE = "step_counter_probe";

    private static final String STEPS_KEY = "STEPS";

    private static final boolean DEFAULT_ENABLED = false;

    private static final String STEP_COUNT = "STEP_COUNT";

    private static final String ENABLED = "config_probe_step_counter_enabled";

    private float _steps = 0;

    private Context _context;

    @Override
    public String getPreferenceKey() {
        return "built_in_step_counter";
    }

    @Override
    public Intent viewIntent(Context context)
    {
        Intent i = new Intent(context, WebkitLandscapeActivity.class);

        return i;
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_sensor_category);
    }

    @Override
    public String contentSubtitle(Context context)
    {
        Cursor c = ProbeValuesProvider.getProvider(context).retrieveValues(context, StepCounterProbe.DB_TABLE, this.databaseSchema());

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
        HashMap<String, String> schema = new HashMap<>();

        schema.put(StepCounterProbe.STEPS_KEY, ProbeValuesProvider.REAL_TYPE);

        return schema;
    }

    @Override
    public String getDisplayContent(Activity activity)
    {
        try
        {
            String template = WebkitActivity.stringForAsset(activity, "webkit/chart_spline_full.html");

            SplineChart c = new SplineChart();

            ArrayList<Double> battery = new ArrayList<>();
            ArrayList<Double> time = new ArrayList<>();

            Cursor cursor = ProbeValuesProvider.getProvider(activity).retrieveValues(activity, StepCounterProbe.DB_TABLE, this.databaseSchema());

            int count = -1;

            if (cursor != null)
            {
                count = cursor.getCount();

                while (cursor.moveToNext())
                {
                    double d = cursor.getDouble(cursor.getColumnIndex(StepCounterProbe.STEPS_KEY));
                    double t = cursor.getDouble(cursor.getColumnIndex(ProbeValuesProvider.TIMESTAMP));

                    battery.add(d);
                    time.add(t);
                }

                cursor.close();
            }

            c.addSeries(activity.getString(R.string.step_count_label), battery);
            c.addTime(activity.getString(R.string.step_count_time_label), time);

            JSONObject json = c.dataJson(activity);

            template = template.replace("{{{ highchart_json }}}", json.toString());
            template = template.replace("{{{ highchart_count }}}", "" + count);

            return template;
        }
        catch (IOException | JSONException e)
        {
            LogManager.getInstance(activity).logException(e);
        }

        return null;
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.StepCounterProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_step_counter_probe);
    }

    @Override
    @SuppressLint("InlinedApi")
    public boolean isEnabled(Context context)
    {
        if (Build.VERSION.SDK_INT < 19)
            return false;

        this._context = context.getApplicationContext();

        SharedPreferences prefs = ContinuousProbe.getPreferences(context);

        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        sensors.unregisterListener(this, sensor);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(StepCounterProbe.ENABLED, ContinuousProbe.DEFAULT_ENABLED))
            {
                sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL, null);

                return true;
            }
        }

        return false;
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(StepCounterProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(StepCounterProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double steps = bundle.getDouble(StepCounterProbe.STEP_COUNT);

        return String.format(context.getResources().getString(R.string.summary_step_counter_probe), (int) steps);
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_step_counter_probe_desc);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = super.preferenceScreen(context, manager);

        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_step_counter_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(StepCounterProbe.ENABLED);
        enabled.setDefaultValue(StepCounterProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        return screen;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        float steps = event.values[0];

        if (this._steps < steps)
        {
            Bundle bundle = new Bundle();
            bundle.putString("PROBE", this.name(this._context));
            bundle.putLong("TIMESTAMP", event.timestamp / 1000000000);

            bundle.putFloat(StepCounterProbe.STEP_COUNT, steps);
            this.transmitData(this._context, bundle);

            Map<String, Object> values = new HashMap<>();

            values.put(StepCounterProbe.STEPS_KEY, bundle.getFloat("STEPS"));
            values.put(ProbeValuesProvider.TIMESTAMP, (double) bundle.getLong("TIMESTAMP"));

            ProbeValuesProvider.getProvider(this._context).insertValue(this._context, StepCounterProbe.DB_TABLE, this.databaseSchema(), values);

            this._steps = steps;
        }
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = super.fetchSettings(context);

        if (Build.VERSION.SDK_INT < 19)
            return new JSONObject();

        return settings;
    }
}
