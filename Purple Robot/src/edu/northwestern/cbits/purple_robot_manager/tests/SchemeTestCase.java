package edu.northwestern.cbits.purple_robot_manager.tests;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class SchemeTestCase extends RobotTestCase 
{
	public SchemeTestCase(Context context, int priority) 
	{
		super(context, priority);
	}
	
	public void test() 
	{
		if (this.isSelected(this._context) == false)
			return;
	}

	public String name(Context context) 
	{
		return context.getString(R.string.name_scheme_test);
	}
}
