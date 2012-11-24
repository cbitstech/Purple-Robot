package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class LocationProbe extends Probe implements LocationListener
{
	private static final String LATITUDE = "LATITUDE";
	private static final String LONGITUDE = "LONGITUDE";
	private static final String PROVIDER = "PROVIDER";
	private static final String ACCURACY = "ACCURACY";
	private static final String ALTITUDE = "ALTITUDE";
	private static final String BEARING = "BEARING";
	private static final String SPEED = "SPEED";
	private static final String TIME_FIX = "TIME_FIX";

	protected Context _context = null;

	private long _lastCheck = 0;
	private long _lastTransmit = 0;

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));

		String key = "location";

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_" + key + "_enabled");
		enabled.setDefaultValue(false);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_" + key + "_frequency");
		duration.setDefaultValue("300000");
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

			long now = System.currentTimeMillis();

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

			if (prefs.getBoolean("config_probe_location_enabled", false))
			{
				synchronized(this)
				{
					long freq = Long.parseLong(prefs.getString("config_probe_location_frequency", "300000"));

					if (now - this._lastCheck > 30000 && now - this._lastCheck < freq) // Try to get position in 30 seconds...
						locationManager.removeUpdates(this);
					else if (now - this._lastCheck > freq)
					{
						if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
							locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
						else
							locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);

						this._lastCheck = now;
					}
				}

				return true;
			}
		}

		locationManager.removeUpdates(this);

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
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe";
	}

	public void onLocationChanged(Location location)
	{
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
			this.transmitData(this._context, bundle);
		}
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
