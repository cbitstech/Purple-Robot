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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.TimeZone;

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

//    private static final String BLOOD_OXYGEN_ENABLED = "config_feature_ihealth_blood_oxygen_enabled";
//    private static final boolean BLOOD_OXYGEN_DEFAULT = false;

    private static final String BLOOD_PRESSURE_ENABLED = "config_feature_ihealth_blood_pressure_enabled";
    private static final boolean BLOOD_PRESSURE_DEFAULT = false;

    private static final String WEIGHT_ENABLED = "config_feature_ihealth_weight_enabled";
    private static final boolean WEIGHT_DEFAULT = false;

//    private static final String SLEEP_ENABLED = "config_feature_ihealth_sleep_enabled";
//    private static final boolean SLEEP_DEFAULT = true;

    private static final String ACTIVITY_ENABLED = "config_feature_ihealth_activity_enabled";
    private static final boolean ACTIVITY_DEFAULT = true;


    private static final String OAUTH_TOKEN = "oauth_ihealth_token";
    private static final String OAUTH_SECRET = "oauth_ihealth_secret";

    private static final String ACTIVITY_DATA_ID = "ACTIVITY_DATA_ID";
    private static final String ACTIVITY_LATITUDE = "ACTIVITY_LATITUDE";
    private static final String ACTIVITY_LONGITUDE = "ACTIVITY_LONGITUDE";
    private static final String ACTIVITY_NOTE = "ACTIVITY_NOTE";
    private static final String ACTIVITY_DATA_SOURCE = "ACTIVITY_DATA_SOURCE";
    private static final String ACTIVITY_CALORIES = "ACTIVITY_CALORIES";
    private static final String ACTIVITY_DISTANCE_TRAVELLED = "ACTIVITY_DISTANCE_TRAVELLED";
    private static final String ACTIVITY_STEPS = "ACTIVITY_STEPS";

    private static final String WEIGHT_DATA_ID = "WEIGHT_DATA_ID";
    private static final String WEIGHT_NOTE = "WEIGHT_NOTE";
    private static final String WEIGHT_DATA_SOURCE = "WEIGHT_DATA_SOURCE";
    private static final String WEIGHT_BMI = "WEIGHT_BMI";
    private static final String WEIGHT_BONE_VALUE = "WEIGHT_BONE_VALUE";
    private static final String WEIGHT_DCI = "WEIGHT_DCI";
    private static final String WEIGHT_FAT_VALUE = "WEIGHT_FAT_VALUE";
    private static final String WEIGHT_MUSCLE_VALUE = "WEIGHT_MUSCLE_VALUE";
    private static final String WEIGHT_WATER_VALUE = "WEIGHT_WATER_VALUE";
    private static final String WEIGHT_WEIGHT_VALUE = "WEIGHT_WEIGHT_VALUE";

    private static final String PRESSURE_DATA_ID = "PRESSURE_DATA_ID";
    private static final String PRESSURE_LATITUDE = "PRESSURE_LATITUDE";
    private static final String PRESSURE_LONGITUDE = "PRESSURE_LONGITUDE";
    private static final String PRESSURE_NOTE = "PRESSURE_NOTE";
    private static final String PRESSURE_DATA_SOURCE = "PRESSURE_DATA_SOURCE";
    private static final String PRESSURE_BPL = "PRESSURE_BPL";
    private static final String PRESSURE_SYSTOLIC = "PRESSURE_SYSTOLIC";
    private static final String PRESSURE_DIASTOLIC = "PRESSURE_DIASTOLIC";
    private static final String PRESSURE_IS_ARRHYTHMIA = "PRESSURE_IS_ARRYTHMIA";
    private static final String PRESSURE_HEART_RATE = "PRESSURE_HEART_RATE";

    private static final String GLUCOSE_DATA_ID = "GLUCOSE_DATA_ID";
    private static final String GLUCOSE_LATITUDE = "GLUCOSE_LATITUDE";
    private static final String GLUCOSE_LONGITUDE = "GLUCOSE_LONGITUDE";
    private static final String GLUCOSE_NOTE = "GLUCOSE_NOTE";
    private static final String GLUCOSE_DATA_SOURCE = "GLUCOSE_DATA_SOURCE";
    private static final String GLUCOSE_BG = "ACTIVITY_CALORIES";

    private long _lastUpdate = 0;

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double steps = bundle.getDouble(iHealthProbe.ACTIVITY_STEPS, -1);
        double weight = bundle.getDouble(iHealthProbe.WEIGHT_WEIGHT_VALUE, -1);
        double systolic = bundle.getDouble(iHealthProbe.PRESSURE_SYSTOLIC, -1);
        double diastolic = bundle.getDouble(iHealthProbe.PRESSURE_DIASTOLIC, -1);
        double glucose = bundle.getDouble(iHealthProbe.GLUCOSE_BG, -1);

        StringBuffer sb = new StringBuffer();

        if (steps != -1)
            sb.append(context.getString(R.string.summary_ihealth_steps, steps));

        if (weight != -1)
        {
            if (sb.length() > 0)
                sb.append(", ");

            sb.append(context.getString(R.string.summary_ihealth_weight, weight));
        }

        if (systolic != -1 && diastolic != -1)
        {
            if (sb.length() > 0)
                sb.append(", ");

            sb.append(context.getString(R.string.summary_ihealth_pressure, systolic, diastolic));
        }

        if (glucose != -1)
        {
            if (sb.length() > 0)
                sb.append(", ");

            sb.append(context.getString(R.string.summary_ihealth_glucose, glucose));
        }

        return sb.toString();
    }

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

        Keystore.put(iHealthApi.REDIRECT_URL, "http://purple.robot.com/oauth/ihealth");
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

        /*
        CheckBoxPreference sleepPref = new CheckBoxPreference(context);
        sleepPref.setKey(iHealthProbe.SLEEP_ENABLED);
        sleepPref.setDefaultValue(iHealthProbe.SLEEP_DEFAULT);
        sleepPref.setTitle(R.string.config_ihealth_sleep_title);
        screen.addPreference(sleepPref);

        CheckBoxPreference bloodOxygenPref = new CheckBoxPreference(context);
        bloodOxygenPref.setKey(iHealthProbe.BLOOD_OXYGEN_ENABLED);
        bloodOxygenPref.setDefaultValue(iHealthProbe.BLOOD_OXYGEN_DEFAULT);
        bloodOxygenPref.setTitle(R.string.config_ihealth_blood_oxygen_title);
        screen.addPreference(bloodOxygenPref);
        */

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

