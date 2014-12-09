package edu.northwestern.cbits.purple_robot_manager.tests;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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

public class RandomDateTriggerTestCase extends RobotTestCase
{
    public RandomDateTriggerTestCase(Context context, int priority)
    {
        super(context, priority);
    }

    @Override
    public void test()
    {
        if (this.isSelected(this._context) == false)
            return;

        TriggerManager triggers = TriggerManager.getInstance(this._context);

        this.broadcastUpdate("Clearing triggers...");

        triggers.removeAllTriggers();
        Assert.assertEquals("RRRDT0", 0, triggers.allTriggers().size());

        long[] intervals =
        { 600000, 900000, 1800000, 3600000, 7200000, 14400000 };

        for (long interval : intervals)
        {
            BaseScriptEngine.runScript(this._context, "PurpleRobot.persistString('repeat-test-token', '');");
            NativeJavaObject persisted = (NativeJavaObject) BaseScriptEngine.runScript(this._context, "PurpleRobot.fetchString('repeat-test-token');");

            Assert.assertEquals("RRDT1-" + interval, "", persisted.unwrap()); // TODO:
            // Unwrap at
            // scripting
            // engine?

            long now = System.currentTimeMillis();

            String triggerId = "random-date-test-" + now;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

            try
            {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(now + interval);

                JSONObject triggerDef = new JSONObject();
                triggerDef.put("type", "datetime");

                triggerDef.put(Trigger.NAME, "Random Date Test");
                triggerDef.put(Trigger.IDENTIFIER, triggerId);
                triggerDef.put(Trigger.ACTION, "PurpleRobot.playDefaultTone(); PurpleRobot.testLog('Date Test: Random Fired!'); PurpleRobot.persistString('repeat-test-token', 'run'); PurpleRobot.vibrate('SOS');");
                triggerDef.put(DateTrigger.DATETIME_START, sdf.format(new Date(now)));
                triggerDef.put(DateTrigger.DATETIME_END, sdf.format(new Date(cal.getTimeInMillis())));
                triggerDef.put(DateTrigger.DATETIME_RANDOM, true);

                String script = "PurpleRobot.updateTrigger('" + triggerId + "', " + triggerDef.toString().replace("'", "\\'").replace("\"", "'") + ");";

                BaseScriptEngine.runScript(this._context, script);

                this.broadcastUpdate("Created random trigger. Sleeping...");

                Thread.sleep(interval);
            }
            catch (JSONException e)
            {
                Assert.fail("RRDT2-" + interval);
            }
            catch (InterruptedException e)
            {
                Assert.fail("RRDT3-" + interval);
            }

            this.broadcastUpdate("Verifying random trigger fired...");

            persisted = (NativeJavaObject) BaseScriptEngine.runScript(this._context, "PurpleRobot.fetchString('repeat-test-token');");

            Assert.assertEquals("RRDT4-" + interval, "run", persisted.unwrap());
        }

        this.broadcastUpdate("Clearing triggers...");

        triggers.removeAllTriggers();
    }

    @Override
    public int estimatedMinutes()
    {
        return 60 * 16;
    }

    @Override
    public String name(Context context)
    {
        return context.getString(R.string.name_random_date_trigger_test);
    }
}
