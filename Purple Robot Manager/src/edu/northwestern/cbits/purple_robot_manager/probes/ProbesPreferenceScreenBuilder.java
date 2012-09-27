package edu.northwestern.cbits.purple_robot_manager.probes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import edu.northwestern.cbits.purple_robot_manager.R;
import android.content.Context;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

public class ProbesPreferenceScreenBuilder
{
	private static Class[] _probeClasses = { LocationProbe.class };

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
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = ProbesPreferenceScreenBuilder.inflatePreferenceScreenFromResource(activity, R.layout.layout_settings_probes_screen, manager);
		screen.setOrder(0);
		screen.setTitle(R.string.title_preference_probes_screen);

		PreferenceCategory probesCategory = (PreferenceCategory) screen.findPreference("key_available_probes");

		for (Class probeClass : _probeClasses)
		{
			try
			{
				Method m = probeClass.getDeclaredMethod("preferenceScreen", PreferenceActivity.class);

				m.setAccessible(true);
				Object o = m.invoke(null, activity);

				Log.e("PRM", "GOT OBJECT " + o.getClass());

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
