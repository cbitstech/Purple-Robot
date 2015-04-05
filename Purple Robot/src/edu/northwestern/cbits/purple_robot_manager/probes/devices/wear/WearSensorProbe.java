package edu.northwestern.cbits.purple_robot_manager.probes.devices.wear;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import org.json.JSONObject;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.devices.AndroidWearProbe;

public abstract class WearSensorProbe extends Probe
{
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_other_devices_category);
    }

    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        return null;
    }

    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean(this.getPreferenceKey(), true);

        e.commit();
    }

    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean(this.getPreferenceKey(), false);

        e.commit();
    }

    public boolean isEnabled(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        boolean enabled = prefs.getBoolean(AndroidWearProbe.ENABLED, AndroidWearProbe.DEFAULT_ENABLED);

        if (enabled)
            return prefs.getBoolean(this.getPreferenceKey(), false);

        return enabled;
    }

    protected abstract String getPreferenceKey();

    @Override
    public JSONObject fetchSettings(Context context)
    {
        return new JSONObject();
    }
}
