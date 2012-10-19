package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import android.os.Bundle;

import com.WazaBe.HoloEverywhere.preference.Preference;
import com.WazaBe.HoloEverywhere.preference.PreferenceActivity;
import com.WazaBe.HoloEverywhere.preference.PreferenceManager;
import com.WazaBe.HoloEverywhere.preference.PreferenceScreen;
import com.WazaBe.HoloEverywhere.preference.SharedPreferences;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

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

		PreferenceScreen screen = ProbeManager.inflatePreferenceScreenFromResource(activity, R.layout.layout_settings_probe_funf_period, manager);

		screen.setTitle(this.funfTitle());
		screen.setSummary(this.funfSummary());

		String key = this.key();

		Preference period = screen.findPreference("config_probe_funf_period");
		period.setKey("config_probe_" + key + "_period");

		Preference enabled = screen.findPreference("config_probe_funf_enabled");
		enabled.setKey("config_probe_" + key + "_enabled");

		return screen;
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
