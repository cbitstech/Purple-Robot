package edu.northwestern.cbits.purple_robot_manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class ShutdownReceiver extends BroadcastReceiver
{
	public static final String SHUTDOWN_KEY = "system_last_halt";

    public void onReceive(Context context, Intent intent)
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	
    	Editor e = prefs.edit();
    	
    	e.putLong(ShutdownReceiver.SHUTDOWN_KEY, System.currentTimeMillis());
    	
    	e.commit();
    }
}
