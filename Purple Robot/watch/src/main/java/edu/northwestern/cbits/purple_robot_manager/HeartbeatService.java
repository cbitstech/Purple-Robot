package edu.northwestern.cbits.purple_robot_manager;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Administrator on 3/24/15.
 */
public class HeartbeatService extends IntentService
{
    private static final String LAST_FIRE = "HeartbeatService.LAST_FIRE";
    private static final long INTERVAL = 10000;

    public HeartbeatService()
    {
        super("HeartbeatService");
    }

    protected void onHandleIntent(Intent intent)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        long now = System.currentTimeMillis();
        long lastFire = prefs.getLong(HeartbeatService.LAST_FIRE, 0);

        if (now - lastFire > HeartbeatService.INTERVAL)
        {
            Log.e("PW", "HEARTBEAT");

            SharedPreferences.Editor e = prefs.edit();
            e.putLong(HeartbeatService.LAST_FIRE, now);
            e.commit();

            AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

            PendingIntent pi = PendingIntent.getService(this, 0, new Intent(this, HeartbeatService.class), PendingIntent.FLAG_UPDATE_CURRENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, now + HeartbeatService.INTERVAL, pi);
            else
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), HeartbeatService.INTERVAL, pi);

            Intent sensorIntent = new Intent(this, SensorService.class);
            this.startService(sensorIntent);
        }
    }
}
