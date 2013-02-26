package edu.northwestern.cbits.purple_robot_manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WidgetReceiver extends BroadcastReceiver
{
    public void onReceive(Context context, Intent intent)
    {
    	String action = intent.getStringExtra("widget_action");
    	if ("tap".equals(action))
    	{
    		String script = intent.getStringExtra("action");
    		
    		if (script != null)
    		{
    			JavaScriptEngine engine = new JavaScriptEngine(context);
    			
    			engine.runScript(script);
    		}
    	}
    }
}
