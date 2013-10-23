package edu.northwestern.cbits.purple.notifier;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotifierReceiver extends BroadcastReceiver 
{
	public void onReceive(Context context, Intent intent) 
	{
		context.startService(intent);
	}
}
