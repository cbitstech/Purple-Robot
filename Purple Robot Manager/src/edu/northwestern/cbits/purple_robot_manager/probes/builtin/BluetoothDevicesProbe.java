package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;
import java.util.Map;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class BluetoothDevicesProbe extends Probe
{
	protected static final String NAME = "BLUETOOTH_NAME";
	protected static final String ADDRESS = "BLUETOOTH_ADDRESS";
	protected static final String MAJOR_CLASS = "DEVICE MAJOR CLASS";
	protected static final String MINOR_CLASS = "DEVICE MINOR CLASS";
	protected static final String BOND_STATE = "BOND_STATE";
	protected static final String DEVICES_COUNT = "DEVICE_COUNT";
	protected static final String DEVICES = "DEVICES";
	
	private static final boolean DEFAULT_ENABLED = false;

	private long _lastCheck = 0;
	private BroadcastReceiver _receiver = null;

	private BluetoothAdapter _adapter = null;

	private ArrayList<Bundle> _foundDevices = new ArrayList<Bundle>();

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.BluetoothDevicesProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_bluetooth_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_other_devices_category);
	}

	public static String majorDeviceClass(int majorClass)
	{
		String deviceClassName = "Unknown";

		switch(majorClass)
		{
			case BluetoothClass.Device.Major.AUDIO_VIDEO:
				deviceClassName = "Audio/Video";
				break;
			case BluetoothClass.Device.Major.COMPUTER:
				deviceClassName = "Computer";
				break;
			case BluetoothClass.Device.Major.HEALTH:
				deviceClassName = "Health";
				break;
			case BluetoothClass.Device.Major.IMAGING:
				deviceClassName = "Imaging";
				break;
			case BluetoothClass.Device.Major.MISC:
				deviceClassName = "Miscellaneous";
				break;
			case BluetoothClass.Device.Major.NETWORKING:
				deviceClassName = "Networking";
				break;
			case BluetoothClass.Device.Major.PERIPHERAL:
				deviceClassName = "Peripheral";
				break;
			case BluetoothClass.Device.Major.PHONE:
				deviceClassName = "Phone";
				break;
			case BluetoothClass.Device.Major.TOY:
				deviceClassName = "Toy";
				break;
			case BluetoothClass.Device.Major.UNCATEGORIZED:
				deviceClassName = "Uncategorized";
				break;
			case BluetoothClass.Device.Major.WEARABLE:
				deviceClassName = "Wearable";
				break;
		}

		return String.format("0x%08x %s", majorClass, deviceClassName);
	}

	public static String minorDeviceClass(int minorClass)
	{
		String deviceClassName = "Unknown";

		switch(minorClass)
		{
			case BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER:
				deviceClassName = "Camcorder";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO:
				deviceClassName = "Car Audio";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:
				deviceClassName = "Handsfree";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
				deviceClassName = "Headphones";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO:
				deviceClassName = "HiFi Audio";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
				deviceClassName = "Loudspeaker";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE:
				deviceClassName = "Microphone";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO:
				deviceClassName = "Portable Audio";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX:
				deviceClassName = "Set-Top Box";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED:
				deviceClassName = "Uncategorized";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_VCR:
				deviceClassName = "VCR";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA:
				deviceClassName = "Video Camera";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING:
				deviceClassName = "Video Conferencing";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER:
				deviceClassName = "Display & Loudspeaker";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY:
				deviceClassName = "Gaming Toy";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR:
				deviceClassName = "Video Monitor";
				break;
			case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
				deviceClassName = "Wearable Headset";
				break;
			case BluetoothClass.Device.COMPUTER_DESKTOP:
				deviceClassName = "Desktop";
				break;
			case BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA:
				deviceClassName = "Handheld PC or PDA";
				break;
			case BluetoothClass.Device.COMPUTER_LAPTOP:
				deviceClassName = "Laptop";
				break;
			case BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA:
				deviceClassName = "Palm-Size PC or PDA";
				break;
			case BluetoothClass.Device.COMPUTER_SERVER:
				deviceClassName = "Server";
				break;
			case BluetoothClass.Device.COMPUTER_UNCATEGORIZED:
				deviceClassName = "Uncategorized";
				break;
			case BluetoothClass.Device.COMPUTER_WEARABLE:
				deviceClassName = "Wearable";
				break;
			case BluetoothClass.Device.HEALTH_BLOOD_PRESSURE:
				deviceClassName = "Blood Pressure";
				break;
			case BluetoothClass.Device.HEALTH_DATA_DISPLAY:
				deviceClassName = "Data Display";
				break;
			case BluetoothClass.Device.HEALTH_GLUCOSE:
				deviceClassName = "Glucose";
				break;
			case BluetoothClass.Device.HEALTH_PULSE_OXIMETER:
				deviceClassName = "Oximeter";
				break;
			case BluetoothClass.Device.HEALTH_PULSE_RATE:
				deviceClassName = "Pulse Rate";
				break;
			case BluetoothClass.Device.HEALTH_THERMOMETER:
				deviceClassName = "Thermometer";
				break;
			case BluetoothClass.Device.HEALTH_UNCATEGORIZED:
				deviceClassName = "Uncategorized";
				break;
			case BluetoothClass.Device.HEALTH_WEIGHING:
				deviceClassName = "Weighing";
				break;
			case BluetoothClass.Device.PHONE_CELLULAR:
				deviceClassName = "Cellular";
				break;
			case BluetoothClass.Device.PHONE_CORDLESS:
				deviceClassName = "Cordless";
				break;
			case BluetoothClass.Device.PHONE_ISDN:
				deviceClassName = "ISDN";
				break;
			case BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY:
				deviceClassName = "Modem or Gateway";
				break;
			case BluetoothClass.Device.PHONE_SMART:
				deviceClassName = "Smartphone";
				break;
			case BluetoothClass.Device.PHONE_UNCATEGORIZED:
				deviceClassName = "Uncategorized";
				break;
			case BluetoothClass.Device.TOY_CONTROLLER:
				deviceClassName = "Controller";
				break;
			case BluetoothClass.Device.TOY_DOLL_ACTION_FIGURE:
				deviceClassName = "Doll or Action Figure";
				break;
			case BluetoothClass.Device.TOY_GAME:
				deviceClassName = "Game";
				break;
			case BluetoothClass.Device.TOY_ROBOT:
				deviceClassName = "Robot";
				break;
			case BluetoothClass.Device.TOY_UNCATEGORIZED:
				deviceClassName = "Uncategorized";
				break;
			case BluetoothClass.Device.TOY_VEHICLE:
				deviceClassName = "Vehicle";
				break;
			case BluetoothClass.Device.WEARABLE_GLASSES:
				deviceClassName = "Glasses";
				break;
			case BluetoothClass.Device.WEARABLE_HELMET:
				deviceClassName = "Helmet";
				break;
			case BluetoothClass.Device.WEARABLE_JACKET:
				deviceClassName = "Jacket";
				break;
			case BluetoothClass.Device.WEARABLE_PAGER:
				deviceClassName = "Pager";
				break;
			case BluetoothClass.Device.WEARABLE_UNCATEGORIZED:
				deviceClassName = "Uncategorized";
				break;
			case BluetoothClass.Device.WEARABLE_WRIST_WATCH:
				deviceClassName = "Wrist Watch";
				break;
		}

		return String.format("0x%08x %s", minorClass, deviceClassName);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_bluetooth_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_bluetooth_enabled", false);
		
		e.commit();
	}

	@SuppressLint("InlinedApi")
	public boolean isEnabled(Context context)
	{
		final SharedPreferences prefs = Probe.getPreferences(context);
		final EncryptionManager em = EncryptionManager.getInstance();
		
		final BluetoothDevicesProbe me = this;

		if (this._receiver == null)
		{
			this._receiver = new BroadcastReceiver()
			{
				@SuppressWarnings("unchecked")
				public void onReceive(Context context, Intent intent)
				{
					if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction()))
					{
						boolean doHash = prefs.getBoolean("config_probe_bluetooth_hash_data", Probe.DEFAULT_HASH_DATA);

						BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

						Bundle deviceBundle = new Bundle();

						if (doHash)
						{
							deviceBundle.putString(BluetoothDevicesProbe.NAME, em.createHash(context, device.getName()));
							deviceBundle.putString(BluetoothDevicesProbe.ADDRESS, em.createHash(context, device.getAddress()));
						}
						else
						{
							deviceBundle.putString(BluetoothDevicesProbe.NAME, device.getName());
							deviceBundle.putString(BluetoothDevicesProbe.ADDRESS, device.getAddress());
						}

						deviceBundle.putString(BluetoothDevicesProbe.BOND_STATE, BluetoothDevicesProbe.bondState(device.getBondState()));

						BluetoothClass deviceClass = device.getBluetoothClass();

						deviceBundle.putString(BluetoothDevicesProbe.MAJOR_CLASS, BluetoothDevicesProbe.majorDeviceClass(deviceClass.getMajorDeviceClass()));
						deviceBundle.putString(BluetoothDevicesProbe.MINOR_CLASS, BluetoothDevicesProbe.minorDeviceClass(deviceClass.getDeviceClass()));

						me._foundDevices.add(deviceBundle);
					}
					else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction()))
					{
						Bundle bundle = new Bundle();

						bundle.putString("PROBE", me.name(context));
						bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
						bundle.putParcelableArrayList(BluetoothDevicesProbe.DEVICES, (ArrayList<Bundle>) me._foundDevices.clone());
						bundle.putInt(BluetoothDevicesProbe.DEVICES_COUNT, me._foundDevices.size());

						synchronized(me)
						{
							me.transmitData(context, bundle);
						}
					}
					else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction()))
						me._lastCheck = 0;
				}
			};

			IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
			filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
			filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
			filter.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
			filter.addAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			filter.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
			filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);

			context.registerReceiver(this._receiver, filter);
		}

		long now = System.currentTimeMillis();

		boolean enabled = super.isEnabled(context);

		if (enabled)
			enabled = prefs.getBoolean("config_probe_bluetooth_enabled", BluetoothDevicesProbe.DEFAULT_ENABLED);

		if (enabled)
		{
			synchronized(this)
			{
				try
				{
					long freq = Long.parseLong(prefs.getString("config_probe_bluetooth_frequency", Probe.DEFAULT_FREQUENCY));
	
					if (now - this._lastCheck > freq)
					{
						if (this._adapter == null)
							this._adapter = BluetoothAdapter.getDefaultAdapter();
	
						if (this._adapter != null && this._adapter.isEnabled())
						{
							this._foundDevices.clear();
							
							this._adapter.startDiscovery();
						}
	
						this._lastCheck = now;
					}
				}
				catch (SecurityException e)
				{
					LogManager.getInstance(context).logException(e);
				}
			}

			return true;
		}
		else
		{
			if (this._adapter != null)
			{
				this._adapter.cancelDiscovery();

				this._adapter = null;
			}
		}

		return false;
	}

	protected static String bondState(int bondState)
	{
		switch (bondState)
		{
			case BluetoothDevice.BOND_BONDED:
				return "Paired";
			case BluetoothDevice.BOND_BONDING:
				return "Pairing";
			case BluetoothDevice.BOND_NONE:
				return "Not Paired";
		}

		return "Unknown or Error";
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		int count = (int) bundle.getDouble(BluetoothDevicesProbe.DEVICES_COUNT);

		return String.format(context.getResources().getString(R.string.summary_bluetooth_probe), count);
	}

	private Bundle bundleForDevicesArray(Context context, ArrayList<Bundle> objects)
	{
		Bundle bundle = new Bundle();
		
		if (objects == null)
			return bundle;

		for (Bundle value : objects)
		{
			ArrayList<String> keys = new ArrayList<String>();

			String key = String.format(context.getString(R.string.display_bluetooth_device_title), value.getString(BluetoothDevicesProbe.NAME), value.getString(BluetoothDevicesProbe.ADDRESS));

			Bundle deviceBundle = new Bundle();

			deviceBundle.putString(context.getString(R.string.display_bluetooth_device_title_label), value.getString(BluetoothDevicesProbe.NAME));
			deviceBundle.putString(context.getString(R.string.display_bluetooth_device_address_label), value.getString(BluetoothDevicesProbe.ADDRESS));
			deviceBundle.putString(context.getString(R.string.display_bluetooth_device_pair), value.getString(BluetoothDevicesProbe.BOND_STATE));
			deviceBundle.putString(context.getString(R.string.display_bluetooth_device_major), value.getString(BluetoothDevicesProbe.MAJOR_CLASS));
			deviceBundle.putString(context.getString(R.string.display_bluetooth_device_minor), value.getString(BluetoothDevicesProbe.MINOR_CLASS));

			keys.add(context.getString(R.string.display_bluetooth_device_title_label));
			keys.add(context.getString(R.string.display_bluetooth_device_address_label));
			keys.add(context.getString(R.string.display_bluetooth_device_pair));
			keys.add(context.getString(R.string.display_bluetooth_device_major));
			keys.add(context.getString(R.string.display_bluetooth_device_minor));

			deviceBundle.putStringArrayList("KEY_ORDER", keys);

			bundle.putBundle(key, deviceBundle);
		}

		return bundle;
	}

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		@SuppressWarnings("unchecked")
		ArrayList<Bundle> array = (ArrayList<Bundle>) bundle.get(BluetoothDevicesProbe.DEVICES);
		int count = (int) bundle.getDouble(BluetoothDevicesProbe.DEVICES_COUNT);

		Bundle devicesBundle = this.bundleForDevicesArray(context, array);

		formatted.putBundle(String.format(context.getString(R.string.display_bluetooth_devices_title), count), devicesBundle);

		return formatted;
	};

	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);
		
		// TODO: include whether enabled?

		long freq = Long.parseLong(prefs.getString("config_probe_bluetooth_frequency", Probe.DEFAULT_FREQUENCY));
		map.put(Probe.PROBE_FREQUENCY, freq);
		
		boolean hash = prefs.getBoolean("config_probe_bluetooth_hash_data", Probe.DEFAULT_HASH_DATA);
		map.put(Probe.HASH_DATA, hash);

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
				
				e.putString("config_probe_bluetooth_frequency", frequency.toString());
				e.commit();
			}
		}

		if (params.containsKey(Probe.HASH_DATA))
		{
			Object hash = params.get(Probe.HASH_DATA);
			
			if (hash instanceof Boolean)
			{
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putBoolean("config_probe_bluetooth_hash_data", ((Boolean) hash).booleanValue());
				e.commit();
			}
		}
	}

	public String summary(Context context) 
	{
		return context.getString(R.string.summary_bluetooth_probe_desc);
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_bluetooth_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_bluetooth_enabled");
		enabled.setDefaultValue(BluetoothDevicesProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_bluetooth_frequency");
		duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);
		duration.setEntryValues(R.array.probe_satellite_frequency_values);
		duration.setEntries(R.array.probe_satellite_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);

		screen.addPreference(duration);

		CheckBoxPreference hash = new CheckBoxPreference(activity);
		hash.setKey("config_probe_bluetooth_hash_data");
		hash.setDefaultValue(Probe.DEFAULT_HASH_DATA);
		hash.setTitle(R.string.config_probe_bluetooth_hash_title);
		hash.setSummary(R.string.config_probe_bluetooth_hash_summary);

		screen.addPreference(hash);

		return screen;
	}
}
