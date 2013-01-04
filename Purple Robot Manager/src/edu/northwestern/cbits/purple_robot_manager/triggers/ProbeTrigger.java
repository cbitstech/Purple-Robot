package edu.northwestern.cbits.purple_robot_manager.triggers;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.EcmaError;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.JavaScriptEngine;

public class ProbeTrigger extends Trigger
{
	public static final String TYPE_NAME = "probe";
	private static final String TRIGGER_TEST = "test";
	private static final String TRIGGER_FREQUENCY = "frequency";

	private static long _lastUpdate = 0;

	private String _test = null;
	private long _frequency;

	public ProbeTrigger(Context context, JSONObject object) throws JSONException
	{
		super(context, object);

		this._test = object.getString(ProbeTrigger.TRIGGER_TEST);
		this._frequency = object.getLong(ProbeTrigger.TRIGGER_FREQUENCY);
	}

	public boolean updateFromJson(Context context, JSONObject json) 
	{
		if (super.updateFromJson(context, json))
		{
			try
			{
				if (json.has(ProbeTrigger.TRIGGER_TEST))
					this._test = json.getString(ProbeTrigger.TRIGGER_TEST);
				
				if (json.has(ProbeTrigger.TRIGGER_FREQUENCY))
					this._frequency = json.getLong(ProbeTrigger.TRIGGER_FREQUENCY);

				return true;
			} 
			catch (JSONException e) 
			{
				e.printStackTrace();
			}
		}
		
		return false;
	}

	public void merge(Trigger trigger) 
	{
		if (trigger instanceof ProbeTrigger)
		{
			super.merge(trigger);
			
			ProbeTrigger probeTrigger = (ProbeTrigger) trigger;
		
			this._test = probeTrigger._test;
			this._frequency = probeTrigger._frequency;
		}
	}
	
	public boolean matches(Context context, Object object)
	{
		if (this._test == null)
			return false;

		long now = System.currentTimeMillis();

		if (now - this._frequency < ProbeTrigger._lastUpdate)
			return false;

		try
		{
			JavaScriptEngine js = new JavaScriptEngine(context);

			Object result = js.runScript(this._test, "probeInfo", object);

			ProbeTrigger._lastUpdate = now;

			if (result instanceof Boolean)
			{
				Boolean boolResult = (Boolean) result;

				return boolResult.booleanValue();
			}
		}
		catch (EcmaError e)
		{

		}

		return false;
	}
}
