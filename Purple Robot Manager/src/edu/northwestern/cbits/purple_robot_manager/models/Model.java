package edu.northwestern.cbits.purple_robot_manager.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public abstract class Model 
{
	public static final boolean DEFAULT_ENABLED = true;
	private static long _lastEnabledCheck = 0;
	private static boolean _lastEnabled = false;

	public abstract String getPreferenceKey();
	public abstract String title(Context context);
	public abstract String summary(Context context);

	public static Model modelForUrl(Context context, String jsonUrl) 
	{
		String hash = EncryptionManager.getInstance().createHash(context, jsonUrl);
		
		SharedPreferences prefs = Probe.getPreferences(context);

		File internalStorage = context.getFilesDir();

		if (prefs.getBoolean("config_external_storage", false))
			internalStorage = context.getExternalFilesDir(null);

		if (internalStorage != null && !internalStorage.exists())
			internalStorage.mkdirs();

		File modelsFolder = new File(internalStorage, "persisted_models");

		if (modelsFolder != null && !modelsFolder.exists())
			modelsFolder.mkdirs();
		
		String contents = null;
		File cachedModel = new File(modelsFolder, hash);
		
		try 
		{
			contents = FileUtils.readFileToString(cachedModel);
		}
		catch (IOException e) 
		{

		}
		
		try 
		{
			URL u = new URL(jsonUrl);

	        BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
	        
	        StringBuffer sb = new StringBuffer();
	        
	        String inputLine = null;
	        
	        while ((inputLine = in.readLine()) != null)
	        	sb.append(inputLine);

	        in.close();
	        
	        contents = sb.toString();
		} 
		catch (MalformedURLException e) 
		{
			LogManager.getInstance(context).logException(e);
		} 
		catch (IOException e) 
		{
			LogManager.getInstance(context).logException(e);
		} 
		
		if (contents != null)
		{
			try
			{
		        JSONObject json = new JSONObject(contents);
		        
		        String type = json.getString("model_type");
		        
		        if ("regression".equals(type))
		        	return new RegressionModel(context, Uri.parse(jsonUrl));
		        if ("decision-tree".equals(type))
		        	return new TreeModel(context, Uri.parse(jsonUrl));
		        
			}
			catch (JSONException e) 
			{
				LogManager.getInstance(context).logException(e);
			} 
		}
		
		return null;
	}

	public Uri uri() 
	{
		return null;
	}

	public void enable(Context context)
	{
		String key = this.getPreferenceKey();

		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_model_" + key + "_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		String key = this.getPreferenceKey();

		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_model_" + key + "_enabled", false);
		
		e.commit();
	}
	
	public boolean enabled(Context context) 
	{
		if (ModelManager.getInstance(context).enabled(context))
		{
			String key = this.getPreferenceKey();
	
			SharedPreferences prefs = Probe.getPreferences(context);
			
			return prefs.getBoolean("config_model_" + key + "_enabled", Model.DEFAULT_ENABLED);
		}
		
		return false;
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(this.summary(activity));

		String key = this.getPreferenceKey();

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_model);
		enabled.setKey("config_model_" + key + "_enabled");
		enabled.setDefaultValue(Model.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		return screen;
	}
	
	public boolean isEnabled(Context context)
	{
		long now = System.currentTimeMillis();
		SharedPreferences prefs = Probe.getPreferences(context);
		
		if (now - Model._lastEnabledCheck  > 10000)
		{
			Model._lastEnabledCheck = now;

			Model._lastEnabled  = prefs.getBoolean("config_models_enabled", Model.DEFAULT_ENABLED);
		}
		
		if (Model._lastEnabled)
		{
			String key = this.getPreferenceKey();

			return prefs.getBoolean("config_model_" + key + "_enabled", true);
		}
		
		return Model._lastEnabled;
	}
	
	protected void transmitPrediction(Context context, double prediction, double accuracy) 
	{
		Bundle bundle = new Bundle();
		bundle.putString("PROBE", this.name(context));
		bundle.putDouble("TIMESTAMP", ((double) System.currentTimeMillis()) / 1000);
		bundle.putDouble("PREDICTION", prediction);
		bundle.putBoolean("FROM_MODEL", true);

		this.transmitData(context, bundle);
	}

	protected void transmitPrediction(Context context, String prediction, double accuracy) 
	{
		Bundle bundle = new Bundle();
		bundle.putString("PROBE", this.name(context));
		bundle.putDouble("TIMESTAMP", ((double) System.currentTimeMillis()) / 1000);
		bundle.putString("PREDICTION", prediction);
		bundle.putBoolean("FROM_MODEL", true);
		bundle.putDouble("ACCURACY", accuracy);

		this.transmitData(context, bundle);
	}

	protected void transmitData(Context context, Bundle data) 
	{
		if (context != null)
		{
			UUID uuid = UUID.randomUUID();
			data.putString("GUID", uuid.toString());
			data.putString("MODEL_NAME", this.title(context));

			LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
			Intent intent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
			intent.putExtras(data);

			localManager.sendBroadcast(intent);
		}
	}

	public abstract void predict(Context context, HashMap<String, Object> snapshot);
	protected abstract String name(Context context);
}
