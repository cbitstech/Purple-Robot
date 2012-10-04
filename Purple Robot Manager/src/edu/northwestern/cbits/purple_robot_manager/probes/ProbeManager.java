package edu.northwestern.cbits.purple_robot_manager.probes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;

public class ProbeManager
{
	public static Map<String, Bundle[]> getDataRequests(Context context)
	{
		HashMap<String, Bundle[]> probesMap = new HashMap<String, Bundle[]>();

		for (Class<Probe> probeClass : Probe.availableProbeClasses())
		{
			try
			{
				Probe probe = (Probe) probeClass.newInstance();

				String name = probe.name(context);
				Bundle[] bundles = probe.dataRequestBundles(context);

				probesMap.put(name, bundles);
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}
			catch (InstantiationException e)
			{
				e.printStackTrace();
			}
		}

		return probesMap;
	}

	public static PreferenceScreen inflatePreferenceScreenFromResource(PreferenceActivity activity, int resId, PreferenceManager manager)
    {
    	try
    	{
            Class<PreferenceManager> cls = PreferenceManager.class;
            Method method = cls.getDeclaredMethod("inflateFromResource", Context.class, int.class, PreferenceScreen.class);
            return (PreferenceScreen) method.invoke(manager, activity, resId, null);
        }
    	catch(Exception e)
    	{
    		e.printStackTrace();
        }

        return null;
    }

	public static PreferenceScreen buildPreferenceScreen(PreferenceActivity activity)
	{
		@SuppressWarnings("deprecation")
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = ProbeManager.inflatePreferenceScreenFromResource(activity, R.layout.layout_settings_probes_screen, manager);
		screen.setOrder(0);
		screen.setTitle(R.string.title_preference_probes_screen);

		PreferenceCategory probesCategory = (PreferenceCategory) screen.findPreference("key_available_probes");

		for (Class<Probe> probeClass : Probe.availableProbeClasses())
		{
			try
			{
				Probe probe = (Probe) probeClass.newInstance();

				PreferenceScreen probeScreen = probe.preferenceScreen(activity);

				probesCategory.addPreference(probeScreen);
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}
			catch (InstantiationException e)
			{
				e.printStackTrace();
			}
		}

		return screen;
	}
}
