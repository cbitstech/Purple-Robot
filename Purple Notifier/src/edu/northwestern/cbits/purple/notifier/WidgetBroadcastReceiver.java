package edu.northwestern.cbits.purple.notifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WidgetBroadcastReceiver extends BroadcastReceiver 
{
	public void onReceive(Context context, Intent intent) 
	{
		Log.e("PR-RECV", "GOT INTENT, PASSING ALONG: " + intent);
		
		context.startService(intent);
	}
}
