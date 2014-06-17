package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class RawLocationProbe extends Probe implements LocationListener
{
	public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.RawLocationProbe";

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
	
	public static String ENABLED = "config_probe_raw_location_enabled";
	public static String FREQUENCY = "config_probe_raw_location_frequency";

	private static final boolean DEFAULT_ENABLED = false;

	private static final String GPS_AVAILABLE = "GPS_AVAILABLE";
	private static final String NETWORK_AVAILABLE = "NETWORK_AVAILABLE";

	private static final String LOG_EVENT = "LOG_EVENT";
	private static final String PROVIDER_DISABLED = "PROVIDER_DISABLED";
	private static final String PROVIDER_ENABLED = "PROVIDER_ENABLED";

	private static final String PROVIDER_STATUS_CHANGE = "PROVIDER_STATUS_CHANGE";
	private static final String PROVIDER_STATUS = "PROVIDER_STATUS";
	private static final String PROVIDER_STATUS_AVAILABLE = "AVAILABLE";
	private static final String PROVIDER_STATUS_OUT_OF_SERVICE = "OUT_OF_SERVICE";
	private static final String PROVIDER_STATUS_TEMPORARILY_UNAVAILABLE = "TEMPORARILY_UNAVAILABLE";

	protected Context _context = null;

	private long _lastFrequency = 0;
	private boolean _listening = false;
	
	private HashMap<String, Boolean> _lastEnabled = new HashMap<String, Boolean>();
	private HashMap<String, Integer> _lastStatus = new HashMap<String, Integer>();

	public String probeCategory(Context context)
	{
		return context.getString(R.string.probe_misc_category);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean(RawLocationProbe.ENABLED, true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean(RawLocationProbe.ENABLED, false);
		
		e.commit();
	}

	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);

		long freq = Long.parseLong(prefs.getString(RawLocationProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
		
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
				
				e.putString(RawLocationProbe.FREQUENCY, frequency.toString());
				e.commit();
			}
		}
	}

	public String summary(Context context) 
	{
		return context.getString(R.string.summary_raw_location_probe_desc);
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(final PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_raw_location_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey(RawLocationProbe.ENABLED);
		enabled.setDefaultValue(RawLocationProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey(RawLocationProbe.FREQUENCY);
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

			if (prefs.getBoolean(RawLocationProbe.ENABLED, RawLocationProbe.DEFAULT_ENABLED))
			{
				long freq = Long.parseLong(prefs.getString(RawLocationProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

				if (this._lastFrequency != freq || this._listening == false)
				{
					this._lastFrequency = freq;
					this._listening = false;
					
					locationManager.removeUpdates(this);

					if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
						locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, freq, 1, this);

					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, freq, 1, this);

					this._listening = true;

					this.onLocationChanged(locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
				}

				return true;
			}
		}

		this._listening = false;

		locationManager.removeUpdates(this);

		return false;
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_raw_location_probe);
	}

	public String name(Context context)
	{
		return RawLocationProbe.NAME;
	}

	public void onLocationChanged(Location location)
	{
		if (location == null)
			return;
		
		long now = System.currentTimeMillis();

		final Bundle bundle = new Bundle();

		bundle.putString("PROBE", this.name(this._context));
		bundle.putLong("TIMESTAMP", now / 1000);

		bundle.putDouble(RawLocationProbe.LATITUDE, location.getLatitude());
		bundle.putDouble(RawLocationProbe.LONGITUDE, location.getLongitude());
		
		bundle.putString(RawLocationProbe.PROVIDER, location.getProvider());

		LocationManager locationManager = (LocationManager) this._context.getSystemService(Context.LOCATION_SERVICE);
		
		bundle.putBoolean(RawLocationProbe.GPS_AVAILABLE, locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
		bundle.putBoolean(RawLocationProbe.NETWORK_AVAILABLE, locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));

		if (location.hasAccuracy())
			bundle.putFloat(RawLocationProbe.ACCURACY, location.getAccuracy());

		if (location.hasAltitude())
			bundle.putDouble(RawLocationProbe.ALTITUDE, location.getAltitude());

		if (location.hasBearing())
			bundle.putFloat(RawLocationProbe.BEARING, location.getBearing());

		if (location.hasSpeed())
			bundle.putFloat(RawLocationProbe.SPEED, location.getSpeed());

		bundle.putLong(RawLocationProbe.TIME_FIX, location.getTime());

		this.transmitData(this._context, bundle);
	}
	
	private void logEvent(Bundle bundle)
	{
		long now = System.currentTimeMillis();

		bundle.putString("PROBE", this.name(this._context) + "EventLog");
		bundle.putLong("TIMESTAMP", now / 1000);

		this.transmitData(this._context, bundle);
	}

	public void onProviderDisabled(String provider)
	{
		if (this._lastEnabled.containsKey(provider) && this._lastEnabled.get(provider).booleanValue() == false)
			return;
			
		this._lastFrequency = 0;
		
		Bundle bundle = new Bundle();
		bundle.putString(RawLocationProbe.PROVIDER, provider);
		bundle.putString(RawLocationProbe.LOG_EVENT, RawLocationProbe.PROVIDER_DISABLED);
		
		this.logEvent(bundle);
		
		this._lastEnabled.put(provider, false);

		if (this._context != null)
			this.isEnabled(this._context);
	}

	public void onProviderEnabled(String provider)
	{
		if (this._lastEnabled.containsKey(provider) && this._lastEnabled.get(provider).booleanValue() == true)
			return;

		this._lastFrequency = 0;

		Bundle bundle = new Bundle();
		bundle.putString(RawLocationProbe.PROVIDER, provider);
		bundle.putString(RawLocationProbe.LOG_EVENT, RawLocationProbe.PROVIDER_ENABLED);
		
		this.logEvent(bundle);

		this._lastEnabled.put(provider, true);

		if (this._context != null)
			this.isEnabled(this._context);
	}

	public void onStatusChanged(String provider, int status, Bundle bundle)
	{
		if (this._lastStatus.containsKey(provider) && this._lastStatus.get(provider).intValue() == status)
			return;

		this._lastFrequency = 0;

		bundle.putString(RawLocationProbe.PROVIDER, provider);
		bundle.putString(RawLocationProbe.LOG_EVENT, RawLocationProbe.PROVIDER_STATUS_CHANGE);
		
		switch (status)
		{
			case LocationProvider.OUT_OF_SERVICE:
				bundle.putString(RawLocationProbe.PROVIDER_STATUS, RawLocationProbe.PROVIDER_STATUS_OUT_OF_SERVICE);
				break;
			case LocationProvider.AVAILABLE:
				bundle.putString(RawLocationProbe.PROVIDER_STATUS, RawLocationProbe.PROVIDER_STATUS_AVAILABLE);
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				bundle.putString(RawLocationProbe.PROVIDER_STATUS, RawLocationProbe.PROVIDER_STATUS_TEMPORARILY_UNAVAILABLE);
				break;
		}
		
		this.logEvent(bundle);

		this._lastStatus.put(provider, status);

		if (this._context != null)
			this.isEnabled(this._context);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		double latitude = bundle.getDouble(RawLocationProbe.LATITUDE);
		double longitude = bundle.getDouble(RawLocationProbe.LONGITUDE);

		return String.format(context.getResources().getString(R.string.summary_location_probe), latitude, longitude);
	}
}
