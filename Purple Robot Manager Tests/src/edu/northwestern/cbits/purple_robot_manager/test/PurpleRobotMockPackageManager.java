package edu.northwestern.cbits.purple_robot_manager.test;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.test.mock.MockPackageManager;

public class PurpleRobotMockPackageManager extends MockPackageManager
{
	public Intent getLaunchIntentForPackage(String packageName)
	{
		if ("com.facebook.katana".equals(packageName))
			return new Intent();

		return null;
	}

	public PackageInfo getPackageInfo(String packageName, int flags)
	{
		PackageInfo info = new PackageInfo();

		info.versionName = "1.0";
		info.versionCode = 100;

		return info;
	}
}
