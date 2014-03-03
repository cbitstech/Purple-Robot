package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;

// import org.apache.commons.net.ntp.NTPUDPClient;
// import org.apache.commons.net.ntp.TimeInfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.BootUpReceiver;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.ShutdownReceiver;
import edu.northwestern.cbits.purple_robot_manager.config.JSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.config.SchemeConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.plugins.HttpUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPluginManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class RobotHealthProbe extends Probe
{
	private static final String PENDING_COUNT = "PENDING_COUNT";
	private static final String PENDING_SIZE = "PENDING_SIZE";
//	private static final String ARCHIVE_COUNT = "ARCHIVE_COUNT";
//	private static final String ARCHIVE_SIZE = "ARCHIVE_SIZE";
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
	protected static final String ROOT_FREE = "ROOT_FREE";
	protected static final String ROOT_TOTAL = "ROOT_TOTAL";
	protected static final String EXTERNAL_FREE = "EXTERNAL_FREE";
	protected static final String EXTERNAL_TOTAL = "EXTERNAL_TOTAL";
	protected static final String SCHEME_CONFIG = "SCHEME_CONFIG";
	protected static final String JSON_CONFIG = "JSON_CONFIG";

	protected static final String LAST_BOOT = "LAST_BOOT";
	protected static final String LAST_HALT = "LAST_HALT";

	private static final boolean DEFAULT_ENABLED = true;
	private static final boolean DEFAULT_SCHEME = true;
	private static final boolean DEFAULT_JAVASCRIPT = true;
	private static final Object INCLUDE_JSON = "include_json";
	private static final Object INCLUDE_SCHEME = "include_scheme";
	

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
		return context.getResources().getString(R.string.probe_device_info_category);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_robot_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_robot_enabled", false);
		
		e.commit();
	}

	// Source: http://stackoverflow.com/questions/3118234/how-to-get-memory-usage-and-cpu-usage-in-android
	
	private float readUsage(Context context) 
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
	    catch (IOException e) 
	    {
 			LogManager.getInstance(context).logException(e);
	    }

	    return 0;
	} 

	
	public boolean isEnabled(final Context context)
	{
		final SharedPreferences prefs = Probe.getPreferences(context);

		final long now = System.currentTimeMillis();

		if (super.isEnabled(context))
		{
			if (prefs.getBoolean("config_probe_robot_enabled", RobotHealthProbe.DEFAULT_ENABLED))
			{
				synchronized(this)
				{
					long freq = Long.parseLong(prefs.getString("config_probe_robot_frequency", Probe.DEFAULT_FREQUENCY));

					if (now - this._lastCheck  > freq)
					{
						OutputPlugin plugin = OutputPluginManager.sharedInstance.pluginForClass(context, HttpUploadPlugin.class);

						if (plugin != null && plugin instanceof HttpUploadPlugin)
						{
							final HttpUploadPlugin httpPlugin = (HttpUploadPlugin) plugin;

							final RobotHealthProbe me = this;

							Runnable r = new Runnable()
							{
								@SuppressWarnings("deprecation")
								public void run()
								{
									if (me._checking)
										return;

									me._checking = true;
									
//									File archiveFolder = httpPlugin.getArchiveFolder();
									File pendingFolder = httpPlugin.getPendingFolder();

									int pendingCount = 0;
//									int archiveCount = 0;

									long pendingSize = 0;
//									long archiveSize = 0;


//									String[] archivesNames = archiveFolder.list();

//									archiveCount = archivesNames.length;
	
/*									if (archiveCount < 2048)
									{
										for (File f : archiveFolder.listFiles())
										{
											archiveSize += f.length();
										}
									}
									else
										archiveSize = Integer.MAX_VALUE;
*/

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

//									bundle.putInt(RobotHealthProbe.ARCHIVE_COUNT, archiveCount);
//									bundle.putLong(RobotHealthProbe.ARCHIVE_SIZE, archiveSize);

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
										LogManager.getInstance(context).logException(e);
									}
									catch (RuntimeException e)
									{
										LogManager.getInstance(context).logException(e);
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
											LogManager.getInstance(context).logException(e);
										} 
										catch (SocketTimeoutException e) 
										{
											LogManager.getInstance(context).logException(e);
										} 
										catch (IOException e) 
										{
											LogManager.getInstance(context).logException(e);
										}
										finally
										{
											me._lastTimeCheck = now;
										}
									}

									bundle.putLong(RobotHealthProbe.TIME_OFFSET_MS, me._lastOffset);
									
									bundle.putLong(RobotHealthProbe.ACTIVE_RUNTIME, System.currentTimeMillis() - ManagerService.startTimestamp);
									bundle.putFloat(RobotHealthProbe.CPU_USAGE, me.readUsage(context));
									
									try
									{
										StatFs root = new StatFs(Environment.getRootDirectory().getAbsolutePath());
										
										int rootFree = root.getAvailableBlocks() * root.getBlockSize();
										int rootTotal = root.getBlockCount() * root.getBlockSize();
	
										bundle.putInt(RobotHealthProbe.ROOT_FREE, rootFree);
										bundle.putInt(RobotHealthProbe.ROOT_TOTAL, rootTotal);
									}
									catch (IllegalArgumentException e)
									{
										LogManager.getInstance(context).logException(e);
									}

									try
									{
										StatFs external = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
										
										int externalFree = external.getAvailableBlocks() * external.getBlockSize();
										int externalTotal = external.getBlockCount() * external.getBlockSize();

										bundle.putInt(RobotHealthProbe.EXTERNAL_FREE, externalFree);
										bundle.putInt(RobotHealthProbe.EXTERNAL_TOTAL, externalTotal);
									}
									catch (IllegalArgumentException e)
									{
										LogManager.getInstance(context).logException(e);
									}
									
									if (prefs.getBoolean("config_probe_robot_scheme_config", RobotHealthProbe.DEFAULT_SCHEME))
									{
										SchemeConfigFile file = new SchemeConfigFile(context);
										
										bundle.putString(RobotHealthProbe.SCHEME_CONFIG, file.toString());
									}

									if (prefs.getBoolean("config_probe_robot_json_config", RobotHealthProbe.DEFAULT_JAVASCRIPT))
									{
										JSONConfigFile file = new JSONConfigFile(context);
										
										bundle.putString(RobotHealthProbe.JSON_CONFIG, file.toString());
									}

									bundle.putLong(RobotHealthProbe.LAST_BOOT, prefs.getLong(BootUpReceiver.BOOT_KEY, 0));
									bundle.putLong(RobotHealthProbe.LAST_HALT, prefs.getLong(ShutdownReceiver.SHUTDOWN_KEY, 0));
									
									ArrayList<String> errors = new ArrayList<String>();
									
									for (String check : SanityManager.getInstance(context).errors().values())
										errors.add(check);
									
									if (errors.size() > 0)
										bundle.putStringArrayList("CHECK_ERRORS", errors);

									ArrayList<String> warnings = new ArrayList<String>();
									
									for (String check : SanityManager.getInstance(context).warnings().values())
										warnings.add(check);
									
									if (warnings.size() > 0)
										bundle.putStringArrayList("CHECK_WARNNIGS", warnings);
									
									long later = System.currentTimeMillis();
									
									bundle.putLong("MEASURE_TIME", later - now);

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

				return true;
			}
		}

		return false;
	}

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		formatted.putLong(context.getString(R.string.robot_runtime_label), bundle.getLong(RobotHealthProbe.ACTIVE_RUNTIME, 0));
		formatted.putFloat(context.getString(R.string.robot_cpu_load_label), bundle.getFloat(RobotHealthProbe.CPU_USAGE, 0));
		formatted.putLong(context.getString(R.string.robot_time_offset_label), bundle.getLong(RobotHealthProbe.TIME_OFFSET_MS, 0));
		formatted.putInt(context.getString(R.string.robot_pending_count_label), (int) bundle.getDouble(RobotHealthProbe.PENDING_COUNT, 0));
		formatted.putLong(context.getString(R.string.robot_pending_size_label), bundle.getLong(RobotHealthProbe.PENDING_SIZE, 0));
		formatted.putLong(context.getString(R.string.robot_clear_time_label), bundle.getLong(RobotHealthProbe.CLEAR_TIME, 0));

		formatted.putString(context.getString(R.string.robot_version_label), bundle.getString(RobotHealthProbe.APP_VERSION_NAME));

		return formatted;
	};
	
	public String summarizeValue(Context context, Bundle bundle)
	{
		int count = (int) bundle.getDouble(RobotHealthProbe.PENDING_COUNT);
		double size = 0.0 + (int) bundle.getDouble(RobotHealthProbe.PENDING_SIZE);

		long clear = (int) bundle.getDouble(RobotHealthProbe.CLEAR_TIME);

		if (clear < 0)
			clear = 0;

		size = size / (1024 * 1024);

		double cpu = bundle.getDouble(RobotHealthProbe.CPU_USAGE);

		return String.format(context.getResources().getString(R.string.summary_robot_probe), cpu, count, size, clear);
	}

	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);

		long freq = Long.parseLong(prefs.getString("config_probe_robot_frequency", Probe.DEFAULT_FREQUENCY));
		
		map.put(Probe.PROBE_FREQUENCY, freq);
		
		// TODO: Add JS + Scheme params...
		
		return map;
	}
	
	public void updateFromMap(Context context, Map<String, Object> params) 
	{
		super.updateFromMap(context, params);

		SharedPreferences prefs = Probe.getPreferences(context);
		Editor e = prefs.edit();

		if (params.containsKey(Probe.PROBE_FREQUENCY))
		{
			Object frequency = params.get(Probe.PROBE_FREQUENCY);
			
			if (frequency instanceof Long)
				e.putString("config_probe_robot_frequency", frequency.toString());
		}

		if (params.containsKey(RobotHealthProbe.INCLUDE_SCHEME))
		{
			Object include = params.get(RobotHealthProbe.INCLUDE_SCHEME);
			
			if (include instanceof Boolean)
				e.putBoolean("config_probe_robot_scheme_config", ((Boolean) include).booleanValue());
		}

		if (params.containsKey(RobotHealthProbe.INCLUDE_JSON))
		{
			Object include = params.get(RobotHealthProbe.INCLUDE_JSON);
			
			if (include instanceof Boolean)
				e.putBoolean("config_probe_robot_json_config", ((Boolean) include).booleanValue());
		}

		e.commit();
	}

	public String summary(Context context) 
	{
		return context.getString(R.string.summary_robot_probe_desc);
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_robot_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_robot_enabled");
		enabled.setDefaultValue(RobotHealthProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_robot_frequency");
		duration.setEntryValues(R.array.probe_low_frequency_values);
		duration.setEntries(R.array.probe_low_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);
		duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

		screen.addPreference(duration);

		CheckBoxPreference scheme = new CheckBoxPreference(activity);
		scheme.setTitle(R.string.title_enable_scheme_config);
		scheme.setKey("config_probe_robot_scheme_config");
		scheme.setDefaultValue(RobotHealthProbe.DEFAULT_SCHEME);
		screen.addPreference(scheme);

		CheckBoxPreference json = new CheckBoxPreference(activity);
		json.setTitle(R.string.title_enable_json_config);
		json.setKey("config_probe_robot_json_config");
		json.setDefaultValue(RobotHealthProbe.DEFAULT_JAVASCRIPT);
		screen.addPreference(json);

		return screen;
	}
}
