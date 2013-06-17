package edu.northwestern.cbits.purple_robot_manager;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import edu.northwestern.cbits.purple_robot_manager.http.LocalHttpServer;
import edu.northwestern.cbits.purple_robot_manager.plugins.HttpUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPluginManager;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ContinuousProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.RandomNoiseProbe;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class PersistentService extends Service
{
	public static final String NUDGE_PROBES = "purple_robot_manager_nudge_probe";
	
	private LocalHttpServer _httpServer = new LocalHttpServer();

	public IBinder onBind(Intent intent)
	{
		return null;
	}

	public void onCreate()
	{
		super.onCreate();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		String title = this.getString(R.string.notify_running_title);
		String message = this.getString(R.string.notify_running);

		Notification note = new Notification(R.drawable.ic_notify_foreground, title, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, StartActivity.class), Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR);
		note.setLatestEventInfo(this, title, message, contentIntent);

		this.startForeground(12345, note);

		AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

		PendingIntent pi = PendingIntent.getService(this, 0, new Intent(PersistentService.NUDGE_PROBES), PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 15000, pi);

		OutputPlugin.loadPluginClasses(this);

		if (prefs.getBoolean("config_http_server_enabled", true))
			this._httpServer.start(this);
	}

	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if (intent != null)
		{
			if (NUDGE_PROBES.equals(intent.getAction()))
			{
				ProbeManager.nudgeProbes(this);
				TriggerManager.getInstance(this).refreshTriggers(this);
				ScheduleManager.runOverdueScripts(this);

				OutputPlugin plugin = OutputPluginManager.sharedInstance.pluginForClass(this, HttpUploadPlugin.class);
				
				if (plugin instanceof HttpUploadPlugin)
				{
					HttpUploadPlugin http = (HttpUploadPlugin) plugin;
					http.uploadPendingObjects();
				}
			}
			else if (ContinuousProbe.WAKE_ACTION.equals(intent.getAction()))
			{
				
			}
			else if (RandomNoiseProbe.ACTION.equals(intent.getAction()) && RandomNoiseProbe.instance != null)
				RandomNoiseProbe.instance.isEnabled(this);
		}

		return Service.START_STICKY;
	}
}
