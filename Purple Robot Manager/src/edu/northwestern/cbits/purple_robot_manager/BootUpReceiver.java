package edu.northwestern.cbits.purple_robot_manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootUpReceiver extends BroadcastReceiver
{
    public void onReceive(Context context, Intent intent)
    {
    	Log.e("PR", "ROBOT RECV " + intent.getAction());
    	Log.e("PR", "ROBOT BUNDLE " + intent.getExtras());
    	
    	ManagerService.setupPeriodicCheck(context);
    }
}
