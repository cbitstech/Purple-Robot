package edu.northwestern.cbits.purple_robot_manager.probes.features;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe;

public class GooglePlacesFeature extends Feature 
{
	private boolean _isEnabled = false;
	private BroadcastReceiver _receiver = null;

	protected String featureKey() 
	{
		return "google_places";
	}

	protected String summary(Context context) 
	{
		return "tOdO: SuMmArY";
	}

	public String name(Context context) 
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.features.GooglePlacesFeature";
	}

	public String title(Context context) 
	{
		return "gOoGlE pLaCeS";
	}

	public void enable(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_google_places_enabled", true);
		e.commit();
		
		this._isEnabled = true;
		
		this.isEnabled(context);

	}

	public void disable(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_google_places_enabled", false);
		e.commit();
		
		this._isEnabled = false;
		
		if (this._receiver != null)
		{
			LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
			localManager.unregisterReceiver(this._receiver);
		}
	}

	public boolean isEnabled(Context context)
	{
		if (super.isEnabled(context))
		{
			SharedPreferences prefs = Probe.getPreferences(context);
			
			if (prefs.getBoolean("config_probe_google_places_enabled", true))
				this._isEnabled = true;
		}

		if (this._isEnabled && this._receiver == null)
		{
			IntentFilter intentFilter = new IntentFilter(Probe.PROBE_READING);

			final GooglePlacesFeature me = this;
			
			this._receiver = new BroadcastReceiver()
			{
				public void onReceive(Context context, Intent intent)
				{
					Bundle extras = intent.getExtras();

					String probeName = extras.getString("PROBE");
					
					if (probeName != null && (LocationProbe.NAME.equals(probeName)))
					{
						Log.e("PR", "GOOGLE PLACES GOT " + extras);

					/*
					if (probeName != null && (ScreenProbe.NAME.equals(probeName) || CallStateProbe.NAME.equals(probeName)))
					{
						boolean xmit = false;

						if (ScreenProbe.NAME.equals(probeName))
							me._screenActive = extras.getBoolean(ScreenProbe.SCREEN_ACTIVE);
						else if (CallStateProbe.NAME.equals(probeName))
						{
							String state = extras.getString(CallStateProbe.CALL_STATE);

							me._callActive = CallStateProbe.STATE_OFF_HOOK.equals(state);
						}

						xmit = me._callActive || me._screenActive;

						if (me._lastXmit != xmit)
						{
							if (me._isEnabled)
							{
								Bundle bundle = new Bundle();
								bundle.putString("PROBE", me.name(context));
								bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

								bundle.putBoolean(DeviceInUseFeature.DEVICE_ACTIVE, xmit);

								me.transmitData(context, bundle);
							}

							me._lastXmit = xmit;
						}
					} */
					}
				}
			};

			LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
			localManager.registerReceiver(this._receiver, intentFilter);
		}

		return this._isEnabled;
	}
}
