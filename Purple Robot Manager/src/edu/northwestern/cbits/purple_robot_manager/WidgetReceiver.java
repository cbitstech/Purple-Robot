package edu.northwestern.cbits.purple_robot_manager;

import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WidgetReceiver extends BroadcastReceiver
{
    public void onReceive(Context context, Intent intent)
    {
    	String action = intent.getStringExtra("widget_action");
    	
    	String script = null;

    	if ("tap".equals(action))
    		script = intent.getStringExtra("action");
    	else if ("tap_one".equals(action))
    		script = intent.getStringExtra("action_one");
    	else if ("tap_two".equals(action))
    		script = intent.getStringExtra("action_two");
    	else if ("tap_three".equals(action))
    		script = intent.getStringExtra("action_three");
    	else if ("tap_four".equals(action))
    		script = intent.getStringExtra("action_four");
    	else if ("tap_five".equals(action))
    		script = intent.getStringExtra("action_five");
    	
		if (script != null)
		{
			try
			{
    			JavaScriptEngine engine = new JavaScriptEngine(context);
    			
    			engine.runScript(script);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
    }
}
