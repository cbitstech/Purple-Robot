package edu.northwestern.cbits.purple_robot_manager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import android.os.IBinder;
import android.preference.PreferenceManager;

import edu.northwestern.cbits.purple_robot_manager.activities.StartActivity;
import edu.northwestern.cbits.purple_robot_manager.http.JsonScriptRequestHandler;
import edu.northwestern.cbits.purple_robot_manager.http.LocalHttpServer;
import edu.northwestern.cbits.purple_robot_manager.http.commands.JSONCommand;
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
	public static final String SCRIPT_ACTION = "edu.northwestern.cbits.purplerobot.run_script";
	
	private LocalHttpServer _httpServer = new LocalHttpServer();

	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@SuppressWarnings("deprecation")
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
					            	e.printStackTrace();
					            	
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
