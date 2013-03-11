package edu.northwestern.cbits.purple_robot_manager.scripting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import jscheme.JScheme;
import jsint.DynamicEnvironment;
import jsint.Evaluator;
import jsint.Pair;
import jsint.Symbol;
import android.content.Context;
import android.util.Log;

public class SchemeEngine extends BaseScriptEngine
{
	public SchemeEngine(Context context) 
	{
		super(context);
	}
	
	public Object evaluateSource(String source)
	{
		DynamicEnvironment env = new DynamicEnvironment();
		env.setValue(Symbol.intern("PurpleRobot"), this);
		Evaluator eval = new Evaluator(env);
		
		JScheme scheme = new JScheme(eval);
		
		return scheme.eval(source);
	}

	protected String language() 
	{
		return "Scheme";
	}
	
	public boolean updateTrigger(String triggerId, Pair parameters)
	{
		Map<String, String> paramsMap = SchemeEngine.parsePair(parameters);
		paramsMap.put("identifier", triggerId);

		Iterator<Entry<String, String>> it = paramsMap.entrySet().iterator();

		while (it.hasNext())
		{
			Entry<String, String> entry = it.next();
			
			Log.e("PR-SCM", "MAP: " + entry.getKey() + " => " + entry.getValue());
		}
		
		return false;
	}

	private static Map<String, String> parsePair(Pair pair) 
	{
		HashMap<String, String> map = new HashMap<String, String>();
		
		if (pair.isEmpty() == false)
		{
			Object car = pair.first();
			
			if (car instanceof Pair)
			{
				Pair carPair = (Pair) car;
				
				Object caar = carPair.first();
				
				if (caar instanceof String)
				{
					String key = (String) caar;
					
					Object cdar = carPair.second();
					
					if (cdar instanceof String)
					{
						String value = (String) cdar;
						
						map.put(key, value);
					}
					else if (cdar instanceof Pair)
					{
						Pair cadar = (Pair) cdar;
						
						if (cadar.first().toString().equalsIgnoreCase("begin"))
							map.put(key, cadar.toString());
					}
				}
			}
			
			Object cdr = pair.rest();
	
			if (cdr instanceof Pair && !((Pair) cdr).isEmpty())
			{
				Pair cdrPair = (Pair) cdr;
				
				Map<String, String> restMap = SchemeEngine.parsePair(cdrPair);
				
				if (restMap != null)
					map.putAll(restMap);
			}
		}
		
		return map;
	}
}
