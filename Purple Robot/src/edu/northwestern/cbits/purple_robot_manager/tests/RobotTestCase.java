package edu.northwestern.cbits.purple_robot_manager.tests;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.test.AndroidTestCase;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.TestActivity;

public abstract class RobotTestCase extends AndroidTestCase 
{
	protected int _priority = Integer.MIN_VALUE;
	protected Context _context;
	
	public RobotTestCase(Context context, int priority) 
	{
		super();
		
		this._context = context;
		this._priority = priority;

		this.setName("test");
	}

	protected void setUp() throws Exception 
	{
	    super.setUp();
	}
	
	public abstract void test();

	public abstract String name(Context context); 

	public String description(Context context) 
	{
		int count = this.countTestCases();
		
		if (count != 1)
			return context.getString(R.string.subtitle_run_tests, count);

		return context.getString(R.string.subtitle_run_tests_single, count);
	}

	protected void broadcastUpdate(String message, long delay) 
	{
		LocalBroadcastManager bcast = LocalBroadcastManager.getInstance(this._context);
		
		Intent intent = new Intent(TestActivity.INTENT_PROGRESS_MESSAGE);
		intent.putExtra(TestActivity.PROGRESS_MESSAGE, message);
		intent.putExtra(TestActivity.PROGRESS_DELAY, delay);
		
		bcast.sendBroadcastSync(intent);
	}

	protected void broadcastUpdate(String message)
	{
		this.broadcastUpdate(message, 500);
	}
}
