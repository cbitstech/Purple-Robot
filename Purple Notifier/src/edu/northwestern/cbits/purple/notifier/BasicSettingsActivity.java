package edu.northwestern.cbits.purple.notifier;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class BasicSettingsActivity extends SherlockPreferenceActivity implements OnSharedPreferenceChangeListener 
{
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.layout.layout_basic_settings_activity);
    }
	
	protected void onResume()
	{
		super.onResume();

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));

        this.setResult(Activity.RESULT_OK, resultValue);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	protected void onPause()
	{
		super.onPause();
		
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) 
	{
		Intent intent = new Intent(WidgetIntentService.UPDATE_WIDGET);
		
		intent.putExtra(WidgetIntentService.WIDGET, BasicWidgetProvider.NAME);
		intent.putExtra("config_basic_title", prefs.getString("config_basic_title", this.getString(R.string.config_message_title)));
		intent.putExtra("config_basic_message", prefs.getString("config_basic_message", this.getString(R.string.config_message_title)));
		
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, Integer.MAX_VALUE));
		
		this.startService(intent);
	}
}
