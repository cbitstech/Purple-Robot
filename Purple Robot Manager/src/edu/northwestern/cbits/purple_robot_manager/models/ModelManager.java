package edu.northwestern.cbits.purple_robot_manager.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.SettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.util.Slugify;

public class ModelManager extends BroadcastReceiver
{
	private static ModelManager _instance = null;

	private List<Model> _models = new ArrayList<Model>();
	private HashMap<String, Object> _milieu = new HashMap<String, Object>();
	
    private ModelManager(Context context) 
    {
        if (ModelManager._instance != null)
            throw new IllegalStateException("Already instantiated");

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Probe.PROBE_READING);
		
		LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
		localManager.registerReceiver(this, intentFilter);

//        this._models.add(new NoiseModel());
		this._models.add(new TreeModel(context, Uri.parse("http://dashboard.cbits.northwestern.edu/media/brain/stats/1568_f0210f21-7204-4109-8706-835e09d0d641")));
    }

	public static ModelManager getInstance(Context context) 
    {
    	if (ModelManager._instance == null)
    	{
    		ModelManager._instance = new ModelManager(context.getApplicationContext());
    	}
    	
    	return ModelManager._instance;
    }

	public PreferenceScreen buildPreferenceScreen(PreferenceActivity settingsActivity) 
	{
		PreferenceManager manager = settingsActivity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(settingsActivity);
		screen.setOrder(0);
		screen.setTitle(R.string.title_preference_models_screen);
		screen.setKey(SettingsActivity.MODELS_SCREEN_KEY);

		PreferenceCategory globalCategory = new PreferenceCategory(settingsActivity);
		globalCategory.setTitle(R.string.title_preference_models_global_category);
		globalCategory.setKey("key_available_models");

		screen.addPreference(globalCategory);

		CheckBoxPreference enabled = new CheckBoxPreference(settingsActivity);
		enabled.setTitle(R.string.title_preference_models_enable_models);
		enabled.setKey("config_models_enabled");
		enabled.setDefaultValue(Model.DEFAULT_ENABLED);

		globalCategory.addPreference(enabled);

		PreferenceCategory probesCategory = new PreferenceCategory(settingsActivity);
		probesCategory.setTitle(R.string.title_preference_models_available_category);
		probesCategory.setKey("key_available_models");

		screen.addPreference(probesCategory);
		
		for (Model model : this.allModels(settingsActivity))
		{
			PreferenceScreen modelScreen = model.preferenceScreen(settingsActivity);

			if (modelScreen != null)
				screen.addPreference(modelScreen);
		}

		return screen;
	}

	private List<Model> allModels(Context context) 
	{
		ArrayList<Model> models = new ArrayList<Model>();
		models.addAll(this._models);
				
		return models;
	}

	public void onReceive(Context context, Intent intent) 
	{
		Bundle extras = intent.getExtras();
		
		if (extras.containsKey("FROM_MODEL"))
			return;

		String[] nameComponents = extras.getString("PROBE").split("\\.");
		
		String probeName = nameComponents[nameComponents.length - 1];
		
		for (String key : extras.keySet())
		{
			if ("PROBE".equals(key) || "GUID".equals(key) || "TIMESTAMP".equals(key))
			{
				// Do nothing...
			}
			else
			{
				String readingName = Slugify.slugify((probeName + " " + key).toLowerCase()).replaceAll("-", "_");
				
				this._milieu.put(readingName, extras.get(key));
			}
		}

		for (Model model : this.allModels(context))
		{
			HashMap<String, Object> snapshot = new HashMap<String, Object>();
			
			synchronized(this._milieu)
			{
				snapshot.putAll(this._milieu);
			}

			model.predict(context, snapshot);
		}
	}
}
