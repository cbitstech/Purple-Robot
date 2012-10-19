package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;

import com.WazaBe.HoloEverywhere.preference.PreferenceActivity;
import com.WazaBe.HoloEverywhere.preference.PreferenceManager;
import com.WazaBe.HoloEverywhere.preference.PreferenceScreen;
import com.WazaBe.HoloEverywhere.preference.SharedPreferences;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

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

		PreferenceScreen screen = ProbeManager.inflatePreferenceScreenFromResource(activity, R.layout.layout_settings_probe_contact, manager);

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
