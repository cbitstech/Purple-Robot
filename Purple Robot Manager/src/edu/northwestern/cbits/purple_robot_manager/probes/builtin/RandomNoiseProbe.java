package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.security.SecureRandom;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class RandomNoiseProbe extends Probe
{
	private static final String NOISE_VALUE = "NOISE_VALUE";
	public static final String ACTION = "purple_robot_generate_noise";

	private static final boolean DEFAULT_ENABLED = false;
	private static final boolean DEFAULT_PERSIST = false;

	private PendingIntent _intent = null;
	
	public static RandomNoiseProbe instance = null;
	
	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.RandomNoiseProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_random_noise_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_misc_category);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_random_noise_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_random_noise_enabled", false);
		
		e.commit();
	}

	public boolean isEnabled(Context context)
	{
		if (RandomNoiseProbe.instance == null)
			RandomNoiseProbe.instance = this;
		
		SharedPreferences prefs = Probe.getPreferences(context);
		
		if (super.isEnabled(context))
		{
			if (prefs.getBoolean("config_probe_random_noise_enabled", RandomNoiseProbe.DEFAULT_ENABLED))
			{
				synchronized(this)
				{
					Bundle bundle = new Bundle();
					bundle.putString("PROBE", this.name(context));
					bundle.putDouble("TIMESTAMP", ((double) System.currentTimeMillis()) / 1000);

					SecureRandom random = new SecureRandom();
					
					bundle.putFloat(RandomNoiseProbe.NOISE_VALUE, random.nextFloat());

					bundle.putBoolean("TRANSMIT", prefs.getBoolean("config_probe_random_noise_persist", RandomNoiseProbe.DEFAULT_PERSIST));

					this.transmitData(context, bundle);
					
					if (this._intent == null)
					{
						this._intent = PendingIntent.getService(context, 0, new Intent(RandomNoiseProbe.ACTION), PendingIntent.FLAG_UPDATE_CURRENT);

						AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
						
						am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 250, this._intent);
					}
				}

				return true;
			}
			else if (this._intent != null)
			{
				AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
				
				am.cancel(this._intent);
				
				this._intent = null;
			}
		}

		return false;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		float noise = bundle.getFloat(RandomNoiseProbe.NOISE_VALUE);

		return String.format(context.getResources().getString(R.string.summary_random_noise_probe), noise);
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_random_noise_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_random_noise_enabled");
		enabled.setDefaultValue(RandomNoiseProbe.DEFAULT_ENABLED);
		screen.addPreference(enabled);

		CheckBoxPreference persist = new CheckBoxPreference(activity);
		persist.setTitle(R.string.title_probe_random_noise_persist);
		persist.setSummary(R.string.summary_probe_random_noise_persist);
		persist.setKey("config_probe_random_noise_persist");
		screen.addPreference(persist);
		persist.setDefaultValue(RandomNoiseProbe.DEFAULT_PERSIST);

		return screen;
	}
	
	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);
		
		map.put("retain", prefs.getBoolean("config_probe_random_noise_persist", false));
		
		return map;
	}
	
	public void updateFromMap(Context context, Map<String, Object> params) 
	{
		super.updateFromMap(context, params);
		
		if (params.containsKey("retain"))
		{
			Object retain = params.get("retain");
			
			if (retain instanceof Boolean)
			{
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putBoolean("config_probe_random_noise_persist", ((Boolean) retain).booleanValue());
				e.commit();
			}
		}
	}



	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO Auto-generated method stub
	}
}
