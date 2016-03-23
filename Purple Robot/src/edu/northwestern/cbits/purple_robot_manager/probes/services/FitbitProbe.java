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
import edu.northwestern.cbits.xsi.oauth.FitbitApi;
import edu.northwestern.cbits.xsi.oauth.Keystore;
import edu.northwestern.cbits.xsi.oauth.OAuthActivity;

public class FitbitProbe extends Probe
{
    public final static String PROBE_NAME = "edu.northwestern.cbits.purple_robot_manager.probes.services.FitbitProbe";

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
    public String getPreferenceKey() {
        return "services_fitbit";
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_fitbit_probe_desc);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_external_services_category);
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.services.FitbitProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_fitbit_probe);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(FitbitProbe.ENABLED, true);
        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(FitbitProbe.ENABLED, false);
        e.commit();
    }

    private void initKeystore(Context context)
    {
        Keystore.put(FitbitApi.CONSUMER_KEY, context.getString(R.string.fitbit_consumer_key));
        Keystore.put(FitbitApi.CONSUMER_SECRET, context.getString(R.string.fitbit_consumer_secret));
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        final SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(FitbitProbe.ENABLED, false))
            {
                this.initKeystore(context);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                final String dateString = sdf.format(new Date());

                String token = prefs.getString(FitbitProbe.OAUTH_TOKEN, "");
                String secret = prefs.getString(FitbitProbe.OAUTH_SECRET, "");

                final String title = context.getString(R.string.title_fitbit_check);
                final SanityManager sanity = SanityManager.getInstance(context);

                final FitbitProbe me = this;
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
                    Keystore.put(FitbitApi.USER_TOKEN, token);
                    Keystore.put(FitbitApi.USER_SECRET, secret);

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
                                    JSONObject body = FitbitApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/activities/date/" + dateString + ".json"));

                                    JSONObject summary = body.getJSONObject("summary");

                                    Bundle bundle = new Bundle();
                                    bundle.putString(Probe.BUNDLE_PROBE, me.name(context));
                                    bundle.putLong(Probe.BUNDLE_TIMESTAMP, System.currentTimeMillis() / 1000);

                                    long veryActive = summary.getLong("veryActiveMinutes");
                                    long fairlyActive = summary.getLong("fairlyActiveMinutes");
                                    long lightlyActive = summary.getLong("lightlyActiveMinutes");
                                    long sedentary = summary.getLong("sedentaryMinutes");

                                    long total = veryActive + fairlyActive + lightlyActive + sedentary;

                                    bundle.putLong(FitbitProbe.VERY_ACTIVE_MINUTES, veryActive);
                                    bundle.putLong(FitbitProbe.FAIRLY_ACTIVE_MINUTES, fairlyActive);
                                    bundle.putLong(FitbitProbe.LIGHTLY_ACTIVE_MINUTES, lightlyActive);
                                    bundle.putLong(FitbitProbe.SEDENTARY_MINUTES, sedentary);

                                    bundle.putDouble(FitbitProbe.VERY_ACTIVE_RATIO, (double) veryActive / (double) total);
                                    bundle.putDouble(FitbitProbe.FAIRLY_ACTIVE_RATIO, (double) fairlyActive / (double) total);
                                    bundle.putDouble(FitbitProbe.LIGHTLY_ACTIVE_RATIO, (double) lightlyActive / (double) total);
                                    bundle.putDouble(FitbitProbe.SEDENTARY_RATIO, (double) sedentary / (double) total);

                                    long steps = summary.getLong("steps");
                                    bundle.putLong(FitbitProbe.STEPS, steps);

                                    long caloriesOut = summary.getLong("caloriesOut");
                                    bundle.putLong(FitbitProbe.CALORIES_OUT, caloriesOut);
                                    bundle.putLong(FitbitProbe.CALORIES_BMR, summary.getLong("caloriesBMR"));
                                    bundle.putLong(FitbitProbe.MARGINAL_CALORIES, summary.getLong("marginalCalories"));
                                    bundle.putLong(FitbitProbe.ACTIVITY_CALORIES, summary.getLong("activityCalories"));

                                    long score = summary.getLong("activeScore");
                                    bundle.putLong(FitbitProbe.ACTIVE_SCORE, score);

                                    JSONArray activities = summary.getJSONArray("distances");

                                    long distance = 0;

                                    for (int i = 0; i < activities.length(); i++)
                                    {
                                        JSONObject activity = activities.getJSONObject(i);

                                        if ("total".equals(activity.getString("activity")))
                                            distance = activity.getLong("distance");
                                    }

                                    bundle.putLong(FitbitProbe.TOTAL_DISTANCE, distance);

                                    JSONObject goals = body.getJSONObject("goals");

                                    long goalDistance = goals.getLong("distance");
                                    long goalSteps = goals.getLong("steps");
                                    long goalCalories = goals.getLong("caloriesOut");

                                    bundle.putLong(FitbitProbe.GOAL_DISTANCE, goalDistance);
                                    bundle.putLong(FitbitProbe.GOAL_STEPS, goalSteps);
                                    bundle.putLong(FitbitProbe.GOAL_CALORIES, goalCalories);

                                    bundle.putDouble(FitbitProbe.GOAL_DISTANCE_RATIO, (double) distance / (double) goalDistance);
                                    bundle.putDouble(FitbitProbe.GOAL_STEPS_RATIO, (double) steps / (double) goalSteps);
                                    bundle.putDouble(FitbitProbe.GOAL_CALORIES_RATIO, (double) caloriesOut / (double) goalCalories);

                                    if (prefs.getBoolean(FitbitProbe.SLEEP_ENABLED, FitbitProbe.SLEEP_DEFAULT))
                                    {
                                        body = FitbitApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/sleep/date/" + dateString + ".json"));

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

                                        sleepBundle.putDouble(FitbitProbe.MEASUREMENT_RESTLESS_COUNT, restlessCount);
                                        sleepBundle.putDouble(FitbitProbe.MEASUREMENT_RESTLESS_DURATION, restlessDuration);
                                        sleepBundle.putDouble(FitbitProbe.MEASUREMENT_AWAKE_COUNT, awakeCount);
                                        sleepBundle.putDouble(FitbitProbe.MEASUREMENT_AWAKE_DURATION, awakeDuration);
                                        sleepBundle.putDouble(FitbitProbe.MEASUREMENT_AWAKENINGS_COUNT, awakeningsCount);
                                        sleepBundle.putDouble(FitbitProbe.MEASUREMENT_MINUTES_IN_BED_BEFORE, minutesInBedBefore);
                                        sleepBundle.putDouble(FitbitProbe.MEASUREMENT_MINUTES_ASLEEP, minutesAsleep);
                                        sleepBundle.putDouble(FitbitProbe.MEASUREMENT_MINUTES_AWAKE, minutesAwake);
                                        sleepBundle.putDouble(FitbitProbe.MEASUREMENT_MINUTES_IN_BED_AFTER, minutesInBedAfter);
                                        sleepBundle.putDouble(FitbitProbe.MEASUREMENT_TIME_IN_BED, timeInBed);
                                        sleepBundle.putDouble(FitbitProbe.MEASUREMENT_DURATION, duration);

                                        bundle.putBundle(FitbitProbe.MEASUREMENT_SLEEP, sleepBundle);
                                    }

                                    if (prefs.getBoolean(FitbitProbe.FOOD_ENABLED, FitbitProbe.FOOD_DEFAULT))
                                    {
                                        body = FitbitApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/foods/log/date/" + dateString + ".json"));

                                        body = body.getJSONObject("summary");

                                        Bundle foodBundle = new Bundle();

                                        foodBundle.putDouble(FitbitProbe.MEASUREMENT_WATER, (double) body.getInt("water"));
                                        foodBundle.putDouble(FitbitProbe.MEASUREMENT_CALORIES, (double) body.getInt("calories"));
                                        foodBundle.putDouble(FitbitProbe.MEASUREMENT_SODIUM, (double) body.getInt("sodium"));
                                        foodBundle.putDouble(FitbitProbe.MEASUREMENT_FIBER, (double) body.getInt("fiber"));
                                        foodBundle.putDouble(FitbitProbe.MEASUREMENT_CARBS, (double) body.getInt("carbs"));
                                        foodBundle.putDouble(FitbitProbe.MEASUREMENT_PROTEIN, (double) body.getInt("protein"));
                                        foodBundle.putDouble(FitbitProbe.MEASUREMENT_FAT_FOOD, (double) body.getInt("fat"));

                                        bundle.putBundle(FitbitProbe.MEASUREMENT_FOOD, foodBundle);
                                    }

                                    if (prefs.getBoolean(FitbitProbe.BODY_ENABLED, FitbitProbe.BODY_DEFAULT))
                                    {
                                        body = FitbitApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/body/date/" + dateString + ".json"));

                                        body = body.getJSONObject("body");

                                        Bundle bodyBundle = new Bundle();

                                        bodyBundle.putDouble(FitbitProbe.MEASUREMENT_BICEP, (double) body.getInt("bicep"));
                                        bodyBundle.putDouble(FitbitProbe.MEASUREMENT_BMI, (double) body.getInt("bmi"));
                                        bodyBundle.putDouble(FitbitProbe.MEASUREMENT_CALF, (double) body.getInt("calf"));
                                        bodyBundle.putDouble(FitbitProbe.MEASUREMENT_CHEST, (double) body.getInt("chest"));
                                        bodyBundle.putDouble(FitbitProbe.MEASUREMENT_FAT, (double) body.getInt("fat"));
                                        bodyBundle.putDouble(FitbitProbe.MEASUREMENT_FOREARM, (double) body.getInt("forearm"));
                                        bodyBundle.putDouble(FitbitProbe.MEASUREMENT_HIPS, (double) body.getInt("hips"));
                                        bodyBundle.putDouble(FitbitProbe.MEASUREMENT_NECK, (double) body.getInt("neck"));
                                        bodyBundle.putDouble(FitbitProbe.MEASUREMENT_THIGH, (double) body.getInt("thigh"));
                                        bodyBundle.putDouble(FitbitProbe.MEASUREMENT_WAIST, (double) body.getInt("waist"));
                                        bodyBundle.putDouble(FitbitProbe.MEASUREMENT_WEIGHT, (double) body.getInt("weight"));

                                        bundle.putBundle(FitbitProbe.MEASUREMENT_BODY, bodyBundle);
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
        intent.putExtra(OAuthActivity.REQUESTER, "fitbit");
        intent.putExtra(OAuthActivity.CALLBACK_URL, "http://tech.cbits.northwestern.edu/oauth/fitbit");
        intent.putExtra(OAuthActivity.LOG_URL, LogManager.getInstance(context).getLogUrl(context));
        intent.putExtra(OAuthActivity.HASH_SECRET, userId);

        context.startActivity(intent);
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = super.fetchSettings(context);

        try
        {
            JSONObject enabled = new JSONObject();

            settings.put(FitbitProbe.SLEEP_ENABLED, enabled);
            settings.put(FitbitProbe.FOOD_ENABLED, enabled);
            settings.put(FitbitProbe.BODY_ENABLED, enabled);
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

        map.put(FitbitProbe.SLEEP_ENABLED, prefs.getBoolean(FitbitProbe.SLEEP_ENABLED, FitbitProbe.SLEEP_DEFAULT));
        map.put(FitbitProbe.FOOD_ENABLED, prefs.getBoolean(FitbitProbe.FOOD_ENABLED, FitbitProbe.FOOD_DEFAULT));
        map.put(FitbitProbe.BODY_ENABLED, prefs.getBoolean(FitbitProbe.BODY_ENABLED, FitbitProbe.BODY_DEFAULT));

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(FitbitProbe.SLEEP_ENABLED))
        {
            Object value = params.get(FitbitProbe.SLEEP_ENABLED);

            if (value instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(FitbitProbe.SLEEP_ENABLED, (Boolean) value);
                e.commit();
            }
        }

        if (params.containsKey(FitbitProbe.BODY_ENABLED))
        {
            Object value = params.get(FitbitProbe.BODY_ENABLED);

            if (value instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(FitbitProbe.BODY_ENABLED, (Boolean) value);
                e.commit();
            }
        }

        if (params.containsKey(FitbitProbe.FOOD_ENABLED))
        {
            Object value = params.get(FitbitProbe.FOOD_ENABLED);

            if (value instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(FitbitProbe.FOOD_ENABLED, (Boolean) value);
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
        enabled.setKey(FitbitProbe.ENABLED);
        enabled.setDefaultValue(FitbitProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        CheckBoxPreference sleepPref = new CheckBoxPreference(context);
        sleepPref.setKey(FitbitProbe.SLEEP_ENABLED);
        sleepPref.setDefaultValue(FitbitProbe.SLEEP_DEFAULT);
        sleepPref.setTitle(R.string.config_fitbit_sleep_title);
        screen.addPreference(sleepPref);

        CheckBoxPreference foodPref = new CheckBoxPreference(context);
        foodPref.setKey(FitbitProbe.FOOD_ENABLED);
        foodPref.setDefaultValue(FitbitProbe.FOOD_DEFAULT);
        foodPref.setTitle(R.string.config_fitbit_food_title);
        screen.addPreference(foodPref);

        CheckBoxPreference bodyPref = new CheckBoxPreference(context);
        bodyPref.setKey(FitbitProbe.BODY_ENABLED);
        bodyPref.setDefaultValue(FitbitProbe.BODY_DEFAULT);
        bodyPref.setTitle(R.string.config_fitbit_body_title);
        screen.addPreference(bodyPref);

        // TODO: Should activity have a boolean switch as well?

        final SharedPreferences prefs = Probe.getPreferences(context);

        String token = prefs.getString(FitbitProbe.OAUTH_TOKEN, null);
        String secret = prefs.getString(FitbitProbe.OAUTH_SECRET, null);

        final Preference authPreference = new Preference(context);
        authPreference.setTitle(R.string.title_authenticate_fitbit_probe);
        authPreference.setSummary(R.string.summary_authenticate_fitbit_probe);

        final Preference logoutPreference = new Preference(context);
        logoutPreference.setTitle(R.string.title_logout_fitbit_probe);
        logoutPreference.setSummary(R.string.summary_logout_fitbit_probe);

        final FitbitProbe me = this;

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
                e.remove(FitbitProbe.OAUTH_TOKEN);
                e.remove(FitbitProbe.OAUTH_SECRET);
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
        double steps = bundle.getDouble(FitbitProbe.STEPS);
        double progress = bundle.getDouble(FitbitProbe.GOAL_STEPS_RATIO) * 100;

        return String.format(context.getResources().getString(R.string.summary_fitbit), steps, progress);
    }
}
