package edu.northwestern.cbits.purple_robot_manager.probes.features;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.WiFiHelper;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe;

public class WeatherUndergroundFeature extends Feature 
{
	private static final String FEATURE_KEY = "weather_underground";
	protected static final String OBS_TIMESTAMP = "OBS_TIMESTAMP";
	protected static final String WEATHER = "WEATHER";
	protected static final String TEMPERATURE = "TEMPERATURE";
	protected static final String WIND_DEGREES = "WIND_DEGREES";
	protected static final String GUST_SPEED = "GUST_SPEED";
	protected static final String PRESSURE_TREND = "PRESSURE_TREND";
	protected static final String VISIBILITY = "VISIBILITY";
	protected static final String WIND_SPEED = "WIND_SPEED";
	protected static final String DEWPOINT = "DEWPOINT";
	protected static final String WIND_DIR = "WIND_DIR";
	protected static final String PRESSURE = "PRESSURE";
	protected static final String STATION_ID = "STATION_ID";
	protected static final String LOCATION = "LOCATION";

	private boolean _isInited = false;
	private boolean _isEnabled = false;
	
	public String probeCategory(Context context)
	{
		return context.getString(R.string.probe_external_environment_category);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		String weather = bundle.getString(WeatherUndergroundFeature.WEATHER);
		String station = bundle.getString(WeatherUndergroundFeature.STATION_ID);
		double temp = bundle.getDouble(WeatherUndergroundFeature.TEMPERATURE);

		return context.getResources().getString(R.string.summary_weather_underground_probe, weather, temp, station);
	}

	protected String featureKey() 
	{
		return WeatherUndergroundFeature.FEATURE_KEY;
	}

	protected String summary(Context context) 
	{
		return context.getString(R.string.summary_weather_underground_feature_desc);
	}

	public String name(Context context) 
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.features.WeatherUndergroundFeature";
	}

	public String title(Context context) 
	{
		return context.getString(R.string.title_weather_underground_feature);
	}

	public void enable(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);;
		
		Editor e = prefs.edit();
		e.putBoolean("config_feature_weather_underground_enabled", true);
		
		e.commit();
	}

	private long lastCheck(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);;
		
		return prefs.getLong("config_last_weather_underground_check", 0);
	}

	public void disable(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_feature_weather_underground_enabled", false);
		
		e.commit();
	}
	
	public boolean isEnabled(Context context)
	{
		if (!this._isInited)
		{
			IntentFilter intentFilter = new IntentFilter(Probe.PROBE_READING);

			final WeatherUndergroundFeature me = this;

			BroadcastReceiver receiver = new BroadcastReceiver()
			{
				public void onReceive(final Context context, Intent intent)
				{
					Bundle extras = intent.getExtras();

					String probeName = extras.getString("PROBE");

					if (probeName != null && (LocationProbe.NAME.equals(probeName)))
					{
						long now = System.currentTimeMillis();
					
						if (now - me.lastCheck(context) > (1000 * 60 * 60))
						{
							SharedPreferences prefs = Probe.getPreferences(context);

							if (prefs.getBoolean("config_restrict_data_wifi", true))
							{
								if (WiFiHelper.wifiAvailable(context) == false)
									return;
							}

							Editor e = prefs.edit();
							
							e.putLong("config_last_weather_underground_check", now);
							e.commit();
							
							final double latitude = extras.getDouble(LocationProbe.LATITUDE);
							final double longitude = extras.getDouble(LocationProbe.LONGITUDE);
							
							Runnable r = new Runnable()
							{
								public void run() 
								{
									try 
									{
										URL u = new URL("http://api.wunderground.com/api/eb50926364bb4c4f/conditions/q/" + latitude + "," + longitude + ".json");
										
										HttpURLConnection conn = (HttpURLConnection) u.openConnection();
										
										BufferedInputStream bin = new BufferedInputStream(conn.getInputStream());
										ByteArrayOutputStream bout = new ByteArrayOutputStream();

										byte[] buffer = new byte[4096];
										int read = 0;

										while ((read = bin.read(buffer, 0, buffer.length)) != -1)
										{
											bout.write(buffer, 0, read);
										}

										bin.close();

										String json = new String(bout.toByteArray(), "UTF-8");
										
										JSONObject conditions = new JSONObject(json);
										JSONObject obs = conditions.getJSONObject("current_observation");
										
										String location = obs.getJSONObject("observation_location").getString("full");
										String stationId = obs.getString("station_id");
										
										long obsTimestamp = Long.parseLong(obs.getString("observation_epoch"));
										
										String weather = obs.getString("weather");

										double temp = Double.parseDouble(obs.getString("temp_c"));
										
										String windDir = obs.getString("wind_dir");
										double windDegrees = obs.getDouble("wind_degrees");
										double windSpeed = obs.getDouble("wind_kph");
										double gustSpeed = obs.getDouble("wind_gust_kph");
										
										double dewPoint = obs.getDouble("dewpoint_c");
										double visiblility = obs.getDouble("visibility_km");

										double pressure = Double.parseDouble(obs.getString("pressure_mb"));
										String pressureTrend = obs.getString("pressure_trend");
										
										Bundle bundle = new Bundle();
										bundle.putString("PROBE", me.name(context));
										bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

										bundle.putLong(WeatherUndergroundFeature.OBS_TIMESTAMP, obsTimestamp);
										bundle.putString(WeatherUndergroundFeature.STATION_ID, stationId);
										bundle.putString(WeatherUndergroundFeature.LOCATION, location);
										bundle.putString(WeatherUndergroundFeature.WEATHER, weather);
										bundle.putDouble(WeatherUndergroundFeature.TEMPERATURE, temp);
										bundle.putString(WeatherUndergroundFeature.WIND_DIR, windDir);
										bundle.putDouble(WeatherUndergroundFeature.WIND_DEGREES, windDegrees);
										bundle.putDouble(WeatherUndergroundFeature.WIND_SPEED, windSpeed);
										bundle.putDouble(WeatherUndergroundFeature.GUST_SPEED, gustSpeed);
										bundle.putString(WeatherUndergroundFeature.PRESSURE_TREND, pressureTrend);
										bundle.putDouble(WeatherUndergroundFeature.PRESSURE, pressure);

										bundle.putDouble(WeatherUndergroundFeature.DEWPOINT, dewPoint);
										bundle.putDouble(WeatherUndergroundFeature.VISIBILITY, visiblility);

										me.transmitData(context, bundle);
									} 
									catch (MalformedURLException e) 
									{
										LogManager.getInstance(context).logException(e);
									} 
									catch (IOException e) 
									{
										LogManager.getInstance(context).logException(e);
									} 
									catch (JSONException e) 
									{
										LogManager.getInstance(context).logException(e);
									}
									catch (NumberFormatException e)
									{
										LogManager.getInstance(context).logException(e);
									}
								}
							};
							
							Thread t = new Thread(r);
							t.start();
						}
					}
				}
			};

			LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
			localManager.registerReceiver(receiver, intentFilter);

			SharedPreferences prefs = Probe.getPreferences(context);
			prefs.edit().putLong("config_last_weather_underground_check", 0).commit();

			this._isInited = true;
		}

		SharedPreferences prefs = Probe.getPreferences(context);

		this._isEnabled = false;

		if (super.isEnabled(context))
		{
			if (prefs.getBoolean("config_feature_weather_underground_enabled", true))
				this._isEnabled = true;
		}

		return this._isEnabled;
	}
}
