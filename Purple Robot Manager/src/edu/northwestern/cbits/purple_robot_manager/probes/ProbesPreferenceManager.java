package edu.northwestern.cbits.purple_robot_manager.probes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;

public class ProbesPreferenceManager
{
	public static Map<String, Bundle[]> getDataRequests(Context context)
	{
		HashMap<String, Bundle[]> probesMap = new HashMap<String, Bundle[]>();

		for (Class<Probe> probeClass : Probe.availableProbeClasses())
		{
			Log.e("PRM", "GETTING CONFIG FROM " + probeClass.getCanonicalName());

			try
			{
				String name = "unknown.probe";
				Bundle[] bundles = new Bundle[0];

				Method m = probeClass.getDeclaredMethod("funfName");
				m.setAccessible(true);
				Object o = m.invoke(null);

				Log.e("PRM", "RETURNED FUNF OBJECT: " + o.getClass());

				if (o instanceof String)
					name = (String) o;

				m = probeClass.getDeclaredMethod("dataRequestBundles", Context.class);

				m.setAccessible(true);
				o = m.invoke(null, context);

				if (o instanceof Bundle[])
					bundles = (Bundle[]) o;

				probesMap.put(name, bundles);
			}
			catch (NoSuchMethodException e)
			{
				e.printStackTrace();
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}
			catch (InvocationTargetException e)
			{
				e.printStackTrace();
			}
		}

		return probesMap;
	}

	static PreferenceScreen inflatePreferenceScreenFromResource(PreferenceActivity activity, int resId, PreferenceManager manager)
    {
    	try
    	{
            Class<PreferenceManager> cls = PreferenceManager.class;
            Method method = cls.getDeclaredMethod("inflateFromResource", Context.class, int.class, PreferenceScreen.class);
            return (PreferenceScreen) method.invoke(manager, activity, resId, null);
        }
    	catch(Exception e)
    	{
            Log.w("PRM", "Could not inflate preference screen from XML", e);
        }

        return null;
    }

	public static PreferenceScreen buildPreferenceScreen(PreferenceActivity activity)
	{
		@SuppressWarnings("deprecation")
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = ProbesPreferenceManager.inflatePreferenceScreenFromResource(activity, R.layout.layout_settings_probes_screen, manager);
		screen.setOrder(0);
		screen.setTitle(R.string.title_preference_probes_screen);

		PreferenceCategory probesCategory = (PreferenceCategory) screen.findPreference("key_available_probes");

		for (Class<Probe> probeClass : Probe.availableProbeClasses())
		{
			try
			{
				Method m = probeClass.getDeclaredMethod("preferenceScreen", PreferenceActivity.class);

				m.setAccessible(true);
				Object o = m.invoke(null, activity);

				if (o instanceof PreferenceScreen)
				{
					PreferenceScreen probeScreen = (PreferenceScreen) o;

					probesCategory.addPreference(probeScreen);
				}
			}
			catch (NoSuchMethodException e)
			{
				e.printStackTrace();
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}
			catch (InvocationTargetException e)
			{
				e.printStackTrace();
			}
		}

		return screen;
	}
}
