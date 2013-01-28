package edu.northwestern.cbits.purple.notifier;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
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
		String widget = intent.getStringExtra(WidgetIntentService.WIDGET);
		
		String launchIntent = null;
		
		if (intent.hasExtra(WidgetIntentService.LAUNCH_INTENT))
			launchIntent = intent.getStringExtra(WidgetIntentService.LAUNCH_INTENT);
		
		if (BasicWidgetProvider.NAME.equals(widget))
		{
			RemoteViews rv = new RemoteViews(this.getPackageName(), R.layout.layout_basic_widget);

			rv.setTextViewText(R.id.widget_basic_title_text, intent.getStringExtra("config_basic_title"));
			rv.setTextViewText(R.id.widget_basic_message_text, intent.getStringExtra("config_basic_message"));

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			Editor e = prefs.edit();

			if (launchIntent != null)
				e.putString(BasicWidgetProvider.WIDGET_LAUNCH, launchIntent);
			else
				e.remove(BasicWidgetProvider.WIDGET_LAUNCH);
			
			e.commit();

			PendingIntent pendingIntent = this.intentForString(prefs.getString(BasicWidgetProvider.WIDGET_LAUNCH, null));

			if (pendingIntent != null)
				rv.setOnClickPendingIntent(R.id.widget_basic_layout, pendingIntent);
			
			ComponentName widgetName = new ComponentName(this, BasicWidgetProvider.class);
			AppWidgetManager.getInstance(this).updateAppWidget(widgetName, rv);
		}
		else if (TextWidgetProvider.NAME.equals(widget))
		{
			RemoteViews rv = new RemoteViews(this.getPackageName(), R.layout.layout_text_widget);

			rv.setTextViewText(R.id.widget_text_title_text, intent.getStringExtra("config_text_title"));
			rv.setTextViewText(R.id.widget_text_message_text, intent.getStringExtra("config_text_message"));
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			Editor e = prefs.edit();

			if (launchIntent != null)
				e.putString(TextWidgetProvider.WIDGET_LAUNCH, launchIntent);
			else
				e.remove(TextWidgetProvider.WIDGET_LAUNCH);
			
			e.commit();
			
			PendingIntent pendingIntent = this.intentForString(prefs.getString(TextWidgetProvider.WIDGET_LAUNCH, null));
			
			if (pendingIntent != null)
				rv.setOnClickPendingIntent(R.id.widget_text_layout, pendingIntent);
			
			ComponentName widgetName = new ComponentName(this, TextWidgetProvider.class);
			AppWidgetManager.getInstance(this).updateAppWidget(widgetName, rv);
		}
	}

	private PendingIntent intentForString(String jsonString) 
	{
		return null;
	}
}
