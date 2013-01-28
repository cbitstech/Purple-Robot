package edu.northwestern.cbits.purple.notifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WidgetBroadcastReceiver extends BroadcastReceiver 
{
	public void onReceive(Context context, Intent intent) 
	{
		Intent newIntent = new Intent(context, WidgetIntentService.class);
		newIntent.setAction(intent.getAction());
		newIntent.putExtras(intent.getExtras());
		
		context.startService(newIntent);
	}
}
