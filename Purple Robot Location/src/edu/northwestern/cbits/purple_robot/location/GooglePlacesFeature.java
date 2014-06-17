package edu.northwestern.cbits.purple_robot.location;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;
import edu.emory.mathcs.backport.java.util.Arrays;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe;

public class GooglePlacesFeature extends Feature 
{
	private static final String DEFAULT_RADIUS = "1000";

	private static String[] EXCLUDED_TYPES = { "establishment" };
	
	private BroadcastReceiver _receiver = null;
	protected long _lastCheck = 0;

	protected String featureKey() 
	{
		return "google_places";
	}

	public String summary(Context context) 
	{
		return context.getString(R.string.summary_google_places_feature_desc);
	}

	public String name(Context context) 
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.features.GooglePlacesFeature";
	}

	public String title(Context context) 
	{
		return context.getString(R.string.title_google_places_feature);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_external_services_category);
	}

	public void enable(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_google_places_enabled", true);
		e.commit();
	}

	public void disable(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_google_places_enabled", false);
		e.commit();
	}

	public boolean isEnabled(Context context)
	{
		LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);

		if (super.isEnabled(context))
		{
			SharedPreferences prefs = Probe.getPreferences(context);
			
			if (prefs.getBoolean("config_probe_google_places_enabled", true))
			{
				if (this._receiver == null)
				{		
					IntentFilter intentFilter = new IntentFilter(Probe.PROBE_READING);

					final GooglePlacesFeature me = this;
					
					this._receiver = new BroadcastReceiver()
					{
						public void onReceive(final Context context, Intent intent)
						{
							final Bundle extras = intent.getExtras();
							
							long now = System.currentTimeMillis();
							
							if (now - me._lastCheck > 300000) // 5 minutes
							{
								String probeName = extras.getString("PROBE");
								
								if (probeName != null && (LocationProbe.NAME.equals(probeName)))
								{
									Runnable r = new Runnable()
									{
										public void run() 
										{
											try 
											{
												Map<String, Integer> place = GooglePlacesFeature.nearestLocation(context, extras.getDouble(LocationProbe.LATITUDE), extras.getDouble(LocationProbe.LONGITUDE));
			
												Bundle bundle = new Bundle();
												bundle.putString("PROBE", me.name(context));
												bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
												
												if (place != null)
												{
													for (String key : place.keySet())
													{
														if (Arrays.asList(GooglePlacesFeature.EXCLUDED_TYPES).contains(key) == false)
															bundle.putInt(key, place.get(key).intValue());
													}
			
													me.transmitData(context, bundle);
												}
											} 
											catch (IOException e) 
											{
												LogManager.getInstance(context).logException(e);
											} 
											catch (JSONException e) 
											{
												LogManager.getInstance(context).logException(e);
											}
										}
									};
									
									Thread t = new Thread(r);
									t.start();
									
									me._lastCheck = now;
								}
							}
						}
					};

					localManager.registerReceiver(this._receiver, intentFilter);
				}
				
				return true;
			}
		}
		
		if (this._receiver != null)
		{
			localManager.unregisterReceiver(this._receiver);
			this._receiver = null;
		}

		return false;
	}

	protected static Map<String, Integer> nearestLocation(Context context, double latitude, double longitude) throws IOException, JSONException 
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		String key = context.getString(R.string.google_places_browser_key);
		
		String radius = prefs.getString("config_feature_google_places_radius", GooglePlacesFeature.DEFAULT_RADIUS);
		
		URL u = new URL("https://maps.googleapis.com/maps/api/place/search/json?location=" + latitude + "," + longitude + "&radius=" + radius + "&sensor=false&key=" + key);
		InputStream in = u.openStream();

		String jsonString = IOUtils.toString(in);
		
		in.close();

		JSONObject json = new JSONObject(jsonString);
		
		JSONArray results = json.getJSONArray("results");
		
		HashMap<String, Integer> place = new HashMap<String, Integer>();

		String[] availableTypes = context.getResources().getStringArray(R.array.google_places_types);
		
		for (String type : availableTypes)
		{
			place.put(type, Integer.valueOf(0));
		}
		
		if (results.length() > 0)
		{
			for (int i = 0; i < results.length(); i++)
			{
				JSONObject result = results.getJSONObject(i);
				
				JSONArray types = result.getJSONArray("types");
				
				for (int j = 0; j < types.length(); j++)
				{
					String type = types.getString(j);
					
					Integer count = place.get(type);
					
					if (count == null)
						count = Integer.valueOf(0);
					
					count = Integer.valueOf(count.intValue() + 1);
					
					place.put(type, count);
				}
			}
		}
		
		return place;
	}
	
	public String summarizeValue(Context context, Bundle bundle)
	{
		String frequentPlace = "none";
		int maxCount = 0;
		
		for (String key : bundle.keySet())
		{
			Object o = bundle.get(key);
			
			if (o instanceof Integer)
			{
				Integer count = (Integer) o;
				
				if (count.intValue() > maxCount)
				{
					frequentPlace = key;
					maxCount = count.intValue();
				}
			}
		}

		return String.format(context.getResources().getString(R.string.summary_google_places), frequentPlace.replaceAll("_", " "), maxCount);
	}
	
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceScreen screen = super.preferenceScreen(activity);

		ListPreference radius = new ListPreference(activity);
		radius.setKey("config_feature_google_places_radius");
		radius.setEntryValues(R.array.feature_google_places_values);
		radius.setEntries(R.array.feature_google_places_labels);
		radius.setTitle(R.string.feature_google_places_radius_label);
		radius.setDefaultValue(GooglePlacesFeature.DEFAULT_RADIUS);

		screen.addPreference(radius);

		return screen;
	}
}
