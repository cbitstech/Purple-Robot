package edu.northwestern.cbits.purple_robot_manager.logging;

import android.content.Context;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.probes.devices.PebbleProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.studies.LivewellPebbleActivityCountsProbe;

public class PebbleProbesCheck extends SanityCheck
{
    public void runCheck(Context context)
    {
        PebbleProbe pebble = (PebbleProbe) ProbeManager.probeForName("edu.northwestern.cbits.purple_robot_manager.probes.devices.PebbleProbe", context);
        LivewellPebbleActivityCountsProbe livewell = (LivewellPebbleActivityCountsProbe) ProbeManager.probeForName("edu.northwestern.cbits.purple_robot_manager.probes.studies.LivewellPebbleActivityCountsProbe", context);

        this._errorMessage = null;
        this._errorLevel = SanityCheck.OK;

        if (pebble != null && livewell != null && pebble.isEnabled(context) && livewell.isEnabled(context))
        {
            this._errorLevel = SanityCheck.WARNING;
            this._errorMessage = context.getString(R.string.name_sanity_pebble_probes_warning);
        }
    }

    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_pebble_probes_title);
    }
}
