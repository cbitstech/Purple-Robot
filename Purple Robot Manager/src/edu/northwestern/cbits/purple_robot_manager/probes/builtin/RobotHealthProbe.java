package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;

import com.WazaBe.HoloEverywhere.preference.CheckBoxPreference;
import com.WazaBe.HoloEverywhere.preference.ListPreference;
import com.WazaBe.HoloEverywhere.preference.PreferenceManager;
import com.WazaBe.HoloEverywhere.preference.PreferenceScreen;
import com.WazaBe.HoloEverywhere.preference.SharedPreferences;
import com.WazaBe.HoloEverywhere.sherlock.SPreferenceActivity;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.plugins.HttpUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPluginManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class RobotHealthProbe extends Probe
{
	private static final String PENDING_COUNT = "PENDING_COUNT";
	private static final String PENDING_SIZE = "PENDING_SIZE";
	private static final String ARCHIVE_COUNT = "ARCHIVE_COUNT";
	private static final String ARCHIVE_SIZE = "ARCHIVE_SIZE";
	private static final String THROUGHPUT = "THROUGHPUT";
	private static final String CLEAR_TIME = "CLEAR_TIME";

	private long _lastCheck = 0;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.RobotHealthProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_builtin_robot_probe);
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
			if (prefs.getBoolean("config_probe_robot_enabled", true))
			{
				synchronized(this)
				{
					long freq = Long.parseLong(prefs.getString("config_probe_robot_frequency", "60000"));

					if (now - this._lastCheck  > freq)
					{
						OutputPlugin plugin = OutputPluginManager.sharedInstance.pluginForClass(HttpUploadPlugin.class);

						if (plugin != null && plugin instanceof HttpUploadPlugin)
						{
							HttpUploadPlugin httpPlugin = (HttpUploadPlugin) plugin;

							File archiveFolder = httpPlugin.getArchiveFolder();
							File pendingFolder = httpPlugin.getPendingFolder();

							int pendingCount = 0;
							int archiveCount = 0;

							long pendingSize = 0;
							long archiveSize = 0;

							for (File f : archiveFolder.listFiles())
							{
								if (f.isFile())
								{
									archiveCount += 1;
									archiveSize += f.length();
								}
							}

							for (File f : pendingFolder.listFiles())
							{
								if (f.isFile())
								{
									pendingCount += 1;
									pendingSize += f.length();
								}
							}

							Bundle bundle = new Bundle();
							bundle.putString("PROBE", this.name(context));
							bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

							bundle.putInt(RobotHealthProbe.PENDING_COUNT, pendingCount);
							bundle.putLong(RobotHealthProbe.PENDING_SIZE, pendingSize);

							bundle.putInt(RobotHealthProbe.ARCHIVE_COUNT, archiveCount);
							bundle.putLong(RobotHealthProbe.ARCHIVE_SIZE, archiveSize);

							double throughput = httpPlugin.getRecentThroughput();

							bundle.putDouble(RobotHealthProbe.THROUGHPUT, throughput);

							long cleartime = -1;

							if (throughput > 0.0)
								cleartime = pendingSize / ((long) throughput);

							bundle.putLong(RobotHealthProbe.CLEAR_TIME, cleartime);

							this.transmitData(context, bundle);
						}

						this._lastCheck = now;
					}
				}
			}

			return true;
		}

		return false;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		int count = bundle.getInt(RobotHealthProbe.PENDING_COUNT);
		double size = 0.0 + bundle.getLong(RobotHealthProbe.PENDING_SIZE);

		long clear = bundle.getLong(RobotHealthProbe.CLEAR_TIME);

		if (clear < 0)
			clear = 0;

		size = size / (1024 * 1024);

		return String.format(context.getResources().getString(R.string.summary_robot_probe), count, size, clear);
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
