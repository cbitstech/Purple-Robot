package edu.northwestern.cbits.purple.notifier;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class TextSettingsActivity extends SherlockPreferenceActivity implements OnSharedPreferenceChangeListener 
{
	private int _widgetId = Integer.MAX_VALUE;
	
	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Log.e("PN-SETUP", "CREATE");

        this.addPreferencesFromResource(R.layout.layout_text_settings_activity);
    }
	
	protected void onResume()
	{
		super.onResume();

        Log.e("PN-SETUP", "RESUME");

		this._widgetId = this.getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, Integer.MAX_VALUE);

		Log.e("PN-SETUP", "TEXT WIDGET ID: " + this._widgetId);
		
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));

        this.setResult(Activity.RESULT_OK, resultValue);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	protected void onPause()
	{
		super.onPause();

        Log.e("PN-SETUP", "PAUSE");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) 
	{
        Log.e("PN-SETUP", "PREF CHANGE");

        Intent intent = new Intent(WidgetIntentService.UPDATE_WIDGET);
		
		intent.putExtra(WidgetIntentService.WIDGET, TextWidgetProvider.NAME);
		intent.putExtra("config_text_title", prefs.getString("config_text_title", this.getString(R.string.config_message_title)));
		intent.putExtra("config_text_message", prefs.getString("config_text_message", this.getString(R.string.config_message_title)));

		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this._widgetId);

		this.startService(intent);
	}
}
