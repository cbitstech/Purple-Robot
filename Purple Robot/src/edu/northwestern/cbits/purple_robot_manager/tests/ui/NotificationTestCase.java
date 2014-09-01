package edu.northwestern.cbits.purple_robot_manager.tests.ui;

import org.mozilla.javascript.NativeJavaObject;

import junit.framework.Assert;
import android.content.Context;
import android.util.Log;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.tests.RobotTestCase;

public class NotificationTestCase extends RobotTestCase 
{
	public NotificationTestCase(Context context, int priority) 
	{
		super(context, priority);
	}
	
	public void test() 
	{
		if (this.isSelected(this._context) == false)
			return;
		
		try 
		{
			BaseScriptEngine.runScript(this._context, "PurpleRobot.showScriptNotification('Test Notification', 'Testing the notification system...', true, '');");

			Thread.sleep(1000);

			BaseScriptEngine.runScript(this._context, "PurpleRobot.showNativeDialog('Notification Test Case', 'Do you see a notification above?', 'Yes', 'No', 'PurpleRobot.persistString(\"Dialog\", \"Y\");', 'PurpleRobot.persistString(\"Dialog\", \"N\")');");

			this.broadcastUpdate("Hiding notification...");

			Thread.sleep(10000);

			NativeJavaObject value = (NativeJavaObject) BaseScriptEngine.runScript(this._context, "PurpleRobot.fetchString('Dialog')");
			
			Assert.assertEquals("NOT0001", "Y", value.unwrap());

			Thread.sleep(1000);

			BaseScriptEngine.runScript(this._context, "PurpleRobot.cancelScriptNotification();");
			
			Thread.sleep(5000);
			
			Log.e("PR", "1");

			BaseScriptEngine.runScript(this._context, "PurpleRobot.showNativeDialog('Notification Test Case', 'Is the notification gone?', 'Yes', 'No', 'PurpleRobot.persistString(\"Dialog-Gone\", \"Y\");', 'PurpleRobot.persistString(\"Dialog-Gone\", \"N\")');");

			Thread.sleep(10000);

			Log.e("PR", "2");

			value = (NativeJavaObject) BaseScriptEngine.runScript(this._context, "PurpleRobot.fetchString('Dialog-Gone')");

			Log.e("PR", "3");

			Assert.assertNotNull("NOT0002", value);
			Assert.assertEquals("NOT0003", "Y", value.unwrap());

			Log.e("PR", "4");

			BaseScriptEngine.runScript(this._context, "PurpleRobot.persistString('Dialog-Gone', 'N');");
			Thread.sleep(1000);

			Log.e("PR", "5");

			BaseScriptEngine.runScript(this._context, "PurpleRobot.showScriptNotification('Test Notification', 'Tap the notification to hide it.', true, 'PurpleRobot.persistString(\"Dialog-Hide\", \"Y\")');");

			Log.e("PR", "6");

			Thread.sleep(10000);

			value = (NativeJavaObject) BaseScriptEngine.runScript(this._context, "PurpleRobot.fetchString('Dialog-Hide')");

			Log.e("PR", "7");

			Assert.assertEquals("NOT0004", "Y", value.unwrap());
		}
		catch (InterruptedException e) 
		{
			Assert.fail("NOT0100");
		}
	}

	public String name(Context context) 
	{
		return context.getString(R.string.name_notification_test);
	}
}
