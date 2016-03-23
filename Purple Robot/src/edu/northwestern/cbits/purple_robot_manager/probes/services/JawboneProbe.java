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

import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.xsi.oauth.JawboneApi;
import edu.northwestern.cbits.xsi.oauth.Keystore;
import edu.northwestern.cbits.xsi.oauth.OAuthActivity;

public class JawboneProbe extends Probe
{
    public final static String PROBE_NAME = "edu.northwestern.cbits.purple_robot_manager.probes.services.JawboneProbe";

    private static final String ENABLED = "config_feature_jawbone_probe_enabled";

    private static final boolean DEFAULT_ENABLED = false;

    private long _lastUpdate = 0;

    @Override
    public String getPreferenceKey() {
        return "services_jawbone";
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_jawbone_probe_desc);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_external_services_category);
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.services.JawboneProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_jawbone_probe);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(JawboneProbe.ENABLED, true);
        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(JawboneProbe.ENABLED, false);
        e.commit();
    }

    private void initKeystore(Context context)
    {
        Keystore.put(JawboneApi.CONSUMER_KEY, context.getString(R.string.jawbone_consumer_key));
        Keystore.put(JawboneApi.CONSUMER_SECRET, context.getString(R.string.jawbone_consumer_secret));
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        final SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(JawboneProbe.ENABLED, false))
            {
                this.initKeystore(context);

                String token = prefs.getString("oauth_jawbone_token", "");

                final String title = context.getString(R.string.title_jawbone_check);
                final SanityManager sanity = SanityManager.getInstance(context);

                final JawboneProbe me = this;
                final long now = System.currentTimeMillis();

                if (token == null || token.trim().length() == 0)
                {
                    String message = context.getString(R.string.message_jawbone_check);

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
                    Keystore.put(JawboneApi.USER_TOKEN, token);

                    sanity.clearAlert(title);

                    if (now - this._lastUpdate > 1000 * 60 * 15) // 15 min refresh interval
                    {
                        this._lastUpdate = now;

                        Runnable r = new Runnable()
                        {
                            public void run()
                            {
                                try
                                {
                                    JSONObject goals = JawboneApi.fetch(context, Uri.parse("https://jawbone.com/nudge/api/v.1.1/users/@me/goals"), "Purple Robot");

                                    Bundle bundle = new Bundle();
                                    bundle.putString("PROBE", me.name(context));
                                    bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                                    JSONObject data = goals.getJSONObject("data");
                                    JSONObject remaining = data.getJSONObject("remaining_for_day");

                                    if (data.has("move_steps"))
                                    {
                                        double goalSteps = data.getDouble("move_steps");
                                        double remainingSteps = remaining.getDouble("move_steps_remaining");

                                        bundle.putDouble("STEPS_GOAL", goalSteps);
                                        bundle.putDouble("GOAL_STEPS", goalSteps - remainingSteps);
                                    }

                                    if (data.has("sleep_total"))
                                    {
                                        double goalSleep = data.getDouble("sleep_total");
                                        double remainingSleep = remaining.getDouble("sleep_seconds_remaining");

                                        bundle.putDouble("SLEEP_GOAL", goalSleep);
                                        bundle.putDouble("GOAL_SLEEP", goalSleep - remainingSleep);
                                    }

                                    if (remaining.has("intake_calories_remaining"))
                                        bundle.putDouble("CALORIES_REMAINING", remaining.getDouble("intake_calories_remaining"));

                                    JSONObject mood = JawboneApi.fetch(context, Uri.parse("https://jawbone.com/nudge/api/v.1.1/users/@me/mood"), "Purple Robot");
                                    data = mood.getJSONObject("data");

                                    if (data.has("sub_type"))
                                    {
                                        bundle.putString("MOOD_TITLE", data.getString("title"));
                                        bundle.putDouble("MOOD_TIMESTAMP", data.getDouble("time_updated"));

                                        switch(data.getInt("sub_type"))
                                        {
                                            case 1:
                                                bundle.putDouble("MOOD", 8.0);
                                                break;
                                            case 2:
                                                bundle.putDouble("MOOD", 7.0);
                                                break;
                                            case 3:
                                                bundle.putDouble("MOOD", 6.0);
                                                break;
                                            case 8:
                                                bundle.putDouble("MOOD", 5.0);
                                                break;
                                            case 4:
                                                bundle.putDouble("MOOD", 4.0);
                                                break;
                                            case 5:
                                                bundle.putDouble("MOOD", 3.0);
                                                break;
                                            case 6:
                                                bundle.putDouble("MOOD", 2.0);
                                                break;
                                            case 7:
                                                bundle.putDouble("MOOD", 1.0);
                                                break;

                                        }
                                    }

                                    JSONObject moves = JawboneApi.fetch(context, Uri.parse("https://jawbone.com/nudge/api/v.1.1/users/@me/noves"), "Purple Robot");
                                    data = moves.getJSONObject("data");

                                    if (moves.has("items"))
                                    {
                                        JSONObject item = data.getJSONArray("items").getJSONObject(0);

                                        bundle.putDouble("DISTANCE", item.getDouble("distance"));
                                        bundle.putDouble("STEPS", item.getDouble("steps"));
                                        bundle.putDouble("ACTIVE_TIME", item.getDouble("active_time"));
                                        bundle.putDouble("LONGEST_ACTIVE", item.getDouble("longest_active"));
                                        bundle.putDouble("INACTIVE_TIME", item.getDouble("inactive_time"));
                                        bundle.putDouble("LONGEST_IDLE", item.getDouble("longest_idle"));
                                        bundle.putDouble("CALORIES", item.getDouble("calories"));
                                        bundle.putDouble("BMR_DAY", item.getDouble("bmr_day"));
                                        bundle.putDouble("WO_CALORIES", item.getDouble("wo_calories"));
                                        bundle.putDouble("WO_TIME", item.getDouble("wo_time"));
                                        bundle.putDouble("WO_ACTIVE_TIME", item.getDouble("wo_active_time"));
                                        bundle.putDouble("WO_COUNT", item.getDouble("wo_count"));
                                    }

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
        String userId = EncryptionManager.getInstance().getUserHash(context);

        Intent intent = new Intent(context, OAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra(OAuthActivity.CONSUMER_KEY, context.getString(R.string.jawbone_consumer_key));
        intent.putExtra(OAuthActivity.CONSUMER_SECRET, context.getString(R.string.jawbone_consumer_secret));
        intent.putExtra(OAuthActivity.REQUESTER, "jawbone");
        intent.putExtra(OAuthActivity.CALLBACK_URL, "http://tech.cbits.northwestern.edu/oauth/jawbone");
        intent.putExtra(OAuthActivity.LOG_URL, LogManager.getInstance(context).getLogUrl(context));
        intent.putExtra(OAuthActivity.HASH_SECRET, userId);

        context.startActivity(intent);
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

    }

    @Override
    public PreferenceScreen preferenceScreen(final Context context, PreferenceManager manager)
    {
        final PreferenceScreen screen = super.preferenceScreen(context, manager);

        screen.setTitle(this.title(context));
        screen.setSummary(this.summary(context));

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(JawboneProbe.ENABLED);
        enabled.setDefaultValue(JawboneProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        final SharedPreferences prefs = Probe.getPreferences(context);

        String token = prefs.getString("oauth_jawbone_token", null);
        String secret = prefs.getString("oauth_jawbone_secret", null);

        final Preference authPreference = new Preference(context);
        authPreference.setTitle(R.string.title_authenticate_jawbone_probe);
        authPreference.setSummary(R.string.summary_authenticate_jawbone_probe);

        final Preference logoutPreference = new Preference(context);
        logoutPreference.setTitle(R.string.title_logout_jawbone_probe);
        logoutPreference.setSummary(R.string.summary_logout_jawbone_probe);

        final JawboneProbe me = this;

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
                e.remove("oauth_jawbone_token");
                e.remove("oauth_jawbone_secret");
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
                            Toast.makeText(context, context.getString(R.string.toast_jawbone_logout), Toast.LENGTH_LONG).show();
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
        double steps = bundle.getDouble("STEPS_GOAL");
        double progress = (bundle.getDouble("GOAL_STEPS") / steps) * 100;

        return String.format(context.getResources().getString(R.string.summary_jawbone), steps, progress);
    }
}
