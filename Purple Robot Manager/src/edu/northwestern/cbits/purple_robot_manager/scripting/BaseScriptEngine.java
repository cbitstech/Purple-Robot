package edu.northwestern.cbits.purple_robot_manager.scripting;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.DialogActivity;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.ScheduleManager;
import edu.northwestern.cbits.purple_robot_manager.SettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.LabelActivity;
import edu.northwestern.cbits.purple_robot_manager.config.LegacyJSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;
import edu.northwestern.cbits.purple_robot_manager.widget.PurpleRobotAppWideWidgetProvider;
import edu.northwestern.cbits.purple_robot_manager.widget.PurpleRobotAppWidgetProvider;

public abstract class BaseScriptEngine 
{
	protected static String SCRIPT_ENGINE_PERSISTENCE_PREFIX = "purple_robot_script_persist_prefix_";
	protected static String SCRIPT_ENGINE_NAMESPACES = "purple_robot_script_namespaces";

	private static final int NOTIFICATION_ID = 1;
	private static String LOG_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

	protected Context _context = null;
	private static Map<String, String> packageMap = null;

	protected abstract String language();

	public BaseScriptEngine(Context context)
	{
		this._context = context;
	}
	
	public Date dateFromTimestamp(long epoch)
	{
		return new Date(epoch);
	}

	public String formatDate(Date date)
	{
		return ScheduleManager.formatString(date);
	}

	public Date parseDate(String dateString)
	{
		return ScheduleManager.parseString(dateString);
	}

	public Date now()
	{
		return ScheduleManager.clearMillis(new Date());
	}

	@SuppressLint("SimpleDateFormat")
	public void log(Object message)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(BaseScriptEngine.LOG_DATE_FORMAT);
		
		Log.e("PRM." + this.language(), sdf.format(new Date()) + ": " + message.toString());

