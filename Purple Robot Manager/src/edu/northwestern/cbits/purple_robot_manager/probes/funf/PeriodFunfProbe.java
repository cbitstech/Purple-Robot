package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public abstract class PeriodFunfProbe extends Probe
{
	public abstract String funfName();

	public abstract String key();

	public String name(Context context)
	{
		return this.funfName();
	}

	public String title(Context context)
	{
		return context.getResources().getString(this.funfTitle());
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.funfTitle());
		screen.setSummary(this.funfSummary());

		String key = this.key();

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_" + key + "_enabled");
		enabled.setDefaultValue(true);

		screen.addPreference(enabled);

		ListPreference period = new ListPreference(activity);
		period.setKey("config_probe_" + key + "_period");
		period.setDefaultValue(this.period());
		period.setEntryValues(R.array.probe_period_values);
		period.setEntries(R.array.probe_period_labels);
		period.setTitle(R.string.probe_period_label);
		period.setSummary(R.string.probe_period_summary);

		screen.addPreference(period);

		return screen;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = prefs.edit();

		if (json.has("period"))
			editor.putString("config_probe_" + this.key() + "_period", json.getString("period"));

		if (json.has("enabled"))
			editor.putBoolean("config_probe_" + this.key() + "_enabled", json.getBoolean("enabled"));

		editor.commit();
	}

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		return prefs.getBoolean("config_probe_" + this.key() + "_enabled", true);
	}

	protected abstract int funfTitle();
	protected abstract int funfSummary();

	public String period()
	{
		return "3600";
	}

	public Bundle[] dataRequestBundles(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		Bundle bundle = new Bundle();
		bundle.putLong(Probe.PERIOD, Long.parseLong(prefs.getString("config_probe_" + this.key() + "_period", this.period())));

		return new Bundle[] { bundle };
	}

	public abstract String probeCategory(Context context);
}
