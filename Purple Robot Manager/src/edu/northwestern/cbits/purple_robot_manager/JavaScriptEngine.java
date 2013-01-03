package edu.northwestern.cbits.purple_robot_manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import edu.northwestern.cbits.purple_robot_manager.widget.PurpleRobotAppWidgetProvider;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class JavaScriptEngine
{
	private static String JS_ENGINE_PERSISTENCE_PREFIX = "purple_robot_js_persist_prefix_";

	private static final int NOTIFICATION_ID = 1;

	private android.content.Context _context = null;

	private static Map<String, String> packageMap = null;

	private Context _jsContext = null;
	private Scriptable _scope = null;

	public JavaScriptEngine(android.content.Context context)
	{
		this._context = context;
	}

	public Object runScript(String script) throws EvaluatorException
	{
		return this.runScript(script, null, null);
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
			e.printStackTrace();
		} 
			
		return null;
	}

	public Object runScript(String script, String extrasName, Object extras) throws EvaluatorException
	{
		this._jsContext = Context.enter();
		this._jsContext.setOptimizationLevel(-1);

		this._scope = _jsContext.initStandardObjects();

		/* if (extras instanceof JSONObject)
		{
			JSONObject json = (JSONObject) extras;

			extras = new JsonParser().parse(json.toString());
		} */

		Object thisWrapper = Context.javaToJS(this, this._scope);
		ScriptableObject.putProperty(this._scope, "PurpleRobot", thisWrapper);

		if (extras != null && extrasName != null)
			script = "var " + extrasName + " = " + extras.toString() + "; " + script;

		try
		{
			return this._jsContext.evaluateString(this._scope, script, "<engine>", 1, null);
		}
		catch (EvaluatorException e)
		{
			Toast.makeText(this._context, e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (EcmaError e)
		{
			Toast.makeText(this._context, e.getMessage(), Toast.LENGTH_LONG).show();
		}

		return null;
	}

	public boolean loadLibrary(String libraryName)
	{
		if (this._jsContext != null && this._scope != null)
		{
			try
			{
				if (libraryName.endsWith(".js") == false)
					libraryName += ".js";

				AssetManager am = this._context.getAssets();

				InputStream jsStream = am.open("js/" + libraryName);

				// http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
			    Scanner s = new Scanner(jsStream).useDelimiter("\\A");

			    String script = "";

			    if (s.hasNext())
			    	script = s.next();
			    else
			    	return false;

				this._jsContext.evaluateString(this._scope, script, "<engine>", 1, null);

				return true;
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		return false;
	}

	@SuppressLint("DefaultLocale")
	private Intent constructDirectLaunchIntent(final String applicationName, final NativeObject launchParams)
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
					for (Entry<Object, Object> e : launchParams.entrySet())
					{
						intent.putExtra(e.getKey().toString(), e.getValue().toString());
					}
				}

				return intent;
			}
		}

		return null;
	}

	private Intent constructLaunchIntent(final String applicationName, final NativeObject launchParams, final String script)
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

				for (Entry<Object, Object> e : launchParams.entrySet())
				{
					launchMap.put(e.getKey().toString(), e.getValue().toString());
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

				for (Entry<Object, Object> e : launchParams.entrySet())
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

	public boolean updateWidget(final String title, final String message, final String applicationName)
	{
		return this.updateWidget(title, message, applicationName, new NativeObject(), null);
	}

	public boolean updateWidget(final String title, final String message, final String applicationName, final NativeObject launchParams, final String script)
	{
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(this._context);

		ComponentName provider = new ComponentName(this._context.getPackageName(), PurpleRobotAppWidgetProvider.class.getName());

		int[] widgetIds = widgetManager.getAppWidgetIds(provider);

		RemoteViews views = new RemoteViews(this._context.getPackageName(), R.layout.layout_widget);

		views.setCharSequence(R.id.widget_title_text, "setText", title);
		views.setCharSequence(R.id.widget_message_text, "setText", message);

		Intent intent = this.constructLaunchIntent(applicationName, launchParams, script);

		if (intent != null)
		{
			if (intent.getAction().equals(ManagerService.APPLICATION_LAUNCH_INTENT))
			{
				PendingIntent pi = PendingIntent.getService(this._context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				views.setOnClickPendingIntent(R.id.widget_root_layout, pi);
			}
			else
			{
				PendingIntent pi = PendingIntent.getActivity(this._context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				views.setOnClickPendingIntent(R.id.widget_root_layout, pi);
			}
		}

		widgetManager.updateAppWidget(widgetIds, views);

		return true;
	}

	public boolean emitToast(final String message, final boolean longDuration)
	{
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

			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}

	public String packageForApplicationName(String applicationName)
	{
		if (applicationName == null)
			return null;

		if (JavaScriptEngine.packageMap == null)
		{
			JavaScriptEngine.packageMap = new HashMap<String, String>();

			String[] keyArray = this._context.getResources().getStringArray(R.array.app_package_keys);
			String[] valueArray = this._context.getResources().getStringArray(R.array.app_package_values);

			if (keyArray.length == valueArray.length)
			{
				for (int i = 0; i < keyArray.length; i++)
				{
					JavaScriptEngine.packageMap.put(keyArray[i].toLowerCase(), valueArray[i]);
				}
			}
		}

		String packageName = JavaScriptEngine.packageMap.get(applicationName.toLowerCase());

		if (packageName == null)
			packageName = applicationName; // Allows us to launch by package name as well.

		PackageManager pkgManager = this._context.getPackageManager();

		Intent launchIntent = pkgManager.getLaunchIntentForPackage(packageName);

		if (launchIntent == null) // No matching package found on system...

			packageName = null;

		return packageName;
	}

	public boolean launchApplication(String applicationName)
	{
		return this.launchApplication(applicationName, new NativeObject(), null);
	}

	public boolean launchApplication(String applicationName, final NativeObject launchParams, final String script)
	{
		Intent intent = this.constructLaunchIntent(applicationName, launchParams, script);

		if (intent != null)
		{
			this._context.startService(intent);

			return true;
		}

		return false;
	}

	public boolean showApplicationLaunchNotification(String title, String message, String applicationName, long displayWhen)
	{
		return this.showApplicationLaunchNotification(title, message, applicationName, displayWhen, new NativeObject(), null);
	}

	public boolean showApplicationLaunchNotification(String title, String message, String applicationName, long displayWhen, final NativeObject launchParams, final String script)
	{
		try
		{
			long now = System.currentTimeMillis();

			if (displayWhen < now)
				displayWhen = now;

			Notification note = new Notification(R.drawable.ic_launcher, message, displayWhen);

			Intent intent = this.constructDirectLaunchIntent(applicationName, launchParams);

			if (intent != null)
			{
				PendingIntent pendingIntent = PendingIntent.getActivity(this._context, 0, intent, 0);

				note.setLatestEventInfo(this._context, title, message, pendingIntent);

				note.flags = Notification.FLAG_AUTO_CANCEL;

				try
				{
					NotificationManager noteManager = (NotificationManager) this._context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
					noteManager.notify(JavaScriptEngine.NOTIFICATION_ID, note);
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
			e.printStackTrace();
		}

		return false;
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
			e.printStackTrace();
		}

		return null;
	}

	public void showNativeDialog(String title, String message, String confirmTitle, String cancelTitle, String confirmScript, String cancelScript)
	{
		Intent intent = new Intent(this._context, DialogActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		intent.putExtra(DialogActivity.DIALOG_TITLE, title);
		intent.putExtra(DialogActivity.DIALOG_MESSAGE, message);
		intent.putExtra(DialogActivity.DIALOG_CONFIRM_BUTTON, confirmTitle);
		intent.putExtra(DialogActivity.DIALOG_CANCEL_BUTTON, cancelTitle);
		intent.putExtra(DialogActivity.DIALOG_CONFIRM_SCRIPT, confirmScript);
		intent.putExtra(DialogActivity.DIALOG_CANCEL_SCRIPT, cancelScript);

		this._context.startActivity(intent);
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
			e.printStackTrace();
		}

		return -1;
	}

	public boolean persistString(String key, String value)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		Editor editor = prefs.edit();

		key = JS_ENGINE_PERSISTENCE_PREFIX + key;

		if (value != null)
			editor.putString(key,  value.toString());
		else
			editor.remove(key);

		return editor.commit();
	}

	public String fetchString(String key)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

		key = JS_ENGINE_PERSISTENCE_PREFIX + key;

		return prefs.getString(key, null);
	}

	public boolean persistEncryptedString(String key, String value)
	{
		key = JS_ENGINE_PERSISTENCE_PREFIX + key;

		return EncryptionManager.getInstance().persistEncryptedString(this._context, key, value);
	}

	public String fetchEncryptedString(String key)
	{
		key = JS_ENGINE_PERSISTENCE_PREFIX + key;
		
		return EncryptionManager.getInstance().fetchEncryptedString(this._context, key);
	}
	
	public void vibrate(String pattern)
	{
		Intent intent = new Intent(ManagerService.HAPTIC_PATTERN_INTENT);
		intent.putExtra(ManagerService.HAPTIC_PATTERN_NAME, pattern);

		this._context.startService(intent);
	}

	public void playTone(String tone)
	{
		Intent intent = new Intent(ManagerService.RINGTONE_INTENT);

		if (tone != null)
			intent.putExtra(ManagerService.RINGTONE_NAME, tone);

		this._context.startService(intent);
	}

	public void playDefaultTone()
	{
		this.playTone(null);
	}

	public void log(String message)
	{
		Log.e("PRM.JavaScript", message);
	}
}
