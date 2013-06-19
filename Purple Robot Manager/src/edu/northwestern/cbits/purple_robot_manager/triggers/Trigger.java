package edu.northwestern.cbits.purple_robot_manager.triggers;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.CodeViewerActivity;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;

public abstract class Trigger
{
	private String _name = null;
	private String _action = null;
	private String _identifier = "unidentified-trigger";
	
	public Trigger (Context context, Map<String, Object> map)
	{
		this.updateFromMap(context, map);
	}

	public abstract boolean matches(Context context, Object obj);
	public abstract void refresh(Context context);
	
	public boolean enabled(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		return prefs.getBoolean(this.enabledKey(), true);
	}
	
	public void setEnabled(Context context, boolean enabled)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor e = prefs.edit();
		e.putBoolean(this.enabledKey(), enabled);
		e.commit();
	}

	public void execute(final Context context, boolean force)
	{
		if (this.enabled(context) && this._action != null)
		{
			final Trigger me = this;

			Runnable r = new Runnable()
			{
				public void run()
				{
					try
					{
						BaseScriptEngine.runScript(context, me._action);
					}
					catch (Exception e)
					{
						LogManager.getInstance(context).logException(e);
					}
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
			
			if ((t._identifier == null || this._identifier == null) && (t._name != null && t._name.equals(this._name)))
				return true;
			else if (t._identifier != null && t._identifier.equals(this._identifier))
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

	public PreferenceScreen preferenceScreen(final PreferenceActivity activity) 
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

		final Trigger me = this;

		Preference viewAction = new Preference(activity);
		viewAction.setTitle(R.string.label_trigger_show_action);
		viewAction.setSummary(R.string.label_trigger_show_action_desc);
		viewAction.setOrder(Integer.MAX_VALUE);
		
		viewAction.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			public boolean onPreferenceClick(Preference preference) 
			{
				Intent intent = new Intent(activity, CodeViewerActivity.class);
				intent.putExtra(CodeViewerActivity.SOURCE_CODE, me._action);
				intent.putExtra(CodeViewerActivity.TITLE, me.name());
				
				activity.startActivity(intent);

				return true;
			}
		});
		
		screen.addPreference(viewAction);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.label_trigger_enable_action);
		enabled.setSummary(R.string.label_trigger_enable_action_desc);
		enabled.setKey(this.enabledKey());
		enabled.setDefaultValue(true);
		
		enabled.setOrder(Integer.MAX_VALUE);
		
		enabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
		{
			public boolean onPreferenceChange(Preference preference, Object newValue) 
			{
				Log.e("FC", "NEW TRIGGER VALUE: " + newValue + " (" + preference.getKey() + ")");
				
				return true;
			}
		});

		screen.addPreference(enabled);

		Preference fireNow = new Preference(activity);
		fireNow.setTitle(R.string.label_trigger_fire_now);
		fireNow.setSummary(R.string.label_trigger_fire_now_desc);
		fireNow.setOrder(Integer.MAX_VALUE / 2);
		
		fireNow.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			public boolean onPreferenceClick(Preference preference) 
			{
				me.execute(activity, true);

				return true;
			}
		});
		
		screen.addPreference(fireNow);

		return screen;
	}

	private String enabledKey() 
	{
		return "trigger_enabled_" + this._identifier;
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
		
		config.put("name", this._name);
		
		if (this._identifier != null)
			config.put("identifier", this._identifier);
		else
			config.put("identifier", "unspecified-identifier");

		if (this._action != null)
			config.put("action", this._action);
		else
			config.put("action", "");

		if (this._name != null)
			config.put("name", this._name);
		else
			config.put("name", context.getString(R.string.name_anonymous_trigger));

		return config;
	}

	public boolean updateFromMap(Context context, Map<String, Object> params) 
	{
		if (params.containsKey("name"))
			this._name = params.get("name").toString();

		if (params.containsKey("action"))
			this._action = params.get("action").toString();

		if (params.containsKey("identifier"))
		{
			try
			{
				this._identifier = params.get("identifier").toString();
			}
			catch (NullPointerException e)
			{
				this._identifier = "unspecified-identifier";
			}
		}
		
		TriggerManager.getInstance(context).persistTriggers(context);

		return true;
	}
}
