package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.LocationProbeActivity;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class LocationProbe extends Probe implements LocationListener
{
	public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe";

	public static final String LATITUDE = "LATITUDE";
	public static final String LONGITUDE = "LONGITUDE";
	private static final String PROVIDER = "PROVIDER";
	private static final String ACCURACY = "ACCURACY";
	private static final String ALTITUDE = "ALTITUDE";
	private static final String BEARING = "BEARING";
	private static final String SPEED = "SPEED";
	private static final String TIME_FIX = "TIME_FIX";
	public static final String LONGITUDE_KEY = LONGITUDE;
	public static final String LATITUDE_KEY = LATITUDE;
	public static final String DB_TABLE = "location_probe";

	private static final boolean DEFAULT_ENABLED = true;

	protected Context _context = null;

	private long _lastCheck = 0;
	private long _lastTransmit = 0;
	private boolean _listening = false;
	private long _lastCache = 0;
	private Location _lastLocation = null;

	public Intent viewIntent(Context context)
	{
		try
		{
			Class.forName("com.google.android.maps.MapActivity");
			
			Intent i = new Intent(context, LocationProbeActivity.class);
			
			return i;
		}
		catch (Exception e)
		{
			return super.viewIntent(context);
		}
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_location_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_location_enabled", false);
		
		e.commit();
	}

	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);

		long freq = Long.parseLong(prefs.getString("config_probe_location_frequency", Probe.DEFAULT_FREQUENCY));
		
		map.put(Probe.PROBE_FREQUENCY, freq);
		
		return map;
	}
	
	public void updateFromMap(Context context, Map<String, Object> params) 
	{
		super.updateFromMap(context, params);
		
		if (params.containsKey(Probe.PROBE_FREQUENCY))
		{
			Object frequency = params.get(Probe.PROBE_FREQUENCY);
			
			if (frequency instanceof Long)
			{
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putString("config_probe_location_frequency", frequency.toString());
				e.commit();
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_location_probe_desc);

		String key = "location";

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_" + key + "_enabled");
		enabled.setDefaultValue(LocationProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_" + key + "_frequency");
		duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);
		duration.setEntryValues(R.array.probe_satellite_frequency_values);
		duration.setEntries(R.array.probe_satellite_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);

		screen.addPreference(duration);

		return screen;
	}


	public boolean isEnabled(Context context)
	{
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		if (super.isEnabled(context))
		{
	        this._context = context.getApplicationContext();

			SharedPreferences prefs = Probe.getPreferences(context);

			if (prefs.getBoolean("config_probe_location_enabled", LocationProbe.DEFAULT_ENABLED))
			{
				long now = System.currentTimeMillis();

				synchronized(this)
				{
					Looper looper = Looper.myLooper();
					
					if (looper == null)
						Looper.prepare();
					
					long freq = Long.parseLong(prefs.getString("config_probe_location_frequency", Probe.DEFAULT_FREQUENCY));

					if (now - this._lastCheck > 30000 && now - this._lastCheck < freq && this._listening) // Try to get position in 30 seconds...
					{
						locationManager.removeUpdates(this);
						this._listening = false;
					}
					else if (now - this._lastCheck > freq && this._listening == false)
					{
						if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
							locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
						else
							locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);

						this._lastCheck = now;
						this._listening = true;

						this.onLocationChanged(locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
					}
				}

				return true;
			}
		}

		locationManager.removeUpdates(this);
		this._listening = false;

		return false;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO...
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_location_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public String name(Context context)
	{
		return LocationProbe.NAME;
	}

	public void onLocationChanged(Location location)
	{
		if (location == null)
			return;
		
		long now = System.currentTimeMillis();

		if (now - this._lastTransmit < 1000) // 1s
			return;

		this._lastTransmit = now;

		Bundle bundle = new Bundle();

		bundle.putString("PROBE", this.name(this._context));
		bundle.putLong("TIMESTAMP", now / 1000);

		bundle.putDouble(LocationProbe.LATITUDE, location.getLatitude());
		bundle.putDouble(LocationProbe.LONGITUDE, location.getLongitude());
		bundle.putString(LocationProbe.PROVIDER, location.getProvider());

		if (location.hasAccuracy())
			bundle.putFloat(LocationProbe.ACCURACY, location.getAccuracy());

		if (location.hasAltitude())
			bundle.putDouble(LocationProbe.ALTITUDE, location.getAltitude());

		if (location.hasBearing())
			bundle.putFloat(LocationProbe.BEARING, location.getBearing());

		if (location.hasSpeed())
			bundle.putFloat(LocationProbe.SPEED, location.getSpeed());

		bundle.putLong(LocationProbe.TIME_FIX, location.getTime());

		synchronized(this)
		{
			long time = location.getTime();

			if (time - this._lastCache > 30000 || this._lastLocation == null)
			{
				boolean include = true;

				if (this._lastLocation != null && this._lastLocation.distanceTo(location) < 50.0)
					include = false;

				if (include)
				{
					Map<String, Object> values = new HashMap<String, Object>();

					values.put(LocationProbe.LONGITUDE_KEY, Double.valueOf(location.getLongitude()));
					values.put(LocationProbe.LATITUDE_KEY, Double.valueOf(location.getLatitude()));
					values.put(ProbeValuesProvider.TIMESTAMP, Double.valueOf(location.getTime() / 1000));

					ProbeValuesProvider.getProvider(this._context).insertValue(this._context, LocationProbe.DB_TABLE, LocationProbe.databaseSchema(), values);

					this._lastCache = time;
					this._lastLocation = new Location(location);
				}
			}

			this.transmitData(this._context, bundle);
		}
	}

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);
		
		formatted.putString(context.getString(R.string.display_location_coordinates_label), String.format(context.getString(R.string.display_location_coordinates), bundle.getDouble(LocationProbe.LATITUDE), bundle.getDouble(LocationProbe.LONGITUDE)));
		formatted.putString(context.getString(R.string.display_location_provider_label), bundle.getString(LocationProbe.PROVIDER));
		formatted.putDouble(context.getString(R.string.display_location_altitude_label), bundle.getFloat(LocationProbe.ALTITUDE));
		formatted.putFloat(context.getString(R.string.display_location_accuracy_label), bundle.getFloat(LocationProbe.ACCURACY));
		formatted.putFloat(context.getString(R.string.display_location_bearing_label), bundle.getFloat(LocationProbe.BEARING));
		formatted.putFloat(context.getString(R.string.display_location_speed_label), bundle.getFloat(LocationProbe.SPEED));

		return formatted;
	};
	
	public static Map<String, String> databaseSchema()
	{
		HashMap<String, String> schema = new HashMap<String, String>();

		schema.put(LocationProbe.LATITUDE_KEY, ProbeValuesProvider.REAL_TYPE);
		schema.put(LocationProbe.LONGITUDE_KEY, ProbeValuesProvider.REAL_TYPE);

		return schema;
	}

	public void onProviderDisabled(String provider)
	{
		if (this._context != null)
			this.isEnabled(this._context);
	}

	public void onProviderEnabled(String provider)
	{
		if (this._context != null)
			this.isEnabled(this._context);
	}

	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		if (this._context != null)
			this.isEnabled(this._context);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		double latitude = bundle.getDouble(LocationProbe.LATITUDE);
		double longitude = bundle.getDouble(LocationProbe.LONGITUDE);

		return String.format(context.getResources().getString(R.string.summary_location_probe), latitude, longitude);
	}
}
