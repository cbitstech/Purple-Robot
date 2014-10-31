package edu.northwestern.cbits.purple_robot_manager.tests;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.triggers.DateTrigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class HalfHourDateTriggerTestCase extends RobotTestCase
{
    public HalfHourDateTriggerTestCase(Context context, int priority)
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

        Assert.assertEquals("HHDT0", 0, triggers.allTriggers().size());

        String triggerId = "half-hour-date-test";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

        long now = System.currentTimeMillis();

        try
        {
            JSONObject triggerDef = new JSONObject();
            triggerDef.put("type", "datetime");

            triggerDef.put(Trigger.NAME, "Half Hour Date Test");
            triggerDef.put(Trigger.IDENTIFIER, triggerId);
            triggerDef
                    .put(Trigger.ACTION,
                            "PurpleRobot.playDefaultTone(); PurpleRobot.testLog('Date Test: Half Hour Fired!'); PurpleRobot.vibrate('SOS');");
            triggerDef.put(DateTrigger.DATETIME_START, sdf.format(new Date(now + (30 * 60 * 1000))));

            String script = "PurpleRobot.updateTrigger('" + triggerId + "', "
                    + triggerDef.toString().replace("'", "\\'").replace("\"", "'") + ");";

            BaseScriptEngine.runScript(this._context, script);

            this.broadcastUpdate("Created half-hour trigger.");

            Thread.sleep(2000);
        }
        catch (JSONException e)
        {
            Assert.fail("HHDT1");
        }
        catch (InterruptedException e)
        {
            Assert.fail("HHDT2");
        }

        this.broadcastUpdate("Verifying half-hour trigger exists...");

        List<Trigger> allTriggers = triggers.allTriggers();
        Assert.assertEquals("HHDT3", 1, allTriggers.size());

        Trigger trigger = allTriggers.get(0);
        Assert.assertEquals("HHDT4", triggerId, trigger.identifier());

        Assert.assertEquals("HHDT5", trigger.getClass(), DateTrigger.class);

        DateTrigger dateTrigger = (DateTrigger) trigger;

        Assert.assertTrue("HHDT6", dateTrigger.matches(this._context, new Date(now + (30 * 60 * 1000) + 15000)));

        List<Long> upcomingTimes = triggers.upcomingFireTimes(this._context);

        Assert.assertTrue("HHDT7", upcomingTimes.size() > 0);

        try
        {
            long wait = (31 * 60 * 1000);

            while (wait > 0)
            {
                this.broadcastUpdate("Sleeping. " + (wait / (60 * 1000)) + " minutes remaining...", 0);
                Thread.sleep(60 * 1000);

                wait = wait - 60 * 1000;
            }
        }
        catch (InterruptedException e)
        {
            Assert.fail();
        }

        this.broadcastUpdate("Verifying that half-hour trigger fired...");

        Assert.assertTrue("HHDT8", dateTrigger.lastFireTime(this._context) > 0);
        Assert.assertTrue("HHDT9", dateTrigger.lastFireTime(this._context) < System.currentTimeMillis());

        this.broadcastUpdate("Clearing triggers...");

        triggers.removeAllTriggers();
    }

    public int estimatedMinutes()
    {
        return 33;
    }

    public String name(Context context)
    {
        return context.getString(R.string.name_half_hour_date_trigger_test);
    }
}
