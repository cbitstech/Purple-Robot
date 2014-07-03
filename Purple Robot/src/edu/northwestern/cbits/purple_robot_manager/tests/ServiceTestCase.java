package edu.northwestern.cbits.purple_robot_manager.tests;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class ServiceTestCase extends RobotTestCase 
{
	public ServiceTestCase(Context context, int priority) 
	{
		super(context, priority);
	}
	
	public void test() 
	{

	}

	public String name(Context context) 
	{
		return context.getString(R.string.name_service_test);
	}
}
