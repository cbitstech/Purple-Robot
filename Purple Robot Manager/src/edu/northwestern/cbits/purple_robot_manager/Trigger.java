package edu.northwestern.cbits.purple_robot_manager;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public abstract class Trigger
{
	private String _name = null;
	private String _action = null;

	public Trigger (JSONObject object) throws JSONException
	{
		this._name = object.getString("name");
		this._action = object.getString("action");
	}

	public static Trigger parse(JSONObject object)
	{
		try
		{
			String type = object.getString("type");

			if (DateTrigger.TYPE_NAME.equals(type))
				return new DateTrigger(object);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		return null;
	}

	public abstract boolean matches(Context context, Object obj);

	public void execute(Context context)
	{
		if (this._action != null)
		{
			JavaScriptEngine engine = new JavaScriptEngine(context);

			engine.runScript(this._action);
		}
	}

	public String name()
	{
		return this._name;
	}
}
