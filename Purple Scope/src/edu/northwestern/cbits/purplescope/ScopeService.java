package edu.northwestern.cbits.purplescope;

import java.io.IOException;
import java.io.RandomAccessFile;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

public class ScopeService extends IntentService
{
	public static final String TICK = "scope_tick";
	public static final String BEGIN_SAMPLING = "scope_begin_sampling";
	public static final String END_SAMPLING = "scope_end_sampling";
	public static final String TIMESTAMP = "timestamp";
	public static final String DURATION = "duration";
	protected static final String SESSION_NAME = "session_name";

	private static boolean _running = false;
	private static long _timestamp = 0;
	private static long _duration = 0;
	private static boolean _cpuEnabled = false;
	private static boolean _batteryEnabled = false;
	private static boolean _robotMemoryEnabled = false;
	private static boolean _funfMemoryEnabled = false;
	
	private static BroadcastReceiver _batteryReceiver = null;
	private static String _sessionName = null;
	
	public ScopeService() 
	{
		super("Purple Scope");
	}

	public ScopeService(String name) 
	{
		super(name);
	}

	public static boolean isSampling()
	{
		return ScopeService._running;
	}
	
	@SuppressLint("NewApi")
	protected void onHandleIntent(Intent intent) 
	{
		String action = intent.getAction();

		AlarmManager alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pi = PendingIntent.getService(this, 0, new Intent(ScopeService.TICK), PendingIntent.FLAG_UPDATE_CURRENT);
		
		if (ScopeService.TICK.equals(action))
		{
			if (ScopeService._running)
			{
				if (ScopeService._duration > 0)
				{
					long now = System.currentTimeMillis();
					
					long delta = (now - ScopeService._timestamp);
					
					ScopeService._duration = ScopeService._duration - delta;

					ScopeService._timestamp = now;

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
						alarms.setExact(AlarmManager.RTC_WAKEUP, now + 1000, pi);
					else
						alarms.set(AlarmManager.RTC_WAKEUP, now + 1000, pi);
					
					this.sample();
	
					Intent broadcastIntent = new Intent(HomeActivity.UPDATE_INTERFACE);
					broadcastIntent.putExtra(ScopeService.TIMESTAMP, ScopeService._timestamp);
					broadcastIntent.putExtra(ScopeService.DURATION, ScopeService._duration);
					
					LocalBroadcastManager broadcasts = LocalBroadcastManager.getInstance(this);
					broadcasts.sendBroadcast(broadcastIntent);
				}
				else
					this.startService(new Intent(ScopeService.END_SAMPLING));
			}	
		}
		else if (ScopeService.BEGIN_SAMPLING.equals(action))
		{
			if (ScopeService._running == false)
			{
				ScopeService._running = true;
				ScopeService._duration = intent.getLongExtra(ScopeService.DURATION, 0);
				ScopeService._sessionName = intent.getStringExtra(ScopeService.SESSION_NAME);

				ContentValues values = new ContentValues();
		        values.put("source", ScopeService._sessionName);
		        values.put("recorded", System.currentTimeMillis());
		        values.put("value", "begin");

		        this.getContentResolver().insert(PerformanceContentProvider.PERFORMANCE_VALUES, values);
				
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

				if (prefs.contains(this.getString(R.string.resource_cpu)))
					ScopeService._cpuEnabled = true;
				else
					ScopeService._cpuEnabled = false;

				if (prefs.contains(this.getString(R.string.resource_battery)))
					ScopeService._batteryEnabled = true;
				else
					ScopeService._batteryEnabled = false;

				if (prefs.contains(this.getString(R.string.resource_memory_robot)))
					ScopeService._robotMemoryEnabled = true;
				else
					ScopeService._robotMemoryEnabled = false;
				
				if (prefs.contains(this.getString(R.string.resource_memory_funf)))
					ScopeService._funfMemoryEnabled = true;
				else
					ScopeService._funfMemoryEnabled = false;
				
				long now = System.currentTimeMillis();
				
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
					alarms.setExact(AlarmManager.RTC_WAKEUP, now + 1000, pi);
				else
					alarms.set(AlarmManager.RTC_WAKEUP, now + 1000, pi);
				
				ScopeService._timestamp = System.currentTimeMillis();
			}
		}
		else if (ScopeService.END_SAMPLING.equals(action))
		{
			if (ScopeService._running)
			{
				ContentValues values = new ContentValues();
		        values.put("source", ScopeService._sessionName);
		        values.put("recorded", System.currentTimeMillis());
		        values.put("value", "end");

		        this.getContentResolver().insert(PerformanceContentProvider.PERFORMANCE_VALUES, values);

		        ScopeService._running = false;
				
				ScopeService._cpuEnabled = false;
				ScopeService._robotMemoryEnabled = false;
				ScopeService._batteryEnabled = false;
				ScopeService._funfMemoryEnabled = false;
				
				Intent broadcastIntent = new Intent(HomeActivity.UPDATE_INTERFACE);
				broadcastIntent.putExtra(ScopeService.TIMESTAMP, ScopeService._timestamp);
				broadcastIntent.putExtra(ScopeService.DURATION, 0L);

				LocalBroadcastManager broadcasts = LocalBroadcastManager.getInstance(this);
				broadcasts.sendBroadcast(broadcastIntent);
			}
		}
	}

	private void sample() 
	{
		if (ScopeService._cpuEnabled)
		{
		    try 
		    {
		        RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
		        String load = reader.readLine();

		        String[] toks = load.split(" ");

		        long idle1 = Long.parseLong(toks[5]);
		        long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
		              + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

		        try 
		        {
		            Thread.sleep(100);
		        } 
		        catch (Exception e) 
		        {
		        	
		        }

		        reader.seek(0);
		        load = reader.readLine();
		        reader.close();

		        toks = load.split(" ");

		        long idle2 = Long.parseLong(toks[5]);
		        long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
		            + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

		        float use = (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

		        ContentValues values = new ContentValues();
		        values.put("source", "cpu");
		        values.put("recorded", System.currentTimeMillis());
		        values.put("value", "" + use);
		        
		        this.getContentResolver().insert(PerformanceContentProvider.PERFORMANCE_VALUES, values);
		    }
		    catch (IOException e) 
		    {
		        e.printStackTrace();
		    }
		}

		if (ScopeService._robotMemoryEnabled)
		{
			// public MemoryInfo[] getProcessMemoryInfo (int[] pids)
		}

		if (ScopeService._funfMemoryEnabled)
		{
			
		}

		if (ScopeService._batteryEnabled)
		{
			if (ScopeService._batteryReceiver == null)
			{
				ScopeService._batteryReceiver = new BroadcastReceiver()
				{
					public void onReceive(Context context, Intent intent)
					{
						int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

				        ContentValues values = new ContentValues();
				        values.put("source", "battery_level");
				        values.put("recorded", System.currentTimeMillis());
				        values.put("value", "" + level);
				        
				        context.getContentResolver().insert(PerformanceContentProvider.PERFORMANCE_VALUES, values);
					}
				};

				this.getApplicationContext().registerReceiver(ScopeService._batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			}
		}
		else if (ScopeService._batteryReceiver != null)
		{
			this.getApplicationContext().unregisterReceiver(ScopeService._batteryReceiver);
			ScopeService._batteryReceiver = null;
		}
	}
}
