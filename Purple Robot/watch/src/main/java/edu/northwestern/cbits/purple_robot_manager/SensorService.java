package edu.northwestern.cbits.purple_robot_manager;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Administrator on 3/24/15.
 */
public class SensorService extends IntentService
{
    private static final String LAST_FIRE = "SensorService.LAST_FIRE";
    private static final long INTERVAL = 10000;

    private static final String ACCELEROMETER_ENABLED = "SensorService.ACCELEROMETER_ENABLED";
    private static final boolean ACCELEROMETER_DEFAULT = true;
    private static final String LIGHT_ENABLED = "SensorService.LIGHT_ENABLED";
    private static final boolean LIGHT_DEFAULT = true;
    private static final String MAGNETIC_ENABLED = "SensorService.MAGNETIC_ENABLED";
    private static final boolean MAGNETIC_DEFAULT = false;
    private static final String GYROSCOPE_ENABLED = "SensorService.GYROSCOPE_ENABLED";
    private static final boolean GYROSCOPE_DEFAULT = false;

    private static SensorEventListener accelerometerListener = null;
    private static SensorEventListener lightListener = null;
    private static SensorEventListener magneticListener = null;
    private static SensorEventListener gyroscopeListener = null;

    public SensorService()
    {
        super("SensorService");
    }

    protected void onHandleIntent(Intent intent)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        long now = System.currentTimeMillis();
        long lastFire = prefs.getLong(SensorService.LAST_FIRE, 0);

        if (now - lastFire > SensorService.INTERVAL)
        {
            SensorManager sensors = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

            boolean accelEnabled = prefs.getBoolean(SensorService.ACCELEROMETER_ENABLED, SensorService.ACCELEROMETER_DEFAULT);

            if (accelEnabled)
            {
                SensorService.accelerometerListener = new SensorEventListener()
                {
                    public void onSensorChanged(SensorEvent sensorEvent)
                    {

                    }

                    public void onAccuracyChanged(Sensor sensor, int i)
                    {

                    }
                };

                sensors.registerListener(SensorService.accelerometerListener, sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
            }
            else if (accelerometerListener != null);
            {
                try
                {
                    sensors.unregisterListener(accelerometerListener);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                accelerometerListener = null;
            }

            boolean lightEnabled = prefs.getBoolean(SensorService.LIGHT_ENABLED, SensorService.LIGHT_DEFAULT);

            if (lightEnabled)
            {
                SensorService.lightListener = new SensorEventListener()
                {
                    public void onSensorChanged(SensorEvent sensorEvent)
                    {

                    }

                    public void onAccuracyChanged(Sensor sensor, int i)
                    {

                    }
                };

                sensors.registerListener(SensorService.lightListener, sensors.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_FASTEST);
            }
            else if (lightListener != null);
            {
                try
                {
                    sensors.unregisterListener(lightListener);
                }
                catch (Exception e)
                {
                       e.printStackTrace();
                }

                lightListener = null;
            }

            boolean magneticEnabled = prefs.getBoolean(SensorService.MAGNETIC_ENABLED, SensorService.MAGNETIC_DEFAULT);

            if (magneticEnabled)
            {
                SensorService.magneticListener = new SensorEventListener()
                {
                    public void onSensorChanged(SensorEvent sensorEvent)
                    {

                    }

                    public void onAccuracyChanged(Sensor sensor, int i)
                    {

                    }
                };

                sensors.registerListener(SensorService.magneticListener, sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
            }
            else if (magneticListener != null);
            {
                try
                {
                    sensors.unregisterListener(magneticListener);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                magneticListener = null;
            }

            boolean gyroscopeEnabled = prefs.getBoolean(SensorService.GYROSCOPE_ENABLED, SensorService.GYROSCOPE_DEFAULT);

            if (gyroscopeEnabled)
            {
                SensorService.gyroscopeListener = new SensorEventListener()
                {
                    public void onSensorChanged(SensorEvent sensorEvent)
                    {

                    }

                    public void onAccuracyChanged(Sensor sensor, int i)
                    {

                    }
                };

                sensors.registerListener(SensorService.gyroscopeListener, sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
            }
            else if (gyroscopeListener != null);
            {
                try
                {
                    sensors.unregisterListener(gyroscopeListener);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                gyroscopeListener = null;
            }
        }
    }
}
