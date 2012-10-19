package edu.northwestern.cbits.purple_robot_manager.plugins;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.WazaBe.HoloEverywhere.preference.PreferenceManager;
import com.WazaBe.HoloEverywhere.preference.SharedPreferences;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import edu.mit.media.funf.Utils;
import edu.northwestern.cbits.purple_robot_manager.JsonUtils;
import edu.northwestern.cbits.purple_robot_manager.R;

public abstract class OutputPlugin
{
	public static final String PAYLOAD = "edu.northwestern.cbits.purple_robot.OUTPUT_EVENT_PLUGIN";
	public static final String OUTPUT_EVENT = "edu.northwestern.cbits.purple_robot.OUTPUT_EVENT";
	public static final String LOG_EVENT = "edu.northwestern.cbits.purple_robot.LOG_EVENT";
	public static final String FORCE_UPLOAD = "edu.northwestern.cbits.purple_robot.FORCE_UPLOAD";
	public static final String DISPLAY_MESSAGE = "edu.northwestern.cbits.purple_robot.DISPLAY_MESSAGE";

	public abstract String[] respondsTo();
	public abstract void processIntent(Intent intent);

	protected SharedPreferences preferences = null;

	private Context _context = null;

	public void setContext(Context context)
	{
		this._context = context;

		this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public Context getContext()
	{
		return this._context;
	}

	protected void broadcastMessage(String message)
	{
		Intent displayIntent = new Intent(OutputPlugin.DISPLAY_MESSAGE);
		displayIntent.putExtra("MESSAGE", message);

		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this.getContext());
		manager.sendBroadcast(displayIntent);
	}

	public boolean shouldRespond(String intentAction)
	{
		String[] actions = this.respondsTo();

		for (String action : actions)
		{
			if (action.equalsIgnoreCase(intentAction))
				return true;
		}

		return false;
	}

	@SuppressWarnings("rawtypes")
	private static List<Class> _pluginClasses = new ArrayList<Class>();

	@SuppressWarnings("unchecked")
	public static void loadPluginClasses(Context context)
	{
		String packageName = OutputPlugin.class.getPackage().getName();

		String[] probeClasses = context.getResources().getStringArray(R.array.output_plugin_classes);

		IntentFilter intentFilter = new IntentFilter();

		for (String className : probeClasses)
		{
			try
			{
				@SuppressWarnings("rawtypes")
				Class pluginClass = Class.forName(packageName + "." + className);

				OutputPlugin.registerPluginClass(pluginClass);

				OutputPlugin plugin = (OutputPlugin) pluginClass.newInstance();

	            Method method = pluginClass.getDeclaredMethod("respondsTo");

	            String[] actions = (String[]) method.invoke(plugin, new Object[0]);

	            for (String action : actions)
	            {
	            	if (intentFilter.hasAction(action) == false)
	            		intentFilter.addAction(action);
	            }
			}
			catch (ClassNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (NoSuchMethodException e)
			{
				e.printStackTrace();
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}
			catch (InvocationTargetException e)
			{
				e.printStackTrace();
			}
			catch (InstantiationException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		intentFilter.addAction(OutputPlugin.OUTPUT_EVENT);

		LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
		localManager.registerReceiver(new OutputPluginManager(), intentFilter);
	}

	@SuppressWarnings("rawtypes")
	public static void registerPluginClass(Class pluginClass)
	{
		if (!OutputPlugin._pluginClasses.contains(pluginClass))
			OutputPlugin._pluginClasses.add(pluginClass);
	}

	@SuppressWarnings("rawtypes")
	public static List<Class> availablePluginClasses()
	{
		return OutputPlugin._pluginClasses;
	}

	public void process(Intent intent)
	{
		if (this.shouldRespond(intent.getAction()))
			this.processIntent(intent);
	}

	public static JSONObject jsonForBundle(Bundle bundle) throws JSONException
	{
		String jsonString = JsonUtils.getGson().toJson(Utils.getValues(bundle));

		return new JSONObject(jsonString);
	}
}
