package edu.northwestern.cbits.purple_robot_manager.activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.PersistentService;
import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.RobotContentProvider;
import edu.northwestern.cbits.purple_robot_manager.config.LegacyJSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.db.DistancesProvider;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.models.ModelManager;
import edu.northwestern.cbits.purple_robot_manager.plugins.HttpUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPluginManager;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceClickListener, OnPreferenceChangeListener
{
	public static final String PROBES_SCREEN_KEY = "config_probes_screen";
	private static final String MANUAL_REFRESH_KEY = "config_json_refresh_manually";
	private static final String LOG_REFRESH_KEY = "config_log_refresh_manually";
	private static final String HAPTIC_PATTERN_KEY = "config_json_haptic_pattern";
	public static final String RINGTONE_KEY = "config_default_notification_sound";
	public static final String ZIP_ARCHIVES_KEY = "config_mail_archives";
	public static final String DELETE_ARCHIVES_KEY = "config_delete_archives";
	public static final CharSequence USER_ID_KEY = "config_user_id";
	protected static final String USER_HASH_KEY = "config_user_hash";
	public static final String CHECK_UPDATES_KEY = "config_hockey_update";
	public static final String TRIGGERS_SCREEN_KEY = "config_triggers_screen";
	public static final String MODELS_SCREEN_KEY = "config_models_screen";
	private static final String DUMP_JSON_KEY = "config_dump_json";
	private static final String RESET_KEY = "config_reset";

	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

		PurpleRobotApplication.fixPreferences(this, true);

        this.addPreferencesFromResource(R.layout.layout_settings_activity);

        PreferenceScreen prefs = this.getPreferenceScreen();

        Preference refresh = prefs.findPreference(MANUAL_REFRESH_KEY);
        refresh.setOnPreferenceClickListener(this);

        Preference logRefresh = prefs.findPreference(LOG_REFRESH_KEY);
        logRefresh.setOnPreferenceClickListener(this);

        final SettingsActivity me = this;

        ListPreference haptic = (ListPreference) prefs.findPreference(HAPTIC_PATTERN_KEY);
        haptic.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
        {
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				String pattern = (String) newValue;

				Intent intent = new Intent(ManagerService.HAPTIC_PATTERN_INTENT);
				intent.putExtra(ManagerService.HAPTIC_PATTERN_NAME, pattern);

				me.startService(intent);

				return true;
			}
        });
        
        PreferenceScreen probesScreen = ProbeManager.buildPreferenceScreen(this);

        PreferenceCategory category = (PreferenceCategory) prefs.findPreference("config_settings_probe_category");
        category.addPreference(probesScreen);

        PreferenceScreen triggersScreen = TriggerManager.getInstance(this).buildPreferenceScreen(this);

        PreferenceCategory triggerCategory = (PreferenceCategory) prefs.findPreference("config_settings_trigger_category");
        triggerCategory.addPreference(triggersScreen);

        PreferenceScreen modelsScreen = ModelManager.getInstance(this).buildPreferenceScreen(this);

        PreferenceCategory modelCategory = (PreferenceCategory) prefs.findPreference("config_settings_models_category");
        modelCategory.addPreference(modelsScreen);

        Preference archive = prefs.findPreference(ZIP_ARCHIVES_KEY);
        archive.setOnPreferenceClickListener(this);

        Preference delete = prefs.findPreference(DELETE_ARCHIVES_KEY);
        delete.setOnPreferenceClickListener(this);

        Preference dump = prefs.findPreference(DUMP_JSON_KEY);
        dump.setOnPreferenceClickListener(this);

        CheckBoxPreference update = (CheckBoxPreference) prefs.findPreference(CHECK_UPDATES_KEY);
        update.setOnPreferenceChangeListener(this);

        ListPreference listUpdate = (ListPreference) prefs.findPreference(RINGTONE_KEY);
        listUpdate.setOnPreferenceChangeListener(this);

        Preference reset = prefs.findPreference(RESET_KEY);
        reset.setOnPreferenceClickListener(this);

        LogManager.getInstance(me).log("settings_visited", null);
    }
	
	protected void onDestroy()
	{
		super.onDestroy();

		LogManager.getInstance(this).log("settings_exited", null);
	}

	@SuppressWarnings("deprecation")
	public boolean onPreferenceClick(Preference preference)
	{
        if (HAPTIC_PATTERN_KEY.equals(preference.getKey()))
        {
        	ListPreference listPref = (ListPreference) preference;

			String pattern = listPref.getValue();

			Intent intent = new Intent(ManagerService.HAPTIC_PATTERN_INTENT);
			intent.putExtra(ManagerService.HAPTIC_PATTERN_NAME, pattern);

			this.startService(intent);

			return true;
        }
        else if (PROBES_SCREEN_KEY.equals(preference.getKey()))
			return true;
        else if (MANUAL_REFRESH_KEY.equals(preference.getKey()))
        {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
			Editor editor = prefs.edit();

			editor.putLong(LegacyJSONConfigFile.JSON_LAST_UPDATE, 0);
			editor.putString(LegacyJSONConfigFile.JSON_LAST_HASH, "");

			editor.commit();
			LegacyJSONConfigFile.update(this, true);

			ProbeManager.nudgeProbes(this);
			TriggerManager.getInstance(this).refreshTriggers(this);

            return true;
        }
        else if (LOG_REFRESH_KEY.equals(preference.getKey()))
        {
        	Intent refreshIntent = new Intent(ManagerService.UPLOAD_LOGS_INTENT);
        	this.startService(refreshIntent);

            return true;
        }
        else if (ZIP_ARCHIVES_KEY.equals(preference.getKey()))
        {
        	HttpUploadPlugin plugin = (HttpUploadPlugin) OutputPluginManager.sharedInstance.pluginForClass(this, HttpUploadPlugin.class);

        	if (plugin != null)
        	{
	        	plugin.mailArchiveFiles(this);
	
	        	return true;
        	}
        }
        else if (DELETE_ARCHIVES_KEY.equals(preference.getKey()))
        {
        	HttpUploadPlugin plugin = (HttpUploadPlugin) OutputPluginManager.sharedInstance.pluginForClass(this, HttpUploadPlugin.class);
        	
        	if (plugin != null)
        	{
        		plugin.deleteArchiveFiles(this);

        		return true;
        	}
        }
        else if (DUMP_JSON_KEY.equals(preference.getKey()))
        {
        	try 
        	{
            	JSONObject root = new JSONObject();

            	ApplicationInfo info = this.getApplicationInfo();
            	root.put("name", this.getString(info.labelRes)); 
            
            	PackageInfo pkgInfo = this.getPackageManager().getPackageInfo(info.packageName, 0); 

            	root.put("package_name", pkgInfo.packageName); 
            	root.put("version", pkgInfo.versionCode); 
            	root.put("version_name", pkgInfo.versionName); 

       			JSONObject config = this.dumpJson(this.getPreferenceScreen());
            	root.put("configuration", config); 
            	
            	File cacheDir = this.getExternalCacheDir();
            	File configJsonFile = new File(cacheDir, "config.json");
            	
        		FileOutputStream fout = new FileOutputStream(configJsonFile);

        		fout.write(root.toString(2).getBytes(Charset.defaultCharset().name()));

        		fout.flush();
        		fout.close();
        		
        		Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, this.getString(R.string.message_mail_app_schema));
        		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, this.getString(R.string.subject_mail_app_schema));
        		emailIntent.setType("text/plain");

        		Uri uri = Uri.fromFile(configJsonFile);
        		emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
        		this.startActivity(emailIntent);
			}
        	catch (JSONException e) 
        	{
     			LogManager.getInstance(this).logException(e);
			}
        	catch (NameNotFoundException e) 
        	{
     			LogManager.getInstance(this).logException(e);
			} 
        	catch (FileNotFoundException e) 
        	{
     			LogManager.getInstance(this).logException(e);
			} 
        	catch (IOException e) 
        	{
     			LogManager.getInstance(this).logException(e);
			}
        }
        else if (RESET_KEY.equals(preference.getKey()))
        {
        	final SettingsActivity me = this;
        	
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	
        	builder = builder.setTitle(R.string.title_clear_configuration);
        	builder = builder.setMessage(R.string.message_clear_configuration);
        	
        	builder = builder.setPositiveButton(R.string.button_clear_yes, new OnClickListener()
        	{
				public void onClick(DialogInterface dialog, int which) 
				{
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(me);
					Editor e = prefs.edit();

					Map<String, ?> prefMap = prefs.getAll();
					
					for (String key : prefMap.keySet())
					{
						e.remove(key);
					}
					
					e.commit();
					
					me.startService(new Intent(PersistentService.NUDGE_PROBES));
					
					TriggerManager.getInstance(me).removeAllTriggers();
					TriggerManager.getInstance(me).refreshTriggers(me);
					HttpUploadPlugin.clearFiles(me);
					
					String where = "_id != -1";
					
					me.getContentResolver().delete(RobotContentProvider.RECENT_PROBE_VALUES, where, null);
					me.getContentResolver().delete(RobotContentProvider.SNAPSHOTS, where, null);
					me.getContentResolver().delete(DistancesProvider.CONTENT_URI, where, null);
					
					ProbeValuesProvider.getProvider(me).clear(me);
					
					android.os.Process.killProcess(android.os.Process.myPid());
				}
        	});
        	
        	builder = builder.setNegativeButton(R.string.button_clear_no, new OnClickListener()
        	{
				public void onClick(DialogInterface dialog, int which) 
				{
					// TODO Auto-generated method stub
					
				}
        	});
        	
        	builder.create().show();
        }

        return false;
	}

	private JSONObject dumpJson(Preference preference) throws JSONException 
	{
		JSONObject prefJson = new JSONObject();
		
		if (preference.getKey() != null)
			prefJson.put("key", preference.getKey());

		if (preference.getTitle() != null)
			prefJson.put("title", preference.getTitle());

		if (preference.getSummary() != null)
			prefJson.put("summary", preference.getSummary());

		if (preference instanceof PreferenceGroup)
		{
			if ("config_settings_trigger_category".equals(preference.getKey()))
				return null;
			else
			{
				PreferenceGroup group = (PreferenceGroup) preference;
				
				if (group.getPreferenceCount() == 0)
					return null;
				
				prefJson.put("type", "group");
				
				JSONArray children = new JSONArray();
				
				for (int i = 0; i < group.getPreferenceCount(); i++)
				{
					JSONObject child = this.dumpJson(group.getPreference(i));
					
					if (child != null)
						children.put(child);
				}
	
				prefJson.put("children", children);
			}
		}
		else if (preference instanceof CheckBoxPreference)
			prefJson.put("type", "boolean");
		else if (preference instanceof EditTextPreference)
			prefJson.put("type", "string");
		else if (preference instanceof ListPreference)
		{
			ListPreference list = (ListPreference) preference;
			prefJson.put("type", "list");
			
			JSONArray entries = new JSONArray();
			
			for (CharSequence cs : list.getEntries())
				entries.put(cs);

			prefJson.put("labels", entries);

			JSONArray values = new JSONArray();
			
			for (CharSequence cs : list.getEntryValues())
				values.put(cs);

			prefJson.put("values", values);
		}
		else
			prefJson = null;
		
		return prefJson;
	}

	public boolean onPreferenceChange(Preference pref, Object value) 
	{
		if (CHECK_UPDATES_KEY.equals(pref.getKey()))
		{
			Toast.makeText(this, R.string.message_update_check, Toast.LENGTH_LONG).show();

			return true;
		}
		else if (RINGTONE_KEY.equals(pref.getKey()))
		{
			String name = ManagerService.soundNameForPath(this, value.toString());
			
        	Intent playIntent = new Intent(ManagerService.RINGTONE_INTENT);
        	
        	if (name != null)
        		playIntent.putExtra(SettingsActivity.RINGTONE_KEY, name);
        	
        	this.startService(playIntent);
        	
        	return true;
		}

		return false;
	}
}
