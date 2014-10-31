package edu.northwestern.cbits.purple_robot_manager.tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Assert;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.features.Feature;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;

public class SnapshotsTestCase extends RobotTestCase
{
    public SnapshotsTestCase(Context context, int priority)
    {
        super(context, priority);
    }

    @SuppressWarnings(
    { "unchecked", "rawtypes" })
    public void test()
    {
        if (this.isSelected(this._context) == false)
            return;

        long now = System.currentTimeMillis();

        BaseScriptEngine.runScript(this._context, "PurpleRobot.emitReading('SnapshotTest', '" + now + "');");

        try
        {
            Thread.sleep(5000);
        }
        catch (InterruptedException e)
        {

        }

        NativeJavaObject snapId = (NativeJavaObject) BaseScriptEngine.runScript(this._context,
                "PurpleRobot.takeSnapshot('Snapshot Test');");

        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {

        }

        NativeArray value = (NativeArray) BaseScriptEngine.runScript(this._context, "PurpleRobot.fetchSnapshotIds();");

        Assert.assertNotNull("SNAP001", value);

        Assert.assertTrue("SNAP002", value.getLength() > 0);

        Iterator iter = value.iterator();

        long testId = 0;

        while (iter.hasNext())
        {
            String id = iter.next().toString();

            long idTimestamp = Long.parseLong(id);

            if (idTimestamp > now)
                testId = idTimestamp;
        }

        Assert.assertTrue("SNAP003", testId > now);

        Assert.assertTrue("SNAP004", snapId.unwrap().toString().equals("" + testId));

        NativeObject snapshot = (NativeObject) BaseScriptEngine.runScript(this._context, "PurpleRobot.fetchSnapshot('"
                + testId + "');");

        Assert.assertNotNull("SNAP005", snapshot);

        Map<String, Object> snapValue = JavaScriptEngine.nativeToMap(snapshot);

        Assert.assertNotNull("SNAP006", snapValue);

        ArrayList<Map<String, Object>> valuesList = (ArrayList<Map<String, Object>>) snapValue.get("values");

        Object savedValue = null;

        for (Map<String, Object> values : valuesList)
        {
            if (values.get("probe").equals("SnapshotTest"))
            {
                Map<String, Object> probeValue = (Map<String, Object>) values.get("value");

                savedValue = probeValue.get(Feature.FEATURE_VALUE);
            }
        }

        Assert.assertNotNull("SNAP007", savedValue);
        Assert.assertEquals("SNAP008", savedValue.toString(), "" + now);

        BaseScriptEngine.runScript(this._context, "PurpleRobot.deleteSnapshot('" + testId + "');");

        snapshot = (NativeObject) BaseScriptEngine.runScript(this._context, "PurpleRobot.fetchSnapshot('" + testId
                + "');");

        Assert.assertTrue("SNAP009", snapshot == null);
    }

    public int estimatedMinutes()
    {
        return 1;
    }

    public String name(Context context)
    {
        return context.getString(R.string.name_snapshot_test);
    }
}
