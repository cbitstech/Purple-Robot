package edu.northwestern.cbits.purple.notifier;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public abstract class WidgetSettingsActivity extends SherlockPreferenceActivity 
{
	protected int _widgetId = Integer.MAX_VALUE;
	
	protected void onResume()
	{
		super.onResume();

		this._widgetId = this.getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, Integer.MAX_VALUE);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));

        this.setResult(Activity.RESULT_OK, resultValue);
	}
	
	protected void onPause()
	{
		super.onPause();
		
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        this.onSharedPreferenceChanged(prefs, null);
	}

	public abstract void onSharedPreferenceChanged(SharedPreferences prefs, String key);
}
