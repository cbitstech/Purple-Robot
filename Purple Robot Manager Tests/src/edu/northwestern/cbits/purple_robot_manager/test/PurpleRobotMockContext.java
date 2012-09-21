package edu.northwestern.cbits.purple_robot_manager.test;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;

public class PurpleRobotMockContext extends MockContext
{
	public void startActivity (Intent intent)
	{

	}

	public Resources getResources()
	{
		return new PurpleRobotMockResources();
	}

	public PackageManager getPackageManager()
	{
		return new PurpleRobotMockPackageManager();
	}

	public Object getSystemService(String name)
	{
		if (Context.NOTIFICATION_SERVICE.equals(name))
		{

		}

		return super.getSystemService(name);
	}

	public ContentResolver getContentResolver()
	{
		return new MockContentResolver();
	}

	public String getPackageName()
	{
		return "edu.northwestern.cbits.purple_robot_manager";
	}
}
