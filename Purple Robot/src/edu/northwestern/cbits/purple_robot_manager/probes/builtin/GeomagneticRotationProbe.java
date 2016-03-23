package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

@SuppressLint("SimpleDateFormat")
public class GeomagneticRotationProbe extends RotationProbe
{
    public static final String DB_TABLE = "geomagnetic_rotation_probe";

    public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.GeomagneticRotationProbe";

    private static final String THRESHOLD = "config_probe_geomagnetic_rotation_built_in_threshold";
    private static final String FREQUENCY = "config_probe_geomagnetic_rotation_built_in_frequency";
    private static final String ENABLED = "config_probe_geomagnetic_rotation_built_in_enabled";
    private static final String USE_HANDLER = "config_probe_geomagnetic_rotation_built_in_handler";

    private static Handler _handler = null;

    @Override
    public String contentSubtitle(Context context)
    {
        Cursor c = ProbeValuesProvider.getProvider(context).retrieveValues(context, GeomagneticRotationProbe.DB_TABLE, this.databaseSchema());

        int count = -1;

        if (c != null)
        {
            count = c.getCount();
            c.close();
        }

        return String.format(context.getString(R.string.display_item_count), count);
    }

    @Override
    public long getFrequency()
    {
        SharedPreferences prefs = ContinuousProbe.getPreferences(this._context);

        return Long.parseLong(prefs.getString(GeomagneticRotationProbe.FREQUENCY, ContinuousProbe.DEFAULT_FREQUENCY));
    }

    @Override
    public String name(Context context)
    {
        return GeomagneticRotationProbe.NAME;
    }

    @Override
    public int getTitleResource()
    {
        return R.string.title_geomagnetic_rotation_probe;
    }

    @Override
    protected String dbTable()
    {
        return GeomagneticRotationProbe.DB_TABLE;
    }

    @Override
    @SuppressLint("InlinedApi")
    public boolean isEnabled(Context context)
    {
        if (Build.VERSION.SDK_INT < 19)
            return false;

        SharedPreferences prefs = ContinuousProbe.getPreferences(context);

        this._context = context.getApplicationContext();

        final SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        final Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);

        if (super.isSuperEnabled(context))
        {
            if (prefs.getBoolean(GeomagneticRotationProbe.ENABLED, ContinuousProbe.DEFAULT_ENABLED))
            {
                int frequency = Integer.parseInt(prefs.getString(GeomagneticRotationProbe.FREQUENCY, ContinuousProbe.DEFAULT_FREQUENCY));

                if (this._lastFrequency != frequency)
                {
                    sensors.unregisterListener(this, sensor);

                    if (GeomagneticRotationProbe._handler != null)
                    {
                        Looper loop = GeomagneticRotationProbe._handler.getLooper();
                        loop.quit();

                        GeomagneticRotationProbe._handler = null;
                    }

                    if (frequency != SensorManager.SENSOR_DELAY_FASTEST && frequency != SensorManager.SENSOR_DELAY_UI &&
                            frequency != SensorManager.SENSOR_DELAY_NORMAL)
                    {
                        frequency = SensorManager.SENSOR_DELAY_GAME;
                    }

                    if (prefs.getBoolean(GeomagneticRotationProbe.USE_HANDLER, ContinuousProbe.DEFAULT_USE_HANDLER))
                    {
                        final GeomagneticRotationProbe me = this;
                        final int finalFrequency = frequency;

                        Runnable r = new Runnable()
                        {
                            public void run()
                            {
                                Looper.prepare();

                                GeomagneticRotationProbe._handler = new Handler();

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                                    sensors.registerListener(me, sensor, finalFrequency, 0, GeomagneticRotationProbe._handler);
                                else
                                    sensors.registerListener(me, sensor, finalFrequency, GeomagneticRotationProbe._handler);

                                Looper.loop();
                            }
                        };

                        Thread t = new Thread(r, "geomagnetic_rotation");
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

                if (GeomagneticRotationProbe._handler != null)
                {
                    Looper loop = GeomagneticRotationProbe._handler.getLooper();
                    loop.quit();

                    GeomagneticRotationProbe._handler = null;
                }
            }
        }
        else
        {
            sensors.unregisterListener(this, sensor);
            this._lastFrequency = -1;

            if (GeomagneticRotationProbe._handler != null)
            {
                Looper loop = GeomagneticRotationProbe._handler.getLooper();
                loop.quit();

                GeomagneticRotationProbe._handler = null;
            }
        }

        return false;
    }

    @Override
    public PreferenceScreen preferenceScreen(final Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = super.preferenceScreen(context, manager);

        screen.removePreference(screen.findPreference(RotationProbe.THRESHOLD));

        FlexibleListPreference threshold = new FlexibleListPreference(context);
        threshold.setKey(GeomagneticRotationProbe.THRESHOLD);
        threshold.setDefaultValue(RotationProbe.DEFAULT_THRESHOLD);
        threshold.setEntryValues(R.array.probe_rotation_threshold);
        threshold.setEntries(R.array.probe_rotation_threshold_labels);
        threshold.setTitle(R.string.probe_noise_threshold_label);
        threshold.setSummary(R.string.probe_noise_threshold_summary);

        screen.addPreference(threshold);

        CheckBoxPreference handler = new CheckBoxPreference(context);
        handler.setTitle(R.string.title_own_sensor_handler);
        handler.setKey(GeomagneticRotationProbe.USE_HANDLER);
        handler.setDefaultValue(ContinuousProbe.DEFAULT_USE_HANDLER);

        screen.addPreference(handler);

        return screen;
    }

    @Override
    public String getPreferenceKey()
    {
        return "geomagnetic_rotation_built_in";
    }

    @Override
    public int getSummaryResource()
    {
        return R.string.summary_geomagnetic_rotation_probe_desc;
    }

    @Override
    protected double getThreshold()
    {
        SharedPreferences prefs = Probe.getPreferences(this._context);

        return Double.parseDouble(prefs.getString(GeomagneticRotationProbe.THRESHOLD, RotationProbe.DEFAULT_THRESHOLD));
    }

    @Override
    protected int getResourceThresholdValues()
    {
        return R.array.probe_rotation_threshold;
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = super.fetchSettings(context);

        if (Build.VERSION.SDK_INT < 19)
            return settings;

        settings = super.fetchSettings(context);

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
                SharedPreferences.Editor e = prefs.edit();

                e.putBoolean(GeomagneticRotationProbe.USE_HANDLER, (Boolean) handler);
                e.commit();
            }
        }
    }
}
