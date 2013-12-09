package edu.northwestern.cbits.purple_robot_manager.logging;

import java.io.File;
import java.io.FilenameFilter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.plugins.HttpUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPluginManager;

public class LastUploadCheck extends SanityCheck 
{
	private static long WARN_DURATION = (1000 * 60 * 60 * 12);
	private static long ERROR_DURATION = WARN_DURATION * 2;
	
	public String name(Context context) 
	{
		return context.getString(R.string.name_sanity_last_upload);
	}

	public void runCheck(Context context) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		PurpleRobotApplication.fixPreferences(context, false);

		if (prefs.getBoolean("config_enable_data_server", false) == false)
		{
			this._errorLevel = SanityCheck.OK;
			return;
		}
		
		OutputPlugin plugin = OutputPluginManager.sharedInstance.pluginForClass(context, HttpUploadPlugin.class);
		
		if (plugin instanceof HttpUploadPlugin)
		{
			HttpUploadPlugin http = (HttpUploadPlugin) plugin;

			if (http.pendingFilesCount() < 8)
			{
				this._errorLevel = SanityCheck.OK;
				
				return;
			}
		}
		
		long now = System.currentTimeMillis();
		
		long lastUploadTime = prefs.getLong("http_last_upload", 0);
		
		this._errorLevel = SanityCheck.OK;
		
		if (lastUploadTime == 0)
		{
			if (plugin != null && plugin instanceof HttpUploadPlugin)
			{
				final HttpUploadPlugin httpPlugin = (HttpUploadPlugin) plugin;

				File pendingFolder = httpPlugin.getPendingFolder();

				int pendingCount = 0;

				FilenameFilter jsonFilter =  new FilenameFilter()
				{
					public boolean accept(File dir, String filename)
					{
						return filename.endsWith(".json");
					}
				};

				String[] filenames = pendingFolder.list(jsonFilter);
		
				if (filenames == null)
					filenames = new String[0];

				pendingCount = filenames.length;
				
				if (pendingCount > 0)
				{
					this._errorLevel = SanityCheck.ERROR;
					this._errorMessage = context.getString(R.string.name_sanity_last_upload_never);
				}
			}
		}
		else if (now - lastUploadTime > LastUploadCheck.ERROR_DURATION)
		{
			this._errorLevel = SanityCheck.WARNING;
			this._errorMessage = context.getString(R.string.name_sanity_last_upload_error);
		}
		else if (now - lastUploadTime > LastUploadCheck.WARN_DURATION)
		{
			this._errorLevel = SanityCheck.WARNING;
			this._errorMessage = context.getString(R.string.name_sanity_last_upload_warning);
		}
	}
}
