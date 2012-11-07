package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import edu.northwestern.cbits.purple_robot_manager.R;

public class ContinuousPressureProbe extends ContinuousProbe implements SensorEventListener
{
	public String name(Context context)
	{
		return context.getString(R.string.title_builtin_pressure_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public boolean isEnabled(Context context)
	{
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        sensors.unregisterListener(this);

        sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_PRESSURE), 500000, null);

		return super.isEnabled(context);
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{

	}

	public void onSensorChanged(SensorEvent event)
	{
		long now = System.currentTimeMillis();
		long elapsed = SystemClock.elapsedRealtime();

//		long boot = (now - elapsed) * 1000 * 1000;

//		long timestamp = event.timestamp + boot;

//		Log.e("PRM", "GOT SENSOR READING FOR " + event.sensor.getName());
//		Log.e("PRM", "GOT TIMESTAMP: " + timestamp);
//		Log.e("PRM", "GOT ACCURACY: " + event.accuracy);
//		Log.e("PRM", "GOT VALUE: " + event.values);

//		for (int i = 0; i < event.values.length; i++)
//		{
//			if (event.values[i] != 0.0)
//				Log.e("PRM", "GOT ALTITUDE: " + SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[i]) + " (" +  event.values[i] + ")");
//		}
	}
}
