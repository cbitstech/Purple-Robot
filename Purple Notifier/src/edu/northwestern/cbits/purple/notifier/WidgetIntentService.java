package edu.northwestern.cbits.purple.notifier;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetIntentService extends IntentService 
{
	public final static String UPDATE_WIDGET = "edu.northwestern.cbits.purple.UPDATE_WIDGET";
	public final static String WIDGET = "WIDGET";
	public final static String LAUNCH_INTENT = "LAUNCH_INTENT";

	public WidgetIntentService() 
	{
		super("WidgetIntentService");
	}
	public WidgetIntentService(String name) 
	{
		super(name);
	}

	protected void onHandleIntent(Intent intent) 
	{
		Log.e("PN-SERV", "RUNNING INTENT: " + intent);
		
		String widget = intent.getStringExtra(WidgetIntentService.WIDGET);
		
		int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, Integer.MAX_VALUE);
		
		Log.e("PN-SERV", "WIDGET ID: " + widgetId);
		
		if (BasicWidgetProvider.NAME.equals(widget))
		{
			Log.e("P-NSRV", "BASIC WIDGET");
			RemoteViews rv = new RemoteViews(this.getPackageName(), R.layout.layout_basic_widget);

			rv.setTextViewText(R.id.widget_basic_title_text, intent.getStringExtra("config_basic_title"));
			rv.setTextViewText(R.id.widget_basic_message_text, intent.getStringExtra("config_basic_message"));

			PendingIntent pendingIntent = this.pendingIntentFromIntent(intent);
			
			if (pendingIntent != null)
				rv.setOnClickPendingIntent(R.id.widget_basic_layout, pendingIntent);
			
			ComponentName widgetName = new ComponentName(this, BasicWidgetProvider.class);
			AppWidgetManager.getInstance(this).updateAppWidget(widgetName, rv);
		}
		else if (TextWidgetProvider.NAME.equals(widget))
		{
			Log.e("P-NSRV", "TEXT WIDGET");
			RemoteViews rv = new RemoteViews(this.getPackageName(), R.layout.layout_text_widget);

			rv.setTextViewText(R.id.widget_text_title_text, intent.getStringExtra("config_text_title"));
			rv.setTextViewText(R.id.widget_text_message_text, intent.getStringExtra("config_text_message"));
			
			PendingIntent pendingIntent = this.pendingIntentFromIntent(intent);
			
			if (pendingIntent != null)
				rv.setOnClickPendingIntent(R.id.widget_text_layout, pendingIntent);
			
			ComponentName widgetName = new ComponentName(this, TextWidgetProvider.class);
			AppWidgetManager.getInstance(this).updateAppWidget(widgetName, rv);
		}
	}

	private PendingIntent pendingIntentFromIntent(Intent intent) 
	{
		if (intent.hasExtra("package_name"))
			return PendingIntent.getActivity(this, 0, this.getPackageManager().getLaunchIntentForPackage(intent.getStringExtra("package_name")), 0);
		
		return null;
	}
}
