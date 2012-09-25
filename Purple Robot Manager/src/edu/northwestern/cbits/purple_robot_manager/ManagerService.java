package edu.northwestern.cbits.purple_robot_manager;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class ManagerService extends IntentService
{
	public static String PERIODIC_CHECK_INTENT = "purple_robot_manager_periodic_check";
	public static String INCOMING_DATA_INTENT = "purple_robot_manager_incoming_data";
	public static String APPLICATION_LAUNCH_INTENT = "purple_robot_manager_application_launch";
	public static String APPLICATION_LAUNCH_INTENT_PACKAGE = "purple_robot_manager_widget_launch_package";
	public static String APPLICATION_LAUNCH_INTENT_PARAMETERS = "purple_robot_manager_widget_launch_parameters";
	public static String APPLICATION_LAUNCH_INTENT_POSTSCRIPT = "purple_robot_manager_widget_launch_postscript";

	public ManagerService()
	{
		super("ManagerService");
	}

	public ManagerService(String name)
	{
		super(name);
	}

	protected void onHandleIntent(Intent intent)
	{
		Log.e("PRM", "SERVER GOT INTENT " + intent.getAction());

		if (APPLICATION_LAUNCH_INTENT.equalsIgnoreCase(intent.getAction()))
		{
			String packageName = intent.getStringExtra(APPLICATION_LAUNCH_INTENT_PACKAGE);

			Log.e("PRM", "LAUNCH PACKAGE " + packageName);

			if (packageName != null)
			{
				Intent launchIntent = this.getPackageManager().getLaunchIntentForPackage(packageName);


				if (launchIntent != null)
				{
					String launchParams = intent.getStringExtra(APPLICATION_LAUNCH_INTENT_PARAMETERS);

					Log.e("PRM", "LAUNCH PARAMS " + launchParams);

					if (launchParams != null)
					{
						try
						{
							JSONObject paramsObj = new JSONObject(launchParams);

							@SuppressWarnings("unchecked")
							Iterator<String> keys = paramsObj.keys();

							while (keys.hasNext())
							{
								String key = keys.next();

								launchIntent.putExtra(key, paramsObj.getString(key));
							}
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
					}

					this.startActivity(launchIntent);
				}

				String script = intent.getStringExtra(APPLICATION_LAUNCH_INTENT_POSTSCRIPT);

				if (script != null)
				{
					JavaScriptEngine jsEngine = new JavaScriptEngine(this);

					jsEngine.runScript(script);
				}
			}
		}
		else
		{
			try
			{
				JSONConfigFile jsonConfig = new JSONConfigFile(this);

				List<Trigger> triggers = jsonConfig.getTriggers();

				Date now = new Date();

				for (Trigger trigger : triggers)
				{
					boolean execute = false;

					if (PERIODIC_CHECK_INTENT.equals(intent.getAction()) && trigger instanceof DateTrigger)
					{
						if (trigger.matches(this, now))
							execute = true;
					}
					else if (INCOMING_DATA_INTENT.equals(intent.getAction())) // TODO: Define trigger...
					{
						Log.e("PRM", "TODO: Check if need to do something based on incoming FUNF or other data.");
					}

					if (execute)
					{
						Log.e("PRM", "FIRING " + trigger.name());

						trigger.execute(this);
					}
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
	}

	public static void setupPeriodicCheck(Context context)
	{
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		PendingIntent pi = PendingIntent.getService(context, 0, new Intent(ManagerService.PERIODIC_CHECK_INTENT), PendingIntent.FLAG_UPDATE_CURRENT);

		alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 60000, pi);
	}
}
