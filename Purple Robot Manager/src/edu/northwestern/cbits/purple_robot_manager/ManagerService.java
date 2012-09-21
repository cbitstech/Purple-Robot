package edu.northwestern.cbits.purple_robot_manager;

import java.util.Date;
import java.util.List;

import org.json.JSONException;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ManagerService extends IntentService
{
	public static String PERIODIC_CHECK_INTENT = "purple_robot_manager_periodic_check";
	public static String INCOMING_DATA_INTENT = "purple_robot_manager_incoming_data";

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

	public static void setupPeriodicCheck(Context context)
	{
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		PendingIntent pi = PendingIntent.getService(context, 0, new Intent(ManagerService.PERIODIC_CHECK_INTENT), PendingIntent.FLAG_UPDATE_CURRENT);

		alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 60000, pi);
	}
}
