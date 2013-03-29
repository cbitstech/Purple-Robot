package edu.northwestern.cbits.purple_robot_manager.scripting;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.config.JSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.probes.features.Feature;
import edu.northwestern.cbits.purple_robot_manager.probes.features.JavascriptFeature;

public class JavaScriptEngine extends BaseScriptEngine
{
	private Context _jsContext = null;
	private Scriptable _scope = null;

	public JavaScriptEngine(android.content.Context context)
	{
		super(context);
	}

	public Object runScript(String script) throws EvaluatorException, EcmaError
	{
		return this.runScript(script, null, null);
	}

	public Object runScript(String script, String extrasName, Object extras) throws EvaluatorException, EcmaError
	{
		this._jsContext = Context.enter();
		this._jsContext.setOptimizationLevel(-1);

		this._scope = _jsContext.initStandardObjects();

		/* if (extras instanceof JSONObject)
		{
			JSONObject json = (JSONObject) extras;

			extras = new JsonParser().parse(json.toString());
		} */

		Object thisWrapper = Context.javaToJS(this, this._scope);
		ScriptableObject.putProperty(this._scope, "PurpleRobot", thisWrapper);

		if (extras != null && extrasName != null)
			script = "var " + extrasName + " = " + extras.toString() + "; " + script;

		try
		{
			return this._jsContext.evaluateString(this._scope, script, "<engine>", 1, null);
		}
		catch (EvaluatorException e)
		{
			e.printStackTrace();

			try
			{
				Toast.makeText(this._context, e.getMessage(), Toast.LENGTH_LONG).show();
			}
			catch (RuntimeException ee)
			{
				ee.printStackTrace();
			}
			
			throw e;
		}
		catch (EcmaError e)
		{
			e.printStackTrace();

			try
			{
				Toast.makeText(this._context, e.getMessage(), Toast.LENGTH_LONG).show();
			}
			catch (RuntimeException ee)
			{
				ee.printStackTrace();
			}
			
			throw e;
		}
	}

	public boolean loadLibrary(String libraryName)
	{
		if (this._jsContext != null && this._scope != null)
		{
			try
			{
				if (libraryName.endsWith(".js") == false)
					libraryName += ".js";

				AssetManager am = this._context.getAssets();

				InputStream jsStream = am.open("js/" + libraryName);

				// http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
			    Scanner s = new Scanner(jsStream).useDelimiter("\\A");

			    String script = "";

			    if (s.hasNext())
			    	script = s.next();
			    else
			    	return false;

				this._jsContext.evaluateString(this._scope, script, "<engine>", 1, null);

				return true;
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		return false;
	}

	public void updateWidget(final NativeObject parameters)
	{
		this.updateWidget(JavaScriptEngine.nativeToMap(parameters));
	}

	public boolean broadcastIntent(final String action, final NativeObject extras)
	{
		return this.broadcastIntent(action, JavaScriptEngine.nativeToMap(extras));
	}
	
	public boolean updateWidget(final String title, final String message, final String applicationName, final NativeObject launchParams, final String script)
	{
		return this.updateWidget(title, message, applicationName, JavaScriptEngine.nativeToMap(launchParams), script);
	}

	public boolean launchApplication(String applicationName, final NativeObject launchParams, final String script)
	{
		return this.launchApplication(applicationName, JavaScriptEngine.nativeToMap(launchParams), script);
	}

	public boolean showApplicationLaunchNotification(String title, String message, String applicationName, long displayWhen, final NativeObject launchParams, final String script)
	{
		return this.showApplicationLaunchNotification(title, message, applicationName, displayWhen, JavaScriptEngine.nativeToMap(launchParams), script);
	}
	
	public boolean updateTrigger(String triggerId, NativeObject nativeJson)
	{
		return this.updateTrigger(triggerId, JavaScriptEngine.nativeToMap(nativeJson));
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
		else if (value instanceof NativeObject)
		{
			NativeObject nativeObj = (NativeObject) value;

			Bundle b = JavascriptFeature.bundleForNativeObject(nativeObj);

			bundle.putParcelable(Feature.FEATURE_VALUE, b);
		}
		else
		{
			Log.e("PRM", "JS PLUGIN GOT UNKNOWN VALUE " + value);

			if (value != null)
				Log.e("PRM", "JS PLUGIN GOT UNKNOWN CLASS " + value.getClass());
		}

		this.transmitData(bundle);
	}

	private static JSONArray nativeToJson(NativeArray nativeArray) throws JSONException
	{
		JSONArray array = new JSONArray();
		
		for (int i = 0; i < nativeArray.getLength(); i++)
		{
			Object value = nativeArray.get(i);
			
			if (value instanceof String)
				array.put(value);
			else if (value instanceof Double)
				array.put(value);
			else if (value instanceof NativeObject)
			{
				NativeObject obj = (NativeObject) value;
				
				array.put(JavaScriptEngine.nativeToJson(obj));
			}
			else if (value instanceof NativeArray)
			{
				NativeArray arr = (NativeArray) value;
				
				array.put(JavaScriptEngine.nativeToJson(arr));
			}
		}

		return array;
	}
	
	private static JSONObject nativeToJson(NativeObject nativeObj) throws JSONException 
	{
		if (nativeObj == null)
			return null;
		
		JSONObject json = new JSONObject();
		
		for (Entry<Object, Object> e : nativeObj.entrySet())
		{
			String key = e.getKey().toString();
			Object value = e.getValue();
			
			if (value instanceof String)
				json.put(key, value);
			else if (value instanceof Double)
				json.put(key, value);
			else if (value instanceof NativeObject)
			{
				NativeObject obj = (NativeObject) value;
				
				json.put(key, JavaScriptEngine.nativeToJson(obj));
			}
			else if (value instanceof NativeArray)
			{
				NativeArray arr = (NativeArray) value;
				
				json.put(key, JavaScriptEngine.nativeToJson(arr));
			}
		}
					
		return json;
	}
	
	protected String language() 
	{
		return "JavaScript";
	}

	public static boolean canRun(String script) 
	{
		// TODO: Validate if actually JavaScript...

		return true;
	}

	private static Map<String, Object> nativeToMap(NativeObject nativeObj)
	{
		HashMap<String, Object> params = new HashMap<String, Object>();
		
		for (Entry<Object, Object> e : nativeObj.entrySet())
		{
			Object value = e.getValue();
			
			if (value instanceof NativeObject)
				value = JavaScriptEngine.nativeToMap((NativeObject) value);
			
			params.put(e.getKey().toString(), value);
		}
		
		return params;
	}
	
	public String fetchConfig()
	{
		JSONConfigFile config = new JSONConfigFile(this._context);
		
		return config.toString();
	}
	
	public boolean updateConfig(NativeObject nativeObj)
	{
		Map<String, Object> paramsMap = JavaScriptEngine.nativeToMap(nativeObj);
		
		return super.updateConfig(paramsMap);
	}


}
