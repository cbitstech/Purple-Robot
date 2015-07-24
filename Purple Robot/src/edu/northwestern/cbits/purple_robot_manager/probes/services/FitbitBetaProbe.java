package edu.northwestern.cbits.purple_robot_manager.probes.services;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.xsi.oauth.FitbitBetaApi;
import edu.northwestern.cbits.xsi.oauth.Keystore;
import edu.northwestern.cbits.xsi.oauth.OAuthActivity;

public class FitbitBetaProbe extends Probe
{
    protected static final String STEPS = "STEPS";
    public static final String STEP_TIMESTAMPS = "STEP_TIMESTAMPS";
    protected static final String CALORIES = "CALORIES";
    public static final String CALORIES_TIMESTAMPS = "CALORIES_TIMESTAMPS";
    protected static final String ELEVATION = "ELEVATION";
    public static final String ELEVATION_TIMESTAMPS = "ELEVATION_TIMESTAMPS";
    protected static final String DISTANCE = "DISTANCE";
    public static final String DISTANCE_TIMESTAMPS = "DISTANCE_TIMESTAMPS";
    protected static final String FLOORS = "FLOORS";
    public static final String FLOORS_TIMESTAMPS = "FLOORS_TIMESTAMPS";
    protected static final String HEART = "HEART";
    public static final String HEART_TIMESTAMPS = "HEART_TIMESTAMPS";

    private static final String ENABLED = "config_feature_fitbit_beta_probe_enabled";
    private static final boolean DEFAULT_ENABLED = false;
    private static final String OAUTH_ACCESS_TOKEN = "oauth_fitbit-beta_access_token";
    private static final String OAUTH_REFRESH_TOKEN = "oauth_fitbit-beta_refresh_token";
    private static final String OAUTH_TOKEN_EXPIRES = "oauth_fitbit-beta_expires";

    private static final String ENABLE_STEPS = "config_fitbit-beta_enable_distance";
    private static final boolean DEFAULT_ENABLE_STEPS = true;

    private static final String ENABLE_CALORIES = "config_fitbit-beta_enable_calories";
    private static final boolean DEFAULT_ENABLE_CALORIES = false;

    private static final String ENABLE_DISTANCE = "config_fitbit-beta_enable_distance";
    private static final boolean DEFAULT_ENABLE_DISTANCE = true;

    private static final String ENABLE_FLOORS = "config_fitbit-beta_enable_floors";
    private static final boolean DEFAULT_ENABLE_FLOORS = true;

    private static final String ENABLE_HEART = "config_fitbit-beta_enable_heart";
    private static final boolean DEFAULT_ENABLE_HEART = false;

    private static final String ENABLE_ELEVATION = "config_fitbit-beta_enable_elevation";
    private static final boolean DEFAULT_ENABLE_ELEVATION = false;

    private static final String LAST_STEP_TIMESTAMP = "config_fitbit-beta_last_step_timestamp";
    private static final String LAST_HEART_TIMESTAMP = "config_fitbit-beta_last_heart_timestamp";
    private static final String LAST_CALORIE_TIMESTAMP = "config_fitbit-beta_last_calorie_timestamp";
    private static final String LAST_FLOOR_TIMESTAMP = "config_fitbit-beta_last_floor_timestamp";
    private static final String LAST_DISTANCE_TIMESTAMP = "config_fitbit-beta_last_distance_timestamp";
    private static final String LAST_ELEVATION_TIMESTAMP = "config_fitbit-beta_last_elevation_timestamp";

