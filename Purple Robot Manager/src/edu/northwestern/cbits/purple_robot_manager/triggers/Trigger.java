package edu.northwestern.cbits.purple_robot_manager.triggers;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.EvaluatorException;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.JavaScriptEngine;

public abstract class Trigger
{
	private String _name = null;
	private String _action = null;
	private String _identifier = null;
	
	private boolean _enabled = true;

	public Trigger (Context context, JSONObject object) throws JSONException
	{
		this._name = object.getString("name");
		this._action = object.getString("action");
		
		if (object.has("identifier"))
			this._identifier = object.getString("identifier");
		else
			this._identifier = this._name;
	}

	public static Trigger parse(Context context, JSONObject object)
	{
		try
		{
			String type = object.getString("type");

			if (DateTrigger.TYPE_NAME.equals(type))
				return new DateTrigger(context, object);
			else if (ProbeTrigger.TYPE_NAME.equals(type))
				return new ProbeTrigger(context, object);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		return null;
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
					try
					{
						JavaScriptEngine engine = new JavaScriptEngine(context);

						engine.runScript(me._action);
					}
					catch (EvaluatorException e)
					{
						e.printStackTrace();
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
		if (obj instanceof Trigger)
		{
			Trigger t = (Trigger) obj;
			
			if (t._identifier.equals(this._identifier))
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

	public boolean updateFromJson(Context context, JSONObject json) 
	{
		try 
		{
			if (json.has("name"))
				this._name = json.getString("name");

			if (json.has("action"))
				this._action = json.getString("action");
			
			if (json.has("identifier"))
				this._identifier = json.getString("identifier");

			return true;
		} 
		catch (JSONException e) 
		{
			e.printStackTrace();
		}
		
		return false;
	}

	public void reset(Context context) 
	{
		// Default implementation does nothing...
	}
}
