package edu.northwestern.cbits.purple_robot_manager.test;

import edu.northwestern.cbits.purple_robot_manager.R;
import android.test.mock.MockResources;

public class PurpleRobotMockResources extends MockResources
{
	public String[] getStringArray(int resourceId)
	{
		switch (resourceId)
		{
			case R.array.app_package_keys:
				String[] keyArray = new String[1];
				keyArray[0] = "Facebook";

				return keyArray;

			case R.array.app_package_values:
				String[] valueArray = new String[1];
				valueArray[0] = "com.facebook.katana";

				return valueArray;
		}

		return super.getStringArray(resourceId);
	}
}
