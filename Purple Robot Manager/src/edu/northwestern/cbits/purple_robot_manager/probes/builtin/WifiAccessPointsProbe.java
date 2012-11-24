package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

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

						bundle.putInt(WifiAccessPointsProbe.ACCESS_POINT_COUNT, results.size());

						ArrayList<Bundle> accessPoints = new ArrayList<Bundle>();

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
		}

		IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

		long now = System.currentTimeMillis();

		boolean enabled = super.isEnabled(context);

		if (enabled)
			enabled = prefs.getBoolean("config_probe_wifi_enabled", true);

		if (enabled)
		{
			synchronized(this)
			{
				long freq = Long.parseLong(prefs.getString("config_probe_wifi_frequency", "300000"));

				if (now - this._lastCheck > freq)
				{
					WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

					if (wifi.isWifiEnabled())
					{
						context.registerReceiver(this._receiver, filter);

						this._lastCheck = now;
						this._foundNetworks.clear();

						wifi.startScan();
					}
					else
						context.unregisterReceiver(this._receiver);
				}
			}

			return true;
		}

		context.unregisterReceiver(this._receiver);

		return false;
	}


	public String summarizeValue(Context context, Bundle bundle)
	{
		int count = bundle.getInt(WifiAccessPointsProbe.ACCESS_POINT_COUNT);

		return String.format(context.getResources().getString(R.string.summary_wifi_probe), count);
	}

/*	private Bundle bundleForNetworksArray(Context context, ArrayList<Bundle> objects)
	{
		Bundle bundle = new Bundle();

		for (Bundle value : objects)
		{
			ArrayList<String> keys = new ArrayList<String>();

			String key = String.format(context.getString(R.string.display), value.getString(WifiAccessPointsProbe.NAME), value.getString(WifiAccessPointsProbe.ADDRESS));

			Bundle apBundle = new Bundle();


			apBundle.putStringArrayList("KEY_ORDER", keys);

			bundle.putBundle(key, apBundle);
		}

		return bundle;
	}

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		@SuppressWarnings("unchecked")
		ArrayList<Bundle> array = (ArrayList<Bundle>) bundle.get(WifiAccessPointsProbe.DEVICES);
		int count = bundle.getInt(WifiAccessPointsProbe.DEVICES_COUNT);

		Bundle devicesBundle = this.bundleForDevicesArray(context, array);

		formatted.putBundle(String.format(context.getString(R.string.display_bluetooth_devices_title), count), devicesBundle);

		return formatted;
	};
*/

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_wifi_enabled");
		enabled.setDefaultValue(true);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_wifi_frequency");
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
