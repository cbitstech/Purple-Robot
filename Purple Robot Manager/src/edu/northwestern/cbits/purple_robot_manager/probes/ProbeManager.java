package edu.northwestern.cbits.purple_robot_manager.probes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.SettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.BatteryProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.BluetoothDevicesProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.CallStateProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.CommunicationLogProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ContinuousProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.HardwareInformationProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.NetworkProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.RobotHealthProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.RunningSoftwareProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ScreenProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.SoftwareInformationProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.TelephonyProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.VisibleSatelliteProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.WifiAccessPointsProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.features.DeviceInUseFeature;
import edu.northwestern.cbits.purple_robot_manager.probes.features.JavascriptFeature;

public class ProbeManager
{
	private static final String PROBE_NAME = "name";
	private static Map<String, Probe> _cachedProbes = new HashMap<String, Probe>();
	private static List<Probe> _probeInstances = new ArrayList<Probe>();

	private static boolean _initing = false;
	private static boolean _inited = false;

	public static List<Probe> allProbes(Context context)
	{
		if (ProbeManager._inited == false && ProbeManager._initing == false)
		{
			Probe.loadProbeClasses(context);

			ProbeManager._initing = true;

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

			ProbeManager._inited = true;
			ProbeManager._initing = false;
		}

		return new ArrayList<Probe>(ProbeManager._probeInstances);
	}

	public static void nudgeProbes(Context context)
	{
		if (ProbeManager._inited == false)
			return;

		if (context != null && ProbeManager._probeInstances != null)
		{
			for (Probe probe : ProbeManager.allProbes(context))
			{
				probe.nudge(context);
			}
		}
	}

