package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public abstract class ContinuousProbe extends Probe
{
	public static final String WAKE_ACTION = "purple_robot_sensor_wake";

	protected static final String PROBE_THRESHOLD = "threshold";

	protected static final boolean DEFAULT_ENABLED = false;
	protected static final String DEFAULT_FREQUENCY = "0";

	private PendingIntent _intent = null;
	
	private WakeLock _wakeLock = null;
	private int _wakeLockLevel = -1;

	protected Context _context = null;

	public void enable(Context context)
	{
		String key = this.getPreferenceKey();

		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_" + key + "_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		String key = this.getPreferenceKey();

		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_" + key + "_enabled", false);
		
		e.commit();
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(this.summary(activity));

		String key = this.getPreferenceKey();

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_" + key + "_enabled");
		enabled.setDefaultValue(ContinuousProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_" + key + "_frequency");
		duration.setEntryValues(this.getResourceFrequencyValues());
		duration.setEntries(this.getResourceFrequencyLabels());
		duration.setTitle(R.string.probe_frequency_label);
		duration.setDefaultValue(ContinuousProbe.DEFAULT_FREQUENCY);

		screen.addPreference(duration);

		ListPreference wakelock = new ListPreference(activity);
		wakelock.setKey("config_probe_" + key + "_wakelock");
		wakelock.setEntryValues(R.array.wakelock_values);
		wakelock.setEntries(R.array.wakelock_labels);
		wakelock.setTitle(R.string.probe_wakelock_title);
		wakelock.setSummary(R.string.probe_wakelock_summary);
		wakelock.setDefaultValue("-1");

		screen.addPreference(wakelock);
		
		return screen;
	}

	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		map.put(Probe.PROBE_FREQUENCY, this.getFrequency());
		
		return map;
	}
	
	public void updateFromMap(Context context, Map<String, Object> params) 
	{
		super.updateFromMap(context, params);
		
		if (params.containsKey(Probe.PROBE_FREQUENCY))
		{
			Object frequency = params.get(Probe.PROBE_FREQUENCY);
			
			if (frequency instanceof Long)
			{
				String key = "config_probe_" + this.getPreferenceKey() + "_frequency";
				
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putString(key, frequency.toString());
				e.commit();
			}
			else if (frequency instanceof String)
			{
				String key = "config_probe_" + this.getPreferenceKey() + "_frequency";
				
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putString(key, frequency.toString());
				e.commit();
			}
		}
	}

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		return formatted;
	};

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO...
	}
	
	public int getResourceFrequencyLabels()
	{
		return R.array.probe_continuous_frequency_labels;
	}

	public int getResourceFrequencyValues()
	{
		return R.array.probe_continuous_frequency_values;
	}
	
	public abstract long getFrequency();

	public abstract int getTitleResource();
	public abstract int getSummaryResource();

	public abstract String getPreferenceKey();

	protected abstract boolean passesThreshold(SensorEvent event);

	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{

	}

	public String title(Context context)
	{
		return context.getString(this.getTitleResource());
	}

	public String summary(Context context)
	{
		return context.getString(this.getSummaryResource());
	}

	protected void transmitData(Bundle data)
	{
		if (this._context != null)
		{
			UUID uuid = UUID.randomUUID();
			data.putString("GUID", uuid.toString());

			LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this._context);
			Intent intent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
			intent.putExtras(data);

			localManager.sendBroadcast(intent);
		}
	}
	
	@SuppressLint("Wakelock")
	public boolean isEnabled(Context context)
	{
		boolean enabled = super.isEnabled(context);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		String key = this.getPreferenceKey();
		
		int wakeLevel = Integer.parseInt(prefs.getString("config_probe_" + key + "_wakelock", "-1"));

		if (enabled)
		{
			if (this._intent == null)
			{
				this._intent = PendingIntent.getService(context, 0, new Intent(ContinuousProbe.WAKE_ACTION), PendingIntent.FLAG_UPDATE_CURRENT);

				AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
				
				am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 250, this._intent);
			}
			
			if (wakeLevel != this._wakeLockLevel)
			{
				if (this._wakeLock != null)
				{
					this._wakeLock.release();
					
					this._wakeLock = null;
				}
				
				this._wakeLockLevel = wakeLevel;
			}
			
			if (this._wakeLockLevel != -1 && this._wakeLock == null)
			{
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				
				this._wakeLock = pm.newWakeLock(this._wakeLockLevel, "key");
				this._wakeLock.acquire();
			}
		}
		else
		{
			if (this._intent != null)
			{
				AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
				
				am.cancel(this._intent);
				
				this._intent = null;
			}

			if (this._wakeLock != null)
			{
				this._wakeLock.release();
				this._wakeLock = null;
			}
		}
		
		return enabled;
	}
}
