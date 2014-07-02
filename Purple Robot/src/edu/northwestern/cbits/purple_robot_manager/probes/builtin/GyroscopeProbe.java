package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.RealTimeProbeViewActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitLandscapeActivity;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

@SuppressLint("SimpleDateFormat")
public class GyroscopeProbe extends Continuous3DProbe implements SensorEventListener
{
	private static int BUFFER_SIZE = 1024;

	public static final String DB_TABLE = "gyroscope_probe";

	private static final String DEFAULT_THRESHOLD = "0.0025";

	public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.GyroscopeProbe";

	private static String X_KEY = "X";
	private static String Y_KEY = "Y";
	private static String Z_KEY = "Z";

	private static String[] fieldNames = { X_KEY, Y_KEY, Z_KEY };

	private double _lastX = Double.MAX_VALUE;
	private double _lastY = Double.MAX_VALUE;
	private double _lastZ = Double.MAX_VALUE;

	private long lastThresholdLookup = 0;
	private double lastThreshold = 0.0025;

	private float valueBuffer[][] = new float[3][BUFFER_SIZE];
	private int accuracyBuffer[] = new int[BUFFER_SIZE];
	private double timeBuffer[] = new double[BUFFER_SIZE];

	private Map<String, String> _schema = null;

	private int bufferIndex  = 0;

	private int _lastFrequency = -1;

	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		map.put(ContinuousProbe.PROBE_THRESHOLD, this.lastThreshold);
		
