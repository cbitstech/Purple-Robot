package edu.northwestern.cbits.purple_robot_manager.probes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.SettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.probes.funf.BasicFunfProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.funf.ContactProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.funf.PeriodFunfProbe;

public class ProbeManager
{
	private static final String PROBE_NAME = "name";
	private static Map<String, Probe> _cachedProbes = new HashMap<String, Probe>();
	private static List<Probe> _probeInstances = null;

	public static List<Probe> allProbes()
	{
		if (ProbeManager._probeInstances == null)
		{
			ProbeManager._probeInstances = new ArrayList<Probe>();

			for (Class<Probe> probeClass : Probe.availableProbeClasses())
			{
				try
				{
					Probe probe = (Probe) probeClass.newInstance();

					ProbeManager._probeInstances.add(probe);
				}
				catch (InstantiationException e)
				{
					e.printStackTrace();
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
				}
			}
		}

		return ProbeManager._probeInstances;
	}

	public static Map<String, Bundle[]> getDataRequests(Context context)
	{
		HashMap<String, Bundle[]> probesMap = new HashMap<String, Bundle[]>();

		for (Probe probe : ProbeManager.allProbes())
		{
			String name = probe.name(context);

			Bundle[] bundles = new Bundle[0];

			if (probe.isEnabled(context))
				bundles = probe.dataRequestBundles(context);

			probesMap.put(name, bundles);
		}

		return probesMap;
	}

/*	public static PreferenceScreen inflatePreferenceScreenFromResource(PreferenceActivity settingsActivity, int resId, PreferenceManager manager)
    {
    	try
    	{
            Class<PreferenceManager> cls = PreferenceManager.class;
            Method method = cls.getDeclaredMethod("inflateFromResource", Context.class, int.class, PreferenceScreen.class);
            return (PreferenceScreen) method.invoke(manager, settingsActivity, resId, null);
        }
    	catch(Exception e)
    	{
    		e.printStackTrace();
        }

        return null;
    } */

	public static Probe probeForName(String name)
	{
		if (ProbeManager._cachedProbes.containsKey(name))
			return ProbeManager._cachedProbes.get(name);

		for (Probe probe : ProbeManager.allProbes())
		{
			boolean found = false;

			if (probe instanceof BasicFunfProbe)
			{
				BasicFunfProbe funf = (BasicFunfProbe) probe;

				if (funf.funfName().equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof PeriodFunfProbe)
			{
				PeriodFunfProbe funf = (PeriodFunfProbe) probe;

				if (funf.funfName().equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof ContactProbe)
			{
				ContactProbe contact = (ContactProbe) probe;

				if (contact.funfName().equalsIgnoreCase(name))
					found = true;
			}

			if (found)
			{
				ProbeManager._cachedProbes.put(name, probe);
				return probe;
			}
		}

		return null;
	}

	public static PreferenceScreen buildPreferenceScreen(PreferenceActivity settingsActivity)
	{
		@SuppressWarnings("deprecation")
		PreferenceManager manager = settingsActivity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(settingsActivity);
		screen.setOrder(0);
		screen.setTitle(R.string.title_preference_probes_screen);
		screen.setKey(SettingsActivity.PROBES_SCREEN_KEY);

/*		PreferenceCategory globalCategory = new PreferenceCategory(settingsActivity);
		globalCategory.setTitle(R.string.title_preference_probes_global_category);
		globalCategory.setKey("key_available_probes");

		screen.addPreference(globalCategory);

		CheckBoxPreference enabled = new CheckBoxPreference(settingsActivity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_enabled");
		enabled.setDefaultValue(true);

		globalCategory.addPreference(enabled);
*/
		PreferenceCategory probesCategory = new PreferenceCategory(settingsActivity);
		probesCategory.setTitle(R.string.title_preference_probes_available_category);
		probesCategory.setKey("key_available_probes");

		screen.addPreference(probesCategory);

		for (Probe probe : ProbeManager.allProbes())
		{
			PreferenceScreen probeScreen = probe.preferenceScreen(settingsActivity);

			if (probeScreen != null)
				screen.addPreference(probeScreen);
		}


		return screen;
	}

	public static void updateProbesFromJSON(Context context, JSONArray probeSettings)
	{
		for (int i = 0; i < probeSettings.length(); i++)
		{
			try
			{
				JSONObject json = probeSettings.getJSONObject(i);

				if (json.has(ProbeManager.PROBE_NAME))
				{
					String name = json.getString(ProbeManager.PROBE_NAME);

					for (Probe p : ProbeManager.allProbes())
					{
						if (name.equalsIgnoreCase(p.title(context)))
							p.updateFromJSON(context, json);
					}
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
	}
}
