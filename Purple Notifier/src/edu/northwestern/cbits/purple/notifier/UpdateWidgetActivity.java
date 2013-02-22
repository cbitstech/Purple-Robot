package edu.northwestern.cbits.purple.notifier;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class UpdateWidgetActivity extends SherlockPreferenceActivity implements OnPreferenceClickListener
{
	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.layout.layout_update_activity);

		Preference update = this.findPreference("config_update_widget");
		update.setOnPreferenceClickListener(this);
    }
	
	protected void onResume()
	{
		super.onResume();
		
		String identifier = this.getIntent().getStringExtra("identifier");
		
		this.getSupportActionBar().setTitle(identifier);
	}

	public boolean onPreferenceClick(Preference preference) 
	{
		Log.e("PN", "CLICK");

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
        Intent intent = new Intent(WidgetIntentService.UPDATE_WIDGETS);
		
		intent.putExtra("title", prefs.getString("config_update_title", this.getString(R.string.config_message_title)));
		intent.putExtra("message", prefs.getString("config_update_message", this.getString(R.string.config_message_title)));
		intent.putExtra("identifier", this.getIntent().getStringExtra("identifier"));

		this.startService(intent);
		
		Log.e("PN", "INTENT SENT!");

		this.finish();
		
		return true;
	}
}
