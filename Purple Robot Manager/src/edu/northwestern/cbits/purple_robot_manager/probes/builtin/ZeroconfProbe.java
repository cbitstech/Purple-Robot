package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.IOException;
import java.util.ArrayList;

import javax.jmdns.JmmDNS;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class ZeroconfProbe extends Probe
{
	private static final String SERVICES = null;
	private static final String SERVICES_COUNT = null;

	private long _lastCheck = 0;
	private static long DURATION = 60000;
	
	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.ZeroconfProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_zeroconf_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		long now = System.currentTimeMillis();

		boolean enabled = super.isEnabled(context);

		if (enabled)
			enabled = prefs.getBoolean("config_probe_zeroconf_enabled", true);

		if (enabled)
		{
			synchronized(this)
			{
				long freq = Long.parseLong(prefs.getString("config_probe_zeroconf_frequency", "300000"));

				if (now - this._lastCheck > freq + DURATION)
				{
					this.scan(context);
				}
			}

			return true;
		}
		else
		{

		}

		return false;
	}

	private void scan(final Context context) 
	{
		Runnable r = new Runnable()
		{
			public void run() 
			{
				WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				
				MulticastLock lock = wifi.createMulticastLock("ZeroconfProbeLock");
				lock.setReferenceCounted(true);
				lock.acquire();
				
				try 
				{
					JmmDNS jmmdns = JmmDNS.Factory.getInstance();
					
					jmmdns.close();
				} 
				catch (IOException e) 
				{
				}	
				
				lock.release();
			}
		};
	}
	
	public String summarizeValue(Context context, Bundle bundle)
	{
		int count = bundle.getInt(ZeroconfProbe.SERVICES_COUNT);

		return String.format(context.getResources().getString(R.string.summary_bluetooth_probe), count);
	}

/*	private Bundle bundleForDevicesArray(Context context, ArrayList<Bundle> objects)
	{
		Bundle bundle = new Bundle();

		for (Bundle value : objects)
		{
			ArrayList<String> keys = new ArrayList<String>();

			String key = String.format(context.getString(R.string.display_bluetooth_device_title), value.getString(ZeroconfProbe.NAME), value.getString(ZeroconfProbe.ADDRESS));

			Bundle deviceBundle = new Bundle();

			deviceBundle.putString(context.getString(R.string.display_bluetooth_device_pair), value.getString(ZeroconfProbe.BOND_STATE));
			deviceBundle.putString(context.getString(R.string.display_bluetooth_device_major), value.getString(ZeroconfProbe.MAJOR_CLASS));
			deviceBundle.putString(context.getString(R.string.display_bluetooth_device_minor), value.getString(ZeroconfProbe.MINOR_CLASS));

			keys.add(context.getString(R.string.display_bluetooth_device_pair));
			keys.add(context.getString(R.string.display_bluetooth_device_major));
			keys.add(context.getString(R.string.display_bluetooth_device_minor));

			deviceBundle.putStringArrayList("KEY_ORDER", keys);

			bundle.putBundle(key, deviceBundle);
		}

		return bundle;
	} */

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		@SuppressWarnings("unchecked")
		ArrayList<Bundle> array = (ArrayList<Bundle>) bundle.get(ZeroconfProbe.SERVICES);
		int count = bundle.getInt(ZeroconfProbe.SERVICES_COUNT);

//		Bundle devicesBundle = this.bundleForDevicesArray(context, array);
//
//		formatted.putBundle(String.format(context.getString(R.string.summary_zeroconf_probe), count), devicesBundle);

		return formatted;
	};

	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_zeroconf_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_zeroconf_enabled");
		enabled.setDefaultValue(true);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_zeroconf_frequency");
		duration.setDefaultValue("300000");
		duration.setEntryValues(R.array.probe_satellite_frequency_values);
		duration.setEntries(R.array.probe_satellite_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);

		screen.addPreference(duration);

		return screen;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO Auto-generated method stub
	}
}
