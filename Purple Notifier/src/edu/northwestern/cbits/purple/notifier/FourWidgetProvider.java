package edu.northwestern.cbits.purple.notifier;

import java.io.IOException;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

public class FourWidgetProvider extends PurpleWidgetProvider
{
	public static final String NAME = "FOUR_WIDGET_UPDATE";
	public static final String WIDGET_LAUNCH = "config_widget_four_launch";

	public static void setupWidget(Context context, int widgetId, Intent intent) 
	{
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_four_widget);

		Bundle extras = intent.getExtras();

		AppWidgetManager widgets = AppWidgetManager.getInstance(context);

		String image = extras.getString("image");
		String imageTwo = extras.getString("image_two");
		String imageThree = extras.getString("image_three");
		String imageFour = extras.getString("image_four");
		
		Uri imageUri = null;
		Uri imageTwoUri = null;
		Uri imageThreeUri = null;
		Uri imageFourUri = null;
		
		try 
		{
			if (image != null && "".equals(image.trim()) == false)
			{
				imageUri = Uri.parse(image);
				remoteViews.setImageViewBitmap(R.id.widget_four_one, PurpleWidgetProvider.bitmapForUri(context, imageUri));
			}
			
			if (imageTwo != null && "".equals(imageTwo.trim()) == false)
			{
				imageTwoUri = Uri.parse(imageTwo);
				remoteViews.setImageViewBitmap(R.id.widget_four_two, PurpleWidgetProvider.bitmapForUri(context, imageTwoUri));
			}
			
			if (imageThree != null && "".equals(imageThree.trim()) == false)
			{
				imageThreeUri = Uri.parse(imageThree);
				remoteViews.setImageViewBitmap(R.id.widget_four_three, PurpleWidgetProvider.bitmapForUri(context, imageThreeUri));
			}
			
			if (imageFour != null && "".equals(imageFour.trim()) == false)
			{
				imageFourUri = Uri.parse(imageFour);
				remoteViews.setImageViewBitmap(R.id.widget_four_four, PurpleWidgetProvider.bitmapForUri(context, imageFourUri));
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

		Intent tapIntent = new Intent(WidgetIntentService.WIDGET_ACTION);
		tapIntent.putExtras(intent);
		tapIntent.putExtra("widget_action", "tap_one");
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (widgetId * 100) + 1, tapIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_four_one, pendingIntent);
		
		tapIntent = new Intent(WidgetIntentService.WIDGET_ACTION);
		tapIntent.putExtras(intent);
		tapIntent.putExtra("widget_action", "tap_two");
		pendingIntent = PendingIntent.getBroadcast(context, (widgetId * 100) + 2, tapIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_four_two, pendingIntent);

		tapIntent = new Intent(WidgetIntentService.WIDGET_ACTION);
		tapIntent.putExtras(intent);
		tapIntent.putExtra("widget_action", "tap_three");
		pendingIntent = PendingIntent.getBroadcast(context, (widgetId * 100) + 3, tapIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_four_three, pendingIntent);

		tapIntent = new Intent(WidgetIntentService.WIDGET_ACTION);
		tapIntent.putExtras(intent);
		tapIntent.putExtra("widget_action", "tap_four");
		pendingIntent = PendingIntent.getBroadcast(context, (widgetId * 100) + 4, tapIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_four_four, pendingIntent);
		
		widgets.updateAppWidget(widgetId, remoteViews);
	}
}
