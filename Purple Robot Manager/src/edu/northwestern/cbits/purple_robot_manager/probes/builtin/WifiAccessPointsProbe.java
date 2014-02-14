package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class WifiAccessPointsProbe extends Probe
{
	private static final String ACCESS_POINT_COUNT = "ACCESS_POINT_COUNT";
	protected static final String ACCESS_POINTS = "ACCESS_POINTS";
	protected static final String BSSID = "BSSID";
	protected static final String SSID = "SSID";
	protected static final String CAPABILITIES = "CAPABILITIES";
	protected static final String FREQUENCY = "FREQUENCY";
	protected static final String LEVEL = "LEVEL";
	protected static final String CURRENT_SSID = "CURRENT_SSID";
	protected static final String CURRENT_RSSI = "CURRENT_RSSI";
	protected static final String CURRENT_BSSID = "CURRENT_BSSID";
	protected static final String CURRENT_LINK_SPEED = "CURRENT_LINK_SPEED";

	private static final boolean DEFAULT_ENABLED = true;

	private long _lastCheck = 0;

	private BroadcastReceiver _receiver = null;

	private ArrayList<Bundle> _foundNetworks = new ArrayList<Bundle>();

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.WifiAccessPointsProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_wifi_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_other_devices_category);
	}
	
