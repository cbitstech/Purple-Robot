package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.WazaBe.HoloEverywhere.preference.CheckBoxPreference;
import com.WazaBe.HoloEverywhere.preference.ListPreference;
import com.WazaBe.HoloEverywhere.preference.PreferenceManager;
import com.WazaBe.HoloEverywhere.preference.PreferenceScreen;
import com.WazaBe.HoloEverywhere.preference.SharedPreferences;
import com.WazaBe.HoloEverywhere.sherlock.SPreferenceActivity;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class VisibleSatelliteProbe extends Probe implements GpsStatus.Listener, GpsStatus.NmeaListener, LocationListener
{
	protected Context _context = null;

	private ArrayList<Bundle> _satellites = new ArrayList<Bundle>();
	private long _lastCheck = 0;
	private long _startCheck = 0;
	private long _lastTransmit = 0;

	private boolean _listening = false;

	public Bundle[] dataRequestBundles(Context context)
	{
		return new Bundle[0];
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(SPreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));

		String key = "satellites";

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_" + key + "_enabled");
		enabled.setDefaultValue(true);

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

		locationManager.removeGpsStatusListener(this);
		locationManager.removeNmeaListener(this);

        this._context = context.getApplicationContext();

		long now = System.currentTimeMillis();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		if (prefs.getBoolean("config_probe_satellites_enabled", false))
		{
			locationManager.addGpsStatusListener(this);
			locationManager.addNmeaListener(this);

			synchronized(this)
			{
				long freq = Long.parseLong(prefs.getString("config_probe_satellite_frequency", "300000"));

				if (now - this._lastCheck > freq)
				{
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);

					this._lastCheck = now;
					this._startCheck = now;

					this._listening = true;
				}
			}

			return true;
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
		return context.getString(R.string.title_builtin_satellite_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	protected void transmitData(Bundle data)
	{
		if (this._context != null)
		{
			UUID uuid = UUID.randomUUID();
			data.putString("GUID", uuid.toString());

			try
			{
				JSONObject json = OutputPlugin.jsonForBundle(data);

				// Log.e("PRM", "SAT JSON: " + json.toString(2));
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}

			LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this._context);
			Intent intent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
			intent.putExtras(data);

			localManager.sendBroadcast(intent);
		}
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

					this._satellites.add(satBundle);
				}

				Bundle bundle = new Bundle();

				bundle.putString("PROBE", this.name(this._context));
				bundle.putLong("TIMESTAMP", now / 1000);
				bundle.putParcelableArrayList("SATELLITES", (ArrayList<Bundle>) this._satellites.clone());
				bundle.putInt("SATELLITE_COUNT", this._satellites.size());

				synchronized(this)
				{
					this.transmitData(bundle);
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

		return String.format(context.getResources().getString(R.string.summary_satellite_probe), count);
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
