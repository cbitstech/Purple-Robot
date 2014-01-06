package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

	private static final boolean DEFAULT_ENABLED = true;

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
		return context.getResources().getString(R.string.probe_device_info_category);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_running_software_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_running_software_enabled", false);
		
		e.commit();
	}

	public boolean isEnabled(final Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);

		if (super.isEnabled(context))
		{
			final long now = System.currentTimeMillis();

			if (prefs.getBoolean("config_probe_running_software_enabled", RunningSoftwareProbe.DEFAULT_ENABLED))
			{
				synchronized(this)
				{
					long freq = Long.parseLong(prefs.getString("config_probe_running_software_frequency", Probe.DEFAULT_FREQUENCY));

					if (now - this._lastCheck  > freq)
					{
						final RunningSoftwareProbe me = this;
						
						Runnable r = new Runnable()
						{
							public void run() 
							{
								Bundle bundle = new Bundle();
								bundle.putString("PROBE", me.name(context));
								bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

								ActivityManager am = (ActivityManager) context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);

								List<RunningTaskInfo> tasks = am.getRunningTasks(9999);

								if (tasks != null)
								{
									ArrayList<Bundle> running = new ArrayList<Bundle>();
			
									for (RunningTaskInfo info : tasks)
									{
										Bundle taskBundle = new Bundle();
			
										taskBundle.putString(RunningSoftwareProbe.PACKAGE_NAME, info.baseActivity.getPackageName());

										running.add(taskBundle);

										String category = RunningSoftwareProbe.fetchCategory(context, info.baseActivity.getPackageName());
										taskBundle.putString(RunningSoftwareProbe.PACKAGE_CATEGORY, category);
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

		if (prefs.getBoolean("config_restrict_data_wifi", true))
		{
			if (WiFiHelper.wifiAvailable(context) == false)
				return context.getString(R.string.app_category_unknown);
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

	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);

		long freq = Long.parseLong(prefs.getString("config_probe_running_software_frequency", Probe.DEFAULT_FREQUENCY));
		
		map.put(Probe.PROBE_FREQUENCY, freq);
		
		return map;
	}
	
	public void updateFromMap(Context context, Map<String, Object> params) 
	{
		super.updateFromMap(context, params);
		
		if (params.containsKey(Probe.PROBE_FREQUENCY))
		{
			Object frequency = params.get(Probe.PROBE_FREQUENCY);
			
			if (frequency instanceof Long)
			{
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putString("config_probe_running_software_frequency", frequency.toString());
				e.commit();
			}
		}
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_running_software_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_running_software_enabled");
		enabled.setDefaultValue(RunningSoftwareProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_running_software_frequency");
		duration.setEntryValues(R.array.probe_low_frequency_values);
		duration.setEntries(R.array.probe_low_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);
		duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

		screen.addPreference(duration);

		return screen;
	}
}
