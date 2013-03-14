package edu.northwestern.cbits.purple_robot_manager.triggers;

import java.util.HashMap;
import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;

import android.content.Context;

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
		
		HashMap<String, Object> objects = new HashMap<String, Object>();
		
		objects.put("probeInfo", object);

		Object result = BaseScriptEngine.runScript(context, this._test, objects);

		ProbeTrigger._lastUpdate = now;

		if (result instanceof Boolean)
		{
			Boolean boolResult = (Boolean) result;

			return boolResult.booleanValue();
		}

		return false;
	}
	
	public Map<String, Object> configuration(Context context) 
	{
		Map<String, Object> config = super.configuration(context);
		
		config.put(ProbeTrigger.TRIGGER_TEST, this._test);
		config.put(ProbeTrigger.TRIGGER_FREQUENCY, this._frequency);
		
		return config;
	}

	public boolean updateFromMap(Context context, Map<String, Object> map) 
	{
		if (super.updateFromMap(context, map))
		{
			if (map.containsKey(ProbeTrigger.TRIGGER_TEST))
				this._test = map.get(ProbeTrigger.TRIGGER_TEST).toString();
			
			if (map.containsKey(ProbeTrigger.TRIGGER_FREQUENCY))
				this._frequency = ((Long) map.get(ProbeTrigger.TRIGGER_FREQUENCY)).longValue();

			return true;
		}
		
		return false;
	}
}
