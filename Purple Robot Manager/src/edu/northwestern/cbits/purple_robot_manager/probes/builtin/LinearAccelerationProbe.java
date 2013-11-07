package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitLandscapeActivity;
import edu.northwestern.cbits.purple_robot_manager.charts.SplineChart;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

@SuppressLint("SimpleDateFormat")
public class LinearAccelerationProbe extends ContinuousProbe implements SensorEventListener
{
	private static int BUFFER_SIZE = 1024;

	public static final String DB_TABLE = "linear_acceleration_probe";

	private static final String X_KEY = "X";
	private static final String Y_KEY = "Y";
	private static final String Z_KEY = "Z";
	
	private static final String[] fieldNames = { X_KEY, Y_KEY, Z_KEY };

	private static final String DEFAULT_THRESHOLD = "0.5";

	public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.LinearAccelerationProbe";

	private long lastThresholdLookup = 0;
	private double lastThreshold = 0.5;

	private double _lastX = Double.MAX_VALUE;
	private double _lastY = Double.MAX_VALUE;
	private double _lastZ = Double.MAX_VALUE;
	
	private float valueBuffer[][] = new float[3][BUFFER_SIZE];
	private int accuracyBuffer[] = new int[BUFFER_SIZE];
	private double timeBuffer[] = new double[BUFFER_SIZE];

	private Map<String, String> _schema = null;
	
	private int _lastFrequency = -1;

	private int bufferIndex  = 0;

	public Intent viewIntent(Context context)
	{
		Intent i = new Intent(context, WebkitLandscapeActivity.class);

		return i;
	}

	public String probeCategory(Context context)
	{
		return context.getString(R.string.probe_sensor_category);
	}

	public String contentSubtitle(Context context)
	{
		Cursor c = ProbeValuesProvider.getProvider(context).retrieveValues(context, LinearAccelerationProbe.DB_TABLE, this.databaseSchema());

		int count = -1;

		if (c != null)
		{
			count = c.getCount();
			c.close();
		}

		return String.format(context.getString(R.string.display_item_count), count);
	}

	public Map<String, String> databaseSchema()
	{
		if (this._schema == null)
		{
			this._schema = new HashMap<String, String>();

			this._schema.put(LinearAccelerationProbe.X_KEY, ProbeValuesProvider.REAL_TYPE);
			this._schema.put(LinearAccelerationProbe.Y_KEY, ProbeValuesProvider.REAL_TYPE);
			this._schema.put(LinearAccelerationProbe.Z_KEY, ProbeValuesProvider.REAL_TYPE);
		}

		return this._schema;
	}

	public String getDisplayContent(Activity activity)
	{
		try
		{
			String template = WebkitActivity.stringForAsset(activity, "webkit/highcharts_full.html");

			ArrayList<Double> x = new ArrayList<Double>();
			ArrayList<Double> y = new ArrayList<Double>();
			ArrayList<Double> z = new ArrayList<Double>();
			ArrayList<Double> time = new ArrayList<Double>();

			Cursor cursor = ProbeValuesProvider.getProvider(activity).retrieveValues(activity, LinearAccelerationProbe.DB_TABLE, this.databaseSchema());

			int count = -1;

			if (cursor != null)
			{
				count = cursor.getCount();

				while (cursor.moveToNext())
				{
					double xd = cursor.getDouble(cursor.getColumnIndex(LinearAccelerationProbe.X_KEY));
					double yd = cursor.getDouble(cursor.getColumnIndex(LinearAccelerationProbe.Y_KEY));
					double zd = cursor.getDouble(cursor.getColumnIndex(LinearAccelerationProbe.Z_KEY));

					double t = cursor.getDouble(cursor.getColumnIndex(ProbeValuesProvider.TIMESTAMP));

					x.add(xd);
					y.add(yd);
					z.add(zd);
					time.add(t);
				}

				cursor.close();
			}

			SplineChart c = new SplineChart();
			c.addSeries("X", x);
			c.addSeries("Y", y);
			c.addSeries("Z", z);

			c.addTime("tIME", time);

			JSONObject json = c.highchartsJson(activity);

			template = template.replace("{{{ highchart_json }}}", json.toString());
			template = template.replace("{{{ highchart_count }}}", "" + count);

		    return template;
		}
		catch (IOException e)
		{
			LogManager.getInstance(activity).logException(e);
		}
		catch (JSONException e)
		{
			LogManager.getInstance(activity).logException(e);
		}

		return null;
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

		if (x == null || y == null || z == null)
		{

		}
		else if (eventTimes.length > 1)
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


			formatted.putBundle(context.getString(R.string.display_linear_acceleration_readings), readings);
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

		return Long.parseLong(prefs.getString("config_probe_linear_acceleration_built_in_frequency", ContinuousProbe.DEFAULT_FREQUENCY));
	}