		HashMap <String, Object> payload = new HashMap<String, Object>();
		payload.put("message", message);
		LogManager.getInstance(this._context).log("script_log_message", payload);
	}
	
	public void playDefaultTone()
	{
		LogManager.getInstance(this._context).log("default_tone_played", null);

		this.playTone(null);
	}
	
	public void playTone(String tone)
	{
		Intent intent = new Intent(ManagerService.RINGTONE_INTENT);

		if (tone != null)
			intent.putExtra(ManagerService.RINGTONE_NAME, tone);

		HashMap <String, Object> payload = new HashMap<String, Object>();
		payload.put("tone", tone);
		LogManager.getInstance(this._context).log("tone_played", payload);

		this._context.startService(intent);
	}

	public boolean persistEncryptedString(String key, String value)
	{
		key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;

		return EncryptionManager.getInstance().persistEncryptedString(this._context, key, value);
	}

	public boolean persistEncryptedString(String namespace, String key, String value)
	{
		key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;
		key = namespace + " - " + key;

		return EncryptionManager.getInstance().persistEncryptedString(this._context, key, value);
	}

	public String fetchEncryptedString(String key)
	{
		key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;
		
		return EncryptionManager.getInstance().fetchEncryptedString(this._context, key);
	}

	public String fetchEncryptedString(String namespace, String key)
	{
		key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;
		key = namespace + " - " + key;

		return EncryptionManager.getInstance().fetchEncryptedString(this._context, key);
	}

	public void vibrate(String pattern)
	{
		Intent intent = new Intent(ManagerService.HAPTIC_PATTERN_INTENT);
		intent.putExtra(ManagerService.HAPTIC_PATTERN_NAME, pattern);

		HashMap <String, Object> payload = new HashMap<String, Object>();
		payload.put("pattern", pattern);
		LogManager.getInstance(this._context).log("vibrate_device", payload);

		this._context.startService(intent);
	}

	public String readUrl(String urlString)
	{
		try 
		{
			URL url = new URL(urlString);

	        URLConnection connection = url.openConnection();

	        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

	        StringBuilder response = new StringBuilder();

	        String inputLine;

	        while ((inputLine = in.readLine()) != null) 
	            response.append(inputLine);

	        in.close();

	        return response.toString();
		}
		catch (Exception e) 
		{
			LogManager.getInstance(this._context).logException(e);
		} 
			
		return null;
	}
	
	public boolean emitToast(final String message, final boolean longDuration)
	{
		HashMap <String, Object> payload = new HashMap<String, Object>();
		payload.put("has_activity", (this._context instanceof Activity));
		payload.put("message", message);
		LogManager.getInstance(this._context).log("toast_message", payload);

		if (this._context instanceof Activity)
		{
			final Activity activity = (Activity) this._context;

			activity.runOnUiThread(new Runnable()
			{
				public void run()
				{
					if (longDuration)
						Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
					else
						Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
				}
			});

			return true;
		}

		return false;
	}

	public boolean launchUrl(String urlString)
	{
		try
		{
			Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
			launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			this._context.startActivity(launchIntent);

			HashMap <String, Object> payload = new HashMap<String, Object>();
			payload.put("url", urlString);
			LogManager.getInstance(this._context).log("launch_url", payload);

			return true;
		}
		catch (Exception e)
		{
			LogManager.getInstance(this._context).logException(e);
		}

		return false;
	}

	@SuppressLint("DefaultLocale")
	public String packageForApplicationName(String applicationName)
	{
		if (applicationName == null)
			return null;

		if (BaseScriptEngine.packageMap == null)
		{
			BaseScriptEngine.packageMap = new HashMap<String, String>();

			String[] keyArray = this._context.getResources().getStringArray(R.array.app_package_keys);
			String[] valueArray = this._context.getResources().getStringArray(R.array.app_package_values);

			if (keyArray.length == valueArray.length)
			{
				for (int i = 0; i < keyArray.length; i++)
				{
					BaseScriptEngine.packageMap.put(keyArray[i].toLowerCase(), valueArray[i]);
				}
			}
		}

		String packageName = BaseScriptEngine.packageMap.get(applicationName.toLowerCase());

		if (packageName == null)
			packageName = applicationName; // Allows us to launch by package name as well.

		PackageManager pkgManager = this._context.getPackageManager();

		Intent launchIntent = pkgManager.getLaunchIntentForPackage(packageName);

		if (launchIntent == null) // No matching package found on system...

			packageName = null;

		return packageName;
	}
	
	public String version()
	{
		try
		{
			PackageInfo info = this._context.getPackageManager().getPackageInfo(this._context.getPackageName(), 0);

			return info.versionName;
		}
		catch (NameNotFoundException e)
		{
			LogManager.getInstance(this._context).logException(e);
		}

		return null;
	}

	public int versionCode()
	{
		try
		{
			PackageInfo info = this._context.getPackageManager().getPackageInfo(this._context.getPackageName(), 0);

			return info.versionCode;
		}
		catch (NameNotFoundException e)
		{
			LogManager.getInstance(this._context).logException(e);
		}

		return -1;
	}

	public boolean persistString(String key, String value)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		Editor editor = prefs.edit();

		key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;

		if (value != null)
			editor.putString(key,  value.toString());
		else
			editor.remove(key);

		return editor.commit();
	}
	
	public void addNamespace(String namespace)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		try 
		{
			JSONArray namespaces = new JSONArray(prefs.getString(BaseScriptEngine.SCRIPT_ENGINE_NAMESPACES, "[]"));
			
			for (int i = 0; i < namespaces.length(); i++)
			{
				String item = namespaces.getString(i);
				
				if (item.equals(namespace))
					return;
			}
			
			namespaces.put(namespace);
			
			Editor e = prefs.edit();
			e.putString(BaseScriptEngine.SCRIPT_ENGINE_NAMESPACES, namespaces.toString());
			e.commit();
		} 
		catch (JSONException e) 
		{
			LogManager.getInstance(this._context).logException(e);
		}
	}

	public boolean persistString(String namespace, String key, String value)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		Editor editor = prefs.edit();

		this.addNamespace(namespace);
		
		key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;
		key = namespace + " - " + key;

		if (value != null)
			editor.putString(key,  value.toString());
		else
			editor.remove(key);

		return editor.commit();
	}

	public String fetchString(String namespace, String key)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;
		key = namespace + " - " + key;
		
		return prefs.getString(key, null);
	}

	public void fetchLabel(String context, String key)
	{
		Intent labelIntent = new Intent(this._context, LabelActivity.class);
		labelIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		if (context == null || context.trim().length() < 1)
			context = this._context.getString(R.string.label_unknown_context);
		
		labelIntent.putExtra(LabelActivity.LABEL_CONTEXT, context);
		
		if (key != null && key.trim().length() > 1)
			labelIntent.putExtra(LabelActivity.LABEL_KEY, key);
		
		this._context.getApplicationContext().startActivity(labelIntent);
	}
	
	public String fetchString(String key)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

		key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;

		return prefs.getString(key, null);
	}

	public void resetTrigger(String triggerId)
	{
		for (Trigger trigger : TriggerManager.getInstance(this._context).triggersForId(triggerId))
		{
			trigger.reset(this._context);
		}
	}

	public void enableTrigger(String triggerId)
	{
		for (Trigger trigger : TriggerManager.getInstance(this._context).triggersForId(triggerId))
		{
			trigger.setEnabled(this._context, true);
		}
	}

	public void disableTrigger(String triggerId)
	{
		for (Trigger trigger : TriggerManager.getInstance(this._context).triggersForId(triggerId))
		{
			trigger.setEnabled(this._context, false);
		}
	}
	
	public void disableAutoConfigUpdates(String triggerId)
	{
		// TODO
	}

	public void enableAutoConfigUpdates(String triggerId)
	{
		// TODO
	}

	public void enableProbes()
	{
		ProbeManager.enableProbes(this._context);
	}

	public void disableProbes()
	{
		ProbeManager.disableProbes(this._context);
	}
	
	public boolean probesState()
	{
		return ProbeManager.probesState(this._context);
	}
	
	protected void transmitData(Bundle data)
	{
		UUID uuid = UUID.randomUUID();
		data.putString("GUID", uuid.toString());

		LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this._context);
		Intent intent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
		intent.putExtras(data);

		localManager.sendBroadcast(intent);
	}
	
	public void disableProbe(String probeName)
	{
		ProbeManager.disableProbe(this._context, probeName);
	}
	
	public void updateConfigUrl(String newUrl)
	{
		if (newUrl != null && newUrl.trim().length() == 0)
			newUrl = null;
		
		EncryptionManager.getInstance().setConfigUri(this._context, Uri.parse(newUrl));
		
		LegacyJSONConfigFile.update(this._context);
	}
	
	public void setPassword(String password)
	{
		if (password == null || password.trim().length() == 0)
			this.clearPassword();
		else
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
			
			Editor e = prefs.edit();
			e.putString("config_password", password);
			e.commit();
		}
	}
	
	public void clearPassword()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		Editor e = prefs.edit();
		e.remove("config_password");
		e.commit();
	}

	public void enableBackgroundImage()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_show_background", true);
		e.commit();
	}

	public void disableBackgroundImage()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_show_background", false);
		e.commit();
	}
	
	private void refreshConfigUrl()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		Editor editor = prefs.edit();

		editor.putLong(LegacyJSONConfigFile.JSON_LAST_UPDATE, 0);
		editor.commit();
		
		LegacyJSONConfigFile.update(this._context);

		ProbeManager.nudgeProbes(this._context);
	}

	public void setUserId(String userId)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		Editor e = prefs.edit();
		e.putString("config_user_id", userId);
		
		e.commit();
		
		this.refreshConfigUrl();
	}

	public String fetchUserId()
	{
		return EncryptionManager.getInstance().getUserId(this._context);
	}

	public String fetchUserHash()
	{
		return EncryptionManager.getInstance().getUserHash(this._context);
	}

	public void restoreDefaultId()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		Editor e = prefs.edit();
		e.remove("config_user_id");
		
		e.commit();

		this.refreshConfigUrl();
	}

	public void enableUpdateChecks()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		Editor e = prefs.edit();
		e.putBoolean(SettingsActivity.CHECK_UPDATES_KEY, true);
		e.commit();
	}

	public void disableUpdateChecks()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		Editor e = prefs.edit();
		e.putBoolean(SettingsActivity.CHECK_UPDATES_KEY, false);
		e.commit();
	}
	
	public void enableProbe(String probeName)
	{
		ProbeManager.enableProbe(this._context, probeName);
	}

	protected boolean updateWidget(final String title, final String message, final String applicationName, final Map<String, Object> launchParams, final String script)
	{
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(this._context);

		ComponentName provider = new ComponentName(this._context.getPackageName(), PurpleRobotAppWidgetProvider.class.getName());

		int[] widgetIds = widgetManager.getAppWidgetIds(provider);

		ComponentName wideProvider = new ComponentName(this._context.getPackageName(), PurpleRobotAppWideWidgetProvider.class.getName());

		int[] wideWidgetIds = widgetManager.getAppWidgetIds(wideProvider);

		RemoteViews views = new RemoteViews(this._context.getPackageName(), R.layout.layout_widget);

		views.setCharSequence(R.id.widget_title_text, "setText", title);
		views.setCharSequence(R.id.widget_message_text, "setText", message);

		RemoteViews wideViews = new RemoteViews(this._context.getPackageName(), R.layout.layout_wide_widget);

		wideViews.setCharSequence(R.id.widget_wide_title_text, "setText", title);
		wideViews.setCharSequence(R.id.widget_wide_message_text, "setText", message);

		Intent intent = this.constructLaunchIntent(applicationName, launchParams, script);

		if (intent != null)
		{
			if (intent.getAction().equals(ManagerService.APPLICATION_LAUNCH_INTENT))
			{
				PendingIntent pi = PendingIntent.getService(this._context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

				views.setOnClickPendingIntent(R.id.widget_root_layout, pi);
				wideViews.setOnClickPendingIntent(R.id.widget_root_layout, pi);
			}
			else
			{
				PendingIntent pi = PendingIntent.getActivity(this._context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				
				views.setOnClickPendingIntent(R.id.widget_root_layout, pi);
				wideViews.setOnClickPendingIntent(R.id.widget_root_layout, pi);
			}
		}

		widgetManager.updateAppWidget(widgetIds, views);
		widgetManager.updateAppWidget(wideWidgetIds, wideViews);

		return true;
	}

	@SuppressLint("DefaultLocale")
	protected Intent constructLaunchIntent(String applicationName, Map<String, Object> launchParams, String script) 
	{
		if (applicationName == null)
			return null;

		String packageName = this.packageForApplicationName(applicationName);

		if (packageName != null)
		{
			Intent intent = new Intent(ManagerService.APPLICATION_LAUNCH_INTENT);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			intent.putExtra(ManagerService.APPLICATION_LAUNCH_INTENT_PACKAGE, packageName);

			if (script != null)
				intent.putExtra(ManagerService.APPLICATION_LAUNCH_INTENT_POSTSCRIPT, script);

			if (launchParams != null)
			{
				HashMap<String, String> launchMap = new HashMap<String, String>();

				for (Entry<String, Object> e : launchParams.entrySet())
				{
					launchMap.put(e.getKey(), e.getValue().toString());
				}

				JSONObject jsonMap = new JSONObject(launchMap);

				intent.putExtra(ManagerService.APPLICATION_LAUNCH_INTENT_PARAMETERS, jsonMap.toString());
			}

			return intent;
		}

		if (applicationName.toLowerCase().startsWith("http://") || applicationName.toLowerCase().startsWith("https://"))
		{
			Intent intent = new Intent(ManagerService.APPLICATION_LAUNCH_INTENT);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			intent.putExtra(ManagerService.APPLICATION_LAUNCH_INTENT_URL, applicationName);

			if (script != null)
				intent.putExtra(ManagerService.APPLICATION_LAUNCH_INTENT_POSTSCRIPT, script);

			if (launchParams != null)
			{
				HashMap<String, String> launchMap = new HashMap<String, String>();

				for (Entry<String, Object> e : launchParams.entrySet())
				{
					launchMap.put(e.getKey().toString(), e.getValue().toString());
				}

				JSONObject jsonMap = new JSONObject(launchMap);

				intent.putExtra(ManagerService.APPLICATION_LAUNCH_INTENT_PARAMETERS, jsonMap.toString());
			}

			return intent;
		}

		return null;
	}
	
	@SuppressLint("DefaultLocale")
	protected Intent constructDirectLaunchIntent(final String applicationName, Map<String,Object> launchParams)
	{
		if (applicationName.toLowerCase().startsWith("http://") || applicationName.toLowerCase().startsWith("https://"))
			return new Intent(Intent.ACTION_VIEW, Uri.parse(applicationName));
		else
		{
			String packageName = this.packageForApplicationName(applicationName);

			if (packageName != null)
			{
				Intent intent = this._context.getPackageManager().getLaunchIntentForPackage(packageName);

				if (launchParams != null)
				{
					for (Entry<String, Object> e : launchParams.entrySet())
					{
						intent.putExtra(e.getKey().toString(), e.getValue().toString());
					}
				}

				return intent;
			}
		}

		return null;
	}

	protected boolean updateTrigger(String triggerId, Map<String, Object> params)
	{
		boolean found = false;
		
		params.put("identifier", triggerId);
		
		for (Trigger trigger : TriggerManager.getInstance(this._context).triggersForId(triggerId))
		{
			trigger.updateFromMap(this._context, params);
			
			found = true;
		}

		if (found == false)
		{
			Trigger t = Trigger.parse(this._context, params);

			TriggerManager.getInstance(this._context).addTrigger(this._context, t);

			found = true;
		}

		return found;
	}

	protected boolean updateProbe(Map<String, Object> params)
	{
		if (params.containsKey("name"))
		{
			String probeName = params.get("name").toString();

			return ProbeManager.updateProbe(this._context, probeName, params);
		}
		
		return false;
	}
	
	public boolean launchApplication(String applicationName)
	{
		return this.launchApplication(applicationName, new HashMap<String, Object>(), null);
	}

	protected boolean launchApplication(String applicationName, Map<String, Object> launchParams, final String script)
	{
		Intent intent = this.constructLaunchIntent(applicationName, launchParams, script);

		HashMap <String, Object> payload = new HashMap<String, Object>();
		payload.put("application_present", (intent != null));
		payload.put("application_name", applicationName);
		LogManager.getInstance(this._context).log("application_launch", payload);

		if (intent != null)
		{
			this._context.startService(intent);

			return true;
		}

		return false;
	}

	protected boolean showApplicationLaunchNotification(String title, String message, String applicationName, long displayWhen, Map<String,Object> launchParams, final String script)
	{
		try
		{
			long now = System.currentTimeMillis();

			if (displayWhen < now)
				displayWhen = now;

			Notification note = new Notification(R.drawable.ic_launcher, message, displayWhen);

			Intent intent = this.constructDirectLaunchIntent(applicationName, launchParams);

			HashMap <String, Object> payload = new HashMap<String, Object>();
			payload.put("application_present", (intent != null));
			payload.put("application_name", applicationName);
			LogManager.getInstance(this._context).log("application_launch_notification", payload);

			if (intent != null)
			{
				PendingIntent pendingIntent = PendingIntent.getActivity(this._context, 0, intent, 0);

				note.setLatestEventInfo(this._context, title, message, pendingIntent);

				note.flags = Notification.FLAG_AUTO_CANCEL;

				try
				{
					NotificationManager noteManager = (NotificationManager) this._context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
					noteManager.notify(BaseScriptEngine.NOTIFICATION_ID, note);
				}
				catch (UnsupportedOperationException e)
				{
					// Added so that the mock test cases could still execute.
				}
			}

			return true;
		}
		catch (Exception e)
		{
			LogManager.getInstance(this._context).logException(e);
		}

		return false;
	}

	public void showNativeDialog(String title, String message, String confirmTitle, String cancelTitle, String confirmScript, String cancelScript)
	{
		Intent intent = new Intent(this._context, DialogActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		intent.putExtra(DialogActivity.DIALOG_TITLE, title);
		intent.putExtra(DialogActivity.DIALOG_MESSAGE, message);
		intent.putExtra(DialogActivity.DIALOG_CONFIRM_BUTTON, confirmTitle);
		intent.putExtra(DialogActivity.DIALOG_CONFIRM_SCRIPT, confirmScript);

		if (cancelTitle != null  && "".equals(cancelTitle.trim()) == false)
			intent.putExtra(DialogActivity.DIALOG_CANCEL_BUTTON, cancelTitle);

		if (cancelScript != null && "".equals(cancelScript.trim()) == false)
			intent.putExtra(DialogActivity.DIALOG_CANCEL_SCRIPT, cancelScript);

		this._context.startActivity(intent);
	}

	public boolean showApplicationLaunchNotification(String title, String message, String applicationName, long displayWhen)
	{
		return this.showApplicationLaunchNotification(title, message, applicationName, displayWhen, new HashMap<String, Object>(), null);
	}

	public boolean updateWidget(final String title, final String message, final String applicationName)
	{
		return this.updateWidget(title, message, applicationName, new HashMap<String, Object>(), null);
	}

	protected void updateWidget(Map<String, Object> parameters)
	{
		Intent intent = new Intent(ManagerService.UPDATE_WIDGETS);
		
		for (Object keyObj : parameters.keySet())
		{
			String key = keyObj.toString();
			
			intent.putExtra(key, parameters.get(key).toString());
		}

		this._context.startService(intent);
	}
	
	public void scheduleScript(String identifier, String dateString, String action)
	{
		ScheduleManager.updateScript(this._context, identifier, dateString, action);
	}

	protected boolean broadcastIntent(final String action, final Map<String, Object> extras)
	{
		Intent intent = new Intent(action);

		if (extras != null)
		{
			for (Entry<String, Object> e : extras.entrySet())
			{
				intent.putExtra(e.getKey(), e.getValue().toString());
			}
		}
		
		this._context.sendBroadcast(intent);
		
		return true;
	}

	public static Object runScript(Context context, String script) 
	{
		return BaseScriptEngine.runScript(context, script, null);
	}

	public static Object runScript(Context context, String script, Map<String, Object> objects) 
	{
		
		if (SchemeEngine.canRun(script))
		{
			SchemeEngine engine = new SchemeEngine(context, objects);
			
			return engine.evaluateSource(script);
		}
		else if (JavaScriptEngine.canRun(script))
		{
			JavaScriptEngine engine = new JavaScriptEngine(context);
			
			return engine.runScript(script, "extras", objects);
		}
		
		return null;
	}

	protected boolean updateConfig(Map<String, Object> config) 
	{
		return PurpleRobotApplication.updateFromMap(this._context, config);
	}

	public boolean updateConfig(String key, Object value) 
	{
		Map<String, Object> values = new HashMap<String, Object>();
		
		return PurpleRobotApplication.updateFromMap(this._context, values);
	}

	public Object valueFromString(String key, String string) 
	{
		try 
		{
			JSONObject json = new JSONObject(string);
			
			if (json.has(key))
			{
				Object value = json.get(key);
				
				if (value instanceof JSONObject)
					value = this.jsonToMap((JSONObject) value);
				else if (value instanceof JSONArray)
					value = this.jsonToList((JSONArray) value);
				
				return value;
			}
		}
		catch (JSONException e) 
		{
			LogManager.getInstance(this._context).logException(e);
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> jsonToMap(JSONObject object) 
	{
		Map<String, Object> map = new HashMap<String, Object>();
		
		Iterator<String> keys = object.keys();

		while (keys.hasNext())
		{
			String key = keys.next();

			try 
			{
				Object value = object.get(key);

				if (value instanceof JSONObject)
					value = this.jsonToMap((JSONObject) value);
				else if (value instanceof JSONArray)
					value = this.jsonToList((JSONArray) value);
				
				map.put(key, value);
			}
			catch (JSONException e) 
			{
				LogManager.getInstance(this._context).logException(e);
			}
		}

		return map;
	}

	private List<Object> jsonToList(JSONArray array) 
	{
		List<Object> list = new ArrayList<Object>();
		
		for (int i = 0; i < array.length(); i++)
		{
			try 
			{
				Object value = array.get(i);

				if (value instanceof JSONObject)
					value = this.jsonToMap((JSONObject) value);
				else if (value instanceof JSONArray)
					value = this.jsonToList((JSONArray) value);
			
				list.add(value);
			}
			catch (JSONException e) 
			{
				LogManager.getInstance(this._context).logException(e);
			}
		}

		return list;
	}

	public List<String> fetchNamespaces() 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		ArrayList<String> list = new ArrayList<String>();
		
		try 
		{
			JSONArray namespaces = new JSONArray(prefs.getString(BaseScriptEngine.SCRIPT_ENGINE_NAMESPACES, "[]"));
			
			for (int i = 0; i < namespaces.length(); i++)
			{
				list.add(namespaces.getString(i));
			}
		} 
		catch (JSONException e) 
		{
			LogManager.getInstance(this._context).logException(e);
		}
		
		return list;
	}

	public Map<String, Object> fetchNamespaceMap(String namespace) 
	{
		Map<String, Object> map = new HashMap<String, Object>();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		Map<String, ?> all = prefs.getAll();
		
		String prefix = namespace + " - " + BaseScriptEngine.SCRIPT_ENGINE_PERSISTENCE_PREFIX;
		
		for (String key : all.keySet())
		{
			if (key.indexOf(prefix) == 0)
				map.put(key.substring(prefix.length()), all.get(key));
		}
		
		return map;
	}
	
	public List<String> fetchTriggerIds() 
	{
		return TriggerManager.getInstance(this._context).triggerIds();
	}

	public Map<String, Object> fetchTrigger(String id) 
	{
		return TriggerManager.getInstance(this._context).fetchTrigger(this._context, id);
	}
	
	public boolean deleteTrigger(String id)
	{
		return TriggerManager.getInstance(this._context).deleteTrigger(id);
	}
	
	public void clearTriggers()
	{
		for (String id : this.fetchTriggerIds())
		{
			this.deleteTrigger(id);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void fetchLabels(String appContext, Map<String, Object> labels) 
	{
		Intent labelIntent = new Intent();
		labelIntent.setClass(this._context, LabelActivity.class);
		labelIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);

		labelIntent.putExtra(LabelActivity.LABEL_CONTEXT, appContext);
		labelIntent.putExtra(LabelActivity.TIMESTAMP, ((double) System.currentTimeMillis()));

		Bundle labelsBundle = new Bundle();
		
		for (String key : labels.keySet())
		{
			Map<String, Object> labelMap = (Map<String, Object>) labels.get(key);
			
			Bundle labelBundle = new Bundle();
			
			for (String labelKey : labelMap.keySet())
			{
				Object o = labelMap.get(labelKey);

				if (o instanceof String)
					labelBundle.putString(labelKey, o.toString());
				else if (o instanceof Double)
					labelBundle.putDouble(labelKey, ((Double) o).doubleValue());
				else if (o instanceof ArrayList)
				{
					ArrayList<String> listItems = new ArrayList<String>();
					
					for (Object item : ((ArrayList) o))
					{
						listItems.add(item.toString());
					}

					labelBundle.putStringArrayList(labelKey, listItems);
				}
			}
			
			labelsBundle.putParcelable(key, labelBundle);
		}
		
		labelIntent.putExtra(LabelActivity.LABEL_DEFINITIONS, labelsBundle);
		
		this._context.startActivity(labelIntent);
	}		
}
