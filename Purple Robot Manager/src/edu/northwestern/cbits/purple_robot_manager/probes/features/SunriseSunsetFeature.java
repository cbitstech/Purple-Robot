package edu.northwestern.cbits.purple_robot_manager.probes.features;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe;

public class SunriseSunsetFeature extends Feature 
{
	private static final String FEATURE_KEY = "sunrise_sunset";

	protected static final String LONGITUDE = "LONGITUDE";
	protected static final String LATITUDE = "LATITUDE";
	protected static final String SUNRISE_DISTANCE = "SUNRISE_DISTANCE";
	protected static final String SUNSET_DISTANCE = "SUNSET_DISTANCE";
	protected static final String IS_DAY = "IS_DAY";
	protected static final String SUNRISE = "SUNRISE";
	protected static final String SUNSET = "SUNSET";
	protected static final String DAY_DURATION = "DAY_DURATION";

	private boolean _isEnabled = false;
	private boolean _isInited = false;

	public String summarizeValue(Context context, Bundle bundle)
	{
		long now = System.currentTimeMillis();
		
		long sunrise = bundle.getLong(SunriseSunsetFeature.SUNRISE);
		long sunset = bundle.getLong(SunriseSunsetFeature.SUNSET);
		
		long diff = (now - sunset) / (60 * 1000);
		
		int stringId = R.string.summary_after_sunset_probe;
		
		if (now < sunrise)
		{
			stringId = R.string.summary_before_sunrise_probe;			
			diff = (sunrise - now) / (60 * 1000);
		}
		if (now < sunset)
		{
			stringId = R.string.summary_before_sunset_probe;			
			diff = (sunset - now) / (60 * 1000);
		}

		return context.getResources().getString(stringId, diff);
	}

	public String probeCategory(Context context)
	{
		return context.getString(R.string.probe_external_environment_category);
	}

	protected String featureKey() 
	{
		return SunriseSunsetFeature.FEATURE_KEY;
	}

	protected String summary(Context context) 
	{
		return context.getString(R.string.summary_sunrise_sunset_feature_desc);
	}

	public String name(Context context) 
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.features.SunriseSunsetFeature";
	}

	public String title(Context context) 
	{
		return context.getString(R.string.title_sunrise_sunset_feature);
	}

	public void enable(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_feature_sunrise_sunset_enabled", true);
		
		e.commit();
	}

	public void disable(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_feature_sunrise_sunset_enabled", false);
		
		e.commit();
	}
	
	public boolean isEnabled(Context context)
	{
		if (!this._isInited )
		{
			IntentFilter intentFilter = new IntentFilter(Probe.PROBE_READING);

			final SunriseSunsetFeature me = this;

			BroadcastReceiver receiver = new BroadcastReceiver()
			{
				public void onReceive(final Context context, Intent intent)
				{
					Bundle extras = intent.getExtras();

					String probeName = extras.getString("PROBE");

					if (probeName != null && (LocationProbe.NAME.equals(probeName)))
					{
						final double latitude = extras.getDouble(LocationProbe.LATITUDE);
						final double longitude = extras.getDouble(LocationProbe.LONGITUDE);
							
						Runnable r = new Runnable()
						{
							public void run() 
							{
								if (me._isEnabled == false)
									return;

								Calendar c = Calendar.getInstance();
								Location location = new Location("" + latitude, "" + longitude);
								SunriseSunsetCalculator calc = new SunriseSunsetCalculator(location, c.getTimeZone());
								
								Bundle bundle = new Bundle();
								bundle.putString("PROBE", me.name(context));
								bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

								bundle.putDouble(SunriseSunsetFeature.LATITUDE, latitude);
								bundle.putDouble(SunriseSunsetFeature.LONGITUDE, longitude);
								
								Calendar civilSunrise = calc.getCivilSunriseCalendarForDate(c);
								Calendar civilSunset = calc.getCivilSunsetCalendarForDate(c);
								
								long now = c.getTime().getTime();
								
								long sunrise = civilSunrise.getTime().getTime();
								long sunset = civilSunset.getTime().getTime();
								
								bundle.putLong(SunriseSunsetFeature.SUNRISE, sunrise);
								bundle.putLong(SunriseSunsetFeature.SUNSET, sunset);
								bundle.putLong(SunriseSunsetFeature.DAY_DURATION, sunset - sunrise);
								bundle.putLong(SunriseSunsetFeature.SUNRISE_DISTANCE, now - sunrise);
								bundle.putLong(SunriseSunsetFeature.SUNSET_DISTANCE, now - sunset);
								
								boolean isDay = (now >= sunrise && now <= sunset);
								
								bundle.putBoolean(SunriseSunsetFeature.IS_DAY, isDay);

								me.transmitData(context, bundle);
							}
						};
						
						Thread t = new Thread(r);
						t.start();
					}
				}
			};

			LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
			localManager.registerReceiver(receiver, intentFilter);

			this._isInited = true;
		}

		SharedPreferences prefs = Probe.getPreferences(context);

		this._isEnabled = false;

		if (super.isEnabled(context))
		{
			if (prefs.getBoolean("config_feature_sunrise_sunset_enabled", true))
				this._isEnabled = true;
		}

		return this._isEnabled;
	}
}
