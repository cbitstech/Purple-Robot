package edu.northwestern.cbits.purple.notifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.OrientationEventListener;

public class WidgetIntentService extends IntentService 
{
	public final static String WIDGET_ACTION = "edu.northwestern.cbits.purple.WIDGET_ACTION";
	public final static String UPDATE_WIDGET = "edu.northwestern.cbits.purple.UPDATE_WIDGET";
	public final static String UPDATE_WIDGETS = "edu.northwestern.cbits.purple.UPDATE_WIDGETS";
	public static final String ACTION_BOOT = "edu.northwestern.cbits.purple.ACTION_BOOT";

	public final static String WIDGET = "WIDGET";
	public final static String LAUNCH_INTENT = "LAUNCH_INTENT";

	private static OrientationEventListener _orientation = null;
	private int _lastOrientation = 0;
	
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
		Log.e("PN", "RECVED 22 INTENT: " + intent);
		
		final WidgetIntentService me = this;
		
		Log.e("PN", "FOO 1");
		
		if (WidgetIntentService._orientation == null)
		{
			WidgetIntentService._orientation = new OrientationEventListener(this)
			{
				public void onOrientationChanged(int orientation) 
				{
					if (Math.abs(orientation - me._lastOrientation) > 45)
					{
						Intent newIntent = new Intent(WidgetIntentService.UPDATE_WIDGETS);
						me.startService(newIntent);
						
						me._lastOrientation = orientation;
					}
				}
			};
			
//			WidgetIntentService._orientation.enable();
		}

		Log.e("PN", "FOO 2");

		if (intent.hasExtra("identifier"))
		{
			String identifier = intent.getStringExtra("identifier");

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			
			String key = identifier + "_saved_state";
			
			String idString = prefs.getString(key, "{}");
			
			try 
			{
				JSONObject json = new JSONObject(idString);
				
				for (String bundleKey : intent.getExtras().keySet())
				{
					if (AppWidgetManager.EXTRA_APPWIDGET_ID.equals(bundleKey) == false)
					{
						String value = intent.getStringExtra(bundleKey);

						json.put(bundleKey, value);
					}
				}
				
				@SuppressWarnings("unchecked")
				Iterator<String> iter = json.keys();
				
				while (iter.hasNext())
				{
					String jsonKey = iter.next();
					
					String value = json.getString(jsonKey);

					if (intent.hasExtra(jsonKey) == false)
						intent.putExtra(jsonKey, value);
				}

				Editor e = prefs.edit();
				e.putString(key, json.toString());
				e.commit();
			} 
			catch (JSONException e) 
			{
				e.printStackTrace();
			}
		}

		Log.e("PN", "FOO 3");

		if (ACTION_BOOT.equals(intent.getAction()))
		{
			String[] identifiers = IdentifiersManager.fetchIdentifiers(this);
			
			for (String identifier : identifiers)
			{
				Bundle b = this.fetchWidgetState(identifier);
				
				Intent update = new Intent(WidgetIntentService.UPDATE_WIDGETS);
				update.putExtras(b);
				
				this.onHandleIntent(update);
			}
		}
		else if (UPDATE_WIDGETS.equals(intent.getAction()))
		{
			String identifier = intent.getStringExtra("identifier");

			int[] widgetIds = this.getWidgetIds(identifier);
			
			for (int widgetId : widgetIds)
			{
				Log.e("PR", "REFRESHING WIDGET 1 " + widgetId + " FOR " + identifier);
				
				this.refreshWidget(widgetId, intent);
			}

			this.saveWidgetState(identifier, intent.getExtras());
		}
		else if (UPDATE_WIDGET.equals(intent.getAction()))
		{
			String identifier = intent.getStringExtra("identifier");

			int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, Integer.MAX_VALUE);

			this.registerIdentifier(identifier, widgetId);

			this.saveWidgetState(identifier, intent.getExtras());
			
			int[] widgetIds = this.getWidgetIds(identifier);
			
