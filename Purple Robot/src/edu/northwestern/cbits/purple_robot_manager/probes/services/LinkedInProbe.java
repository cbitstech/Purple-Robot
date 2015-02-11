package edu.northwestern.cbits.purple_robot_manager.probes.services;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.xsi.oauth.Keystore;
import edu.northwestern.cbits.xsi.oauth.LinkedInApi;
import edu.northwestern.cbits.xsi.oauth.OAuthActivity;

public class LinkedInProbe extends Probe
{
    private static final boolean DEFAULT_ENABLED = false;
    private static final String OAUTH_TOKEN = "oauth_linkedin_token";
    private static final String OAUTH_SECRET = "oauth_linkedin_secret";

    private static final String ENABLED = "config_probe_linkedin_enabled";
    private static final String FREQUENCY = "config_probe_linkedin_frequency";
    private static final String ENCRYPT_DATA = "config_probe_linkedin_encrypt_data";
    private static final boolean DEFAULT_ENCRYPT = true;
    private static final String MOST_RECENT = "\"config_linkedin_most_recent\"" ;

    private long _lastCheck = 0;

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_linkedin_probe_desc);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_external_services_category);
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.services.LinkedInProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_linkedin_probe);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(LinkedInProbe.ENABLED, true);
        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(LinkedInProbe.ENABLED, false);
        e.commit();
    }

    private void initKeystore(Context context)
    {
        Keystore.put(LinkedInApi.CONSUMER_KEY, context.getString(R.string.linkedin_consumer_key));
        Keystore.put(LinkedInApi.CONSUMER_SECRET, context.getString(R.string.linkedin_consumer_secret));
    }

    public boolean isEnabled(final Context context)
    {
        final SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            final long now = System.currentTimeMillis();

            if (prefs.getBoolean(LinkedInProbe.ENABLED, LinkedInProbe.DEFAULT_ENABLED))
            {
                synchronized (this)
                {
                    this.initKeystore(context);

                    long freq = Long.parseLong(prefs.getString(LinkedInProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
                    final boolean doEncrypt = prefs.getBoolean(LinkedInProbe.ENCRYPT_DATA, LinkedInProbe.DEFAULT_ENCRYPT);

                    final EncryptionManager em = EncryptionManager.getInstance();

                    if (now - this._lastCheck > freq)
                    {
                        final LinkedInProbe me = this;

                        final String token  = prefs.getString(LinkedInProbe.OAUTH_TOKEN, null);
                        final String secret = prefs.getString(LinkedInProbe.OAUTH_SECRET, null);

                        final String title = context.getString(R.string.title_linkedin_check);
                        final SanityManager sanity = SanityManager.getInstance(context);

                        if (token == null || secret == null)
                        {
                            String message = context.getString(R.string.message_linkedin_check);

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

                            Keystore.put(LinkedInApi.USER_TOKEN, token);
                            Keystore.put(LinkedInApi.USER_SECRET, secret);

                            Runnable r = new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    long mostRecent = prefs.getLong(LinkedInProbe.MOST_RECENT, 0);

                                    try
                                    {
                                        JSONObject root = LinkedInApi.fetch("http://api.linkedin.com/v1/people/~/network/updates?scope=self&type=RECU&type=SHAR&type=VIRL&format=json");

                                        if (root.getInt("_total") > 0)
                                        {
                                            JSONArray values = root.getJSONArray("values");

                                            for (int i = (values.length() - 1); i >= 0; i--)
                                            {
                                                JSONObject post = values.getJSONObject(i);

                                                long postTime = post.getLong("timestamp");

                                                if (postTime > mostRecent)
                                                {
                                                    Bundle eventBundle = new Bundle();
                                                    eventBundle.putString(Probe.BUNDLE_PROBE, me.name(context));
                                                    eventBundle.putLong(Probe.BUNDLE_TIMESTAMP, postTime / 1000);

                                                    eventBundle.putString("TYPE", post.getString("updateType"));

                                                    if (post.has("updateContent"))
                                                    {
                                                        JSONObject content = post.getJSONObject("updateContent").getJSONObject("person").getJSONObject("currentShare");

                                                        if (content.has("comment"))
                                                        {
                                                            String comment = content.getString("comment");

                                                            if (doEncrypt)
                                                                comment = em.encryptString(context, comment);

                                                            eventBundle.putString("COMMENT", comment);
                                                        }

                                                        if (content.has("content"))
                                                        {
                                                            JSONObject contentContent = content.getJSONObject("content");

                                                            if (contentContent.has("title"))
                                                                eventBundle.putString("CONTENT_TITLE", contentContent.getString("title"));

                                                            if (contentContent.has("submittedUrl"))
                                                                eventBundle.putString("CONTENT_URL", contentContent.getString("submittedUrl"));

                                                            if (contentContent.has("description"))
                                                                eventBundle.putString("CONTENT_DESCRIPTION", contentContent.getString("description"));
                                                        }
                                                    }

                                                    eventBundle.putBoolean("IS_OBFUSCATED", doEncrypt);

                                                    try
                                                    {
                                                        // Sleep so initial load is delivered in order...
                                                        if (i == 0)
                                                            Thread.sleep(5000);
                                                    }
                                                    catch (InterruptedException e)
                                                    {
                                                        e.printStackTrace();
                                                    }

                                                    me.transmitData(context, eventBundle);
                                                }
                                            }
                                        }

                                        Editor e = prefs.edit();
                                        e.putLong(LinkedInProbe.MOST_RECENT, System.currentTimeMillis());
                                        e.commit();
                                    }
                                    catch (JSONException e)
                                    {
                                        LogManager.getInstance(context).logException(e);
                                    }
                                    catch (IllegalBlockSizeException e)
                                    {
                                        LogManager.getInstance(context).logException(e);
                                    }
                                    catch (BadPaddingException e)
                                    {
                                        LogManager.getInstance(context).logException(e);
                                    }
                                    catch (UnsupportedEncodingException e)
                                    {
                                        LogManager.getInstance(context).logException(e);
                                    }
                                    catch (IllegalStateException e)
                                    {
                                        LogManager.getInstance(context).logException(e);
                                    }
                                }
                            };

                            Thread t = new Thread(r);
                            t.start();
                        }

                        me._lastCheck = now;
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

        intent.putExtra(OAuthActivity.CONSUMER_KEY, context.getString(R.string.linkedin_consumer_key));
        intent.putExtra(OAuthActivity.CONSUMER_SECRET, context.getString(R.string.linkedin_consumer_secret));
        intent.putExtra(OAuthActivity.REQUESTER, "linkedin");
        intent.putExtra(OAuthActivity.CALLBACK_URL, "http://tech.cbits.northwestern.edu/oauth/linkedin");
        intent.putExtra(OAuthActivity.LOG_URL, LogManager.getInstance(context).getLogUrl(context));
        intent.putExtra(OAuthActivity.HASH_SECRET, userId);

        context.startActivity(intent);
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString(LinkedInProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
        map.put(Probe.PROBE_FREQUENCY, freq);

        boolean hash = prefs.getBoolean(LinkedInProbe.ENCRYPT_DATA, LinkedInProbe.DEFAULT_ENCRYPT);
        map.put(Probe.ENCRYPT_DATA, hash);

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(Probe.PROBE_FREQUENCY))
        {
            Object frequency = params.get(Probe.PROBE_FREQUENCY);

            if (frequency instanceof Double)
            {
                frequency = Long.valueOf(((Double) frequency).longValue());
            }

            if (frequency instanceof Long)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(LinkedInProbe.FREQUENCY, frequency.toString());
                e.commit();
            }
        }

        if (params.containsKey(Probe.ENCRYPT_DATA))
        {
            Object encrypt = params.get(Probe.ENCRYPT_DATA);

            if (encrypt instanceof Boolean)
            {
                Boolean encryptBoolean = (Boolean) encrypt;

                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(LinkedInProbe.ENCRYPT_DATA, encryptBoolean.booleanValue());
                e.commit();
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(final Context context, PreferenceManager manager)
    {
        final PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_linkedin_probe_desc);

        final SharedPreferences prefs = Probe.getPreferences(context);

        String token = prefs.getString(LinkedInProbe.OAUTH_TOKEN, null);
        String secret = prefs.getString(LinkedInProbe.OAUTH_SECRET, null);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(LinkedInProbe.ENABLED);
        enabled.setDefaultValue(LinkedInProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleListPreference duration = new FlexibleListPreference(context);
        duration.setKey(LinkedInProbe.FREQUENCY);
        duration.setEntryValues(R.array.probe_low_frequency_values);
        duration.setEntries(R.array.probe_low_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

        screen.addPreference(duration);

        CheckBoxPreference encrypt = new CheckBoxPreference(context);
        encrypt.setKey(LinkedInProbe.ENCRYPT_DATA);
        encrypt.setDefaultValue(LinkedInProbe.DEFAULT_ENCRYPT);
        encrypt.setTitle(R.string.config_probe_linkedin_encrypt_title);
        encrypt.setSummary(R.string.config_probe_linkedin_encrypt_summary);

        screen.addPreference(encrypt);

        final Preference authPreference = new Preference(context);
        authPreference.setTitle(R.string.title_authenticate_linkedin_probe);
        authPreference.setSummary(R.string.summary_authenticate_linkedin_probe);

        final LinkedInProbe me = this;

        final Preference logoutPreference = new Preference(context);
        logoutPreference.setTitle(R.string.title_logout_linkedin_probe);
        logoutPreference.setSummary(R.string.summary_logout_linkedin_probe);

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
                e.remove(LinkedInProbe.OAUTH_TOKEN);
                e.remove(LinkedInProbe.OAUTH_SECRET);
                e.commit();

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
                            Toast.makeText(context, context.getString(R.string.toast_linkedin_logout), Toast.LENGTH_LONG).show();
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
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = new JSONObject();

        try
        {
            JSONArray values = new JSONArray();
            values.put(true);
            values.put(false);

            JSONObject enabled = new JSONObject();
            enabled.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            enabled.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_ENABLED, enabled);

            JSONObject encrypt = new JSONObject();
            encrypt.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            encrypt.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.ENCRYPT_DATA, encrypt);

            JSONObject frequency = new JSONObject();
            frequency.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
            values = new JSONArray();

            String[] options = context.getResources().getStringArray(R.array.probe_low_frequency_values);

            for (String option : options)
            {
                values.put(Long.parseLong(option));
            }

            frequency.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_FREQUENCY, frequency);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        if (bundle.containsKey("COMMENT"))
            return bundle.getString("COMMENT") + " (" + bundle.getString("TYPE") + ")";

        return bundle.getString("TYPE");
    }
}
