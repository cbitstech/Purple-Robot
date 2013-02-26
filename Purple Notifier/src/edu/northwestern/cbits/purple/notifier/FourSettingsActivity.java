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

public class FourSettingsActivity extends WidgetSettingsActivity 
{
	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.layout.layout_four_settings_activity);
        
        PreferenceScreen screen = this.getPreferenceScreen();
        
        ListPreference idList = new ListPreference(this);
        idList.setTitle(R.string.config_identifiers_title);
        idList.setDialogTitle(R.string.config_identifiers_title);
        idList.setKey("config_four_identifier");

        String[] identifiers = IdentifiersManager.fetchIdentifiers(this);

        idList.setEntries(identifiers);
        idList.setEntryValues(identifiers);
        
        screen.addPreference(idList);

        EditTextPreference editPreference = (EditTextPreference) screen.findPreference("config_four_one");
        EditText field = editPreference.getEditText();
        field.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        
        editPreference = (EditTextPreference) screen.findPreference("config_four_two");
        field = editPreference.getEditText();
        field.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

        editPreference = (EditTextPreference) screen.findPreference("config_four_three");
        field = editPreference.getEditText();
        field.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

        editPreference = (EditTextPreference) screen.findPreference("config_four_four");
        field = editPreference.getEditText();
        field.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
    }
	
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) 
	{
        Intent intent = new Intent(WidgetIntentService.UPDATE_WIDGET);
		
		intent.putExtra(WidgetIntentService.WIDGET, FourWidgetProvider.NAME);
		intent.putExtra("identifier", prefs.getString("config_four_identifier", this.getString(R.string.config_message_title)));

		intent.putExtra("image", prefs.getString("config_four_one", ""));
		intent.putExtra("image_two", prefs.getString("config_four_two", ""));
		intent.putExtra("image_three", prefs.getString("config_four_three", ""));
		intent.putExtra("image_four", prefs.getString("config_four_four", ""));

		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this._widgetId);

		this.startService(intent);
	}
}
