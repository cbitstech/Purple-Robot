package edu.northwestern.cbits.purple_robot_manager;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

public class PersistentService extends Service
{
	public static final String NUDGE_PROBES = "purple_robot_manager_nudge_probe";

	public IBinder onBind(Intent intent)
	{
		return null;
	}

	public void onCreate()
	{
		super.onCreate();

		String title = this.getString(R.string.app_name);
		String message = this.getString(R.string.notify_running);

		Notification note = new Notification(R.drawable.ic_notify_foreground, title, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, StartActivity.class), Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR);
		note.setLatestEventInfo(this, title, message, contentIntent);

		this.startForeground(12345, note);

		AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

		PendingIntent pi = PendingIntent.getService(this, 0, new Intent(PersistentService.NUDGE_PROBES), PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 15000, pi);

		OutputPlugin.loadPluginClasses(this);
	}

	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if (intent != null && NUDGE_PROBES.equals(intent.getAction()))
			ProbeManager.nudgeProbes(this);

		return Service.START_STICKY;
	}
}
