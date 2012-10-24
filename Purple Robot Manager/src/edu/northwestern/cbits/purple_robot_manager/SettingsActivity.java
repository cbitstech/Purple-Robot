package edu.northwestern.cbits.purple_robot_manager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceClickListener
{
	public static final String PROBES_SCREEN_KEY = "config_probes_screen";
	private static final String MANUAL_REFRESH_KEY = "config_json_refresh_manually";
	private static final String HAPTIC_PATTERN_KEY = "config_json_haptic_pattern";
	public static final String RINGTONE_KEY = "config_default_notification_sound";

	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.layout.layout_settings_activity);

        PreferenceScreen prefs = getPreferenceScreen();

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
        {
        	Log.e("PRM", "START PROBES KEY");

			return true;
        }
        else if (MANUAL_REFRESH_KEY.equals(preference.getKey()))
        {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
			Editor editor = prefs.edit();

			Log.e("PRM", "PREFS: " + prefs.getClass().getCanonicalName() + " " + prefs.hashCode());

			editor.putLong(JSONConfigFile.JSON_LAST_UPDATE, 0);
			editor.commit();
			JSONConfigFile.update(this);

			Intent funfIntent = new Intent(FunfService.ACTION_RELOAD);
			this.startService(funfIntent);

            return true;
        }

        return false;
	}
}
