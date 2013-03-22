package edu.northwestern.cbits.purple_robot_manager.plugins;

import java.util.List;

import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import edu.northwestern.cbits.purple_robot_manager.config.LegacyJSONConfigFile;
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

		LegacyJSONConfigFile jsonConfig = LegacyJSONConfigFile.getSharedFile(context);

		if (jsonConfig == null)
			return;

		synchronized(this)
		{
			List<Trigger> triggers = TriggerManager.getInstance(context).allTriggers();
	
			for (Trigger trigger : triggers)
			{
				if (trigger instanceof ProbeTrigger)
				{
					ProbeTrigger probeTrigger = (ProbeTrigger) trigger;
	
					try
					{
						if (probeTrigger.matches(context, OutputPlugin.jsonForBundle(intent.getExtras())))
							trigger.execute(context);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
	}
}
