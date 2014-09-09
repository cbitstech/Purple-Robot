package edu.northwestern.cbits.purple_robot_manager.probes.features;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ContinuousProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.sample.SampleProbe;

public class P20FeaturesProbe extends Probe implements SensorEventListener 
{
	public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.features.P20FeaturesProbe";

	private static final String ENABLE = "config_probe_p20_enabled";
	private static final boolean DEFAULT_ENABLED = false;

	private int _lastFrequency = -1;
	private Context _context = null;

	public String name(Context context) 
	{
		return P20FeaturesProbe.NAME;
	}

	public String title(Context context) 
	{
		// TODO: Pull out into strings.xml
		
		return "P20 Features Probe";
	}

	public String probeCategory(Context context) 
	{
		return context.getString(R.string.probe_misc_category);
	}

	public PreferenceScreen preferenceScreen(PreferenceActivity activity) 
	{
		@SuppressWarnings("deprecation")
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(this.summary(activity));

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey(P20FeaturesProbe.ENABLE);
		enabled.setDefaultValue(P20FeaturesProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		return screen;
	}
	
	@Override
	public String summary(Context context) 
	{
		// TODO
		return "TODO";
	}

	public void enable(Context context) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean(P20FeaturesProbe.ENABLE, true);
		e.commit();
	}

	public void disable(Context context) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean(P20FeaturesProbe.ENABLE, false);
		e.commit();
	}

	public boolean isEnabled(final Context context)
	{
		if (this._context == null)
			this._context = context.getApplicationContext();
		
		boolean enabled = super.isEnabled(context);

		SharedPreferences prefs = Probe.getPreferences(context);

		if (enabled)
			enabled = prefs.getBoolean(P20FeaturesProbe.ENABLE, P20FeaturesProbe.DEFAULT_ENABLED);
		
    	SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

    	Sensor accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Sensor gyroscope = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

		if (enabled)
		{
			int frequency = SensorManager.SENSOR_DELAY_GAME; // TODO: Set in preferences!
	
			if (this._lastFrequency != frequency)
			{
				sensors.unregisterListener(this, accelerometer);
				sensors.unregisterListener(this, gyroscope);
	            
	            switch (frequency)
	            {
	            	case SensorManager.SENSOR_DELAY_FASTEST:
	                	sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST, null);
	                	sensors.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST, null);
	            		break;
	            	case SensorManager.SENSOR_DELAY_GAME:
	                	sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, null);
	                	sensors.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME, null);
	            		break;
	            	case SensorManager.SENSOR_DELAY_UI:
	                	sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI, null);
	                	sensors.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI, null);
	            		break;
	            	case SensorManager.SENSOR_DELAY_NORMAL:
	                	sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL, null);
	                	sensors.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL, null);
	            		break;
	            	default:
	                	sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, null);
	                	sensors.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME, null);
	            		break;
	            }
	            
	            this._lastFrequency = frequency;
			}
				
			return true;
        }
    	else
    	{
    		sensors.unregisterListener(this, accelerometer);
    		sensors.unregisterListener(this, gyroscope);

    		this._lastFrequency = -1;
    	}

		
		return false;
	}

	public void onAccuracyChanged(Sensor arg0, int arg1) 
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void onSensorChanged(SensorEvent event) 
	{
		// Sohrob - configure this!

		Sensor sensor = event.sensor;
		
		final float[] values = event.values; // values.length = 3: X, Y, Z
		final long timestamp = event.timestamp;
		
		switch(sensor.getType())
		{
			case Sensor.TYPE_ACCELEROMETER:
				// Do accelerometer stuff...
				
				break;
			case Sensor.TYPE_GYROSCOPE:
				// Do gyro stuff...
				break;
		}
		
		// Save values somewhere...
		
		// When enough data collected & analyzed...
		
		// this.transmitAnalysis();
	}
	
	public void transmitAnalysis()
	{
		Bundle bundle = new Bundle();
		bundle.putString("PROBE", this.name(this._context)); // Required
		bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000); // Required
		
		bundle.putDouble("MY_FEATURE_A", 1.23456); 		// define your own keys and values 
		bundle.putLong("MY_FEATURE_B", Long.MAX_VALUE); // for the models & data collection

		this.transmitData(this._context, bundle);
	}
}
