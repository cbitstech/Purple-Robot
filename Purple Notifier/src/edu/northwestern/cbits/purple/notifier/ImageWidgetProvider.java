package edu.northwestern.cbits.purple.notifier;

import java.io.IOException;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

public class ImageWidgetProvider extends PurpleWidgetProvider
{
	public static final String NAME = "IMAGE_WIDGET_UPDATE";
	public static final String WIDGET_LAUNCH = "config_widget_image_launch";

	public static void setupWidget(Context context, int widgetId, Intent intent) 
	{
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_image_widget);

		Bundle extras = intent.getExtras();
		AppWidgetManager widgets = AppWidgetManager.getInstance(context);

		String image = extras.getString("image");

		Uri imageUri = null;
		
		try
		{
			if (image.trim().length() > 0)
				imageUri = Uri.parse(image);
		}
		catch (NullPointerException e)
		{
			
		}
		
		if (imageUri != null)
		{
			try
			{
				remoteViews.setImageViewBitmap(R.id.widget_image_image, PurpleWidgetProvider.bitmapForUri(context, imageUri));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		Intent tapIntent = new Intent(WidgetIntentService.WIDGET_ACTION);
		tapIntent.putExtras(intent);
		tapIntent.putExtra("widget_action", "tap");

		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, widgetId, tapIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_image_layout, pendingIntent);
		try
		{
			widgets.updateAppWidget(widgetId, remoteViews);
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
		}
	}
}
