package edu.northwestern.cbits.purple_robot_manager;

import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.activities.SettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.config.LegacyJSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ActivityDetectionProbe;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class ManagerService extends IntentService
{
	public static String RUN_SCRIPT_INTENT = "purple_robot_manager_run_script";
	public static String RUN_SCRIPT = "run_script";

	public static String APPLICATION_LAUNCH_INTENT = "purple_robot_manager_application_launch";
	public static String APPLICATION_LAUNCH_INTENT_PACKAGE = "purple_robot_manager_widget_launch_package";
	public static String APPLICATION_LAUNCH_INTENT_URL = "purple_robot_manager_widget_launch_url";
	public static String APPLICATION_LAUNCH_INTENT_PARAMETERS = "purple_robot_manager_widget_launch_parameters";
	public static String APPLICATION_LAUNCH_INTENT_POSTSCRIPT = "purple_robot_manager_widget_launch_postscript";

	public static String UPDATE_WIDGETS = "purple_robot_manager_update_widgets";
	public static String PERIODIC_CHECK_INTENT = "purple_robot_manager_periodic_check";
	public static String INCOMING_DATA_INTENT = "purple_robot_manager_incoming_data";
	public static String UPLOAD_LOGS_INTENT = "purple_robot_manager_upload_logs";
	public static String REFRESH_ERROR_STATE_INTENT = "purple_robot_manager_refresh_errors";

	public static String GOOGLE_PLAY_ACTIVITY_DETECTED = "purple_robot_manager_google_play_activity_detected";

	public static String HAPTIC_PATTERN_INTENT = "purple_robot_manager_haptic_pattern";
	public static String HAPTIC_PATTERN_NAME = "purple_robot_manager_haptic_pattern_name";
	public static String RINGTONE_INTENT = "purple_robot_manager_ringtone";
	public static String RINGTONE_NAME = "purple_robot_manager_ringtone_name";

	public static String REFRESH_CONFIGURATION = "purple_robot_manager_refresh_configuration";
	
	public static long startTimestamp = System.currentTimeMillis();

	private static boolean _checkSetup = false;

	public ManagerService()
	{
		super("ManagerService");
	}

	public ManagerService(String name)
	{
		super(name);
	}

	@SuppressWarnings("deprecation")
	protected void onHandleIntent(Intent intent)
	{
		String action = intent.getAction();
		
		if (GOOGLE_PLAY_ACTIVITY_DETECTED.equalsIgnoreCase(action))
			ActivityDetectionProbe.activityDetected(this, intent);
		if (UPLOAD_LOGS_INTENT.equalsIgnoreCase(action))
		{
			final ManagerService me = this;
			
			Runnable r = new Runnable()
			{
				public void run() 
				{
					LogManager.getInstance(me).attemptUploads();
				}
			};
			
			try
			{
				Thread t = new Thread(r);
				t.start();
			}
			catch (OutOfMemoryError e)
			{
				System.gc();
			}
		}
		else if (REFRESH_ERROR_STATE_INTENT.equalsIgnoreCase(action))
		{
			final ManagerService me = this;
			
			Runnable r = new Runnable()
			{
				public void run() 
				{
					SanityManager.getInstance(me).refreshState();
				}
			};
			
			Thread t = new Thread(r);
			t.start();
		}
		else if (UPDATE_WIDGETS.equalsIgnoreCase(action))
		{
			Intent broadcast = new Intent("edu.northwestern.cbits.purple.UPDATE_WIDGET");
			broadcast.putExtras(intent.getExtras());
			
			this.startService(broadcast);
		}
		else if (HAPTIC_PATTERN_INTENT.equalsIgnoreCase(action))
		{
			String pattern = intent.getStringExtra(HAPTIC_PATTERN_NAME);

			if (!pattern.startsWith("vibrator_"))
				pattern = "vibrator_" + pattern;

			int[] patternSpec = this.getResources().getIntArray(R.array.vibrator_buzz);

			if ("vibrator_blip".equalsIgnoreCase(pattern))
				patternSpec = this.getResources().getIntArray(R.array.vibrator_blip);
			if ("vibrator_sos".equalsIgnoreCase(pattern))
				patternSpec = this.getResources().getIntArray(R.array.vibrator_sos);

			long[] longSpec = new long[patternSpec.length];

			for (int i = 0; i < patternSpec.length; i++)
			{
				longSpec[i] = (long) patternSpec[i];
			}

			Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
			v.cancel();
			v.vibrate(longSpec, -1);
		}
		else if (RINGTONE_INTENT.equalsIgnoreCase(action))
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

			Uri toneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

			String toneString = prefs.getString(SettingsActivity.RINGTONE_KEY, null);

			String name = null;
			
			if (intent.hasExtra(ManagerService.RINGTONE_NAME))
				name = intent.getStringExtra(ManagerService.RINGTONE_NAME);
			
			try
			{
				if (toneString != null)
				{
					if (toneString.equals("0"))
						toneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION);
					else if (toneString.startsWith("sounds/") == false) 
						toneUri = Uri.parse(toneString);
					else
						toneUri = null;
				}
				else
				{
					if (name != null)
					{
						RingtoneManager rm = new RingtoneManager(this);
						rm.setType(RingtoneManager.TYPE_NOTIFICATION);

						Cursor cursor = rm.getCursor();

						do
						{
							String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);

							if (name.equalsIgnoreCase(title))
								toneUri = rm.getRingtoneUri(cursor.getPosition());
						}
						while (cursor.moveToNext());

						cursor.deactivate();
					}
				}
			}
			catch(Exception e)
			{
				LogManager.getInstance(this).logException(e);
			}
			
			if (toneUri != null)
			{
				final Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), toneUri);
	
				if (r != null)
				{
					final ManagerService me = this;
					
					Thread t = new Thread(new Runnable()
					{
						public void run()
						{
							r.play();
	
							try
							{
								while (r.isPlaying())
									Thread.sleep(100);
							}
							catch (InterruptedException e)
							{
								LogManager.getInstance(me).logException(e);
							}
						}
					});
	
					t.start();
				}
			}
			else
			{
				try 
				{
					final ManagerService me = this;

					final AssetFileDescriptor afd = this.getAssets().openFd(ManagerService.pathForSound(this, toneString));

					Runnable r = new Runnable()
					{
						public void run() 
						{
							MediaPlayer player = new MediaPlayer();
							
							try 
							{
								player.setDataSource(afd.getFileDescriptor() ,afd.getStartOffset(),afd.getLength());
							    player.prepare();
							    player.start();
							}
							catch (IllegalArgumentException e) 
							{
								LogManager.getInstance(me).logException(e);
							} 
							catch (IllegalStateException e) 
							{
								LogManager.getInstance(me).logException(e);
							} 
							catch (IOException e) 
							{
								LogManager.getInstance(me).logException(e);
							}
						}
					};
					
					Thread t = new Thread(r);
					t.start();
				}
				catch (IOException e) 
				{
					LogManager.getInstance(this).logException(e);
				}
			}
		}
		else if (APPLICATION_LAUNCH_INTENT.equalsIgnoreCase(action))
		{
			if (intent.hasExtra(APPLICATION_LAUNCH_INTENT_PACKAGE))
			{
				String packageName = intent.getStringExtra(APPLICATION_LAUNCH_INTENT_PACKAGE);

				if (packageName != null)
				{
					Intent launchIntent = this.getPackageManager().getLaunchIntentForPackage(packageName);

					if (launchIntent != null)
					{
						String launchParams = intent.getStringExtra(APPLICATION_LAUNCH_INTENT_PARAMETERS);

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
								LogManager.getInstance(this).logException(e);
							}
						}

						this.startActivity(launchIntent);
					}

					String script = intent.getStringExtra(APPLICATION_LAUNCH_INTENT_POSTSCRIPT);

					if (script != null)
						BaseScriptEngine.runScript(this, script);
				}
			}
			else if (intent.hasExtra(APPLICATION_LAUNCH_INTENT_URL))
			{
				String url = intent.getStringExtra(APPLICATION_LAUNCH_INTENT_URL);

				if (url != null)
				{
					Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

					this.startActivity(launchIntent);

					String script = intent.getStringExtra(APPLICATION_LAUNCH_INTENT_POSTSCRIPT);

					if (script != null)
						BaseScriptEngine.runScript(this, script);
				}
			}
		}
		else if (RUN_SCRIPT_INTENT.equalsIgnoreCase(action))
		{
			if (intent.hasExtra(RUN_SCRIPT))
			{
				String script = intent.getStringExtra(RUN_SCRIPT);

				if (script != null)
					BaseScriptEngine.runScript(this, script);
			}
		}
		else if (PERIODIC_CHECK_INTENT.equals(action))
		{
			TriggerManager.getInstance(this).nudgeTriggers(this);
		}
		else if (REFRESH_CONFIGURATION.equals(action))
			LegacyJSONConfigFile.update(this, false);
	}

	public static void setupPeriodicCheck(final Context context)
	{
		if (ManagerService._checkSetup)
			return;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		Editor editor = prefs.edit();
		editor.putString(LegacyJSONConfigFile.JSON_LAST_HASH, "");
		editor.commit();

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		PendingIntent pi = PendingIntent.getService(context, 0, new Intent(ManagerService.PERIODIC_CHECK_INTENT), PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 15000, pi);

		pi = PendingIntent.getService(context, 0, new Intent(ManagerService.REFRESH_CONFIGURATION), PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 5000, pi);
		
		prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener()
		{
	        public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
	        {
	        	Intent reloadIntent = new Intent(ManagerService.REFRESH_CONFIGURATION);
	        	reloadIntent.setClass(context, ManagerService.class);

	        	context.startService(reloadIntent);
	        }
	    });

		Intent nudgeIntent = new Intent(PersistentService.NUDGE_PROBES);
		nudgeIntent.setClass(context, PersistentService.class);

		context.startService(nudgeIntent);
		
		SanityManager.getInstance(context);

		ManagerService._checkSetup = true;
	}

	public static String soundNameForPath(Context context, String path) 
	{
		String[] values = context.getResources().getStringArray(R.array.sound_effect_values);
		String[] labels = context.getResources().getStringArray(R.array.sound_effect_labels);
		
		for (int i = 0; i < labels.length && i < values.length; i++)
		{
			if (values[i].toLowerCase().equals(path.toLowerCase(Locale.getDefault())))
				return labels[i];
		}
			
		return path;
	}
	
	private static String pathForSound(Context context, String name) 
	{
		String[] values = context.getResources().getStringArray(R.array.sound_effect_values);
		String[] labels = context.getResources().getStringArray(R.array.sound_effect_labels);
		
		for (int i = 0; i < labels.length && i < values.length; i++)
		{
			if (labels[i].toLowerCase(Locale.getDefault()).equals(name.toLowerCase(Locale.getDefault())))
				return values[i];
		}
			
		return name;
	}

}
