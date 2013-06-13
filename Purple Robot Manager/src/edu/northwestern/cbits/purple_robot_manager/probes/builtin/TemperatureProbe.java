package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import edu.northwestern.cbits.purple_robot_manager.R;

@SuppressLint("SimpleDateFormat")
public class TemperatureProbe extends ContinuousProbe implements SensorEventListener
{
	private static final String DEFAULT_THRESHOLD = "1.0";

	private static int BUFFER_SIZE = 256;

	private static String[] fieldNames = { "TEMPERATURE" };

	private double _lastValue = Double.MAX_VALUE;

	private long lastThresholdLookup = 0;
	private double lastThreshold = 1.0;

	private float valueBuffer[][] = new float[1][BUFFER_SIZE];
	private double timeBuffer[] = new double[BUFFER_SIZE];

	private int bufferIndex  = 0;

	private int _lastFrequency = -1;

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		double[] eventTimes = bundle.getDoubleArray("EVENT_TIMESTAMP");
		float[] temp = bundle.getFloatArray("TEMPERATURE");

		ArrayList<String> keys = new ArrayList<String>();

		if (temp != null && eventTimes != null)
		{
			SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.display_date_format));

			if (eventTimes.length > 1)
			{
				Bundle readings = new Bundle();

				for (int i = 0; i < eventTimes.length; i++)
				{
					String formatString = String.format(context.getString(R.string.display_temperature_reading), temp[i]);

					double time = eventTimes[i];

					Date d = new Date((long) time);

					String key = sdf.format(d);

					readings.putString(key, formatString);

					keys.add(key);
				}

				if (keys.size() > 0)
					readings.putStringArrayList("KEY_ORDER", keys);

				formatted.putBundle(context.getString(R.string.display_temperature_readings), readings);
			}
			else if (eventTimes.length > 0)
			{
				String formatString = String.format(context.getString(R.string.display_temperature_reading), temp[0]);

				double time = eventTimes[0];

				Date d = new Date((long) time);

				formatted.putString(sdf.format(d), formatString);
			}
		}

		return formatted;
	};

	public long getFrequency()
	{
		SharedPreferences prefs = ContinuousProbe.getPreferences(this._context);

		return Long.parseLong(prefs.getString("config_probe_temperature_built_in_frequency", ContinuousProbe.DEFAULT_FREQUENCY));
	}

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.TemperatureProbe";
	}

	public int getTitleResource()
	{
		return R.string.title_temperature_probe;
	}

	public int getCategoryResource()
	{
		return R.string.probe_environment_category;
	}

	public boolean isEnabled(Context context)
	{
    	SharedPreferences prefs = ContinuousProbe.getPreferences(context);

    	this._context = context.getApplicationContext();
    	
        if (super.isEnabled(context))
        {
        	if (prefs.getBoolean("config_probe_temperature_built_in_enabled", ContinuousProbe.DEFAULT_ENABLED))
        	{
            	SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

				Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_TEMPERATURE);

				int frequency = Integer.parseInt(prefs.getString("config_probe_temperature_built_in_frequency", ContinuousProbe.DEFAULT_FREQUENCY));

				if (this._lastFrequency  != frequency)
				{
	                sensors.unregisterListener(this);
	                
	                switch (frequency)
	                {
	                	case SensorManager.SENSOR_DELAY_FASTEST:
		                	sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST, null);
	                		break;
	                	case SensorManager.SENSOR_DELAY_GAME:
		                	sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME, null);
	                		break;
	                	case SensorManager.SENSOR_DELAY_UI:
		                	sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, null);
	                		break;
	                	case SensorManager.SENSOR_DELAY_NORMAL:
		                	sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL, null);
	                		break;
	                }
	                
	                this._lastFrequency = frequency;
				}
				
				return true;
        	}
        	else
        	{
            	SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                sensors.unregisterListener(this);

                this._lastFrequency = -1;
        	}
        }
    	else
    	{
        	SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            sensors.unregisterListener(this);

            this._lastFrequency = -1;
    	}

        return false;
	}

	protected boolean passesThreshold(SensorEvent event)
	{
		long now = System.currentTimeMillis();
		
		if (now - this.lastThresholdLookup > 5000)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
			this.lastThreshold = Double.parseDouble(prefs.getString("config_probe_temperature_threshold", TemperatureProbe.DEFAULT_THRESHOLD));
			
			this.lastThresholdLookup = now;
		}

		double value = event.values[0];

		boolean passes = false;

		if (Math.abs(value - this._lastValue) > this.lastThreshold)
			passes = true;
		
		if (passes)
			this._lastValue = value;
		
		return passes;
	}

	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		map.put(ContinuousProbe.PROBE_THRESHOLD, this.lastThreshold);
		
		return map;
	}
	
	public void updateFromMap(Context context, Map<String, Object> params) 
	{
		super.updateFromMap(context, params);
		
		if (params.containsKey(ContinuousProbe.PROBE_THRESHOLD))
		{
			Object threshold = params.get(ContinuousProbe.PROBE_THRESHOLD);
			
			if (threshold instanceof Double)
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
				Editor e = prefs.edit();
				
				e.putString("config_probe_temperature_threshold", threshold.toString());
				e.commit();
			}
		}
	}

	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceScreen screen = super.preferenceScreen(activity);

		ListPreference threshold = new ListPreference(activity);
		threshold.setKey("config_probe_temperature_threshold");
		threshold.setDefaultValue(TemperatureProbe.DEFAULT_THRESHOLD);
		threshold.setEntryValues(R.array.probe_temperature_threshold);
		threshold.setEntries(R.array.probe_temperature_threshold_labels);
		threshold.setTitle(R.string.probe_noise_threshold_label);
		threshold.setSummary(R.string.probe_noise_threshold_summary);

		screen.addPreference(threshold);

		return screen;
	}

	@SuppressLint("NewApi")
	public void onSensorChanged(SensorEvent event)
	{
		double now = System.currentTimeMillis();

		if (this.passesThreshold(event))
		{
			synchronized(this)
			{
				double elapsed = SystemClock.uptimeMillis();
				double boot = (now - elapsed) * 1000 * 1000;

				double timestamp = event.timestamp + boot;

				timeBuffer[bufferIndex] = timestamp / 1000000;
				valueBuffer[0][bufferIndex] = event.values[0];

				bufferIndex += 1;

				if (bufferIndex >= timeBuffer.length)
				{
					Sensor sensor = event.sensor;

					Bundle data = new Bundle();

					Bundle sensorBundle = new Bundle();
					sensorBundle.putFloat("MAXIMUM_RANGE", sensor.getMaximumRange());
					sensorBundle.putString("NAME", sensor.getName());
					sensorBundle.putFloat("POWER", sensor.getPower());
					sensorBundle.putFloat("RESOLUTION", sensor.getResolution());
					sensorBundle.putInt("TYPE", sensor.getType());
					sensorBundle.putString("VENDOR", sensor.getVendor());
					sensorBundle.putInt("VERSION", sensor.getVersion());

					data.putString("PROBE", this.name(this._context));

					data.putBundle("SENSOR", sensorBundle);
					data.putDouble("TIMESTAMP", now / 1000);

					data.putDoubleArray("EVENT_TIMESTAMP", timeBuffer);

					for (int i = 0; i < fieldNames.length; i++)
					{
						data.putFloatArray(fieldNames[i], valueBuffer[i]);
					}

					this.transmitData(data);

					bufferIndex = 0;
				}
			}
		}
	}

	public String getPreferenceKey()
	{
		return "temperature_built_in";
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		float lux = bundle.getFloatArray("TEMPERATURE")[0];

		return String.format(context.getResources().getString(R.string.summary_temperature_probe), lux);
	}

	public int getSummaryResource()
	{
		return R.string.summary_temperature_probe_desc;
	}
}
