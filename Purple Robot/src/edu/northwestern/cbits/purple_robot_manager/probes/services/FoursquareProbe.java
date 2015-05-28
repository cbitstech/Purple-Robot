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

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe;
import edu.northwestern.cbits.xsi.oauth.FoursquareApi;
import edu.northwestern.cbits.xsi.oauth.Keystore;
import edu.northwestern.cbits.xsi.oauth.OAuthActivity;


public class FoursquareProbe extends Probe
{
    private static final boolean DEFAULT_ENABLED = false;
    private static final String OAUTH_TOKEN = "oauth_foursquare_token";
    private static final String OAUTH_SECRET = "oauth_foursquare_secret";
    private static final String ENABLED = "config_probe_foursquare_enabled";
    private static final String FREQUENCY = "config_probe_foursquare_frequency";
    private static final String PULL_ACTIVITY = "config_probe_foursquare_pull_activity";
    private static final boolean DEFAULT_PULL_ACTIVITY = false;
    private static final String MOST_RECENT = "config_foursquare_most_recent";

    private long _lastCheck = 0;

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_foursquare_probe_desc);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_external_services_category);
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.services.FoursquareProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_foursquare_probe);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(FoursquareProbe.ENABLED, true);
        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(FoursquareProbe.ENABLED, false);
        e.commit();
    }

    private void initKeystore(Context context)
    {
        Keystore.put(FoursquareApi.CONSUMER_KEY, context.getString(R.string.foursquare_consumer_key));
        Keystore.put(FoursquareApi.CONSUMER_SECRET, context.getString(R.string.foursquare_consumer_secret));
        Keystore.put(FoursquareApi.CALLBACK_URL, "http://purple.robot.com/oauth/foursquare");
    }

    public boolean isEnabled(final Context context)
    {
        final SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            final long now = System.currentTimeMillis();

            if (prefs.getBoolean(FoursquareProbe.ENABLED, FoursquareProbe.DEFAULT_ENABLED))
            {
                synchronized (this)
                {
                    this.initKeystore(context);

                    long freq = Long.parseLong(prefs.getString(FoursquareProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

                    if (now - this._lastCheck > freq)
                    {
                        final FoursquareProbe me = this;

                        final String token  = prefs.getString(FoursquareProbe.OAUTH_TOKEN, null);
                        final String secret = prefs.getString(FoursquareProbe.OAUTH_SECRET, null);

                        final String title = context.getString(R.string.title_foursquare_check);
                        final SanityManager sanity = SanityManager.getInstance(context);

                        if (token == null || secret == null)
                        {
                            String message = context.getString(R.string.message_foursquare_check);

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

                            Keystore.put(FoursquareApi.USER_TOKEN, token);
                            Keystore.put(FoursquareApi.USER_SECRET, secret);

                            Runnable r = new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    try
                                    {
                                        long mostRecent = prefs.getLong(FoursquareProbe.MOST_RECENT, 0);
                                        long newRecent = 0;

                                        JSONObject checkins = FoursquareApi.fetch("https://api.foursquare.com/v2/users/self/checkins?limit=250&sort=newestfirst&v=20130815");

                                        if (checkins.has("response"))
                                        {
                                            JSONArray items = checkins.getJSONObject("response").getJSONObject("checkins").getJSONArray("items");

                                            for (int i = items.length() - 1; i >= 0; i--)
                                            {
                                                JSONObject item = items.getJSONObject(i);

                                                long created = item.getLong("createdAt") * 1000;

                                                if (created > mostRecent)
                                                {
                                                    if (created > newRecent)
                                                        newRecent = created;

                                                    Bundle eventBundle = new Bundle();
                                                    eventBundle.putString(Probe.BUNDLE_PROBE, me.name(context));
                                                    eventBundle.putLong(Probe.BUNDLE_TIMESTAMP, created / 1000);

                                                    eventBundle.putString("CHECKIN_SOURCE", item.getJSONObject("source").getString("name"));

                                                    if (item.has("venue"))
                                                    {
                                                        JSONObject venue = item.getJSONObject("venue");
                                                        JSONObject location = venue.getJSONObject("location");

                                                        eventBundle.putDouble("LATITUDE", location.getDouble("lat"));
                                                        eventBundle.putDouble("LONGITUDE", location.getDouble("lng"));

                                                        if (location.has("address"))
                                                            eventBundle.putString("ADDRESS", location.getString("address"));

                                                        if (location.has("city"))
                                                            eventBundle.putString("CITY", location.getString("city"));

                                                        if (location.has("state"))
                                                            eventBundle.putString("STATE", location.getString("state"));

                                                        if (location.has("postalCode"))
                                                            eventBundle.putString("POSTAL_CODE", location.getString("postalCode"));

                                                        eventBundle.putString("COUNTRY", location.getString("cc"));

                                                        eventBundle.putString("VENUE_ID", venue.getString("id"));

                                                        if (venue.has("url"))
                                                            eventBundle.putString("VENUE_URL", venue.getString("url"));

                                                        eventBundle.putString("VENUE_NAME", venue.getString("name"));

                                                        JSONArray categories = venue.getJSONArray("categories");

                                                        for (int j = 0; j < categories.length(); j++)
                                                        {
                                                            JSONObject category = categories.getJSONObject(j);

                                                            if (category.getBoolean("primary"))
                                                                eventBundle.putString("VENUE_CATEGORY", category.getString("name"));
                                                        }

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

                                            if (newRecent > 0)
                                            {
                                                Editor e = prefs.edit();
                                                e.putLong(FoursquareProbe.MOST_RECENT, newRecent);
                                                e.commit();
                                            }
                                        }
                                    }
                                    catch (NullPointerException | JSONException | IllegalStateException e)
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

        intent.putExtra(OAuthActivity.CONSUMER_KEY, context.getString(R.string.foursquare_consumer_key));
        intent.putExtra(OAuthActivity.CONSUMER_SECRET, context.getString(R.string.foursquare_consumer_secret));
        intent.putExtra(OAuthActivity.REQUESTER, "foursquare");
        intent.putExtra(OAuthActivity.CALLBACK_URL, "http://purple.robot.com/oauth/foursquare");
        intent.putExtra(OAuthActivity.LOG_URL, LogManager.getInstance(context).getLogUrl(context));
        intent.putExtra(OAuthActivity.HASH_SECRET, userId);

        context.startActivity(intent);
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString(FoursquareProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
        map.put(Probe.PROBE_FREQUENCY, freq);

        boolean pullActivity = prefs.getBoolean(FoursquareProbe.PULL_ACTIVITY, FoursquareProbe.DEFAULT_PULL_ACTIVITY);
        map.put(FoursquareProbe.PULL_ACTIVITY, pullActivity);

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
                frequency = ((Double) frequency).longValue();
            }

            if (frequency instanceof Long)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(FoursquareProbe.FREQUENCY, frequency.toString());
                e.commit();
            }
        }

        if (params.containsKey(FoursquareProbe.PULL_ACTIVITY))
        {
            Object pull = params.get(FoursquareProbe.PULL_ACTIVITY);

            if (pull instanceof Boolean)
            {
                Boolean pullBoolean = (Boolean) pull;

                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(FoursquareProbe.PULL_ACTIVITY, pullBoolean);
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
        screen.setSummary(R.string.summary_foursquare_probe_desc);

        final SharedPreferences prefs = Probe.getPreferences(context);

        String token = prefs.getString(FoursquareProbe.OAUTH_TOKEN, null);
        String secret = prefs.getString(FoursquareProbe.OAUTH_SECRET, null);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(FoursquareProbe.ENABLED);
        enabled.setDefaultValue(FoursquareProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleListPreference duration = new FlexibleListPreference(context);
        duration.setKey(FoursquareProbe.FREQUENCY);
        duration.setEntryValues(R.array.probe_low_frequency_values);
        duration.setEntries(R.array.probe_low_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

        screen.addPreference(duration);

        CheckBoxPreference pull = new CheckBoxPreference(context);
        pull.setKey(FoursquareProbe.PULL_ACTIVITY);
        pull.setDefaultValue(FoursquareProbe.DEFAULT_PULL_ACTIVITY);
        pull.setTitle(R.string.config_probe_foursquare_pull_title);
        pull.setSummary(R.string.config_probe_foursquare_pull_summary);

        screen.addPreference(pull);

        final Preference authPreference = new Preference(context);
        authPreference.setTitle(R.string.title_authenticate_foursquare_probe);
        authPreference.setSummary(R.string.summary_authenticate_foursquare_probe);

        final FoursquareProbe me = this;

        final Preference logoutPreference = new Preference(context);
        logoutPreference.setTitle(R.string.title_logout_foursquare_probe);
        logoutPreference.setSummary(R.string.summary_logout_foursquare_probe);

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
                e.remove(FoursquareProbe.OAUTH_TOKEN);
                e.remove(FoursquareProbe.OAUTH_SECRET);
                e.remove(FoursquareProbe.MOST_RECENT);

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
                            Toast.makeText(context, context.getString(R.string.toast_foursquare_logout), Toast.LENGTH_LONG).show();
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
            settings.put(FoursquareProbe.PULL_ACTIVITY, encrypt);

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

    // TODO: Decide whether to keep or change this method...
    public static void annotate(Context context, Bundle bundle)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean(FoursquareProbe.ENABLED, FoursquareProbe.DEFAULT_ENABLED))
        {
            String urlString = "https://api.foursquare.com/v2/venues/search?client_id=" + context.getString(R.string.foursquare_consumer_key) + "&client_secret=" + context.getString(R.string.foursquare_consumer_secret) + "&v=20130815&ll=";

            double latitude = bundle.getDouble(LocationProbe.LATITUDE);
            double longitude = bundle.getDouble(LocationProbe.LONGITUDE);

            urlString += latitude + "," + longitude;

            try
            {
                URL u = new URL(urlString);

                InputStream in = u.openStream();

                String jsonString = IOUtils.toString(in);

                in.close();

                JSONObject respJson = new JSONObject(jsonString);

                JSONArray venues = respJson.getJSONObject("response").getJSONArray("venues");

                if (venues.length() > 0)
                {
                    JSONObject venue = venues.getJSONObject(0);

                    bundle.putString("FOURSQUARE_NAME", venue.getString("name"));

                    JSONObject location = venue.getJSONObject("location");

                    if (location.has("address"))
                        bundle.putString("FOURSQUARE_ADDRESS", location.getString("address"));

                    if (location.has("city"))
                        bundle.putString("FOURSQUARE_CITY", location.getString("city"));

                    if (location.has("state"))
                        bundle.putString("FOURSQUARE_STATE", location.getString("state"));

                    if (location.has("postalCode"))
                        bundle.putString("FOURSQUARE_POSTAL_CODE", location.getString("postalCode"));

                    bundle.putString("FOURSQUARE_COUNTRY", location.getString("cc"));
                    bundle.putString("FOURSQUARE_VENUE_ID", venue.getString("id"));

                    if (venue.has("url"))
                        bundle.putString("FOURSQUARE_VENUE_URL", venue.getString("url"));

                    JSONArray categories = venue.getJSONArray("categories");

                    String[] venueCategories = new String[categories.length()];

                    for (int j = 0; j < categories.length(); j++)
                    {
                        JSONObject category = categories.getJSONObject(j);

                        venueCategories[j] = category.getString("name");

                        if (category.getBoolean("primary"))
                            bundle.putString("FOURSQUARE_VENUE_CATEGORY", category.getString("name"));
                    }

                    bundle.putStringArray("FOURSQUARE_VENUE_CATEGORIES", venueCategories);
                }
            }
            catch (JSONException | IOException e)
            {
                LogManager.getInstance(context).logException(e);
            }
        }
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        String venue = bundle.getString("VENUE_NAME");

        return context.getString(R.string.summary_foursquare_checkin_summary, venue);
    }
}
