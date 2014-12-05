package edu.northwestern.cbits.purple_robot_manager.probes.features;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.OAuthActivity;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.oauth.FitbitApi;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class FitbitApiFeature extends Feature
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

    private static final String SLEEP_ENABLED = "config_feature_fitbit_api_sleep_enabled";
    private static final String FOOD_ENABLED = "config_feature_fitbit_api_food_enabled";
    private static final String BODY_ENABLED = "config_feature_fitbit_api_body_enabled";
    private static final String ENABLED = "config_feature_fitbit_api_enabled";

    private long _lastUpdate = 0;
    private long _lastFetch = 0;

    private String _token = null;
    private String _secret = null;

    @Override
    protected String featureKey()
    {
        return "fitbit_api";
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_fitbit_api_feature_desc);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_external_services_category);
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.features.FitBitApiFeature";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_fitbit_api_feature);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(FitbitApiFeature.ENABLED, true);
        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(FitbitApiFeature.ENABLED, false);
        e.commit();
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        final SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(FitbitApiFeature.ENABLED, false))
            {
                long now = System.currentTimeMillis();

                if (now - this._lastUpdate > 1000 * 60)
                {
                    this._lastUpdate = now;

                    final FitbitApiFeature me = this;

                    this._token = prefs.getString("oauth_fitbit_token", null);
                    this._secret = prefs.getString("oauth_fitbit_secret", null);

                    final String title = context.getString(R.string.title_fitbit_check);
                    final SanityManager sanity = SanityManager.getInstance(context);

                    if (this._token == null || this._secret == null)
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
                        sanity.clearAlert(title);

                        if (now - this._lastFetch > 1000 * 60 * 5)
                        {
                            this._lastFetch = now;

                            Runnable r = new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    try
                                    {
                                        Token accessToken = new Token(me._token, me._secret);

                                        ServiceBuilder builder = new ServiceBuilder();
                                        builder = builder.provider(FitbitApi.class);
                                        builder = builder.apiKey(context.getString(R.string.fitbit_consumer_key));
                                        builder = builder.apiSecret(context.getString(R.string.fitbit_consumer_secret));

                                        final OAuthService service = builder.build();

                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                                        String dateString = sdf.format(new Date());

                                        OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/activities/date/" + dateString + ".json");
                                        service.signRequest(accessToken, request);

                                        Response response = request.send();

                                        JSONObject body = new JSONObject(response.getBody());

                                        JSONObject summary = body.getJSONObject("summary");

                                        Bundle bundle = new Bundle();
                                        bundle.putString("PROBE", me.name(context));
                                        bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                                        long veryActive = summary.getLong("veryActiveMinutes");
                                        long fairlyActive = summary.getLong("fairlyActiveMinutes");
                                        long lightlyActive = summary.getLong("lightlyActiveMinutes");
                                        long sedentary = summary.getLong("sedentaryMinutes");

                                        long total = veryActive + fairlyActive + lightlyActive + sedentary;

                                        bundle.putLong(FitbitApiFeature.VERY_ACTIVE_MINUTES, veryActive);
                                        bundle.putLong(FitbitApiFeature.FAIRLY_ACTIVE_MINUTES, fairlyActive);
                                        bundle.putLong(FitbitApiFeature.LIGHTLY_ACTIVE_MINUTES, lightlyActive);
                                        bundle.putLong(FitbitApiFeature.SEDENTARY_MINUTES, sedentary);

                                        bundle.putDouble(FitbitApiFeature.VERY_ACTIVE_RATIO, (double) veryActive / (double) total);
                                        bundle.putDouble(FitbitApiFeature.FAIRLY_ACTIVE_RATIO, (double) fairlyActive / (double) total);
                                        bundle.putDouble(FitbitApiFeature.LIGHTLY_ACTIVE_RATIO, (double) lightlyActive / (double) total);
                                        bundle.putDouble(FitbitApiFeature.SEDENTARY_RATIO, (double) sedentary / (double) total);

                                        long steps = summary.getLong("steps");
                                        bundle.putLong(FitbitApiFeature.STEPS, steps);

                                        long caloriesOut = summary.getLong("caloriesOut");
                                        bundle.putLong(FitbitApiFeature.CALORIES_OUT, caloriesOut);
                                        bundle.putLong(FitbitApiFeature.CALORIES_BMR, summary.getLong("caloriesBMR"));
                                        bundle.putLong(FitbitApiFeature.MARGINAL_CALORIES, summary.getLong("marginalCalories"));
                                        bundle.putLong(FitbitApiFeature.ACTIVITY_CALORIES, summary.getLong("activityCalories"));

                                        long score = summary.getLong("activeScore");
                                        bundle.putLong(FitbitApiFeature.ACTIVE_SCORE, score);

                                        JSONArray activities = summary.getJSONArray("distances");

                                        long distance = 0;

                                        for (int i = 0; i < activities.length(); i++)
                                        {
                                            JSONObject activity = activities.getJSONObject(i);

                                            if ("total".equals(activity.getString("activity")))
                                                distance = activity.getLong("distance");
                                        }

                                        bundle.putLong(FitbitApiFeature.TOTAL_DISTANCE, distance);

                                        JSONObject goals = body.getJSONObject("goals");

                                        long goalDistance = goals.getLong("distance");
                                        long goalSteps = goals.getLong("steps");
                                        long goalCalories = goals.getLong("caloriesOut");

                                        bundle.putLong(FitbitApiFeature.GOAL_DISTANCE, goalDistance);
                                        bundle.putLong(FitbitApiFeature.GOAL_STEPS, goalSteps);
                                        bundle.putLong(FitbitApiFeature.GOAL_CALORIES, goalCalories);

                                        bundle.putDouble(FitbitApiFeature.GOAL_DISTANCE_RATIO, (double) distance / (double) goalDistance);
                                        bundle.putDouble(FitbitApiFeature.GOAL_STEPS_RATIO, (double) steps / (double) goalSteps);
                                        bundle.putDouble(FitbitApiFeature.GOAL_CALORIES_RATIO, (double) caloriesOut / (double) goalCalories);

                                        if (prefs.getBoolean(FitbitApiFeature.SLEEP_ENABLED, FitbitApiFeature.SLEEP_DEFAULT))
                                        {
                                            request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/sleep/date/" + dateString + ".json");
                                            service.signRequest(accessToken, request);

                                            response = request.send();

                                            body = new JSONObject(response.getBody());

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

                                            sleepBundle.putDouble(FitbitApiFeature.MEASUREMENT_RESTLESS_COUNT, restlessCount);
                                            sleepBundle.putDouble(FitbitApiFeature.MEASUREMENT_RESTLESS_DURATION, restlessDuration);
                                            sleepBundle.putDouble(FitbitApiFeature.MEASUREMENT_AWAKE_COUNT, awakeCount);
                                            sleepBundle.putDouble(FitbitApiFeature.MEASUREMENT_AWAKE_DURATION, awakeDuration);
                                            sleepBundle.putDouble(FitbitApiFeature.MEASUREMENT_AWAKENINGS_COUNT, awakeningsCount);
                                            sleepBundle.putDouble(FitbitApiFeature.MEASUREMENT_MINUTES_IN_BED_BEFORE, minutesInBedBefore);
                                            sleepBundle.putDouble(FitbitApiFeature.MEASUREMENT_MINUTES_ASLEEP, minutesAsleep);
                                            sleepBundle.putDouble(FitbitApiFeature.MEASUREMENT_MINUTES_AWAKE, minutesAwake);
                                            sleepBundle.putDouble(FitbitApiFeature.MEASUREMENT_MINUTES_IN_BED_AFTER, minutesInBedAfter);
                                            sleepBundle.putDouble(FitbitApiFeature.MEASUREMENT_TIME_IN_BED, timeInBed);
                                            sleepBundle.putDouble(FitbitApiFeature.MEASUREMENT_DURATION, duration);

                                            bundle.putBundle(FitbitApiFeature.MEASUREMENT_SLEEP, sleepBundle);
                                        }

                                        if (prefs.getBoolean(FitbitApiFeature.FOOD_ENABLED, FitbitApiFeature.FOOD_DEFAULT))
                                        {
                                            request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/foods/log/date/" + dateString + ".json");
                                            service.signRequest(accessToken, request);

                                            response = request.send();

                                            body = new JSONObject(response.getBody());
                                            body = body.getJSONObject("summary");

                                            Bundle foodBundle = new Bundle();

                                            foodBundle.putDouble(FitbitApiFeature.MEASUREMENT_WATER, (double) body.getInt("water"));
                                            foodBundle.putDouble(FitbitApiFeature.MEASUREMENT_CALORIES, (double) body.getInt("calories"));
                                            foodBundle.putDouble(FitbitApiFeature.MEASUREMENT_SODIUM, (double) body.getInt("sodium"));
                                            foodBundle.putDouble(FitbitApiFeature.MEASUREMENT_FIBER, (double) body.getInt("fiber"));
                                            foodBundle.putDouble(FitbitApiFeature.MEASUREMENT_CARBS, (double) body.getInt("carbs"));
                                            foodBundle.putDouble(FitbitApiFeature.MEASUREMENT_PROTEIN, (double) body.getInt("protein"));
                                            foodBundle.putDouble(FitbitApiFeature.MEASUREMENT_FAT_FOOD, (double) body.getInt("fat"));

                                            bundle.putBundle(FitbitApiFeature.MEASUREMENT_FOOD, foodBundle);
                                        }

                                        if (prefs.getBoolean(FitbitApiFeature.BODY_ENABLED, FitbitApiFeature.BODY_DEFAULT))
                                        {
                                            request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/body/date/" + dateString + ".json");
                                            service.signRequest(accessToken, request);

                                            response = request.send();

                                            body = new JSONObject(response.getBody());
                                            body = body.getJSONObject("body");

                                            Bundle bodyBundle = new Bundle();

                                            bodyBundle.putDouble(FitbitApiFeature.MEASUREMENT_BICEP, (double) body.getInt("bicep"));
                                            bodyBundle.putDouble(FitbitApiFeature.MEASUREMENT_BMI, (double) body.getInt("bmi"));
                                            bodyBundle.putDouble(FitbitApiFeature.MEASUREMENT_CALF, (double) body.getInt("calf"));
                                            bodyBundle.putDouble(FitbitApiFeature.MEASUREMENT_CHEST, (double) body.getInt("chest"));
                                            bodyBundle.putDouble(FitbitApiFeature.MEASUREMENT_FAT, (double) body.getInt("fat"));
                                            bodyBundle.putDouble(FitbitApiFeature.MEASUREMENT_FOREARM, (double) body.getInt("forearm"));
                                            bodyBundle.putDouble(FitbitApiFeature.MEASUREMENT_HIPS, (double) body.getInt("hips"));
                                            bodyBundle.putDouble(FitbitApiFeature.MEASUREMENT_NECK, (double) body.getInt("neck"));
                                            bodyBundle.putDouble(FitbitApiFeature.MEASUREMENT_THIGH, (double) body.getInt("thigh"));
                                            bodyBundle.putDouble(FitbitApiFeature.MEASUREMENT_WAIST, (double) body.getInt("waist"));
                                            bodyBundle.putDouble(FitbitApiFeature.MEASUREMENT_WEIGHT, (double) body.getInt("weight"));

                                            bundle.putBundle(FitbitApiFeature.MEASUREMENT_BODY, bodyBundle);
                                        }

                                        me.transmitData(context, bundle);
                                    }
                                    catch (JSONException e)
                                    {
                                        LogManager.getInstance(context).logException(e);
                                    }
                                    catch (OAuthException e)
                                    {
                                        LogManager.getInstance(context).logException(e);
                                    }
                                    catch (IllegalArgumentException e)
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

                return true;
            }
        }

        return false;
    }

    @Override
    protected boolean defaultEnabled()
    {
        return false;
    }

    private void fetchAuth(Context context)
    {
        Intent intent = new Intent(context, OAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra(OAuthActivity.CONSUMER_KEY, context.getString(R.string.fitbit_consumer_key));
        intent.putExtra(OAuthActivity.CONSUMER_SECRET, context.getString(R.string.fitbit_consumer_secret));
        intent.putExtra(OAuthActivity.REQUESTER, "fitbit");
        intent.putExtra(OAuthActivity.CALLBACK_URL, "http://pr-oauth/oauth/fitbit");

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

            settings.put(FitbitApiFeature.SLEEP_ENABLED, enabled);
            settings.put(FitbitApiFeature.FOOD_ENABLED, enabled);
            settings.put(FitbitApiFeature.BODY_ENABLED, enabled);
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

        map.put(FitbitApiFeature.SLEEP_ENABLED, prefs.getBoolean(FitbitApiFeature.SLEEP_ENABLED, FitbitApiFeature.SLEEP_DEFAULT));
        map.put(FitbitApiFeature.FOOD_ENABLED, prefs.getBoolean(FitbitApiFeature.FOOD_ENABLED, FitbitApiFeature.FOOD_DEFAULT));
        map.put(FitbitApiFeature.BODY_ENABLED, prefs.getBoolean(FitbitApiFeature.BODY_ENABLED, FitbitApiFeature.BODY_DEFAULT));

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(FitbitApiFeature.SLEEP_ENABLED))
        {
            Object value = params.get(FitbitApiFeature.SLEEP_ENABLED);

            if (value instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(FitbitApiFeature.SLEEP_ENABLED, ((Boolean) value).booleanValue());
                e.commit();
            }
        }

        if (params.containsKey(FitbitApiFeature.BODY_ENABLED))
        {
            Object value = params.get(FitbitApiFeature.BODY_ENABLED);

            if (value instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(FitbitApiFeature.BODY_ENABLED, ((Boolean) value).booleanValue());
                e.commit();
            }
        }

        if (params.containsKey(FitbitApiFeature.FOOD_ENABLED))
        {
            Object value = params.get(FitbitApiFeature.FOOD_ENABLED);

            if (value instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(FitbitApiFeature.FOOD_ENABLED, ((Boolean) value).booleanValue());
                e.commit();
            }
        }
    }

    @Override
    public PreferenceScreen preferenceScreen(final PreferenceActivity activity)
    {
        final PreferenceScreen screen = super.preferenceScreen(activity);

        CheckBoxPreference sleepPref = new CheckBoxPreference(activity);
        sleepPref.setKey(FitbitApiFeature.SLEEP_ENABLED);
        sleepPref.setDefaultValue(FitbitApiFeature.SLEEP_DEFAULT);
        sleepPref.setTitle(R.string.config_fitbit_sleep_title);
        screen.addPreference(sleepPref);

        CheckBoxPreference foodPref = new CheckBoxPreference(activity);
        foodPref.setKey(FitbitApiFeature.FOOD_ENABLED);
        foodPref.setDefaultValue(FitbitApiFeature.FOOD_DEFAULT);
        foodPref.setTitle(R.string.config_fitbit_food_title);
        screen.addPreference(foodPref);

        CheckBoxPreference bodyPref = new CheckBoxPreference(activity);
        bodyPref.setKey(FitbitApiFeature.BODY_ENABLED);
        bodyPref.setDefaultValue(FitbitApiFeature.BODY_DEFAULT);
        bodyPref.setTitle(R.string.config_fitbit_body_title);
        screen.addPreference(bodyPref);

        // TODO: Should activity have a boolean switch as well?

        final SharedPreferences prefs = Probe.getPreferences(activity);

        String token = prefs.getString("oauth_fitbit_token", null);
        String secret = prefs.getString("oauth_fitbit_secret", null);

        final Preference authPreference = new Preference(activity);
        authPreference.setTitle(R.string.title_authenticate_fitbit_probe);
        authPreference.setSummary(R.string.summary_authenticate_fitbit_probe);

        final Preference logoutPreference = new Preference(activity);
        logoutPreference.setTitle(R.string.title_logout_fitbit_probe);
        logoutPreference.setSummary(R.string.summary_logout_fitbit_probe);

        final FitbitApiFeature me = this;

        authPreference.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                me.fetchAuth(activity);

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
                e.remove("oauth_fitbit_token");
                e.remove("oauth_fitbit_secret");
                e.commit();

                me._lastUpdate = 0;

                screen.addPreference(authPreference);
                screen.removePreference(logoutPreference);

                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(activity, activity.getString(R.string.toast_fitbit_logout), Toast.LENGTH_LONG).show();
                    }
                });

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
        double steps = bundle.getDouble(FitbitApiFeature.STEPS);
        double progress = bundle.getDouble(FitbitApiFeature.GOAL_STEPS_RATIO) * 100;

        return String.format(context.getResources().getString(R.string.summary_fitbit), steps, progress);
    }
}
