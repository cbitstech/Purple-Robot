package edu.northwestern.cbits.purple_robot_manager.logging;

import org.json.JSONArray;
import org.json.JSONException;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.plugins.HttpUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPluginManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class UploadProgressCheck extends SanityCheck 
{
	private static final String THROUGHPUT = "check_upload_throughput";
	private static final String ACCUMULATION = "check_upload_accumulation";
	private static final long WINDOW = (1000 * 60 * 60 * 6);
	
	private boolean _inited = false;
	
	public String name(Context context) 
	{
		return context.getString(R.string.name_sanity_upload_progress);
	}
	
	public void runCheck(Context context) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		HttpUploadPlugin.fixUploadPreference(context);

		if (prefs.getBoolean("config_enable_data_server", false) == false)
		{
			this._errorLevel = SanityCheck.OK;
			return;
		}
		
		Editor e = prefs.edit();

		if (this._inited == false)
		{
			e.remove(UploadProgressCheck.THROUGHPUT);
			e.remove(UploadProgressCheck.ACCUMULATION);
			
			e.commit();
		}
		
		long now = System.currentTimeMillis();
		
		final HttpUploadPlugin plugin = (HttpUploadPlugin) OutputPluginManager.sharedInstance.pluginForClass(context, HttpUploadPlugin.class);
		
		if (plugin != null)
		{
			double throughput = plugin.getRecentThroughput();
			double accumulation = plugin.getRecentAccumulation();
			
			double throughputSum = throughput;
			double accumulationSum = accumulation;
			
			try 
			{
				JSONArray uploads = new JSONArray(prefs.getString(UploadProgressCheck.THROUGHPUT, "[]"));
				
				JSONArray newUploads = new JSONArray();
				
				for (int i = 0; i < uploads.length(); i++)
				{
					JSONArray sample = uploads.getJSONArray(i);
					
					double timestamp = sample.getLong(0);
					
					if (now - timestamp <= UploadProgressCheck.WINDOW)
					{
						newUploads.put(sample);
						throughputSum += sample.getDouble(1);
					}
				}
				
				JSONArray thisSample = new JSONArray();
				thisSample.put(now);
				thisSample.put(throughput);
				
				newUploads.put(thisSample);
				
				double averageThroughput = throughputSum / newUploads.length();

				// ---
				JSONArray accumlates = new JSONArray(prefs.getString(UploadProgressCheck.ACCUMULATION, "[]"));
				
				JSONArray newAccumlates = new JSONArray();
				
				for (int i = 0; i < accumlates.length(); i++)
				{
					JSONArray sample = accumlates.getJSONArray(i);
					
					double timestamp = sample.getLong(0);
					
					if (now - timestamp <= UploadProgressCheck.WINDOW)
					{
						newAccumlates.put(sample);
						accumulationSum += sample.getDouble(1);
					}
				}
				
				thisSample = new JSONArray();
				thisSample.put(now);
				thisSample.put(accumulation);
				
				newAccumlates.put(thisSample);
				
				e.putString(UploadProgressCheck.THROUGHPUT, newUploads.toString());
				e.putString(UploadProgressCheck.ACCUMULATION, newAccumlates.toString());
				e.commit();
				
				double averageAccumulation = accumulationSum / newUploads.length();
				
				if (averageThroughput < averageAccumulation)
				{
					this._errorLevel = SanityCheck.ERROR;
					this._errorMessage = context.getString(R.string.name_sanity_upload_progress_error);
				}
				else if ((averageThroughput / 2) < averageAccumulation)
				{
					this._errorLevel = SanityCheck.WARNING;
					this._errorMessage = context.getString(R.string.name_sanity_upload_progress_warning);
				}
				else
				{
					this._errorLevel = SanityCheck.OK;
					this._errorMessage = null;
				}
			} 
			catch (JSONException ee) 
			{
				ee.printStackTrace();
				
				LogManager.getInstance(context).logException(ee);

				this._errorLevel = SanityCheck.WARNING;
				this._errorMessage = context.getString(R.string.name_sanity_upload_progress_unknown);
			}
		}
		else
		{
			this._errorLevel = SanityCheck.WARNING;
			this._errorMessage = context.getString(R.string.name_sanity_upload_progress_unknown);
		}

	}
}
