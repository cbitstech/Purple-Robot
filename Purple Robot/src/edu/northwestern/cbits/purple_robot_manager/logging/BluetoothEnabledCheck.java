package edu.northwestern.cbits.purple_robot_manager.logging;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.BluetoothDevicesProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.BluetoothReceiverProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.devices.AndroidWearProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.devices.PebbleProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.services.FitbitBetaProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.services.FitbitProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.services.JawboneProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.services.iHealthProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.studies.LivewellPebbleActivityCountsProbe;

public class BluetoothEnabledCheck extends SanityCheck
{
    private static String[] _probeNames = {
            BluetoothDevicesProbe.PROBE_NAME,
//            BluetoothReceiverProbe.PROBE_NAME,
            AndroidWearProbe.PROBE_NAME,
            PebbleProbe.PROBE_NAME,
            FitbitProbe.PROBE_NAME,
            FitbitBetaProbe.PROBE_NAME,
            iHealthProbe.PROBE_NAME,
            JawboneProbe.PROBE_NAME,
            LivewellPebbleActivityCountsProbe.PROBE_NAME,
    };

    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_location_services_enabled);
    }

    public Runnable getAction(final Context context)
    {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);
            }
        };

        return r;
    }

    public void runCheck(Context context)
    {
        boolean needsBluetooth = false;

        for (String name : BluetoothEnabledCheck._probeNames)
        {
            Probe probe = ProbeManager.probeForName(name, context);

            if (probe != null) {
                if (probe.isEnabled(context))
                    needsBluetooth = true;
            }
            else
                Log.e("PR", "NULL PROBE: " + name);
        }

        this._errorLevel = SanityCheck.OK;

        if (needsBluetooth)
        {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

            if (adapter == null) {
                this._errorLevel = SanityCheck.WARNING;
                this._errorMessage = context.getString(R.string.name_sanity_bluetooth_missing_error);
            } else if (adapter.isEnabled() == false) {
                this._errorLevel = SanityCheck.WARNING;
                this._errorMessage = context.getString(R.string.name_sanity_bluetooth_disabled_error);
            }
        }
    }
}
