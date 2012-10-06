package edu.northwestern.cbits.purple_robot_manager.triggers;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.EvaluatorException;

import edu.northwestern.cbits.purple_robot_manager.JavaScriptEngine;

import android.content.Context;

public abstract class Trigger
{
	private String _name = null;
	private String _action = null;

	public Trigger (Context context, JSONObject object) throws JSONException
	{
		this._name = object.getString("name");
		this._action = object.getString("action");
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

	public void execute(Context context)
	{
		if (this._action != null)
		{
			try
			{
				JavaScriptEngine engine = new JavaScriptEngine(context);

				engine.runScript(this._action);
			}
			catch (EvaluatorException e)
			{
				e.printStackTrace();
			}
		}
	}

	public String name()
	{
		return this._name;
	}
}
