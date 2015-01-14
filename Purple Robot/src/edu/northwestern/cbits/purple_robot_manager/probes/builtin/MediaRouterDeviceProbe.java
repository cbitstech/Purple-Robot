package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;

import com.google.android.gms.cast.CastMediaControlIntent;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class MediaRouterDeviceProbe extends Probe
{
    private static final boolean DEFAULT_ENABLED = false;
    protected static final String ROUTES = "ROUTES";
    protected static final String ROUTE_COUNT = "ROUTE_COUNT";

    private static final String FREQUENCY = "config_probe_mediarouter_frequency";
    private static final String ENABLED = "config_probe_mediarouter_enabled";

    private final Handler _handler = new Handler(Looper.getMainLooper());

    private long _lastCheck = 0;
    private final long _lastScan = 0;
    private boolean _isScanning = false;

    private final MediaRouter.Callback _callback = new MediaRouter.Callback()
    {
        @Override
        public void onProviderAdded(MediaRouter router, MediaRouter.ProviderInfo info)
        {
            // Log.e("PR", "PROVIDER ADD: " + info + " -- " +
            // info.getPackageName());
        }

        @Override
        public void onProviderChanged(MediaRouter router, MediaRouter.ProviderInfo info)
        {
            // Log.e("PR", "PROVIDER CHANGE: " + info + " -- " +
            // info.getPackageName());
        }

        @Override
        public void onProviderRemoved(MediaRouter router, MediaRouter.ProviderInfo info)
        {
            // Log.e("PR", "PROVIDER REMOVE: " + info + " -- " +
            // info.getPackageName());
        }

        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route)
        {
            // Log.e("PR", "ROUTE ADD: " + route.getName() + " -- " +
            // route.getPlaybackType() + " -- " + route.getVolume() + "/" +
            // route.getVolumeMax());
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route)
        {
            // Log.e("PR", "ROUTE CHANGE: " + route.getName() + " -- " +
            // route.getPlaybackType() + " -- " + route.getVolume() + "/" +
            // route.getVolumeMax());
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route)
        {
            // Log.e("PR", "ROUTE REMOVE: " + route.getName() + " -- " +
            // route.getPlaybackType() + " -- " + route.getVolume() + "/" +
            // route.getVolumeMax());
        }
    };

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.MediaRouterDeviceProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_mediarouter_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_other_devices_category);
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        if (super.isEnabled(context))
        {
            SharedPreferences prefs = Probe.getPreferences(context);

            if (prefs.getBoolean(MediaRouterDeviceProbe.ENABLED, MediaRouterDeviceProbe.DEFAULT_ENABLED))
            {
                final long now = System.currentTimeMillis();

                synchronized (this)
                {
                    long freq = Long.parseLong(prefs.getString(MediaRouterDeviceProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

                    final MediaRouterDeviceProbe me = this;

                    if (now - this._lastCheck > freq)
                    {
                        this._lastCheck = now;

                        this._handler.post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                MediaRouter router = MediaRouter.getInstance(context);

                                Bundle bundle = new Bundle();

                                bundle.putString("PROBE", me.name(context));
                                bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                                if (now - me._lastScan > 30000 && me._isScanning)
                                {
                                    router.removeCallback(me._callback);

                                    me._isScanning = false;
                                }
                                else if (now - me._lastScan > 300000 && me._isScanning == false)
                                {
                                    MediaRouteSelector.Builder builder = new MediaRouteSelector.Builder();
                                    builder = builder.addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO);
                                    builder = builder.addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO);
                                    builder = builder.addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
                                    builder = builder.addControlCategory(CastMediaControlIntent.CATEGORY_CAST);

                                    router.addCallback(builder.build(), me._callback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
                                }

                                ArrayList<Bundle> routes = new ArrayList<Bundle>();

                                for (MediaRouter.ProviderInfo info : router.getProviders())
                                {
                                    for (MediaRouter.RouteInfo route : info.getRoutes())
                                    {
                                        Bundle routeBundle = new Bundle();
                                        routeBundle.putString("PACKAGE", info.getPackageName());

                                        routeBundle.putString("NAME", route.getName());
                                        routeBundle.putString("DESCRIPTION", route.getDescription());
                                        routeBundle.putBoolean("ENABLED", route.isEnabled());
                                        routeBundle.putBoolean("DEFAULT", route.isDefault());
                                        routeBundle.putBoolean("SELECTED", route.isSelected());
                                        routeBundle.putInt("VOLUME", route.getVolume());
                                        routeBundle.putInt("VOLUME_MAX", route.getVolumeMax());

                                        switch (route.getPlaybackType())
                                        {
                                        case MediaRouter.RouteInfo.PLAYBACK_TYPE_LOCAL:
                                            routeBundle.putString("TYPE", "local");
                                            break;
                                        case MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE:
                                            routeBundle.putString("TYPE", "remote");
                                            break;
                                        }

                                        // routeBundle.putBundle("EXTRAS",
                                        // route.getExtras());

                                        routes.add(routeBundle);
                                    }
                                }

                                bundle.putParcelableArrayList(MediaRouterDeviceProbe.ROUTES, routes);
                                bundle.putInt(MediaRouterDeviceProbe.ROUTE_COUNT, routes.size());

                                bundle.putString("SELECTED_ROUTE", router.getSelectedRoute().getName());
                                bundle.putString("DEFAULT_ROUTE", router.getDefaultRoute().getName());

                                me.transmitData(context, bundle);
                            }
                        });
                    }
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(MediaRouterDeviceProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(MediaRouterDeviceProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        String name = bundle.getString("SELECTED_ROUTE");

        double volume = -1;
        double volumeMax = -1;

        ArrayList<Bundle> bundles = bundle.getParcelableArrayList("ROUTES");

        for (Bundle route : bundles)
        {
            if (name.equals(route.get("NAME")))
            {
                volume = route.getDouble("VOLUME");
                volumeMax = route.getDouble("VOLUME_MAX");
            }
        }

        return context.getString(R.string.summary_mediarouter_probe, name, volume, volumeMax);
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString(MediaRouterDeviceProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

        map.put(Probe.PROBE_FREQUENCY, freq);

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
                frequency = Long.valueOf(((Double) frequency).longValue());
            }

            if (frequency instanceof Long)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(MediaRouterDeviceProbe.FREQUENCY, frequency.toString());
                e.commit();
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_mediarouter_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(MediaRouterDeviceProbe.ENABLED);
        enabled.setDefaultValue(MediaRouterDeviceProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        ListPreference duration = new ListPreference(context);
        duration.setKey(MediaRouterDeviceProbe.FREQUENCY);
        duration.setEntryValues(R.array.probe_satellite_frequency_values);
        duration.setEntries(R.array.probe_satellite_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

        screen.addPreference(duration);

        return screen;
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_mediarouter_probe_desc);
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

}
