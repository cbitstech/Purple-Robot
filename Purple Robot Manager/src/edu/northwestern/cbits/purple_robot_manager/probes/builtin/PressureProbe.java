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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebkitLandscapeActivity;
import edu.northwestern.cbits.purple_robot_manager.charts.SplineChart;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

@SuppressLint("SimpleDateFormat")
public class PressureProbe extends ContinuousProbe implements SensorEventListener
{
	public static final String DB_TABLE = "pressure_probe";

	private static final String DEFAULT_THRESHOLD = "0.5";

	private static int BUFFER_SIZE = 1024;

	private static String PRESSURE_KEY = "PRESSURE";
	private static String ALTITUDE_KEY = "ALTITUDE";

	private static String[] fieldNames = { PRESSURE_KEY, ALTITUDE_KEY };

	private double _lastValue = Double.MAX_VALUE;

	private long lastThresholdLookup = 0;
	private double lastThreshold = 0.5;

	private float valueBuffer[][] = new float[2][BUFFER_SIZE];
	private int accuracyBuffer[] = new int[BUFFER_SIZE];
	private double timeBuffer[] = new double[BUFFER_SIZE];

	private int bufferIndex  = 0;

	private Map<String, String> _schema = null;

	private int _lastFrequency = -1;

	public Intent viewIntent(Context context)
	{
		Intent i = new Intent(context, WebkitLandscapeActivity.class);

		return i;
	}

	public String contentSubtitle(Context context)
	{
		Cursor c = ProbeValuesProvider.getProvider(context).retrieveValues(context, PressureProbe.DB_TABLE, this.databaseSchema());

		int count = -1;

		if (c != null)
		{
			count = c.getCount();
			c.close();
		}

		return String.format(context.getString(R.string.display_item_count), count);
	}

	public String getDisplayContent(Activity activity)
	{
		try
		{
			String template = WebkitActivity.stringForAsset(activity, "webkit/highcharts_full.html");

			SplineChart c = new SplineChart();

			ArrayList<Double> pressure = new ArrayList<Double>();
			ArrayList<Double> time = new ArrayList<Double>();

			Cursor cursor = ProbeValuesProvider.getProvider(activity).retrieveValues(activity, PressureProbe.DB_TABLE, this.databaseSchema());

			int count = -1;

			if (cursor != null)
			{
				count = cursor.getCount();

				while (cursor.moveToNext())
				{
					double d = cursor.getDouble(cursor.getColumnIndex(PressureProbe.PRESSURE_KEY));
					double t = cursor.getDouble(cursor.getColumnIndex(ProbeValuesProvider.TIMESTAMP));

					pressure.add(d);
					time.add(t);
				}

				cursor.close();
			}

			c.addSeries(activity.getString(R.string.pressure_label), pressure);
			c.addTime(activity.getString(R.string.pressure_time_label), time);

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
		float[] altitudes = bundle.getFloatArray(ALTITUDE_KEY);
		float[] pressures = bundle.getFloatArray(PRESSURE_KEY);

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
		SharedPreferences prefs = ContinuousProbe.getPreferences(this._context);
		return Long.parseLong(prefs.getString("config_probe_pressure_built_in_frequency", ContinuousProbe.DEFAULT_FREQUENCY));
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
    	SharedPreferences prefs = ContinuousProbe.getPreferences(context);

    	this._context = context.getApplicationContext();
    	
        if (super.isEnabled(context))
        {
        	if (prefs.getBoolean("config_probe_pressure_built_in_enabled", ContinuousProbe.DEFAULT_ENABLED))
        	{
            	SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

				Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_PRESSURE);

				int frequency = Integer.parseInt(prefs.getString("config_probe_pressure_built_in_frequency", ContinuousProbe.DEFAULT_FREQUENCY));

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
				
				e.putString("config_probe_pressure_threshold", threshold.toString());
				e.commit();
			}
		}
	}

	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceScreen screen = super.preferenceScreen(activity);

		ListPreference threshold = new ListPreference(activity);
		threshold.setKey("config_probe_pressure_threshold");
		threshold.setDefaultValue(PressureProbe.DEFAULT_THRESHOLD);
		threshold.setEntryValues(R.array.probe_pressure_threshold);
		threshold.setEntries(R.array.probe_pressure_threshold_labels);
		threshold.setTitle(R.string.probe_noise_threshold_label);
		threshold.setSummary(R.string.probe_noise_threshold_summary);

		screen.addPreference(threshold);

		return screen;
	}

	protected boolean passesThreshold(SensorEvent event)
	{
		long now = System.currentTimeMillis();
		
		if (now - this.lastThresholdLookup > 5000)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
			this.lastThreshold = Double.parseDouble(prefs.getString("config_probe_pressure_threshold", PressureProbe.DEFAULT_THRESHOLD));
			
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
					}

					this.transmitData(data);

					for (int j = 0; j < timeBuffer.length; j++)
					{
						Double pressure = null;
						Double altitude = null;

						for (int i = 0; i < fieldNames.length; i++)
						{
							if (fieldNames[i].equals(PressureProbe.PRESSURE_KEY))
								pressure = Double.valueOf(valueBuffer[i][j]);
							else if (fieldNames[i].equals(PressureProbe.ALTITUDE_KEY))
								altitude = Double.valueOf(valueBuffer[i][j]);
						}

						if (pressure != null && altitude != null)
						{
							Map<String, Object> values = new HashMap<String, Object>();

							values.put(PressureProbe.PRESSURE_KEY, pressure);
							values.put(PressureProbe.ALTITUDE_KEY, altitude);

							values.put(ProbeValuesProvider.TIMESTAMP, Double.valueOf(timeBuffer[j] / 1000));

							ProbeValuesProvider.getProvider(this._context).insertValue(this._context, PressureProbe.DB_TABLE, this.databaseSchema(), values);
						}
					}

					bufferIndex = 0;
				}
			}
		}
	}

	public Map<String, String> databaseSchema()
	{
		if (this._schema == null)
		{
			this._schema = new HashMap<String, String>();

			this._schema.put(PressureProbe.PRESSURE_KEY, ProbeValuesProvider.REAL_TYPE);
			this._schema.put(PressureProbe.ALTITUDE_KEY, ProbeValuesProvider.REAL_TYPE);
		}

		return this._schema;
	}

	public String getPreferenceKey()
	{
		return "pressure_built_in";
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		float pressure = bundle.getFloatArray(PressureProbe.PRESSURE_KEY)[0];
		float altitude = bundle.getFloatArray(PressureProbe.ALTITUDE_KEY)[0];

		return String.format(context.getResources().getString(R.string.summary_pressure_probe), pressure, altitude);
	}

	public int getSummaryResource()
	{
		return R.string.summary_pressure_probe_desc;
	}
}
