package edu.northwestern.cbits.purple_robot_manager.tests;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.triggers.DateTrigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class DateTriggerTestCase extends RobotTestCase
{
    public DateTriggerTestCase(Context context, int priority)
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

        Assert.assertEquals("DTT0", 0, triggers.allTriggers().size());

        String triggerId = "date-test";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault());

        long now = System.currentTimeMillis();

        try
        {
            JSONObject triggerDef = new JSONObject();
            triggerDef.put("type", "datetime");

            triggerDef.put(Trigger.NAME, "Date Test");
            triggerDef.put(Trigger.IDENTIFIER, triggerId);
            triggerDef
                    .put(Trigger.ACTION,
                            "PurpleRobot.playDefaultTone(); PurpleRobot.testLog('Date Test: Fired!'); PurpleRobot.vibrate('SOS');");
            triggerDef.put(DateTrigger.DATETIME_START, sdf.format(new Date(now + 60000)));
            triggerDef.put(DateTrigger.DATETIME_END, sdf.format(new Date(now + 119999)));

            String script = "PurpleRobot.updateTrigger('" + triggerId + "', "
                    + triggerDef.toString().replace("'", "\\'").replace("\"", "'") + ");";

            BaseScriptEngine.runScript(this._context, script);

            this.broadcastUpdate("Created test trigger.");

            Thread.sleep(2000);
        }
        catch (JSONException e)
        {
            Assert.fail("DTT1");
        }
        catch (InterruptedException e)
        {
            Assert.fail("DTT2");
        }

        this.broadcastUpdate("Verifying trigger exists...");

        List<Trigger> allTriggers = triggers.allTriggers();
        Assert.assertEquals("DTT3", 1, allTriggers.size());

        Trigger trigger = allTriggers.get(0);
        Assert.assertEquals("DTT4", triggerId, trigger.identifier());

        Assert.assertEquals("DTT5", trigger.getClass(), DateTrigger.class);

        DateTrigger dateTrigger = (DateTrigger) trigger;

        Assert.assertTrue("DTT6", dateTrigger.matches(this._context, new Date(now + 90000)));

        List<Long> upcomingTimes = triggers.upcomingFireTimes(this._context);

        Assert.assertTrue("DTT7", upcomingTimes.size() > 0);

        try
        {
            this.broadcastUpdate("Sleeping. 90 seconds remaining...", 0);
            Thread.sleep(15000);
            this.broadcastUpdate("Sleeping. 75 seconds remaining...", 0);
            Thread.sleep(15000);
            this.broadcastUpdate("Sleeping. 60 seconds remaining...", 0);
            Thread.sleep(15000);
            this.broadcastUpdate("Sleeping. 45 seconds remaining...", 0);
            Thread.sleep(15000);
            this.broadcastUpdate("Sleeping. 30 seconds remaining...", 0);
            Thread.sleep(15000);
            this.broadcastUpdate("Sleeping. 15 seconds remaining...", 0);
            Thread.sleep(15000);
        }
        catch (InterruptedException e)
        {
            Assert.fail();
        }

        this.broadcastUpdate("Verifying that trigger fired...");

        Assert.assertTrue("DTT8", dateTrigger.lastFireTime(this._context) > 0);
        Assert.assertTrue("DTT9", dateTrigger.lastFireTime(this._context) < System.currentTimeMillis());

        this.broadcastUpdate("Clearing triggers...");

        triggers.removeAllTriggers();
    }

    public int estimatedMinutes()
    {
        return 2;
    }

    public String name(Context context)
    {
        return context.getString(R.string.name_date_trigger_test);
    }
}
