package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

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
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitLandscapeActivity;
import edu.northwestern.cbits.purple_robot_manager.charts.SplineChart;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

@SuppressLint("SimpleDateFormat")
public class GyroscopeProbe extends ContinuousProbe implements SensorEventListener
{
	private static int BUFFER_SIZE = 1024;

	public static final String DB_TABLE = "gyroscope_probe";

	private static final String DEFAULT_THRESHOLD = "0.0025";

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

	public Intent viewIntent(Context context)
	{
		Intent i = new Intent(context, WebkitLandscapeActivity.class);

		return i;
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
				
				e.putString("config_probe_gyroscope_threshold", threshold.toString());
				e.commit();
			}
		}
	}

	public String contentSubtitle(Context context)
	{
		Cursor c = ProbeValuesProvider.getProvider(context).retrieveValues(context, GyroscopeProbe.DB_TABLE, this.databaseSchema());

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

			this._schema.put(GyroscopeProbe.X_KEY, ProbeValuesProvider.REAL_TYPE);
			this._schema.put(GyroscopeProbe.Y_KEY, ProbeValuesProvider.REAL_TYPE);
			this._schema.put(GyroscopeProbe.Z_KEY, ProbeValuesProvider.REAL_TYPE);
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

			Cursor cursor = ProbeValuesProvider.getProvider(activity).retrieveValues(activity, GyroscopeProbe.DB_TABLE, this.databaseSchema());

			int count = -1;

			if (cursor != null)
			{
				count = cursor.getCount();

				while (cursor.moveToNext())
				{
					double xd = cursor.getDouble(cursor.getColumnIndex(GyroscopeProbe.X_KEY));
					double yd = cursor.getDouble(cursor.getColumnIndex(GyroscopeProbe.Y_KEY));
					double zd = cursor.getDouble(cursor.getColumnIndex(GyroscopeProbe.Z_KEY));

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

			HashMap<String, Object> scope = new HashMap<String, Object>();
			scope.put("highchart_json", json.toString());
			scope.put("highchart_count", count);

			StringWriter writer = new StringWriter();

		    MustacheFactory mf = new DefaultMustacheFactory();
		    Mustache mustache = mf.compile(new StringReader(template), "template");
		    mustache.execute(writer, scope);
		    writer.flush();

		    return writer.toString();

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
		float[] x = bundle.getFloatArray("X");
		float[] y = bundle.getFloatArray("Y");
		float[] z = bundle.getFloatArray("Z");

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
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.GyroscopeProbe";
	}

	public int getTitleResource()
	{
		return R.string.title_gyroscope_probe;
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
        	if (prefs.getBoolean("config_probe_gyroscope_built_in_enabled", ContinuousProbe.DEFAULT_ENABLED))
        	{
            	SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

				Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

				int frequency = Integer.parseInt(prefs.getString("config_probe_gyroscope_built_in_frequency", ContinuousProbe.DEFAULT_FREQUENCY));

				if (this._lastFrequency != frequency)
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
			this.lastThreshold = Double.parseDouble(prefs.getString("config_probe_gyroscope_threshold", GyroscopeProbe.DEFAULT_THRESHOLD));
			
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

	public void onSensorChanged(SensorEvent event)
	{
		double now = (double) System.currentTimeMillis();

		if (this.passesThreshold(event))
		{
			synchronized(this)
			{
				double elapsed = (double) SystemClock.uptimeMillis();
				double boot = (now - elapsed) * 1000 * 1000;

				double timestamp = event.timestamp + boot;

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

					data.putString("PROBE", this.name(this._context));

					data.putBundle("SENSOR", sensorBundle);
					data.putDouble("TIMESTAMP", now / 1000);

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
							if (fieldNames[i].equals(GyroscopeProbe.X_KEY))
								x = Double.valueOf(valueBuffer[i][j]);
							else if (fieldNames[i].equals(GyroscopeProbe.Y_KEY))
								y = Double.valueOf(valueBuffer[i][j]);
							else if (fieldNames[i].equals(GyroscopeProbe.Z_KEY))
								z = Double.valueOf(valueBuffer[i][j]);
						}

						if (x != null && y != null && z != null)
						{
							Map<String, Object> values = new HashMap<String, Object>();

							values.put(GyroscopeProbe.X_KEY, x);
							values.put(GyroscopeProbe.Y_KEY, y);
							values.put(GyroscopeProbe.Z_KEY, z);

							values.put(ProbeValuesProvider.TIMESTAMP, Double.valueOf(timeBuffer[j] / 1000));

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
		float xReading = bundle.getFloatArray("X")[0];
		float yReading = bundle.getFloatArray("Y")[0];
		float zReading = bundle.getFloatArray("Z")[0];

		return String.format(context.getResources().getString(R.string.summary_gyroscope_probe), xReading, yReading, zReading);
	}

	public int getSummaryResource()
	{
		return R.string.summary_gyroscope_probe_desc;
	}
}
