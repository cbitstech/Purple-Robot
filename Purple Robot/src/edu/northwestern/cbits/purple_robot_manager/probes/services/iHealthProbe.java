package edu.northwestern.cbits.purple_robot_manager.probes.services;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.xsi.oauth.Keystore;
import edu.northwestern.cbits.xsi.oauth.OAuthActivity;
import edu.northwestern.cbits.xsi.oauth.iHealthApi;

public class iHealthProbe extends Probe
{
    private static final String ENABLED = "config_feature_ihealth_probe_enabled";
    private static final boolean DEFAULT_ENABLED = false;

    private static final String BLOOD_GLUCOSE_ENABLED = "config_feature_ihealth_blood_glucose_enabled";
    private static final boolean BLOOD_GLUCOSE_DEFAULT = false;

    private static final String BLOOD_OXYGEN_ENABLED = "config_feature_ihealth_blood_oxygen_enabled";
    private static final boolean BLOOD_OXYGEN_DEFAULT = false;

    private static final String BLOOD_PRESSURE_ENABLED = "config_feature_ihealth_blood_pressure_enabled";
    private static final boolean BLOOD_PRESSURE_DEFAULT = false;

    private static final String WEIGHT_ENABLED = "config_feature_ihealth_weight_enabled";
    private static final boolean WEIGHT_DEFAULT = false;

    private static final String ACTIVITY_ENABLED = "config_feature_ihealth_activity_enabled";
    private static final boolean ACTIVITY_DEFAULT = true;

    private static final String SLEEP_ENABLED = "config_feature_ihealth_sleep_enabled";
    private static final boolean SLEEP_DEFAULT = true;

    private static final String OAUTH_TOKEN = "oauth_ihealth_token";
    private static final String OAUTH_SECRET = "oauth_ihealth_secret";

    private long _lastUpdate = 0;

