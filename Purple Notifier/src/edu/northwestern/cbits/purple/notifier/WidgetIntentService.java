package edu.northwestern.cbits.purple.notifier;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class WidgetIntentService extends IntentService 
{
	public final static String WIDGET_ACTION = "edu.northwestern.cbits.purple.WIDGET_ACTION";
	public final static String UPDATE_WIDGET = "edu.northwestern.cbits.purple.UPDATE_WIDGET";
	public final static String UPDATE_WIDGETS = "edu.northwestern.cbits.purple.UPDATE_WIDGETS";
	public static final String ACTION_BOOT = "edu.northwestern.cbits.purple.ACTION_BOOT";

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
				this.refreshWidget(widgetId, intent.getExtras(), intent);
			}

			this.saveWidgetState(identifier, intent.getExtras());
		}
		else if (UPDATE_WIDGET.equals(intent.getAction()))
		{
			String widget = intent.getStringExtra(WidgetIntentService.WIDGET);
			
			int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, Integer.MAX_VALUE);
			String identifier = intent.getStringExtra("identifier");
			
			this.saveWidgetState(identifier, intent.getExtras());
			
			this.registerIdentifier(identifier, widgetId);
			
			int[] widgetIds = this.getWidgetIds(identifier);
			
			this.registerType(widgetId, widget);

			for (int id : widgetIds)
			{
				this.refreshWidget(id, intent.getExtras(), intent);
			}
		}
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

	private void refreshWidget(int widgetId, Bundle extras, Intent intent) 
	{
		String widget = this.fetchType(widgetId);

		if (BasicWidgetProvider.NAME.equals(widget))
		{
			String title = extras.getString("title");
			String message = extras.getString("message");
			String image = extras.getString("image");
			
			RemoteViews rv = new RemoteViews(this.getPackageName(), R.layout.layout_basic_widget);

			Uri imageUri = null;
			
			try
			{
				imageUri = Uri.parse(image);
			}
			catch (NullPointerException e)
			{
				
			}
			
			if (imageUri != null)
			{
				try
				{
					rv.setImageViewBitmap(R.id.widget_basic_image, this.bitmapForUri(imageUri));
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}

			rv.setTextViewText(R.id.widget_basic_title_text, title);
			rv.setTextViewText(R.id.widget_basic_message_text, message);
			
			Intent tapIntent = new Intent(WidgetIntentService.WIDGET_ACTION);
			tapIntent.putExtras(intent);
			tapIntent.putExtra("widget_action", "tap");

			PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			rv.setOnClickPendingIntent(R.id.widget_basic_layout, pendingIntent);
			
			AppWidgetManager.getInstance(this).updateAppWidget(widgetId, rv);
		}
		else if (TextWidgetProvider.NAME.equals(widget))
		{
			String title = extras.getString("title");
			String message = extras.getString("message");

			RemoteViews rv = new RemoteViews(this.getPackageName(), R.layout.layout_text_widget);

			rv.setTextViewText(R.id.widget_text_title_text, title);
			rv.setTextViewText(R.id.widget_text_message_text, message);

			Intent tapIntent = new Intent(WidgetIntentService.WIDGET_ACTION);
			tapIntent.putExtras(intent);
			tapIntent.putExtra("widget_action", "tap");

			PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			rv.setOnClickPendingIntent(R.id.widget_text_layout, pendingIntent);
			
			AppWidgetManager.getInstance(this).updateAppWidget(widgetId, rv);
		}
	}

	// http://stackoverflow.com/questions/3879992/get-bitmap-from-an-uri-android

	private static int getPowerOfTwoForSampleRatio(double ratio)
    {
        int k = Integer.highestOneBit((int)Math.floor(ratio));

        if (k == 0)
        	return 1;
        
        return k;
    }
	
	private InputStream inputStreamForUri(Uri imageUri) throws MalformedURLException, IOException
	{
		// TODO: Insert caching layer../
		
		InputStream input = null;

		if ("http".equals(imageUri.getScheme().toLowerCase()) || 
			"https".equals(imageUri.getScheme().toLowerCase()))
		{
			HttpURLConnection conn = (HttpURLConnection) (new URL(imageUri.toString())).openConnection();
			
			input = conn.getInputStream();
		}
		else
			input = this.getContentResolver().openInputStream(imageUri);
		
		return input;
	}

	private Bitmap bitmapForUri(Uri imageUri) throws IOException 
	{
		InputStream input = this.inputStreamForUri(imageUri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();

        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1))
            return null;

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = (originalSize > 144) ? (originalSize / 144) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();

        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither=true; 
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional

		input = this.inputStreamForUri(imageUri);

		Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();

        return bitmap;
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
}