		return map;
	}
	
	public String probeCategory(Context context)
	{
		return context.getString(R.string.probe_sensor_category);
	}

	public void updateFromMap(Context context, Map<String, Object> params) 
	{
		super.updateFromMap(context, params);
		
		if (params.containsKey(ContinuousProbe.PROBE_THRESHOLD))
		{
			Object threshold = params.get(ContinuousProbe.PROBE_THRESHOLD);
			
			if (threshold instanceof Double)
			{
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putString("config_probe_gyroscope_threshold", threshold.toString());
				e.commit();
			}
		}
	}

	public Map<String, String> databaseSchema()
	{
		if (this._schema == null)
		{
			this._schema = new HashMap<String, String>();

			this._schema.put(GyroscopeProbe.X_KEY, ProbeValuesProvider.REAL_TYPE);
			this._schema.put(GyroscopeProbe.Y_KEY, ProbeValuesProvider.REAL_TYPE);
			this._schema.put(GyroscopeProbe.Z_KEY, ProbeValuesProvider.REAL_TYPE);
		}

		return this._schema;
	}

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		double[] eventTimes = bundle.getDoubleArray("EVENT_TIMESTAMP");
		double[] x = bundle.getDoubleArray("X");
		double[] y = bundle.getDoubleArray("Y");
		double[] z = bundle.getDoubleArray("Z");

		ArrayList<String> keys = new ArrayList<String>();

		SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.display_date_format));

		if (eventTimes.length > 1)
		{
			Bundle readings = new Bundle();

			for (int i = 0; i < eventTimes.length; i++)
			{
				String formatString = String.format(context.getString(R.string.display_gyroscope_reading), x[i], y[i], z[i]);

				double time = eventTimes[i];

				Date d = new Date((long) time);

				String key = sdf.format(d);

				readings.putString(key, formatString);

				keys.add(key);
			}

			if (keys.size() > 0)
				readings.putStringArrayList("KEY_ORDER", keys);

			formatted.putBundle(context.getString(R.string.display_gyroscope_readings), readings);
		}
		else if (eventTimes.length > 0)
		{
			String formatString = String.format(context.getString(R.string.display_gyroscope_reading), x[0], y[0], z[0]);

			double time = eventTimes[0];

			Date d = new Date((long) time);

			formatted.putString(sdf.format(d), formatString);
		}

		return formatted;
	};

	public long getFrequency()
	{
		SharedPreferences prefs = ContinuousProbe.getPreferences(this._context);
		
		return Long.parseLong(prefs.getString("config_probe_gyroscope_built_in_frequency", ContinuousProbe.DEFAULT_FREQUENCY));
	}

	public String name(Context context)
	{
		return GyroscopeProbe.NAME;
	}

	public int getTitleResource()
	{
		return R.string.title_gyroscope_probe;
	}

	public boolean isEnabled(Context context)
	{
    	SharedPreferences prefs = ContinuousProbe.getPreferences(context);

    	this._context = context.getApplicationContext();

    	SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if (super.isEnabled(context))
        {
        	if (prefs.getBoolean("config_probe_gyroscope_built_in_enabled", ContinuousProbe.DEFAULT_ENABLED))
        	{
				int frequency = Integer.parseInt(prefs.getString("config_probe_gyroscope_built_in_frequency", ContinuousProbe.DEFAULT_FREQUENCY));

				if (this._lastFrequency != frequency)
				{
					sensors.unregisterListener(this, sensor);
	                
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
	                	default:
		                	sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME, null);
	                		break;
	                }
	                
	                this._lastFrequency = frequency;
				}
				
				return true;
        	}
        	else
        	{
                sensors.unregisterListener(this, sensor);
                this._lastFrequency = -1;
        	}
        }
    	else
    	{
            sensors.unregisterListener(this, sensor);
            this._lastFrequency = -1;
    	}

        return false;

	}
	
	protected boolean passesThreshold(SensorEvent event)
	{
		long now = System.currentTimeMillis();
		
		if (now - this.lastThresholdLookup > 5000)
		{
			SharedPreferences prefs = Probe.getPreferences(this._context);
			this.lastThreshold = Double.parseDouble(prefs.getString("config_probe_gyroscope_threshold", GyroscopeProbe.DEFAULT_THRESHOLD));
			
			this.lastThresholdLookup = now;
		}

		double x = event.values[0];
		double y = event.values[1];
		double z = event.values[2];

		boolean passes = false;

		if (Math.abs(x - this._lastX) >= this.lastThreshold)
			passes = true;
		else if (Math.abs(y - this._lastY) >= this.lastThreshold)
			passes = true;
		else if (Math.abs(z - this._lastZ) >= this.lastThreshold)
			passes = true;
		
		if (passes)
		{
			this._lastX = x;
			this._lastY = y;
			this._lastZ = z;
		}
		
		return passes;
	}

	public void onSensorChanged(SensorEvent event)
	{
		if (this.shouldProcessEvent(event) == false)
			return;

		double now = (double) System.currentTimeMillis();

		if (this.passesThreshold(event))
		{
			synchronized(this)
			{
				double elapsed = (double) SystemClock.uptimeMillis();
				double boot = (now - elapsed) * 1000 * 1000;

				double timestamp = event.timestamp + boot;

				if (timestamp > now * (1000 * 1000) * 1.1) // Used to detect if sensors already have built-in times...
					timestamp = event.timestamp;

				timeBuffer[bufferIndex] = timestamp / 1000000;
				accuracyBuffer[bufferIndex] = event.accuracy;

				for (int i = 0; i < event.values.length; i++)
				{
					valueBuffer[i][bufferIndex] = event.values[i];
				}

				double[] plotValues = {timeBuffer[0] / 1000, valueBuffer[0][bufferIndex], valueBuffer[1][bufferIndex], valueBuffer[2][bufferIndex] }; 
				RealTimeProbeViewActivity.plotIfVisible(this.getTitleResource(), plotValues);

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
					data.putIntArray("ACCURACY", accuracyBuffer);

					for (int i = 0; i < fieldNames.length; i++)
					{
						data.putFloatArray(fieldNames[i], valueBuffer[i]);
					}

					this.transmitData(this._context, data);

					if (timeBuffer.length > 0)
					{
						double x = Double.NaN;
						double y = Double.NaN;
						double z = Double.NaN;

						for (int i = 0; i < fieldNames.length; i++)
						{
							if (fieldNames[i].equals(GyroscopeProbe.X_KEY))
								x = valueBuffer[i][0];
							else if (fieldNames[i].equals(GyroscopeProbe.Y_KEY))
								y = valueBuffer[i][0];
							else if (fieldNames[i].equals(GyroscopeProbe.Z_KEY))
								z = valueBuffer[i][0];
						}

						if (Double.isNaN(x) == false && Double.isNaN(y) == false && Double.isNaN(z) == false)
						{
							Map<String, Object> values = new HashMap<String, Object>(4);

							values.put(GyroscopeProbe.X_KEY, x);
							values.put(GyroscopeProbe.Y_KEY, y);
							values.put(GyroscopeProbe.Z_KEY, z);

							values.put(ProbeValuesProvider.TIMESTAMP, Double.valueOf(timeBuffer[0] / 1000));

							ProbeValuesProvider.getProvider(this._context).insertValue(this._context, GyroscopeProbe.DB_TABLE, this.databaseSchema(), values);
						}
					}

					bufferIndex = 0;
				}
			}
		}
	}
	
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceScreen screen = super.preferenceScreen(activity);

		ListPreference threshold = new ListPreference(activity);
		threshold.setKey("config_probe_gyroscope_threshold");
		threshold.setDefaultValue(GyroscopeProbe.DEFAULT_THRESHOLD);
		threshold.setEntryValues(R.array.probe_gyroscope_threshold);
		threshold.setEntries(R.array.probe_gyroscope_threshold_labels);
		threshold.setTitle(R.string.probe_noise_threshold_label);
		threshold.setSummary(R.string.probe_noise_threshold_summary);

		screen.addPreference(threshold);

		return screen;
	}

	public String getPreferenceKey()
	{
		return "gyroscope_built_in";
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		double xReading = bundle.getDoubleArray("X")[0];
		double yReading = bundle.getDoubleArray("Y")[0];
		double zReading = bundle.getDoubleArray("Z")[0];

		return String.format(context.getResources().getString(R.string.summary_gyroscope_probe), xReading, yReading, zReading);
	}

	public int getSummaryResource()
	{
		return R.string.summary_gyroscope_probe_desc;
	}

	protected String tableName() 
	{
		return GyroscopeProbe.DB_TABLE;
	}
}
