package edu.northwestern.cbits.purple_robot_manager.tests;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.NativeJavaObject;

import android.content.Context;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.triggers.DateTrigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class RepeatingTriggerTestCase extends RobotTestCase
{
    public RepeatingTriggerTestCase(Context context, int priority)
    {
        super(context, priority);
    }

    public void test()
    {
        if (this.isSelected(this._context) == false)
            return;

        TriggerManager triggers = TriggerManager.getInstance(this._context);

        this.broadcastUpdate("Clearing triggers...");

        triggers.removeAllTriggers();
        Assert.assertEquals("RDT0", 0, triggers.allTriggers().size());

        // TODO: Tests for these script calls elsewhere...
        BaseScriptEngine.runScript(this._context, "PurpleRobot.persistString('repeat-test-token', '');");
        NativeJavaObject persisted = (NativeJavaObject) BaseScriptEngine.runScript(this._context,
                "PurpleRobot.fetchString('repeat-test-token');");

        Assert.assertEquals("RDT1", "", persisted.unwrap()); // TODO: Unwrap at
                                                             // scripting
                                                             // engine?

        String triggerId = "repeat-date-test";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

        long now = System.currentTimeMillis() + 60000;

        try
        {
            JSONObject triggerDef = new JSONObject();
            triggerDef.put("type", "datetime");

            triggerDef.put(Trigger.NAME, "Repeating Date Test");
            triggerDef.put(Trigger.IDENTIFIER, triggerId);
            triggerDef
                    .put(Trigger.ACTION,
                            "PurpleRobot.playDefaultTone(); PurpleRobot.testLog('Date Test: Repeat Fired!'); PurpleRobot.persistString('repeat-test-token', PurpleRobot.fetchString('repeat-test-token') + 'a'); PurpleRobot.vibrate('SOS');");
            triggerDef.put(DateTrigger.DATETIME_START, sdf.format(new Date(now)));
            triggerDef.put(DateTrigger.DATETIME_REPEATS, "FREQ=MINUTELY;INTERVAL=2");

            String script = "PurpleRobot.updateTrigger('" + triggerId + "', "
                    + triggerDef.toString().replace("'", "\\'").replace("\"", "'") + ");";

            BaseScriptEngine.runScript(this._context, script);

            this.broadcastUpdate("Created repeating trigger.");

            Thread.sleep(2000);
        }
        catch (JSONException e)
        {
            Assert.fail("RDT2");
        }
        catch (InterruptedException e)
        {
            Assert.fail("RDT3");
        }

        this.broadcastUpdate("Verifying repeating trigger exists...");

        List<Trigger> allTriggers = triggers.allTriggers();
        Assert.assertEquals("RDT4", 1, allTriggers.size());

        Trigger trigger = allTriggers.get(0);
        Assert.assertEquals("RDT5", triggerId, trigger.identifier());

        Assert.assertEquals("RDT6", trigger.getClass(), DateTrigger.class);

        DateTrigger dateTrigger = (DateTrigger) trigger;

        List<Long> upcomingTimes = triggers.upcomingFireTimes(this._context);

        Assert.assertTrue("RDT7", upcomingTimes.size() > 0);

        Assert.assertTrue("RDT8", dateTrigger.matches(this._context, new Date(now + (2 * 60 * 1000) + 15000)));

        try
        {
            long wait = (10 * 60 * 1000);

            while (wait > 0)
            {
                this.broadcastUpdate("Sleeping. " + (wait / (60 * 1000)) + " minutes remaining...", 0);
                Thread.sleep(60 * 1000);

                wait = wait - (60 * 1000);
            }
        }
        catch (InterruptedException e)
        {
            Assert.fail();
        }

        this.broadcastUpdate("Verifying that repeating trigger fired...");

        Assert.assertTrue("RDT9", dateTrigger.lastFireTime(this._context) > 0);
        Assert.assertTrue("RDTA", dateTrigger.lastFireTime(this._context) < System.currentTimeMillis());

        this.broadcastUpdate("Checking trigger results...");

        persisted = (NativeJavaObject) BaseScriptEngine.runScript(this._context,
                "PurpleRobot.fetchString('repeat-test-token');");
        Assert.assertEquals("RDTB", "aaaaa", persisted.unwrap());

        this.broadcastUpdate("Clearing triggers...");

        triggers.removeAllTriggers();
    }

    public int estimatedMinutes()
    {
        return 11;
    }

    public String name(Context context)
    {
        return context.getString(R.string.name_repeating_date_trigger_test);
    }
}
