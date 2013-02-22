package edu.northwestern.cbits.purple_robot_manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WidgetReceiver extends BroadcastReceiver
{
    public void onReceive(Context context, Intent intent)
    {
    	Log.e("PR", "ROBOT RECV " + intent.getAction());
    	Log.e("PR", "ROBOT BUNDLE " + intent.getExtras());
    	
    	String action = intent.getStringExtra("widget_action");

    	Log.e("PR", "ROBOT ACTION " + action);

    	if ("tap".equals(action))
    	{
    		String script = intent.getStringExtra("action");

    		Log.e("PR", "ROBOT SCRIPT " + script);
    		
    		if (script != null)
    		{
    			JavaScriptEngine engine = new JavaScriptEngine(context);
    			
    			engine.runScript(script);
    		}
    	}
    }
}
