package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import java.util.ArrayList;

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

public class ContactProbe extends Probe
{
	private static String USE_FULL = "FULL";

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_social_category);
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(R.string.title_contact_probe);
		screen.setSummary(R.string.summary_contact_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_contact_enabled");
		enabled.setDefaultValue(true);

		screen.addPreference(enabled);

		ListPreference period = new ListPreference(activity);
		period.setKey("config_probe_contact_period");
		period.setDefaultValue("1200");
		period.setEntryValues(R.array.probe_period_values);
		period.setEntries(R.array.probe_period_labels);
		period.setTitle(R.string.probe_period_label);
		period.setSummary(R.string.probe_period_summary);

		screen.addPreference(period);

		ListPreference full = new ListPreference(activity);
		full.setKey("config_probe_contact_full_list");
		full.setDefaultValue("false");
		full.setEntryValues(R.array.probe_boolean_values);
		full.setEntries(R.array.probe_boolean_labels);
		full.setTitle(R.string.probe_contact_full_label);
		full.setSummary(R.string.probe_contact_full_summary);

		screen.addPreference(full);

		return screen;
	}


	public Bundle[] dataRequestBundles(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		Bundle bundle = new Bundle();
		bundle.putLong(Probe.PERIOD, Long.parseLong(prefs.getString("config_probe_contact_period", "3600")));
		bundle.putBoolean(ContactProbe.USE_FULL, "true".equals(prefs.getString("config_probe_contact_full_list", "false")));

		return new Bundle[] { bundle };
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = prefs.edit();

		if (json.has("period"))
			editor.putString("config_probe_contact_period", json.getString("period"));

		if (json.has("full_list"))
			editor.putString("config_probe_contact_full_list", json.getString("full_list"));

		if (json.has("enabled"))
			editor.putBoolean("config_probe_contact_enabled", json.getBoolean("enabled"));

		editor.commit();
	}

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		return prefs.getBoolean("config_probe_contact_enabled", true);
	}

	public String name(Context context)
	{
		return this.funfName();
	}

	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.ContactProbe";
	}

	public String title(Context context)
	{
		return context.getResources().getString(R.string.title_contact_probe);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		@SuppressWarnings("unchecked")
		ArrayList<Object> contacts = (ArrayList<Object>) bundle.get("CONTACT_DATA");

		return String.format(context.getResources().getString(R.string.summary_contact_probe), contacts.size());
	}
}
