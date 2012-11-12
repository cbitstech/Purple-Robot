package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.WazaBe.HoloEverywhere.preference.PreferenceManager;
import com.WazaBe.HoloEverywhere.preference.PreferenceScreen;
import com.WazaBe.HoloEverywhere.preference.SharedPreferences;
import com.WazaBe.HoloEverywhere.sherlock.SPreferenceActivity;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class VisibleSatelliteProbe extends Probe implements GpsStatus.Listener, GpsStatus.NmeaListener
{
	protected Context _context = null;

	private ArrayList<Bundle> _satellites = new ArrayList<Bundle>();

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

/*		String key = this.getPreferenceKey();

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_" + key + "_enabled");
		enabled.setDefaultValue(true);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_" + key + "_frequency");
		duration.setDefaultValue("1000");
		duration.setEntryValues(this.getResourceFrequencyValues());
		duration.setEntries(this.getResourceFrequencyLabels());
		duration.setTitle(R.string.probe_frequency_label);

		screen.addPreference(duration);
*/

		return screen;
	}

	public boolean isEnabled(Context context)
	{
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		locationManager.removeGpsStatusListener(this);
		locationManager.removeNmeaListener(this);

        this._context = context.getApplicationContext();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		if (prefs.getBoolean("config_probe_satellites_enabled", true))
		{
			locationManager.addGpsStatusListener(this);
			locationManager.addNmeaListener(this);

			return true;
		}

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

			LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this._context);
			Intent intent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
			intent.putExtras(data);

			localManager.sendBroadcast(intent);
		}
	}

	public void onGpsStatusChanged(int event)
	{
		switch (event)
		{
			case GpsStatus.GPS_EVENT_STARTED:
				break;
			case GpsStatus.GPS_EVENT_STOPPED:
				this._satellites.clear();

				Bundle b = new Bundle();

				b.putParcelableArrayList("SATELLITE_INFO", this._satellites);
				b.putInt("SATELLITE_COUNT", this._satellites.size());

				this.transmitData(b);

				break;
			case GpsStatus.GPS_EVENT_FIRST_FIX:
				break;
			case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
				LocationManager locationManager = (LocationManager) this._context.getSystemService(Context.LOCATION_SERVICE);

				GpsStatus status = locationManager.getGpsStatus(null);

				Iterable<GpsSatellite> sats = status.getSatellites();

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

				bundle.putParcelableArrayList("SATELLITE_INFO", this._satellites);
				bundle.putInt("SATELLITE_COUNT", this._satellites.size());

				this.transmitData(bundle);

				break;
		}
	}

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.VisibleSatelliteProbe";
	}

	public void onNmeaReceived(long timestamp, String nmea)
	{
		Log.e("PRM", "NMEA (" + timestamp + "): " + nmea);
	}
}
