package edu.northwestern.cbits.purple_robot_manager.activities;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;

public class DiagnosticActivity extends SherlockActivity 
{
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

        this.getSupportActionBar().setTitle(R.string.activity_diagnostic_title);

        this.setContentView(R.layout.layout_diagnostic_activity);
    }
	
	protected void onResume()
	{
		super.onResume();
		
		TextView userId = (TextView) this.findViewById(R.id.user_id_value);
		TextView probeStatus = (TextView) this.findViewById(R.id.probe_status_value);
		TextView uploadStatus = (TextView) this.findViewById(R.id.upload_status_value);
		TextView lastUpload = (TextView) this.findViewById(R.id.last_upload_value);
		
		userId.setText(EncryptionManager.getInstance().getUserId(this));

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		boolean probeEnabled = prefs.getBoolean("config_probes_enabled", false);
		
		if (probeEnabled)
			probeStatus.setText(R.string.probe_status_enabled);
		else
			probeStatus.setText(R.string.probe_status_disabled);

		boolean uploadEnabled = prefs.getBoolean("config_enable_data_server", false);
		
		if (uploadEnabled)
		{
			boolean wifiOnly = prefs.getBoolean("config_restrict_data_wifi", true);
			
			if (wifiOnly)
				uploadStatus.setText(R.string.upload_status_enabled_wifi_only);
			else
				uploadStatus.setText(R.string.upload_status_enabled);
		}
		else
			uploadStatus.setText(R.string.upload_status_disabled);
		
		if (prefs.contains("http_last_upload") && prefs.contains("http_last_payload_size"))
		{
			long lastUploadTime = prefs.getLong("http_last_upload", 0);
			long lastPayloadSize = prefs.getLong("http_last_payload_size", 0) / 1024;

			SimpleDateFormat sdf = new SimpleDateFormat("MMM d - HH:mm:ss");
			
			String dateString = sdf.format(new Date(lastUploadTime));
			
			lastUpload.setText(String.format(this.getString(R.string.last_upload_format), dateString, lastPayloadSize));
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
        MenuInflater inflater = this.getSupportMenuInflater();
        inflater.inflate(R.menu.menu_diagnostics, menu);

        return true;
	}

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
    	{
    		case R.id.menu_email_item:
    			StringBuffer message = new StringBuffer();

         		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
         		
         		String newline = System.getProperty("line.separator");

         		message.append(this.getString(R.string.user_id_label));
         		message.append(newline);
         		message.append(EncryptionManager.getInstance().getUserId(this));

         		message.append(newline);
         		message.append(newline);

         		message.append(this.getString(R.string.probe_status_label));
         		message.append(newline);

        		boolean probeEnabled = prefs.getBoolean("config_probes_enabled", false);
        		
        		if (probeEnabled)
             		message.append(this.getString(R.string.probe_status_enabled));
        		else
             		message.append(this.getString(R.string.probe_status_disabled));

         		message.append(newline);
         		message.append(newline);

         		message.append(this.getString(R.string.upload_status_label));
         		message.append(newline);

        		boolean uploadEnabled = prefs.getBoolean("config_enable_data_server", false);
        		
        		if (uploadEnabled)
        		{
        			boolean wifiOnly = prefs.getBoolean("config_restrict_data_wifi", true);
        			
        			if (wifiOnly)
                 		message.append(this.getString(R.string.upload_status_enabled_wifi_only));
        			else
                 		message.append(this.getString(R.string.upload_status_enabled));
        		}
        		else
             		message.append(this.getString(R.string.upload_status_disabled));

         		message.append(newline);
         		message.append(newline);

         		message.append(this.getString(R.string.last_upload_label));
         		message.append(newline);
         		
         		if (prefs.contains("http_last_upload") && prefs.contains("http_last_payload_size"))
				{
					long lastUploadTime = prefs.getLong("http_last_upload", 0);
					long lastPayloadSize = prefs.getLong("http_last_payload_size", 0) / 1024;
		
					SimpleDateFormat sdf = new SimpleDateFormat("MMM d - HH:mm:ss");
					
					String dateString = sdf.format(new Date(lastUploadTime));

	         		message.append(String.format(this.getString(R.string.last_upload_format), dateString, lastPayloadSize));
				}
         		else
             		message.append(this.getString(R.string.last_upload_placeholder));
         		
         		Intent intent = new Intent(Intent.ACTION_SEND);

         		intent.setType("message/rfc822");
         		intent.putExtra(Intent.EXTRA_SUBJECT, this.getString(R.string.email_diagnostic_subject));
         		intent.putExtra(Intent.EXTRA_TEXT, message.toString());

         		this.startActivity(intent);

    			break;
    	}
        
        return true;
    }
}
