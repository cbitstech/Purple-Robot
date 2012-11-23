package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class BatteryProbe extends Probe
{
	private boolean _isInited = false;
	private boolean _isEnabled = false;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.BatteryProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_battery_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public boolean isEnabled(Context context)
	{
		if (!this._isInited)
		{
			IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

			final BatteryProbe me = this;

			BroadcastReceiver receiver = new BroadcastReceiver()
			{
				public void onReceive(Context context, Intent intent)
				{
					if (me._isEnabled)
					{
						Bundle bundle = new Bundle();
						bundle.putString("PROBE", me.name(context));
						bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

						bundle.putAll(intent.getExtras());

						me.transmitData(context, bundle);
					}
				}
			};

			context.registerReceiver(receiver, filter);

			this._isInited = true;
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		 this._isEnabled = false;

		if (super.isEnabled(context))
		{
			if (prefs.getBoolean("config_probe_battery_enabled", true))
				this._isEnabled = true;
		}

		return this._isEnabled;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		String status = this.getStatus(context, bundle.getInt(BatteryManager.EXTRA_STATUS));

		int level = bundle.getInt(BatteryManager.EXTRA_LEVEL);

		return String.format(context.getResources().getString(R.string.summary_battery_probe), level, status);
	}

	private String getStatus(Context context, int statusInt)
	{
		switch (statusInt)
		{
			case BatteryManager.BATTERY_STATUS_CHARGING:
				return context.getString(R.string.label_battery_charging);
			case BatteryManager.BATTERY_STATUS_DISCHARGING:
				return context.getString(R.string.label_battery_discharging);
			case BatteryManager.BATTERY_STATUS_FULL:
				return context.getString(R.string.label_battery_full);
			case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
				return context.getString(R.string.label_battery_not_charging);
		}

		return context.getString(R.string.label_unknown);
	}

	/*

	public Bundle formattedBundle(Context context, Bundle bundle)
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
		enabled.setKey("config_probe_battery_enabled");
		enabled.setDefaultValue(true);

		screen.addPreference(enabled);

		return screen;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO Auto-generated method stub
	}
}
