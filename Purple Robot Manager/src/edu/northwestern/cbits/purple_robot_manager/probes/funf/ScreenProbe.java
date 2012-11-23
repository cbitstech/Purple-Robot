package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import org.json.JSONException;
import org.json.JSONObject;

import com.WazaBe.HoloEverywhere.preference.CheckBoxPreference;
import com.WazaBe.HoloEverywhere.preference.PreferenceManager;
import com.WazaBe.HoloEverywhere.preference.PreferenceScreen;
import com.WazaBe.HoloEverywhere.preference.SharedPreferences;
import com.WazaBe.HoloEverywhere.preference.SharedPreferences.Editor;
import com.WazaBe.HoloEverywhere.sherlock.SPreferenceActivity;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class ScreenProbe extends Probe
{
	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_interaction_category);
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(SPreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(R.string.title_screen_probe);
		screen.setSummary(R.string.summary_screen_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_screen_enabled");
		enabled.setDefaultValue(true);

		screen.addPreference(enabled);

		return screen;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = prefs.edit();

		if (json.has("enabled"))
			editor.putBoolean("config_probe_screen_enabled", json.getBoolean("enabled"));

		editor.commit();
	}

	public boolean isEnabled(Context context)
	{
		if (super.isEnabled(context))
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

			return prefs.getBoolean("config_probe_screen_enabled", true);
		}

		return false;
	}

	public Bundle[] dataRequestBundles(Context context)
	{
		Bundle bundle = new Bundle();
		return new Bundle[] { bundle };
	}

	public String name(Context context)
	{
		return "edu.mit.media.funf.probe.builtin.ScreenProbe";
	}

	public String title(Context context)
	{
		return context.getResources().getString(R.string.title_contact_probe);
	}
}
