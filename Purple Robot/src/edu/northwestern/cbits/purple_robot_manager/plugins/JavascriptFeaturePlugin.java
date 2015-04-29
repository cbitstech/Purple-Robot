package edu.northwestern.cbits.purple_robot_manager.plugins;

import org.json.JSONException;

import android.content.Intent;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.probes.features.JavascriptFeature;

public class JavascriptFeaturePlugin extends OutputPlugin
{
    public String[] respondsTo()
    {
        String[] activeActions = { Probe.PROBE_READING };

        return activeActions;
    }

    public void processIntent(Intent intent)
    {
        Bundle extras = intent.getExtras();

        if (Probe.PROBE_READING.equals(intent.getAction()))
        {
            for (Probe probe : ProbeManager.allProbes(this.getContext()))
            {
                if (probe instanceof JavascriptFeature)
                {
                    JavascriptFeature feature = (JavascriptFeature) probe;

                    if (feature.isEnabled(this.getContext()))
                    {
                        try
                        {
                            feature.processData(this.getContext(), OutputPlugin.jsonForBundle(extras));
                        }
                        catch (JSONException e)
                        {
                            LogManager.getInstance(this.getContext()).logException(e);
                        }
                    }
                }
            }
        }
    }
}
