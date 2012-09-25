package edu.northwestern.cbits.purple_robot_manager;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceClickListener
{
	private static String MANUAL_REFRESH_KEY = "config_json_refresh_manually";

    @SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.layout.layout_settings_activity);

        PreferenceScreen prefs = getPreferenceScreen();

        Preference refresh = prefs.findPreference(MANUAL_REFRESH_KEY);
        refresh.setOnPreferenceClickListener(this);
    }

	public boolean onPreferenceClick(Preference preference)
	{
        if (MANUAL_REFRESH_KEY.equals(preference.getKey()) )
        {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			Editor editor = prefs.edit();

			editor.putLong(JSONConfigFile.JSON_LAST_UPDATE, 0);
			editor.commit();

            return true;
        }

        return false;
	}
}
