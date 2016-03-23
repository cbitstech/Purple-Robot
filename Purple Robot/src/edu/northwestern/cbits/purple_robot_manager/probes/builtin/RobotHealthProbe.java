package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.StatFs;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.BootUpReceiver;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.ShutdownReceiver;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.config.JSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.config.SchemeConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.plugins.HttpUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPluginManager;
import edu.northwestern.cbits.purple_robot_manager.plugins.StreamingJacksonUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;
// import org.apache.commons.net.ntp.NTPUDPClient;
// import org.apache.commons.net.ntp.TimeInfo;

public class RobotHealthProbe extends Probe
{
    private static final String PENDING_COUNT = "PENDING_COUNT";
    private static final String PENDING_SIZE = "PENDING_SIZE";
    // private static final String ARCHIVE_COUNT = "ARCHIVE_COUNT";
    // private static final String ARCHIVE_SIZE = "ARCHIVE_SIZE";
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
    private static final boolean DEFAULT_JSON = true;
    private static final String INCLUDE_SCHEME_CONFIG = "config_probe_robot_scheme_config";
    private static final String ENABLED = "config_probe_robot_enabled";
    private static final String FREQUENCY = "config_probe_robot_frequency";
    private static final String INCLUDE_JSON_CONFIG = "config_probe_robot_json_config";

    public static String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.RobotHealthProbe";

    private long _lastOffset = 0;
    private long _lastTimeCheck = 0;

    @Override
    public String getPreferenceKey() {
        return "built_in_robot_health";
    }

    @Override
    public String name(Context context)
    {
        return RobotHealthProbe.NAME;
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_robot_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_device_info_category);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(RobotHealthProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(RobotHealthProbe.ENABLED, false);

        e.commit();
    }

    // Source:
    // http://stackoverflow.com/questions/3118234/how-to-get-memory-usage-and-cpu-usage-in-android