    private void initKeystore(Context context)
    {
        Keystore.put(iHealthApi.CONSUMER_KEY, context.getString(R.string.ihealth_client_id));
        Keystore.put(iHealthApi.CONSUMER_SECRET, context.getString(R.string.ihealth_client_secret));

        Keystore.put(iHealthApi.ACTIVITY_SC, context.getString(R.string.ihealth_activity_sc));
        Keystore.put(iHealthApi.ACTIVITY_SV, context.getString(R.string.ihealth_activity_sv));
        Keystore.put(iHealthApi.SLEEP_SC, context.getString(R.string.ihealth_sleep_sc));
        Keystore.put(iHealthApi.SLEEP_SV, context.getString(R.string.ihealth_sleep_sv));
        Keystore.put(iHealthApi.WEIGHT_SC, context.getString(R.string.ihealth_weight_sc));
        Keystore.put(iHealthApi.WEIGHT_SV, context.getString(R.string.ihealth_weight_sv));
        Keystore.put(iHealthApi.OXYGEN_SC, context.getString(R.string.ihealth_oxygen_sc));
        Keystore.put(iHealthApi.OXYGEN_SV, context.getString(R.string.ihealth_oxygen_sv));
        Keystore.put(iHealthApi.PRESSURE_SC, context.getString(R.string.ihealth_pressure_sc));
        Keystore.put(iHealthApi.PRESSURE_SV, context.getString(R.string.ihealth_pressure_sv));
        Keystore.put(iHealthApi.GLUCOSE_SC, context.getString(R.string.ihealth_glucose_sc));
        Keystore.put(iHealthApi.GLUCOSE_SV, context.getString(R.string.ihealth_glucose_sv));

        Keystore.put(iHealthApi.REDIRECT_URL, "http://purplerobot/oauth/ihealth");
    }

    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.services.iHealthProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_ihealth_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_external_services_category);
    }

    @Override
    public PreferenceScreen preferenceScreen(final Context context, PreferenceManager manager)
    {
        final PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(this.summary(context));

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(iHealthProbe.ENABLED);
        enabled.setDefaultValue(iHealthProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        CheckBoxPreference activityPref = new CheckBoxPreference(context);
        activityPref.setKey(iHealthProbe.ACTIVITY_ENABLED);
        activityPref.setDefaultValue(iHealthProbe.ACTIVITY_DEFAULT);
        activityPref.setTitle(R.string.config_ihealth_activity_title);
        screen.addPreference(activityPref);

        CheckBoxPreference sleepPref = new CheckBoxPreference(context);
        sleepPref.setKey(iHealthProbe.SLEEP_ENABLED);
        sleepPref.setDefaultValue(iHealthProbe.SLEEP_DEFAULT);
        sleepPref.setTitle(R.string.config_ihealth_sleep_title);
        screen.addPreference(sleepPref);

        CheckBoxPreference weightPref = new CheckBoxPreference(context);
        weightPref.setKey(iHealthProbe.WEIGHT_ENABLED);
        weightPref.setDefaultValue(iHealthProbe.WEIGHT_DEFAULT);
        weightPref.setTitle(R.string.config_ihealth_weight_title);
        screen.addPreference(weightPref);

        CheckBoxPreference bloodPressurePref = new CheckBoxPreference(context);
        bloodPressurePref.setKey(iHealthProbe.BLOOD_PRESSURE_ENABLED);
        bloodPressurePref.setDefaultValue(iHealthProbe.BLOOD_PRESSURE_DEFAULT);
        bloodPressurePref.setTitle(R.string.config_ihealth_blood_pressure_title);
        screen.addPreference(bloodPressurePref);

        CheckBoxPreference bloodGlucosePref = new CheckBoxPreference(context);
        bloodGlucosePref.setKey(iHealthProbe.BLOOD_GLUCOSE_ENABLED);
        bloodGlucosePref.setDefaultValue(iHealthProbe.BLOOD_GLUCOSE_DEFAULT);
        bloodGlucosePref.setTitle(R.string.config_ihealth_blood_glucose_title);
        screen.addPreference(bloodGlucosePref);

        CheckBoxPreference bloodOxygenPref = new CheckBoxPreference(context);
        bloodOxygenPref.setKey(iHealthProbe.BLOOD_OXYGEN_ENABLED);
        bloodOxygenPref.setDefaultValue(iHealthProbe.BLOOD_OXYGEN_DEFAULT);
        bloodOxygenPref.setTitle(R.string.config_ihealth_blood_oxygen_title);
        screen.addPreference(bloodOxygenPref);

        final SharedPreferences prefs = Probe.getPreferences(context);

        String token = prefs.getString(iHealthProbe.OAUTH_TOKEN, null);
        String secret = prefs.getString(iHealthProbe.OAUTH_SECRET, null);

        final Preference authPreference = new Preference(context);
        authPreference.setTitle(R.string.title_authenticate_ihealth_probe);
        authPreference.setSummary(R.string.summary_authenticate_ihealth_probe);

        final Preference logoutPreference = new Preference(context);
        logoutPreference.setTitle(R.string.title_logout_ihealth_probe);
        logoutPreference.setSummary(R.string.summary_logout_ihealth_probe);

        final iHealthProbe me = this;

        authPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
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

        logoutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                SharedPreferences.Editor e = prefs.edit();
                e.remove(iHealthProbe.OAUTH_TOKEN);
                e.remove(iHealthProbe.OAUTH_SECRET);
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
                            Toast.makeText(context, context.getString(R.string.toast_ihealth_logout), Toast.LENGTH_LONG).show();
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
    public String summary(Context context)
    {
        return context.getString(R.string.summary_ihealth_probe_desc);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean(iHealthProbe.ENABLED, true);
        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean(iHealthProbe.ENABLED, false);
        e.commit();
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

            settings.put(iHealthProbe.SLEEP_ENABLED, enabled);
            settings.put(iHealthProbe.ACTIVITY_ENABLED, enabled);
            settings.put(iHealthProbe.WEIGHT_ENABLED, enabled);
            settings.put(iHealthProbe.BLOOD_GLUCOSE_ENABLED, enabled);
            settings.put(iHealthProbe.BLOOD_OXYGEN_ENABLED, enabled);
            settings.put(iHealthProbe.BLOOD_PRESSURE_ENABLED, enabled);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    private void fetchAuth(Context context)
    {
        String userId = EncryptionManager.getInstance().getUserHash(context);

        Intent intent = new Intent(context, OAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra(OAuthActivity.CONSUMER_KEY, context.getString(R.string.ihealth_client_id));
        intent.putExtra(OAuthActivity.CONSUMER_SECRET, context.getString(R.string.ihealth_client_secret));
        intent.putExtra(OAuthActivity.REQUESTER, "ihealth");
        intent.putExtra(OAuthActivity.CALLBACK_URL, "http://purple.robot.com/oauth/ihealth");
        intent.putExtra(OAuthActivity.LOG_URL, LogManager.getInstance(context).getLogUrl(context));
        intent.putExtra(OAuthActivity.HASH_SECRET, userId);

        context.startActivity(intent);
    }

    public boolean isEnabled(final Context context)
    {
        final SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(iHealthProbe.ENABLED, iHealthProbe.DEFAULT_ENABLED))
            {
                this.initKeystore(context);

                String token = prefs.getString(iHealthProbe.OAUTH_TOKEN, "");
                String secret = prefs.getString(iHealthProbe.OAUTH_SECRET, "");

                final String title = context.getString(R.string.title_ihealth_check);
                final SanityManager sanity = SanityManager.getInstance(context);

                final iHealthProbe me = this;
                final long now = System.currentTimeMillis();

                if (token == null ||  token.trim().length() == 0)
                {
                    String message = context.getString(R.string.message_ihealth_check);

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
                    Keystore.put(iHealthApi.USER_TOKEN, token);
                    Keystore.put(iHealthApi.USER_SECRET, secret);

                    sanity.clearAlert(title);

                    if (now - this._lastUpdate > 1000 * 60 * 1)
                    {
                        Log.e("PR", "FETCHING iHealth DATA");

                        this._lastUpdate = now;

                        Runnable r = new Runnable()
                        {
                            public void run()
                            {
                                try
                                {
                                    Calendar calendar = Calendar.getInstance();
                                    calendar.set(Calendar.HOUR, 0);
                                    calendar.set(Calendar.MINUTE, 0);
                                    calendar.set(Calendar.SECOND, 0);
                                    calendar.set(Calendar.MILLISECOND, 0);

                                    calendar.add(Calendar.DATE, -3);

                                    long start = calendar.getTimeInMillis();

                                    calendar.add(Calendar.DATE, 4);

                                    long end = calendar.getTimeInMillis();

                                    Bundle bundle = new Bundle();
                                    bundle.putString("PROBE", me.name(context));
                                    bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                                    if (prefs.getBoolean(iHealthProbe.BLOOD_GLUCOSE_ENABLED, iHealthProbe.BLOOD_GLUCOSE_DEFAULT))
                                    {
                                        /*
                                            JSONObject body = iHealthApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/activities/date/" + dateString + ".json"));
                                         */

                                        // http://sandboxapi.ihealthlabs.com/openapiv2/application/glucose.json/?client_id=ddb9cbc759*****&client_secret=4738f9d00e*****&redirect_uri=http%3a%2f%2f+yourcallback.com%2f%3fthis%3dthat&access_token=xpoBt0ThQQ*****&start_time=1342006062&end_time=1405078062&page_index=1&sc=d63493704c*****&sv=90f0793451*****
                                    }

                                    if (prefs.getBoolean(iHealthProbe.BLOOD_OXYGEN_ENABLED, iHealthProbe.BLOOD_OXYGEN_DEFAULT))
                                    {
                                        // http://sandboxapi.ihealthlabs.com/openapiv2/application/spo2.json/?client_id=ddb9cbc759*****&client_secret=4738f9d00e*****&redirect_uri=http%3a%2f%2f+yourcallback.com%2f%3fthis%3dthat&access_token=xpoBt0ThQQ*****&start_time=1342005726&end_time=1405077726&page_index=1&sc=d63493704c*****&sv=88f34288d5*****
                                    }

                                    if (prefs.getBoolean(iHealthProbe.BLOOD_PRESSURE_ENABLED, iHealthProbe.BLOOD_PRESSURE_DEFAULT))
                                    {
                                        // http://sandboxapi.ihealthlabs.com/openapiv2/application/bp.json/?client_id=ddb9cbc759*****&client_secret=4738f9d00e*****&redirect_uri=http%3a%2f%2f+yourcallback.com%2f%3fthis%3dthat&access_token=xpoBt0ThQQ*****&start_time=1342007016&end_time=1405079016&page_index=1&sc=d63493704c*****&sv=113cb40956*****
                                    }

                                    if (prefs.getBoolean(iHealthProbe.ACTIVITY_ENABLED, iHealthProbe.ACTIVITY_DEFAULT))
                                    {
                                        JSONObject activity = iHealthApi.fetchActivity(context, start / 1000, end / 1000);

                                        Log.e("PR-iHealth", "FETCHED: " + activity.toString(2));
                                    }

                                    if (prefs.getBoolean(iHealthProbe.SLEEP_ENABLED, iHealthProbe.SLEEP_DEFAULT))
                                    {
                                        // http://sandboxapi.ihealthlabs.com/openapiv2/application/sleep.json/?client_id=ddb9cbc759*****&client_secret=4738f9d00e*****&redirect_uri=http%3a%2f%2f+yourcallback.com%2f%3fthis%3dthat&access_token=xpoBt0ThQQ*****&start_time=1342004920&end_time=1405076920&page_index=1&sc=d63493704c*****&sv=68d3f571e6*****
                                    }

                                    if (prefs.getBoolean(iHealthProbe.WEIGHT_ENABLED, iHealthProbe.WEIGHT_DEFAULT))
                                    {
                                        // http://sandboxapi.ihealthlabs.com/openapiv2/application/weight.json/?client_id=ddb9cbc759*****&client_secret=4738f9d00e*****&redirect_uri=http%3a%2f%2fapi.testweb2.com%2foauthtest.aspx&access_token=xpoBt0ThQQ*****&start_time=1342007425&end_time=1405079425&page_index=1&sc=d63493704c*****&sv=2b26f1c0af*****
                                    }

                                    // me.transmitData(context, bundle);
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

}
