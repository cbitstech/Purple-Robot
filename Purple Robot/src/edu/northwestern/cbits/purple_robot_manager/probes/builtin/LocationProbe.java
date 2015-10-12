package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.ContextCompat;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.probes.LocationLabelActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.probes.LocationProbeActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.calibration.LocationCalibrationHelper;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.services.FoursquareProbe;
import edu.northwestern.cbits.purple_robot_manager.util.DBSCAN;

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
    private static final String CLUSTER = "CLUSTER";
    private static final String GPS_AVAILABLE = "GPS_AVAILABLE";
    private static final String NETWORK_AVAILABLE = "NETWORK_AVAILABLE";

    public static final boolean DEFAULT_ENABLED = true;

    public static final String ENABLED = "config_probe_location_enabled";
    private static final String FREQUENCY = "config_probe_location_frequency";

    public static final boolean DEFAULT_ENABLE_CALIBRATION_NOTIFICATIONS = true;
    public static final String ENABLE_CALIBRATION_NOTIFICATIONS = "config_probe_location_calibration_notifications";

    protected Context _context = null;

    private long _lastCheck = 0;
    private long _lastTransmit = 0;
    private boolean _listening = false;
    private long _lastCache = 0;
    private Location _lastLocation = null;

    @Override
    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_sensor_category);
    }

    @Override
    public Intent viewIntent(Context context)
    {
        try
        {
            Class.forName("com.google.android.maps.MapActivity");

            Intent i = new Intent(context, LocationProbeActivity.class);
            i.putExtra(LocationProbeActivity.DB_TABLE_NAME, LocationProbe.DB_TABLE);

            return i;
        }
        catch (Exception e)
        {
            return super.viewIntent(context);
        }
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(LocationProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(LocationProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString(LocationProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

        map.put(Probe.PROBE_FREQUENCY, freq);

        boolean calibrateNotes = prefs.getBoolean(LocationProbe.ENABLE_CALIBRATION_NOTIFICATIONS, LocationProbe.DEFAULT_ENABLE_CALIBRATION_NOTIFICATIONS);

        map.put(Probe.PROBE_CALIBRATION_NOTIFICATIONS, calibrateNotes);

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(Probe.PROBE_FREQUENCY))
        {
            Object frequency = params.get(Probe.PROBE_FREQUENCY);

            if (frequency instanceof Double)
            {
                frequency = ((Double) frequency).longValue();
            }

            if (frequency instanceof Long)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(LocationProbe.FREQUENCY, frequency.toString());
                e.commit();
            }
        }

        if (params.containsKey(Probe.PROBE_CALIBRATION_NOTIFICATIONS))
        {
            Object enable = params.get(Probe.PROBE_CALIBRATION_NOTIFICATIONS);

            if (enable instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(LocationProbe.ENABLE_CALIBRATION_NOTIFICATIONS, ((Boolean) enable));
                e.commit();
            }
        }
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_location_probe_desc);
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = new JSONObject();

        try
        {
            JSONObject enabled = new JSONObject();
            enabled.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            JSONArray values = new JSONArray();
            values.put(true);
            values.put(false);
            enabled.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_ENABLED, enabled);

            settings.put(Probe.PROBE_CALIBRATION_NOTIFICATIONS, enabled);

            JSONObject frequency = new JSONObject();
            frequency.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
            values = new JSONArray();

            String[] options = context.getResources().getStringArray(R.array.probe_satellite_frequency_values);

            for (String option : options)
            {
                values.put(Long.parseLong(option));
            }

            frequency.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_FREQUENCY, frequency);

        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(final Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_location_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(LocationProbe.ENABLED);
        enabled.setDefaultValue(LocationProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleListPreference duration = new FlexibleListPreference(context);
        duration.setKey(LocationProbe.FREQUENCY);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);
        duration.setEntryValues(R.array.probe_satellite_frequency_values);
        duration.setEntries(R.array.probe_satellite_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);

        screen.addPreference(duration);

        Preference calibrate = new Preference(context);
        calibrate.setTitle(R.string.config_probe_calibrate_title);
        calibrate.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference pref)
            {
                Intent intent = new Intent(context, LocationLabelActivity.class);
                context.startActivity(intent);

                return true;
            }
        });

        screen.addPreference(calibrate);

        CheckBoxPreference enableCalibrationNotifications = new CheckBoxPreference(context);
        enableCalibrationNotifications.setTitle(R.string.title_enable_calibration_notifications);
        enableCalibrationNotifications.setSummary(R.string.summary_enable_calibration_notifications);
        enableCalibrationNotifications.setKey(LocationProbe.ENABLE_CALIBRATION_NOTIFICATIONS);
        enableCalibrationNotifications.setDefaultValue(LocationProbe.DEFAULT_ENABLE_CALIBRATION_NOTIFICATIONS);

        screen.addPreference(enableCalibrationNotifications);

        return screen;
    }

    @Override
    public boolean isEnabled(Context context)
    {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (super.isEnabled(context))
        {
            this._context = context.getApplicationContext();

            SharedPreferences prefs = Probe.getPreferences(context);

            if (prefs.getBoolean(LocationProbe.ENABLED, LocationProbe.DEFAULT_ENABLED))
            {
                if (ContextCompat.checkSelfPermission(this._context, "android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this._context, "android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
                    long now = System.currentTimeMillis();

                    synchronized (this) {
                        Looper looper = Looper.myLooper();

                        if (looper == null)
                            Looper.prepare();

                        long freq = Long.parseLong(prefs.getString(LocationProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

                        if (now - this._lastCheck > 30000 && now - this._lastCheck < freq && this._listening) {
                            locationManager.removeUpdates(this);
                            this._listening = false;
                        } else if (now - this._lastCheck > freq && this._listening == false) {
                            LocationCalibrationHelper.check(context);

                            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);

                            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);

                            this._lastCheck = now;
                            this._listening = true;

                            this.onLocationChanged(locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
                        }
                    }
                }
                else
                    SanityManager.getInstance(context).addPermissionAlert(this.name(context), "android.permission.ACCESS_FINE_LOCATION", context.getString(R.string.rationale_pr_location_probe), null);

                return true;
            }
        }

        locationManager.removeUpdates(this);
        this._listening = false;

        return false;
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_location_probe);
    }

    @Override
    public String name(Context context)
    {
        return LocationProbe.NAME;
    }

    @Override
    public void onLocationChanged(Location location)
    {
        if (location == null)
            return;

        long now = System.currentTimeMillis();

        if (now - this._lastTransmit < 1000) // 1s
            return;

        this._lastTransmit = now;

        final Bundle bundle = new Bundle();

        bundle.putString("PROBE", this.name(this._context));
        bundle.putLong("TIMESTAMP", now / 1000);

        bundle.putDouble(LocationProbe.LATITUDE, location.getLatitude());
        bundle.putDouble(LocationProbe.LONGITUDE, location.getLongitude());

        String cluster = DBSCAN.inCluster(this._context, location.getLatitude(), location.getLongitude());

        if (cluster != null)
            bundle.putString(LocationProbe.CLUSTER, cluster);

        bundle.putString(LocationProbe.PROVIDER, location.getProvider());

        LocationManager locationManager = (LocationManager) this._context.getSystemService(Context.LOCATION_SERVICE);

        bundle.putBoolean(LocationProbe.GPS_AVAILABLE, locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
        bundle.putBoolean(LocationProbe.NETWORK_AVAILABLE, locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));

        if (location.hasAccuracy())
            bundle.putFloat(LocationProbe.ACCURACY, location.getAccuracy());

        if (location.hasAltitude())
            bundle.putDouble(LocationProbe.ALTITUDE, location.getAltitude());

        if (location.hasBearing())
            bundle.putFloat(LocationProbe.BEARING, location.getBearing());

        if (location.hasSpeed())
            bundle.putFloat(LocationProbe.SPEED, location.getSpeed());

        bundle.putLong(LocationProbe.TIME_FIX, location.getTime());

        synchronized (this)
        {
            long time = location.getTime();

            if (time - this._lastCache > 30000 || this._lastLocation == null)
            {
                boolean include = true;

                if (this._lastLocation != null && this._lastLocation.distanceTo(location) < 50.0)
                    include = false;

                if (include)
                {
                    Map<String, Object> values = new HashMap<>();

                    values.put(LocationProbe.LONGITUDE_KEY, location.getLongitude());
                    values.put(LocationProbe.LATITUDE_KEY, location.getLatitude());
                    values.put(ProbeValuesProvider.TIMESTAMP, (double) (location.getTime() / 1000));

                    ProbeValuesProvider.getProvider(this._context).insertValue(this._context, LocationProbe.DB_TABLE, LocationProbe.databaseSchema(), values);

                    this._lastCache = time;
                    this._lastLocation = new Location(location);
                }
            }

            final LocationProbe me = this;

            Runnable r = new Runnable()
            {
                @Override
                public void run()
                {
                    FoursquareProbe.annotate(me._context, bundle);

                    me.transmitData(me._context, bundle);
                }
            };

            Thread t = new Thread(r);
            t.start();
        }
    }

    @Override
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
    }

    public static Map<String, String> databaseSchema()
    {
        HashMap<String, String> schema = new HashMap<>();

        schema.put(LocationProbe.LATITUDE_KEY, ProbeValuesProvider.REAL_TYPE);
        schema.put(LocationProbe.LONGITUDE_KEY, ProbeValuesProvider.REAL_TYPE);

        return schema;
    }

    @Override
    public void onProviderDisabled(String provider)
    {
        if (this._context != null)
            this.isEnabled(this._context);
    }

    @Override
    public void onProviderEnabled(String provider)
    {
        if (this._context != null)
            this.isEnabled(this._context);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        if (this._context != null)
            this.isEnabled(this._context);
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double latitude = bundle.getDouble(LocationProbe.LATITUDE);
        double longitude = bundle.getDouble(LocationProbe.LONGITUDE);

        return String.format(context.getResources().getString(R.string.summary_location_probe), latitude, longitude);
    }
}
