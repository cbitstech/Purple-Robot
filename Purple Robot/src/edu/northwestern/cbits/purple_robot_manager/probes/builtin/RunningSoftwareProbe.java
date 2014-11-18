package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.WiFiHelper;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class RunningSoftwareProbe extends Probe
{
    private static final String PACKAGE_NAME = "PACKAGE_NAME";
    private static final String RUNNING_TASKS = "RUNNING_TASKS";
    private static final String RUNNING_TASK_COUNT = "RUNNING_TASK_COUNT";
    private static final String PACKAGE_CATEGORY = "PACKAGE_CATEGORY";
    private static final String TASK_STACK_INDEX = "TASK_STACK_INDEX";

    private static final boolean DEFAULT_ENABLED = true;
    private static final String ENABLED = "config_probe_running_software_enabled";
    private static final String FREQUENCY = "config_probe_running_software_frequency";

    private long _lastCheck = 0;

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.RunningSoftwareProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_running_software_probe);
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
        e.putBoolean(RunningSoftwareProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(RunningSoftwareProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            final long now = System.currentTimeMillis();

            if (prefs.getBoolean(RunningSoftwareProbe.ENABLED, RunningSoftwareProbe.DEFAULT_ENABLED))
            {
                synchronized (this)
                {
                    long freq = Long
                            .parseLong(prefs.getString(RunningSoftwareProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

                    if (now - this._lastCheck > freq)
                    {
                        final RunningSoftwareProbe me = this;

                        Runnable r = new Runnable()
                        {
                            @Override
                            @SuppressWarnings("deprecation")
                            public void run()
                            {
                                Bundle bundle = new Bundle();
                                bundle.putString("PROBE", me.name(context));
                                bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                                ActivityManager am = (ActivityManager) context.getApplicationContext()
                                        .getSystemService(Context.ACTIVITY_SERVICE);

                                List<RunningTaskInfo> tasks = am.getRunningTasks(9999);

                                if (tasks != null)
                                {
                                    ArrayList<Bundle> running = new ArrayList<Bundle>();

                                    for (int i = 0; i < tasks.size(); i++)
                                    {
                                        RunningTaskInfo info = tasks.get(i);

                                        Bundle taskBundle = new Bundle();

                                        taskBundle.putString(RunningSoftwareProbe.PACKAGE_NAME,
                                                info.baseActivity.getPackageName());
                                        taskBundle.putInt(RunningSoftwareProbe.TASK_STACK_INDEX, i);

                                        String category = RunningSoftwareProbe.fetchCategory(context,
                                                info.baseActivity.getPackageName());
                                        taskBundle.putString(RunningSoftwareProbe.PACKAGE_CATEGORY, category);

                                        running.add(taskBundle);
                                    }

                                    bundle.putParcelableArrayList(RunningSoftwareProbe.RUNNING_TASKS, running);
                                    bundle.putInt(RunningSoftwareProbe.RUNNING_TASK_COUNT, running.size());

                                    me.transmitData(context, bundle);
                                }
                            }
                        };

                        Thread t = new Thread(r);
                        t.start();

                        me._lastCheck = now;
                    }
                }

                return true;
            }
        }

        return false;
    }

    protected static String fetchCategory(Context context, String packageName)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        String key = "category_" + packageName;

        if (prefs.contains(key))
            return prefs.getString(key, context.getString(R.string.app_category_unknown));

        try
        {
            // TODO: Replace with constant...
            if (prefs.getBoolean("config_restrict_data_wifi", true))
            {
                if (WiFiHelper.wifiAvailable(context) == false)
                    return context.getString(R.string.app_category_unknown);
            }
        }
        catch (ClassCastException e)
        {
            if (prefs.getString("config_restrict_data_wifi", "true").equalsIgnoreCase("true"))
            {
                Editor ed = prefs.edit();
                ed.putBoolean("config_restrict_data_wifi", true);
                ed.commit();

                if (WiFiHelper.wifiAvailable(context) == false)
                    return context.getString(R.string.app_category_unknown);
            }
            else
            {
                Editor ed = prefs.edit();
                ed.putBoolean("config_restrict_data_wifi", false);
                ed.commit();
            }
        }

        String category = null;

        try
        {
            String url = "https://play.google.com/store/apps/details?id=" + packageName;

            Document doc = Jsoup.connect(url).get();

            Element detailsTab = doc.select("a.category").first();

            if (detailsTab != null)
            {
                Elements spans = detailsTab.select("span");

                for (Element span : spans)
                {
                    category = span.text();
                }
            }
        }
        catch (HttpStatusException ex)
        {
            if (ex.getStatusCode() == 404)
                category = context.getString(R.string.app_category_bundled);
            else
                LogManager.getInstance(context).logException(ex);
        }
        catch (IOException ex)
        {
            LogManager.getInstance(context).logException(ex);
        }

        if (category == null)
            category = context.getString(R.string.app_category_unknown);

        Editor e = prefs.edit();

        e.putString(key, category);

        e.commit();

        return category;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        int count = (int) bundle.getDouble(RunningSoftwareProbe.RUNNING_TASK_COUNT);

        return String.format(context.getResources().getString(R.string.summary_running_software_probe), count);
    }

    private Bundle bundleForTaskArray(Context context, ArrayList<Bundle> objects)
    {
        Bundle bundle = new Bundle();

        ArrayList<String> keys = new ArrayList<String>();

        for (int i = 0; i < objects.size(); i++)
        {
            Bundle value = objects.get(i);
            String name = value.getString(RunningSoftwareProbe.PACKAGE_NAME);

            String key = String.format(context.getString(R.string.display_running_task_title), (i + 1));

            keys.add(key);
            bundle.putString(key, name);
        }

        bundle.putStringArrayList("KEY_ORDER", keys);

        return bundle;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Bundle formattedBundle(Context context, Bundle bundle)
    {
        Bundle formatted = super.formattedBundle(context, bundle);

        ArrayList<Bundle> array = (ArrayList<Bundle>) bundle.get(RunningSoftwareProbe.RUNNING_TASKS);

        int count = (int) bundle.getDouble(RunningSoftwareProbe.RUNNING_TASK_COUNT);

        Bundle tasksBundle = this.bundleForTaskArray(context, array);

        formatted.putBundle(String.format(context.getString(R.string.display_running_tasks_title), count), tasksBundle);

        return formatted;
    };

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString(RunningSoftwareProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

        map.put(Probe.PROBE_FREQUENCY, freq);

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(Probe.PROBE_FREQUENCY))
        {
            Object frequency = params.get(Probe.PROBE_FREQUENCY);

            if ((frequency instanceof Double) == false)
                frequency = Double.valueOf(frequency.toString()).longValue();
            else
                frequency = ((Double) frequency).longValue();

            SharedPreferences prefs = Probe.getPreferences(context);
            Editor e = prefs.edit();

            e.putString(RunningSoftwareProbe.FREQUENCY, frequency.toString());
            e.commit();
        }
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_running_software_probe_desc);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(PreferenceActivity activity)
    {
        PreferenceManager manager = activity.getPreferenceManager();

        PreferenceScreen screen = manager.createPreferenceScreen(activity);
        screen.setTitle(this.title(activity));
        screen.setSummary(R.string.summary_running_software_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(RunningSoftwareProbe.ENABLED);
        enabled.setDefaultValue(RunningSoftwareProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        ListPreference duration = new ListPreference(activity);
        duration.setKey(RunningSoftwareProbe.FREQUENCY);
        duration.setEntryValues(R.array.probe_low_frequency_values);
        duration.setEntries(R.array.probe_low_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

        screen.addPreference(duration);

        return screen;
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = new JSONObject();

        try
        {
            JSONObject enabled = new JSONObject();
            enabled.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            JSONArray values = new JSONArray();
            values.put(true);
            values.put(false);
            enabled.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_ENABLED, enabled);

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
}
