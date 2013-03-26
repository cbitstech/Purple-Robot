package edu.northwestern.cbits.purple_robot_manager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;

public class ScheduleManager 
{
	private static final String DATE_FORMAT = "yyyyMMdd'T'HHmmss";
	
	public static String formatString(Date date)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(ScheduleManager.DATE_FORMAT);

		return sdf.format(date);
	}

	public static void runOverdueScripts(Context context) 
	{
		List<Map<String, String>> scripts = ScheduleManager.fetchScripts(context);
		
		long now = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat(ScheduleManager.DATE_FORMAT);

		List<Map<String, String>> executed = new ArrayList<Map<String, String>>();
		
		for (Map<String, String> script : scripts)
		{
			String dateString = script.get("date").toString();
			
			try 
			{
				Date d = sdf.parse(dateString);

				if (d.getTime() < now)
				{
					executed.add(script);
					
					String action = script.get("action");
					
					BaseScriptEngine.runScript(context, action);
				}
			}
			catch (Exception e) 
			{
				e.printStackTrace();

				executed.add(script);
			}
		}	
		
		scripts.removeAll(executed);
		
		ScheduleManager.persistScripts(context, scripts);
	}

	private static void persistScripts(Context context, List<Map<String, String>> scripts) 
	{
		JSONArray array = new JSONArray();
		
		for (Map<String, String> script : scripts)
		{
			JSONObject json = new JSONObject();
			
			if (script.containsKey("identifier") && script.containsKey("date") && script.containsKey("action"))
			{
				try 
				{
					json.put("identifier", script.get("identifier"));
					json.put("date", script.get("date"));
					json.put("action", script.get("action"));
					
					array.put(json);
				} 
				catch (JSONException e) 
				{
					e.printStackTrace();
				}
			}
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor e = prefs.edit();
		
		e.putString("scheduled_scripts", array.toString());
		
		e.commit();
	}

	private static List<Map<String, String>> fetchScripts(Context context) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		ArrayList<Map<String, String>> scripts = new ArrayList<Map<String, String>>();
		
		try 
		{
			JSONArray array = new JSONArray(prefs.getString("scheduled_scripts", "[]"));

			for (int i = 0; i < array.length(); i++)
			{
				JSONObject json = (JSONObject) array.get(i);
				
				HashMap<String, String> script = new HashMap<String, String>();
				
				if (json.has("identifier") && json.has("date") && json.has("action"))
				{
					script.put("identifier", json.get("identifier").toString());
					script.put("date", json.get("date").toString());
					script.put("action", json.get("action").toString());
					
					scripts.add(script);
				}
			}
		}
		catch (JSONException e) 
		{
			e.printStackTrace();
		}
		
		return scripts;
	}

	public static void updateScript(Context context, String identifier, String dateString, String action) 
	{
		List<Map<String, String>> scripts = ScheduleManager.fetchScripts(context);
		
		boolean found = false;
		
		for (Map<String, String> script : scripts)
		{
			if (identifier.equals(script.get("identifier")))
			{
				script.put("date", dateString);
				script.put("action", action);
				
				found = true;
			}
		}
		
		if (found == false)
		{
			HashMap<String, String> script = new HashMap<String, String>();

			script.put("date", dateString);
			script.put("action", action);
			script.put("identifier", identifier);
			
			scripts.add(script);
		}
		
		ScheduleManager.persistScripts(context, scripts);
	}
	
	public static Date clearMillis(Date d)
	{
		long time = d.getTime();
		
		time = time - (time % 1000);
		
		return new Date(time);
	}

	public static Date parseString(String dateString) 
	{
		SimpleDateFormat sdf = new SimpleDateFormat(ScheduleManager.DATE_FORMAT);
		
		try 
		{
			return ScheduleManager.clearMillis(sdf.parse(dateString));
			
		} 
		catch (ParseException e) 
		{
			e.printStackTrace();
			
			return null;
		}
	}
}
