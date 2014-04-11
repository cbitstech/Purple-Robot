package edu.northwestern.cbits.purple_robot_manager;

import org.json.JSONException;
import org.json.JSONObject;

import edu.northwestern.cbits.purple_robot_manager.config.LegacyJSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class BootUpReceiver extends BroadcastReceiver
{
	public static final String BOOT_KEY = "system_last_boot";

    public void onReceive(Context context, Intent intent)
    {
    	long now = System.currentTimeMillis();
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	
    	Editor e = prefs.edit();
    	
    	e.putLong(BootUpReceiver.BOOT_KEY, now);
    	
    	e.commit();

    	ManagerService.setupPeriodicCheck(context);

    	LegacyJSONConfigFile.getSharedFile(context.getApplicationContext());
    	
    	TriggerManager.getInstance(context).fireMissedTriggers(context, now);
    	
    	if (prefs.contains(BaseScriptEngine.STICKY_NOTIFICATION_PARAMS))
    	{
    		try 
    		{
				JSONObject json = new JSONObject(prefs.getString(BaseScriptEngine.STICKY_NOTIFICATION_PARAMS, "{}"));

				JavaScriptEngine engine = new JavaScriptEngine(context);
				
				engine.showScriptNotification(json.getString("title"), json.getString("message"), json.getBoolean("persistent"), json.getBoolean("sticky"), json.getString("script"));
			}
    		catch (JSONException ex) 
    		{
    			LogManager.getInstance(context).logException(ex);
			}
    		
    	}
    }
}
