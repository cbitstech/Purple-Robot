package edu.northwestern.cbits.purple_robot_manager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.plugins.HttpUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPluginManager;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceClickListener, OnPreferenceChangeListener
{
	public static final String PROBES_SCREEN_KEY = "config_probes_screen";
	private static final String MANUAL_REFRESH_KEY = "config_json_refresh_manually";
	private static final String HAPTIC_PATTERN_KEY = "config_json_haptic_pattern";
	public static final String RINGTONE_KEY = "config_default_notification_sound";
	public static final String ZIP_ARCHIVES_KEY = "config_mail_archives";
	public static final String DELETE_ARCHIVES_KEY = "config_delete_archives";
	static final CharSequence USER_ID_KEY = "config_user_id";
	protected static final String USER_HASH_KEY = "config_user_hash";
	public static final String CHECK_UPDATES_KEY = "config_hockey_update";
	public static final String TRIGGERS_SCREEN_KEY = "config_triggers_screen";

	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.layout.layout_settings_activity);

        PreferenceScreen prefs = this.getPreferenceScreen();

        Preference refresh = prefs.findPreference(MANUAL_REFRESH_KEY);
        refresh.setOnPreferenceClickListener(this);

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

        PreferenceScreen triggersScreen = TriggerManager.buildPreferenceScreen(this);

        PreferenceCategory triggerCategory = (PreferenceCategory) prefs.findPreference("config_settings_trigger_category");
        triggerCategory.addPreference(triggersScreen);
        
        Preference archive = prefs.findPreference(ZIP_ARCHIVES_KEY);
        archive.setOnPreferenceClickListener(this);

        Preference delete = prefs.findPreference(DELETE_ARCHIVES_KEY);
        delete.setOnPreferenceClickListener(this);
        
        CheckBoxPreference update = (CheckBoxPreference) prefs.findPreference(CHECK_UPDATES_KEY);
        update.setOnPreferenceChangeListener(this);
    }

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

			editor.putLong(JSONConfigFile.JSON_LAST_UPDATE, 0);
			editor.commit();
			JSONConfigFile.update(this);

			ProbeManager.nudgeProbes(this);

            return true;
        }
        else if (ZIP_ARCHIVES_KEY.equals(preference.getKey()))
        {
        	HttpUploadPlugin plugin = (HttpUploadPlugin) OutputPluginManager.sharedInstance.pluginForClass(HttpUploadPlugin.class);

        	plugin.mailArchiveFiles(this);

        	return true;
        }
        else if (DELETE_ARCHIVES_KEY.equals(preference.getKey()))
        {
        	HttpUploadPlugin plugin = (HttpUploadPlugin) OutputPluginManager.sharedInstance.pluginForClass(HttpUploadPlugin.class);

        	plugin.deleteArchiveFiles(this);

        	return true;
        }

        return false;
	}

	public boolean onPreferenceChange(Preference pref, Object value) 
	{
		if (CHECK_UPDATES_KEY.equals(pref.getKey()))
		{
			Toast.makeText(this, R.string.message_update_check, Toast.LENGTH_LONG).show();

			return true;
		}

		return false;
	}
}
