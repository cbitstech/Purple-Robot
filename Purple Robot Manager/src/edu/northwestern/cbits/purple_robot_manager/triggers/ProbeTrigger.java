package edu.northwestern.cbits.purple_robot_manager.triggers;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.EcmaError;

import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;

import android.content.Context;
import android.util.Log;

public class ProbeTrigger extends Trigger
{
	public static final String TYPE_NAME = "probe";
	private static final String TRIGGER_TEST = "test";
	private static final String TRIGGER_PROBE = "probe";

	private String _probe = null;
	private String _test = null;
	
	private long _lastUpdate = 0;

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
		}
	}
	
	public boolean matches(Context context, Object object)
	{
		long now = System.currentTimeMillis();
		
		if (this._test == null || now - this._lastUpdate < 5000)
			return false;

		this._lastUpdate = now;
		
		HashMap<String, Object> objects = new HashMap<String, Object>();
		
		if (object instanceof JSONObject)
		{
			JSONObject json = (JSONObject) object;
			
			JSONArray names = json.names();
			
			for (int i = 0; i < names.length(); i++)
			{
				try 
				{
					String name = names.getString(i);
					objects.put(name, json.get(name));
				}
				catch (JSONException e) 
				{
					LogManager.getInstance(context).logException(e);
				}
			}
		}
		
		try
		{
			Object result = BaseScriptEngine.runScript(context, this._test, objects);
	
			if (result instanceof Boolean)
			{
				Boolean boolResult = (Boolean) result;
	
				return boolResult.booleanValue();
			}
		}
		catch (Throwable e)
		{
			LogManager.getInstance(context).logException(e);
		}
		
		return false;
	}
	
	public Map<String, Object> configuration(Context context) 
	{
		Map<String, Object> config = super.configuration(context);
		
		config.put(ProbeTrigger.TRIGGER_TEST, this._test);
		config.put(ProbeTrigger.TRIGGER_PROBE, this._probe);
		config.put("type", ProbeTrigger.TYPE_NAME);
		
		return config;
	}

	public boolean updateFromMap(Context context, Map<String, Object> map) 
	{
		if (super.updateFromMap(context, map))
		{
			if (map.containsKey(ProbeTrigger.TRIGGER_TEST))
				this._test = map.get(ProbeTrigger.TRIGGER_TEST).toString();
			
			if (map.containsKey(ProbeTrigger.TRIGGER_PROBE))
				this._probe = map.get(ProbeTrigger.TRIGGER_PROBE).toString();

			return true;
		}
		
		return false;
	}

	public void refresh(Context context) 
	{
		// Nothing to do for this trigger type...
	}

	public boolean matchesProbe(String probeName) 
	{
		if (this._probe != null && this._probe .equals(probeName))
			return true;
		
		return false;
	}
}
