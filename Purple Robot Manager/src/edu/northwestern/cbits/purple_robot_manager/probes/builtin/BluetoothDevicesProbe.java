package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.WazaBe.HoloEverywhere.preference.PreferenceManager;
import com.WazaBe.HoloEverywhere.preference.PreferenceScreen;
import com.WazaBe.HoloEverywhere.preference.SharedPreferences;
import com.WazaBe.HoloEverywhere.sherlock.SPreferenceActivity;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class BluetoothDevicesProbe extends Probe
{
	private long _lastCheck = 0;

	private BluetoothAdapter _adapter = null;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.BluetoothDevicesProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_builtin_bluetooth_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public Bundle[] dataRequestBundles(Context context)
	{
		return new Bundle[0];
	}

	public PreferenceScreen preferenceScreen(SPreferenceActivity settingsActivity)
	{
		return new PreferenceScreen(settingsActivity, null);
	}

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		long now = System.currentTimeMillis();

		if (prefs.getBoolean("config_probe_bluetooth_enabled", true))
		{
			synchronized(this)
			{
				long freq = Long.parseLong(prefs.getString("config_probe_bluetooth_frequency", "60000"));

				Log.e("PRM", "BT IS ENABLED " + (now - this._lastCheck > freq) + " " + this._adapter);

				if (now - this._lastCheck > freq)
				{
					if (this._adapter == null)
					{
						final BluetoothDevicesProbe me = this;

						BroadcastReceiver bluetoothReceiver = new BroadcastReceiver()
						{
							public void onReceive(Context context, Intent intent)
							{
								Log.e("PRM", "BT INTENT: " + intent.getAction());

								if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction()))
								{
									BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

									Log.e("PRM", "FOUND " + device.getName() + " " + device.getAddress() + " " + device.getBluetoothClass().getDeviceClass());
								}
								else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction()))
								{
									context.unregisterReceiver(this);

									Log.e("PRM", "FINISHED");

									me._adapter = null;
								}
							}
						};

						IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
						context.registerReceiver(bluetoothReceiver, filter);

						filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
						context.registerReceiver(bluetoothReceiver, filter);

						this._lastCheck = now;

						this._adapter = BluetoothAdapter.getDefaultAdapter();

						this._adapter.startDiscovery();

						Log.e("PRM", "BT SCAN START");
					}
				}
			}

			return true;
		}

		return false;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO Auto-generated method stub
	}
}
