package edu.northwestern.cbits.purple_robot_manager.probes.features.p20;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class ExternalAppHelperProbe extends Probe
{
    private static final String NEW_DATA = "edu.northwestern.sohrob.activityrecognition.activityrecognition.NEW_DATA";
    private static final String DATA = "DATA";

    private static final boolean DEFAULT_ENABLED = false;
    private static final String ENABLED = "config_p20_external_app_enabled";

    private BroadcastReceiver _receiver = null;

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.features.p20.ExternalAppHelperProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_p20_external_app_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_misc_category);
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        if (super.isEnabled(context))
        {
            SharedPreferences prefs = Probe.getPreferences(context);

            if (prefs.getBoolean(ExternalAppHelperProbe.ENABLED, ExternalAppHelperProbe.DEFAULT_ENABLED))
            {
                if (this._receiver == null)
                {
                    final ExternalAppHelperProbe me = this;

                    this._receiver = new BroadcastReceiver()
                    {
                        @Override
                        public void onReceive(Context context, Intent intent)
                        {
                            Bundle bundle = intent.getBundleExtra(ExternalAppHelperProbe.DATA);

                            bundle.putString("PROBE", me.name(context));
                            bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                            Log.e("PR", "EXT RECV: " + bundle);

                            me.transmitData(context, bundle);
                        }
                    };

                    IntentFilter filter = new IntentFilter();
                    filter.addAction(ExternalAppHelperProbe.NEW_DATA);

                    context.registerReceiver(this._receiver, filter);
                }

                return true;
            }
        }

        if (this._receiver != null)
        {
            try
            {
                context.unregisterReceiver(this._receiver);
            }
            catch (IllegalArgumentException e)
            {
                LogManager.getInstance(context).logException(e);
            }

            this._receiver = null;
        }

        return false;
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(ExternalAppHelperProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(ExternalAppHelperProbe.ENABLED, false);

        e.commit();
    }

    /*
     * @Override public String summarizeValue(Context context, Bundle bundle) {
     * int lastSpeech = (int) bundle.getDouble("LAST_SPEECH_READING");
     * 
     * int speechResource = R.string.label_speech_none;
     * 
     * if (lastSpeech == 0) speechResource = R.string.label_speech_human;
     * 
     * int lastActivity = (int) bundle.getDouble("LAST_ACTIVITY");
     * 
     * int activityResource = R.string.label_activity_unknown;
     * 
     * switch (lastActivity) { case 1: activityResource =
     * R.string.label_activity_stationary; break; case 2: activityResource =
     * R.string.label_activity_walking; break; case 3: activityResource =
     * R.string.label_activity_running; break; }
     * 
     * return context.getString(R.string.summary_saint_probe,
     * context.getString(activityResource), context.getString(speechResource));
     * }
     */

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(PreferenceActivity activity)
    {
        PreferenceManager manager = activity.getPreferenceManager();

        PreferenceScreen screen = manager.createPreferenceScreen(activity);
        screen.setTitle(this.title(activity));
        screen.setSummary(R.string.summary_p20_external_app_probe);

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(ExternalAppHelperProbe.ENABLED);
        enabled.setDefaultValue(ExternalAppHelperProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        return screen;
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_p20_external_app_probe);
    }
}
