package edu.northwestern.cbits.purple_robot_manager.probes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.SettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.BatteryProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.BluetoothDevicesProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.CallStateProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.CommunicationLogProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ContinuousProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.HardwareInformationProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.RobotHealthProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ScreenProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.SoftwareInformationProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.TelephonyProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.VisibleSatelliteProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.WifiAccessPointsProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.features.JavascriptFeature;

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

			Context appContext = PurpleRobotApplication.getAppContext();

			String[] featureFiles = JavascriptFeature.availableFeatures(appContext);
			String[] featureNames = JavascriptFeature.availableFeatureNames(appContext);

			for (int i = 0; i < featureFiles.length && i < featureNames.length; i++)
			{
				String title = featureNames[i];
				String filename = featureFiles[i];

				String script = JavascriptFeature.scriptForFeature(appContext, filename);

				JavascriptFeature feature = new JavascriptFeature(title, filename, script);

				ProbeManager._probeInstances.add(feature);
			}
		}

		return ProbeManager._probeInstances;
	}

	public static void nudgeProbes(Context context)
	{
		if (context != null)
		{
			for (Probe probe : ProbeManager.allProbes())
			{
				probe.nudge(context);
			}
		}
	}

	public static Probe probeForName(String name, Context context)
	{
		if (ProbeManager._cachedProbes.containsKey(name))
			return ProbeManager._cachedProbes.get(name);

		for (Probe probe : ProbeManager.allProbes())
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
