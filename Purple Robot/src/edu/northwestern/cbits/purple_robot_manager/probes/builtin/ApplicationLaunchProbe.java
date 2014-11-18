package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.Map;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class ApplicationLaunchProbe extends Probe
{
    private static final boolean DEFAULT_ENABLED = false;

    private static final String WAKE_ACTION = "ACTIVITY_LAUNCH_WAKE";
    private static final String DEFAULT_FREQUENCY = "100";

    private static final String ENABLED = "config_probe_application_launch_enabled";
    private static final String FREQUENCY = "config_probe_application_launch_frequency";

    private PendingIntent _pollIntent = null;
    private final long _lastInterval = 0;

    private String _lastPkgName = null;
    private String _lastName = null;
    private long _lastStart = 0;

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.ApplicationLaunchProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_application_launch_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_device_info_category);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(ApplicationLaunchProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(ApplicationLaunchProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        boolean disable = false;
        long interval = 0;
        boolean set = false;

        boolean isEnabled = false;

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(ApplicationLaunchProbe.ENABLED, ApplicationLaunchProbe.DEFAULT_ENABLED))
            {
                interval = Long.parseLong(prefs.getString(ApplicationLaunchProbe.FREQUENCY, "10"));

                if (interval != this._lastInterval)
                {
                    disable = true;
                    set = true;

                    isEnabled = true;
                }
            }
            else
                disable = true;
        }
        else
            disable = true;

        final ApplicationLaunchProbe me = this;

        if (this._pollIntent == null)
        {
            Intent intent = new Intent(ApplicationLaunchProbe.WAKE_ACTION);
            this._pollIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            context.registerReceiver(new BroadcastReceiver()
            {
                @Override
                @SuppressWarnings("deprecation")
                public void onReceive(final Context context, Intent intent)
                {
                    ActivityManager activityManager = (ActivityManager) context
                            .getSystemService(Context.ACTIVITY_SERVICE);

                    RunningTaskInfo foregroundTaskInfo = activityManager.getRunningTasks(1).get(0);
                    final String pkgName = foregroundTaskInfo.topActivity.getPackageName();

                    PackageManager packageManager = context.getPackageManager();

                    ApplicationInfo applicationInfo = null;

                    try
                    {
                        applicationInfo = packageManager.getApplicationInfo(pkgName, 0);
                    }
                    catch (final NameNotFoundException e)
                    {

                    }

                    final String name = (String) ((applicationInfo != null) ? packageManager
                            .getApplicationLabel(applicationInfo) : "???");

                    if (pkgName.equals(me._lastPkgName) == false)
                    {
                        Runnable r = new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Bundle bundle = new Bundle();

                                if (me._lastPkgName != null)
                                {
                                    bundle.putString("PREVIOUS_APP_PKG", me._lastPkgName);
                                    bundle.putString("PREVIOUS_APP_NAME", me._lastName);
                                    bundle.putString("PREVIOUS_CATEGORY",
                                            RunningSoftwareProbe.fetchCategory(context, me._lastPkgName));
                                    bundle.putLong("PREVIOUS_TIMESTAMP", me._lastStart);
                                }

                                me._lastPkgName = pkgName;
                                me._lastName = name;

                                bundle.putString("PROBE", me.name(context));
                                bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
                                bundle.putString("CURRENT_APP_PKG", pkgName);
                                bundle.putString("CURRENT_APP_NAME", name);
                                bundle.putString("CURRENT_CATEGORY",
                                        RunningSoftwareProbe.fetchCategory(context, pkgName));

                                me.transmitData(context, bundle);

                                me._lastStart = bundle.getLong("TIMESTAMP");
                            }
                        };

                        Thread t = new Thread(r);
                        t.start();
                    }
                }
            }, new IntentFilter(ApplicationLaunchProbe.WAKE_ACTION));
        }

        if (disable && this._pollIntent != null)
            alarm.cancel(this._pollIntent);

        if (set)
            alarm.setRepeating(AlarmManager.ELAPSED_REALTIME, 0, interval, this._pollIntent);

        return isEnabled;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        String app = bundle.getString("CURRENT_APP_NAME");
        String category = bundle.getString("CURRENT_CATEGORY");

        return String.format(context.getResources().getString(R.string.summary_app_launch_probe), app, category);
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString(ApplicationLaunchProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

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

            if (frequency instanceof Long)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(ApplicationLaunchProbe.FREQUENCY, frequency.toString());
                e.commit();
            }
        }
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_application_launch_probe_desc);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(PreferenceActivity activity)
    {
        PreferenceManager manager = activity.getPreferenceManager();

        PreferenceScreen screen = manager.createPreferenceScreen(activity);
        screen.setTitle(this.title(activity));
        screen.setSummary(R.string.summary_running_software_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(ApplicationLaunchProbe.ENABLED);
        enabled.setDefaultValue(ApplicationLaunchProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        ListPreference duration = new ListPreference(activity);
        duration.setKey("config_probe_application_launch_frequency");
        duration.setEntryValues(R.array.probe_app_launch_frequency_values);
        duration.setEntries(R.array.probe_app_launch_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);
        duration.setDefaultValue(ApplicationLaunchProbe.DEFAULT_FREQUENCY);

        screen.addPreference(duration);

        return screen;
    }
}
