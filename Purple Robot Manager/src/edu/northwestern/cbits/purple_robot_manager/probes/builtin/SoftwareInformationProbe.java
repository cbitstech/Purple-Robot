package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
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

	private long _lastCheck = 0;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.SoftwareInformationProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_software_info_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_software_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_software_enabled", false);
		
		e.commit();
	}

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		if (super.isEnabled(context))
		{
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
		String release = bundle.getString(SoftwareInformationProbe.RELEASE);
		int count = bundle.getInt(SoftwareInformationProbe.INSTALLED_APP_COUNT);

		return String.format(context.getResources().getString(R.string.summary_software_info_probe), release, count);
	}
	
	private Bundle bundleForAppArray(Context context, ArrayList<Bundle> objects)
	{
		Bundle bundle = new Bundle();
		
		ArrayList<String> keys = new ArrayList<String>();

		for (int i = 0; i < objects.size(); i++)
		{
			Bundle value = objects.get(i);
			String name = value.getString(SoftwareInformationProbe.APP_NAME);
			String key = value.getString(SoftwareInformationProbe.PACKAGE_NAME);

			keys.add(key);
			bundle.putString(key, name);
		}
		
		bundle.putStringArrayList("KEY_ORDER", keys);

		return bundle;
	}

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		@SuppressWarnings("unchecked")
		ArrayList<Bundle> array = (ArrayList<Bundle>) bundle.get(SoftwareInformationProbe.INSTALLED_APPS);
		int count = bundle.getInt(SoftwareInformationProbe.INSTALLED_APP_COUNT);

		Bundle appsBundle = this.bundleForAppArray(context, array);

		formatted.putBundle(String.format(context.getString(R.string.display_installed_apps_title), count), appsBundle);
		formatted.putString(context.getString(R.string.display_android_version_title), bundle.getString(SoftwareInformationProbe.RELEASE));

		return formatted;
	};

	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_software_info_probe_desc);

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
