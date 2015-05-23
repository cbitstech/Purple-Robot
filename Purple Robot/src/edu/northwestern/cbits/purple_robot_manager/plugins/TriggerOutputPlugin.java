package edu.northwestern.cbits.purple_robot_manager.plugins;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.triggers.ProbeTrigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class TriggerOutputPlugin extends OutputPlugin
{
    public String[] respondsTo()
    {
        String[] activeActions = { Probe.PROBE_READING };

        return activeActions;
    }

    public void processIntent(Intent intent)
    {
        Context context = this.getContext();

        synchronized (this)
        {
            List<Trigger> triggers = new ArrayList<Trigger>();
            triggers.addAll(TriggerManager.getInstance(context).allTriggers());

            for (Trigger trigger : triggers)
            {
                if (trigger instanceof ProbeTrigger)
                {
                    ProbeTrigger probeTrigger = (ProbeTrigger) trigger;

                    Log.e("PR", "GOT " + probeTrigger.identifier());

                    try
                    {
                        Log.e("PR", intent.getExtras().getString("PROBE") + " MATCHES: " + probeTrigger.matchesProbe(intent.getExtras().getString("PROBE")));

                        if (probeTrigger.matchesProbe(intent.getExtras().getString("PROBE")))
                        {
                            Log.e("PR", "JSON BUNDLE: " + OutputPlugin.jsonForBundle(intent.getExtras()).toString(2));

                            if (probeTrigger.matches(context, OutputPlugin.jsonForBundle(intent.getExtras())))
                                trigger.execute(context, false);
                        }
                    }
                    catch (JSONException e)
                    {
                        LogManager.getInstance(context).logException(e);
                    }
                }
            }
        }
    }
}
