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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitLandscapeActivity;
import edu.northwestern.cbits.purple_robot_manager.charts.SplineChart;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;

@SuppressLint("SimpleDateFormat")
public class AccelerometerProbe extends ContinuousProbe implements SensorEventListener
{
	private static int BUFFER_SIZE = 512;

	public static final String DB_TABLE = "accelerometer_probe";

	private static final String X_KEY = "X";
	private static final String Y_KEY = "Y";
	private static final String Z_KEY = "Z";
	
	private static final String[] fieldNames = { X_KEY, Y_KEY, Z_KEY };

	private double lastSeen = 0;
	private long lastFrequencyLookup = 0;
	private long frequency = 1000;

	private long lastThresholdLookup = 0;
	private double lastThreshold = 0;

	private double _lastX = Double.MAX_VALUE;
	private double _lastY = Double.MAX_VALUE;
	private double _lastZ = Double.MAX_VALUE;
	
	private float valueBuffer[][] = new float[3][BUFFER_SIZE];
	private int accuracyBuffer[] = new int[BUFFER_SIZE];
	private double timeBuffer[] = new double[BUFFER_SIZE];

	private Map<String, String> _schema = null;

	private int bufferIndex  = 0;

	public Intent viewIntent(Context context)
	{
		Intent i = new Intent(context, WebkitLandscapeActivity.class);

		return i;
	}

	public String contentSubtitle(Context context)
	{
		Cursor c = ProbeValuesProvider.getProvider(context).retrieveValues(AccelerometerProbe.DB_TABLE, this.databaseSchema());

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

			this._schema.put(AccelerometerProbe.X_KEY, ProbeValuesProvider.REAL_TYPE);
			this._schema.put(AccelerometerProbe.Y_KEY, ProbeValuesProvider.REAL_TYPE);
			this._schema.put(AccelerometerProbe.Z_KEY, ProbeValuesProvider.REAL_TYPE);
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

			Cursor cursor = ProbeValuesProvider.getProvider(activity).retrieveValues(AccelerometerProbe.DB_TABLE, this.databaseSchema());

			int count = -1;

			if (cursor != null)
			{
				count = cursor.getCount();

				while (cursor.moveToNext())
				{
					double xd = cursor.getDouble(cursor.getColumnIndex(AccelerometerProbe.X_KEY));
					double yd = cursor.getDouble(cursor.getColumnIndex(AccelerometerProbe.Y_KEY));
					double zd = cursor.getDouble(cursor.getColumnIndex(AccelerometerProbe.Z_KEY));

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
			e.printStackTrace();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
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


			formatted.putBundle(context.getString(R.string.display_accelerometer_readings), readings);
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
		long now = System.currentTimeMillis();

		if (now - this.lastFrequencyLookup > 5000 && this._context != null)
		{
			SharedPreferences prefs = ContinuousProbe.getPreferences(this._context);

			frequency = Long.parseLong(prefs.getString("config_probe_accelerometer_built_in_frequency", "1000"));

			int bufferSize = 1000 / (int) frequency;

			if (timeBuffer.length != bufferSize)
			{
				if (bufferSize < 1)
					bufferSize = 1;

				synchronized(this)
				{
					bufferIndex = 0;

					valueBuffer = new float[3][bufferSize];
					accuracyBuffer = new int[bufferSize];
					timeBuffer = new double[bufferSize];
				}
			}

			this.lastFrequencyLookup = now;
		}

		return frequency;
	}

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.AccelerometerProbe";
	}

	public int getTitleResource()
	{
		return R.string.title_accelerometer_probe;
	}

	public int getCategoryResource()
	{
		return R.string.probe_environment_category;
	}

	public boolean isEnabled(Context context)
	{
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        sensors.unregisterListener(this);

        if (super.isEnabled(context))
        {
        	this._context = context.getApplicationContext();

        	SharedPreferences prefs = ContinuousProbe.getPreferences(context);

        	if (prefs.getBoolean("config_probe_accelerometer_built_in_enabled", false))
        	{
				Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				
				if (sensor != null)
					sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST, null);

				return true;
        	}
        }

        return false;
	}

	protected boolean passesThreshold(SensorEvent event)
	{
		long now = System.currentTimeMillis();
		
		if (now - this.lastThresholdLookup > 5000)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
			this.lastThreshold = Double.parseDouble(prefs.getString("config_probe_accelerometer_threshold", "0.5"));
			
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
	
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceScreen screen = super.preferenceScreen(activity);

		ListPreference threshold = new ListPreference(activity);
		threshold.setKey("config_probe_accelerometer_threshold");
		threshold.setDefaultValue("0.5");
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
		
		if (now - this.lastSeen > this.getFrequency() && bufferIndex <= timeBuffer.length && this.passesThreshold(event))
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
							if (fieldNames[i].equals(AccelerometerProbe.X_KEY))
								x = Double.valueOf(valueBuffer[i][j]);
							else if (fieldNames[i].equals(AccelerometerProbe.Y_KEY))
								y = Double.valueOf(valueBuffer[i][j]);
							else if (fieldNames[i].equals(AccelerometerProbe.Z_KEY))
								z = Double.valueOf(valueBuffer[i][j]);
						}

						if (x != null && y != null && z != null)
						{
							Map<String, Object> values = new HashMap<String, Object>();

							values.put(AccelerometerProbe.X_KEY, x);
							values.put(AccelerometerProbe.Y_KEY, y);
							values.put(AccelerometerProbe.Z_KEY, z);

							values.put(ProbeValuesProvider.TIMESTAMP, Double.valueOf(timeBuffer[j] / 1000));

							ProbeValuesProvider.getProvider(this._context).insertValue(AccelerometerProbe.DB_TABLE, this.databaseSchema(), values);
						}
					}

					bufferIndex = 0;
				}

				this.lastSeen = now;
			}
		}
	}

	public String getPreferenceKey()
	{
		return "accelerometer_built_in";
	}

	public int getResourceFrequencyLabels()
	{
		return R.array.probe_acceleration_frequency_labels;
	}

	public int getResourceFrequencyValues()
	{
		return R.array.probe_acceleration_frequency_values;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		float xReading = bundle.getFloatArray("X")[0];
		float yReading = bundle.getFloatArray("Y")[0];
		float zReading = bundle.getFloatArray("Z")[0];

		return String.format(context.getResources().getString(R.string.summary_accelerator_probe), xReading, yReading, zReading);
	}

	public int getSummaryResource()
	{
		return R.string.summary_accelerometer_probe_desc;
	}
}
