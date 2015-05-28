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

import java.text.SimpleDateFormat;
import java.util.Date;
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
    protected static final String VERY_ACTIVE_MINUTES = "VERY_ACTIVE_MINUTES";
    protected static final String LIGHTLY_ACTIVE_MINUTES = "LIGHTLY_ACTIVE_MINUTES";
    protected static final String STEPS = "STEPS";
    protected static final String MARGINAL_CALORIES = "MARGINAL_CALORIES";
    protected static final String CALORIES_OUT = "CALORIES_OUT";
    protected static final String ACTIVE_SCORE = "ACTIVE_SCORE";
    protected static final String CALORIES_BMR = "CALORIES_BMR";
    protected static final String FAIRLY_ACTIVE_MINUTES = "FAIRLY_ACTIVE_MINUTES";
    protected static final String SEDENTARY_MINUTES = "SEDENTARY_MINUTES";
    protected static final String ACTIVITY_CALORIES = "ACTIVITY_CALORIES";

    protected static final String VERY_ACTIVE_RATIO = "VERY_ACTIVE_RATIO";
    protected static final String LIGHTLY_ACTIVE_RATIO = "LIGHTLY_ACTIVE_RATIO";
    protected static final String SEDENTARY_RATIO = "SEDENTARY_RATIO";
    protected static final String FAIRLY_ACTIVE_RATIO = "FAIRLY_ACTIVE_RATIO";

    protected static final String TOTAL_DISTANCE = "TOTAL_DISTANCE";
    protected static final String GOAL_STEPS = "GOAL_STEPS";
    protected static final String GOAL_SCORE = "GOAL_SCORE";
    protected static final String GOAL_DISTANCE = "GOAL_DISTANCE";
    protected static final String GOAL_CALORIES = "GOAL_CALORIES";
    protected static final String GOAL_DISTANCE_RATIO = "GOAL_DISTANCE_RATIO";
    protected static final String GOAL_STEPS_RATIO = "GOAL_STEPS_RATIO";
    protected static final String GOAL_CALORIES_RATIO = "GOAL_CALORIES_RATIO";
    protected static final String GOAL_SCORE_RATIO = "GOAL_SCORE_RATIO";

    private static final boolean SLEEP_DEFAULT = false;
    private static final boolean FOOD_DEFAULT = false;
    private static final boolean BODY_DEFAULT = false;

    protected static final String MEASUREMENT_BICEP = "BICEP";
    protected static final String MEASUREMENT_CALF = "CALF";
    protected static final String MEASUREMENT_FAT = "FAT";
    protected static final String MEASUREMENT_HIPS = "HIPS";
    protected static final String MEASUREMENT_THIGH = "THIGH";
    protected static final String MEASUREMENT_WEIGHT = "WEIGHT";
    protected static final String MEASUREMENT_BMI = "BMI";
    protected static final String MEASUREMENT_FOREARM = "FOREARM";
    protected static final String MEASUREMENT_WAIST = "WAIST";
    protected static final String MEASUREMENT_CHEST = "CHEST";
    protected static final String MEASUREMENT_NECK = "NECK";

    protected static final String MEASUREMENT_WATER = "WATER";
    protected static final String MEASUREMENT_SODIUM = "SODIUM";
    protected static final String MEASUREMENT_CARBS = "CARBOHYDRATES";
    protected static final String MEASUREMENT_FAT_FOOD = "FATS";
    protected static final String MEASUREMENT_CALORIES = "CALORIES";
    protected static final String MEASUREMENT_PROTEIN = "PROTIEN";
    protected static final String MEASUREMENT_FIBER = "FIBER";
    protected static final String MEASUREMENT_BODY = "BODY_MEASUREMENTS";
    protected static final String MEASUREMENT_FOOD = "FOOD_MEASUREMENTS";

    protected static final String MEASUREMENT_RESTLESS_COUNT = "RESTLESS_COUNT";
    protected static final String MEASUREMENT_AWAKE_COUNT = "AWAKE_COUNT";
    protected static final String MEASUREMENT_AWAKENINGS_COUNT = "AWAKENINGS_COUNT";
    protected static final String MEASUREMENT_MINUTES_ASLEEP = "MINUTES_ASLEEP";
    protected static final String MEASUREMENT_MINUTES_IN_BED_AFTER = "MINUTES_IN_BED_BEFORE";
    protected static final String MEASUREMENT_DURATION = "DURATION";
    protected static final String MEASUREMENT_SLEEP = "SLEEP_MEASUREMENTS";
    protected static final String MEASUREMENT_RESTLESS_DURATION = "DURATION";
    protected static final String MEASUREMENT_MINUTES_IN_BED_BEFORE = "MINUTES_IN_BED_AFTER";
    protected static final String MEASUREMENT_TIME_IN_BED = "TIME_IN_BED";
    protected static final String MEASUREMENT_AWAKE_DURATION = "DURATION";
    protected static final String MEASUREMENT_MINUTES_AWAKE = "MINUTES_AWAKE";

    private static final String SLEEP_ENABLED = "config_feature_fitbit_probe_sleep_enabled";
    private static final String FOOD_ENABLED = "config_feature_fitbit_probe_food_enabled";
    private static final String BODY_ENABLED = "config_feature_fitbit_probe_body_enabled";
    private static final String ENABLED = "config_feature_fitbit_probe_enabled";
    private static final boolean DEFAULT_ENABLED = false;
    private static final String OAUTH_TOKEN = "oauth_fitbit_token";
    private static final String OAUTH_SECRET = "oauth_fitbit_secret";

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

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                final String dateString = sdf.format(new Date());

                String token = prefs.getString(FitbitBetaProbe.OAUTH_TOKEN, "");
                String secret = prefs.getString(FitbitBetaProbe.OAUTH_SECRET, "");

                final String title = context.getString(R.string.title_fitbit_check);
                final SanityManager sanity = SanityManager.getInstance(context);

                final FitbitBetaProbe me = this;
                final long now = System.currentTimeMillis();

                if (token == null || secret == null || token.trim().length() == 0 || secret.trim().length() == 0)
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
                    Keystore.put(FitbitBetaApi.USER_TOKEN, token);
                    Keystore.put(FitbitBetaApi.USER_SECRET, secret);

                    sanity.clearAlert(title);

                    if (now - this._lastUpdate > 1000 * 60 * 5)
                    {
                        this._lastUpdate = now;

                        Runnable r = new Runnable()
                        {
                            public void run()
                            {
                                try
                                {
                                    JSONObject body = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/activities/date/" + dateString + ".json"));

                                    JSONObject summary = body.getJSONObject("summary");

                                    Bundle bundle = new Bundle();
                                    bundle.putString(Probe.BUNDLE_PROBE, me.name(context));
                                    bundle.putLong(Probe.BUNDLE_TIMESTAMP, System.currentTimeMillis() / 1000);

                                    long veryActive = summary.getLong("veryActiveMinutes");
                                    long fairlyActive = summary.getLong("fairlyActiveMinutes");
                                    long lightlyActive = summary.getLong("lightlyActiveMinutes");
                                    long sedentary = summary.getLong("sedentaryMinutes");

                                    long total = veryActive + fairlyActive + lightlyActive + sedentary;

                                    bundle.putLong(FitbitBetaProbe.VERY_ACTIVE_MINUTES, veryActive);
                                    bundle.putLong(FitbitBetaProbe.FAIRLY_ACTIVE_MINUTES, fairlyActive);
                                    bundle.putLong(FitbitBetaProbe.LIGHTLY_ACTIVE_MINUTES, lightlyActive);
                                    bundle.putLong(FitbitBetaProbe.SEDENTARY_MINUTES, sedentary);

                                    bundle.putDouble(FitbitBetaProbe.VERY_ACTIVE_RATIO, (double) veryActive / (double) total);
                                    bundle.putDouble(FitbitBetaProbe.FAIRLY_ACTIVE_RATIO, (double) fairlyActive / (double) total);
                                    bundle.putDouble(FitbitBetaProbe.LIGHTLY_ACTIVE_RATIO, (double) lightlyActive / (double) total);
                                    bundle.putDouble(FitbitBetaProbe.SEDENTARY_RATIO, (double) sedentary / (double) total);

                                    long steps = summary.getLong("steps");
                                    bundle.putLong(FitbitBetaProbe.STEPS, steps);

                                    long caloriesOut = summary.getLong("caloriesOut");
                                    bundle.putLong(FitbitBetaProbe.CALORIES_OUT, caloriesOut);
                                    bundle.putLong(FitbitBetaProbe.CALORIES_BMR, summary.getLong("caloriesBMR"));
                                    bundle.putLong(FitbitBetaProbe.MARGINAL_CALORIES, summary.getLong("marginalCalories"));
                                    bundle.putLong(FitbitBetaProbe.ACTIVITY_CALORIES, summary.getLong("activityCalories"));

                                    long score = summary.getLong("activeScore");
                                    bundle.putLong(FitbitBetaProbe.ACTIVE_SCORE, score);

                                    JSONArray activities = summary.getJSONArray("distances");

                                    long distance = 0;

                                    for (int i = 0; i < activities.length(); i++)
                                    {
                                        JSONObject activity = activities.getJSONObject(i);

                                        if ("total".equals(activity.getString("activity")))
                                            distance = activity.getLong("distance");
                                    }

                                    bundle.putLong(FitbitBetaProbe.TOTAL_DISTANCE, distance);

                                    JSONObject goals = body.getJSONObject("goals");

                                    long goalDistance = goals.getLong("distance");
                                    long goalSteps = goals.getLong("steps");
                                    long goalCalories = goals.getLong("caloriesOut");

                                    bundle.putLong(FitbitBetaProbe.GOAL_DISTANCE, goalDistance);
                                    bundle.putLong(FitbitBetaProbe.GOAL_STEPS, goalSteps);
                                    bundle.putLong(FitbitBetaProbe.GOAL_CALORIES, goalCalories);

                                    bundle.putDouble(FitbitBetaProbe.GOAL_DISTANCE_RATIO, (double) distance / (double) goalDistance);
                                    bundle.putDouble(FitbitBetaProbe.GOAL_STEPS_RATIO, (double) steps / (double) goalSteps);
                                    bundle.putDouble(FitbitBetaProbe.GOAL_CALORIES_RATIO, (double) caloriesOut / (double) goalCalories);

                                    if (prefs.getBoolean(FitbitBetaProbe.SLEEP_ENABLED, FitbitBetaProbe.SLEEP_DEFAULT))
                                    {
                                        body = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/sleep/date/" + dateString + ".json"));

                                        JSONArray sleeps = body.getJSONArray("sleep");

                                        int restlessCount = 0;
                                        int restlessDuration = 0;

                                        int awakeCount = 0;
                                        int awakeDuration = 0;
                                        int awakeningsCount = 0;

                                        int minutesInBedBefore = 0;
                                        int minutesAsleep = 0;
                                        int minutesAwake = 0;
                                        int minutesInBedAfter = 0;

                                        int timeInBed = 0;
                                        int duration = 0;

                                        for (int j = 0; j < sleeps.length(); j++)
                                        {
                                            JSONObject sleep = sleeps.getJSONObject(j);

                                            restlessCount += sleep.getInt("restlessCount");
                                            restlessDuration += sleep.getInt("restlessDuration");

                                            awakeCount += sleep.getInt("awakeCount");
                                            awakeDuration += sleep.getInt("awakeDuration");
                                            awakeningsCount += sleep.getInt("awakeningsCount");

                                            minutesInBedBefore += sleep.getInt("minutesToFallAsleep");
                                            minutesAsleep += sleep.getInt("minutesAsleep");
                                            minutesAwake += sleep.getInt("minutesAwake");
                                            minutesInBedAfter += sleep.getInt("minutesAfterWakeup");

                                            timeInBed += sleep.getInt("timeInBed");
                                            duration += sleep.getInt("duration");
                                        }

                                        Bundle sleepBundle = new Bundle();

                                        sleepBundle.putDouble(FitbitBetaProbe.MEASUREMENT_RESTLESS_COUNT, restlessCount);
                                        sleepBundle.putDouble(FitbitBetaProbe.MEASUREMENT_RESTLESS_DURATION, restlessDuration);
                                        sleepBundle.putDouble(FitbitBetaProbe.MEASUREMENT_AWAKE_COUNT, awakeCount);
                                        sleepBundle.putDouble(FitbitBetaProbe.MEASUREMENT_AWAKE_DURATION, awakeDuration);
                                        sleepBundle.putDouble(FitbitBetaProbe.MEASUREMENT_AWAKENINGS_COUNT, awakeningsCount);
                                        sleepBundle.putDouble(FitbitBetaProbe.MEASUREMENT_MINUTES_IN_BED_BEFORE, minutesInBedBefore);
                                        sleepBundle.putDouble(FitbitBetaProbe.MEASUREMENT_MINUTES_ASLEEP, minutesAsleep);
                                        sleepBundle.putDouble(FitbitBetaProbe.MEASUREMENT_MINUTES_AWAKE, minutesAwake);
                                        sleepBundle.putDouble(FitbitBetaProbe.MEASUREMENT_MINUTES_IN_BED_AFTER, minutesInBedAfter);
                                        sleepBundle.putDouble(FitbitBetaProbe.MEASUREMENT_TIME_IN_BED, timeInBed);
                                        sleepBundle.putDouble(FitbitBetaProbe.MEASUREMENT_DURATION, duration);

                                        bundle.putBundle(FitbitBetaProbe.MEASUREMENT_SLEEP, sleepBundle);
                                    }

                                    if (prefs.getBoolean(FitbitBetaProbe.FOOD_ENABLED, FitbitBetaProbe.FOOD_DEFAULT))
                                    {
                                        body = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/foods/log/date/" + dateString + ".json"));

                                        body = body.getJSONObject("summary");

                                        Bundle foodBundle = new Bundle();

                                        foodBundle.putDouble(FitbitBetaProbe.MEASUREMENT_WATER, (double) body.getInt("water"));
                                        foodBundle.putDouble(FitbitBetaProbe.MEASUREMENT_CALORIES, (double) body.getInt("calories"));
                                        foodBundle.putDouble(FitbitBetaProbe.MEASUREMENT_SODIUM, (double) body.getInt("sodium"));
                                        foodBundle.putDouble(FitbitBetaProbe.MEASUREMENT_FIBER, (double) body.getInt("fiber"));
                                        foodBundle.putDouble(FitbitBetaProbe.MEASUREMENT_CARBS, (double) body.getInt("carbs"));
                                        foodBundle.putDouble(FitbitBetaProbe.MEASUREMENT_PROTEIN, (double) body.getInt("protein"));
                                        foodBundle.putDouble(FitbitBetaProbe.MEASUREMENT_FAT_FOOD, (double) body.getInt("fat"));

                                        bundle.putBundle(FitbitBetaProbe.MEASUREMENT_FOOD, foodBundle);
                                    }

                                    if (prefs.getBoolean(FitbitBetaProbe.BODY_ENABLED, FitbitBetaProbe.BODY_DEFAULT))
                                    {
                                        body = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/body/date/" + dateString + ".json"));

                                        body = body.getJSONObject("body");

                                        Bundle bodyBundle = new Bundle();

                                        bodyBundle.putDouble(FitbitBetaProbe.MEASUREMENT_BICEP, (double) body.getInt("bicep"));
                                        bodyBundle.putDouble(FitbitBetaProbe.MEASUREMENT_BMI, (double) body.getInt("bmi"));
                                        bodyBundle.putDouble(FitbitBetaProbe.MEASUREMENT_CALF, (double) body.getInt("calf"));
                                        bodyBundle.putDouble(FitbitBetaProbe.MEASUREMENT_CHEST, (double) body.getInt("chest"));
                                        bodyBundle.putDouble(FitbitBetaProbe.MEASUREMENT_FAT, (double) body.getInt("fat"));
                                        bodyBundle.putDouble(FitbitBetaProbe.MEASUREMENT_FOREARM, (double) body.getInt("forearm"));
                                        bodyBundle.putDouble(FitbitBetaProbe.MEASUREMENT_HIPS, (double) body.getInt("hips"));
                                        bodyBundle.putDouble(FitbitBetaProbe.MEASUREMENT_NECK, (double) body.getInt("neck"));
                                        bodyBundle.putDouble(FitbitBetaProbe.MEASUREMENT_THIGH, (double) body.getInt("thigh"));
                                        bodyBundle.putDouble(FitbitBetaProbe.MEASUREMENT_WAIST, (double) body.getInt("waist"));
                                        bodyBundle.putDouble(FitbitBetaProbe.MEASUREMENT_WEIGHT, (double) body.getInt("weight"));

                                        bundle.putBundle(FitbitBetaProbe.MEASUREMENT_BODY, bodyBundle);
                                    }

                                    me.transmitData(context, bundle);
                                } catch (Exception e)
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
        String userId = EncryptionManager.getInstance().getUserHash(context);

        Intent intent = new Intent(context, OAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra(OAuthActivity.CONSUMER_KEY, context.getString(R.string.fitbit_consumer_key));
        intent.putExtra(OAuthActivity.CONSUMER_SECRET, context.getString(R.string.fitbit_consumer_secret));
        intent.putExtra(OAuthActivity.REQUESTER, "fitbit-beta");
        intent.putExtra(OAuthActivity.CALLBACK_URL, "http://tech.cbits.northwestern.edu/oauth/fitbit-beta");
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

            settings.put(FitbitBetaProbe.SLEEP_ENABLED, enabled);
            settings.put(FitbitBetaProbe.FOOD_ENABLED, enabled);
            settings.put(FitbitBetaProbe.BODY_ENABLED, enabled);
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

        map.put(FitbitBetaProbe.SLEEP_ENABLED, prefs.getBoolean(FitbitBetaProbe.SLEEP_ENABLED, FitbitBetaProbe.SLEEP_DEFAULT));
        map.put(FitbitBetaProbe.FOOD_ENABLED, prefs.getBoolean(FitbitBetaProbe.FOOD_ENABLED, FitbitBetaProbe.FOOD_DEFAULT));
        map.put(FitbitBetaProbe.BODY_ENABLED, prefs.getBoolean(FitbitBetaProbe.BODY_ENABLED, FitbitBetaProbe.BODY_DEFAULT));

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(FitbitBetaProbe.SLEEP_ENABLED))
        {
            Object value = params.get(FitbitBetaProbe.SLEEP_ENABLED);

            if (value instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(FitbitBetaProbe.SLEEP_ENABLED, ((Boolean) value));
                e.commit();
            }
        }

        if (params.containsKey(FitbitBetaProbe.BODY_ENABLED))
        {
            Object value = params.get(FitbitBetaProbe.BODY_ENABLED);

            if (value instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(FitbitBetaProbe.BODY_ENABLED, ((Boolean) value));
                e.commit();
            }
        }

        if (params.containsKey(FitbitBetaProbe.FOOD_ENABLED))
        {
            Object value = params.get(FitbitBetaProbe.FOOD_ENABLED);

            if (value instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(FitbitBetaProbe.FOOD_ENABLED, ((Boolean) value));
                e.commit();
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

        CheckBoxPreference sleepPref = new CheckBoxPreference(context);
        sleepPref.setKey(FitbitBetaProbe.SLEEP_ENABLED);
        sleepPref.setDefaultValue(FitbitBetaProbe.SLEEP_DEFAULT);
        sleepPref.setTitle(R.string.config_fitbit_sleep_title);
        screen.addPreference(sleepPref);

        CheckBoxPreference foodPref = new CheckBoxPreference(context);
        foodPref.setKey(FitbitBetaProbe.FOOD_ENABLED);
        foodPref.setDefaultValue(FitbitBetaProbe.FOOD_DEFAULT);
        foodPref.setTitle(R.string.config_fitbit_food_title);
        screen.addPreference(foodPref);

        CheckBoxPreference bodyPref = new CheckBoxPreference(context);
        bodyPref.setKey(FitbitBetaProbe.BODY_ENABLED);
        bodyPref.setDefaultValue(FitbitBetaProbe.BODY_DEFAULT);
        bodyPref.setTitle(R.string.config_fitbit_body_title);
        screen.addPreference(bodyPref);

        // TODO: Should activity have a boolean switch as well?

        final SharedPreferences prefs = Probe.getPreferences(context);

        String token = prefs.getString(FitbitBetaProbe.OAUTH_TOKEN, null);
        String secret = prefs.getString(FitbitBetaProbe.OAUTH_SECRET, null);

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
                e.remove(FitbitBetaProbe.OAUTH_TOKEN);
                e.remove(FitbitBetaProbe.OAUTH_SECRET);
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

        if (token == null || secret == null)
            screen.addPreference(authPreference);
        else
            screen.addPreference(logoutPreference);

        return screen;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double steps = bundle.getDouble(FitbitBetaProbe.STEPS);
        double progress = bundle.getDouble(FitbitBetaProbe.GOAL_STEPS_RATIO) * 100;

        return String.format(context.getResources().getString(R.string.summary_fitbit), steps, progress);
    }
}
