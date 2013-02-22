package edu.northwestern.cbits.purple.notifier;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;

public class BasicSettingsActivity extends WidgetSettingsActivity 
{
	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.layout.layout_basic_settings_activity);
        
        PreferenceScreen screen = this.getPreferenceScreen();
        
        ListPreference idList = new ListPreference(this);
        idList.setTitle(R.string.config_identifiers_title);
        idList.setDialogTitle(R.string.config_identifiers_title);
        idList.setKey("config_basic_identifier");

        String[] identifiers = IdentifiersManager.fetchIdentifiers(this);

        idList.setEntries(identifiers);
        idList.setEntryValues(identifiers);
        
        screen.addPreference(idList);
    }
	
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) 
	{
        Intent intent = new Intent(WidgetIntentService.UPDATE_WIDGET);
		
		intent.putExtra(WidgetIntentService.WIDGET, BasicWidgetProvider.NAME);
		intent.putExtra("title", prefs.getString("config_basic_title", this.getString(R.string.config_message_title)));
		intent.putExtra("message", prefs.getString("config_basic_message", this.getString(R.string.config_message_title)));
		intent.putExtra("identifier", prefs.getString("config_basic_identifier", this.getString(R.string.config_message_title)));
		intent.putExtra("image", prefs.getString("config_basic_image", ""));

		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this._widgetId);

		this.startService(intent);
	}
}
