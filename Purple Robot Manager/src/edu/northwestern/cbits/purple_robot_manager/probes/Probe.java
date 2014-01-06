package edu.northwestern.cbits.purple_robot_manager.probes;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

public abstract class Probe
{
	public static final String PROBE_READING = "edu.northwestern.cbits.purple_robot.PROBE_READING";

	public static final String DURATION = "DURATION";
	public static final String PERIOD = "PERIOD";

	public static final String PROBE_FREQUENCY = "frequency";
	public static final String HASH_DATA = "hash_data";

	public static final String DEFAULT_FREQUENCY = "300000";
	public static final boolean DEFAULT_HASH_DATA = true;

	public static final String ENCRYPT_DATA = "encrypt_data";

	public abstract String name(Context context);
	public abstract String title(Context context);
	public abstract String probeCategory(Context context);
	public abstract PreferenceScreen preferenceScreen(PreferenceActivity settingsActivity);
	
	private static long _lastEnabledCheck = 0;
	private static boolean _lastEnabled = false;
	
	private static SharedPreferences _preferences = null;
	
	public static SharedPreferences getPreferences(Context context)
	{
		if (Probe._preferences == null)
			Probe._preferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		return Probe._preferences;
	}
	
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
				LogManager.getInstance(context).logException(e);
			}
		}
	}

	public static boolean probesEnabled(Context context)
	{
		long now = System.currentTimeMillis();
		
		if (now - Probe._lastEnabledCheck > 10000)
		{
			Probe._lastEnabledCheck = now;

			SharedPreferences prefs = Probe.getPreferences(context);

			Probe._lastEnabled = prefs.getBoolean("config_probes_enabled", false);
		}
		
		return Probe._lastEnabled;
	}

	public boolean isEnabled(Context context)
	{
		long now = System.currentTimeMillis();
		
		if (now - Probe._lastEnabledCheck > 10000)
		{
			Probe._lastEnabledCheck = now;

			SharedPreferences prefs = Probe.getPreferences(context);

			Probe._lastEnabled = prefs.getBoolean("config_probes_enabled", false);
		}
		
		return Probe._lastEnabled;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		return bundle.toString();
	}

//	public abstract void updateFromJSON(Context context, JSONObject json) throws JSONException;

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

	public Intent viewIntent(Context context)
	{
		return null;
	}

	public String getDisplayContent(Activity activity)
	{
		return null;
	}

	public String contentSubtitle(Context context)
	{
		return null;
	}

	public abstract void enable(Context context);
	public abstract void disable(Context context);

	public Map<String, Object> configuration(Context context)
	{
		HashMap<String, Object> map = new HashMap<String, Object>();
		
		map.put("name", this.name(context));
		map.put("enabled", this.isEnabled(context));
		
		return map;
	}
	
	public void updateFromMap(Context context, Map<String, Object> params) 
	{
		if (params.containsKey("enabled"))
		{
			Object enabled = params.get("enabled");
			
			if (enabled instanceof Boolean)
			{
				if (((Boolean) enabled).booleanValue())
					this.enable(context);
				else
					this.disable(context);
			}
		}
	}
}
