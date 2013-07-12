package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class ScreenProbe extends Probe
{
	public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.ScreenProbe";

	public static final String SCREEN_ACTIVE = "SCREEN_ACTIVE";

	private static final boolean DEFAULT_ENABLED = true;

	private boolean _isInited = false;
	private boolean _isEnabled = false;

	public String name(Context context)
	{
		return ScreenProbe.NAME;
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_screen_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public boolean isEnabled(Context context)
	{
		if (!this._isInited)
		{
			IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);

			final ScreenProbe me = this;

			BroadcastReceiver receiver = new BroadcastReceiver()
			{
				public void onReceive(Context context, Intent intent)
				{
					if (me._isEnabled)
					{
						Bundle bundle = new Bundle();
						bundle.putString("PROBE", me.name(context));
						bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

						final String action = intent.getAction();

						if (Intent.ACTION_SCREEN_OFF.equals(action))
							bundle.putBoolean(ScreenProbe.SCREEN_ACTIVE, false);
						else if (Intent.ACTION_SCREEN_ON.equals(action))
							bundle.putBoolean(ScreenProbe.SCREEN_ACTIVE, true);

						me.transmitData(context, bundle);
					}
				}
			};

			context.registerReceiver(receiver, filter);

			this._isInited = true;
		}

		SharedPreferences prefs = Probe.getPreferences(context);

		this._isEnabled = false;

		if (super.isEnabled(context))
		{
			if (prefs.getBoolean("config_probe_screen_enabled", ScreenProbe.DEFAULT_ENABLED))
				this._isEnabled = true;
		}

		return this._isEnabled;
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_screen_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_screen_enabled", false);
		
		e.commit();
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		boolean active = bundle.getBoolean(ScreenProbe.SCREEN_ACTIVE, false);

		if (active)
			return context.getResources().getString(R.string.summary_screen_probe_active);

		return context.getResources().getString(R.string.summary_screen_probe_inactive);
	}

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		boolean active = bundle.getBoolean(ScreenProbe.SCREEN_ACTIVE, false);

		if (active)
			formatted.putString(context.getString(R.string.display_screen_label), context.getString(R.string.display_screen_active_label));
		else
			formatted.putString(context.getString(R.string.display_screen_label), context.getString(R.string.display_screen_inactive_label));
		
		return formatted;
	};


	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_screen_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_screen_enabled");
		enabled.setDefaultValue(ScreenProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		return screen;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO Auto-generated method stub
	}
}
