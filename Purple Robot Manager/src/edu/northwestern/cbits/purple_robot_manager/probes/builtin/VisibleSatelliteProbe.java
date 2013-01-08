package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.GpsSatellite;
import android.location.GpsStatus;
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

public class VisibleSatelliteProbe extends Probe implements GpsStatus.Listener, GpsStatus.NmeaListener, LocationListener
{
	protected Context _context = null;

	private ArrayList<Bundle> _satellites = new ArrayList<Bundle>();
	private long _lastCheck = 0;
	private long _startCheck = 0;
	private long _lastTransmit = 0;

	private boolean _listening = false;

	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_visible_satellite_probe_desc);

		String key = "satellites";

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

	public void enable(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_satellites_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_satellites_enabled", false);
		
		e.commit();
	}

	public boolean isEnabled(Context context)
	{
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		if (super.isEnabled(context))
		{
	        this._context = context.getApplicationContext();

			long now = System.currentTimeMillis();

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

			if (prefs.getBoolean("config_probe_satellites_enabled", false))
			{
				if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
				{
					locationManager.addGpsStatusListener(this);
					locationManager.addNmeaListener(this);

					synchronized(this)
					{
						long freq = Long.parseLong(prefs.getString("config_probe_satellite_frequency", "300000"));

						if (now - this._lastCheck > 60000 && now - this._lastCheck < freq) // Try to get satellites in 60 seconds...
						{
							locationManager.removeGpsStatusListener(this);
							locationManager.removeNmeaListener(this);

							locationManager.removeUpdates(this);
						}
						if (now - this._lastCheck > freq)
						{
							locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);

							this._lastCheck = now;
							this._startCheck = now;

							this._listening = true;
						}
					}
				}

				return true;
			}
		}

		locationManager.removeGpsStatusListener(this);
		locationManager.removeNmeaListener(this);

		locationManager.removeUpdates(this);

		return false;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO...
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_visible_satellite_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}


	@SuppressWarnings("unchecked")
	public void onGpsStatusChanged(int event)
	{
		long now = System.currentTimeMillis();

		if (now - this._lastTransmit < 10000) // 10s
			return;

		this._lastTransmit = now;

		switch (event)
		{
			case GpsStatus.GPS_EVENT_STARTED:
				break;
			case GpsStatus.GPS_EVENT_STOPPED:
				/* this._satellites.clear();

				Bundle b = new Bundle();

				b.putString("PROBE", this.name(this._context));
				b.putLong("TIMESTAMP", now / 1000);

				b.putParcelableArrayList("SATELLITES", (ArrayList<Bundle>) this._satellites.clone());
				b.putInt("SATELLITE_COUNT", this._satellites.size());

				synchronized(this)
				{
					this.transmitData(b);
				}
				*/
				break;
			case GpsStatus.GPS_EVENT_FIRST_FIX:
				break;
			case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
				LocationManager locationManager = (LocationManager) this._context.getSystemService(Context.LOCATION_SERVICE);

				GpsStatus status = locationManager.getGpsStatus(null);

				Iterable<GpsSatellite> sats = status.getSatellites();

				this._satellites.clear();

				float snrSum = 0;
				int snrCount = 0;

				for (GpsSatellite sat : sats)
				{
					Bundle satBundle = new Bundle();

					satBundle.putFloat("AZIMUTH", sat.getAzimuth());
					satBundle.putFloat("ELEVATION", sat.getElevation());
					satBundle.putInt("RANDOM_NUMBER", sat.getPrn());
					satBundle.putFloat("SIGNAL_RATIO", sat.getSnr());
					satBundle.putBoolean("HAS_ALMANAC", sat.hasAlmanac());
					satBundle.putBoolean("HAS_EPHEMERIS", sat.hasEphemeris());
					satBundle.putBoolean("USED_IN_FIX", sat.usedInFix());

					snrSum += sat.getSnr();
					snrCount += 1;

					this._satellites.add(satBundle);
				}

				Bundle bundle = new Bundle();

				bundle.putString("PROBE", this.name(this._context));
				bundle.putLong("TIMESTAMP", now / 1000);
				bundle.putParcelableArrayList("SATELLITES", (ArrayList<Bundle>) this._satellites.clone());
				bundle.putInt("SATELLITE_COUNT", this._satellites.size());

				if (snrSum > 0)
					snrSum = snrSum / snrCount;

				bundle.putFloat("AVERAGE_SIGNAL_RATIO", snrSum);

				synchronized(this)
				{
					this.transmitData(this._context, bundle);
				}

				break;
		}

		if (this._context != null)
		{
			LocationManager locationManager = (LocationManager) this._context.getSystemService(Context.LOCATION_SERVICE);

			if (now - this._startCheck > 10000 && this._listening)
			{
				synchronized(this)
				{
					this._listening = false;

					locationManager.removeUpdates(this);

					this._startCheck = now;
				}

				this.isEnabled(this._context);
			}

			locationManager = null;
		}
	}

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.VisibleSatelliteProbe";
	}

	public void onNmeaReceived(long timestamp, String nmea)
	{
//		Log.e("PRM", "NMEA (" + timestamp + "): " + nmea);
	}

	public void onLocationChanged(Location location)
	{
//		Log.e("PRM", "LOCATION CHANGED: " + location);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		int count = bundle.getInt("SATELLITE_COUNT");
		float snr = bundle.getFloat("AVERAGE_SIGNAL_RATIO");

		return String.format(context.getResources().getString(R.string.summary_satellite_probe), count, snr);
	}

	public void onProviderDisabled(String provider)
	{

	}

	public void onProviderEnabled(String provider)
	{

	}

	public void onStatusChanged(String provider, int status, Bundle extras)
	{

	}
}
