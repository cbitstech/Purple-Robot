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
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitLandscapeActivity;
import edu.northwestern.cbits.purple_robot_manager.charts.SplineChart;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;

public class LightProbe extends ContinuousProbe implements SensorEventListener
{
	public static final String DB_TABLE = "light_probe";

	private static final String LIGHT_KEY = "LUX";

	private static int BUFFER_SIZE = 512;

	private static String[] fieldNames = { LIGHT_KEY };

	private double lastSeen = 0;
	private long lastFrequencyLookup = 0;
	private long frequency = 1000;

	private float valueBuffer[][] = new float[1][BUFFER_SIZE];
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
		Cursor c = ProbeValuesProvider.getProvider(context).retrieveValues(LightProbe.DB_TABLE, this.databaseSchema());

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

			this._schema.put(LightProbe.LIGHT_KEY, ProbeValuesProvider.REAL_TYPE);
		}

		return this._schema;
	}

	public String getDisplayContent(Activity activity)
	{
		try
		{
			String template = WebkitActivity.stringForAsset(activity, "webkit/highcharts_full.html");

			ArrayList<Double> light = new ArrayList<Double>();
			ArrayList<Double> time = new ArrayList<Double>();

			Cursor cursor = ProbeValuesProvider.getProvider(activity).retrieveValues(LightProbe.DB_TABLE, this.databaseSchema());

			int count = -1;

			if (cursor != null)
			{
				count = cursor.getCount();

				while (cursor.moveToNext())
				{
					double d = cursor.getDouble(cursor.getColumnIndex(LightProbe.LIGHT_KEY));
					double t = cursor.getDouble(cursor.getColumnIndex(ProbeValuesProvider.TIMESTAMP));

					light.add(d);
					time.add(t);
				}

				cursor.close();
			}


			SplineChart c = new SplineChart();
			c.addSeries("lIGHT", light);
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
		float[] lux = bundle.getFloatArray("LUX");

		ArrayList<String> keys = new ArrayList<String>();

		if (lux != null && eventTimes != null)
		{
			SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.display_date_format));

			if (eventTimes.length > 1)
			{
				Bundle readings = new Bundle();

				for (int i = 0; i < eventTimes.length; i++)
				{
					String formatString = String.format(context.getString(R.string.display_light_reading), lux[i]);

					double time = eventTimes[i];

					Date d = new Date((long) time);

					String key = sdf.format(d);

					readings.putString(key, formatString);

					keys.add(key);
				}

				if (keys.size() > 0)
					readings.putStringArrayList("KEY_ORDER", keys);

				formatted.putBundle(context.getString(R.string.display_light_readings), readings);
			}
			else if (eventTimes.length > 0)
			{
				String formatString = String.format(context.getString(R.string.display_light_reading), lux[0]);

				double time = eventTimes[0];

				Date d = new Date((long) time);

				formatted.putString(sdf.format(d), formatString);
			}
		}

		return formatted;
	};

	public long getFrequency()
	{
		long now = System.currentTimeMillis();

		if (now - this.lastFrequencyLookup > 5000 && this._context != null)
		{
			SharedPreferences prefs = ContinuousProbe.getPreferences(this._context);

			frequency = Long.parseLong(prefs.getString("config_probe_light_built_in_frequency", "1000"));

			int bufferSize = 1000 / (int) frequency;

			if (timeBuffer.length != bufferSize)
			{
				if (bufferSize < 1)
					bufferSize = 1;

				synchronized(this)
				{
					bufferIndex = 0;

					valueBuffer = new float[1][bufferSize];
					timeBuffer = new double[bufferSize];
				}
			}

			this.lastFrequencyLookup = now;
		}

		return frequency;
	}

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.LightProbe";
	}

	public int getTitleResource()
	{
		return R.string.title_light_probe;
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

			if (prefs.getBoolean("config_probe_light_built_in_enabled", false))
			{
				sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_FASTEST, null);

				return true;
			}
        }

		return false;
	}

	@SuppressLint("NewApi")
	public void onSensorChanged(SensorEvent event)
	{
		double now = System.currentTimeMillis();

		if (now - this.lastSeen > this.getFrequency() && bufferIndex <= timeBuffer.length)
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

					for (int j = 0; j < timeBuffer.length; j++)
					{
						Double light = null;

						for (int i = 0; i < fieldNames.length; i++)
						{
							if (fieldNames[i].equals(LightProbe.LIGHT_KEY))
								light = Double.valueOf(valueBuffer[i][j]);
						}

						if (light != null)
						{
							Map<String, Object> values = new HashMap<String, Object>();

							values.put(LightProbe.LIGHT_KEY, light);

							values.put(ProbeValuesProvider.TIMESTAMP, Double.valueOf(timeBuffer[j] / 1000));

							ProbeValuesProvider.getProvider(this._context).insertValue(LightProbe.DB_TABLE, this.databaseSchema(), values);
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
		return "light_built_in";
	}

	public int getResourceFrequencyLabels()
	{
		return R.array.probe_builtin_frequency_labels;
	}

	public int getResourceFrequencyValues()
	{
		return R.array.probe_builtin_frequency_values;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		float lux = bundle.getFloatArray("LUX")[0];

		return String.format(context.getResources().getString(R.string.summary_light_probe), lux);
	}

	public int getSummaryResource()
	{
		return R.string.summary_light_probe_desc;
	}
}
