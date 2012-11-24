package edu.northwestern.cbits.purple_robot_manager;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class PersistentService extends Service
{
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@SuppressWarnings("deprecation")
	public void onCreate()
	{
		super.onCreate();

		String title = this.getString(R.string.app_name);
		String message = this.getString(R.string.notify_running);

		Notification note = new Notification(R.drawable.ic_notify_foreground, title, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, StartActivity.class), Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR);
		note.setLatestEventInfo(this, title, message, contentIntent);

		this.startForeground(12345, note);
	}
}
