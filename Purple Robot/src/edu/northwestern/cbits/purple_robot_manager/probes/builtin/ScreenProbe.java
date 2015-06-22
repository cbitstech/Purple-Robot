package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class ScreenProbe extends Probe
{
    public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.ScreenProbe";

    public static final String SCREEN_ACTIVE = "SCREEN_ACTIVE";

    private static final boolean DEFAULT_ENABLED = true;
    private static final String ENABLED = "config_probe_screen_enabled";

    private boolean _isInited = false;
    private boolean _isEnabled = false;

    private BroadcastReceiver _receiver = null;

    @Override
    public String name(Context context)
    {
        return ScreenProbe.NAME;
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_screen_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_device_info_category);
    }

    @Override
    public boolean isEnabled(Context context)
    {
        if (!this._isInited)
        {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);

            final ScreenProbe me = this;

            this._receiver = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    if (me._isEnabled)
                    {
                        Bundle bundle = new Bundle();
                        bundle.putString("PROBE", me.name(context));
                        bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                        final String action = intent.getAction();

                        if (Intent.ACTION_SCREEN_OFF.equals(action))
                            bundle.putBoolean(ScreenProbe.SCREEN_ACTIVE, false);
                        else if (Intent.ACTION_SCREEN_ON.equals(action))
                            bundle.putBoolean(ScreenProbe.SCREEN_ACTIVE, true);

                        me.transmitData(context, bundle);
                    }
                }
            };

            context.registerReceiver(this._receiver, filter);

            this._isInited = true;
        }

        SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean("config_probe_screen_enabled", ScreenProbe.DEFAULT_ENABLED))
                this._isEnabled = true;
            else
                this._isEnabled = false;
        }
        else
            this._isEnabled = false;

        return this._isEnabled;
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(ScreenProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(ScreenProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        boolean active = bundle.getBoolean(ScreenProbe.SCREEN_ACTIVE, false);

        if (active)
            return context.getResources().getString(R.string.summary_screen_probe_active);

        return context.getResources().getString(R.string.summary_screen_probe_inactive);
    }

    @Override
    public Bundle formattedBundle(Context context, Bundle bundle)
    {
        Bundle formatted = super.formattedBundle(context, bundle);

        boolean active = bundle.getBoolean(ScreenProbe.SCREEN_ACTIVE, false);

        if (active)
            formatted.putString(context.getString(R.string.display_screen_label), context.getString(R.string.display_screen_active_label));
        else
            formatted.putString(context.getString(R.string.display_screen_label), context.getString(R.string.display_screen_inactive_label));

        return formatted;
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_screen_probe_desc);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_screen_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(ScreenProbe.ENABLED);
        enabled.setDefaultValue(ScreenProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

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
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    public String assetPath(Context context)
    {
        return "screen-probe.html";
    }
}
