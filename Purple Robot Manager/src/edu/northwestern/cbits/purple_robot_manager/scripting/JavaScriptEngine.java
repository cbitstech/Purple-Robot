package edu.northwestern.cbits.purple_robot_manager.scripting;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.DialogActivity;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.R.drawable;
import edu.northwestern.cbits.purple_robot_manager.R.id;
import edu.northwestern.cbits.purple_robot_manager.R.layout;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.probes.features.Feature;
import edu.northwestern.cbits.purple_robot_manager.probes.features.JavascriptFeature;
import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;
import edu.northwestern.cbits.purple_robot_manager.widget.PurpleRobotAppWideWidgetProvider;
import edu.northwestern.cbits.purple_robot_manager.widget.PurpleRobotAppWidgetProvider;

public class JavaScriptEngine extends BaseScriptEngine
{
	private static final int NOTIFICATION_ID = 1;

	private Context _jsContext = null;
	private Scriptable _scope = null;

	public JavaScriptEngine(android.content.Context context)
	{
		super(context);
	}

	public Object runScript(String script) throws EvaluatorException, EcmaError
	{
		return this.runScript(script, null, null);
	}

	public Object runScript(String script, String extrasName, Object extras) throws EvaluatorException, EcmaError
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
			e.printStackTrace();

			try
			{
				Toast.makeText(this._context, e.getMessage(), Toast.LENGTH_LONG).show();
			}
			catch (RuntimeException ee)
			{
				ee.printStackTrace();
			}
			
			throw e;
		}
		catch (EcmaError e)
		{
			e.printStackTrace();

			try
			{
				Toast.makeText(this._context, e.getMessage(), Toast.LENGTH_LONG).show();
			}
			catch (RuntimeException ee)
			{
				ee.printStackTrace();
			}
			
			throw e;
		}
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

	public void updateWidget(final NativeObject parameters)
	{
		Intent intent = new Intent(ManagerService.UPDATE_WIDGETS);
		
		for (Object keyObj : parameters.keySet())
		{
			String key = keyObj.toString();
			
			intent.putExtra(key, parameters.get(key).toString());
		}

		this._context.startService(intent);
	}

	public boolean broadcastIntent(final String action, final NativeObject extras)
	{
		Intent intent = new Intent(action);

		if (extras != null)
		{
			for (Entry<Object, Object> e : extras.entrySet())
			{
				intent.putExtra(e.getKey().toString(), e.getValue().toString());
			}
		}
		
		this._context.sendBroadcast(intent);
		
		return true;
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

	public boolean updateTrigger(String triggerId, NativeObject nativeJson)
	{
		boolean found = false;

		try 
		{
			JSONObject json = JavaScriptEngine.nativeToJson(nativeJson);

			for (Trigger trigger : TriggerManager.getInstance().triggersForId(triggerId))
			{
				trigger.updateFromJson(this._context, json);
				
				found = true;
			}
			
			if (found == false)
			{
				Trigger t = Trigger.parse(this._context, json);

				TriggerManager.getInstance().addTrigger(t);
				
				found = true;
			}
		}
		catch (JSONException e) 
		{
			e.printStackTrace();
		}
		
		return found;
	}
	
	private static JSONArray nativeToJson(NativeArray nativeArray) throws JSONException
	{
		JSONArray array = new JSONArray();
		
		for (int i = 0; i < nativeArray.getLength(); i++)
		{
			Object value = nativeArray.get(i);
			
			if (value instanceof String)
				array.put(value);
			else if (value instanceof Double)
				array.put(value);
			else if (value instanceof NativeObject)
			{
				NativeObject obj = (NativeObject) value;
				
				array.put(JavaScriptEngine.nativeToJson(obj));
			}
			else if (value instanceof NativeArray)
			{
				NativeArray arr = (NativeArray) value;
				
				array.put(JavaScriptEngine.nativeToJson(arr));
			}
		}

		return array;
	}
	
	private static JSONObject nativeToJson(NativeObject nativeObj) throws JSONException 
	{
		if (nativeObj == null)
			return null;
		
		JSONObject json = new JSONObject();
		
		for (Entry<Object, Object> e : nativeObj.entrySet())
		{
			String key = e.getKey().toString();
			Object value = e.getValue();
			
			if (value instanceof String)
				json.put(key, value);
			else if (value instanceof Double)
				json.put(key, value);
			else if (value instanceof NativeObject)
			{
				NativeObject obj = (NativeObject) value;
				
				json.put(key, JavaScriptEngine.nativeToJson(obj));
			}
			else if (value instanceof NativeArray)
			{
				NativeArray arr = (NativeArray) value;
				
				json.put(key, JavaScriptEngine.nativeToJson(arr));
			}
		}
					
		return json;
	}

	public void emitReading(String name, Object value)
	{
		Bundle bundle = new Bundle();
		bundle.putString("PROBE", name);
		bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
		
		if (value instanceof String)
			bundle.putString(Feature.FEATURE_VALUE, value.toString());
		else if (value instanceof Double)
		{
			Double d = (Double) value;

			bundle.putDouble(Feature.FEATURE_VALUE, d.doubleValue());
		}
		else if (value instanceof NativeObject)
		{
			NativeObject nativeObj = (NativeObject) value;

			Bundle b = JavascriptFeature.bundleForNativeObject(nativeObj);

			bundle.putParcelable(Feature.FEATURE_VALUE, b);
		}
		else
		{
			Log.e("PRM", "JS PLUGIN GOT UNKNOWN VALUE " + value);

			if (value != null)
				Log.e("PRM", "JS PLUGIN GOT UNKNOWN CLASS " + value.getClass());
		}

		this.transmitData(bundle);
	}
	
	public void enableProbe(String probeName)
	{
		ProbeManager.enableProbe(this._context, probeName);
	}
	
	protected String language() 
	{
		return "JavaScript";
	}
}
