package edu.northwestern.cbits.purple_robot_manager;

import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;

import edu.northwestern.cbits.purple_robot_manager.activities.StartActivity;
import edu.northwestern.cbits.purple_robot_manager.http.JsonScriptRequestHandler;
import edu.northwestern.cbits.purple_robot_manager.http.LocalHttpServer;
import edu.northwestern.cbits.purple_robot_manager.http.commands.JSONCommand;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.plugins.HttpUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPluginManager;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.RandomNoiseProbe;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class PersistentService extends Service
{
    public static final String NUDGE_PROBES = "purple_robot_manager_nudge_probe";
    public static final String SCRIPT_ACTION = "edu.northwestern.cbits.purplerobot.run_script";
    public static final String START_HTTP_SERVICE = "purple_robot_start_http_service";
    public static final String STOP_HTTP_SERVICE = "purple_robot_stop_http_service";
    public static final String PROBE_NUDGE_INTERVAL = "probe_nudge_interval";
    public static final String PROBE_NUDGE_INTERVAL_DEFAULT = "15000";

    private LocalHttpServer _httpServer = new LocalHttpServer();

    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public void onCreate()
    {
        super.onCreate();

        LogManager.getInstance(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String title = this.getString(R.string.notify_running_title);
        String message = this.getString(R.string.notify_running);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, StartActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.drawable.ic_note_normal);
        builder.setWhen(System.currentTimeMillis());
        builder.setColor(0xff4e015c);

        Notification note = builder.build();

        this.startForeground(SanityManager.NOTE_ID, note);

        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        PendingIntent pi = PendingIntent.getService(this, 0, new Intent(PersistentService.NUDGE_PROBES), PendingIntent.FLAG_UPDATE_CURRENT);

        long now = System.currentTimeMillis();
        long interval = Long.parseLong(prefs.getString(PersistentService.PROBE_NUDGE_INTERVAL, PersistentService.PROBE_NUDGE_INTERVAL_DEFAULT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, now + interval, pi);
        else
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pi);

        OutputPlugin.loadPluginClasses(this);

        if (prefs.getBoolean(LocalHttpServer.BUILTIN_HTTP_SERVER_ENABLED, LocalHttpServer.BUILTIN_HTTP_SERVER_ENABLED_DEFAULT))
            this._httpServer.start(this);
        else
            this._httpServer.stop(this);

        BroadcastReceiver scriptReceiver = new BroadcastReceiver()
        {
            public void onReceive(Context context, Intent intent)
            {
                if (intent.hasExtra("response_mode"))
                {
                    if ("activity".equals(intent.getStringExtra("response_mode")))
                    {
                        if (intent.hasExtra("package_name") && intent.hasExtra("activity_class"))
                        {
                            String pkgName = intent.getStringExtra("package_name");
                            String clsName = intent.getStringExtra("activity_class");

                            Intent response = new Intent();
                            response.setComponent(new ComponentName(pkgName, clsName));
                            response.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            if (intent.hasExtra("command"))
                            {
                                try
                                {
                                    JSONObject arguments = new JSONObject();

                                    for (String key : intent.getExtras().keySet())
                                    {
                                        arguments.put(key, intent.getStringExtra(key));
                                    }

                                    JSONCommand cmd = JsonScriptRequestHandler.commandForJson(arguments, context);

                                    JSONObject result = cmd.execute(context);

                                    response.putExtra("full_payload", result.toString(2));

                                    JSONArray names = result.names();

                                    for (int i = 0; i < names.length(); i++)
                                    {
                                        String name = names.getString(i);

                                        response.putExtra(name, result.getString(name));
                                    }

                                    response.putExtra("full_payload", result.toString(2));
                                }
                                catch (JSONException e)
                                {
                                    LogManager.getInstance(context).logException(e);

                                    response.putExtra("error", e.toString());
                                }
                            }

                            context.startActivity(response);
                        }
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(PersistentService.SCRIPT_ACTION);

        this.registerReceiver(scriptReceiver, filter);

        final PersistentService me = this;

        final Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            public void uncaughtException(Thread thread, Throwable ex)
            {
                HashMap<String, Object> payload = new HashMap<>();
                payload.put("message", ex.getMessage());
                LogManager.getInstance(me).log("pr_app_crashed", payload);

                handler.uncaughtException(thread, ex);
            }
        });
    }

    @SuppressLint("NewApi")
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent != null)
        {
            String action = intent.getAction();

            if (NUDGE_PROBES.equals(action))
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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                {
                    AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

                    PendingIntent pi = PendingIntent.getService(this, 0, new Intent(PersistentService.NUDGE_PROBES), PendingIntent.FLAG_UPDATE_CURRENT);

                    long now = System.currentTimeMillis();

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    long interval = Long.parseLong(prefs.getString(PersistentService.PROBE_NUDGE_INTERVAL, PersistentService.PROBE_NUDGE_INTERVAL_DEFAULT));

                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, now + interval, pi);
                }
            }
            else if (RandomNoiseProbe.ACTION.equals(action) && RandomNoiseProbe.instance != null)
                RandomNoiseProbe.instance.isEnabled(this);
            else if (START_HTTP_SERVICE.equals(action))
                this._httpServer.start(this);
            else if (STOP_HTTP_SERVICE.equals(action))
                this._httpServer.stop(this);
        }

        return Service.START_STICKY;
    }

    public void onTaskRemoved(Intent rootIntent)
    {
        LogManager.getInstance(this).log("pr_service_stopped", null);
    }
}