    private long _lastUpdate = 0;

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_fitbit_beta_probe_desc);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_beta_category);
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.services.FitbitBetaProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_fitbit_beta_probe);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(FitbitBetaProbe.ENABLED, true);
        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(FitbitBetaProbe.ENABLED, false);
        e.commit();
    }

    private void initKeystore(Context context)
    {
        Keystore.put(FitbitBetaApi.OAUTH2_CLIENT_ID, context.getString(R.string.fitbit_client_id));
        Keystore.put(FitbitBetaApi.CONSUMER_KEY, context.getString(R.string.fitbit_consumer_key));
        Keystore.put(FitbitBetaApi.CONSUMER_SECRET, context.getString(R.string.fitbit_consumer_secret));
        Keystore.put(FitbitBetaApi.CALLBACK_URL, "http://purple.robot.com/oauth/fitbit-beta");
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        final SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(FitbitBetaProbe.ENABLED, false))
            {
                this.initKeystore(context);

                String token = prefs.getString(FitbitBetaProbe.OAUTH_ACCESS_TOKEN, "");
                String refresh = prefs.getString(FitbitBetaProbe.OAUTH_REFRESH_TOKEN, "");

                final String title = context.getString(R.string.title_fitbit_check);
                final SanityManager sanity = SanityManager.getInstance(context);

                final FitbitBetaProbe me = this;
                final long now = System.currentTimeMillis();

                if (token == null || refresh == null || token.trim().length() == 0 || refresh.trim().length() == 0)
                {
                    String message = context.getString(R.string.message_fitbit_check);

                    Runnable action = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            me.fetchAuth(context);
                        }
                    };

                    sanity.addAlert(SanityCheck.WARNING, title, message, action);
                }
                else
                {
                    final long expires =  prefs.getLong(FitbitBetaProbe.OAUTH_TOKEN_EXPIRES, 0);
                    Keystore.put(FitbitBetaApi.USER_ACCESS_TOKEN, prefs.getString(FitbitBetaProbe.OAUTH_ACCESS_TOKEN, null));
                    Keystore.put(FitbitBetaApi.USER_REFRESH_TOKEN, prefs.getString(FitbitBetaProbe.OAUTH_REFRESH_TOKEN, null));
                    Keystore.put(FitbitBetaApi.USER_TOKEN_EXPIRES, "" + expires);

                    sanity.clearAlert(title);

                    if (now - this._lastUpdate > 1000 * 60 * 15)
                    {
                        this._lastUpdate = now;

                        Runnable r = new Runnable()
                        {
                            public void run()
                            {
                                try
                                {
                                    if (System.currentTimeMillis() > expires) {
                                        FitbitBetaApi.refreshTokens(context, FitbitBetaProbe.OAUTH_ACCESS_TOKEN, FitbitBetaProbe.OAUTH_REFRESH_TOKEN, FitbitBetaProbe.OAUTH_TOKEN_EXPIRES);

                                        long expires = prefs.getLong(FitbitBetaProbe.OAUTH_TOKEN_EXPIRES, 0);
                                        Keystore.put(FitbitBetaApi.USER_ACCESS_TOKEN, prefs.getString(FitbitBetaProbe.OAUTH_ACCESS_TOKEN, null));
                                        Keystore.put(FitbitBetaApi.USER_REFRESH_TOKEN, prefs.getString(FitbitBetaProbe.OAUTH_REFRESH_TOKEN, null));
                                        Keystore.put(FitbitBetaApi.USER_TOKEN_EXPIRES, "" + expires);
                                    }

                                    Bundle bundle = new Bundle();
                                    bundle.putString(Probe.BUNDLE_PROBE, me.name(context));
                                    bundle.putLong(Probe.BUNDLE_TIMESTAMP, System.currentTimeMillis() / 1000);

                                    boolean transmit = false;

                                    if (prefs.getBoolean(FitbitBetaProbe.ENABLE_STEPS, FitbitBetaProbe.DEFAULT_ENABLE_STEPS))
                                    {
                                        long lastUpdate = prefs.getLong(FitbitBetaProbe.LAST_STEP_TIMESTAMP, 0);

                                        JSONObject stepsObj = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/activities/steps/date/today/1d/1min.json"));

                                        try {
                                            JSONObject stepsIntraday = stepsObj.getJSONObject("activities-steps-intraday");
                                            JSONArray stepsValues = stepsIntraday.getJSONArray("dataset");

                                            ArrayList<Long> stepTimestamps = new ArrayList<>();
                                            ArrayList<Long> valueList = new ArrayList<>();

                                            Calendar c = Calendar.getInstance();

                                            for (int i = 0; i < stepsValues.length(); i++) {
                                                JSONObject value = stepsValues.getJSONObject(i);

                                                String time = value.getString("time");

                                                String[] tokens = time.split(":");

                                                c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tokens[0]));
                                                c.set(Calendar.MINUTE, Integer.parseInt(tokens[1]));
                                                c.set(Calendar.SECOND, Integer.parseInt(tokens[2]));
                                                c.set(Calendar.MILLISECOND, 0);

                                                long timestamp = c.getTimeInMillis();

                                                if (timestamp > lastUpdate) {
                                                    stepTimestamps.add(timestamp);
                                                    lastUpdate = timestamp;

                                                    valueList.add(value.getLong("value"));
                                                }
                                            }

                                            long[] timestamps = new long[stepTimestamps.size()];
                                            long[] steps = new long[stepTimestamps.size()];

                                            for (int i = 0; i < stepTimestamps.size(); i++) {
                                                timestamps[i] = stepTimestamps.get(i);
                                                steps[i] = valueList.get(i);
                                            }

                                            bundle.putLongArray(FitbitBetaProbe.STEP_TIMESTAMPS, timestamps);
                                            bundle.putLongArray(FitbitBetaProbe.STEPS, steps);

                                            Editor e = prefs.edit();
                                            e.putLong(FitbitBetaProbe.LAST_STEP_TIMESTAMP, lastUpdate);
                                            e.commit();

                                            transmit = true;
                                        }
                                        catch (JSONException e)
                                        {

                                        }
                                    }

                                    if (prefs.getBoolean(FitbitBetaProbe.ENABLE_CALORIES, FitbitBetaProbe.DEFAULT_ENABLE_CALORIES))
                                    {
                                        long lastUpdate = prefs.getLong(FitbitBetaProbe.LAST_CALORIE_TIMESTAMP, 0);

                                        JSONObject stepsObj = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/activities/calories/date/today/1d/1min.json"));

                                        try
                                        {
                                            JSONObject intraday = stepsObj.getJSONObject("activities-calories-intraday");
                                            JSONArray valuesArray = intraday.getJSONArray("dataset");

                                            ArrayList<Long> valueTimestamps = new ArrayList<>();
                                            ArrayList<Long> valueList = new ArrayList<>();

                                            Calendar c = Calendar.getInstance();

                                            for (int i = 0; i < valuesArray.length(); i++)
                                            {
                                                JSONObject value = valuesArray.getJSONObject(i);

                                                String time = value.getString("time");

                                                String[] tokens = time.split(":");

                                                c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tokens[0]));
                                                c.set(Calendar.MINUTE, Integer.parseInt(tokens[1]));
                                                c.set(Calendar.SECOND, Integer.parseInt(tokens[2]));
                                                c.set(Calendar.MILLISECOND, 0);

                                                long timestamp = c.getTimeInMillis();

                                                if (timestamp > lastUpdate) {
                                                    valueTimestamps.add(timestamp);
                                                    lastUpdate = timestamp;

                                                    valueList.add(value.getLong("value"));
                                                }
                                            }

                                            long[] timestamps = new long[valueTimestamps.size()];
                                            long[] values = new long[valueList.size()];

                                            for (int i = 0; i < valueTimestamps.size(); i++) {
                                                timestamps[i] = valueTimestamps.get(i);
                                                values[i] = valueList.get(i);
                                            }

                                            bundle.putLongArray(FitbitBetaProbe.CALORIES_TIMESTAMPS, timestamps);
                                            bundle.putLongArray(FitbitBetaProbe.CALORIES, values);

                                            Editor e = prefs.edit();
                                            e.putLong(FitbitBetaProbe.LAST_CALORIE_TIMESTAMP, lastUpdate);
                                            e.commit();

                                            transmit = true;
                                        }
                                        catch (JSONException e)
                                        {

                                        }
                                    }

                                    if (prefs.getBoolean(FitbitBetaProbe.ENABLE_DISTANCE, FitbitBetaProbe.DEFAULT_ENABLE_DISTANCE))
                                    {
                                        long lastUpdate = prefs.getLong(FitbitBetaProbe.LAST_DISTANCE_TIMESTAMP, 0);

                                        JSONObject stepsObj = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/activities/distance/date/today/1d/1min.json"));

                                        try
                                        {
                                            JSONObject intraday = stepsObj.getJSONObject("activities-distance-intraday");
                                            JSONArray valuesArray = intraday.getJSONArray("dataset");

                                            ArrayList<Long> valueTimestamps = new ArrayList<>();
                                            ArrayList<Long> valueList = new ArrayList<>();

                                            Calendar c = Calendar.getInstance();

                                            for (int i = 0; i < valuesArray.length(); i++)
                                            {
                                                JSONObject value = valuesArray.getJSONObject(i);

                                                String time = value.getString("time");

                                                String[] tokens = time.split(":");

                                                c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tokens[0]));
                                                c.set(Calendar.MINUTE, Integer.parseInt(tokens[1]));
                                                c.set(Calendar.SECOND, Integer.parseInt(tokens[2]));
                                                c.set(Calendar.MILLISECOND, 0);

                                                long timestamp = c.getTimeInMillis();

                                                if (timestamp > lastUpdate) {
                                                    valueTimestamps.add(timestamp);
                                                    lastUpdate = timestamp;

                                                    valueList.add(value.getLong("value"));
                                                }
                                            }

                                            long[] timestamps = new long[valueTimestamps.size()];
                                            long[] values = new long[valueList.size()];

                                            for (int i = 0; i < valueTimestamps.size(); i++) {
                                                timestamps[i] = valueTimestamps.get(i);
                                                values[i] = valueList.get(i);
                                            }

                                            bundle.putLongArray(FitbitBetaProbe.DISTANCE_TIMESTAMPS, timestamps);
                                            bundle.putLongArray(FitbitBetaProbe.DISTANCE, values);

                                            Editor e = prefs.edit();
                                            e.putLong(FitbitBetaProbe.LAST_DISTANCE_TIMESTAMP, lastUpdate);
                                            e.commit();

                                            transmit = true;
                                        }
                                        catch (JSONException e)
                                        {

                                        }
                                    }

                                    if (prefs.getBoolean(FitbitBetaProbe.ENABLE_FLOORS, FitbitBetaProbe.DEFAULT_ENABLE_FLOORS))
                                    {
                                        long lastUpdate = prefs.getLong(FitbitBetaProbe.LAST_FLOOR_TIMESTAMP, 0);

                                        JSONObject stepsObj = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/activities/floors/date/today/1d/1min.json"));

                                        try
                                        {
                                            JSONObject intraday = stepsObj.getJSONObject("activities-floors-intraday");
                                            JSONArray valuesArray = intraday.getJSONArray("dataset");

                                            ArrayList<Long> valueTimestamps = new ArrayList<>();
                                            ArrayList<Long> valueList = new ArrayList<>();

                                            Calendar c = Calendar.getInstance();

                                            for (int i = 0; i < valuesArray.length(); i++)
                                            {
                                                JSONObject value = valuesArray.getJSONObject(i);

                                                String time = value.getString("time");

                                                String[] tokens = time.split(":");

                                                c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tokens[0]));
                                                c.set(Calendar.MINUTE, Integer.parseInt(tokens[1]));
                                                c.set(Calendar.SECOND, Integer.parseInt(tokens[2]));
                                                c.set(Calendar.MILLISECOND, 0);

                                                long timestamp = c.getTimeInMillis();

                                                if (timestamp > lastUpdate) {
                                                    valueTimestamps.add(timestamp);
                                                    lastUpdate = timestamp;

                                                    valueList.add(value.getLong("value"));
                                                }
                                            }

                                            long[] timestamps = new long[valueTimestamps.size()];
                                            long[] values = new long[valueList.size()];

                                            for (int i = 0; i < valueTimestamps.size(); i++) {
                                                timestamps[i] = valueTimestamps.get(i);
                                                values[i] = valueList.get(i);
                                            }

                                            bundle.putLongArray(FitbitBetaProbe.FLOORS_TIMESTAMPS, timestamps);
                                            bundle.putLongArray(FitbitBetaProbe.FLOORS, values);

                                            Editor e = prefs.edit();
                                            e.putLong(FitbitBetaProbe.LAST_FLOOR_TIMESTAMP, lastUpdate);
                                            e.commit();

                                            transmit = true;
                                        }
                                        catch (JSONException e)
                                        {

                                        }
                                    }

                                    if (prefs.getBoolean(FitbitBetaProbe.ENABLE_HEART, FitbitBetaProbe.DEFAULT_ENABLE_HEART))
                                    {
                                        long lastUpdate = prefs.getLong(FitbitBetaProbe.LAST_HEART_TIMESTAMP, 0);

                                        JSONObject stepsObj = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/activities/heart/date/today/1d/1min.json"));

                                        try
                                        {
                                            JSONObject intraday = stepsObj.getJSONObject("activities-heart-intraday");
                                            JSONArray valuesArray = intraday.getJSONArray("dataset");

                                            ArrayList<Long> valueTimestamps = new ArrayList<>();
                                            ArrayList<Long> valueList = new ArrayList<>();

                                            Calendar c = Calendar.getInstance();

                                            for (int i = 0; i < valuesArray.length(); i++)
                                            {
                                                JSONObject value = valuesArray.getJSONObject(i);

                                                String time = value.getString("time");

                                                String[] tokens = time.split(":");

                                                c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tokens[0]));
                                                c.set(Calendar.MINUTE, Integer.parseInt(tokens[1]));
                                                c.set(Calendar.SECOND, Integer.parseInt(tokens[2]));
                                                c.set(Calendar.MILLISECOND, 0);

                                                long timestamp = c.getTimeInMillis();

                                                if (timestamp > lastUpdate) {
                                                    valueTimestamps.add(timestamp);
                                                    lastUpdate = timestamp;

                                                    valueList.add(value.getLong("value"));
                                                }
                                            }

                                            long[] timestamps = new long[valueTimestamps.size()];
                                            long[] values = new long[valueList.size()];

                                            for (int i = 0; i < valueTimestamps.size(); i++) {
                                                timestamps[i] = valueTimestamps.get(i);
                                                values[i] = valueList.get(i);
                                            }

                                            bundle.putLongArray(FitbitBetaProbe.HEART_TIMESTAMPS, timestamps);
                                            bundle.putLongArray(FitbitBetaProbe.HEART, values);

                                            Editor e = prefs.edit();
                                            e.putLong(FitbitBetaProbe.LAST_HEART_TIMESTAMP, lastUpdate);
                                            e.commit();

                                            transmit = true;
                                        }
                                        catch (JSONException e)
                                        {

                                        }
                                    }

                                    if (prefs.getBoolean(FitbitBetaProbe.ENABLE_ELEVATION, FitbitBetaProbe.DEFAULT_ENABLE_ELEVATION))
                                    {
                                        long lastUpdate = prefs.getLong(FitbitBetaProbe.LAST_ELEVATION_TIMESTAMP, 0);

                                        JSONObject stepsObj = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/activities/elevation/date/today/1d/1min.json"));

                                        try
                                        {
                                            JSONObject intraday = stepsObj.getJSONObject("activities-elevation-intraday");
                                            JSONArray valuesArray = intraday.getJSONArray("dataset");

                                            ArrayList<Long> valueTimestamps = new ArrayList<>();
                                            ArrayList<Long> valueList = new ArrayList<>();

                                            Calendar c = Calendar.getInstance();

                                            for (int i = 0; i < valuesArray.length(); i++)
                                            {
                                                JSONObject value = valuesArray.getJSONObject(i);

                                                String time = value.getString("time");

                                                String[] tokens = time.split(":");

                                                c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tokens[0]));
                                                c.set(Calendar.MINUTE, Integer.parseInt(tokens[1]));
                                                c.set(Calendar.SECOND, Integer.parseInt(tokens[2]));
                                                c.set(Calendar.MILLISECOND, 0);

                                                long timestamp = c.getTimeInMillis();

                                                if (timestamp > lastUpdate) {
                                                    valueTimestamps.add(timestamp);
                                                    lastUpdate = timestamp;

                                                    valueList.add(value.getLong("value"));
                                                }
                                            }

                                            long[] timestamps = new long[valueTimestamps.size()];
                                            long[] values = new long[valueList.size()];

                                            for (int i = 0; i < valueTimestamps.size(); i++) {
                                                timestamps[i] = valueTimestamps.get(i);
                                                values[i] = valueList.get(i);
                                            }

                                            bundle.putLongArray(FitbitBetaProbe.ELEVATION_TIMESTAMPS, timestamps);
                                            bundle.putLongArray(FitbitBetaProbe.ELEVATION, values);

                                            Editor e = prefs.edit();
                                            e.putLong(FitbitBetaProbe.LAST_ELEVATION_TIMESTAMP, lastUpdate);
                                            e.commit();

                                            transmit = true;
                                        }
                                        catch (JSONException e)
                                        {

                                        }
                                    }

                                    if (transmit)
                                        me.transmitData(context, bundle);
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        };

                        Thread t = new Thread(r);
                        t.start();
                    }
                }

                return true;
            }
        }

        return false;
    }

    private void fetchAuth(Context context)
    {
        this.initKeystore(context);

        String userId = EncryptionManager.getInstance().getUserHash(context);

        Intent intent = new Intent(context, OAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra(OAuthActivity.CONSUMER_KEY, context.getString(R.string.fitbit_consumer_key));
        intent.putExtra(OAuthActivity.CONSUMER_SECRET, context.getString(R.string.fitbit_consumer_secret));
        intent.putExtra(OAuthActivity.REQUESTER, "fitbit-beta");
        intent.putExtra(OAuthActivity.CALLBACK_URL, "http://purple.robot.com/oauth/fitbit-beta");
        intent.putExtra(OAuthActivity.LOG_URL, LogManager.getInstance(context).getLogUrl(context));
        intent.putExtra(OAuthActivity.HASH_SECRET, userId);

        context.startActivity(intent);
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

            settings.put(FitbitBetaProbe.ENABLE_STEPS, enabled);
            settings.put(FitbitBetaProbe.ENABLE_DISTANCE, enabled);
            settings.put(FitbitBetaProbe.ENABLE_CALORIES, enabled);
            settings.put(FitbitBetaProbe.ENABLE_ELEVATION, enabled);
            settings.put(FitbitBetaProbe.ENABLE_HEART, enabled);
            settings.put(FitbitBetaProbe.ENABLE_FLOORS, enabled);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        map.put(FitbitBetaProbe.ENABLE_CALORIES, prefs.getBoolean(FitbitBetaProbe.ENABLE_CALORIES, FitbitBetaProbe.DEFAULT_ENABLE_CALORIES));
        map.put(FitbitBetaProbe.ENABLE_STEPS, prefs.getBoolean(FitbitBetaProbe.ENABLE_STEPS, FitbitBetaProbe.DEFAULT_ENABLE_STEPS));
        map.put(FitbitBetaProbe.ENABLE_HEART, prefs.getBoolean(FitbitBetaProbe.ENABLE_HEART, FitbitBetaProbe.DEFAULT_ENABLE_HEART));
        map.put(FitbitBetaProbe.ENABLE_DISTANCE, prefs.getBoolean(FitbitBetaProbe.ENABLE_DISTANCE, FitbitBetaProbe.DEFAULT_ENABLE_DISTANCE));
        map.put(FitbitBetaProbe.ENABLE_ELEVATION, prefs.getBoolean(FitbitBetaProbe.ENABLE_ELEVATION, FitbitBetaProbe.DEFAULT_ENABLE_ELEVATION));
        map.put(FitbitBetaProbe.FLOORS, prefs.getBoolean(FitbitBetaProbe.FLOORS, FitbitBetaProbe.DEFAULT_ENABLE_FLOORS));

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        String[] keys = { FitbitBetaProbe.ENABLE_CALORIES, FitbitBetaProbe.ENABLE_STEPS, FitbitBetaProbe.ENABLE_DISTANCE, FitbitBetaProbe.ENABLE_HEART };

        for (String key: keys) {
            if (params.containsKey(key)) {
                Object value = params.get(key);

                if (value instanceof Boolean) {
                    SharedPreferences prefs = Probe.getPreferences(context);
                    Editor e = prefs.edit();

                    e.putBoolean(key, ((Boolean) value));
                    e.commit();
                }
            }
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
        enabled.setKey(FitbitBetaProbe.ENABLED);
        enabled.setDefaultValue(FitbitBetaProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        CheckBoxPreference stepsPref = new CheckBoxPreference(context);
        stepsPref.setKey(FitbitBetaProbe.ENABLE_STEPS);
        stepsPref.setDefaultValue(FitbitBetaProbe.DEFAULT_ENABLE_STEPS);
        stepsPref.setTitle(R.string.config_fitbit_steps_title);
        screen.addPreference(stepsPref);

        CheckBoxPreference distancePref = new CheckBoxPreference(context);
        distancePref.setKey(FitbitBetaProbe.ENABLE_DISTANCE);
        distancePref.setDefaultValue(FitbitBetaProbe.DEFAULT_ENABLE_DISTANCE);
        distancePref.setTitle(R.string.config_fitbit_distance_title);
        screen.addPreference(distancePref);

        CheckBoxPreference floorsPref = new CheckBoxPreference(context);
        floorsPref.setKey(FitbitBetaProbe.FLOORS);
        floorsPref.setDefaultValue(FitbitBetaProbe.DEFAULT_ENABLE_FLOORS);
        floorsPref.setTitle(R.string.config_fitbit_floors_title);
        screen.addPreference(floorsPref);

        CheckBoxPreference elevationPref = new CheckBoxPreference(context);
        elevationPref.setKey(FitbitBetaProbe.ENABLE_ELEVATION);
        elevationPref.setDefaultValue(FitbitBetaProbe.DEFAULT_ENABLE_ELEVATION);
        elevationPref.setTitle(R.string.config_fitbit_elevation_title);
        screen.addPreference(elevationPref);

        CheckBoxPreference caloriesPref = new CheckBoxPreference(context);
        caloriesPref.setKey(FitbitBetaProbe.ENABLE_CALORIES);
        caloriesPref.setDefaultValue(FitbitBetaProbe.DEFAULT_ENABLE_CALORIES);
        caloriesPref.setTitle(R.string.config_fitbit_calories_title);
        screen.addPreference(caloriesPref);

        CheckBoxPreference heartPref = new CheckBoxPreference(context);
        heartPref.setKey(FitbitBetaProbe.ENABLE_HEART);
        heartPref.setDefaultValue(FitbitBetaProbe.DEFAULT_ENABLE_HEART);
        heartPref.setTitle(R.string.config_fitbit_heart_title);
        screen.addPreference(heartPref);

        final SharedPreferences prefs = Probe.getPreferences(context);

        String token = prefs.getString(FitbitBetaProbe.OAUTH_ACCESS_TOKEN, null);
        String refresh = prefs.getString(FitbitBetaProbe.OAUTH_REFRESH_TOKEN, null);

        final Preference authPreference = new Preference(context);
        authPreference.setTitle(R.string.title_authenticate_fitbit_probe);
        authPreference.setSummary(R.string.summary_authenticate_fitbit_probe);

        final Preference logoutPreference = new Preference(context);
        logoutPreference.setTitle(R.string.title_logout_fitbit_probe);
        logoutPreference.setSummary(R.string.summary_logout_fitbit_probe);

        final FitbitBetaProbe me = this;

        authPreference.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                me.fetchAuth(context);

                screen.addPreference(logoutPreference);
                screen.removePreference(authPreference);

                return true;
            }
        });

        logoutPreference.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                Editor e = prefs.edit();
                e.remove(FitbitBetaProbe.OAUTH_ACCESS_TOKEN);
                e.remove(FitbitBetaProbe.OAUTH_REFRESH_TOKEN);
                e.remove(FitbitBetaProbe.OAUTH_TOKEN_EXPIRES);
                e.commit();

                me._lastUpdate = 0;

                screen.addPreference(authPreference);
                screen.removePreference(logoutPreference);

                if (context instanceof Activity)
                {
                    Activity activity = (Activity) context;
                    activity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Toast.makeText(context, context.getString(R.string.toast_fitbit_logout), Toast.LENGTH_LONG).show();
                        }
                    });
                }

                return true;
            }
        });

        if (token == null || refresh == null)
            screen.addPreference(authPreference);
        else
            screen.addPreference(logoutPreference);

        return screen;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double[] steps = bundle.getDoubleArray(FitbitBetaProbe.STEP_TIMESTAMPS);

        return String.format(context.getResources().getString(R.string.summary_fitbit_beta), steps.length);
    }
}