	public String name(Context context)
	{
		return LinearAccelerationProbe.NAME;
	}

	public int getTitleResource()
	{
		return R.string.title_linear_acceleration_probe;
	}

	@SuppressLint("InlinedApi")
	public boolean isEnabled(Context context)
	{
		if (Build.VERSION.SDK_INT < 9)
			return false;
		
    	SharedPreferences prefs = ContinuousProbe.getPreferences(context);

    	this._context = context.getApplicationContext();

    	SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        if (super.isEnabled(context))
        {
        	if (prefs.getBoolean("config_probe_linear_acceleration_built_in_enabled", ContinuousProbe.DEFAULT_ENABLED))
        	{
				int frequency = Integer.parseInt(prefs.getString("config_probe_linear_acceleration_built_in_frequency", ContinuousProbe.DEFAULT_FREQUENCY));
				
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

			this.lastThreshold = Double.parseDouble(prefs.getString("config_probe_linear_acceleration_threshold", LinearAccelerationProbe.DEFAULT_THRESHOLD));
			
			this.lastThresholdLookup = now;
		}

		double x = event.values[0];
		double y = event.values[1];
		double z = event.values[2];

		boolean passes = false;
		
		if (Math.abs(x - this._lastX) > this.lastThreshold)
			passes = true;
		else if (Math.abs(y - this._lastY) > this.lastThreshold)
			passes = true;
		else if (Math.abs(z - this._lastZ) > this.lastThreshold)
			passes = true;
		
		if (passes)
		{
			this._lastX = x;
			this._lastY = y;
			this._lastZ = z;
		}
		
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
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putString("config_probe_linear_acceleration_threshold", threshold.toString());
				e.commit();
			}
		}
	}

	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceScreen screen = super.preferenceScreen(activity);

		ListPreference threshold = new ListPreference(activity);
		threshold.setKey("config_probe_linear_acceleration_threshold");
		threshold.setDefaultValue(LinearAccelerationProbe.DEFAULT_THRESHOLD);
		threshold.setEntryValues(R.array.probe_accelerometer_threshold);
		threshold.setEntries(R.array.probe_accelerometer_threshold_labels);
		threshold.setTitle(R.string.probe_noise_threshold_label);
		threshold.setSummary(R.string.probe_noise_threshold_summary);

		screen.addPreference(threshold);

		return screen;
	}
	
	public void onSensorChanged(SensorEvent event)
	{
		final double now = (double) System.currentTimeMillis();
		
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

					data.putDouble("TIMESTAMP", now / 1000);

					data.putString("PROBE", this.name(this._context));

					data.putBundle("SENSOR", sensorBundle);

					data.putDoubleArray("EVENT_TIMESTAMP", timeBuffer);
					data.putIntArray("ACCURACY", accuracyBuffer);

					for (int i = 0; i < fieldNames.length; i++)
					{
						data.putFloatArray(fieldNames[i], valueBuffer[i]);
					}

					this.transmitData(data);

					for (int j = 0; j < timeBuffer.length; j++)
					{
						Double x = null;
						Double y = null;
						Double z = null;

						for (int i = 0; i < fieldNames.length; i++)
						{
							if (fieldNames[i].equals(LinearAccelerationProbe.X_KEY))
								x = Double.valueOf(valueBuffer[i][j]);
							else if (fieldNames[i].equals(LinearAccelerationProbe.Y_KEY))
								y = Double.valueOf(valueBuffer[i][j]);
							else if (fieldNames[i].equals(LinearAccelerationProbe.Z_KEY))
								z = Double.valueOf(valueBuffer[i][j]);
						}

						if (x != null && y != null && z != null)
						{
							Map<String, Object> values = new HashMap<String, Object>();

							values.put(LinearAccelerationProbe.X_KEY, x);
							values.put(LinearAccelerationProbe.Y_KEY, y);
							values.put(LinearAccelerationProbe.Z_KEY, z);

							values.put(ProbeValuesProvider.TIMESTAMP, Double.valueOf(timeBuffer[j] / 1000));

							ProbeValuesProvider.getProvider(this._context).insertValue(this._context, LinearAccelerationProbe.DB_TABLE, this.databaseSchema(), values);
						}
					}

					bufferIndex = 0;
				}
			}
		}
	}

	public String getPreferenceKey()
	{
		return "linear_acceleration_built_in";
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		double xReading = bundle.getDoubleArray("X")[0];
		double yReading = bundle.getDoubleArray("Y")[0];
		double zReading = bundle.getDoubleArray("Z")[0];

		return String.format(context.getResources().getString(R.string.summary_linear_acceleration_probe), xReading, yReading, zReading);
	}

	public int getSummaryResource()
	{
		return R.string.summary_linear_acceleration_probe_desc;
	}
}
