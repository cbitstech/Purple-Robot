package edu.northwestern.cbits.purple_robot_manager.probes;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;

public abstract class Probe
{
	public static final String PROBE_READING = "edu.northwestern.cbits.purple_robot.PROBE_READING";

//	public static final String START_DATE = "START";
//	public static final String END_DATE = "END";
	public static final String DURATION = "DURATION";
	public static final String PERIOD = "PERIOD";

	public abstract String name(Context context);
	public abstract String title(Context context);
	public abstract String probeCategory(Context context);
	public abstract Bundle[] dataRequestBundles(Context context);
	public abstract PreferenceScreen preferenceScreen(PreferenceActivity activity);

	@SuppressWarnings("rawtypes")
	private static List<Class> _probeClasses = new ArrayList<Class>();

	@SuppressWarnings("rawtypes")
	public static void registerProbeClass(Class probeClass)
	{
		if (!Probe._probeClasses.contains(probeClass))
			Probe._probeClasses.add(probeClass);
	}

	@SuppressWarnings("rawtypes")
	public static List<Class> availableProbeClasses()
	{
		return Probe._probeClasses;
	}

	public static void loadProbeClasses(Context context)
	{
		String packageName = Probe.class.getPackage().getName();

		String[] probeClasses = context.getResources().getStringArray(R.array.probe_classes);

		for (String className : probeClasses)
		{
			try
			{
				Probe.registerProbeClass(Class.forName(packageName + "." + className));
			}
			catch (ClassNotFoundException e)
			{
				e.printStackTrace();
			}
		}
	}

	public abstract boolean isEnabled(Context context);

	public String summarizeValue(Context context, Bundle bundle)
	{
		return bundle.toString();
	}

}