			for (int id : widgetIds)
			{
				Log.e("PR", "REFRESHING WIDGET 2 " + widgetId + " FOR " + identifier);

				this.refreshWidget(id, intent);
			}
		}

		Log.e("PN", "FOO 4");
	}
	
	@SuppressWarnings("unchecked")
	private Bundle fetchWidgetState(String identifier)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String jsonString = prefs.getString("widget_state_" + identifier, "{}");

		Bundle bundle = new Bundle();
		
		try 
		{
			JSONObject json = new JSONObject(jsonString);
			
			Iterator<String> keys = json.keys();
			
			while (keys.hasNext())
			{
				String key = keys.next();
				
				bundle.putString(key, json.getString(key));
			}
		} 
		catch (JSONException e) 
		{
			e.printStackTrace();
		}
		
		return bundle;
		
	}

	private void saveWidgetState(String identifier, Bundle extras) 
	{
		JSONObject json = new JSONObject();
		
		for (String key : extras.keySet())
		{
			try 
			{
				json.put(key, extras.get(key).toString());
			} 
			catch (JSONException e) 
			{
				e.printStackTrace();
			}
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			Editor e = prefs.edit();
			e.putString("widget_state_" + identifier, json.toString());
			e.commit();
		}
	}

	private void refreshWidget(int widgetId, Intent intent) 
	{
		String widget = this.fetchType(widgetId);

		if (widget == null)
		{
			this.unregisterIdentifier(widgetId);
			return;
		}
		
		if (BasicWidgetProvider.NAME.equals(widget))
			BasicWidgetProvider.setupWidget(this, widgetId, intent);
		else if (TextWidgetProvider.NAME.equals(widget))
			TextWidgetProvider.setupWidget(this, widgetId, intent);
		else if (TitleWidgetProvider.NAME.equals(widget))
			TitleWidgetProvider.setupWidget(this, widgetId, intent);
		else if (ImageWidgetProvider.NAME.equals(widget))
			ImageWidgetProvider.setupWidget(this, widgetId, intent);
		else if (FourWidgetProvider.NAME.equals(widget))
			FourWidgetProvider.setupWidget(this, widgetId, intent);
		else if (FiveWidgetProvider.NAME.equals(widget))
			FiveWidgetProvider.setupWidget(this, widgetId, intent);
		else if (BadgeWidgetProvider.NAME.equals(widget))
			BadgeWidgetProvider.setupWidget(this, widgetId, intent);
		else if (SmallBadgeWidgetProvider.NAME.equals(widget))
			SmallBadgeWidgetProvider.setupWidget(this, widgetId, intent);
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
	
	private String fetchType(int widgetId)
	{
		ArrayList<ComponentName> names = new ArrayList<ComponentName>();
		
		names.add(new ComponentName(this, BasicWidgetProvider.class));
		names.add(new ComponentName(this, TextWidgetProvider.class));
		names.add(new ComponentName(this, ImageWidgetProvider.class));
		names.add(new ComponentName(this, TitleWidgetProvider.class));
		names.add(new ComponentName(this, FourWidgetProvider.class));
		names.add(new ComponentName(this, FiveWidgetProvider.class));
		names.add(new ComponentName(this, BadgeWidgetProvider.class));
		names.add(new ComponentName(this, SmallBadgeWidgetProvider.class));

		AppWidgetManager widgets = AppWidgetManager.getInstance(this);
		
		for (ComponentName name : names)
		{
			int[] widgetIds = widgets.getAppWidgetIds(name);
			
			for (int arrayId : widgetIds)
			{
				if (widgetId == arrayId)
				{
					if (".BasicWidgetProvider".equals(name.getShortClassName()))
						return BasicWidgetProvider.NAME;
					else if (".TextWidgetProvider".equals(name.getShortClassName()))
						return TextWidgetProvider.NAME;
					else if (".ImageWidgetProvider".equals(name.getShortClassName()))
						return ImageWidgetProvider.NAME;
					else if (".TitleWidgetProvider".equals(name.getShortClassName()))
						return TitleWidgetProvider.NAME;
					else if (".FourWidgetProvider".equals(name.getShortClassName()))
						return FourWidgetProvider.NAME;
					else if (".FiveWidgetProvider".equals(name.getShortClassName()))
						return FiveWidgetProvider.NAME;
					else if (".BadgeWidgetProvider".equals(name.getShortClassName()))
						return BadgeWidgetProvider.NAME;
					else if (".SmallBadgeWidgetProvider".equals(name.getShortClassName()))
						return SmallBadgeWidgetProvider.NAME;
				}
			}
		}
		
		return null;
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
}
