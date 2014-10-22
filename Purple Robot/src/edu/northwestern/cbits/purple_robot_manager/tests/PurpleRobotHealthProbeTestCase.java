package edu.northwestern.cbits.purple_robot_manager.tests;

import junit.framework.Assert;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.RobotHealthProbe;

public class PurpleRobotHealthProbeTestCase extends RobotTestCase {
    public PurpleRobotHealthProbeTestCase(Context context, int priority) {
        super(context, priority);
    }

    public void test() {
        if (this.isSelected(this._context) == false)
            return;

        final ContentValues values = new ContentValues();
        values.put("count", 0L);
        values.put("start", Long.MAX_VALUE);
        values.put("end", 0L);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String name = intent.getStringExtra("PROBE");

                if (RobotHealthProbe.NAME.equals(name)) {
                    long count = values.getAsLong("count");
                    double start = values.getAsDouble("start");
                    double end = values.getAsDouble("end");

                    long time = intent.getLongExtra("TIMESTAMP", 0);

                    if (time < start)
                        start = time;

                    if (time > end)
                        end = time;

                    values.put("count", count + 1);
                    values.put("start", start);
                    values.put("end", end);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Probe.PROBE_READING);

        LocalBroadcastManager localManager = LocalBroadcastManager
                .getInstance(this._context);
        localManager.registerReceiver(receiver, intentFilter);

        try {
            this.broadcastUpdate("Enabling probe...");

            for (Probe probe : ProbeManager.allProbes(this._context)) {
                if (probe instanceof RobotHealthProbe) {
                    SharedPreferences prefs = PreferenceManager
                            .getDefaultSharedPreferences(this._context);
                    Editor e = prefs.edit();
                    e.putString("config_probe_robot_frequency", "60000");
                    e.commit();

                    probe.enable(this._context);
                }
            }

            Thread.sleep(2000);

            for (Probe probe : ProbeManager.allProbes(this._context)) {
                if (probe instanceof RobotHealthProbe) {
                    Assert.assertTrue("ATP0", probe.isEnabled(this._context));
                }
            }

            this.broadcastUpdate("Sleeping. 300 seconds remaining...", 0);
            Thread.sleep(30000);
            this.broadcastUpdate("Sleeping. 270 seconds remaining...", 0);
            Thread.sleep(30000);
            this.broadcastUpdate("Sleeping. 240 seconds remaining...", 0);
            Thread.sleep(30000);
            this.broadcastUpdate("Sleeping. 210 seconds remaining...", 0);
            Thread.sleep(30000);
            this.broadcastUpdate("Sleeping. 180 seconds remaining...", 0);
            Thread.sleep(30000);
            this.broadcastUpdate("Sleeping. 150 seconds remaining...", 0);
            Thread.sleep(30000);
            this.broadcastUpdate("Sleeping. 120 seconds remaining...", 0);
            Thread.sleep(30000);
            this.broadcastUpdate("Sleeping. 90 seconds remaining...", 0);
            Thread.sleep(30000);
            this.broadcastUpdate("Sleeping. 60 seconds remaining...", 0);
            Thread.sleep(30000);
            this.broadcastUpdate("Sleeping. 30 seconds remaining...", 0);
            Thread.sleep(30000);

            this.broadcastUpdate("Halting data collection...", 0);
            localManager.unregisterReceiver(receiver);
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Assert.fail("PRHP1");
        }

        long count = values.getAsLong("count");
        double start = values.getAsDouble("start");
        double end = values.getAsDouble("end");

        Assert.assertTrue("PRHP2", count > 4);
        Assert.assertTrue("PRHP3", start > 0);
        Assert.assertTrue("PRHP3", end < System.currentTimeMillis());
        Assert.assertTrue("PRHP4", (end - start) > 240);

        for (Probe probe : ProbeManager.allProbes(this._context)) {
            if (probe instanceof RobotHealthProbe) {
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(this._context);
                Editor e = prefs.edit();
                e.putString("config_probe_robot_frequency", "300000");
                e.commit();
            }
        }
    }

    public int estimatedMinutes() {
        return 1;
    }

    public String name(Context context) {
        return context.getString(R.string.name_health_probe_test);
    }
}
