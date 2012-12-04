package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class RunningSoftwareProbe extends Probe
{
	private static final String PACKAGE_NAME = "PACKAGE_NAME";
	private static final String RUNNING_TASKS = "RUNNING_TASKS";
	private static final String RUNNING_TASK_COUNT = "RUNNING_TASK_COUNT";

	private long _lastCheck = 0;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.RunningSoftwareProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_running_software_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		if (super.isEnabled(context))
		{
			long now = System.currentTimeMillis();

			if (prefs.getBoolean("config_probe_running_software_enabled", true))
			{
				synchronized(this)
				{
					long freq = Long.parseLong(prefs.getString("config_probe_running_software_frequency", "300000"));

					if (now - this._lastCheck  > freq)
					{
						Bundle bundle = new Bundle();
						bundle.putString("PROBE", this.name(context));
						bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

						ActivityManager am = (ActivityManager) context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
						ArrayList<RunningTaskInfo> tasks = new ArrayList<RunningTaskInfo>(am.getRunningTasks(9999));

						ArrayList<Bundle> running = new ArrayList<Bundle>();

						for (RunningTaskInfo info : tasks)
						{
							Bundle taskBundle = new Bundle();

							taskBundle.putString(RunningSoftwareProbe.PACKAGE_NAME, info.baseActivity.getPackageName());

							running.add(taskBundle);
						}

						bundle.putParcelableArrayList(RunningSoftwareProbe.RUNNING_TASKS, running);
						bundle.putInt(RunningSoftwareProbe.RUNNING_TASK_COUNT, running.size());

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
		int count = bundle.getInt(RunningSoftwareProbe.RUNNING_TASK_COUNT);

		return String.format(context.getResources().getString(R.string.summary_running_software_probe), count);
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

	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_running_software_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_running_software_enabled");
		enabled.setDefaultValue(true);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_running_software_frequency");
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
