package edu.northwestern.cbits.purple_robot.shionhelper;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MainActivity extends PreferenceActivity
{
    @SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.layout.layout_settings_activity);
    }

    /*
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        this.getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) 
    {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }
    */
}
