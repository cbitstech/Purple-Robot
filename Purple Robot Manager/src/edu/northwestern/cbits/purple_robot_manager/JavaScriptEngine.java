package edu.northwestern.cbits.purple_robot_manager;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class JavaScriptEngine
{
	private static final int NOTIFICATION_ID = 1;

	private android.content.Context _context = null;

	private static Map<String, String> packageMap = null;

	private static Map<String, Script> confirmMap = new HashMap<String, Script>();
	private static Map<String, Script> cancelMap = new HashMap<String, Script>();
	private static Map<String, Context> contextMap = new HashMap<String, Context>();
	private static Map<String, Scriptable> scopeMap = new HashMap<String, Scriptable>();

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
		String packageName = this.packageForApplicationName(applicationName);

		if (packageName != null)
		{
			Intent intent = this._context.getPackageManager().getLaunchIntentForPackage(packageName);
			this._context.startActivity(intent);

			return true;
		}

		return false;
	}

	@SuppressWarnings("deprecation")
	public boolean showApplicationLaunchNotification(String title, String message, String applicationName, long displayWhen)
	{
		try
		{
			String packageName = this.packageForApplicationName(applicationName);

			PackageManager packageMgr = this._context.getPackageManager();

			if (packageName == null)
			{
				try
				{
					PackageInfo info = packageMgr.getPackageInfo(this._context.getPackageName(), 0);
					packageName = info.packageName;
				}
				catch (NameNotFoundException e)
				{
					e.printStackTrace();
				}
			}

			long now = System.currentTimeMillis();

			if (displayWhen < now)
				displayWhen = now;

			Intent intent = packageMgr.getLaunchIntentForPackage(packageName);
			PendingIntent pendingIntent = PendingIntent.getActivity(this._context, 0, intent, 0);

			Notification note = new Notification(R.drawable.ic_launcher, message, displayWhen);
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

	public void closureTest(Object o)
	{
		Log.e("PRM", "CLOSURE: " + o.getClass());
	}

	private static void clearClosures(String key)
	{
		confirmMap.remove(key);
		cancelMap.remove(key);
		contextMap.remove(key);
		scopeMap.remove(key);
	}

	public static void executeConfirmClosure(String key)
	{
//		Script script = confirmMap.get(key);

//		Context context = contextMap.get(key);
//		Scriptable scope = scopeMap.get(key);

//		script.exec(context, scope);

		JavaScriptEngine.clearClosures(key);
	}

	public static void executeCancelClosure(String key)
	{
//		Script script = cancelMap.get(key);

//		Context context = contextMap.get(key);
//		Scriptable scope = scopeMap.get(key);

//		script.exec(context, scope);

		JavaScriptEngine.clearClosures(key);
	}

	public void showNativeDialog(String title, String message, String confirmTitle, String cancelTitle, Script confirmClosure, Script denyClosure)
	{
		String key = title + message;

		JavaScriptEngine.confirmMap.put(key, confirmClosure);
		JavaScriptEngine.cancelMap.put(key, denyClosure);
		JavaScriptEngine.contextMap.put(key, this._jsContext);
		JavaScriptEngine.scopeMap.put(key, this._scope);

		Intent intent = new Intent(this._context, DialogActivity.class);
		intent.putExtra(DialogActivity.DIALOG_TITLE, title);
		intent.putExtra(DialogActivity.DIALOG_MESSAGE, message);
		intent.putExtra(DialogActivity.DIALOG_CONFIRM_BUTTON, confirmTitle);
		intent.putExtra(DialogActivity.DIALOG_CANCEL_BUTTON, cancelTitle);

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
