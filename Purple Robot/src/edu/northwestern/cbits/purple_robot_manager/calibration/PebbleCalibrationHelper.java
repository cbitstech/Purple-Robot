package edu.northwestern.cbits.purple_robot_manager.calibration;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceManager;

import com.getpebble.android.kit.PebbleKit;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.probes.devices.PebbleProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.studies.LivewellPebbleActivityCountsProbe;

public class PebbleCalibrationHelper
{
    private static final String PEBBLE_PACKAGE = "com.getpebble.android";

    public static void check(final Context context, boolean isEnabled)
    {
        final SanityManager sanity = SanityManager.getInstance(context);
        final String title = context.getString(R.string.title_pebble_check);
        final String connectedTitle = context.getString(R.string.title_pebble_connected_check);

        sanity.clearAlert(title);
        sanity.clearAlert(connectedTitle);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean pebbleEnabled = prefs.getBoolean(PebbleProbe.ENABLED, PebbleProbe.DEFAULT_ENABLED);
        boolean livewellEnabled = prefs.getBoolean(LivewellPebbleActivityCountsProbe.ENABLED, LivewellPebbleActivityCountsProbe.DEFAULT_ENABLED);

        if (pebbleEnabled || livewellEnabled)
        {
            try
            {
                context.getPackageManager().getPackageInfo(PebbleCalibrationHelper.PEBBLE_PACKAGE, 0);

                if (PebbleKit.isWatchConnected(context) == false)
                {
                    String message = context.getString(R.string.message_pebble_connected_check);

                    Runnable action = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Intent intent = context.getPackageManager().getLaunchIntentForPackage(PebbleCalibrationHelper.PEBBLE_PACKAGE);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            context.startActivity(intent);
                        }
                    };

                    sanity.addAlert(SanityCheck.WARNING, connectedTitle, message, action);
                }
            }
            catch (NameNotFoundException e)
            {
                String message = context.getString(R.string.message_pebble_check);

                Runnable action = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + PebbleCalibrationHelper.PEBBLE_PACKAGE));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        context.startActivity(intent);
                    }
                };

                sanity.addAlert(SanityCheck.WARNING, title, message, action);
            }
        }
    }
}
