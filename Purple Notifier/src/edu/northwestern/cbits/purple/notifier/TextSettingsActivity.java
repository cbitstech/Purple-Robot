package edu.northwestern.cbits.purple.notifier;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class TextSettingsActivity extends SherlockPreferenceActivity implements OnSharedPreferenceChangeListener 
{
	private int _widgetId = Integer.MAX_VALUE;
	
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
	
	protected void onResume()
	{
		super.onResume();

		this._widgetId = this.getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, Integer.MAX_VALUE);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));

        this.setResult(Activity.RESULT_OK, resultValue);
        
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	protected void onPause()
	{
        super.onPause();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        prefs.unregisterOnSharedPreferenceChangeListener(this);
        
        this.onSharedPreferenceChanged(prefs, null);
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
