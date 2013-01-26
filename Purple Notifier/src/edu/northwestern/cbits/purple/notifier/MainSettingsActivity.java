package edu.northwestern.cbits.purple.notifier;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import android.os.Bundle;

public class MainSettingsActivity extends SherlockPreferenceActivity 
{
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.layout.layout_settings_activity);
    }
}
