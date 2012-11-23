package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class HardwareInformationProbe extends Probe
{
	private static final String BOARD = "BOARD";
	private static final String BOOTLOADER = "BOOTLOADER";
	private static final String BRAND = "BRAND";
	private static final String DEVICE = "DEVICE";
	private static final String DISPLAY = "DISPLAY";
	private static final String FINGERPRINT = "FINGERPRINT";
	private static final String HARDWARE = "HARDWARE";
	private static final String HOST = "HOST";
	private static final String ID = "ID";
	private static final String MANUFACTURER = "MANUFACTURER";
	private static final String MODEL = "MODEL";
	private static final String PRODUCT = "PRODUCT";
	private static final String SERIAL = "SERIAL";
	private static final String WIFI_MAC = "WIFI_MAC";
	private static final String BLUETOOTH_MAC = "BLUETOOTH_MAC";
	private static final String MOBILE_ID = "MOBILE_ID";

	private long _lastCheck = 0;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.HardwareInformationProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_builtin_hardware_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		long now = System.currentTimeMillis();

		if (super.isEnabled(context))
		{
			if (prefs.getBoolean("config_probe_hardware_enabled", true))
			{
				synchronized(this)
				{
					long freq = Long.parseLong(prefs.getString("config_probe_hardware_frequency", "300000"));

					if (now - this._lastCheck  > freq)
					{
						Bundle bundle = new Bundle();
						bundle.putString("PROBE", this.name(context));
						bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

						bundle.putString(HardwareInformationProbe.BOARD, Build.BOARD);
						bundle.putString(HardwareInformationProbe.BOOTLOADER, Build.BOOTLOADER);
						bundle.putString(HardwareInformationProbe.BRAND, Build.BRAND);
						bundle.putString(HardwareInformationProbe.DEVICE, Build.DEVICE);
						bundle.putString(HardwareInformationProbe.DISPLAY, Build.DISPLAY);
						bundle.putString(HardwareInformationProbe.FINGERPRINT, Build.FINGERPRINT);
						bundle.putString(HardwareInformationProbe.HARDWARE, Build.HARDWARE);
						bundle.putString(HardwareInformationProbe.HOST, Build.HOST);
						bundle.putString(HardwareInformationProbe.ID, Build.ID);
						bundle.putString(HardwareInformationProbe.MANUFACTURER, Build.MANUFACTURER);
						bundle.putString(HardwareInformationProbe.MODEL, Build.MODEL);
						bundle.putString(HardwareInformationProbe.PRODUCT, Build.PRODUCT);
						bundle.putString(HardwareInformationProbe.SERIAL, Build.SERIAL);

						WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
						bundle.putString(HardwareInformationProbe.WIFI_MAC, wifi.getConnectionInfo().getMacAddress());

						TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
						bundle.putString(HardwareInformationProbe.MOBILE_ID, tm.getDeviceId());

						BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

						if (adapter != null)
							bundle.putString(HardwareInformationProbe.BLUETOOTH_MAC, adapter.getAddress());
						else
							bundle.putString(HardwareInformationProbe.BLUETOOTH_MAC, "");

						this.transmitData(context, bundle);

						this._lastCheck = now;
					}
				}

				return true;
			}
		}

		return false;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		String model = bundle.getString(HardwareInformationProbe.MODEL);
		String wifi = bundle.getString(HardwareInformationProbe.WIFI_MAC);

		return String.format(context.getResources().getString(R.string.summary_hardware_info_probe), model, wifi);
	}

/*	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		@SuppressWarnings("unchecked")
		ArrayList<Bundle> array = (ArrayList<Bundle>) bundle.get(HardwareInformationProbe.DEVICES);
		int count = bundle.getInt(HardwareInformationProbe.DEVICES_COUNT);

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
		enabled.setKey("config_probe_hardware_enabled");
		enabled.setDefaultValue(true);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_hardware_frequency");
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
