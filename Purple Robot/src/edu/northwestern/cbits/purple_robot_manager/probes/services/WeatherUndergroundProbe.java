package edu.northwestern.cbits.purple_robot_manager.probes.services;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.WiFiHelper;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleEditTextPreference;
import edu.northwestern.cbits.purple_robot_manager.calibration.WeatherUndergroundCalibrationHelper;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.plugins.DataUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.FusedLocationProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.RawLocationProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.features.Feature;

public class WeatherUndergroundProbe extends Feature
{
    private static final String FEATURE_KEY = "weather_underground";
    protected static final String OBS_TIMESTAMP = "OBS_TIMESTAMP";
    protected static final String WEATHER = "WEATHER";
    protected static final String TEMPERATURE = "TEMPERATURE";
    protected static final String WIND_DEGREES = "WIND_DEGREES";
    protected static final String GUST_SPEED = "GUST_SPEED";
    protected static final String PRESSURE_TREND = "PRESSURE_TREND";
    protected static final String VISIBILITY = "VISIBILITY";
    protected static final String WIND_SPEED = "WIND_SPEED";
    protected static final String DEWPOINT = "DEWPOINT";
    protected static final String WIND_DIR = "WIND_DIR";
    protected static final String PRESSURE = "PRESSURE";
    protected static final String STATION_ID = "STATION_ID";
    protected static final String LOCATION = "LOCATION";

    public static final String ENABLED = "config_feature_weather_underground_enabled";
    public static final boolean DEFAULT_ENABLED = false;

    public static final String API_KEY = "config_last_weather_underground_api_key";
    public static final String DEFAULT_API_KEY = "";

    public static final String LAST_CHECK = "config_last_weather_underground_check";
    private static final String PROBE_API_KEY = "api_key";

    private boolean _isInited = false;
    private boolean _isEnabled = false;

