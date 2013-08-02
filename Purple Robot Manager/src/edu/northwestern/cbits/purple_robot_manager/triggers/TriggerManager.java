package edu.northwestern.cbits.purple_robot_manager.triggers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.SettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.config.SchemeConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;

public class TriggerManager 
{
	private static TriggerManager _instance = null;
    
	private List<Trigger> _triggers = new ArrayList<Trigger>();
	private Timer _timer = null;
	
	private boolean _triggersInited = false;
	
    private TriggerManager(Context context) 
    {
        if (TriggerManager._instance != null)
            throw new IllegalStateException("Already instantiated");
    }

	public static TriggerManager getInstance(Context context) 
    {
    	if (TriggerManager._instance == null)
    	{
    		TriggerManager._instance = new TriggerManager(context.getApplicationContext());
    		TriggerManager._instance.restoreTriggers(context);
    	}
    	
    	return TriggerManager._instance;
    }

	public void nudgeTriggers(Context context)
	{
		Date now = new Date();
		
		synchronized(this._triggers)
		{
			for (Trigger trigger : this._triggers)
			{
				boolean execute = false;
	
				if (trigger instanceof DateTrigger)
				{
					if (trigger.matches(context, now))
						execute = true;
				}
				
				if (execute)
					trigger.execute(context, false);
			}
		}
	}

	public void updateTriggers(Context context, List<Trigger> triggerList)
	{
		ArrayList<Trigger> toAdd = new ArrayList<Trigger>();
		
		synchronized(this._triggers)
		{
			for (Trigger newTrigger : triggerList)
			{
				boolean found = false;
				
				for (Trigger trigger : this._triggers)
				{
					if (trigger.equals(newTrigger))
					{
						trigger.merge(newTrigger);
						
						found = true;
					}
				}
				
				if (!found)
					toAdd.add(newTrigger);
			}
		
			this._triggers.addAll(toAdd);
		}
		 
		this.persistTriggers(context);
	}

    private void restoreTriggers(Context context) 
    {
    	if (this._triggersInited)
    		return;
    	
    	this._triggersInited = true;
    				
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		if (prefs.contains("triggers_scheme_script"))
		{
			String script = prefs.getString("triggers_scheme_script", "(begin)");
			
			try
			{
				BaseScriptEngine.runScript(context, script);
			}
			catch (Exception e)
			{
				LogManager.getInstance(context).logException(e);
			}
		}
    }

	protected void persistTriggers(final Context context) 
	{
		if (this._timer == null)
		{
			this._timer = new Timer();
			
			final TriggerManager me = this;
			
			this._timer.schedule(new TimerTask()
			{
				public void run() 
				{
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
					Editor e = prefs.edit();
					
					SchemeConfigFile config = new SchemeConfigFile(context);
					String script = config.triggersScript(context);

					e.putString("triggers_scheme_script", script);
					
					e.commit();

					me._timer = null;
				}
				
			}, 5000);
		}
	}

	public List<Trigger> triggersForId(String triggerId) 
	{
		ArrayList<Trigger> matches = new ArrayList<Trigger>();
		
		synchronized(this._triggers)
		{
			for (Trigger trigger : this._triggers)
			{
				if (trigger.identifier() != null && trigger.identifier().equals(triggerId))
					matches.add(trigger);
			}
		}		
		return matches;
	}

	public List<Trigger> allTriggers() 
	{
		return this._triggers;
	}

	public void addTrigger(Context context, Trigger t) 
	{
		ArrayList<Trigger> ts = new ArrayList<Trigger>();
		
		ts.add(t);
		
		this.updateTriggers(context, ts);
	}

	public void removeAllTriggers() 
	{
		synchronized(this._triggers)
		{
			this._triggers.clear();
		}
	}
	
	@SuppressWarnings("deprecation")
	public PreferenceScreen buildPreferenceScreen(PreferenceActivity settingsActivity)
	{
		PreferenceManager manager = settingsActivity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(settingsActivity);
		screen.setOrder(0);
		screen.setTitle(R.string.title_preference_triggers_screen);
		screen.setKey(SettingsActivity.TRIGGERS_SCREEN_KEY);

		PreferenceCategory triggersCategory = new PreferenceCategory(settingsActivity);
		triggersCategory.setTitle(R.string.title_preference_triggers_category);
		triggersCategory.setKey("key_available_triggers");

		screen.addPreference(triggersCategory);

		synchronized(this._triggers)
		{
			for (Trigger trigger : this._triggers)
			{
				PreferenceScreen triggerScreen = trigger.preferenceScreen(settingsActivity);
	
				if (triggerScreen != null)
					screen.addPreference(triggerScreen);
			}
		}
		
		return screen;
	}

	public List<Map<String, Object>> triggerConfigurations(Context context)
	{
		List<Map<String, Object>> configs = new ArrayList<Map<String, Object>>();

		synchronized(this._triggers)
		{
			for (Trigger t : this._triggers)
			{
				Map<String, Object> config = t.configuration(context);
				
				configs.add(config);
			}
		}
		
		return configs;
	}

	public void refreshTriggers(Context context) 
	{
		synchronized(this._triggers)
		{
			for (Trigger t : this._triggers)
				t.refresh(context);
		}
	}

	public List<String> triggerIds() 
	{
		ArrayList<String> triggerIds = new ArrayList<String>();
		
		synchronized(this._triggers)
		{
			for (Trigger t : this._triggers)
			{
				String id = t.identifier();
				
				if (id != null && triggerIds.contains(id) == false)
					triggerIds.add(id);
			}
		}
		
		return triggerIds;
	}

	public Map<String, Object> fetchTrigger(Context context, String id) 
	{
		List<Trigger> triggers = this.triggersForId(id); 
		
		if (triggers.size() > 0)
			return triggers.get(0).configuration(context);

		return null;
	}

	public boolean deleteTrigger(String id) 
	{
		List<Trigger> triggers = this.triggersForId(id);
		
		synchronized(this._triggers)
		{
			this._triggers.removeAll(triggers);
		}
		
		return triggers.size() > 0;
	}
}
