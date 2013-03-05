package edu.northwestern.cbits.purple.notifier;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.widget.EditText;

public class BadgeSettingsActivity extends WidgetSettingsActivity 
{
	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.layout.layout_badge_settings_activity);
        
        PreferenceScreen screen = this.getPreferenceScreen();
        
        ListPreference idList = new ListPreference(this);
        idList.setTitle(R.string.config_identifiers_title);
        idList.setDialogTitle(R.string.config_identifiers_title);
        idList.setKey("config_badge_identifier");

        String[] identifiers = IdentifiersManager.fetchIdentifiers(this);

        idList.setEntries(identifiers);
        idList.setEntryValues(identifiers);
        
        screen.addPreference(idList);

        EditTextPreference editPreference = (EditTextPreference) screen.findPreference("config_badge_image");

        EditText field = editPreference.getEditText();
        field.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
    }
	
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) 
	{
        Intent intent = new Intent(WidgetIntentService.UPDATE_WIDGET);
		
		intent.putExtra(WidgetIntentService.WIDGET, BadgeWidgetProvider.NAME);
		intent.putExtra("title", prefs.getString("config_badge_title", this.getString(R.string.config_message_title)));
		intent.putExtra("message", prefs.getString("config_badge_message", this.getString(R.string.config_message_title)));
		intent.putExtra("identifier", prefs.getString("config_badge_identifier", this.getString(R.string.config_message_title)));
		intent.putExtra("image", prefs.getString("config_badge_image", ""));
		intent.putExtra("badge", prefs.getString("config_badge_badge", ""));
		intent.putExtra("color", prefs.getString("config_badge_color", "#FFFFFF"));

		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this._widgetId);

		this.startService(intent);
	}
}
