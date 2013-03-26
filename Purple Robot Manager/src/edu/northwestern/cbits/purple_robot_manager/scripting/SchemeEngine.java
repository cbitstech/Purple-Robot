package edu.northwestern.cbits.purple_robot_manager.scripting;

import java.util.HashMap;
import java.util.Map;

import jscheme.JScheme;
import jsint.DynamicEnvironment;
import jsint.Evaluator;
import jsint.InputPort;
import jsint.Pair;
import jsint.Scheme;
import jsint.Symbol;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.config.SchemeConfigFile;
import edu.northwestern.cbits.purple_robot_manager.probes.features.Feature;

public class SchemeEngine extends BaseScriptEngine
{
	public SchemeEngine(Context context, Map<String, Object> objects) 
	{
		super(context);
	}

	public static boolean canRun(String script) 
	{
		// TODO: Better validation & heuristics...

		if (script == null || script.trim().length() < 1)
			return false;
		else if (script.trim().charAt(0) == '(')
			return true;
		
		return false;
	}

	public Object evaluateSource(String source)
	{
		Evaluator eval = new Evaluator();

		DynamicEnvironment env = new DynamicEnvironment(eval.getInitialEnvironment());
		env.setValue(Symbol.intern("PurpleRobot"), this);
		
		JScheme scheme = new JScheme(new Evaluator(env));
		
		return scheme.eval(source);
	}

	protected String language() 
	{
		return "Scheme";
	}
	
	public boolean updateTrigger(String triggerId, Pair parameters)
	{
		Map<String, Object> paramsMap = SchemeEngine.parsePairList(parameters);
		paramsMap.put("identifier", triggerId);
		
		return this.updateTrigger(triggerId, paramsMap);
	}

	public boolean updateTrigger(Pair parameters)
	{
		Map<String, Object> paramsMap = SchemeEngine.parsePairList(parameters);

		if (paramsMap.containsKey("identifier"))
		{
			String triggerId = paramsMap.get("identifier").toString();
			
			return this.updateTrigger(triggerId, paramsMap);
		}
		
		return false;
	}

	public void scheduleScript(String identifier, String dateString, Pair action)
	{
		super.scheduleScript(identifier, dateString, action.toString());
	}
	
	public boolean updateProbe(Pair params)
	{
		Map<String, Object> map = SchemeEngine.parsePairList(params);

		return this.updateProbe(map);
	}

	private static Map<String, Object> parsePairList(Pair pair) 
	{
		HashMap<String, Object> map = new HashMap<String, Object>();
		
		if (pair.isEmpty() == false)
		{
			Object first = pair.getFirst();

			if (first instanceof Pair)
			{
				Pair firstPair = (Pair) first;
				
				String key = firstPair.first.toString();

				Object value = firstPair.rest();

				if (value instanceof Pair)
				{
					Pair valuePair = (Pair) value;
					
					value = valuePair.toString();
				}

				map.put(key, value);
			}
			
			Object rest = pair.getRest();

			if (rest instanceof Pair)
			{
				Pair restPair = (Pair) rest;
				
				Map<String, Object> restMap = SchemeEngine.parsePairList(restPair);
				
				map.putAll(restMap);
			}
		}
		
		return map;
	}

	private static Bundle bundleForPair(Pair pair) 
	{
		Bundle b = new Bundle();
		
		if (pair.isEmpty() == false)
		{
			Object first = pair.first();
			
			if (first instanceof Pair)
			{
				Pair firstPair = (Pair) first;
				
				Object firstFirst = firstPair.first();
				
				if (firstFirst instanceof String)
				{
					String key = (String) firstFirst;
					
					Object second = firstPair.second();
					
					if (second instanceof String)
						b.putString(key, second.toString());
					else if (second instanceof Pair)
					{
						Pair secondPair = (Pair) second;
						
						if (secondPair.first().toString().equalsIgnoreCase("begin"))
							b.putString(key, secondPair.toString());
						else
							b.putBundle(key, SchemeEngine.bundleForPair(secondPair));
					}
				}
			}
			
			Object rest = pair.rest();
	
			if (rest instanceof Pair && !((Pair) rest).isEmpty())
			{
				Pair restPair = (Pair) rest;
				
				Bundle restBundle = SchemeEngine.bundleForPair(restPair);
				
				b.putAll(restBundle);
				
			}
		}
		
		return b;
	}

	public void showNativeDialog(String title, String message, String confirmTitle, String cancelTitle, Pair confirmAction, Pair cancelAction)
	{
		this.showNativeDialog(title, message, confirmTitle, cancelTitle, confirmAction.toString(), cancelAction.toString());
	}

	public boolean updateConfig(Pair parameters)
	{
		Map<String, Object> paramsMap = SchemeEngine.parsePairList(parameters);
		
		return super.updateConfigUrl(paramsMap);
	}

	public void updateWidget(Pair parameters)
	{
		Map<String, Object> paramsMap = SchemeEngine.parsePairList(parameters);
		
		Intent intent = new Intent(ManagerService.UPDATE_WIDGETS);
		
		for (String key : paramsMap.keySet())
		{
			intent.putExtra(key, paramsMap.get(key).toString());
		}

		this._context.startService(intent);
	}
	
	public void emitReading(String name, Object value)
	{
		Bundle bundle = new Bundle();
		bundle.putString("PROBE", name);
		bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
		
		if (value instanceof String)
			bundle.putString(Feature.FEATURE_VALUE, value.toString());
		else if (value instanceof Double)
		{
			Double d = (Double) value;

			bundle.putDouble(Feature.FEATURE_VALUE, d.doubleValue());
		}
		else if (value instanceof Pair)
		{
			Pair pair = (Pair) value;

			Bundle b = SchemeEngine.bundleForPair(pair);

			bundle.putParcelable(Feature.FEATURE_VALUE, b);
		}
		else
		{
			Log.e("PRM", "SCHEME PLUGIN GOT UNKNOWN VALUE " + value);

			if (value != null)
				Log.e("PRM", "SCHEME PLUGIN GOT UNKNOWN CLASS " + value.getClass());
		}

		this.transmitData(bundle);
	}

	public boolean broadcastIntent(final String action, Pair extras)
	{
		return this.broadcastIntent(action, SchemeEngine.parsePairList(extras));
	}

	public boolean updateWidget(final String title, final String message, final String applicationName, final Pair launchParams, final String script)
	{
		return this.updateWidget(title, message, applicationName, SchemeEngine.parsePairList(launchParams), script);
	}

	public boolean launchApplication(String applicationName, final Pair launchParams, final String script)
	{
		return this.launchApplication(applicationName, SchemeEngine.parsePairList(launchParams), script);
	}

	public boolean showApplicationLaunchNotification(String title, String message, String applicationName, long displayWhen, final Pair launchParams, final String script)
	{
		return this.showApplicationLaunchNotification(title, message, applicationName, displayWhen, SchemeEngine.parsePairList(launchParams), script);
	}
	
	public String fetchConfig()
	{
		SchemeConfigFile config = new SchemeConfigFile(this._context);
		
		return config.toString();
	}
}
