package edu.northwestern.cbits.purple_robot_manager;

import android.app.Application;
import android.content.Context;

public class PurpleRobotApplication extends Application
{
    private static Context context;

    public void onCreate()
    {
        super.onCreate();

        PurpleRobotApplication.context = this.getApplicationContext();
    }

    public static Context getAppContext()
    {
        return PurpleRobotApplication.context;
    }
}


