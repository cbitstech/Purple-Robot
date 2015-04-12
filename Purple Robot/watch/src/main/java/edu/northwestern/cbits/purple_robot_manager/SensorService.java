package edu.northwestern.cbits.purple_robot_manager;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
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
import java.util.Date;

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

    private static final String LIVEWELL_COUNTS_ENABLED = "SensorService.LIVEWELL_COUNTS_ENABLED";
    private static final boolean LIVEWELL_COUNTS_DEFAULT_ENABLED = false;
    private static final String LIVEWELL_COUNTS_BIN_SIZE = "SensorService.LIVEWELL_COUNTS_BIN_SIZE";
    private static final int LIVEWELL_COUNTS_DEFAULT_BIN_SIZE = 60;

    private static final int DEFAULT_FREQUENCY = SensorManager.SENSOR_DELAY_NORMAL;

    private static final String URI_READING_PREFIX = "/purple-robot-reading";
    private static final String BATTERY_LEVEL = "BATTERY_LEVEL";
    private static final String BATTERY_SCALE = "BATTERY_SCALE";
    private static final String BATTERY_CHARGING = "BATTERY_CHARGING";
    private static final String BATTERY_CHARGE_SOURCE = "BATTERY_CHARGE_SOURCE";

    private static SensorEventListener heartListener = new SensorEventListener()
    {
        public void onSensorChanged(SensorEvent sensorEvent)
        {
            if (SensorService._heartEnabled)
                HeartHandler.handleSensorEvent(sensorEvent);
        }

        public void onAccuracyChanged(Sensor sensor, int i)
        {

        }
    };

    private static SensorEventListener lightListener = new SensorEventListener()
    {
        public void onSensorChanged(SensorEvent sensorEvent)
        {
            if (SensorService._lightEnabled)
                LightHandler.handleSensorEvent(sensorEvent);
        }

        public void onAccuracyChanged(Sensor sensor, int i)
        {

        }
    };

    private static SensorEventListener magnetometerListener = new SensorEventListener()
    {
        public void onSensorChanged(SensorEvent sensorEvent)
        {
            if (SensorService._magnetEnabled)
                MagneticFieldHandler.handleSensorEvent(sensorEvent);
        }

        public void onAccuracyChanged(Sensor sensor, int i)
        {

        }
    };

    private static SensorEventListener gyroscopeListener = new SensorEventListener()
    {
        public void onSensorChanged(SensorEvent sensorEvent)
        {
            if (SensorService._gyroEnabled)
                GyroscopeHandler.handleSensorEvent(sensorEvent);
        }

        public void onAccuracyChanged(Sensor sensor, int i)
        {

        }
    };

    private static SensorEventListener accelerometerListener = new SensorEventListener()
    {
        public void onSensorChanged(SensorEvent sensorEvent)
        {
            if (SensorService._accelEnabled)
                AccelerometerHandler.handleSensorEvent(sensorEvent);

            if (SensorService._livewellEnabled)
                LivewellActivityCountHandler.handleSensorEvent(sensorEvent, SensorService._livewellBinSize);
        }

        public void onAccuracyChanged(Sensor sensor, int i)
        {

        }
    };

    private static BroadcastReceiver batteryListener = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            SensorService._batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            SensorService._batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            SensorService._batteryCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING);

            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

            if (plugged == BatteryManager.BATTERY_PLUGGED_USB)
                SensorService._batteryPlug = "usb";
            else if (plugged == BatteryManager.BATTERY_PLUGGED_AC)
                SensorService._batteryPlug = "wall";
            else if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS)
                SensorService._batteryPlug = "wireless";
            else
                SensorService._batteryPlug = "none/unknown";
        }
    };

    private static GoogleApiClient _apiClient = null;
    private static LongSparseArray<DataMap> _payloads = new LongSparseArray<DataMap>();
    private static boolean _isTransmitting = false;

    private static boolean _accelEnabled = false;
    private static boolean _magnetEnabled = false;
    private static boolean _livewellEnabled = false;
    private static boolean _gyroEnabled = false;
    private static boolean _lightEnabled = false;
    private static boolean _heartEnabled = false;

    private static boolean _accelRegistered = false;
    private static boolean _gyroRegistered = false;
    private static boolean _magnetRegistered = false;
    private static boolean _heartRegistered = false;
    private static boolean _lightRegistered = false;

    private static int _livewellBinSize = 60;

    private static int _lastAccelRate = -1;
    private static int _lastGyroRate = -1;
    private static int _lastMagnetRate = -1;
    private static int _lastLightRate = -1;
    private static int _lastHeartRate = -1;

    private static boolean _batteryInited = false;
    private static int _batteryLevel = -1;
    private static int _batteryScale = -1;
    private static boolean _batteryCharging = false;
    private static String _batteryPlug = "unknown";
    private static long _lastBatteryLog = 0;

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

    private void setupGyroscope(SensorManager sensors)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        SensorService._gyroEnabled = prefs.getBoolean(SensorService.GYROSCOPE_ENABLED, SensorService.GYROSCOPE_DEFAULT_ENABLED);
        int rate = prefs.getInt(SensorService.GYROSCOPE_FREQUENCY, SensorService.DEFAULT_FREQUENCY);

        if (rate != SensorService._lastGyroRate)
        {
            try
            {
                sensors.unregisterListener(SensorService.gyroscopeListener, sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SensorService._lastGyroRate = rate;
            SensorService._gyroRegistered = false;
        }

        if (SensorService._gyroEnabled && SensorService._gyroRegistered == false)
        {
            sensors.registerListener(SensorService.gyroscopeListener, sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE), rate);
            SensorService._gyroRegistered = false;
        }
        else if (SensorService._gyroEnabled == false)
        {
            try
            {
                sensors.unregisterListener(SensorService.gyroscopeListener, sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SensorService._gyroRegistered = false;
        }
    }

    private void setupAccelerometer(SensorManager sensors)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        SensorService._accelEnabled = prefs.getBoolean(SensorService.ACCELEROMETER_ENABLED, SensorService.ACCELEROMETER_DEFAULT_ENABLED);
        SensorService._livewellEnabled = prefs.getBoolean(SensorService.LIVEWELL_COUNTS_ENABLED, SensorService.LIVEWELL_COUNTS_DEFAULT_ENABLED);
        SensorService._livewellBinSize = prefs.getInt(SensorService.LIVEWELL_COUNTS_BIN_SIZE, SensorService.LIVEWELL_COUNTS_DEFAULT_BIN_SIZE);

        int rate = prefs.getInt(SensorService.ACCELEROMETER_FREQUENCY, SensorService.DEFAULT_FREQUENCY);

        if (rate != SensorService._lastAccelRate)
        {
            try
            {
                sensors.unregisterListener(SensorService.accelerometerListener, sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SensorService._lastAccelRate = rate;
            SensorService._accelRegistered = false;
        }

        if ((SensorService._accelEnabled || SensorService._livewellEnabled) && SensorService._accelRegistered == false)
            sensors.registerListener(SensorService.accelerometerListener, sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), rate);
        else if (SensorService._accelEnabled == false && SensorService._livewellEnabled == false && SensorService._accelRegistered)
        {
            try
            {
                sensors.unregisterListener(SensorService.accelerometerListener, sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SensorService._accelRegistered = false;
        }
    }

    private void setupMagnetometer(SensorManager sensors)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        SensorService._magnetEnabled = prefs.getBoolean(SensorService.MAGNETOMETER_ENABLED, SensorService.MAGNETOMETER_DEFAULT_ENABLED);
        int rate = prefs.getInt(SensorService.MAGNETOMETER_FREQUENCY, SensorService.DEFAULT_FREQUENCY);

        if (rate != SensorService._lastMagnetRate)
        {
            try
            {
                sensors.unregisterListener(SensorService.magnetometerListener, sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SensorService._lastMagnetRate = rate;
            SensorService._magnetRegistered = false;
        }

        if (SensorService._magnetEnabled && SensorService._magnetRegistered == false)
        {
            sensors.registerListener(SensorService.magnetometerListener, sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), rate);
            SensorService._magnetRegistered = false;
        }
        else if (SensorService._magnetEnabled == false)
        {
            try
            {
                sensors.unregisterListener(SensorService.magnetometerListener, sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SensorService._magnetRegistered = false;
        }
    }

    private void setupLightMeter(SensorManager sensors)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        SensorService._lightEnabled = prefs.getBoolean(SensorService.LIGHT_METER_ENABLED, SensorService.LIGHT_METER_DEFAULT_ENABLED);
        int rate = prefs.getInt(SensorService.LIGHT_METER_FREQUENCY, SensorService.DEFAULT_FREQUENCY);

        if (rate != SensorService._lastLightRate)
        {
            try
            {
                sensors.unregisterListener(SensorService.lightListener, sensors.getDefaultSensor(Sensor.TYPE_LIGHT));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SensorService._lastLightRate = rate;
            SensorService._lightRegistered = false;
        }

        if (SensorService._lightEnabled && SensorService._lightRegistered == false)
        {
            sensors.registerListener(SensorService.lightListener, sensors.getDefaultSensor(Sensor.TYPE_LIGHT), rate);
            SensorService._lightRegistered = false;
        }
        else if (SensorService._lightEnabled == false)
        {
            try
            {
                sensors.unregisterListener(SensorService.lightListener, sensors.getDefaultSensor(Sensor.TYPE_LIGHT));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SensorService._lightRegistered = false;
        }
    }

    private void setupHeartMeter(SensorManager sensors)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        SensorService._heartEnabled = prefs.getBoolean(SensorService.HEART_METER_ENABLED, SensorService.HEART_METER_DEFAULT_ENABLED);
        int rate = prefs.getInt(SensorService.HEART_METER_FREQUENCY, SensorService.DEFAULT_FREQUENCY);

        if (rate != SensorService._lastHeartRate)
        {
            try
            {
                sensors.unregisterListener(SensorService.heartListener, sensors.getDefaultSensor(Sensor.TYPE_HEART_RATE));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SensorService._lastHeartRate = rate;
            SensorService._heartRegistered = false;
        }

        if (SensorService._heartEnabled && SensorService._heartRegistered == false)
        {
            sensors.registerListener(SensorService.heartListener, sensors.getDefaultSensor(Sensor.TYPE_HEART_RATE), rate);
            SensorService._heartRegistered = false;
        }
        else if (SensorService._heartEnabled == false)
        {
            try
            {
                sensors.unregisterListener(SensorService.heartListener, sensors.getDefaultSensor(Sensor.TYPE_HEART_RATE));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SensorService._heartRegistered = false;
        }
    }

    protected void onHandleIntent(Intent intent)
    {
        SensorManager sensors = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

        this.setupAccelerometer(sensors);
        this.setupGyroscope(sensors);
        this.setupMagnetometer(sensors);
        this.setupLightMeter(sensors);
        this.setupHeartMeter(sensors);

        if (SensorService._batteryInited == false)
        {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            this.registerReceiver(SensorService.batteryListener, filter);

            SensorService._batteryInited = true;
        }
    }

    public static int pendingPayloadsCount()
    {
        return SensorService._payloads.size();
    }

    public static void transmitData(String source, DataMap data)
    {
        Log.e("PW", "TRANSMITTING DATA FROM " + source);

        synchronized (SensorService._payloads)
        {
            while (SensorService._payloads.size() > 512)
                SensorService._payloads.removeAt(0);

            data.putString(SensorService.BUNDLE_SOURCE, source);
            SensorService._payloads.append(System.currentTimeMillis(), data);
        }

        long now = System.currentTimeMillis();

        if (now - SensorService._lastBatteryLog > 60000)
        {
            DataMap batteryData = new DataMap();

            batteryData.putDouble(SensorService.BUNDLE_TIMESTAMP, now / 1000);
            batteryData.putString(SensorService.BUNDLE_PROBE, "edu.northwestern.cbits.purple_robot_manager.WearBatteryProbe");
            batteryData.putInt(SensorService.BATTERY_LEVEL, SensorService._batteryLevel);
            batteryData.putInt(SensorService.BATTERY_SCALE, SensorService._batteryScale);
            batteryData.putBoolean(SensorService.BATTERY_CHARGING, SensorService._batteryCharging);
            batteryData.putString(SensorService.BATTERY_CHARGE_SOURCE, SensorService._batteryPlug);

            SensorService._lastBatteryLog = now;

            SensorService.transmitData("battery", batteryData);
        }
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        final SensorService me = this;

        Wearable.MessageApi.addListener(SensorService._apiClient, new MessageApi.MessageListener()
        {
            public void onMessageReceived(MessageEvent event)
            {
                Log.e("PW", "PATH: " + event.getPath() + " " + (new Date()));

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

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(me);
                    SharedPreferences.Editor e = prefs.edit();

                    e.putBoolean(SensorService.ACCELEROMETER_ENABLED, config[0] > 0x00);
                    e.putBoolean(SensorService.GYROSCOPE_ENABLED, config[2] > 0x00);
                    e.putBoolean(SensorService.MAGNETOMETER_ENABLED, config[4] > 0x00);
                    e.putBoolean(SensorService.LIGHT_METER_ENABLED, config[6] > 0x00);
                    e.putBoolean(SensorService.HEART_METER_ENABLED, config[8] > 0x00);

                    if (config.length > 10)
                        e.putBoolean(SensorService.LIVEWELL_COUNTS_ENABLED, config[10] > 0x00);

                    e.putInt(SensorService.ACCELEROMETER_FREQUENCY, (int) config[1]);
                    e.putInt(SensorService.GYROSCOPE_FREQUENCY, (int) config[3]);
                    e.putInt(SensorService.MAGNETOMETER_FREQUENCY, (int) config[5]);
                    e.putInt(SensorService.LIGHT_METER_FREQUENCY, (int) config[7]);
                    e.putInt(SensorService.HEART_METER_FREQUENCY, (int) config[9]);

                    if (config.length > 11)
                        e.putInt(SensorService.LIVEWELL_COUNTS_BIN_SIZE, (int) config[11]);

                    e.commit();
                }
            }
        });
    }


    public static String bytesToHex(byte[] bytes)
    {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();

        char[] hexChars = new char[bytes.length * 2];

        for ( int j = 0; j < bytes.length; j++ )
        {
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
                try
                {
                    Thread.sleep(2500);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                SensorService._apiClient.connect();
            }
        };

        Thread t = new Thread(r);
        t.start();
    }
}
