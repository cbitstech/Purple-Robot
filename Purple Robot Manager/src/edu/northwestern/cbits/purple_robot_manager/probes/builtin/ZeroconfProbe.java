package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.IOException;
import java.util.ArrayList;

import javax.jmdns.JmDNS;
import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class ZeroconfProbe extends Probe implements ServiceListener
{
	private static final String SERVICES = null;
	private static final String SERVICES_COUNT = null;

	private boolean _inited = false;
	
	private JmmDNS jmdns = null;
	private MulticastLock _lock = null;

    public void serviceResolved(ServiceEvent ev) 
    {
        Log.e("PRM", "Service resolved: "
                 + ev.getInfo().getQualifiedName()
                 + " port:" + ev.getInfo().getPort());
    }
    
    public void serviceRemoved(ServiceEvent ev) 
    {
    	Log.e("PRM", "Service removed: " + ev.getName());
    }
    
    public void serviceAdded(ServiceEvent event) 
    {
        // Required to force serviceResolved to be called again
        // (after the first search)
        jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
    }

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.ZeroconfProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_zeroconf_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_zeroconf_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_zeroconf_enabled", false);
		
		e.commit();
	}

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		boolean enabled = super.isEnabled(context);

		Log.e("PR", "1");
		
		if (enabled)
			enabled = prefs.getBoolean("config_probe_zeroconf_enabled", true);

		Log.e("PR", "2");

		if (enabled)
		{
//			long freq = Long.parseLong(prefs.getString("config_probe_zeroconf_frequency", Probe.DEFAULT_FREQUENCY));

			Log.e("PR", "3");
			
			if (this._inited == false)
			{
				Log.e("PR", "4");
				this.jmdns = JmmDNS.Factory.getInstance();
				WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				
				this._lock = wifi.createMulticastLock("ZeroconfProbeLock");
				this._lock.setReferenceCounted(true);
				this._lock.acquire();

			    jmdns.addServiceListener("_services._dns-sd._udp.", this);
			    
			    this._inited = true;
				Log.e("PR", "5");
			}
			else
			{
				Log.e("PR", "6");
				jmdns.removeServiceListener("_workstation._tcp.local.", this);
				
				this._lock.release();
				this._lock = null;
				
				this._inited = false;

				Log.e("PR", "7");
			}
				
			Log.e("PR", "8");
			
			return true;
		}
		else
		{

		}

		return false;
	}
	
	public String summarizeValue(Context context, Bundle bundle)
	{
		int count = bundle.getInt(ZeroconfProbe.SERVICES_COUNT);

		return String.format(context.getResources().getString(R.string.summary_bluetooth_probe), count);
	}

/*	private Bundle bundleForDevicesArray(Context context, ArrayList<Bundle> objects)
	{
		Bundle bundle = new Bundle();

		for (Bundle value : objects)
		{
			ArrayList<String> keys = new ArrayList<String>();

			String key = String.format(context.getString(R.string.display_bluetooth_device_title), value.getString(ZeroconfProbe.NAME), value.getString(ZeroconfProbe.ADDRESS));

			Bundle deviceBundle = new Bundle();

			deviceBundle.putString(context.getString(R.string.display_bluetooth_device_pair), value.getString(ZeroconfProbe.BOND_STATE));
			deviceBundle.putString(context.getString(R.string.display_bluetooth_device_major), value.getString(ZeroconfProbe.MAJOR_CLASS));
			deviceBundle.putString(context.getString(R.string.display_bluetooth_device_minor), value.getString(ZeroconfProbe.MINOR_CLASS));

			keys.add(context.getString(R.string.display_bluetooth_device_pair));
			keys.add(context.getString(R.string.display_bluetooth_device_major));
			keys.add(context.getString(R.string.display_bluetooth_device_minor));

			deviceBundle.putStringArrayList("KEY_ORDER", keys);

			bundle.putBundle(key, deviceBundle);
		}

		return bundle;
	} */

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		@SuppressWarnings("unchecked")
		ArrayList<Bundle> array = (ArrayList<Bundle>) bundle.get(ZeroconfProbe.SERVICES);
		int count = bundle.getInt(ZeroconfProbe.SERVICES_COUNT);

//		Bundle devicesBundle = this.bundleForDevicesArray(context, array);
//
//		formatted.putBundle(String.format(context.getString(R.string.summary_zeroconf_probe), count), devicesBundle);

		return formatted;
	};

	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_zeroconf_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_zeroconf_enabled");
		enabled.setDefaultValue(true);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_zeroconf_frequency");
		duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);
		duration.setEntryValues(R.array.probe_satellite_frequency_values);
		duration.setEntries(R.array.probe_satellite_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);

		screen.addPreference(duration);

		return screen;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO Auto-generated method stub
	}
}
