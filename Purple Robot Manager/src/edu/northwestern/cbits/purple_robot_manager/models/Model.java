package edu.northwestern.cbits.purple_robot_manager.models;

import java.util.HashMap;
import java.util.UUID;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public abstract class Model 
{
	public static final boolean DEFAULT_ENABLED = true;
	private static long _lastEnabledCheck = 0;
	private static boolean _lastEnabled = false;

	public abstract String getPreferenceKey();
	public abstract String title(Context context);
	public abstract String summary(Context context);
	
	public void enable(Context context)
	{
		String key = this.getPreferenceKey();

		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_model_" + key + "_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		String key = this.getPreferenceKey();

		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_model_" + key + "_enabled", false);
		
		e.commit();
	}

	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(this.summary(activity));

		String key = this.getPreferenceKey();

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_model);
		enabled.setKey("config_model_" + key + "_enabled");
		enabled.setDefaultValue(Model.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		return screen;
	}
	
	public boolean isEnabled(Context context)
	{
		long now = System.currentTimeMillis();
		SharedPreferences prefs = Probe.getPreferences(context);
		
		if (now - Model._lastEnabledCheck  > 10000)
		{
			Model._lastEnabledCheck = now;

			Model._lastEnabled  = prefs.getBoolean("config_models_enabled", Model.DEFAULT_ENABLED);
		}
		
		if (Model._lastEnabled)
		{
			String key = this.getPreferenceKey();

			return prefs.getBoolean("config_model_" + key + "_enabled", true);
		}
		
		return Model._lastEnabled;
	}
	
	protected void transmitPrediction(Context context, double prediction) 
	{
		Bundle bundle = new Bundle();
		bundle.putString("PROBE", this.name(context));
		bundle.putDouble("TIMESTAMP", ((double) System.currentTimeMillis()) / 1000);
		bundle.putDouble("PREDICTION", prediction);
		bundle.putBoolean("FROM_MODEL", true);

		this.transmitData(context, bundle);
	}

	protected void transmitPrediction(Context context, String prediction) 
	{
		Bundle bundle = new Bundle();
		bundle.putString("PROBE", this.name(context));
		bundle.putDouble("TIMESTAMP", ((double) System.currentTimeMillis()) / 1000);
		bundle.putString("PREDICTION", prediction);
		bundle.putBoolean("FROM_MODEL", true);

		this.transmitData(context, bundle);
	}

	protected void transmitData(Context context, Bundle data) 
	{
		if (context != null)
		{
			UUID uuid = UUID.randomUUID();
			data.putString("GUID", uuid.toString());
			data.putString("MODEL_NAME", this.title(context));

			LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
			Intent intent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
			intent.putExtras(data);

			localManager.sendBroadcast(intent);
		}
	}

	public abstract void predict(Context context, HashMap<String, Object> snapshot);
	protected abstract String name(Context context);
}
