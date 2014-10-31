package edu.northwestern.cbits.purple_robot_manager.plugins;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;
import edu.northwestern.cbits.purple_robot_manager.activities.StartActivity;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class DiagnosticAppDisplayPlugin extends OutputPlugin
{
    public String[] respondsTo()
    {
        String[] activeActions =
        { Probe.PROBE_READING, OutputPlugin.LOG_EVENT, DiagnosticAppDisplayPlugin.DISPLAY_MESSAGE };

        return activeActions;
    }

    public void processIntent(Intent intent)
    {
        String message = "WIFI NOT ENABLED";

        WifiManager wifi = (WifiManager) this.getContext().getSystemService(Context.WIFI_SERVICE);

        if (wifi.isWifiEnabled())
        {
            ConnectivityManager connection = (ConnectivityManager) this.getContext().getSystemService(
                    Context.CONNECTIVITY_SERVICE);

            NetworkInfo netInfo = connection.getActiveNetworkInfo();

            if (netInfo != null)
                message = netInfo.getType() + " -- " + netInfo.getState();
            else
                message = "NULL NETINFO";
        }

        Intent displayIntent = new Intent(StartActivity.UPDATE_MESSAGE);
        displayIntent.putExtra(StartActivity.DISPLAY_MESSAGE, message);

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this.getContext());
        manager.sendBroadcast(displayIntent);
    }
}
