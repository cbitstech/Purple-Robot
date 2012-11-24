package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.os.Bundle;
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
	private static SharedPreferences prefs = null;

	protected static SharedPreferences getPreferences(Context context)
	{
		if (ContinuousProbe.prefs == null)
			ContinuousProbe.prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

		return ContinuousProbe.prefs;
	}

	protected Context _context = null;

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));

		String key = this.getPreferenceKey();

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_" + key + "_enabled");
		enabled.setDefaultValue(true);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_" + key + "_frequency");
		duration.setDefaultValue("1000");
		duration.setEntryValues(this.getResourceFrequencyValues());
		duration.setEntries(this.getResourceFrequencyLabels());
		duration.setTitle(R.string.probe_frequency_label);

		screen.addPreference(duration);

		return screen;
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

	public abstract int getResourceFrequencyLabels();
	public abstract int getResourceFrequencyValues();

	public abstract int getTitleResource();
	public abstract int getCategoryResource();
	public abstract long getFrequency();
	public abstract String getPreferenceKey();

	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{

	}

	public String title(Context context)
	{
		return context.getString(this.getTitleResource());
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(this.getCategoryResource());
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
}
