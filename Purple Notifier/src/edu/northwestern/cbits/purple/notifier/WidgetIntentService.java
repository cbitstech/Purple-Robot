package edu.northwestern.cbits.purple.notifier;

import java.util.ArrayList;
import java.util.Map;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetIntentService extends IntentService 
{
	public final static String UPDATE_WIDGET = "edu.northwestern.cbits.purple.UPDATE_WIDGET";
	public final static String UPDATE_WIDGETS = "edu.northwestern.cbits.purple.UPDATE_WIDGETS";

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
		Log.e("PN-SERV", "RUNNING INTENT: " + intent.getAction());
		
		if (UPDATE_WIDGETS.equals(intent.getAction()))
		{
			Log.e("PN-SERV", "UPDATING MULTIPLE WIDGETS");

			String identifier = intent.getStringExtra("identifier");

			Log.e("PN-SERV", "USING " + identifier);

			int[] widgetIds = this.getWidgetIds(identifier);
			
			Log.e("PN-SERV", "WIDGET COUNT " + widgetIds.length);
			
			for (int widgetId : widgetIds)
			{
				this.refreshWidget(widgetId, intent.getExtras(), intent);
			}
		}
		else if (UPDATE_WIDGET.equals(intent.getAction()))
		{
			String widget = intent.getStringExtra(WidgetIntentService.WIDGET);
			
			int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, Integer.MAX_VALUE);
			String identifier = intent.getStringExtra("identifier");
			
			this.registerIdentifier(identifier, widgetId);
			
			int[] widgetIds = this.getWidgetIds(identifier);
			
			Log.e("PN-SERV", "WIDGET ID: " + widgetId);
			
			this.registerType(widgetId, widget);

			for (int id : widgetIds)
			{
				this.refreshWidget(id, intent.getExtras(), intent);
			}
		}
	}

	private void refreshWidget(int widgetId, Bundle extras, Intent intent) 
	{
		String widget = this.fetchType(widgetId);

		Log.e("PN-SRV", "UPDATE WIDGET " + widgetId + " -- " + widget);
		
		if (BasicWidgetProvider.NAME.equals(widget))
		{
			String title = extras.getString("title");
			String message = extras.getString("message");
			
			Log.e("P-NSRV", "BASIC WIDGET");
			RemoteViews rv = new RemoteViews(this.getPackageName(), R.layout.layout_basic_widget);

			rv.setTextViewText(R.id.widget_basic_title_text, title);
			rv.setTextViewText(R.id.widget_basic_message_text, message);

			PendingIntent pendingIntent = this.pendingIntentFromIntent(intent);
			
			if (pendingIntent != null)
				rv.setOnClickPendingIntent(R.id.widget_basic_layout, pendingIntent);
			
			AppWidgetManager.getInstance(this).updateAppWidget(widgetId, rv);
		}
		else if (TextWidgetProvider.NAME.equals(widget))
		{
			String title = extras.getString("title");
			String message = extras.getString("message");

			Log.e("P-NSRV", "TEXT WIDGET");
			RemoteViews rv = new RemoteViews(this.getPackageName(), R.layout.layout_text_widget);

			rv.setTextViewText(R.id.widget_text_title_text, title);
			rv.setTextViewText(R.id.widget_text_message_text, message);

			PendingIntent pendingIntent = this.pendingIntentFromIntent(intent);
			
			if (pendingIntent != null)
				rv.setOnClickPendingIntent(R.id.widget_text_layout, pendingIntent);
			
			AppWidgetManager.getInstance(this).updateAppWidget(widgetId, rv);
		}
	}

	private int[] getWidgetIds(String identifier) 
	{
		String key = "widget_identifiers_" +  identifier;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		String idList = prefs.getString(key, "");
		
		ArrayList<String> identifiers = new ArrayList<String>();
		
		String[] tokens = idList.split(";");
		
		for (int i = 0; i < tokens.length; i++)
		{
			String id = tokens[i].trim();
			
			if (id.length() > 0)
				identifiers.add(id);
		}
		
		int[] intIds = new int[identifiers.size()];
		
		for (int i = 0; i < identifiers.size(); i++)
		{
			intIds[i] = Integer.parseInt(identifiers.get(i));
		}
		
		return intIds;
	}

	private void registerIdentifier(String identifier, int widgetId) 
	{
		this.unregisterIdentifier(widgetId);
		
		String key = "widget_identifiers_" +  identifier;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		String idList = prefs.getString(key, "");
		
		ArrayList<String> identifiers = new ArrayList<String>();
		
		String[] tokens = idList.split(";");
		
		for (int i = 0; i < tokens.length; i++)
		{
			String id = tokens[i].trim();
			
			if (id.length() > 0)
				identifiers.add(id);
		}
		
		String stringId = "" + widgetId;
		
		if (identifiers.contains(stringId) == false)
			identifiers.add(stringId);
		
		StringBuilder sb = new StringBuilder();
		
		for (String id : identifiers)
		{
			if (sb.length() > 0)
				sb.append(";");
			
			sb.append(id);
		}
		
		Editor e = prefs.edit();
		e.putString(key, sb.toString());
		e.commit();
	}

	private void unregisterIdentifier(int widgetId) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		Map<String, ?> values = prefs.getAll();
		
		for (String key : values.keySet())
		{
			if (key.startsWith("widget_identifiers_"))
			{
				String identifier = key.replaceAll("widget_identifiers_", "");
				
				this.unregisterIdentifier(identifier, widgetId);
			}
		}
	}
	
	private void registerType(int widgetId, String type)
	{
		String key = "widget_type_" + widgetId;
			
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		Editor e = prefs.edit();
		e.putString(key,  type);
		e.commit();
	}
	
	private String fetchType(int widgetId)
	{
		String key = "widget_type_" + widgetId;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		return prefs.getString(key, "unknown");
	}

	private void unregisterIdentifier(String identifier, int widgetId) 
	{
		String key = "widget_identifiers_" +  identifier;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		int[] widgetIds = this.getWidgetIds(identifier);
		
		ArrayList<Integer> savedIds = new ArrayList<Integer>();
		
		for (int savedId : widgetIds)
		{
			if (savedId != widgetId)
				savedIds.add(Integer.valueOf(savedId));
		}
		
		if (savedIds.size() == widgetIds.length)
			return;

		StringBuilder sb = new StringBuilder();
		
		for (Integer id : savedIds)
		{
			if (sb.length() > 0)
				sb.append(";");
			
			sb.append(id);
		}
		
		Editor e = prefs.edit();
		e.putString(key, sb.toString());
		e.commit();
	}

	private PendingIntent pendingIntentFromIntent(Intent intent) 
	{
		if (intent.hasExtra("package_name"))
			return PendingIntent.getActivity(this, 0, this.getPackageManager().getLaunchIntentForPackage(intent.getStringExtra("package_name")), 0);
		
		return null;
	}
}