    @Override
    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_external_services_category);
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        String weather = bundle.getString(WeatherUndergroundProbe.WEATHER);
        String station = bundle.getString(WeatherUndergroundProbe.STATION_ID);
        double temp = bundle.getDouble(WeatherUndergroundProbe.TEMPERATURE);

        return context.getResources().getString(R.string.summary_weather_underground_probe, weather, temp, station);
    }

    @Override
    protected String featureKey()
    {
        return WeatherUndergroundProbe.FEATURE_KEY;
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_weather_underground_feature_desc);
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.features.WeatherUndergroundFeature";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_weather_underground_feature);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(WeatherUndergroundProbe.ENABLED, true);

        e.commit();
    }

    private long lastCheck(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        return prefs.getLong(WeatherUndergroundProbe.LAST_CHECK, 0);
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(WeatherUndergroundProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public boolean isEnabled(Context context)
    {
        if (!this._isInited)
        {
            IntentFilter intentFilter = new IntentFilter(Probe.PROBE_READING);

            final WeatherUndergroundProbe me = this;

            BroadcastReceiver receiver = new BroadcastReceiver()
            {
                @Override
                public void onReceive(final Context context, Intent intent)
                {
                    Bundle extras = intent.getExtras();

                    String probeName = extras.getString(Probe.BUNDLE_PROBE);
                    SharedPreferences prefs = Probe.getPreferences(context);

                    final String apiKey = prefs.getString(WeatherUndergroundProbe.API_KEY, WeatherUndergroundProbe.DEFAULT_API_KEY);

                    if (probeName != null && ((LocationProbe.NAME.equals(probeName)) || RawLocationProbe.NAME.equals(probeName) || FusedLocationProbe.NAME.equals(probeName)) && apiKey != null && apiKey.trim().length() > 0)
                    {
                        long now = System.currentTimeMillis();

                        if (now - me.lastCheck(context) > (1000 * 60 * 60))
                        {

                            if (prefs.getBoolean(DataUploadPlugin.RESTRICT_TO_WIFI, true))
                            {
                                if (WiFiHelper.wifiAvailable(context) == false)
                                    return;
                            }

                            Editor e = prefs.edit();

                            e.putLong(WeatherUndergroundProbe.LAST_CHECK, now);
                            e.commit();

                            final double latitude = extras.getDouble(LocationProbe.LATITUDE);
                            final double longitude = extras.getDouble(LocationProbe.LONGITUDE);

                            Runnable r = new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    try
                                    {
                                        URL u = new URL("http://api.wunderground.com/api/" + apiKey + "/conditions/q/" + latitude + "," + longitude + ".json");

                                        HttpURLConnection conn = (HttpURLConnection) u.openConnection();

                                        BufferedInputStream bin = new BufferedInputStream(conn.getInputStream());
                                        ByteArrayOutputStream bout = new ByteArrayOutputStream();

                                        byte[] buffer = new byte[4096];
                                        int read = 0;

                                        while ((read = bin.read(buffer, 0, buffer.length)) != -1)
                                        {
                                            bout.write(buffer, 0, read);
                                        }

                                        bin.close();

                                        String json = new String(bout.toByteArray(), "UTF-8");

                                        JSONObject conditions = new JSONObject(json);
                                        JSONObject obs = conditions.getJSONObject("current_observation");

                                        String location = obs.getJSONObject("observation_location").getString("full");
                                        String stationId = obs.getString("station_id");

                                        long obsTimestamp = Long.parseLong(obs.getString("observation_epoch"));

                                        String weather = obs.getString("weather");

                                        double temp = Double.parseDouble(obs.getString("temp_c"));

                                        String windDir = obs.getString("wind_dir");
                                        double windDegrees = obs.getDouble("wind_degrees");
                                        double windSpeed = obs.getDouble("wind_kph");
                                        double gustSpeed = obs.getDouble("wind_gust_kph");

                                        double dewPoint = obs.getDouble("dewpoint_c");
                                        double visiblility = obs.getDouble("visibility_km");

                                        double pressure = Double.parseDouble(obs.getString("pressure_mb"));
                                        String pressureTrend = obs.getString("pressure_trend");

                                        Bundle bundle = new Bundle();
                                        bundle.putString(Probe.BUNDLE_PROBE, me.name(context));
                                        bundle.putLong(Probe.BUNDLE_TIMESTAMP, System.currentTimeMillis() / 1000);

                                        bundle.putLong(WeatherUndergroundProbe.OBS_TIMESTAMP, obsTimestamp);
                                        bundle.putString(WeatherUndergroundProbe.STATION_ID, stationId);
                                        bundle.putString(WeatherUndergroundProbe.LOCATION, location);
                                        bundle.putString(WeatherUndergroundProbe.WEATHER, weather);
                                        bundle.putDouble(WeatherUndergroundProbe.TEMPERATURE, temp);
                                        bundle.putString(WeatherUndergroundProbe.WIND_DIR, windDir);
                                        bundle.putDouble(WeatherUndergroundProbe.WIND_DEGREES, windDegrees);
                                        bundle.putDouble(WeatherUndergroundProbe.WIND_SPEED, windSpeed);
                                        bundle.putDouble(WeatherUndergroundProbe.GUST_SPEED, gustSpeed);
                                        bundle.putString(WeatherUndergroundProbe.PRESSURE_TREND, pressureTrend);
                                        bundle.putDouble(WeatherUndergroundProbe.PRESSURE, pressure);

                                        bundle.putDouble(WeatherUndergroundProbe.DEWPOINT, dewPoint);
                                        bundle.putDouble(WeatherUndergroundProbe.VISIBILITY, visiblility);

                                        me.transmitData(context, bundle);
                                    } catch (Exception e)
                                    {
                                        LogManager.getInstance(context).logException(e);
                                    }
                                }
                            };

                            Thread t = new Thread(r);
                            t.start();
                        }
                    }
                }
            };

            LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
            localManager.registerReceiver(receiver, intentFilter);

            SharedPreferences prefs = Probe.getPreferences(context);
            prefs.edit().putLong(WeatherUndergroundProbe.LAST_CHECK, 0).commit();

            this._isInited = true;
        }

        SharedPreferences prefs = Probe.getPreferences(context);

        this._isEnabled = false;

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(WeatherUndergroundProbe.ENABLED, WeatherUndergroundProbe.DEFAULT_ENABLED))
                this._isEnabled = true;
        }

        WeatherUndergroundCalibrationHelper.check(context, this._isEnabled);

        return this._isEnabled;
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = new JSONObject();

        try
        {
            JSONObject enabled = new JSONObject();
            enabled.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            JSONArray values = new JSONArray();
            values.put(true);
            values.put(false);
            enabled.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_ENABLED, enabled);

            JSONObject apiKey = new JSONObject();
            apiKey.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_STRING);
            settings.put(WeatherUndergroundProbe.PROBE_API_KEY, apiKey);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    public String assetPath(Context context)
    {
        return "current-weather-probe.html";
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        map.put(WeatherUndergroundProbe.PROBE_API_KEY, prefs.getString(WeatherUndergroundProbe.API_KEY, WeatherUndergroundProbe.DEFAULT_API_KEY));

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(WeatherUndergroundProbe.PROBE_API_KEY))
        {
            String value = params.get(WeatherUndergroundProbe.PROBE_API_KEY).toString();

            SharedPreferences prefs = Probe.getPreferences(context);
            Editor e = prefs.edit();

            e.putString(WeatherUndergroundProbe.API_KEY, value);
            e.commit();
        }
    }

    @Override
    public PreferenceScreen preferenceScreen(final Context context, PreferenceManager manager)
    {
        final PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(this.summary(context));

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(WeatherUndergroundProbe.ENABLED);
        enabled.setDefaultValue(WeatherUndergroundProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleEditTextPreference apiKey = new FlexibleEditTextPreference(context);
        apiKey.setKey(WeatherUndergroundProbe.API_KEY);
        apiKey.setTitle(R.string.config_weather_underground_api_key_title);
        apiKey.setDialogTitle(R.string.config_weather_underground_api_key_title);
        apiKey.setDialogMessage(R.string.config_weather_underground_api_key_message);
        screen.addPreference(apiKey);

        return screen;
    }
}
