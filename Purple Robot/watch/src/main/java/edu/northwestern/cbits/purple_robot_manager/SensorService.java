package edu.northwestern.cbits.purple_robot_manager;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class SensorService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    public static final String SENSOR_MAXIMUM_RANGE = "MAXIMUM_RANGE";
    public static final String SENSOR_NAME = "NAME";
    public static final String SENSOR_POWER = "POWER";
    public static final String SENSOR_TYPE = "TYPE";
    public static final String SENSOR_VENDOR = "VENDOR";
    public static final String SENSOR_VERSION = "VERSION";
    public static final String SENSOR_RESOLUTION = "RESOLUTION";
    public static final String BUNDLE_SENSOR = "SENSOR";
    public static final String SENSOR_ACCURACY = "ACCURACY";

    public static final String EVENT_TIMESTAMP = "EVENT_TIMESTAMP";
    public static final String SENSOR_TIMESTAMP = "SENSOR_TIMESTAMP";

    protected static final String BUNDLE_PROBE = "PROBE";
    protected static final String BUNDLE_TIMESTAMP = "TIMESTAMP";

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

    private static final String URI_READING_PREFIX = "/purple-robot-reading";

    private static SensorEventListener accelerometerListener = null;
    private static SensorEventListener lightListener = null;
    private static SensorEventListener magneticListener = null;
    private static SensorEventListener gyroscopeListener = null;

    private static GoogleApiClient _apiClient = null;

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
            final SensorService me = this;

            SensorManager sensors = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

            boolean accelEnabled = prefs.getBoolean(SensorService.ACCELEROMETER_ENABLED, SensorService.ACCELEROMETER_DEFAULT);

            Log.e("PW", "ACCEL: " + accelEnabled + " -- " + SensorService.accelerometerListener);

            if (accelEnabled && SensorService.accelerometerListener == null)
            {
                Log.e("PW", "SETTING UP ACCEL LISTENER");
                SensorService.accelerometerListener = new SensorEventListener()
                {
                    public void onSensorChanged(SensorEvent sensorEvent)
                    {
                        AccelerometerHandler.handleSensorEvent(me, sensorEvent);
                    }

                    public void onAccuracyChanged(Sensor sensor, int i)
                    {

                    }
                };

                sensors.registerListener(SensorService.accelerometerListener, sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

                Log.e("PW", "DONE SETTING UP ACCEL LISTENER: " + SensorService.accelerometerListener);
            }
            else if (accelEnabled == false && SensorService.accelerometerListener != null)
            {
                Log.e("PW", "TEARING DOWN ACCEL LISTENER");

                try
                {
                    sensors.unregisterListener(SensorService.accelerometerListener);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                SensorService.accelerometerListener = null;

                Log.e("PW", "DONE TEARING DOWN ACCEL LISTENER");
            }

/*            boolean lightEnabled = prefs.getBoolean(SensorService.LIGHT_ENABLED, SensorService.LIGHT_DEFAULT);

            if (lightEnabled && lightListener == null)
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
            else if (lightEnabled == false && lightListener != null);
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

            if (magneticEnabled &&  magneticListener == null)
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
            else if (magneticEnabled == false && magneticListener != null);
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

            if (gyroscopeEnabled && gyroscopeListener == null)
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
            else if (gyroscopeEnabled == false && gyroscopeListener != null);
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
*/
        }

        if (SensorService._apiClient == null)
        {
            GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this);
            builder.addApi(Wearable.API);
            builder.addConnectionCallbacks(this);

            builder.addOnConnectionFailedListener(this);
            SensorService._apiClient = builder.build();

            SensorService._apiClient.connect();
        }
    }

    public static void transmitData(String source, DataMap data)
    {
        Log.e("PW", "IN XMIT");
        if (SensorService._apiClient.isConnected())
        {
            Log.e("PW", "XMITTING");

            PutDataMapRequest putDataMapReq = PutDataMapRequest.create(SensorService.URI_READING_PREFIX + "/" + source + "/" + System.currentTimeMillis());
            putDataMapReq.getDataMap().putAll(data);

            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();

            Wearable.DataApi.putDataItem(SensorService._apiClient, putDataReq);
        }
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Log.e("PW", "CONNECTED");
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.e("PW", "CONNECTION SUSPENDED");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        Log.e("PW", "CONNECTION FAILED");
    }
}
