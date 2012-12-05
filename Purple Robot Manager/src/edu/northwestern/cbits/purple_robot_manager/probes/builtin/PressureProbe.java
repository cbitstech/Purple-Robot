package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitLandscapeActivity;

public class PressureProbe extends ContinuousProbe implements SensorEventListener
{
	private static int BUFFER_SIZE = 40;

	private static String[] fieldNames = { "PRESSURE", "ALTITUDE" };

	private double lastSeen = 0;
	private long lastFrequencyLookup = 0;
	private long frequency = 1000;

	private float valueBuffer[][] = new float[2][BUFFER_SIZE];
	private int accuracyBuffer[] = new int[BUFFER_SIZE];
	private double timeBuffer[] = new double[BUFFER_SIZE];

	private int bufferIndex  = 0;

	private ArrayList<Double> _pressureCache = new ArrayList<Double>();
	private ArrayList<Double> _altitudeCache = new ArrayList<Double>();

	public Intent viewIntent(Context context)
	{
		Intent i = new Intent(context, WebkitLandscapeActivity.class);

		return i;
	}

	public String contentSubtitle(Context context)
	{
		return String.format(context.getString(R.string.display_item_count), this._pressureCache.size());
	}

	public String getDisplayContent(Activity activity)
	{
		try
		{
			String template = WebkitActivity.stringForAsset(activity, "webkit/highcharts_full.html");

			SplineChart c = new SplineChart();
			c.addSeries("pRESSURE", new ArrayList<Double>(this._pressureCache));

			JSONObject json = c.highchartsJson(activity);

			HashMap<String, Object> scope = new HashMap<String, Object>();
			scope.put("highchart_json", json.toString());

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
		float[] altitudes = bundle.getFloatArray("ALTITUDE");
		float[] pressures = bundle.getFloatArray("PRESSURE");

		ArrayList<String> keys = new ArrayList<String>();

		if (altitudes != null && pressures != null && eventTimes != null)
		{
			SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.display_date_format));

			if (eventTimes.length > 1)
			{
				Bundle readings = new Bundle();

				for (int i = 0; i < eventTimes.length; i++)
				{
					String formatString = String.format(context.getString(R.string.display_pressure_reading), pressures[i], altitudes[i]);

					double time = eventTimes[i];

					Date d = new Date((long) time);

					String key = sdf.format(d);

					readings.putString(key, formatString);

					keys.add(key);
				}

				if (keys.size() > 0)
					readings.putStringArrayList("KEY_ORDER", keys);

				formatted.putBundle(context.getString(R.string.display_pressure_readings), readings);
			}
			else if (eventTimes.length > 0)
			{
				String formatString = String.format(context.getString(R.string.display_pressure_reading), pressures[0], altitudes[0]);

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

			frequency = Long.parseLong(prefs.getString("config_probe_pressure_built_in_frequency", "1000"));

			int bufferSize = 1000 / (int) frequency;

			if (timeBuffer.length != bufferSize)
			{
				if (bufferSize < 1)
					bufferSize = 1;

				synchronized(this)
				{
					bufferIndex = 0;

					valueBuffer = new float[2][bufferSize];
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
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.PressureProbe";
	}

	public int getTitleResource()
	{
		return R.string.title_pressure_probe;
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

        	if (prefs.getBoolean("config_probe_pressure_built_in_enabled", true))
        	{
        		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_FASTEST, null);

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
				double elapsed = SystemClock.elapsedRealtime();
				double boot = (now - elapsed) * 1000 * 1000;

				double timestamp = event.timestamp + boot;

				timeBuffer[bufferIndex] = timestamp / 1000000;
				accuracyBuffer[bufferIndex] = event.accuracy;

				valueBuffer[0][bufferIndex] = event.values[0];

				try
				{
					valueBuffer[1][bufferIndex] = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0]);
				}
				catch (Exception e)
				{
					valueBuffer[1][bufferIndex] = 0;
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

						if (fieldNames[i].equals("PRESSURE"))
						{
							while (this._pressureCache.size() > 128)
								this._pressureCache.remove(0);

							for (int j = 0; j < valueBuffer[i].length; j++)
							{
								this._pressureCache.add(Double.valueOf(valueBuffer[i][j]));
							}
						}
						else if (fieldNames[i].equals("ALTITUDE"))
						{
							while (this._altitudeCache.size() > 128)
								this._altitudeCache.remove(0);

							for (int j = 0; j < valueBuffer[i].length; j++)
							{
								this._altitudeCache.add(Double.valueOf(valueBuffer[i][j]));
							}
						}
					}

					this.transmitData(data);

					bufferIndex = 0;
				}

				this.lastSeen = now;
			}
		}
	}

	public String getPreferenceKey()
	{
		return "pressure_built_in";
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
		float pressure = bundle.getFloatArray("PRESSURE")[0];
		float altitude = bundle.getFloatArray("ALTITUDE")[0];

		return String.format(context.getResources().getString(R.string.summary_pressure_probe), pressure, altitude);
	}

	public int getSummaryResource()
	{
		return R.string.summary_pressure_probe_desc;
	}
}
