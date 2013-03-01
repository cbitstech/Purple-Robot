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
				RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.layout_four_widget);
				imageUri = Uri.parse(image);
				rv.setImageViewBitmap(R.id.widget_four_one, PurpleWidgetProvider.bitmapForUri(context, imageUri));
				widgets.updateAppWidget(widgetId, rv);
			}
			
			if (imageTwo != null && "".equals(imageTwo.trim()) == false)
			{
				RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.layout_four_widget);
				imageTwoUri = Uri.parse(imageTwo);
				rv.setImageViewBitmap(R.id.widget_four_two, PurpleWidgetProvider.bitmapForUri(context, imageTwoUri));
				widgets.updateAppWidget(widgetId, rv);
			}
			
			if (imageThree != null && "".equals(imageThree.trim()) == false)
			{
				RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.layout_four_widget);
				imageThreeUri = Uri.parse(imageThree);
				rv.setImageViewBitmap(R.id.widget_four_three, PurpleWidgetProvider.bitmapForUri(context, imageThreeUri));
				widgets.updateAppWidget(widgetId, rv);
			}
			
			if (imageFour != null && "".equals(imageFour.trim()) == false)
			{
				RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.layout_four_widget);
				imageFourUri = Uri.parse(imageFour);
				rv.setImageViewBitmap(R.id.widget_four_four, PurpleWidgetProvider.bitmapForUri(context, imageFourUri));
				widgets.updateAppWidget(widgetId, rv);
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

		Intent tapIntent = new Intent(WidgetIntentService.WIDGET_ACTION);
		tapIntent.putExtra("widget_action", "tap_one");

		if (intent.hasExtra("action_one"))
			tapIntent.putExtra("action_one", intent.getStringExtra("action_one"));

		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (widgetId * 100) + 1, tapIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_four_one, pendingIntent);
		
		tapIntent = new Intent(WidgetIntentService.WIDGET_ACTION);
		tapIntent.putExtra("widget_action", "tap_two");

		if (intent.hasExtra("action_two"))
			tapIntent.putExtra("action_two", intent.getStringExtra("action_two"));

		pendingIntent = PendingIntent.getBroadcast(context, (widgetId * 100) + 2, tapIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_four_two, pendingIntent);

		tapIntent = new Intent(WidgetIntentService.WIDGET_ACTION);
		tapIntent.putExtra("widget_action", "tap_three");
	
		if (intent.hasExtra("action_three"))
			tapIntent.putExtra("action_three", intent.getStringExtra("action_three"));
		
		pendingIntent = PendingIntent.getBroadcast(context, (widgetId * 100) + 3, tapIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_four_three, pendingIntent);

		tapIntent = new Intent(WidgetIntentService.WIDGET_ACTION);
		tapIntent.putExtra("widget_action", "tap_four");

		if (intent.hasExtra("action_four"))
			tapIntent.putExtra("action_four", intent.getStringExtra("action_four"));

		pendingIntent = PendingIntent.getBroadcast(context, (widgetId * 100) + 4, tapIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_four_four, pendingIntent);
		
		widgets.updateAppWidget(widgetId, remoteViews);
	}
}
