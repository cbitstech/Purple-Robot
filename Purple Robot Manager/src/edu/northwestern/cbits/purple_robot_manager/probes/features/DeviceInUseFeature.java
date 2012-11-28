package edu.northwestern.cbits.purple_robot_manager.probes.features;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.CallStateProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ScreenProbe;

public class DeviceInUseFeature extends Feature
{
	protected static final String DEVICE_ACTIVE = "DEVICE_ACTIVE";
	private boolean _isInited = false;
	private boolean _isEnabled = true;

	private boolean _lastXmit = false;

	private boolean _callActive = false;
	private boolean _screenActive = false;

	protected String featureKey()
	{
		return "device_use";
	}

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.features.DeviceInUseFeature";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_device_use_feature);
	}

	public String summary(Context context)
	{
		return context.getString(R.string.summary_device_use_feature_desc);
	}

	public boolean isEnabled(Context context)
	{
		if (!this._isInited)
		{
			IntentFilter intentFilter = new IntentFilter(Probe.PROBE_READING);

			final DeviceInUseFeature me = this;

			BroadcastReceiver receiver = new BroadcastReceiver()
			{
				public void onReceive(Context context, Intent intent)
				{
					Bundle extras = intent.getExtras();

					String probeName = extras.getString("PROBE");

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
					}
				}
			};

			LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
			localManager.registerReceiver(receiver, intentFilter);

			this._isInited = true;
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		 this._isEnabled = false;

		if (super.isEnabled(context))
		{
			if (prefs.getBoolean("config_probe_device_use_enabled", true))
				this._isEnabled = true;
		}

		return this._isEnabled;
	}
}
