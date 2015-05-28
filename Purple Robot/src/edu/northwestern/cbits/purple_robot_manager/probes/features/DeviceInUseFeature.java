package edu.northwestern.cbits.purple_robot_manager.probes.features;

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
import android.support.v4.content.LocalBroadcastManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.CallStateProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ScreenProbe;

public class DeviceInUseFeature extends Feature
{
    protected static final String DEVICE_ACTIVE = "DEVICE_ACTIVE";
    private boolean _isInited = false;
    private boolean _isEnabled = true;

    private boolean _lastXmit = false;

    private boolean _callActive = false;
    private boolean _screenActive = false;

    @Override
    protected String featureKey()
    {
        return "device_use";
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.features.DeviceInUseFeature";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_device_use_feature);
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_device_use_feature_desc);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_device_info_category);
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        boolean inUse = bundle.getBoolean(DeviceInUseFeature.DEVICE_ACTIVE);

        if (inUse)
            return context.getResources().getString(R.string.summary_device_active);
        else
            return context.getResources().getString(R.string.summary_device_inactive);
    }

    @Override
    public boolean isEnabled(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        this._isEnabled = false;

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean("config_probe_device_use_enabled", true))
                this._isEnabled = true;
        }

        if (!this._isInited)
        {
            IntentFilter intentFilter = new IntentFilter(Probe.PROBE_READING);

            final DeviceInUseFeature me = this;

            BroadcastReceiver receiver = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    if (!me._isEnabled)
                        return;

                    Bundle extras = intent.getExtras();

                    String probeName = extras.getString("PROBE");

                    if (probeName != null && (ScreenProbe.NAME.equals(probeName) || CallStateProbe.NAME.equals(probeName)))
                    {
                        boolean xmit = false;

                        if (ScreenProbe.NAME.equals(probeName))
                            me._screenActive = extras.getBoolean(ScreenProbe.SCREEN_ACTIVE);
                        else if (CallStateProbe.NAME.equals(probeName))
                        {
                            String state = extras.getString(CallStateProbe.CALL_STATE);

                            me._callActive = CallStateProbe.STATE_OFF_HOOK.equals(state);
                        }

                        xmit = me._callActive || me._screenActive;

                        if (me._lastXmit != xmit)
                        {
                            if (me._isEnabled)
                            {
                                Bundle bundle = new Bundle();
                                bundle.putString("PROBE", me.name(context));
                                bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                                bundle.putBoolean(DeviceInUseFeature.DEVICE_ACTIVE, xmit);

                                me.transmitData(context, bundle);
                            }

                            me._lastXmit = xmit;
                        }
                    }
                }
            };

            LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
            localManager.registerReceiver(receiver, intentFilter);

            this._isInited = true;
        }

        return this._isEnabled;
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean("config_probe_device_use_enabled", true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean("config_probe_device_use_enabled", false);

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
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }
}
