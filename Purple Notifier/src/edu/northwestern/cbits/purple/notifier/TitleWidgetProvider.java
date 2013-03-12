package edu.northwestern.cbits.purple.notifier;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.RemoteViews;

public class TitleWidgetProvider extends PurpleWidgetProvider
{
	public static final String NAME = "TITLE_WIDGET_UPDATE";
	public static final String WIDGET_LAUNCH = "config_widget_title_launch";

//	private static final int MINIMUM_WIDTH = 250;
//	private static final int MINIMUM_HEIGHT = 40;
	
	public static void setupWidget(Context context, int widgetId, Intent intent) 
	{
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_title_widget);
		
		Bundle extras = intent.getExtras();
		AppWidgetManager widgets = AppWidgetManager.getInstance(context);

		String title = extras.getString("title");

        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(50); 
        textPaint.setTextAlign(Paint.Align.CENTER);
        

        /* String testString = "YppqLYppqLYppqL";
        
        Rect bounds = new Rect();
        textPaint.getTextBounds(testString, 0, testString.length(), bounds);
        
        while (bounds.width() < TitleWidgetProvider.MINIMUM_WIDTH && bounds.height() < TitleWidgetProvider.MINIMUM_HEIGHT)
        {
        	textPaint.setTextSize(textPaint.getTextSize() + 1);

            textPaint.getTextBounds(testString, 0, testString.length(), bounds);
        }

        while (bounds.width() > TitleWidgetProvider.MINIMUM_WIDTH && bounds.height() > TitleWidgetProvider.MINIMUM_HEIGHT)
        {
        	textPaint.setTextSize(textPaint.getTextSize() - 1);

            textPaint.getTextBounds(testString, 0, testString.length(), bounds);
        } */
		
		remoteViews.setTextViewText(R.id.widget_title_title_text, title);

		Intent tapIntent = new Intent(WidgetIntentService.WIDGET_ACTION);
		tapIntent.putExtras(intent);
		tapIntent.putExtra("widget_action", "tap");

		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, widgetId, tapIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_title_layout, pendingIntent);
		
		widgets.updateAppWidget(widgetId, remoteViews);
	}
}
