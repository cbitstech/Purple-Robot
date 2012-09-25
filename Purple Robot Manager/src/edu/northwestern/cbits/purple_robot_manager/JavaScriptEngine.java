package edu.northwestern.cbits.purple_robot_manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

public class JavaScriptEngine
{
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
		this._jsContext = Context.enter();
		this._jsContext.setOptimizationLevel(-1);

		this._scope = _jsContext.initStandardObjects();

		Object thisWrapper = Context.javaToJS(this, this._scope);
		ScriptableObject.putProperty(this._scope, "PurpleRobot", thisWrapper);

		return this._jsContext.evaluateString(this._scope, script, "<engine>", 1, null);
	}

	private Intent constructLaunchIntent(final String applicationName, final NativeObject launchParams, final String script)
	{
		String packageName = this.packageForApplicationName(applicationName);

		if (packageName != null)
		{
			Intent intent = new Intent(ManagerService.APPLICATION_LAUNCH_INTENT);
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
			PendingIntent pi = PendingIntent.getService(this._context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

			views.setOnClickPendingIntent(R.id.widget_root_layout, pi);
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

		if (packageName != null)
		{
			PackageManager pkgManager = this._context.getPackageManager();

			Intent launchIntent = pkgManager.getLaunchIntentForPackage(packageName);

			if (launchIntent == null) // No matching package found on system...
				packageName = null;
		}

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
			this._context.startActivity(intent);

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

			Intent intent = this.constructLaunchIntent(applicationName, launchParams, script);

			if (intent != null)
			{
				PendingIntent pendingIntent = PendingIntent.getActivity(this._context, 0, intent, 0);

				note.setLatestEventInfo(this._context, title, message, pendingIntent);
			}

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
}
