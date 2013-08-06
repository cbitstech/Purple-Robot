package edu.northwestern.cbits.purple.notifier;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.RemoteViews;

public class TextWidgetProvider extends PurpleWidgetProvider
{
	public static final String NAME = "TEXT_WIDGET_UPDATE";
	public static final String WIDGET_LAUNCH = "config_widget_basic_launch";

	public static void setupWidget(Context context, int widgetId, Intent intent) 
	{
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_text_widget);
		
		Bundle extras = intent.getExtras();
		AppWidgetManager widgets = AppWidgetManager.getInstance(context);
		
		String titleColor = "#ffffff";
		String messageColor = "#ffffff";
		
		if (extras.containsKey("title_color"))
			titleColor = extras.getString("title_color");

		if (extras.containsKey("message_color"))
			messageColor = extras.getString("message_color");

		remoteViews.setTextColor(R.id.widget_text_title_text, Color.parseColor(titleColor));
		remoteViews.setTextColor(R.id.widget_text_message_text, Color.parseColor(messageColor));

		String title = extras.getString("title");
		String message = extras.getString("message");

		remoteViews.setTextViewText(R.id.widget_text_title_text, title);
		remoteViews.setTextViewText(R.id.widget_text_message_text, message);

		Intent tapIntent = new Intent(WidgetIntentService.WIDGET_ACTION);
		tapIntent.putExtras(intent);
		tapIntent.putExtra("widget_action", "tap");

		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, widgetId, tapIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_text_layout, pendingIntent);
		
		widgets.updateAppWidget(widgetId, remoteViews);
	}
}
