package edu.northwestern.cbits.purple_robot_manager.tests.ui;

import org.mozilla.javascript.NativeJavaObject;

import junit.framework.Assert;
import android.content.Context;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.tests.RobotTestCase;

public class NonAsciiDialogTestCase extends RobotTestCase
{
    public NonAsciiDialogTestCase(Context context, int priority)
    {
        super(context, priority);
    }

    public void test()
    {
        if (this.isSelected(this._context) == false)
            return;

        try
        {
            BaseScriptEngine.runScript(this._context, "PurpleRobot.persistString('ASCII', '?');");

            Thread.sleep(1000);

            NativeJavaObject value = (NativeJavaObject) BaseScriptEngine.runScript(this._context,
                    "PurpleRobot.fetchString('ASCII')");

            Assert.assertEquals("NAD002", "?", value.unwrap());

            this.broadcastUpdate("Fetching response...");

            Thread.sleep(1000);

            String script = "PurpleRobot.showNativeDialog(\"Non-ASCII Dialog Test\", \"This looks Russian: Век живи́ -- век учи́сь.\", \"Да\", \"Нет\", \"PurpleRobot.persistString('ASCII', 'Y')\", \"PurpleRobot.persistString('ASCII', 'N')\")";

            BaseScriptEngine.runScript(this._context, script);

            Thread.sleep(5000);

            value = (NativeJavaObject) BaseScriptEngine.runScript(this._context, "PurpleRobot.fetchString('ASCII')");

            Assert.assertEquals("NAD003", "Y", value.unwrap());
        }
        catch (InterruptedException e)
        {
            Assert.fail("NAD001");
        }
    }

    public String name(Context context)
    {
        return context.getString(R.string.name_non_ascii_dialog_test);
    }
}
