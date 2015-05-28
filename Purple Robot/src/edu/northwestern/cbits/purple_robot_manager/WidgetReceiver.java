package edu.northwestern.cbits.purple_robot_manager;

import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;

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

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("widget_action", action);
        LogManager.getInstance(context).log("pr_widget_tapped", payload);

        if (script != null)
        {
            try
            {
                BaseScriptEngine.runScript(context, script);
            }
            catch (Exception e)
            {
                LogManager.getInstance(context).logException(e);
            }
        }
    }
}
