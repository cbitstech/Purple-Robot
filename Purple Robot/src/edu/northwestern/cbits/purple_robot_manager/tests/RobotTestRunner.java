package edu.northwestern.cbits.purple_robot_manager.tests;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import android.content.Context;
import android.test.AndroidTestRunner;

public class RobotTestRunner extends AndroidTestRunner 
{
	private List<TestCase> _cases = new ArrayList<TestCase>();
	private RobotTestSuite _suite = new RobotTestSuite();;

	private Thread _thread = null;
	
	public RobotTestRunner(Context context)
	{
		super();
		
		this._suite.addTest(new JUnitTestCase(context, 0));
		this._suite.addTest(new EncryptionTestCase(context, 0));
		this._suite.addTest(new DateTriggerTestCase(context, 0));
		this._suite.addTest(new RepeatingTriggerTestCase(context, 8));
		this._suite.addTest(new HalfHourDateTriggerTestCase(context, 9));
	}
	
	public List<TestCase> getTestCases ()
	{
		if (this._cases.size() == 0)
		{
			Enumeration<Test> tests = this._suite.tests();
		
			while (tests.hasMoreElements())
			{
				Test test = tests.nextElement();
			
				if (test instanceof RobotTestCase)
				{
					RobotTestCase robotTest = (RobotTestCase) test;
				
					this._cases.add(robotTest);
				}
			}
		}
		
		return this._cases;
	}

	public boolean isRunning() 
	{
		return this._thread != null;
	}

	public void startTests(final TestResult result, final Runnable next) 
	{
		final RobotTestRunner me = this;
		
		this._thread = new Thread(new Runnable()
		{
			public void run() 
			{
				for (TestCase testCase : me.getTestCases())
					testCase.run(result);
				
				me._thread = null;
				
				if (next != null)
					next.run();
			}
		});
		
		this._thread.start();
	}

	public void stopTests() 
	{
		if (this._thread != null)
		{
			this._thread.interrupt();
		}
	}
}