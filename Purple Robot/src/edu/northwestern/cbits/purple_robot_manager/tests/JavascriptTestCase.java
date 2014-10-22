package edu.northwestern.cbits.purple_robot_manager.tests;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.hardware.SensorManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.AccelerometerProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ContinuousProbe;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;

public class JavascriptTestCase extends RobotTestCase {
    public JavascriptTestCase(Context context, int priority) {
        super(context, priority);
    }

    public void test() {
        if (this.isSelected(this._context) == false)
            return;

        this.broadcastUpdate("Testing PurpleRobot.updateProbe...");

        try {
            JSONObject probeSettings = new JSONObject();
            probeSettings.put(Probe.PROBE_NAME, AccelerometerProbe.NAME);
            probeSettings.put(Probe.PROBE_ENABLED, true);
            probeSettings.put(Probe.PROBE_FREQUENCY,
                    SensorManager.SENSOR_DELAY_FASTEST);
            probeSettings.put(ContinuousProbe.PROBE_THRESHOLD, 0.0);

            Object returned = BaseScriptEngine.runScript(this._context,
                    "PurpleRobot.updateProbe("
                            + probeSettings.toString().replace("\"", "'")
                            + ");");

            Thread.sleep(2000);

            Assert.assertEquals("JST0", returned.getClass(), Boolean.class);
            Assert.assertTrue("JST1", ((Boolean) returned).booleanValue());

            AccelerometerProbe probe = (AccelerometerProbe) ProbeManager
                    .probeForName(probeSettings.getString(Probe.PROBE_NAME),
                            this._context);

            Assert.assertNotNull("JST2", probe);

            Assert.assertEquals("JST3", probe.isEnabled(this._context),
                    probeSettings.getBoolean(Probe.PROBE_ENABLED));
            Assert.assertEquals("JST4", probe.getFrequency(),
                    probeSettings.getInt(Probe.PROBE_FREQUENCY));
            Assert.assertEquals("JST5", probe.getThreshold(),
                    probeSettings.getDouble(ContinuousProbe.PROBE_THRESHOLD));

            probeSettings.put(Probe.PROBE_ENABLED, false);
            probeSettings.put(Probe.PROBE_FREQUENCY,
                    SensorManager.SENSOR_DELAY_NORMAL);
            probeSettings.put(ContinuousProbe.PROBE_THRESHOLD, 1.0);

            returned = BaseScriptEngine.runScript(this._context,
                    "PurpleRobot.updateProbe("
                            + probeSettings.toString().replace("\"", "'")
                            + ");");

            Thread.sleep(2000);

            Assert.assertEquals("JST6", returned.getClass(), Boolean.class);
            Assert.assertTrue("JST7", ((Boolean) returned).booleanValue());

            Assert.assertEquals("JST8", probe.isEnabled(this._context),
                    probeSettings.getBoolean(Probe.PROBE_ENABLED));
            Assert.assertEquals("JST9", probe.getFrequency(),
                    probeSettings.getInt(Probe.PROBE_FREQUENCY));
            Assert.assertEquals("JST10", probe.getThreshold(),
                    probeSettings.getDouble(ContinuousProbe.PROBE_THRESHOLD));
        } catch (JSONException e) {
            e.printStackTrace();

            Assert.fail("JST1000");
        } catch (InterruptedException e) {
            Assert.fail("JST1001");
        }
    }

    public int estimatedMinutes() {
        return 1;
    }

    public String name(Context context) {
        return context.getString(R.string.name_javascript_test);
    }
}
