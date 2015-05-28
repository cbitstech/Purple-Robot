package edu.northwestern.cbits.purple_robot_manager.probes.services;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.model.GraphObject;
import com.facebook.Response;
import com.facebook.model.GraphObjectList;

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

import edu.emory.mathcs.backport.java.util.Collections;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.calibration.FacebookCalibrationHelper;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.xsi.facebook.FacebookApi;
import edu.northwestern.cbits.xsi.facebook.FacebookLoginActivity;

public class FacebookProbe extends Probe
{
    private static final boolean DEFAULT_ENABLED = false;
    private static final boolean DEFAULT_ENCRYPT = true;

    protected static final String HOUR_COUNT = "HOUR_COUNT";
    private static final String ENABLED = "config_probe_facebook_enabled";
    private static final String FREQUENCY = "config_probe_facebook_frequency";
    private static final String ENCRYPT_DATA = "config_probe_facebook_encrypt_data";
    private static final String DAY_COUNT = "DAY_COUNT";

    private long _lastCheck = 0;

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.services.FacebookProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_facebook_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_external_services_category);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(FacebookProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(FacebookProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        final SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            final long now = System.currentTimeMillis();

            if (prefs.getBoolean(FacebookProbe.ENABLED, FacebookProbe.DEFAULT_ENABLED))
            {
                synchronized (this)
                {
                    long freq = Long.parseLong(prefs.getString(FacebookProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
                    final boolean doHash = prefs.getBoolean(FacebookProbe.ENCRYPT_DATA, FacebookProbe.DEFAULT_ENCRYPT);

                    if (now - this._lastCheck > freq)
                    {
                        this._lastCheck = now;

                        FacebookCalibrationHelper.check(context);

                        final String token = prefs.getString(FacebookApi.TOKEN, "");

                        if (token != null && token.trim().length() > 0)
                        {
                            final FacebookProbe me = this;

                            final Bundle bundle = new Bundle();
                            bundle.putString("PROBE", me.name(context));
                            bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                            final ArrayList<GraphObject> items = new ArrayList<>();
                            final EncryptionManager em = EncryptionManager.getInstance();

                            FacebookApi.fetchRequest(context, token, "/me/statuses", new FacebookApi.OnRequestCallback()
                            {
                                @Override
                                public void onRequestCompleted(Response response)
                                {
                                    try
                                    {
                                        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

                                        GraphObject obj = response.getGraphObject();

                                        GraphObjectList<GraphObject> rawPosts = obj.getPropertyAsList("data", GraphObject.class);

                                        for (GraphObject object : rawPosts) {
                                            items.add(object);
                                        }

                                        FacebookApi.fetchRequest(context, token, "/me/posts", new FacebookApi.OnRequestCallback()
                                        {
                                            @Override
                                            public void onRequestCompleted(Response response)
                                            {
                                                try
                                                {
                                                    GraphObject obj = response.getGraphObject();

                                                    GraphObjectList<GraphObject> rawPosts = obj.getPropertyAsList("data", GraphObject.class);

                                                    for (GraphObject object : rawPosts)
                                                    {
                                                        items.add(object);
                                                    }

                                                    Collections.sort(items, new Comparator<GraphObject>()
                                                    {
                                                        @Override
                                                        public int compare(GraphObject one, GraphObject two)
                                                        {
                                                            String oneTime = one.getProperty("updated_time").toString();
                                                            String twoTime = two.getProperty("updated_time").toString();

                                                            return oneTime.compareTo(twoTime);
                                                        }
                                                    });

                                                    final long mostRecent = prefs.getLong("config_probe_facebook_recent", 0);

                                                    long newRecent = mostRecent;
                                                    int hour = 0;
                                                    int day = 0;

                                                    for (int i = items.size() - 1; i >= 0 ; i--)
                                                    {
                                                        GraphObject object = items.get(i);

                                                        try
                                                        {
                                                            Object createdTime = object.getProperty("created_time");

                                                            if (createdTime != null) {
                                                                Date created = sdf.parse(createdTime.toString());

                                                                long postTime = created.getTime();

                                                                if (now - postTime < 60 * 60 * 1000)
                                                                    hour += 1;

                                                                if (now - postTime < (60 * 60 * 1000 * 24))
                                                                    day += 1;

                                                                if (postTime > mostRecent)
                                                                {
                                                                    Object o = object.getProperty("message");

                                                                    if (o != null)
                                                                    {
                                                                        Bundle eventBundle = new Bundle();
                                                                        eventBundle.putString("PROBE", FacebookEventsProbe.PROBE_NAME);
                                                                        eventBundle.putLong("TIMESTAMP", postTime / 1000);
                                                                        eventBundle.putString("TYPE", object.getProperty("type").toString());

                                                                        String message = o.toString();

                                                                        try
                                                                        {
                                                                            if (doHash)
                                                                                message = em.encryptString(context, message);

                                                                            eventBundle.putString("MESSAGE", message);
                                                                        }
                                                                        catch (IllegalBlockSizeException | UnsupportedEncodingException | BadPaddingException e)
                                                                        {
                                                                            LogManager.getInstance(context).logException(e);
                                                                        }

                                                                        eventBundle.putBoolean("IS_OBFUSCATED", doHash);

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

                                                                        if (postTime > newRecent)
                                                                            newRecent = postTime;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        catch (ParseException | NullPointerException e)
                                                        {
                                                            LogManager.getInstance(context).logException(e);
                                                        }
                                                    }

                                                    Editor e = prefs.edit();
                                                    e.putLong("config_probe_facebook_recent", newRecent);
                                                    e.commit();

                                                    bundle.putInt(FacebookProbe.HOUR_COUNT, hour);
                                                    bundle.putInt(FacebookProbe.DAY_COUNT, day);

                                                    me.transmitData(context, bundle);
                                                }
                                                catch (NullPointerException e)
                                                {
                                                    LogManager.getInstance(context).logException(e);
                                                }
                                            }
                                        });
                                    }
                                    catch (NullPointerException e)
                                    {
                                        LogManager.getInstance(context).logException(e);
                                    }
                                }
                            });
                        }
                    }
                }

                return true;
            }
            else
            {
                Editor e = prefs.edit();
                e.remove(FacebookApi.TOKEN);
                e.commit();
            }
        }

        final SanityManager sanity = SanityManager.getInstance(context);
        final String title = context.getString(R.string.title_facebook_check);

        sanity.clearAlert(title);

        return false;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double hour = bundle.getDouble(FacebookProbe.HOUR_COUNT, 0);
        double day = bundle.getDouble(FacebookProbe.DAY_COUNT, 0);

        if (hour != 1 && day != 1)
            return String.format(context.getResources().getString(R.string.facebook_count_desc), (int) hour, (int) day);
        else if (hour == 1 && day != 1)
            return String.format(context.getResources().getString(R.string.facebook_count_desc_single_hour), (int) day);
        else if (day == 1 && hour != 1)
            return String.format(context.getResources().getString(R.string.facebook_count_desc_single_day), (int) hour);

        return context.getResources().getString(R.string.facebook_count_desc_single_both);
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString(FacebookProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
        map.put(Probe.PROBE_FREQUENCY, freq);

        boolean hash = prefs.getBoolean(FacebookProbe.ENCRYPT_DATA, FacebookProbe.DEFAULT_ENCRYPT);
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
                frequency = ((Double) frequency).longValue();
            }

            if (frequency instanceof Long)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(FacebookProbe.FREQUENCY, frequency.toString());
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

                e.putBoolean(FacebookProbe.ENCRYPT_DATA, encryptBoolean);
                e.commit();
            }
        }
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_facebook_probe_desc);
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
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(final  Context context, PreferenceManager manager)
    {
        final PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_facebook_probe_desc);

        final SharedPreferences prefs = Probe.getPreferences(context);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(FacebookProbe.ENABLED);
        enabled.setDefaultValue(FacebookProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleListPreference duration = new FlexibleListPreference(context);
        duration.setKey(FacebookProbe.FREQUENCY);
        duration.setEntryValues(R.array.probe_low_frequency_values);
        duration.setEntries(R.array.probe_low_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

        screen.addPreference(duration);

        CheckBoxPreference encrypt = new CheckBoxPreference(context);
        encrypt.setKey(FacebookProbe.ENCRYPT_DATA);
        encrypt.setDefaultValue(FacebookProbe.DEFAULT_ENCRYPT);
        encrypt.setTitle(R.string.config_probe_facebook_encrypt_title);
        encrypt.setSummary(R.string.config_probe_facebook_encrypt_summary);

        screen.addPreference(encrypt);

        final Preference authPreference = new Preference(context);
        authPreference.setTitle(R.string.title_authenticate_facebook_probe);
        authPreference.setSummary(R.string.summary_authenticate_facebook_probe);

        final Preference logoutPreference = new Preference(context);
        logoutPreference.setTitle(R.string.title_logout_facebook_probe);
        logoutPreference.setSummary(R.string.summary_logout_facebook_probe);

        authPreference.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                Intent intent = new Intent(context, FacebookLoginActivity.class);
                intent.putExtra(FacebookLoginActivity.APP_ID, context.getString(R.string.facebook_app_id));
                context.startActivity(intent);

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
                e.remove(FacebookApi.TOKEN);
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
                            Toast.makeText(context, context.getString(R.string.toast_facebook_logout), Toast.LENGTH_LONG).show();
                        }
                    });
                }


                return true;
            }
        });

        final String token = prefs.getString(FacebookApi.TOKEN, null);

        if (token == null)
            screen.addPreference(authPreference);
        else
            screen.addPreference(logoutPreference);

        return screen;
    }
}