//            settings.put(iHealthProbe.SLEEP_ENABLED, enabled);
            settings.put(iHealthProbe.ACTIVITY_ENABLED, enabled);
            settings.put(iHealthProbe.WEIGHT_ENABLED, enabled);
            settings.put(iHealthProbe.BLOOD_GLUCOSE_ENABLED, enabled);
//            settings.put(iHealthProbe.BLOOD_OXYGEN_ENABLED, enabled);
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

                                    calendar.add(Calendar.DATE, -1);

                                    long start = calendar.getTimeInMillis();

                                    calendar.add(Calendar.DATE, 2);

                                    long end = calendar.getTimeInMillis();

                                    calendar = Calendar.getInstance();
                                    calendar.set(Calendar.HOUR, 0);
                                    calendar.set(Calendar.MINUTE, 0);
                                    calendar.set(Calendar.SECOND, 0);
                                    calendar.set(Calendar.MILLISECOND, 0);

                                    long todayStart = calendar.getTimeInMillis() / 1000;

                                    Bundle bundle = new Bundle();
                                    bundle.putString("PROBE", me.name(context));
                                    bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                                    boolean hasData = false;

                                    if (prefs.getBoolean(iHealthProbe.BLOOD_GLUCOSE_ENABLED, iHealthProbe.BLOOD_GLUCOSE_DEFAULT))
                                    {
                                        JSONObject glucose = iHealthApi.fetchBloodGlucose(context, start / 1000, end / 1000);

//                                        Log.e("PR-iHealth", "FETCHED GLUCOSE: " + glucose.toString(2));

                                        if (glucose.has("Error") == false)
                                        {
                                            JSONArray dataList = glucose.getJSONArray("BGDataList");

                                            for (int i = 0; i < dataList.length(); i++) {
                                                JSONObject data = dataList.getJSONObject(i);

                                                TimeZone tz = TimeZone.getTimeZone("GMT" + data.getString("TimeZone"));

                                                long modified = data.getLong("MDate");

                                                int offset = tz.getOffset(modified * 1000);

                                                if (modified > todayStart + (offset / 1000)) {
                                                    bundle.putString(iHealthProbe.GLUCOSE_DATA_ID, data.getString("DataID"));
                                                    bundle.putString(iHealthProbe.GLUCOSE_DATA_SOURCE, data.getString("DataSource"));
                                                    bundle.putString(iHealthProbe.GLUCOSE_LATITUDE, data.getString("Lat"));
                                                    bundle.putString(iHealthProbe.GLUCOSE_LONGITUDE, data.getString("Lon"));

                                                    if (data.getString("Note").trim().length() > 0)
                                                        bundle.putString(iHealthProbe.GLUCOSE_NOTE, data.getString("Note"));

                                                    bundle.putDouble(iHealthProbe.GLUCOSE_BG, data.getDouble("BG"));

                                                    // Dinner situation
                                                    // Drug situation

                                                    hasData = true;
                                                }
                                            }
                                        }
                                    }

