package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class SignificantMotionProbe extends Probe
{
    private static final String EVENT_TIME = "EVENT_TIME";

    private static final String ENABLED = "config_probe_significant_motion_built_in_enabled";
    private static final boolean DEFAULT_ENABLED = false;

    private Context _context;

    private TriggerEventListener _trigger = null;

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_sensor_category);
    }

    public Map<String, String> databaseSchema()
    {
        HashMap<String, String> schema = new HashMap<String, String>();

        schema.put(SignificantMotionProbe.EVENT_TIME, ProbeValuesProvider.INTEGER_TYPE);

        return schema;
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.SignificantMotionProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_significant_motion_probe);
    }

    @SuppressLint("InlinedApi")
    @Override
    public boolean isEnabled(final Context context)
    {
        final SignificantMotionProbe me = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            if (this._trigger == null)
            {
                this._trigger = new TriggerEventListener()
                {
                    @Override
                    public void onTrigger(TriggerEvent event)
                    {
                        Sensor sensor = event.sensor;

                        double now = System.currentTimeMillis();

                        double elapsed = SystemClock.uptimeMillis();
                        double boot = (now - elapsed) * 1000 * 1000;

                        double timestamp = event.timestamp + boot;

                        Bundle data = new Bundle();

                        Bundle sensorBundle = new Bundle();
                        sensorBundle.putFloat("MAXIMUM_RANGE", sensor.getMaximumRange());
                        sensorBundle.putString("NAME", sensor.getName());
                        sensorBundle.putFloat("POWER", sensor.getPower());
                        sensorBundle.putFloat("RESOLUTION", sensor.getResolution());
                        sensorBundle.putInt("TYPE", sensor.getType());
                        sensorBundle.putString("VENDOR", sensor.getVendor());
                        sensorBundle.putInt("VERSION", sensor.getVersion());

                        data.putString("PROBE", me.name(context));

                        data.putBundle("SENSOR", sensorBundle);
                        data.putDouble("TIMESTAMP", now / 1000);
                        data.putDouble("EVENT_TIMESTAMP", timestamp);

                        me.transmitData(me._context, data);

                        me.isEnabled(context);
                    }
                };
            }

            SharedPreferences prefs = ContinuousProbe.getPreferences(context);

            this._context = context.getApplicationContext();

            SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

            if (super.isEnabled(context) && prefs.getBoolean(SignificantMotionProbe.ENABLED, SignificantMotionProbe.DEFAULT_ENABLED) && sensor != null)
            {
                if (sensor != null)
                    sensors.requestTriggerSensor(me._trigger, sensor);

                return true;
            }
            else if (sensor != null)
                sensors.cancelTriggerSensor(me._trigger, sensor);
        }

        return false;
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(SignificantMotionProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(SignificantMotionProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        long timestamp = (long) bundle.getDouble("TIMESTAMP") * 1000;

        Date date = new Date(timestamp);

        return context.getString(R.string.summary_significant_motion_probe, date.toString());
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_significant_motion_probe_desc);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(PreferenceActivity activity)
    {
        PreferenceManager manager = activity.getPreferenceManager();

        PreferenceScreen screen = manager.createPreferenceScreen(activity);
        screen.setTitle(this.title(activity));
        screen.setSummary(R.string.summary_significant_motion_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(SignificantMotionProbe.ENABLED);
        enabled.setDefaultValue(SignificantMotionProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        return screen;
    }

    @SuppressLint("InlinedApi")
    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = new JSONObject();

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) == false)
        {
            SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

            if (sensor == null)
                return settings;
        }
        else
            return settings;

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
}
