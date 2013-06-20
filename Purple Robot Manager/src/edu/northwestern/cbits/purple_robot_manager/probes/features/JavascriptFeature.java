package edu.northwestern.cbits.purple_robot_manager.probes.features;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;

@SuppressLint("DefaultLocale")
public class JavascriptFeature extends Feature
{
	private String _name = null;
	private String _title = null;

	private String _script = null;
	private String _formatter = null;

	private boolean _embedded = false;
	private List<String> _sources = new ArrayList<String>();

	public JavascriptFeature()
	{
		throw new RuntimeException("Invalid constructor. Please use JavascriptFeature(scriptName) instead...");
	}

	public JavascriptFeature(String title, String name, String script, String formatter, List<String> sources, boolean embedded)
	{
		this._name = name;
		this._title = title;

		this._script = script;
		this._formatter = formatter;

		this._embedded = embedded;

		this._sources.addAll(sources);
	}

	public boolean embedded()
	{
		return this._embedded;
	}

	protected String featureKey()
	{
		return this._name.replaceAll(".", "_");
	}

	public String name(Context context)
	{
		return "javascript_" + this._name;
	}

	public String title(Context context)
	{
		return this._title;
	}

	public static String[] availableFeatures(Context context)
	{
		return context.getResources().getStringArray(R.array.js_feature_files);
	}

	public static String[] availableFeatureNames(Context context)
	{
		return context.getResources().getStringArray(R.array.js_feature_names);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_feature_" + this.featureKey() + "_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_feature_" + this.featureKey() + "_enabled", false);
		
		e.commit();
	}
	
	public boolean isEnabled(Context context)
	{
		boolean enabled = super.isEnabled(context);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		if (enabled && prefs.getBoolean("config_feature_" + this.featureKey() + "_enabled", true))
			return true;

		return false;
	}

	public static String scriptForFeature(Context context, String filename)
	{
	    String script = "";

	    try
		{
			AssetManager am = context.getAssets();

			InputStream jsStream = am.open("js/features/" + filename);

			// http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string

			Scanner s = new Scanner(jsStream).useDelimiter("\\A");

			if (s.hasNext())
		    	script = s.next();
		}
		catch (IOException e)
		{
			LogManager.getInstance(context).logException(e);
		}

	    return script;
	}

	@SuppressLint("DefaultLocale")
	public void processData(final Context context, final JSONObject json)
	{
		if (this.isEnabled(context) == false)
			return;

		boolean sourceMatches = false;

		try
		{
			String source = json.getString("PROBE");

			for (int i = 0; i < this._sources.size() && sourceMatches == false; i++)
			{
				String probeSource = this._sources.get(i);

				if (source != null && source.toLowerCase().equals(probeSource.toLowerCase()))
					sourceMatches = true;
			}

			if (sourceMatches == false)
				return;
		}
		catch (JSONException e)
		{

		}

		if (this._script == null)
			this._script = JavascriptFeature.scriptForFeature(context, this._name);

		final JavascriptFeature me = this;
		
		Runnable r = new Runnable()
		{
			public void run() 
			{
				Looper.prepare();
				
				String script = me._script;

				script = "var probe = " + json.toString() + "; " + script;

				JavaScriptEngine engine = new JavaScriptEngine(context);

				try
				{
					Object o = engine.runScript(script);
	
					Bundle bundle = new Bundle();
					bundle.putString("PROBE", me.name(context));
					bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
	
					if (o instanceof String)
						bundle.putString(Feature.FEATURE_VALUE, o.toString());
					else if (o instanceof Double)
					{
						Double d = (Double) o;
	
						bundle.putDouble(Feature.FEATURE_VALUE, d.doubleValue());
					}
					else if (o instanceof NativeObject)
					{
						NativeObject nativeObj = (NativeObject) o;
	
						Bundle b = JavascriptFeature.bundleForNativeObject(nativeObj);
	
						bundle.putParcelable(Feature.FEATURE_VALUE, b);
					}
					else
					{
						Log.e("PRM", "JS PLUGIN GOT UNKNOWN VALUE " + o);
	
						if (o != null)
							Log.e("PRM", "JS PLUGIN GOT UNKNOWN CLASS " + o.getClass());
					}
	
					me.transmitData(context, bundle);
				}
				catch (Exception e)
				{
					LogManager.getInstance(context).logException(e);
				}
			}
		};
		
		Thread t = new Thread(r);
		t.start();
	}

	public static Bundle bundleForNativeObject(NativeObject obj)
	{
		Bundle b = new Bundle();

		for (Object key : obj.keySet())
		{
			String keyString = key.toString();

			Object value = obj.get(key);

			if (value instanceof NativeObject)
				b.putParcelable(keyString, JavascriptFeature.bundleForNativeObject((NativeObject) value));
			else if (value instanceof NativeArray)
			{
				NativeArray array = (NativeArray) value;
				
				ArrayList<Bundle> items = new ArrayList<Bundle>();
				
				for (Object o : array.getIds()) 
				{
				    int index = (Integer) o;
				    
				    Object item = array.get(index);
				    
				    if (item instanceof NativeObject)
				    	items.add(JavascriptFeature.bundleForNativeObject((NativeObject) item));
				}
				
				b.putParcelableArrayList(keyString, items);
			}
			else if (value instanceof Double)
				b.putDouble(keyString, ((Double) value).doubleValue());
			else if (value instanceof Integer)
				b.putInt(keyString, ((Integer) value).intValue());
			else if (value == null)
				b.putString(keyString, "(null)");
			else
				b.putString(keyString, value.toString());
		}

		return b;
	}

	protected String summary(Context context)
	{
		return null;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		Object value = bundle.get(Feature.FEATURE_VALUE);

		if (this._formatter == null)
			return String.format(context.getString(R.string.summary_javascript_feature), this._name, value);

		String script = this._formatter;

		if (value instanceof Double || value instanceof Integer)
			script = "var result = " + value + "; " + script;
		else if (value instanceof Bundle)
		{
			try
			{
				script = "var result = " + OutputPlugin.jsonForBundle((Bundle) value).toString() + "; " + script;
			}
			catch (JSONException e)
			{
				LogManager.getInstance(context).logException(e);
			}
		}
		else
			script = "var result = '" + value.toString() + "'; " + script;

		JavaScriptEngine engine = new JavaScriptEngine(context);

		Object o = engine.runScript(script);

		return o.toString();
	}
}
