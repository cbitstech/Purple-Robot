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
    	else if ("tap_one".equals(action))
    	{
    		String script = intent.getStringExtra("action_one");
    		
    		if (script != null)
    		{
    			JavaScriptEngine engine = new JavaScriptEngine(context);
    			
    			engine.runScript(script);
    		}
    	}
    	else if ("tap_two".equals(action))
    	{
    		String script = intent.getStringExtra("action_two");
    		
    		if (script != null)
    		{
    			JavaScriptEngine engine = new JavaScriptEngine(context);
    			
    			engine.runScript(script);
    		}
    	}
    	else if ("tap_three".equals(action))
    	{
    		String script = intent.getStringExtra("action_three");
    		
    		if (script != null)
    		{
    			JavaScriptEngine engine = new JavaScriptEngine(context);
    			
    			engine.runScript(script);
    		}
    	}
    	else if ("tap_four".equals(action))
    	{
    		String script = intent.getStringExtra("action_four");
    		
    		if (script != null)
    		{
    			JavaScriptEngine engine = new JavaScriptEngine(context);
    			
    			engine.runScript(script);
    		}
    	}
    	else if ("tap_five".equals(action))
    	{
    		String script = intent.getStringExtra("action_five");
    		
    		if (script != null)
    		{
    			JavaScriptEngine engine = new JavaScriptEngine(context);
    			
    			engine.runScript(script);
    		}
    	}
    }
}
