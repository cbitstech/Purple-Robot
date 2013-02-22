package edu.northwestern.cbits.purple.notifier;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;

public class TextSettingsActivity extends WidgetSettingsActivity 
{
	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.layout.layout_text_settings_activity);
        
        PreferenceScreen screen = this.getPreferenceScreen();
        
        ListPreference idList = new ListPreference(this);
        idList.setTitle(R.string.config_identifiers_title);
        idList.setDialogTitle(R.string.config_identifiers_title);
        idList.setKey("config_text_identifier");

        String[] identifiers = IdentifiersManager.fetchIdentifiers(this);

        idList.setEntries(identifiers);
        idList.setEntryValues(identifiers);
        
        screen.addPreference(idList);
    }
	
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) 
	{
        Intent intent = new Intent(WidgetIntentService.UPDATE_WIDGET);
		
		intent.putExtra(WidgetIntentService.WIDGET, TextWidgetProvider.NAME);
		intent.putExtra("title", prefs.getString("config_text_title", this.getString(R.string.config_message_title)));
		intent.putExtra("message", prefs.getString("config_text_message", this.getString(R.string.config_message_title)));
		intent.putExtra("identifier", prefs.getString("config_text_identifier", this.getString(R.string.config_message_title)));

		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this._widgetId);

		this.startService(intent);
	}
}