	public static Probe probeForName(String name, Context context)
	{
		if (ProbeManager._inited == false)
			return null;

		if (ProbeManager._cachedProbes.containsKey(name))
			return ProbeManager._cachedProbes.get(name);

		Probe match = null;

		for (Probe probe : ProbeManager.allProbes(context))
		{
			boolean found = false;

			if (probe instanceof ContinuousProbe)
			{
				ContinuousProbe continuous = (ContinuousProbe) probe;

				if (continuous.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof VisibleSatelliteProbe)
			{
				VisibleSatelliteProbe satellite = (VisibleSatelliteProbe) probe;

				if (satellite.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof BluetoothDevicesProbe)
			{
				BluetoothDevicesProbe bluetooth = (BluetoothDevicesProbe) probe;

				if (bluetooth.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof NetworkProbe)
			{
				NetworkProbe network = (NetworkProbe) probe;

				if (network.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof SoftwareInformationProbe)
			{
				SoftwareInformationProbe software = (SoftwareInformationProbe) probe;

				if (software.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof HardwareInformationProbe)
			{
				HardwareInformationProbe hardware = (HardwareInformationProbe) probe;

				if (hardware.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof TelephonyProbe)
			{
				TelephonyProbe telephony = (TelephonyProbe) probe;

				if (telephony.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof RobotHealthProbe)
			{
				RobotHealthProbe robot = (RobotHealthProbe) probe;

				if (robot.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof ScreenProbe)
			{
				ScreenProbe screen = (ScreenProbe) probe;

				if (screen.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof BatteryProbe)
			{
				BatteryProbe battery = (BatteryProbe) probe;

				if (battery.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof LocationProbe)
			{
				LocationProbe location = (LocationProbe) probe;

				if (location.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof WifiAccessPointsProbe)
			{
				WifiAccessPointsProbe wifi = (WifiAccessPointsProbe) probe;

				if (wifi.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof CommunicationLogProbe)
			{
				CommunicationLogProbe comms = (CommunicationLogProbe) probe;

				if (comms.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof CallStateProbe)
			{
				CallStateProbe callState = (CallStateProbe) probe;

				if (callState.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof JavascriptFeature)
			{
				JavascriptFeature jsFeature = (JavascriptFeature) probe;

				if (jsFeature.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof DeviceInUseFeature)
			{
				DeviceInUseFeature device = (DeviceInUseFeature) probe;

				if (device.name(context).equalsIgnoreCase(name))
					found = true;
			}
			else if (probe instanceof RunningSoftwareProbe)
			{
				RunningSoftwareProbe software = (RunningSoftwareProbe) probe;

				if (software.name(context).equalsIgnoreCase(name))
					found = true;
			}

			if (found)
			{
				ProbeManager._cachedProbes.put(name, probe);
				match = probe;
			}
		}

		return match;
	}

	public static PreferenceScreen buildPreferenceScreen(PreferenceActivity settingsActivity)
	{
		PreferenceManager manager = settingsActivity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(settingsActivity);
		screen.setOrder(0);
		screen.setTitle(R.string.title_preference_probes_screen);
		screen.setKey(SettingsActivity.PROBES_SCREEN_KEY);

		PreferenceCategory globalCategory = new PreferenceCategory(settingsActivity);
		globalCategory.setTitle(R.string.title_preference_probes_global_category);
		globalCategory.setKey("key_available_probes");

		screen.addPreference(globalCategory);

		CheckBoxPreference enabled = new CheckBoxPreference(settingsActivity);
		enabled.setTitle(R.string.title_preference_probes_enable_probes);
		enabled.setKey("config_probes_enabled");
		enabled.setDefaultValue(false);

		globalCategory.addPreference(enabled);

		PreferenceCategory probesCategory = new PreferenceCategory(settingsActivity);
		probesCategory.setTitle(R.string.title_preference_probes_available_category);
		probesCategory.setKey("key_available_probes");

		screen.addPreference(probesCategory);

		for (Probe probe : ProbeManager.allProbes(settingsActivity))
		{
			PreferenceScreen probeScreen = probe.preferenceScreen(settingsActivity);

			if (probeScreen != null)
				screen.addPreference(probeScreen);
		}

		return screen;
	}

	public static void updateProbesFromJSON(Context context, JSONArray probeSettings)
	{
		if (ProbeManager._inited == false)
			return;

		for (int i = 0; i < probeSettings.length(); i++)
		{
			try
			{
				JSONObject json = probeSettings.getJSONObject(i);

				if (json.has(ProbeManager.PROBE_NAME))
				{
					String name = json.getString(ProbeManager.PROBE_NAME);

					for (Probe p : ProbeManager.allProbes(context))
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

	public static void clearFeatures()
	{
		if (ProbeManager._inited == false)
			return;

		ArrayList<Probe> toRemove = new ArrayList<Probe>();

		for (Probe p : ProbeManager._probeInstances)
		{
			if (p instanceof JavascriptFeature)
			{
				JavascriptFeature js = (JavascriptFeature) p;

				if (js.embedded() == false)
					toRemove.add(js);
			}
		}

		ProbeManager._probeInstances.removeAll(toRemove);

		ProbeManager._cachedProbes.clear();
	}

	public static void addFeature(String title, String name, String script, String formatter, List<String> sources, boolean b)
	{
		if (ProbeManager._inited == false)
			return;

		JavascriptFeature feature = new JavascriptFeature(title, name, script, formatter, sources, false);

		ProbeManager._probeInstances.add(feature);
	}

	public static void disableProbes(Context context) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		Editor editor = prefs.edit();
		editor.putBoolean("config_probes_enabled", false);
		editor.commit();
		
		ProbeManager.nudgeProbes(context);
	}

	public static void enableProbes(Context context) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		Editor editor = prefs.edit();
		editor.putBoolean("config_probes_enabled", true);
		editor.commit();
		
		ProbeManager.nudgeProbes(context);
	}

	public static boolean probesState(Context context) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		return prefs.getBoolean("config_probes_enabled", false);
	}

	public static void enableProbe(Context context, String probeName) 
	{
		Probe p = ProbeManager.probeForName(probeName, context);
		
		if (p != null)
		{
			p.enable(context);
		
			ProbeManager.nudgeProbes(context);
		}
	}

	public static void disableProbe(Context context, String probeName) 
	{
		Probe p = ProbeManager.probeForName(probeName, context);
		
		if (p != null)
		{
			p.disable(context);

			ProbeManager.nudgeProbes(context);
		}
	}
}