/*                                    if (prefs.getBoolean(iHealthProbe.BLOOD_OXYGEN_ENABLED, iHealthProbe.BLOOD_OXYGEN_DEFAULT))
                                    {
                                        // http://sandboxapi.ihealthlabs.com/openapiv2/application/spo2.json/?client_id=ddb9cbc759*****&client_secret=4738f9d00e*****&redirect_uri=http%3a%2f%2f+yourcallback.com%2f%3fthis%3dthat&access_token=xpoBt0ThQQ*****&start_time=1342005726&end_time=1405077726&page_index=1&sc=d63493704c*****&sv=88f34288d5*****
                                    }
*/

                                    if (prefs.getBoolean(iHealthProbe.BLOOD_PRESSURE_ENABLED, iHealthProbe.BLOOD_PRESSURE_DEFAULT))
                                    {
                                        JSONObject pressure = iHealthApi.fetchBloodPressure(context, start / 1000, end / 1000);

//                                        Log.e("PR-iHealth", "FETCHED PRESSURE: " + pressure.toString(2));

                                        if (pressure.has("Error") == false)
                                        {
                                            JSONArray dataList = pressure.getJSONArray("BPDataList");

                                            for (int i = 0; i < dataList.length(); i++)
                                            {
                                                JSONObject data = dataList.getJSONObject(i);

                                                TimeZone tz = TimeZone.getTimeZone("GMT" + data.getString("TimeZone"));

                                                long modified = data.getLong("MDate");

                                                int offset = tz.getOffset(modified * 1000);

                                                if (modified > todayStart + (offset / 1000))
                                                {
//                                                    Log.e("PR", "BP INCLUDE");
                                                    bundle.putString(iHealthProbe.PRESSURE_DATA_ID, data.getString("DataID"));
                                                    bundle.putString(iHealthProbe.PRESSURE_DATA_SOURCE, data.getString("DataSource"));
                                                    bundle.putDouble(iHealthProbe.PRESSURE_LATITUDE, data.getDouble("Lat"));
                                                    bundle.putDouble(iHealthProbe.PRESSURE_LONGITUDE, data.getDouble("Lon"));

                                                    if (data.getString("Note").trim().length() > 0)
                                                        bundle.putString(iHealthProbe.PRESSURE_NOTE, data.getString("Note"));

                                                    bundle.putLong(iHealthProbe.PRESSURE_BPL, data.getLong("BPL"));
                                                    bundle.putLong(iHealthProbe.PRESSURE_SYSTOLIC, data.getLong("HP"));
                                                    bundle.putLong(iHealthProbe.PRESSURE_DIASTOLIC, data.getLong("LP"));
                                                    bundle.putDouble(iHealthProbe.PRESSURE_HEART_RATE, data.getDouble("HR"));

                                                    int isArr = data.getInt("IsArr");

                                                    switch (isArr)
                                                    {
                                                        case -1:
                                                            bundle.putString(iHealthProbe.PRESSURE_IS_ARRHYTHMIA, "NULL");
                                                            break;
                                                        case 1:
                                                            bundle.putString(iHealthProbe.PRESSURE_IS_ARRHYTHMIA, "NORMAL");
                                                            break;
                                                        case 2:
                                                            bundle.putString(iHealthProbe.PRESSURE_IS_ARRHYTHMIA, "ARRHYTHMIA_CORDIS");
                                                            break;
                                                        default:
                                                            bundle.putString(iHealthProbe.PRESSURE_IS_ARRHYTHMIA, "UNKNOWN");
                                                    }

                                                    hasData = true;
                                                }
//                                                else
//                                                    Log.e("PR", "BP FAIL: " + modified + " >? " + (todayStart + (offset / 1000)));
                                            }
                                        }
                                    }

                                    if (prefs.getBoolean(iHealthProbe.ACTIVITY_ENABLED, iHealthProbe.ACTIVITY_DEFAULT))
                                    {
                                        JSONObject activity = iHealthApi.fetchActivity(context, start / 1000, end / 1000);

                                        if (activity.has("Error") == false)
                                        {
                                            JSONArray dataList = activity.getJSONArray("ARDataList");

                                            for (int i = 0; i < dataList.length(); i++)
                                            {
                                                JSONObject data = dataList.getJSONObject(i);

                                                TimeZone tz = TimeZone.getTimeZone("GMT" + data.getString("TimeZone"));

                                                long modified = data.getLong("MDate");

                                                int offset = tz.getOffset(modified * 1000);

                                                if (modified > todayStart + (offset / 1000))
                                                {
                                                    bundle.putString(iHealthProbe.ACTIVITY_DATA_ID, data.getString("DataID"));
                                                    bundle.putLong(iHealthProbe.ACTIVITY_CALORIES, data.getLong("Calories"));
                                                    bundle.putString(iHealthProbe.ACTIVITY_DATA_SOURCE, data.getString("DataSource"));
                                                    bundle.putDouble(iHealthProbe.ACTIVITY_DISTANCE_TRAVELLED, data.getDouble("DistanceTraveled"));
                                                    bundle.putDouble(iHealthProbe.ACTIVITY_LATITUDE, data.getDouble("Lat"));
                                                    bundle.putDouble(iHealthProbe.ACTIVITY_LONGITUDE, data.getDouble("Lon"));
                                                    bundle.putLong(iHealthProbe.ACTIVITY_STEPS, data.getLong("Steps"));

                                                    if (data.getString("Note").trim().length() > 0)
                                                        bundle.putString(iHealthProbe.ACTIVITY_NOTE, data.getString("Note"));

                                                    hasData = true;
                                                }
                                            }
                                        }
                                    }

