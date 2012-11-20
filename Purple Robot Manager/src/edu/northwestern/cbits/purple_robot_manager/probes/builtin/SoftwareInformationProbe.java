package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.WazaBe.HoloEverywhere.preference.CheckBoxPreference;
import com.WazaBe.HoloEverywhere.preference.ListPreference;
import com.WazaBe.HoloEverywhere.preference.PreferenceManager;
import com.WazaBe.HoloEverywhere.preference.PreferenceScreen;
import com.WazaBe.HoloEverywhere.preference.SharedPreferences;
import com.WazaBe.HoloEverywhere.sherlock.SPreferenceActivity;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class SoftwareInformationProbe extends Probe
{
	private static final String CODENAME = "CODENAME";
	private static final String INCREMENTAL = "INCREMENTAL";
	private static final String RELEASE = "RELEASE";
	private static final String SDK_INT = "SDK_INT";
	private static final String APP_NAME = "APP_NAME";
	private static final String PACKAGE_NAME = "PACKAGE_NAME";
	private static final String INSTALLED_APPS = "INSTALLED_APPS";
	private static final String INSTALLED_APP_COUNT = "INSTALLED_APP_COUNT";
	private static final String RUNNING_TASKS = "RUNNING_TASKS";
	private static final String RUNNING_TASK_COUNT = "RUNNING_TASK_COUNT";

	private long _lastCheck = 0;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.SoftwareInformationProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_builtin_software_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		long now = System.currentTimeMillis();

		if (prefs.getBoolean("config_probe_software_enabled", true))
		{
			synchronized(this)
			{
				long freq = Long.parseLong(prefs.getString("config_probe_software_frequency", "300000"));

				if (now - this._lastCheck  > freq)
				{
					Bundle bundle = new Bundle();
					bundle.putString("PROBE", this.name(context));
					bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

					bundle.putString(SoftwareInformationProbe.CODENAME, Build.VERSION.CODENAME);
					bundle.putString(SoftwareInformationProbe.INCREMENTAL, Build.VERSION.INCREMENTAL);
					bundle.putString(SoftwareInformationProbe.RELEASE, Build.VERSION.RELEASE);
					bundle.putInt(SoftwareInformationProbe.SDK_INT, Build.VERSION.SDK_INT);

					PackageManager pm = context.getApplicationContext().getPackageManager();

					List<ApplicationInfo> infos = pm.getInstalledApplications(0);

					ArrayList<Bundle> installed = new ArrayList<Bundle>();

					for (ApplicationInfo info : infos)
					{
						Bundle appBundle = new Bundle();

						appBundle.putString(SoftwareInformationProbe.APP_NAME, info.loadLabel(pm).toString());
						appBundle.putString(SoftwareInformationProbe.PACKAGE_NAME, info.packageName);

						installed.add(appBundle);
					}

					bundle.putParcelableArrayList(SoftwareInformationProbe.INSTALLED_APPS, installed);

					bundle.putInt(SoftwareInformationProbe.INSTALLED_APP_COUNT, installed.size());

					ActivityManager am = (ActivityManager) context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
					ArrayList<RunningTaskInfo> tasks = new ArrayList<RunningTaskInfo>(am.getRunningTasks(9999));

					ArrayList<Bundle> running = new ArrayList<Bundle>();

					for (RunningTaskInfo info : tasks)
					{
						Bundle taskBundle = new Bundle();

						taskBundle.putString(SoftwareInformationProbe.PACKAGE_NAME, info.baseActivity.getPackageName());

						running.add(taskBundle);
					}

					bundle.putParcelableArrayList(SoftwareInformationProbe.RUNNING_TASKS, running);
					bundle.putInt(SoftwareInformationProbe.RUNNING_TASK_COUNT, running.size());

					this.transmitData(context, bundle);

					this._lastCheck = now;
				}
			}

			return true;
		}

		return false;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		String release = bundle.getString(SoftwareInformationProbe.RELEASE);
		int count = bundle.getInt(SoftwareInformationProbe.INSTALLED_APP_COUNT);

		return String.format(context.getResources().getString(R.string.summary_software_info_probe), release, count);
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
	public PreferenceScreen preferenceScreen(SPreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_software_enabled");
		enabled.setDefaultValue(true);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_software_frequency");
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
