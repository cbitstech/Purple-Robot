package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.format.Formatter;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class NetworkProbe extends Probe 
{
	private static final String HOSTNAME = "HOSTNAME";
	private static final String IP_ADDRESS = "IP_ADDRESS";
	private static final String IFACE_NAME = "INTERFACE_NAME";
	private static final String IFACE_DISPLAY_NAME = "INTERFACE_DISPLAY";

	private static final boolean DEFAULT_ENABLED = true;

	private long _lastCheck = 0;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.NetworkProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_network_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_network_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_network_enabled", false);
		
		e.commit();
	}

	public boolean isEnabled(final Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		if (super.isEnabled(context))
		{
			final long now = System.currentTimeMillis();

			if (prefs.getBoolean("config_probe_network_enabled", NetworkProbe.DEFAULT_ENABLED))
			{
				synchronized(this)
				{
					long freq = Long.parseLong(prefs.getString("config_probe_network_frequency", Probe.DEFAULT_FREQUENCY));

					if (now - this._lastCheck  > freq)
					{
						final NetworkProbe me = this;
						
						Runnable r = new Runnable()
						{
							public void run() 
							{
								Bundle bundle = new Bundle();
								bundle.putString("PROBE", me.name(context));
								bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

								WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
								WifiInfo wifiInfo = wifiManager.getConnectionInfo();
						   
								if (wifiInfo != null)
								{
									int ip = wifiInfo.getIpAddress();

									String ipString = Formatter.formatIpAddress(ip);
									
									bundle.putString(NetworkProbe.IP_ADDRESS, ipString);
									
									try 
									{
										bundle.putString(NetworkProbe.HOSTNAME, InetAddress.getByName(ipString).getHostName());
									}
									catch (UnknownHostException e) 
									{
										bundle.putString(NetworkProbe.HOSTNAME, ipString);
									}
								}
								else
								{
									try 
									{
										Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();

										NetworkInterface iface = null;
										
										while ((iface = ifaces.nextElement()) != null && bundle.containsKey(NetworkProbe.IP_ADDRESS) == false)
										{
											if (iface.getName().equals("lo") == false)
											{
												Enumeration<InetAddress> ips = iface.getInetAddresses();
												InetAddress ipAddr = null;
												
												while (ips.hasMoreElements() && (ipAddr = ips.nextElement()) != null)
												{
													bundle.putString(NetworkProbe.IP_ADDRESS, ipAddr.getHostAddress());
													bundle.putString(NetworkProbe.HOSTNAME, ipAddr.getHostName());
												}
											}
										}
									} 
									catch (SocketException e) 
									{
										LogManager.getInstance(context).logException(e);
									}
								}
								
								if (bundle.containsKey(NetworkProbe.IP_ADDRESS) == false)
								{
									bundle.putString(NetworkProbe.IP_ADDRESS, "127.0.0.1");
									bundle.putString(NetworkProbe.HOSTNAME, "localhost");
								}

								try 
								{
									NetworkInterface iface = NetworkInterface.getByInetAddress(InetAddress.getByName("127.0.0.1"));

									bundle.putString(NetworkProbe.IFACE_NAME, iface.getName());
									bundle.putString(NetworkProbe.IFACE_DISPLAY_NAME, iface.getDisplayName());
								}
								catch (SocketException e) 
								{
									LogManager.getInstance(context).logException(e);
								} 
								catch (UnknownHostException e) 
								{
									LogManager.getInstance(context).logException(e);
								}

								me.transmitData(context, bundle);

								me._lastCheck = now;
							}
						};
						
						Thread t = new Thread(r);
						t.start();
					}
				}

				return true;
			}
		}

		return false;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		String ipAddress = bundle.getString(NetworkProbe.IP_ADDRESS);

		return String.format(context.getResources().getString(R.string.summary_network_probe), ipAddress);
	}

	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		long freq = Long.parseLong(prefs.getString("config_probe_network_frequency", Probe.DEFAULT_FREQUENCY));
		
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
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
				Editor e = prefs.edit();
				
				e.putString("config_probe_network_frequency", frequency.toString());
				e.commit();
			}
		}
	}

	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_network_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_network_enabled");
		enabled.setDefaultValue(NetworkProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_network_frequency");
		duration.setEntryValues(R.array.probe_satellite_frequency_values);
		duration.setEntries(R.array.probe_satellite_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);
		duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

		screen.addPreference(duration);

		return screen;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO Auto-generated method stub
	}
}