    private float readUsage(Context context)
    {
        try
        {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();

            String[] toks = load.split(" ");

            long idle1 = Long.parseLong(toks[5]);
            long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4]) + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

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
            long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4]) + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            return (float) (cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

        }
        catch (IOException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return 0;
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        final SharedPreferences prefs = Probe.getPreferences(context);

        final long now = System.currentTimeMillis();

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(RobotHealthProbe.ENABLED, RobotHealthProbe.DEFAULT_ENABLED))
            {
                synchronized (this)
                {
                    long freq = Long.parseLong(prefs.getString(RobotHealthProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

                    if (now - this._lastCheck > freq)
                    {
                        OutputPlugin plugin = OutputPluginManager.sharedInstance.pluginForClass(context, HttpUploadPlugin.class);

                        if (plugin != null && plugin instanceof HttpUploadPlugin)
                        {
                            final HttpUploadPlugin httpPlugin = (HttpUploadPlugin) plugin;

                            final RobotHealthProbe me = this;

                            Runnable r = new Runnable()
                            {
                                @Override
                                @SuppressWarnings("deprecation")
                                public void run()
                                {
                                    if (me._checking)
                                        return;

                                    if (Looper.myLooper() == null)
                                        Looper.prepare();

                                    me._checking = true;

                                    int pendingCount = 0;

                                    // TODO: Pull string values into
                                    // constants...

                                    if (prefs.getBoolean("config_enable_data_server", false))
                                    {
                                        OutputPlugin plugin = OutputPluginManager.sharedInstance.pluginForClass(context, HttpUploadPlugin.class);

                                        if (plugin instanceof HttpUploadPlugin)
                                        {
                                            HttpUploadPlugin http = (HttpUploadPlugin) plugin;
                                            pendingCount += http.pendingFilesCount();
                                        }
                                    }
                                    else
                                    {
                                        OutputPlugin plugin = OutputPluginManager.sharedInstance.pluginForClass(context, StreamingJacksonUploadPlugin.class);

                                        if (plugin instanceof StreamingJacksonUploadPlugin)
                                        {
                                            StreamingJacksonUploadPlugin http = (StreamingJacksonUploadPlugin) plugin;
                                            pendingCount += http.pendingFilesCount();
                                        }
                                    }

                                    long pendingSize = 0;

                                    if (prefs.getBoolean("config_enable_data_server", false))
                                    {
                                        OutputPlugin plugin = OutputPluginManager.sharedInstance.pluginForClass(context, HttpUploadPlugin.class);

                                        if (plugin instanceof HttpUploadPlugin)
                                        {
                                            HttpUploadPlugin http = (HttpUploadPlugin) plugin;
                                            pendingSize += http.pendingFilesSize();
                                        }
                                    }
                                    else
                                    {
                                        OutputPlugin plugin = OutputPluginManager.sharedInstance.pluginForClass(context, StreamingJacksonUploadPlugin.class);

                                        if (plugin instanceof StreamingJacksonUploadPlugin)
                                        {
                                            StreamingJacksonUploadPlugin http = (StreamingJacksonUploadPlugin) plugin;
                                            pendingSize += http.pendingFilesSize();
                                        }
                                    }

                                    Bundle bundle = new Bundle();
                                    bundle.putString("PROBE", me.name(context));
                                    bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
                                    bundle.putBoolean("PRIORITY", true);

                                    bundle.putInt(RobotHealthProbe.PENDING_COUNT, pendingCount);
                                    bundle.putLong(RobotHealthProbe.PENDING_SIZE, pendingSize);

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
                                    catch (NameNotFoundException | RuntimeException e)
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
                                                    me._lastOffset = info.getOffset();

                                            }
                                        }
                                        catch (Exception e)
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

                                    if (prefs.getBoolean(RobotHealthProbe.INCLUDE_SCHEME_CONFIG, RobotHealthProbe.DEFAULT_SCHEME))
                                    {
                                        SchemeConfigFile file = new SchemeConfigFile(context);

                                        bundle.putString(RobotHealthProbe.SCHEME_CONFIG, file.toString());
                                    }

                                    if (prefs.getBoolean(RobotHealthProbe.INCLUDE_JSON_CONFIG, RobotHealthProbe.DEFAULT_JSON))
                                    {
                                        JSONConfigFile file = new JSONConfigFile(context);

                                        bundle.putString(RobotHealthProbe.JSON_CONFIG, file.toString());
                                    }

                                    bundle.putLong(RobotHealthProbe.LAST_BOOT, prefs.getLong(BootUpReceiver.BOOT_KEY, 0));
                                    bundle.putLong(RobotHealthProbe.LAST_HALT, prefs.getLong(ShutdownReceiver.SHUTDOWN_KEY, 0));

                                    ArrayList<String> errors = new ArrayList<>();

                                    for (String check : SanityManager.getInstance(context).errors().values())
                                        errors.add(check);

                                    bundle.putStringArrayList("CHECK_ERRORS", errors);

                                    ArrayList<String> warnings = new ArrayList<>();

                                    for (String check : SanityManager.getInstance(context).warnings().values())
                                        warnings.add(check);

                                    bundle.putStringArrayList("CHECK_WARNINGS", warnings);

                                    bundle.putParcelableArrayList("TRIGGERS", TriggerManager.getInstance(context).allTriggersBundles(context));

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

    @Override
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
    }

    @Override
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

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString(RobotHealthProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

        map.put(Probe.PROBE_FREQUENCY, freq);

        map.put(RobotHealthProbe.INCLUDE_SCHEME_CONFIG, prefs.getBoolean(RobotHealthProbe.INCLUDE_SCHEME_CONFIG, RobotHealthProbe.DEFAULT_SCHEME));
        map.put(RobotHealthProbe.INCLUDE_JSON_CONFIG, prefs.getBoolean(RobotHealthProbe.INCLUDE_JSON_CONFIG, RobotHealthProbe.DEFAULT_JSON));

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        SharedPreferences prefs = Probe.getPreferences(context);
        Editor e = prefs.edit();

        if (params.containsKey(Probe.PROBE_FREQUENCY))
        {
            Object frequency = params.get(Probe.PROBE_FREQUENCY);

            if (frequency instanceof Double)
            {
                frequency = ((Double) frequency).longValue();
            }

            if (frequency instanceof Long)
                e.putString(RobotHealthProbe.FREQUENCY, frequency.toString());
        }

        if (params.containsKey(RobotHealthProbe.INCLUDE_SCHEME_CONFIG))
        {
            Object include = params.get(RobotHealthProbe.INCLUDE_SCHEME_CONFIG);

            if (include instanceof Boolean)
                e.putBoolean(RobotHealthProbe.INCLUDE_SCHEME_CONFIG, (Boolean) include);
        }

        if (params.containsKey(RobotHealthProbe.INCLUDE_JSON_CONFIG))
        {
            Object include = params.get(RobotHealthProbe.INCLUDE_JSON_CONFIG);

            if (include instanceof Boolean)
                e.putBoolean(RobotHealthProbe.INCLUDE_JSON_CONFIG, (Boolean) include);
        }

        e.commit();
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_robot_probe_desc);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = super.preferenceScreen(context, manager);

        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_robot_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(RobotHealthProbe.ENABLED);
        enabled.setDefaultValue(RobotHealthProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleListPreference duration = new FlexibleListPreference(context);
        duration.setKey(RobotHealthProbe.FREQUENCY);
        duration.setEntryValues(R.array.probe_low_frequency_values);
        duration.setEntries(R.array.probe_low_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

        screen.addPreference(duration);

        CheckBoxPreference scheme = new CheckBoxPreference(context);
        scheme.setTitle(R.string.title_enable_scheme_config);
        scheme.setKey(RobotHealthProbe.INCLUDE_SCHEME_CONFIG);
        scheme.setDefaultValue(RobotHealthProbe.DEFAULT_SCHEME);
        screen.addPreference(scheme);

        CheckBoxPreference json = new CheckBoxPreference(context);
        json.setTitle(R.string.title_enable_json_config);
        json.setKey(RobotHealthProbe.INCLUDE_JSON_CONFIG);
        json.setDefaultValue(RobotHealthProbe.DEFAULT_JSON);
        screen.addPreference(json);

        return screen;
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = super.fetchSettings(context);

        try
        {
            JSONArray values = new JSONArray();
            values.put(true);
            values.put(false);

            JSONObject enabled = new JSONObject();
            enabled.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            enabled.put(Probe.PROBE_VALUES, values);

            settings.put(RobotHealthProbe.INCLUDE_JSON_CONFIG, enabled);
            settings.put(RobotHealthProbe.INCLUDE_SCHEME_CONFIG, enabled);

            JSONObject frequency = new JSONObject();
            frequency.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
            values = new JSONArray();

            String[] options = context.getResources().getStringArray(R.array.probe_low_frequency_values);

            for (String option : options)
            {
                values.put(Long.parseLong(option));
            }

            frequency.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_FREQUENCY, frequency);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    public String assetPath(Context context)
    {
        return "robot-health-probe.html";
    }
}
