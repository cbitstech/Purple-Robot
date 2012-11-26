package edu.northwestern.cbits.purple_robot_manager.probes.features;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.json.JSONObject;
import org.mozilla.javascript.Scriptable;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;

public class JavascriptFeature extends Feature
{
	private String _name = null;
	private String _script = null;
	private String _title = null;

	private long _lastRun = 0;

	public JavascriptFeature()
	{
		throw new RuntimeException("Invalid constructor. Please use JavascriptFeature(scriptName) instead...");
	}

	public JavascriptFeature(String title, String name, String script)
	{
		this._name = name;
		this._script = script;
		this._title = title;
	}

	protected String featureKey()
	{
		return this._name.replaceAll(".", "_");
	}

	public String name(Context context)
	{
		return "javascript::" + this._name;
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
			e.printStackTrace();
		}

	    return script;
	}

	public void processData(Context context, JSONObject json)
	{
		long now = System.currentTimeMillis();

		if (now - this._lastRun < 5000)
			return;

		this._lastRun = now;

		if (this.isEnabled(context) == false)
			return;

		if (this._script == null)
			this._script = JavascriptFeature.scriptForFeature(context, this._name);

		org.mozilla.javascript.Context jsContext = org.mozilla.javascript.Context.enter();
		jsContext.setOptimizationLevel(-1);

		Scriptable scope = jsContext.initStandardObjects();

		String script = this._script;

		script = "var probe = " + json.toString() + "; " + script;

		Object o =  jsContext.evaluateString(scope, script, "<engine>", 1, null);

		Log.e("PRM", "GOT VALUE " + o + " " + o.getClass());

		Bundle bundle = new Bundle();
		bundle.putString("PROBE", this.name(context));
		bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

		if (o instanceof String)
			bundle.putString(Feature.FEATURE_VALUE, o.toString());
		else if (o instanceof Double)
		{
			Double d = (Double) o;

			bundle.putDouble(Feature.FEATURE_VALUE, d.doubleValue());
		}

		Log.e("PRM", "XMITING " + bundle);

		this.transmitData(context, bundle);
	}
}
