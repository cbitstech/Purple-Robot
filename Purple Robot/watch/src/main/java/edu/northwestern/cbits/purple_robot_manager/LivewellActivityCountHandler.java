package edu.northwestern.cbits.purple_robot_manager;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import com.google.android.gms.wearable.DataMap;

public class LivewellActivityCountHandler
{
    private static final String BUNDLE_DURATION = "BUNDLE_DURATION";
    private static final String BUNDLE_NUM_SAMPLES = "BUNDLE_NUM_SAMPLES";
    private static final String BUNDLE_X_DELTA = "BUNDLE_X_DELTA";
    private static final String BUNDLE_Y_DELTA = "BUNDLE_Y_DELTA";
    private static final String BUNDLE_Z_DELTA = "BUNDLE_Z_DELTA";
    private static final String BUNDLE_ALL_DELTA = "BUNDLE_ALL_DELTA";

    private static double _lastX = 0;
    private static double _lastY = 0;
    private static double _lastZ = 0;

    private static double _xSum = 0;
    private static double _ySum = 0;
    private static double _zSum = 0;

    private static double _lastBinStart = 0;
    private static int _numSamples = 0;


    private static String name()
    {
        return "edu.northwestern.cbits.purple_robot_manager.WearLivewellActivityCountProbe";
    }

    public static void handleSensorEvent(SensorEvent event, int binSize)
    {
        final double now = (double) System.currentTimeMillis();

        synchronized(LivewellActivityCountHandler.BUNDLE_DURATION)
        {
            if (LivewellActivityCountHandler._lastBinStart == 0)
                LivewellActivityCountHandler._lastBinStart = now;
            else if (now - LivewellActivityCountHandler._lastBinStart > (((long) binSize) * 1000))
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
                data.putString(SensorService.BUNDLE_PROBE, LivewellActivityCountHandler.name());

                data.putDataMap(SensorService.BUNDLE_SENSOR, sensorBundle);

                data.putDouble(LivewellActivityCountHandler.BUNDLE_X_DELTA, LivewellActivityCountHandler._xSum);
                data.putDouble(LivewellActivityCountHandler.BUNDLE_Y_DELTA, LivewellActivityCountHandler._ySum);
                data.putDouble(LivewellActivityCountHandler.BUNDLE_Z_DELTA, LivewellActivityCountHandler._zSum);
                data.putDouble(LivewellActivityCountHandler.BUNDLE_ALL_DELTA, (LivewellActivityCountHandler._xSum + LivewellActivityCountHandler._ySum + LivewellActivityCountHandler._zSum) / 3);
                data.putDouble(LivewellActivityCountHandler.BUNDLE_DURATION, now - LivewellActivityCountHandler._lastBinStart);
                data.putDouble(LivewellActivityCountHandler.BUNDLE_NUM_SAMPLES, LivewellActivityCountHandler._numSamples);

                SensorService.transmitData("livewell_activity_counts", data);

                LivewellActivityCountHandler._lastBinStart = now;

                LivewellActivityCountHandler._xSum = 0;
                LivewellActivityCountHandler._ySum = 0;
                LivewellActivityCountHandler._zSum = 0;
                LivewellActivityCountHandler._numSamples = 0;

                LivewellActivityCountHandler._lastBinStart = now;
            }

            LivewellActivityCountHandler._xSum += Math.abs(event.values[0] - LivewellActivityCountHandler._lastX);
            LivewellActivityCountHandler._ySum += Math.abs(event.values[1] - LivewellActivityCountHandler._lastY);
            LivewellActivityCountHandler._zSum += Math.abs(event.values[2] - LivewellActivityCountHandler._lastZ);

            LivewellActivityCountHandler._lastX = event.values[0];
            LivewellActivityCountHandler._lastY = event.values[1];
            LivewellActivityCountHandler._lastZ = event.values[2];

            LivewellActivityCountHandler._numSamples += 1;
        }
    }
}
