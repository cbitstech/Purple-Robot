package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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

public class FusedLocationProbe extends Probe implements GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleApiClient.OnConnectionFailedListener {
    public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.FusedLocationProbe";

    public static final String LATITUDE = "LATITUDE";
    public static final String LONGITUDE = "LONGITUDE";
    private static final String PROVIDER = "PROVIDER";
    private static final String ACCURACY = "ACCURACY";
    private static final String ALTITUDE = "ALTITUDE";
    private static final String BEARING = "BEARING";
    private static final String SPEED = "SPEED";
    private static final String TIME_FIX = "TIME_FIX";

    public static final String LATITUDE_KEY = LATITUDE;
    public static final String LONGITUDE_KEY = LONGITUDE;

    private static final long LOCATION_TIMEOUT = (1000 * 60 * 5);

    public static final String DB_TABLE = "fused_location_probe";

    public static String ENABLED = "config_probe_fused_location_enabled";
    public static String FREQUENCY = "config_probe_fused_location_frequency";
    public static final String DISTANCE = "config_probe_fused_location_distance";

    public static final boolean DEFAULT_ENABLED = false;
    public static final String DEFAULT_DISTANCE = "800";

    public static final boolean DEFAULT_ENABLE_CALIBRATION_NOTIFICATIONS = true;
    public static final String ENABLE_CALIBRATION_NOTIFICATIONS = "config_probe_fused_location_calibration_notifications";

    protected Context _context = null;

    private long _lastFrequency = 0;
    private boolean _listening = false;

    private final HashMap<String, Boolean> _lastEnabled = new HashMap<>();
    private final HashMap<String, Integer> _lastStatus = new HashMap<>();
    private GoogleApiClient _apiClient = null;
    private long _lastDistance = 0;
    private long _lastCache = 0;
    private Location _lastLocation = null;

    private long _probeEnabled = 0;
    private long _lastReading = 0;

