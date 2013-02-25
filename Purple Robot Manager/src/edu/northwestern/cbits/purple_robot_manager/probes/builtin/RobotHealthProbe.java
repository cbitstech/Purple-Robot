package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import edu.northwestern.cbits.purple_robot_manager.ManagerService;
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
	protected static final String APP_VERSION_NAME = "APP_VERSION_NAME";
	protected static final String APP_VERSION_CODE = "APP_VERSION_CODE";
	protected static final String ACTIVE_RUNTIME = "ACTIVE_RUNTIME";

	private long _lastCheck = 0;
	private boolean _checking = false;

	private static final long NTP_CHECK_DURATION = 300000;
	private static final String NTP_HOST = "0.north-america.pool.ntp.org";
	protected static final String TIME_OFFSET_MS = "TIME_OFFSET_MS";
	protected static final String CPU_USAGE = "CPU_USAGE";
	
	private long _lastOffset = 0;
	private long _lastTimeCheck = 0;
	
	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.RobotHealthProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_robot_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_robot_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_robot_enabled", false);
		
		e.commit();
	}

	// Source: http://stackoverflow.com/questions/3118234/how-to-get-memory-usage-and-cpu-usage-in-android
	
	private float readUsage() 
	{
	    try 
	    {
	        RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
	        String load = reader.readLine();

	        String[] toks = load.split(" ");

	        long idle1 = Long.parseLong(toks[5]);
	        long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
	              + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

	        try 
	        {
	            Thread.sleep(360);
	        } 
	        catch (Exception e) 
	        {
	        	
	        }

	        reader.seek(0);
	        load = reader.readLine();
	        reader.close();

	        toks = load.split(" ");

	        long idle2 = Long.parseLong(toks[5]);
	        long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
	            + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

	        return (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

	    }
	    catch (IOException ex) 
	    {
	        ex.printStackTrace();
	    }

	    return 0;
	} 

	
	public boolean isEnabled(final Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		final long now = System.currentTimeMillis();

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
							final HttpUploadPlugin httpPlugin = (HttpUploadPlugin) plugin;

							final RobotHealthProbe me = this;

							Runnable r = new Runnable()
							{
								public void run()
								{
									if (me._checking)
										return;

									me._checking = true;

									File archiveFolder = httpPlugin.getArchiveFolder();
									File pendingFolder = httpPlugin.getPendingFolder();

									int pendingCount = 0;
									int archiveCount = 0;

									long pendingSize = 0;
									long archiveSize = 0;

									File[] archives = archiveFolder.listFiles();

									if (archives != null)
									{
										for (File f : archives)
										{
											if (f.isFile())
											{
												archiveCount += 1;
												archiveSize += f.length();
											}
										}
									}

									FilenameFilter jsonFilter =  new FilenameFilter()
									{
										public boolean accept(File dir, String filename)
										{
											return filename.endsWith(".json");
										}
									};

									String[] filenames = pendingFolder.list(jsonFilter);

									if (filenames == null)
										filenames = new String[0];

									pendingCount = filenames.length;

									if (pendingCount < 2048)
									{
										File[] fs = pendingFolder.listFiles(jsonFilter);

										if (fs != null)
										{
											for (File f : fs)
											{
												pendingSize += f.length();
											}
										}
									}
									else
										pendingSize = Integer.MAX_VALUE;

									Bundle bundle = new Bundle();
									bundle.putString("PROBE", me.name(context));
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
									
									// Version checks

									try 
									{
										PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

										bundle.putString(RobotHealthProbe.APP_VERSION_NAME, info.versionName);
										bundle.putInt(RobotHealthProbe.APP_VERSION_CODE, info.versionCode);
									}
									catch (NameNotFoundException e) 
									{
										e.printStackTrace();
									}

									// NTP checks
									
									if ((now - me._lastTimeCheck) > NTP_CHECK_DURATION)
									{
										try 
										{
											NTPUDPClient client = new NTPUDPClient();
											client.setDefaultTimeout(10000);
											
											TimeInfo info = client.getTime(InetAddress.getByName(NTP_HOST));
											
											if (info != null)
											{
												info.computeDetails();
												
												if (info.getOffset() != null)
													me._lastOffset = info.getOffset().longValue();
													
											}
										}
										catch (UnknownHostException e) 
										{

										} 
										catch (IOException e) 
										{
											e.printStackTrace();
										}
										finally
										{
											me._lastTimeCheck = now;
										}
									}

									bundle.putLong(RobotHealthProbe.TIME_OFFSET_MS, me._lastOffset);
									bundle.putLong(RobotHealthProbe.ACTIVE_RUNTIME, System.currentTimeMillis() - ManagerService.startTimestamp);
									bundle.putFloat(RobotHealthProbe.CPU_USAGE, me.readUsage());

									me.transmitData(context, bundle);

									me._checking = false;
								}
							};

							Thread t = new Thread(r);
							t.start();
						}

						this._lastCheck = now;
					}
				}
			}

			return true;
		}

		return false;
	}

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		formatted.putLong(context.getString(R.string.robot_runtime_label), bundle.getLong(RobotHealthProbe.ACTIVE_RUNTIME, 0));
		formatted.putFloat(context.getString(R.string.robot_cpu_load_label), bundle.getFloat(RobotHealthProbe.CPU_USAGE, 0));
		formatted.putLong(context.getString(R.string.robot_time_offset_label), bundle.getLong(RobotHealthProbe.TIME_OFFSET_MS, 0));
		formatted.putInt(context.getString(R.string.robot_pending_count_label), bundle.getInt(RobotHealthProbe.PENDING_COUNT, 0));
		formatted.putLong(context.getString(R.string.robot_pending_size_label), bundle.getLong(RobotHealthProbe.PENDING_SIZE, 0));
		formatted.putLong(context.getString(R.string.robot_clear_time_label), bundle.getLong(RobotHealthProbe.CLEAR_TIME, 0));

		formatted.putString(context.getString(R.string.robot_version_label), bundle.getString(RobotHealthProbe.APP_VERSION_NAME));

		return formatted;
	};
	
	public String summarizeValue(Context context, Bundle bundle)
	{
		int count = bundle.getInt(RobotHealthProbe.PENDING_COUNT);
		double size = 0.0 + bundle.getLong(RobotHealthProbe.PENDING_SIZE);

		long clear = bundle.getLong(RobotHealthProbe.CLEAR_TIME);

		if (clear < 0)
			clear = 0;

		size = size / (1024 * 1024);

		float cpu = bundle.getFloat(RobotHealthProbe.CPU_USAGE);

		return String.format(context.getResources().getString(R.string.summary_robot_probe), cpu, count, size, clear);
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
		screen.setSummary(R.string.summary_robot_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_robot_enabled");
		enabled.setDefaultValue(true);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_robot_frequency");
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
