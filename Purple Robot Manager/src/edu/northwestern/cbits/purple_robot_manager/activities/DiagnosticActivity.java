package edu.northwestern.cbits.purple_robot_manager.activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.config.SchemeConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.plugins.HttpUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPluginManager;

@SuppressLint("SimpleDateFormat")
public class DiagnosticActivity extends ActionBarActivity 
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
		
		this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		TextView userId = (TextView) this.findViewById(R.id.user_id_value);
		TextView probeStatus = (TextView) this.findViewById(R.id.probe_status_value);
		TextView uploadStatus = (TextView) this.findViewById(R.id.upload_status_value);
		TextView lastUpload = (TextView) this.findViewById(R.id.last_upload_value);
		TextView prVersion = (TextView) this.findViewById(R.id.pr_version_value);
		
		userId.setText("\"" + EncryptionManager.getInstance().getUserId(this) + "\"");

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		boolean probeEnabled = prefs.getBoolean("config_probes_enabled", false);
		
		if (probeEnabled)
			probeStatus.setText(R.string.probe_status_enabled);
		else
			probeStatus.setText(R.string.probe_status_disabled);

		PurpleRobotApplication.fixPreferences(this, true);
		
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

		OutputPlugin plugin = OutputPluginManager.sharedInstance.pluginForClass(this, HttpUploadPlugin.class);
		
		if (plugin instanceof HttpUploadPlugin)
		{
			HttpUploadPlugin http = (HttpUploadPlugin) plugin;

			TextView uploadCount = (TextView) this.findViewById(R.id.pending_files_value);
			uploadCount.setText(this.getString(R.string.pending_files_file, http.pendingFilesCount()));
		}
		
		try 
		{
			PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
			prVersion.setText(pInfo.versionName);
		} 
		catch (NameNotFoundException e) 
		{
			LogManager.getInstance(this).logException(e);
		}
		
		TextView okText = (TextView) this.findViewById(R.id.pr_error_none_value);
		LinearLayout errorList = (LinearLayout) this.findViewById(R.id.pr_error_list);
		errorList.removeAllViews();
		
		final SanityManager sanity = SanityManager.getInstance(this);
		
		if (sanity.getErrorLevel() != SanityCheck.OK)
		{
			errorList.setVisibility(View.VISIBLE);
			okText.setVisibility(View.GONE);
			
			Map<String, String> errors = sanity.errors();
			
			if (errors.size() > 0)
			{
				errorList.setVisibility(View.VISIBLE);
				
				for (String error : errors.keySet())
				{
					TextView errorLine = new TextView(this);
					errorLine.setText(errors.get(error));
					errorLine.setTextColor(0xffff4444);
					errorLine.setTextSize(18);
					
					LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
					layout.setMargins(0, 0, 0, 10);
					errorLine.setLayoutParams(layout);
					
					final String errorKey = error; 
					
					errorLine.setOnClickListener(new OnClickListener()
					{
						public void onClick(View view) 
						{
							sanity.runActionForAlert(errorKey);
						}
						
					});
					
					errorList.addView(errorLine);
				}
			}
			
			Map<String, String> warnings = sanity.warnings();

			if (warnings.size() > 0)
			{
				for (String error : warnings.keySet())
				{
					TextView errorLine = new TextView(this);
					errorLine.setText(warnings.get(error));
					errorLine.setTextColor(0xffffbb33);
					errorLine.setTextSize(18);
					
					LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
					layout.setMargins(0, 0, 0, 10);
					errorLine.setLayoutParams(layout);
					
					final String errorKey = error; 
					
					errorLine.setOnClickListener(new OnClickListener()
					{
						public void onClick(View view) 
						{
							sanity.runActionForAlert(errorKey);
						}
						
					});

					errorList.addView(errorLine);
				}
			}
		}
		else
		{
			errorList.setVisibility(View.GONE);
			okText.setVisibility(View.VISIBLE);
		}
		
		TextView sensorsList = (TextView) this.findViewById(R.id.available_sensors_value);
		StringBuffer sb = new StringBuffer();

    	SensorManager sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
    	
    	for (Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL))
    	{
    		if (sb.length() > 0)
    			sb.append("\n");
    		
    		sb.append(s.getName());
    	}
    	
    	sensorsList.setText(sb.toString());
	}
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_diagnostics, menu);

        return true;
	}

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
    	{
		    case android.R.id.home:
				if (this.isTaskRoot())
				{
					Intent intent = new Intent(this, StartActivity.class);
					this.startActivity(intent);
				}

				this.finish();
    		case R.id.menu_email_item:
    			StringBuffer message = new StringBuffer();

         		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
         		
         		String newline = System.getProperty("line.separator");

         		message.append(this.getString(R.string.user_id_label));
         		message.append(newline);
         		message.append("\"" + EncryptionManager.getInstance().getUserId(this) + "\"");

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

        		PurpleRobotApplication.fixPreferences(this, true);

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

         		message.append(newline);
         		message.append(newline);

        		OutputPlugin plugin = OutputPluginManager.sharedInstance.pluginForClass(this, HttpUploadPlugin.class);
        		
        		if (plugin instanceof HttpUploadPlugin)
        		{
        			HttpUploadPlugin http = (HttpUploadPlugin) plugin;
        			
        			message.append(this.getString(R.string.robot_pending_count_label));
             		message.append(newline);
        			message.append(this.getString(R.string.pending_files_file, http.pendingFilesCount()));

             		message.append(newline);
             		message.append(newline);
        		}
         		
         		message.append(this.getString(R.string.pr_version_label));
         		message.append(newline);
         		
         		try 
        		{
        			PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
        			message.append(pInfo.versionName);
        		} 
        		catch (NameNotFoundException e) 
        		{
        			LogManager.getInstance(this).logException(e);
        		}

         		message.append(newline);
         		message.append(newline);

         		message.append(this.getString(R.string.available_sensors_label));
         		message.append(newline);

         		StringBuffer sb = new StringBuffer();
         		
            	SensorManager sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
            	
            	for (Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL))
            	{
            		if (sb.length() > 0)
            			sb.append("\n");
            		
            		sb.append(s.getName());
            	}
            	
            	message.append(sb.toString());
         		
         		try
         		{
	         		Intent intent = new Intent(Intent.ACTION_SEND);
	
	         		intent.setType("message/rfc822");
	         		intent.putExtra(Intent.EXTRA_SUBJECT, this.getString(R.string.email_diagnostic_subject));
	         		intent.putExtra(Intent.EXTRA_TEXT, message.toString());

	         		SchemeConfigFile scheme = new SchemeConfigFile(this);

	            	File cacheDir = this.getExternalCacheDir();
	            	File configFile = new File(cacheDir, "config.scm");
	            	
	        		FileOutputStream fout = new FileOutputStream(configFile);

	        		fout.write(scheme.toString().getBytes(Charset.defaultCharset().name()));

	        		fout.flush();
	        		fout.close();
	        		
	        		intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(configFile));
	
	         		this.startActivity(intent);
         		}
         		catch (ActivityNotFoundException e)
         		{
         			Intent mailIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:c-karr@northwestern.edu"));
         			mailIntent.putExtra(Intent.EXTRA_SUBJECT, this.getString(R.string.email_diagnostic_subject));
         			mailIntent.putExtra(Intent.EXTRA_TEXT, message.toString());

         			this.startActivity(mailIntent);
         		} 
         		catch (FileNotFoundException e) 
         		{
					e.printStackTrace();
				} 
         		catch (IOException e) 
         		{
					e.printStackTrace();
				}
         		
    			break;
    	}
        
        return true;
    }
}
