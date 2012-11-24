package edu.northwestern.cbits.purple_robot_manager.probes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;

import edu.northwestern.cbits.purple_robot_manager.R;

public abstract class Probe
{
	public static final String PROBE_READING = "edu.northwestern.cbits.purple_robot.PROBE_READING";

	public static final String DURATION = "DURATION";
	public static final String PERIOD = "PERIOD";

	public abstract String name(Context context);
	public abstract String title(Context context);
	public abstract String probeCategory(Context context);
	public abstract PreferenceScreen preferenceScreen(PreferenceActivity settingsActivity);

	public void nudge(Context context)
	{
		this.isEnabled(context);
	}

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

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		return prefs.getBoolean("config_probes_enabled", false);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		return bundle.toString();
	}

	public abstract void updateFromJSON(Context context, JSONObject json) throws JSONException;

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = new Bundle();

		if (bundle.containsKey("TIMESTAMP"))
		{
			try
			{
				double time = bundle.getDouble("TIMESTAMP");

				if (time == 0)
					throw new ClassCastException("Catch me.");

				Date d = new Date(((long) time) * 1000);

				formatted.putString(context.getString(R.string.display_date_recorded), d.toString());
			}
			catch (ClassCastException e)
			{
				long time = bundle.getLong("TIMESTAMP");

				Date d = new Date(time * 1000);

				formatted.putString(context.getString(R.string.display_date_recorded), d.toString());
			}
		}

		return formatted;
	};

	protected void transmitData(Context context, Bundle data)
	{
		if (context != null)
		{
			UUID uuid = UUID.randomUUID();
			data.putString("GUID", uuid.toString());

			LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
			Intent intent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
			intent.putExtras(data);

			localManager.sendBroadcast(intent);
		}
	}
}
