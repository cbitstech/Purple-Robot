package edu.northwestern.cbits.purple_robot_manager;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import com.google.android.gms.wearable.DataMap;

public class AccelerometerHandler
{
    private static int BUFFER_SIZE = 512;

    private static final float valueBuffer[][] = new float[3][BUFFER_SIZE];
    private static final long accuracyBuffer[] = new long[BUFFER_SIZE];
    private static final long timeBuffer[] = new long[BUFFER_SIZE];
    private static final long sensorTimeBuffer[] = new long[BUFFER_SIZE];

    private static int bufferIndex = 0;

    protected static final String X_KEY = "X";
    protected static final String Y_KEY = "Y";
    protected static final String Z_KEY = "Z";

    private static final String[] fieldNames = { X_KEY, Y_KEY, Z_KEY };

    private static String name()
    {
        return "edu.northwestern.cbits.purple_robot_manager.WearAccelerometerProbe";
    }

    public static void handleSensorEvent(SensorEvent event)
    {
        final long now = System.currentTimeMillis();

        synchronized (valueBuffer)
        {
            sensorTimeBuffer[bufferIndex] = event.timestamp;
            timeBuffer[bufferIndex] = now / 1000;

            accuracyBuffer[bufferIndex] = event.accuracy;

            for (int i = 0; i < valueBuffer.length; i++)
            {
                valueBuffer[i][bufferIndex] = event.values[i];
            }

            bufferIndex += 1;

            if (bufferIndex >= timeBuffer.length)
            {
                Sensor sensor = event.sensor;

                DataMap data = new DataMap();

                DataMap sensorBundle = new DataMap();
                sensorBundle.putFloat(SensorService.SENSOR_MAXIMUM_RANGE, sensor.getMaximumRange());
                sensorBundle.putString(SensorService.SENSOR_NAME, sensor.getName());
                sensorBundle.putFloat(SensorService.SENSOR_POWER, sensor.getPower());
                sensorBundle.putFloat(SensorService.SENSOR_RESOLUTION, sensor.getResolution());
                sensorBundle.putInt(SensorService.SENSOR_TYPE, sensor.getType());
                sensorBundle.putString(SensorService.SENSOR_VENDOR, sensor.getVendor());
                sensorBundle.putInt(SensorService.SENSOR_VERSION, sensor.getVersion());

                data.putDouble(SensorService.BUNDLE_TIMESTAMP, now / 1000);
                data.putString(SensorService.BUNDLE_PROBE, AccelerometerHandler.name());

                data.putDataMap(SensorService.BUNDLE_SENSOR, sensorBundle);

                data.putLongArray(SensorService.EVENT_TIMESTAMP, timeBuffer);
                data.putLongArray(SensorService.SENSOR_TIMESTAMP, sensorTimeBuffer);
                data.putLongArray(SensorService.SENSOR_ACCURACY, accuracyBuffer);

                for (int i = 0; i < fieldNames.length; i++)
                {
                    data.putFloatArray(fieldNames[i], valueBuffer[i]);
                }

                SensorService.transmitData("accelerometer", data);

                bufferIndex = 0;
            }
        }
    }
}
