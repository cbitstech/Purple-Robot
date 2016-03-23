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
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public abstract class ContinuousProbeFeature extends Feature
{
    @Override
    protected abstract String featureKey();

    @Override
    public abstract String summary(Context context);

    @Override
    public abstract String name(Context context);

    public abstract String source(Context context);

    @Override
    public abstract String title(Context context);

    protected abstract void processData(Context context, Bundle dataBundle);

    private BroadcastReceiver _receiver = null;

    @Override
    public boolean isEnabled(Context context)
    {
        if (super.isEnabled(context))
        {
            SharedPreferences prefs = Probe.getPreferences(context);

            if (prefs.getBoolean("config_feature_" + this.featureKey() + "_enabled", false))
            {
                if (this._receiver == null)
                {
                    IntentFilter intentFilter = new IntentFilter(Probe.PROBE_READING);

                    final ContinuousProbeFeature me = this;

                    this._receiver = new BroadcastReceiver()
                    {
                        @Override
                        public void onReceive(final Context context, Intent intent)
                        {
                            Bundle extras = intent.getExtras();

                            String probeName = extras.getString("PROBE");

                            if (probeName != null && (me.source(context).equals(probeName)))
                                me.processData(context, extras);
                        }
                    };

                    LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
                    localManager.registerReceiver(this._receiver, intentFilter);
                }

                return true;
            }
            else
                this.disable(context);
        }
        else
            this.disable(context);

        return false;
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean("config_feature_" + this.featureKey() + "_enabled", false);

        e.commit();

        if (this._receiver != null)
        {
            LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
            localManager.unregisterReceiver(this._receiver);

            this._receiver = null;
        }
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean("config_feature_" + this.featureKey() + "_enabled", true);

        e.commit();

        this.isEnabled(context);
    }
}
