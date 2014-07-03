package edu.northwestern.cbits.purple_robot_manager.tests;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class ModelsTestCase extends RobotTestCase 
{
	public ModelsTestCase(Context context, int priority) 
	{
		super(context, priority);
	}
	
	public void test() 
	{

	}

	public String name(Context context) 
	{
		return context.getString(R.string.name_models_test);
	}
}
