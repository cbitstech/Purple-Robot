package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class ActivityDetectionProbe extends Probe implements ConnectionCallbacks, OnConnectionFailedListener
{
    private static final boolean DEFAULT_ENABLED = false;

    private static final String ACTIVITY_TYPE = "ACTIVITY_TYPE";
    private static final String ACTIVITY_CONFIDENCE = "ACTIVITY_CONFIDENCE";

    private static final String FREQUENCY = "config_probe_activity_detection_frequency";
    private static final String ENABLED = "config_probe_activity_detection_enabled";

    private static ActivityRecognitionClient _activityDetectionClient = null;
    private Context _context = null;

    private long _lastFreq = 0;

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.ActivityDetectionProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_activity_detection_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_misc_category);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(ActivityDetectionProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(ActivityDetectionProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public boolean isEnabled(Context context)
    {
        final SharedPreferences prefs = Probe.getPreferences(context);

        boolean enabled = super.isEnabled(context);

        if (this._context == null)
            this._context = context.getApplicationContext();

        if (enabled)
            enabled = prefs.getBoolean(ActivityDetectionProbe.ENABLED, ActivityDetectionProbe.DEFAULT_ENABLED);

        if (enabled)
        {
            long interval = Long.parseLong(prefs.getString(ActivityDetectionProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

            if (interval != this._lastFreq && ActivityDetectionProbe._activityDetectionClient != null)
            {
                this._lastFreq = interval;

                if (ActivityDetectionProbe._activityDetectionClient.isConnected())
                    ActivityDetectionProbe._activityDetectionClient.disconnect();

                ActivityDetectionProbe._activityDetectionClient.unregisterConnectionCallbacks(this);
                ActivityDetectionProbe._activityDetectionClient.unregisterConnectionFailedListener(this);

                ActivityDetectionProbe._activityDetectionClient = null;
            }

            if (ActivityDetectionProbe._activityDetectionClient == null)
            {
                int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);

                if (ConnectionResult.SUCCESS == resultCode)
                {
                    ActivityDetectionProbe._activityDetectionClient = new ActivityRecognitionClient(context.getApplicationContext(), this, this);

                    ActivityDetectionProbe._activityDetectionClient.connect();
                }
            }

            return true;
        }
        else if (ActivityDetectionProbe._activityDetectionClient != null)
        {
            ActivityDetectionProbe._activityDetectionClient.unregisterConnectionCallbacks(this);
            ActivityDetectionProbe._activityDetectionClient.unregisterConnectionFailedListener(this);
            ActivityDetectionProbe._activityDetectionClient.disconnect();

            ActivityDetectionProbe._activityDetectionClient = null;
        }

        return false;
    }

    public static void activityDetected(Context context, Intent intent)
    {
        final SharedPreferences prefs = Probe.getPreferences(context);

        boolean enabled = prefs.getBoolean("config_probes_enabled", false);

        if (enabled)
            enabled = prefs.getBoolean(ActivityDetectionProbe.ENABLED, ActivityDetectionProbe.DEFAULT_ENABLED);

        if (enabled && ActivityRecognitionResult.hasResult(intent))
        {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            ActivityDetectionProbe probe = new ActivityDetectionProbe();

            Bundle bundle = new Bundle();

            bundle.putString("PROBE", probe.name(context));
            bundle.putLong("TIMESTAMP", result.getTime() / 1000);

            List<Bundle> activities = ActivityDetectionProbe.parseActivities(result.getProbableActivities());

            bundle.putParcelableArrayList("ACTIVITIES", (ArrayList<? extends Parcelable>) activities);
            bundle.putInt("ACTIVITY_COUNT", activities.size());

            DetectedActivity probable = result.getMostProbableActivity();

            bundle.putString("MOST_PROBABLE_ACTIVITY", ActivityDetectionProbe.activityName(probable.getType()));
            bundle.putInt("MOST_PROBABLE_CONFIDENCE", probable.getConfidence());

            probe.transmitData(context, bundle);
        }
    }

    public static String activityName(int activity)
    {
        switch (activity)
        {
        case DetectedActivity.IN_VEHICLE:
            return "IN_VEHICLE";
        case DetectedActivity.ON_BICYCLE:
            return "ON_BICYCLE";
        case DetectedActivity.ON_FOOT:
            return "ON_FOOT";
        case DetectedActivity.STILL:
            return "STILL";
        case DetectedActivity.TILTING:
            return "TILTING";
        case DetectedActivity.RUNNING:
            return "RUNNING";
        case DetectedActivity.WALKING:
            return "WALKING";
        }

        return "UNKNOWN";
    }

    public static List<Bundle> parseActivities(List<DetectedActivity> activities)
    {
        ArrayList<Bundle> bundles = new ArrayList<Bundle>();

        for (DetectedActivity activity : activities)
        {
            Bundle b = new Bundle();
            b.putString(ActivityDetectionProbe.ACTIVITY_TYPE, ActivityDetectionProbe.activityName(activity.getType()));
            b.putInt(ActivityDetectionProbe.ACTIVITY_CONFIDENCE, activity.getConfidence());

            bundles.add(b);
        }

        return bundles;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double confidence = bundle.getDouble("MOST_PROBABLE_CONFIDENCE");
        String activity = bundle.getString("MOST_PROBABLE_ACTIVITY");

        return context.getResources().getString(R.string.summary_activity_detection_probe, activity, confidence);
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString(ActivityDetectionProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
        map.put(Probe.PROBE_FREQUENCY, freq);

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

                e.putString(ActivityDetectionProbe.FREQUENCY, frequency.toString());
                e.commit();
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(PreferenceActivity activity)
    {
        PreferenceManager manager = activity.getPreferenceManager();

        PreferenceScreen screen = manager.createPreferenceScreen(activity);
        screen.setTitle(this.title(activity));
        screen.setSummary(R.string.summary_activity_detection_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(ActivityDetectionProbe.ENABLED);
        enabled.setDefaultValue(ActivityDetectionProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        ListPreference duration = new ListPreference(activity);
        duration.setKey(ActivityDetectionProbe.FREQUENCY);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);
        duration.setEntryValues(R.array.probe_activity_recognition_frequency_values);
        duration.setEntries(R.array.probe_activity_recognition_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);

        screen.addPreference(duration);

        return screen;
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

            JSONObject frequency = new JSONObject();
            frequency.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
            values = new JSONArray();

            String[] options = this._context.getResources().getStringArray(R.array.probe_activity_recognition_frequency_values);

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
    public String summary(Context context)
    {
        return context.getString(R.string.summary_activity_detection_probe_desc);
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        final SharedPreferences prefs = Probe.getPreferences(this._context);

        long interval = Long.parseLong(prefs.getString(ActivityDetectionProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

        Intent intent = new Intent(ManagerService.GOOGLE_PLAY_ACTIVITY_DETECTED);
        PendingIntent pendingIntent = PendingIntent.getService(this._context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (ActivityDetectionProbe._activityDetectionClient.isConnected())
            ActivityDetectionProbe._activityDetectionClient.requestActivityUpdates(interval, pendingIntent);
        else
            ActivityDetectionProbe._activityDetectionClient.connect();
    }

    @Override
    public void onDisconnected()
    {

    }

    @Override
    public void onConnectionFailed(ConnectionResult result)
    {

    }
}
