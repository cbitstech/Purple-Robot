package edu.northwestern.cbits.purple_robot_manager;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity 
{
    @SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.layout.layout_settings_activity);
    }
}
