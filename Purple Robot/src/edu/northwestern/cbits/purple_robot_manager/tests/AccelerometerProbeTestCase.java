package edu.northwestern.cbits.purple_robot_manager.tests;

import junit.framework.Assert;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.AccelerometerProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ContinuousProbe;

public class AccelerometerProbeTestCase extends RobotTestCase
{
    public AccelerometerProbeTestCase(Context context, int priority)
    {
        super(context, priority);
    }

    @Override
    public void test()
    {
        if (this.isSelected(this._context) == false)
            return;

        final ContentValues values = new ContentValues();
        values.put("count", 0L);
        values.put("start", Double.MAX_VALUE);
        values.put("end", 0D);

        BroadcastReceiver receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                String name = intent.getStringExtra("PROBE");

                if (AccelerometerProbe.NAME.equals(name))
                {
                    double[] times = intent.getDoubleArrayExtra(ContinuousProbe.EVENT_TIMESTAMP);

                    long count = values.getAsLong("count");
                    double start = values.getAsDouble("start");
                    double end = values.getAsDouble("end");

                    for (double time : times)
                    {
                        if (time < start)
                            start = time;

                        if (time > end)
                            end = time;
                    }

                    values.put("count", count + times.length);
                    values.put("start", start);
                    values.put("end", end);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Probe.PROBE_READING);

        LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this._context);
        localManager.registerReceiver(receiver, intentFilter);

        try
        {
            this.broadcastUpdate("Enabling probe...");

            for (Probe probe : ProbeManager.allProbes(this._context))
            {
                if (probe instanceof AccelerometerProbe)
                {
                    probe.enable(this._context);
                }
            }

            Thread.sleep(2000);

            for (Probe probe : ProbeManager.allProbes(this._context))
            {
                if (probe instanceof AccelerometerProbe)
                {
                    Assert.assertTrue("ATP0", probe.isEnabled(this._context));

                    AccelerometerProbe accel = (AccelerometerProbe) probe;

                    accel.setThreshold(0.0);
                }
            }

            this.broadcastUpdate("Sleeping. 60 seconds remaining...", 0);
            Thread.sleep(15000);
            this.broadcastUpdate("Sleeping. 45 seconds remaining...", 0);
            Thread.sleep(15000);
            this.broadcastUpdate("Sleeping. 30 seconds remaining...", 0);
            Thread.sleep(15000);
            this.broadcastUpdate("Sleeping. 15 seconds remaining...", 0);
            Thread.sleep(15000);

            this.broadcastUpdate("Halting data collection...", 0);
            localManager.unregisterReceiver(receiver);
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
            Assert.fail("ATP1");
        }

        long count = values.getAsLong("count");
        double start = values.getAsDouble("start");
        double end = values.getAsDouble("end");

        Assert.assertTrue("ATP2", count > 60);
        Assert.assertTrue("ATP3", start > 0);
        Assert.assertTrue("ATP3", end < System.currentTimeMillis());
        Assert.assertTrue("ATP4", (end - start) > 45);

        for (Probe probe : ProbeManager.allProbes(this._context))
        {
            if (probe instanceof AccelerometerProbe)
            {
                probe.disable(this._context);
            }
        }
    }

    @Override
    public int estimatedMinutes()
    {
        return 1;
    }

    @Override
    public String name(Context context)
    {
        return context.getString(R.string.name_accelerometer_probe_test);
    }
}
