package edu.northwestern.cbits.purple_robot_manager.triggers;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;

public abstract class Trigger
{
	private String _name = null;
	private String _action = null;
	private String _identifier = null;
	
	private boolean _enabled = true;

	public Trigger (Context context, Map<String, Object> map)
	{
		this.updateFromMap(context, map);
	}

	public abstract boolean matches(Context context, Object obj);
	
	public boolean enabled()
	{
		return this._enabled;
	}
	
	public void setEnabled(boolean enabled)
	{
		this._enabled = enabled;
	}

	public void execute(final Context context)
	{
		if (this._enabled && this._action != null)
		{
			final Trigger me = this;

			Runnable r = new Runnable()
			{
				public void run()
				{
					BaseScriptEngine.runScript(context, me._action);
				}
			};

			Thread t = new Thread(new ThreadGroup("Triggers"), r, this.name(), 32768);
			t.start();
		}
	}

	public String name()
	{
		return this._name;
	}
	
	public boolean equals(Object obj)
	{
		if (obj != null && obj instanceof Trigger)
		{
			Trigger t = (Trigger) obj;
			
			if (t._identifier != null && t._identifier.equals(this._identifier))
				return true;
		}
		
		return false;
	}

	public void merge(Trigger trigger) 
	{
		this._name = trigger._name;
		this._action = trigger._action;
	}

	public String identifier() 
	{
		return this._identifier;
	}

	public void reset(Context context) 
	{
		// Default implementation does nothing...
	}

	public PreferenceScreen preferenceScreen(PreferenceActivity activity) 
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this._name);

		String type = activity.getString(R.string.type_trigger_unknown);
		
		if (this instanceof ProbeTrigger)
			type = activity.getString(R.string.type_trigger_probe);
		if (this instanceof DateTrigger)
			type = activity.getString(R.string.type_trigger_datetime);
		
		screen.setSummary(type);

		return screen;
	}

	public static Trigger parse(Context context, Map<String, Object> params) 
	{
		String type = params.get("type").toString();

		if (DateTrigger.TYPE_NAME.equals(type))
			return new DateTrigger(context, params);
		else if (ProbeTrigger.TYPE_NAME.equals(type))
			return new ProbeTrigger(context, params);

		return null;
	}

	public Map<String, Object> configuration(Context context) 
	{
		Map<String, Object> config = new HashMap<String,Object>();
		
		config.put("action", this._action);
		config.put("identifier", this._identifier);
		config.put("name", this._name);
		
		return config;
	}

	public boolean updateFromMap(Context _context, Map<String, Object> params) 
	{
		if (params.containsKey("name"))
			this._name = params.get("name").toString();

		if (params.containsKey("action"))
			this._action = params.get("action").toString();

		if (params.containsKey("identifier"))
			this._identifier = params.get("identifier").toString();

		return true;
	}
}