/*	private boolean fetchEnabled(Context context)
	{
		if (super.isEnabled(context))
		{
			SharedPreferences prefs = Probe.getPreferences(context);

			return prefs.getBoolean("config_probe_wifi_enabled", WifiAccessPointsProbe.DEFAULT_ENABLED);
		}
		
		return false;
	} */

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);

		boolean enabled = prefs.getBoolean("config_probe_wifi_enabled", WifiAccessPointsProbe.DEFAULT_ENABLED);
		
		if (super.isEnabled(context) == false || enabled == false)
		{
			if (this._receiver != null)
			{
				context.unregisterReceiver(this._receiver);
				this._receiver = null;
			}

			return false;
		}

		final WifiAccessPointsProbe me = this;
		
		if (this._receiver == null)
		{
			this._receiver = new BroadcastReceiver()
			{
				public void onReceive(Context context, Intent intent)
				{
					if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction()))
					{
						WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

						List<ScanResult> results = wifi.getScanResults();

						Bundle bundle = new Bundle();

						bundle.putString("PROBE", me.name(context));
						bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

						ArrayList<Bundle> accessPoints = new ArrayList<Bundle>();

						if (results != null)
						{
							bundle.putInt(WifiAccessPointsProbe.ACCESS_POINT_COUNT, results.size());

							for (ScanResult result : results)
							{
								Bundle pointBundle = new Bundle();

								pointBundle.putString(WifiAccessPointsProbe.BSSID, result.BSSID);
								pointBundle.putString(WifiAccessPointsProbe.SSID, result.SSID);
								pointBundle.putString(WifiAccessPointsProbe.CAPABILITIES, result.capabilities);

								pointBundle.putInt(WifiAccessPointsProbe.FREQUENCY, result.frequency);
								pointBundle.putInt(WifiAccessPointsProbe.LEVEL, result.level);

								accessPoints.add(pointBundle);
							}

						}
						else
							bundle.putInt(WifiAccessPointsProbe.ACCESS_POINT_COUNT, 0);

						bundle.putParcelableArrayList(WifiAccessPointsProbe.ACCESS_POINTS, accessPoints);

						WifiInfo wifiInfo = wifi.getConnectionInfo();

						if (wifiInfo != null)
						{
							bundle.putString(WifiAccessPointsProbe.CURRENT_BSSID, wifiInfo.getBSSID());
							bundle.putString(WifiAccessPointsProbe.CURRENT_SSID, wifiInfo.getSSID());
							bundle.putInt(WifiAccessPointsProbe.CURRENT_LINK_SPEED, wifiInfo.getLinkSpeed());
							bundle.putInt(WifiAccessPointsProbe.CURRENT_RSSI, wifiInfo.getRssi());
						}

						me.transmitData(context, bundle);
					}
				}
			};

			IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			context.registerReceiver(this._receiver, filter);
		}

		long now = System.currentTimeMillis();
		
		synchronized(this)
		{
			long freq = Long.parseLong(prefs.getString("config_probe_wifi_frequency", Probe.DEFAULT_FREQUENCY));

			if (now - this._lastCheck > freq)
			{
				WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

				if (wifi.isWifiEnabled())
				{
					this._lastCheck = now;
					this._foundNetworks.clear();

					wifi.startScan();
				}
				else
				{
					Bundle bundle = new Bundle();

					bundle.putString("PROBE", me.name(context));
					bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
					bundle.putInt(WifiAccessPointsProbe.ACCESS_POINT_COUNT, 0);

					bundle.putParcelableArrayList(WifiAccessPointsProbe.ACCESS_POINTS, new ArrayList<Bundle>());

					bundle.putString(WifiAccessPointsProbe.CURRENT_BSSID, "");
					bundle.putString(WifiAccessPointsProbe.CURRENT_SSID, "");
					bundle.putInt(WifiAccessPointsProbe.CURRENT_LINK_SPEED, -1);
					bundle.putInt(WifiAccessPointsProbe.CURRENT_RSSI, -1);

					me.transmitData(context, bundle);
				}
			}
		}

		return true;
	}

	private Bundle bundleForNetworksArray(Context context, ArrayList<Bundle> objects)
	{
		Bundle bundle = new Bundle();
		
		if (objects == null)
			objects = new ArrayList<Bundle>();

		for (Bundle value : objects)
		{
			ArrayList<String> keys = new ArrayList<String>();

			String key = String.format(context.getString(R.string.display_wifi_network_title), value.getString(WifiAccessPointsProbe.SSID), value.getString(WifiAccessPointsProbe.BSSID));

			Bundle wifiBundle = new Bundle();

			wifiBundle.putString(context.getString(R.string.display_wifi_ssid_title), value.getString(WifiAccessPointsProbe.SSID));
			wifiBundle.putString(context.getString(R.string.display_wifi_bssid_title), value.getString(WifiAccessPointsProbe.BSSID));
			wifiBundle.putInt(context.getString(R.string.display_wifi_frequency_title), value.getInt(WifiAccessPointsProbe.FREQUENCY));
			wifiBundle.putInt(context.getString(R.string.display_wifi_level_title), value.getInt(WifiAccessPointsProbe.LEVEL));

			keys.add(context.getString(R.string.display_wifi_ssid_title));
			keys.add(context.getString(R.string.display_wifi_bssid_title));
			keys.add(context.getString(R.string.display_wifi_frequency_title));
			keys.add(context.getString(R.string.display_wifi_level_title));

			wifiBundle.putStringArrayList("KEY_ORDER", keys);

			bundle.putBundle(key, wifiBundle);
		}

		return bundle;
	}

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		@SuppressWarnings("unchecked")
		ArrayList<Bundle> array = (ArrayList<Bundle>) bundle.get(WifiAccessPointsProbe.ACCESS_POINTS);
		int count = (int) bundle.getDouble(WifiAccessPointsProbe.ACCESS_POINT_COUNT);

		Bundle devicesBundle = this.bundleForNetworksArray(context, array);

		formatted.putBundle(String.format(context.getString(R.string.display_wifi_networks_title), count), devicesBundle);
		
		formatted.putString(context.getString(R.string.display_current_ssid_title), bundle.getString(WifiAccessPointsProbe.CURRENT_SSID));
		formatted.putString(context.getString(R.string.display_current_bssid_title), bundle.getString(WifiAccessPointsProbe.CURRENT_BSSID));
		formatted.putInt(context.getString(R.string.display_current_speed_title), (int) bundle.getDouble(WifiAccessPointsProbe.CURRENT_LINK_SPEED));
		formatted.putInt(context.getString(R.string.display_current_rssi_title), (int) bundle.getDouble(WifiAccessPointsProbe.CURRENT_RSSI));

		return formatted;
	}
	
	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_wifi_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_wifi_enabled", false);
		
		e.commit();
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		int count = (int) bundle.getDouble(WifiAccessPointsProbe.ACCESS_POINT_COUNT);

		return String.format(context.getResources().getString(R.string.summary_wifi_probe), count);
	}

	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);

		long freq = Long.parseLong(prefs.getString("config_probe_wifi_frequency", Probe.DEFAULT_FREQUENCY));
		
		map.put(Probe.PROBE_FREQUENCY, freq);
		
		return map;
	}
	
	public void updateFromMap(Context context, Map<String, Object> params) 
	{
		super.updateFromMap(context, params);
		
		if (params.containsKey(Probe.PROBE_FREQUENCY))
		{
			Object frequency = params.get(Probe.PROBE_FREQUENCY);
			
			if (frequency instanceof Long)
			{
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putString("config_probe_wifi_frequency", frequency.toString());
				e.commit();
			}
		}
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_wifi_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_wifi_enabled");
		enabled.setDefaultValue(WifiAccessPointsProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_wifi_frequency");
		duration.setEntryValues(R.array.probe_satellite_frequency_values);
		duration.setEntries(R.array.probe_satellite_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);
		duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

		screen.addPreference(duration);

		return screen;
	}

	public String summary(Context context) 
	{
		return context.getString(R.string.summary_wifi_probe_desc);
	}
}