//                                    if (prefs.getBoolean(iHealthProbe.SLEEP_ENABLED, iHealthProbe.SLEEP_DEFAULT))
//                                    {
//                                        // http://sandboxapi.ihealthlabs.com/openapiv2/application/sleep.json/?client_id=ddb9cbc759*****&client_secret=4738f9d00e*****&redirect_uri=http%3a%2f%2f+yourcallback.com%2f%3fthis%3dthat&access_token=xpoBt0ThQQ*****&start_time=1342004920&end_time=1405076920&page_index=1&sc=d63493704c*****&sv=68d3f571e6*****
//                                    }

                                    if (prefs.getBoolean(iHealthProbe.WEIGHT_ENABLED, iHealthProbe.WEIGHT_DEFAULT))
                                    {
                                        JSONObject weight = iHealthApi.fetchWeight(context, start / 1000, end / 1000);

//                                        Log.e("PR-iHealth", "FETCHED WEIGHT: " + weight.toString(2));

                                        if (weight.has("Error") == false)
                                        {
                                            JSONArray dataList = weight.getJSONArray("WeightDataList");

                                            for (int i = 0; i < dataList.length(); i++)
                                            {
                                                JSONObject data = dataList.getJSONObject(i);

                                                TimeZone tz = TimeZone.getTimeZone("GMT" + data.getString("TimeZone"));

                                                long modified = data.getLong("MDate");

                                                int offset = tz.getOffset(modified * 1000);

                                                if (modified > todayStart + (offset / 1000))
                                                {
//                                                    Log.e("PR", "WEIGHT INCLUDE");
                                                    bundle.putString(iHealthProbe.WEIGHT_DATA_ID, data.getString("DataID"));
                                                    bundle.putString(iHealthProbe.WEIGHT_DATA_SOURCE, data.getString("DataSource"));

                                                    if (data.getString("Note").trim().length() > 0)
                                                        bundle.putString(iHealthProbe.WEIGHT_NOTE, data.getString("Note"));

                                                    bundle.putDouble(iHealthProbe.WEIGHT_BMI, data.getDouble("BMI"));
                                                    bundle.putDouble(iHealthProbe.WEIGHT_BONE_VALUE, data.getDouble("BoneValue"));
                                                    bundle.putDouble(iHealthProbe.WEIGHT_DCI, data.getDouble("DCI"));
                                                    bundle.putDouble(iHealthProbe.WEIGHT_FAT_VALUE, data.getDouble("FatValue"));
                                                    bundle.putDouble(iHealthProbe.WEIGHT_MUSCLE_VALUE, data.getDouble("MuscaleValue")); // Misspelling is same as API...
                                                    bundle.putDouble(iHealthProbe.WEIGHT_WATER_VALUE, data.getDouble("WaterValue"));
                                                    bundle.putDouble(iHealthProbe.WEIGHT_WEIGHT_VALUE, data.getDouble("WeightValue"));

                                                    hasData = true;
                                                }
//                                                else
//                                                    Log.e("PR", "WEIGHT FAIL: " + modified + " >? " + (todayStart + (offset / 1000)));
                                            }
                                        }

                                    }

                                    if (hasData)
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
}
