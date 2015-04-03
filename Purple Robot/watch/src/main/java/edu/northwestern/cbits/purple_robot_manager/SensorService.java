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
import android.util.LongSparseArray;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

public class SensorService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    private static final String PATH_REQUEST_DATA = "/purple-robot/request-data";
    private static final String PATH_SEND_CONFIG = "/purple-robot/send-config";

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
    private static final String BUNDLE_SOURCE = "SOURCE";

    private static final String ACCELEROMETER_ENABLED = "SensorService.ACCELEROMETER_ENABLED";
    private static final boolean ACCELEROMETER_DEFAULT_ENABLED = true;
    private static final String ACCELEROMETER_FREQUENCY = "SensorService.ACCELEROMETER_FREQUENCY";

    private static final String LIGHT_METER_ENABLED = "SensorService.LIGHT_METER_ENABLED";
    private static final boolean LIGHT_METER_DEFAULT_ENABLED = false;
    private static final String LIGHT_METER_FREQUENCY = "SensorService.LIGHT_METER_FREQUENCY";

    private static final String MAGNETOMETER_ENABLED = "SensorService.MAGNETOMETER_ENABLED";
    private static final boolean MAGNETOMETER_DEFAULT_ENABLED = false;
    private static final String MAGNETOMETER_FREQUENCY = "SensorService.MAGNETOMETER_FREQUENCY";

    private static final String GYROSCOPE_ENABLED = "SensorService.GYROSCOPE_ENABLED";
    private static final boolean GYROSCOPE_DEFAULT_ENABLED = false;
    private static final String GYROSCOPE_FREQUENCY = "SensorService.GYROSCOPE_FREQUENCY";

    private static final String HEART_METER_ENABLED = "SensorService.HEART_METER_ENABLED";
    private static final boolean HEART_METER_DEFAULT_ENABLED = false;
    private static final String HEART_METER_FREQUENCY = "SensorService.HEART_METER_FREQUENCY";

    private static final int DEFAULT_FREQUENCY = SensorManager.SENSOR_DELAY_NORMAL;

    private static final String URI_READING_PREFIX = "/purple-robot-reading";

    private static SensorEventListener accelerometerListener = null;
    private static SensorEventListener magnetometerListener = null;
    private static SensorEventListener gyroscopeListener = null;
    private static SensorEventListener lightListener = null;
    private static SensorEventListener heartListener = null;

    private static GoogleApiClient _apiClient = null;
    private static LongSparseArray<DataMap> _payloads = new LongSparseArray<DataMap>();
    private static boolean _isTransmitting = false;

    private int _lastAccelRate = -1;
    private int _lastGyroRate = -1;
    private int _lastMagnetRate = -1;
    private int _lastLightRate = -1;
    private int _lastHeartRate = -1;

    public SensorService()
    {
        super("SensorService");
    }

    public void onCreate()
    {
        super.onCreate();

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

    private void setupAccelerometer(final Context context, SensorManager sensors)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean enabled = prefs.getBoolean(SensorService.ACCELEROMETER_ENABLED, SensorService.ACCELEROMETER_DEFAULT_ENABLED);
        int rate = prefs.getInt(SensorService.ACCELEROMETER_FREQUENCY, SensorService.DEFAULT_FREQUENCY);

        if (rate != this._lastAccelRate)
        {
            if (SensorService.accelerometerListener != null)
            {
                try
                {
                    sensors.unregisterListener(SensorService.accelerometerListener);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                SensorService.accelerometerListener = null;
            }

            this._lastAccelRate = rate;
        }

        Log.e("PW", "ACCEL ENABLED: " + enabled);

        if (enabled && SensorService.accelerometerListener == null)
        {
            SensorService.accelerometerListener = new SensorEventListener()
            {
                public void onSensorChanged(SensorEvent sensorEvent)
                {
                    AccelerometerHandler.handleSensorEvent(sensorEvent);
                }

                public void onAccuracyChanged(Sensor sensor, int i)
                {

                }
            };

            sensors.registerListener(SensorService.accelerometerListener, sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), rate);
        }
        else if (enabled == false && SensorService.accelerometerListener != null)
        {
            try
            {
                sensors.unregisterListener(SensorService.accelerometerListener);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SensorService.accelerometerListener = null;
            this._lastAccelRate = -1;
        }
    }

    private void setupGyroscope(final Context context, SensorManager sensors)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean enabled = prefs.getBoolean(SensorService.GYROSCOPE_ENABLED, SensorService.GYROSCOPE_DEFAULT_ENABLED);
        int rate = prefs.getInt(SensorService.GYROSCOPE_FREQUENCY, SensorService.DEFAULT_FREQUENCY);

        if (rate != this._lastGyroRate)
        {
            Log.e("PW", "RATE CHANGED. RESETTING GYRO... " + rate + " != " + this._lastGyroRate);
            if (SensorService.gyroscopeListener != null)
            {
                try
                {
                    sensors.unregisterListener(SensorService.gyroscopeListener, sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                SensorService.gyroscopeListener = null;
                GyroscopeHandler._enabled = false;
            }

            this._lastGyroRate = rate;
        }

        Log.e("PW", "GYRO ENABLED: " + enabled);

        if (enabled && SensorService.gyroscopeListener == null)
        {
            Log.e("PW", "REGISTERING GYRO...");

            // MAKE SINGLETON.
            SensorService.gyroscopeListener = new SensorEventListener()
            {
                public void onSensorChanged(SensorEvent sensorEvent)
                {
                    GyroscopeHandler.handleSensorEvent(sensorEvent);
                }

                public void onAccuracyChanged(Sensor sensor, int i)
                {

                }
            };

            sensors.registerListener(SensorService.gyroscopeListener, sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE), rate);
            GyroscopeHandler._enabled = true;
        }
        else if (enabled == false && SensorService.gyroscopeListener != null)
        {
            Log.e("PW", "UNREGISTERING GYRO...");

            try
            {
                sensors.unregisterListener(SensorService.gyroscopeListener, sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SensorService.gyroscopeListener = null;
            GyroscopeHandler._enabled = false;
        }
    }

    private void setupMagnetometer(final Context context, SensorManager sensors)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean enabled = prefs.getBoolean(SensorService.MAGNETOMETER_ENABLED, SensorService.MAGNETOMETER_DEFAULT_ENABLED);
        int rate = prefs.getInt(SensorService.MAGNETOMETER_FREQUENCY, SensorService.DEFAULT_FREQUENCY);

        if (rate != this._lastMagnetRate)
        {
            if (SensorService.magnetometerListener != null)
            {
                try
                {
                    sensors.unregisterListener(SensorService.magnetometerListener);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                SensorService.magnetometerListener = null;
            }

            this._lastMagnetRate = rate;
        }

        Log.e("PW", "MAGNET ENABLED: " + enabled);

        if (enabled && SensorService.magnetometerListener == null)
        {
            SensorService.magnetometerListener = new SensorEventListener()
            {
                public void onSensorChanged(SensorEvent sensorEvent)
                {
                    MagnetometerHandler.handleSensorEvent(sensorEvent);
                }

                public void onAccuracyChanged(Sensor sensor, int i)
                {

                }
            };

            sensors.registerListener(SensorService.magnetometerListener, sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), rate);
        }
        else if (enabled == false && SensorService.magnetometerListener != null)
        {
            try
            {
                sensors.unregisterListener(SensorService.magnetometerListener);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SensorService.magnetometerListener = null;
            this._lastMagnetRate = -1;
        }
    }

    private void setupLightMeter(final Context context, SensorManager sensors)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean enabled = prefs.getBoolean(SensorService.LIGHT_METER_ENABLED, SensorService.LIGHT_METER_DEFAULT_ENABLED);
        int rate = prefs.getInt(SensorService.LIGHT_METER_FREQUENCY, SensorService.DEFAULT_FREQUENCY);

        if (rate != this._lastLightRate)
        {
            if (SensorService.lightListener != null)
            {
                try
                {
                    sensors.unregisterListener(SensorService.lightListener);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                SensorService.lightListener = null;
            }

            this._lastLightRate = rate;
        }

        Log.e("PW", "LIGHT ENABLED: " + enabled);

        if (enabled && SensorService.lightListener == null)
        {
            SensorService.lightListener = new SensorEventListener()
            {
                public void onSensorChanged(SensorEvent sensorEvent)
                {
                    LightHandler.handleSensorEvent(sensorEvent);
                }

                public void onAccuracyChanged(Sensor sensor, int i)
                {

                }
            };

            sensors.registerListener(SensorService.lightListener, sensors.getDefaultSensor(Sensor.TYPE_LIGHT), rate);
        }
        else if (enabled == false && SensorService.lightListener != null)
        {
            try
            {
                sensors.unregisterListener(SensorService.lightListener);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SensorService.lightListener = null;
            this._lastLightRate = -1;
        }
    }

    private void setupHeartMeter(final Context context, SensorManager sensors)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean enabled = prefs.getBoolean(SensorService.HEART_METER_ENABLED, SensorService.HEART_METER_DEFAULT_ENABLED);
        int rate = prefs.getInt(SensorService.HEART_METER_FREQUENCY, SensorService.DEFAULT_FREQUENCY);

        if (rate != this._lastHeartRate)
        {
            if (SensorService.heartListener != null)
            {
                try
                {
                    sensors.unregisterListener(SensorService.heartListener);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                SensorService.heartListener = null;
            }

            this._lastHeartRate = rate;
        }

        Log.e("PW", "HEART ENABLED: " + enabled);

        if (enabled && SensorService.heartListener == null)
        {
            SensorService.heartListener = new SensorEventListener()
            {
                public void onSensorChanged(SensorEvent sensorEvent)
                {
                    HeartHandler.handleSensorEvent(sensorEvent);
                }

                public void onAccuracyChanged(Sensor sensor, int i)
                {

                }
            };

            sensors.registerListener(SensorService.heartListener, sensors.getDefaultSensor(Sensor.TYPE_HEART_RATE), rate);
        }
        else if (enabled == false && SensorService.heartListener != null)
        {
            try
            {
                sensors.unregisterListener(SensorService.heartListener);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SensorService.heartListener = null;
            this._lastHeartRate = -1;
        }
    }

    protected void onHandleIntent(Intent intent)
    {
        SensorManager sensors = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

        this.setupAccelerometer(this, sensors);
        this.setupGyroscope(this, sensors);
        this.setupMagnetometer(this, sensors);
        this.setupLightMeter(this, sensors);
        this.setupHeartMeter(this, sensors);
    }

    public static void transmitData(String source, DataMap data)
    {
        Log.e("PW", "XMIT FROM " + source);

        synchronized (SensorService._payloads)
        {
            while (SensorService._payloads.size() > 256)
                SensorService._payloads.removeAt(0);

            data.putString(SensorService.BUNDLE_SOURCE, source);
            SensorService._payloads.append(System.currentTimeMillis(), data);
        }
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Log.e("PW", "CONNECTED");

        final SensorService me = this;

        Wearable.MessageApi.addListener(SensorService._apiClient, new MessageApi.MessageListener()
        {
            public void onMessageReceived(MessageEvent event)
            {
                Log.e("PW", "MSG PATH: " + event.getPath());

                if (SensorService.PATH_REQUEST_DATA.equals(event.getPath()))
                {
                    if (SensorService._isTransmitting == false)
                    {
                        SensorService._isTransmitting = true;

                        long now = System.currentTimeMillis();
                        ArrayList<Long> transmitted = new ArrayList<Long>();

                        synchronized (SensorService._payloads)
                        {
                            for (int i = 0; i < SensorService._payloads.size(); i++)
                            {
                                Long timestamp = SensorService._payloads.keyAt(i);

                                if (timestamp < now)
                                {
                                    if (SensorService._apiClient.isConnected())
                                    {
                                        DataMap map = SensorService._payloads.valueAt(i);

                                        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(SensorService.URI_READING_PREFIX + "/" + map.getString(SensorService.BUNDLE_SOURCE) + "/" + System.currentTimeMillis());
                                        putDataMapReq.getDataMap().putAll(map);

                                        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();

                                        Wearable.DataApi.putDataItem(SensorService._apiClient, putDataReq);

                                        transmitted.add(timestamp);
                                    }
                                }
                            }

                            for (Long timestamp : transmitted)
                            {
                                SensorService._payloads.remove(timestamp);
                            }
                        }

                        SensorService._isTransmitting = false;
                    }
                }
                else if (SensorService.PATH_SEND_CONFIG.equals(event.getPath()))
                {
                    byte[] config = event.getData();

                    Log.e("PW", "CONFIG: " + config);

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(me);
                    SharedPreferences.Editor e = prefs.edit();

                    e.putBoolean(SensorService.ACCELEROMETER_ENABLED, config[0] > 0x00);
                    e.putBoolean(SensorService.GYROSCOPE_ENABLED, config[2] > 0x00);
                    e.putBoolean(SensorService.MAGNETOMETER_ENABLED, config[4] > 0x00);
                    e.putBoolean(SensorService.LIGHT_METER_ENABLED, config[6] > 0x00);
                    e.putBoolean(SensorService.HEART_METER_ENABLED, config[8] > 0x00);

                    e.putInt(SensorService.ACCELEROMETER_FREQUENCY, (int) config[1]);
                    e.putInt(SensorService.GYROSCOPE_FREQUENCY, (int) config[3]);
                    e.putInt(SensorService.MAGNETOMETER_FREQUENCY, (int) config[5]);
                    e.putInt(SensorService.LIGHT_METER_FREQUENCY, (int) config[7]);
                    e.putInt(SensorService.HEART_METER_FREQUENCY, (int) config[9]);

                    e.commit();
                }
            }
        });
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {

        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.e("PW", "CONNECTION SUSPENDED " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        Log.e("PW", "CONNECT FAILED: " + connectionResult.toString());

        final SensorService me = this;

        Runnable r = new Runnable()
        {
            public void run()
            {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                SensorService._apiClient.connect();
            }
        };

        Thread t = new Thread(r);
        t.start();
    }
}
