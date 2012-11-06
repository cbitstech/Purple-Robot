package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;

public class ContinuousAccelerometerProbe extends ContinuousProbe implements SensorEventListener
{
	private static long SENSOR_DELAY = 1000; // ms
	private long lastSeen = 0;

	public String name(Context context)
	{
		return context.getString(R.string.title_builtin_accelerometer_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public boolean isEnabled(Context context)
	{
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        sensors.unregisterListener(this);

        sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST, null);

		return super.isEnabled(context);
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{

	}

	@SuppressLint("NewApi")
	public void onSensorChanged(SensorEvent event)
	{
		long now = System.currentTimeMillis();

		Log.e("PRM", now + " - " + this.lastSeen + " (" + (now - this.lastSeen) + ") > " + SENSOR_DELAY + " "); //  + event.sensor.getName());

		if (now - this.lastSeen > SENSOR_DELAY)
		{
			this.lastSeen = now;

			long elapsed = SystemClock.elapsedRealtime();

			long boot = (now - elapsed) * 1000 * 1000;

			long timestamp = event.timestamp + boot;

			Log.e("PRM", "GOT SENSOR READING FOR " + event.sensor.getName());
			Log.e("PRM", "GOT TIMESTAMP: " + timestamp);
			Log.e("PRM", "GOT ACCURACY: " + event.accuracy);
			Log.e("PRM", "GOT VALUE: " + event.values);

			for (int i = 0; i < event.values.length; i++)
			{
				Log.e("PRM", "GOT VECTOR: " +  event.values[i] + " " + i);
			}
		}
	}
}
