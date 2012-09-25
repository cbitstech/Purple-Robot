package edu.northwestern.cbits.purple_robot_manager.test;

import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeJavaObject;

import junit.framework.Assert;
import edu.northwestern.cbits.purple_robot_manager.JavaScriptEngine;
import android.test.InstrumentationTestCase;

public class JavaScriptEngineTest extends InstrumentationTestCase
{
	protected void setUp() throws Exception
	{
		super.setUp();
	}

	public void testConstructor()
	{
		Assert.assertNotNull(new JavaScriptEngine(new PurpleRobotMockContext()));
	}

	public void testScriptValidity()
	{
		JavaScriptEngine engine = new JavaScriptEngine(new PurpleRobotMockContext());

		try
		{
			engine.runScript("this is an invalid script");
			Assert.fail("Did not throw EvaluatorException on invalid script.");
		}
		catch (EvaluatorException e)
		{

		}

		try
		{
			Object result = engine.runScript("1 + 1");

			Assert.assertNotNull(result);

			if (result instanceof Double)
			{
				Double d = (Double) result;

				Assert.assertEquals(d.doubleValue(), 2.0);
			}
			else
				Assert.fail("Did not return a Double object. Returned " + Object.class);
		}
		catch (EvaluatorException e)
		{
			Assert.fail("Script execution failed.");
		}
	}

	public void testSingleton()
	{
		JavaScriptEngine engine = new JavaScriptEngine(new PurpleRobotMockContext());

		Assert.assertNotNull(engine.runScript("PurpleRobot"));

		try
		{
			engine.runScript("PurpleRobot.noSuchMethod();");
			Assert.fail("Did not throw EcmaError on non-existent method.");
		}
		catch (EcmaError e)
		{

		}
	}

	public void testSingletonMethods()
	{
		JavaScriptEngine engine = new JavaScriptEngine(new PurpleRobotMockContext());

		try
		{
			engine.runScript("PurpleRobot.emitToast('Toast message', false);");
		}
		catch (EcmaError e)
		{
			Assert.fail("Threw error on PurpleRobot.emitToast method.");
		}

		try
		{
			engine.runScript("PurpleRobot.launchUrl('http://www.google.com');");
		}
		catch (EcmaError e)
		{
			Assert.fail("Threw error on PurpleRobot.launchUrl method.");
		}

		try
		{
			Object result = engine.runScript("PurpleRobot.launchUrl('http://www.google.com');");

			if (result instanceof Boolean)
			{
				Boolean b = (Boolean) result;

				Assert.assertTrue(b.booleanValue());
			}
			else
				Assert.fail("Value returned from PurpleRobot.launchUrl is not a boolean: " + result.getClass());
		}
		catch (EcmaError e)
		{
			Assert.fail("Threw error on PurpleRobot.launchUrl method.");
		}

		try
		{
			Object result = engine.runScript("PurpleRobot.launchApplication('NoSuchApplication');");

			if (result instanceof Boolean)
			{
				Boolean b = (Boolean) result;

				Assert.assertFalse(b.booleanValue());
			}
			else
				Assert.fail("Value returned from PurpleRobot.launchApplication is not a boolean: " + result.getClass());

			result = engine.runScript("PurpleRobot.launchApplication('Facebook');");

			if (result instanceof Boolean)
			{
				Boolean b = (Boolean) result;

				Assert.assertTrue(b.booleanValue());
			}
			else
				Assert.fail("Value returned from PurpleRobot.launchApplication is not a boolean: " + result.getClass());

		}
		catch (EcmaError e)
		{
			Assert.fail("Threw error on PurpleRobot.launchApplication method.");
		}

		try
		{
			Object result = engine.runScript("PurpleRobot.showApplicationLaunchNotification('Test Title', 'Test Message', 'Facebook', 0);");

			if (result instanceof Boolean)
			{
				Boolean b = (Boolean) result;

				Assert.assertTrue(b.booleanValue());
			}
			else
				Assert.fail("Value returned from PurpleRobot.showApplicationLaunchNotification is not a boolean: " + result.getClass());

		}
		catch (EcmaError e)
		{
			Assert.fail("Threw error on PurpleRobot.showApplicationLaunchNotification method.");
		}

		try
		{
			Object result = engine.runScript("PurpleRobot.version();");

			if (result instanceof NativeJavaObject)
			{
				NativeJavaObject javaObj = (NativeJavaObject) result;

				if (NativeJavaObject.canConvert(result, String.class))
				{
					String resultString = (String) javaObj.unwrap();

					Assert.assertTrue(resultString.length() > 0);
				}
				else
				{
					Assert.fail("Value returned from PurpleRobot.version cannot convert to a String: " + result);
				}
			}
		}
		catch (EcmaError e)
		{
			Assert.fail("Threw error on PurpleRobot.version method.");
		}

		try
		{
			Object result = engine.runScript("PurpleRobot.versionCode();");

			if (result instanceof Integer)
			{
				Integer resultInt = (Integer) result;

				Assert.assertTrue(resultInt.intValue() > 0);
			}
			else
				Assert.fail("Value returned from PurpleRobot.versionCode is not an Integer: " + result.getClass());
		}
		catch (EcmaError e)
		{
			Assert.fail("Threw error on PurpleRobot.versionCode method.");
		}

		Assert.fail("Need to add test for dialog method...");
		Assert.fail("Need to add test for widget method...");
	}
}
