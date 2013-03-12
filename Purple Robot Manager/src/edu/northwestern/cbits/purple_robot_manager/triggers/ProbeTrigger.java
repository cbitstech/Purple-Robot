package edu.northwestern.cbits.purple_robot_manager.triggers;

import java.util.Map;

import org.mozilla.javascript.EcmaError;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;

public class ProbeTrigger extends Trigger
{
	public static final String TYPE_NAME = "probe";
	private static final String TRIGGER_TEST = "test";
	private static final String TRIGGER_FREQUENCY = "frequency";

	private static long _lastUpdate = 0;

	private String _test = null;
	private long _frequency = 0;

	public ProbeTrigger(Context context, Map<String, Object> map)
	{
		super(context, map);
		
		this.updateFromMap(context, map);
	}

	public boolean updateFromMap(Context context, Map<String, Object> map) 
	{
		if (super.updateFromMap(context, map))
		{
			if (super.updateFromMap(context, map))
			{
				if (map.containsKey(ProbeTrigger.TRIGGER_TEST))
					this._test = map.get(ProbeTrigger.TRIGGER_TEST).toString();
				
				if (map.containsKey(ProbeTrigger.TRIGGER_FREQUENCY))
					this._frequency = ((Long) map.get(ProbeTrigger.TRIGGER_FREQUENCY)).longValue();

				return true;
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
