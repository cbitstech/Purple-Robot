package edu.northwestern.cbits.purple_robot_manager.tests;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
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
		int minutes = this.estimatedMinutes();
		
		if (minutes < 1)
			return context.getString(R.string.description_minute_or_less_test);

		return context.getString(R.string.description_minutes_test, minutes);
	}

	public int estimatedMinutes() 
	{
		return 1;
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

	public boolean isSelected(Context context) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		return prefs.getBoolean("test_" + this.name(context), false);
	}
	
	public void setSelected(Context context, boolean isSelected)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("test_" + this.name(context), isSelected);
		e.commit();
	}

	public int compareTo(Context context, RobotTestCase other) 
	{
		if (this._priority < other._priority)
			return -1;
		else if (this._priority > other._priority)
			return 1;
		else if (this.estimatedMinutes() < other.estimatedMinutes())
			return -1;
		else if (this.estimatedMinutes() > other.estimatedMinutes())
			return 1;
		
		return this.name(context).compareToIgnoreCase(other.name(context));
	}
}
