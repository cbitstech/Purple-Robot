package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
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

import com.facebook.AccessToken;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphObjectList;
import com.facebook.model.GraphUser;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.probes.FacebookLoginActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.calibration.FacebookCalibrationHelper;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class FacebookProbe extends Probe
{
    private static final boolean DEFAULT_ENABLED = false;
    private static final boolean DEFAULT_ENCRYPT = true;

    public static final String TOKEN = "facebook_auth_token";

    protected static final String HOUR_COUNT = "HOUR_COUNT";
    private static final String ENABLED = "config_probe_facebook_enabled";
    private static final String FREQUENCY = "config_probe_facebook_frequency";
    private static final String ENCRYPT_DATA = "config_probe_facebook_encrypt_data";

    private long _lastCheck = 0;

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.FacebookProbe";
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
                        FacebookCalibrationHelper.check(context);

                        String token = prefs.getString(FacebookProbe.TOKEN, "");

                        if (token != null && token.trim().length() > 0)
                        {
                            final FacebookProbe me = this;

                            AccessToken accessToken = AccessToken.createFromExistingAccessToken(token, null, null, null, null);

                            me._lastCheck = now;

                            Session.openActiveSessionWithAccessToken(context, accessToken, new Session.StatusCallback()
                            {
                                @Override
                                public void call(final Session session, SessionState state, Exception exception)
                                {
                                    Request friendRequest = Request.newMyFriendsRequest(session, new Request.GraphUserListCallback()
                                    {
                                        @Override
                                        public void onCompleted(List<GraphUser> users, Response response)
                                        {
                                            if (users == null)
                                                return;

                                            final long mostRecent = prefs.getLong("config_probe_facebook_recent", 0);

                                            try
                                            {
                                                final EncryptionManager em = EncryptionManager.getInstance();

                                                final Bundle bundle = new Bundle();
                                                bundle.putString("PROBE", me.name(context));
                                                bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
                                                bundle.putInt("FRIEND_COUNT", users.size());

                                                final ArrayList<GraphObject> items = new ArrayList<GraphObject>();

                                                Request statuses = new Request(session, "/me/statuses", null, HttpMethod.GET, new Request.Callback()
                                                {
                                                    @Override
                                                    @SuppressLint("SimpleDateFormat")
                                                    public void onCompleted(Response response)
                                                    {
                                                        try
                                                        {
                                                            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

                                                            GraphObject obj = response.getGraphObject();

                                                            GraphObjectList<GraphObject> rawPosts = obj.getPropertyAsList("data", GraphObject.class);

                                                            for (GraphObject object : rawPosts)
                                                            {
                                                                items.add(object);
                                                            }

                                                            Request posts = new Request(session, "/me/posts", null, HttpMethod.GET, new Request.Callback()
                                                            {
                                                                @Override
                                                                public void onCompleted(Response response)
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

                                                                        long newRecent = mostRecent;
                                                                        int hour = 0;

                                                                        for (GraphObject object : items)
                                                                        {
                                                                            try
                                                                            {
                                                                                Object createdTime = object.getProperty("created_time");

                                                                                if (createdTime != null)
                                                                                {
                                                                                    Date created = sdf.parse(createdTime.toString());

                                                                                    long postTime = created.getTime();

                                                                                    if (now - postTime < 60 * 60 * 1000)
                                                                                        hour += 1;

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

                                                                                            eventBundle.putBoolean("IS_OBFUSCATED", doHash);
                                                                                            me.transmitData(context, eventBundle);

                                                                                            if (postTime > newRecent)
                                                                                                newRecent = postTime;
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                            catch (ParseException e)
                                                                            {
                                                                                LogManager.getInstance(context).logException(e);
                                                                            }
                                                                            catch (NullPointerException e)
                                                                            {
                                                                                LogManager.getInstance(context).logException(e);
                                                                            }
                                                                        }

                                                                        Editor e = prefs.edit();
                                                                        e.putLong("config_probe_facebook_recent", newRecent);
                                                                        e.commit();

                                                                        bundle.putInt(FacebookProbe.HOUR_COUNT, hour);

                                                                        me.transmitData(context, bundle);
                                                                    }
                                                                    catch (NullPointerException e)
                                                                    {
                                                                        LogManager.getInstance(context).logException(e);
                                                                    }
                                                                }
                                                            });

                                                            posts.executeAsync();
                                                        }
                                                        catch (NullPointerException e)
                                                        {
                                                            LogManager.getInstance(context).logException(e);
                                                        }
                                                    }
                                                });

                                                statuses.executeAsync();
                                            }
                                            catch (Exception e)
                                            {
                                                LogManager.getInstance(context).logException(e);
                                            }
                                        }
                                    });

                                    Bundle params = new Bundle();
                                    params.putString("fields", "id");
                                    friendRequest.setParameters(params);

                                    friendRequest.executeAsync();
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
                e.remove(FacebookProbe.TOKEN);
                e.commit();
            }
        }

        return false;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double count = bundle.getDouble("HOUR_COUNT", 0);
        double friends = bundle.getDouble("FRIEND_COUNT", 0);

        return String.format(context.getResources().getString(R.string.facebook_count_desc), (int) count, (int) friends);
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
                frequency = Long.valueOf(((Double) frequency).longValue());
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

                e.putBoolean(FacebookProbe.ENCRYPT_DATA, encryptBoolean.booleanValue());
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
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_facebook_probe_desc);

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

        Preference calibrate = new Preference(context);
        calibrate.setTitle(R.string.config_probe_calibrate_title);
        calibrate.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference pref)
            {
                Intent intent = new Intent(context, FacebookLoginActivity.class);
                context.startActivity(intent);

                return true;
            }
        });

        screen.addPreference(calibrate);

        return screen;
    }
}
