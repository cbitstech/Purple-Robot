package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.Map;

import com.google.android.gms.cast.CastMediaControlIntent;

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
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class MediaRouterDeviceProbe extends Probe 
{
	private static final boolean DEFAULT_ENABLED = false;
	private Handler _handler = new Handler(Looper.getMainLooper());

	private long _lastCheck = 0;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.MediaRouterDeviceProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_mediarouter_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_other_devices_category);
	}
	
	public static void handleDevices(Context context)
	{

		/*
								Bundle bundle = new Bundle();

								bundle.putString("PROBE", me.name(context));
								bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

								ArrayList<Bundle> accessPoints = new ArrayList<Bundle>();

								if (results != null)
								{
									bundle.putInt(WifiAccessPointsProbe.ACCESS_POINT_COUNT, results.size());

									for (ScanResult result : results)
									{
										Bundle pointBundle = new Bundle();

										pointBundle.putString(WifiAccessPointsProbe.BSSID, result.BSSID);
										pointBundle.putString(WifiAccessPointsProbe.SSID, result.SSID);
										pointBundle.putString(WifiAccessPointsProbe.CAPABILITIES, result.capabilities);

										pointBundle.putInt(WifiAccessPointsProbe.FREQUENCY, result.frequency);
										pointBundle.putInt(WifiAccessPointsProbe.LEVEL, result.level);

										accessPoints.add(pointBundle);
									}
								}
								else
									bundle.putInt(WifiAccessPointsProbe.ACCESS_POINT_COUNT, 0);

								bundle.putParcelableArrayList(WifiAccessPointsProbe.ACCESS_POINTS, accessPoints);

								WifiInfo wifiInfo = wifi.getConnectionInfo();

								if (wifiInfo != null)
								{
									bundle.putString(WifiAccessPointsProbe.CURRENT_BSSID, wifiInfo.getBSSID());
									bundle.putString(WifiAccessPointsProbe.CURRENT_SSID, wifiInfo.getSSID());
									bundle.putInt(WifiAccessPointsProbe.CURRENT_LINK_SPEED, wifiInfo.getLinkSpeed());
									bundle.putInt(WifiAccessPointsProbe.CURRENT_RSSI, wifiInfo.getRssi());
								}

								me.transmitData(context, bundle);
*/		
	}
	public boolean isEnabled(final Context context)
	{
		if (super.isEnabled(context))
		{
			SharedPreferences prefs = Probe.getPreferences(context);

			if (prefs.getBoolean("config_probe_mediarouter_enabled", MediaRouterDeviceProbe.DEFAULT_ENABLED))
			{
				long now = System.currentTimeMillis();
				
				synchronized(this)
				{
					long freq = Long.parseLong(prefs.getString("config_probe_mediarouter_frequency", Probe.DEFAULT_FREQUENCY));

					final MediaRouterDeviceProbe me = this;
					
					if (now - this._lastCheck  > freq)
					{
						this._lastCheck = now;
						
						this._handler.post(new Runnable()
						{
							public void run() 
							{
								Log.e("PR", "START DISCOVERY");

								MediaRouter router = MediaRouter.getInstance(context);
								
								MediaRouteSelector.Builder builder = new MediaRouteSelector.Builder();
								builder = builder.addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO);
								builder = builder.addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO);
								builder = builder.addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
								builder = builder.addControlCategory(CastMediaControlIntent.CATEGORY_CAST);
								
								router.addCallback(builder.build(), new MediaRouter.Callback() 
								{
									public void onProviderAdded (MediaRouter router, MediaRouter.ProviderInfo info)
									{
										Log.e("PR", "PROVIDER ADD: " + info + " -- " + info.getPackageName());
									}

									public void onProviderChanged (MediaRouter router, MediaRouter.ProviderInfo info)
									{
										Log.e("PR", "PROVIDER CHANGE: " + info + " -- " + info.getPackageName());
									}

									public void onProviderRemoved (MediaRouter router, MediaRouter.ProviderInfo info)
									{
										Log.e("PR", "PROVIDER REMOVE: " + info + " -- " + info.getPackageName());
									}
									
									public void onRouteAdded (MediaRouter router, MediaRouter.RouteInfo route)
									{
										Log.e("PR", "ROUTE ADD: " + route.getName() + " -- " + route.getPlaybackType() + " -- " + route.getVolume() + "/" + route.getVolumeMax());
									}
									
									public void onRouteChanged (MediaRouter router, MediaRouter.RouteInfo route)
									{
										Log.e("PR", "ROUTE CHANGE: " + route.getName() + " -- " + route.getPlaybackType() + " -- " + route.getVolume() + "/" + route.getVolumeMax());
									}

									public void onRouteRemoved (MediaRouter router, MediaRouter.RouteInfo route)
									{
										Log.e("PR", "ROUTE REMOVE: " + route.getName() + " -- " + route.getPlaybackType() + " -- " + route.getVolume() + "/" + route.getVolumeMax());
									}
								}, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
								
								router.updateSelectedRoute(builder.build());
								
								for (MediaRouter.ProviderInfo info : router.getProviders())
								{
									Log.e("PR", "PROVIDER NAME: " + info + " -- " + info.getPackageName());
									
									for (MediaRouter.RouteInfo route : info.getRoutes())
									{
										Log.e("PR", "PROVIDER ROUTE NAME: " + route.getName() + " -- " + route.getPlaybackType() + " -- " + route.getVolume() + "/" + route.getVolumeMax());
									}
								}

								for (MediaRouter.RouteInfo route : router.getRoutes())
								{
									Log.e("PR", "ROUTE NAME: " + route.getName() + " -- " + route.getPlaybackType() + " -- " + route.getVolume() + "/" + route.getVolumeMax());
								}
								
								MediaRouter.RouteInfo selectedRoute = router.getSelectedRoute();
								MediaRouter.RouteInfo defaultRoute = router.getDefaultRoute();

								Log.e("PR", "END DISCOVERY");
							}						
						});
					}
				}
				
				return true;
			}
		}

		return false;
	}
	
	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_mediarouter_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_mediarouter_enabled", false);
		
		e.commit();
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		return super.summarizeValue(context, bundle);
	}

	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);

		long freq = Long.parseLong(prefs.getString("config_probe_mediarouter_frequency", Probe.DEFAULT_FREQUENCY));
		
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
				
				e.putString("config_probe_mediarouter_frequency", frequency.toString());
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
		screen.setSummary(R.string.summary_mediarouter_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_mediarouter_enabled");
		enabled.setDefaultValue(MediaRouterDeviceProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_mediarouter_frequency");
		duration.setEntryValues(R.array.probe_satellite_frequency_values);
		duration.setEntries(R.array.probe_satellite_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);
		duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

		screen.addPreference(duration);

		return screen;
	}

	public String summary(Context context) 
	{
		return context.getString(R.string.summary_mediarouter_probe_desc);
	}
}