    @Override
    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_sensor_category);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(FusedLocationProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(FusedLocationProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString(FusedLocationProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
        map.put(Probe.PROBE_FREQUENCY, freq);

        long distance = Long.parseLong(prefs.getString(FusedLocationProbe.DISTANCE, FusedLocationProbe.DEFAULT_DISTANCE));
        map.put(Probe.PROBE_DISTANCE, distance);

        boolean calibrateNotes = prefs.getBoolean(FusedLocationProbe.ENABLE_CALIBRATION_NOTIFICATIONS, FusedLocationProbe.DEFAULT_ENABLE_CALIBRATION_NOTIFICATIONS);
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

                e.putString(FusedLocationProbe.FREQUENCY, frequency.toString());
                e.commit();
            }
        }

        if (params.containsKey(Probe.PROBE_DISTANCE))
        {
            Object distance = params.get(Probe.PROBE_DISTANCE);

            if (distance instanceof Double)
            {
                distance = ((Double) distance).longValue();
            }

            if (distance instanceof Long)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(FusedLocationProbe.DISTANCE, distance.toString());
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

                e.putBoolean(FusedLocationProbe.ENABLE_CALIBRATION_NOTIFICATIONS, ((Boolean) enable));
                e.commit();
            }
        }

    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_fused_location_probe_desc);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(final Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_fused_location_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(FusedLocationProbe.ENABLED);
        enabled.setDefaultValue(FusedLocationProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleListPreference duration = new FlexibleListPreference(context);
        duration.setKey(FusedLocationProbe.FREQUENCY);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);
        duration.setEntryValues(R.array.probe_satellite_frequency_values);
        duration.setEntries(R.array.probe_satellite_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);

        screen.addPreference(duration);

        FlexibleListPreference distance = new FlexibleListPreference(context);
        distance.setKey(FusedLocationProbe.DISTANCE);
        distance.setDefaultValue(FusedLocationProbe.DEFAULT_DISTANCE);
        distance.setEntryValues(R.array.probe_fused_location_distance);
        distance.setEntries(R.array.probe_fused_location_distance_labels);
        distance.setTitle(R.string.probe_fused_location_distance_label);

        screen.addPreference(distance);

        Preference calibrate = new Preference(context);
        calibrate.setTitle(R.string.config_probe_calibrate_title);
        calibrate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                Intent intent = new Intent(context, LocationLabelActivity.class);
                context.startActivity(intent);

                return true;
            }
        });

        screen.addPreference(calibrate);

        CheckBoxPreference enableCalibrationNotifications = new CheckBoxPreference(context);
        enableCalibrationNotifications.setTitle(R.string.title_enable_calibration_notifications);
        enableCalibrationNotifications.setSummary(R.string.summary_enable_calibration_notifications);
        enableCalibrationNotifications.setKey(FusedLocationProbe.ENABLE_CALIBRATION_NOTIFICATIONS);
        enableCalibrationNotifications.setDefaultValue(FusedLocationProbe.DEFAULT_ENABLE_CALIBRATION_NOTIFICATIONS);

        screen.addPreference(enableCalibrationNotifications);

        return screen;
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

            JSONObject distance = new JSONObject();
            distance.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
            values = new JSONArray();

            options = context.getResources().getStringArray(R.array.probe_fused_location_distance);

            for (String option : options)
            {
                values.put(Long.parseLong(option));
            }

            frequency.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_DISTANCE, distance);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context) && prefs.getBoolean(FusedLocationProbe.ENABLED, FusedLocationProbe.DEFAULT_ENABLED))
        {
            long now = System.currentTimeMillis();

            if (this._probeEnabled == 0)
                this._probeEnabled = now;

            this._context = context.getApplicationContext();

            long freq = Long.parseLong(prefs.getString(FusedLocationProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
            long distance = Long.parseLong(prefs.getString(FusedLocationProbe.DISTANCE, FusedLocationProbe.DEFAULT_DISTANCE));

            if (this._lastFrequency != freq || this._listening == false || this._lastDistance != distance)
            {
                LocationCalibrationHelper.check(context);

                this._lastFrequency = freq;
                this._listening = false;

                if (this._apiClient != null)
                {
                    if (this._apiClient.isConnected())
                    {
                        try
                        {
                            LocationServices.FusedLocationApi.removeLocationUpdates(this._apiClient, this);
                            this._apiClient.disconnect();
                        }
                        catch (IllegalStateException e)
                        {
                            LogManager.getInstance(context).logException(e);
                        }
                    }

                    this._apiClient = null;
                }

                this._listening = true;
            }

            if (this._apiClient == null)
            {
                GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this._context);
                builder.addConnectionCallbacks(this);
                builder.addOnConnectionFailedListener(this);
                builder.addApi(LocationServices.API);

                this._apiClient = builder.build();
                this._apiClient.connect();
            }

            String name = context.getString(R.string.name_location_services_check);
            String message = context.getString(R.string.message_location_services_check);

            if (now - this._probeEnabled > FusedLocationProbe.LOCATION_TIMEOUT && now - this._lastReading > FusedLocationProbe.LOCATION_TIMEOUT)
            {
                SanityManager.getInstance(context).addAlert(SanityCheck.WARNING, name, message, new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        context.startActivity(intent);
                    }
               });
            }
            else
                SanityManager.getInstance(context).clearAlert(name);

            return true;
        }
        else
        {
            this._probeEnabled = 0;

            if (this._apiClient != null)
            {
                this._apiClient.disconnect();
                this._apiClient = null;
            }
        }

        this._listening = false;

        return false;
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_fused_location_probe);
    }

    @Override
    public String name(Context context)
    {
        return FusedLocationProbe.NAME;
    }

    @Override
    public void onLocationChanged(Location location)
    {
        if (location == null)
            return;

        long now = System.currentTimeMillis();

        this._lastReading = now;

        final Bundle bundle = new Bundle();

        bundle.putString("PROBE", this.name(this._context));
        bundle.putLong("TIMESTAMP", now / 1000);

        bundle.putDouble(FusedLocationProbe.LATITUDE, location.getLatitude());
        bundle.putDouble(FusedLocationProbe.LONGITUDE, location.getLongitude());

        bundle.putString(FusedLocationProbe.PROVIDER, location.getProvider());

        if (location.hasAccuracy())
            bundle.putFloat(FusedLocationProbe.ACCURACY, location.getAccuracy());

        if (location.hasAltitude())
            bundle.putDouble(FusedLocationProbe.ALTITUDE, location.getAltitude());

        if (location.hasBearing())
            bundle.putFloat(FusedLocationProbe.BEARING, location.getBearing());

        if (location.hasSpeed())
            bundle.putFloat(FusedLocationProbe.SPEED, location.getSpeed());

        bundle.putLong(FusedLocationProbe.TIME_FIX, location.getTime());

        synchronized (this) {
            long time = location.getTime();

            if (time - this._lastCache > 30000 || this._lastLocation == null) {
                boolean include = true;

                if (this._lastLocation != null && this._lastLocation.distanceTo(location) < 50.0)
                    include = false;

                if (include) {
                    Map<String, Object> values = new HashMap<>();

                    values.put(LocationProbe.LONGITUDE_KEY, location.getLongitude());
                    values.put(LocationProbe.LATITUDE_KEY, location.getLatitude());
                    values.put(ProbeValuesProvider.TIMESTAMP, (double) (location.getTime() / 1000));

                    ProbeValuesProvider.getProvider(this._context).insertValue(this._context, FusedLocationProbe.DB_TABLE, FusedLocationProbe.databaseSchema(), values);

                    this._lastCache = time;
                    this._lastLocation = new Location(location);
                }
            }

            final FusedLocationProbe me = this;

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    FoursquareProbe.annotate(me._context, bundle);

                    me.transmitData(me._context, bundle);
                }
            };

            Thread t = new Thread(r);
            t.start();
        }

        this.transmitData(this._context, bundle);
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double latitude = bundle.getDouble(FusedLocationProbe.LATITUDE);
        double longitude = bundle.getDouble(FusedLocationProbe.LONGITUDE);

        return String.format(context.getResources().getString(R.string.summary_location_probe), latitude, longitude);
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        final LocationRequest request = new LocationRequest();
        request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        SharedPreferences prefs = Probe.getPreferences(this._context);

        long freq = Long.parseLong(prefs.getString(FusedLocationProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

        request.setInterval(freq);

        long distance = Long.parseLong(prefs.getString(FusedLocationProbe.DISTANCE, FusedLocationProbe.DEFAULT_DISTANCE));

        if (distance != 0)
            request.setSmallestDisplacement(distance);

        try {
            if (this._apiClient != null && this._apiClient.isConnected())
                LocationServices.FusedLocationApi.requestLocationUpdates(this._apiClient, request, this, this._context.getMainLooper());
        }
        catch (IllegalStateException e)
        {
            LogManager.getInstance(this._context).logException(e);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (this._apiClient != null && this._apiClient.isConnected())
            LocationServices.FusedLocationApi.removeLocationUpdates(this._apiClient, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        this._apiClient = null;
    }

    public static Map<String, String> databaseSchema()
    {
        HashMap<String, String> schema = new HashMap<>();

        schema.put(LocationProbe.LATITUDE_KEY, ProbeValuesProvider.REAL_TYPE);
        schema.put(LocationProbe.LONGITUDE_KEY, ProbeValuesProvider.REAL_TYPE);

        return schema;
    }

    @Override
    public Intent viewIntent(Context context)
    {
        try
        {
            Class.forName("com.google.android.maps.MapActivity");

            Intent i = new Intent(context, LocationProbeActivity.class);
            i.putExtra(LocationProbeActivity.DB_TABLE_NAME, FusedLocationProbe.DB_TABLE);

            return i;
        }
        catch (Exception e)
        {
            return super.viewIntent(context);
        }
    }
}
