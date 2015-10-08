package edu.northwestern.cbits.purple_robot_manager.probes.sensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.net.Uri;
import android.os.Bundle;

import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.SettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class LightSensorProbe extends SensorProbe
{
    private double[] _sensorTimeBuffer = null;
    private double[] _timeBuffer = null;
    private double[] _accuracyBuffer = null;
    private int _bufferIndex = 0;
    private double[][] _valueBuffer = null;
    private Sensor _sensor = null;

    @Override
    public String getPreferenceKey() {
        return "light_sensor";
    }

    @Override
    protected int getSensorType() {
        return Sensor.TYPE_LIGHT;
    }

    @Override
    protected void clearDataBuffers() {
        this.flushData();

        this._sensorTimeBuffer = null;
        this._timeBuffer = null;
        this._accuracyBuffer = null;
        this._valueBuffer = null;

        this._bufferIndex = 0;
    }

    @Override
    public void createDataBuffers() {
        int duration = this.getDuration();

        if (duration < 1)
            duration = 32;

        int samples = duration * 125;

        this._sensorTimeBuffer = new double[samples];
        this._timeBuffer = new double[samples];
        this._accuracyBuffer = new double[samples];
        this._valueBuffer = new double[1][samples];

        this._bufferIndex = 0;
    }

    @Override
    protected void attachReadings(Bundle data)
    {
        double[] normalBuffer = new double[this._sensorTimeBuffer.length];

        double start = this._sensorTimeBuffer[0];
        double end = this._sensorTimeBuffer[this._sensorTimeBuffer.length - 1];
        double tick = (end - start) / this._sensorTimeBuffer.length;

        start = this._timeBuffer[0] * (1000 * 1000);

        for (int i = 0; i < normalBuffer.length &&  i < this._bufferIndex; i++)
        {
            normalBuffer[i] = (start + (tick * i)) / (1000 * 1000);
        }

        long now = System.currentTimeMillis();

        SharedPreferences prefs = Probe.getPreferences(this._context);

        File internalStorage = this._context.getCacheDir();

        if (SettingsActivity.useExternalStorage(this._context))
            internalStorage = this._context.getExternalCacheDir();

        if (internalStorage != null && !internalStorage.exists())
            internalStorage.mkdirs();

        File folder = new File(internalStorage, "Light Sensor Files");

        if (folder != null && !folder.exists())
            folder.mkdirs();

        final File accelFile = new File(folder, "light-" +  now + ".msgpack");

        try
        {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(accelFile));

            MessagePack msgpack = new MessagePack();
            Packer packer = msgpack.createPacker(out);

            packer.write("CpuTime,SensorTime,NormalTime,Accuracy,Lux");

            packer.write(Arrays.copyOf(this._timeBuffer, this._bufferIndex));
            packer.write(Arrays.copyOf(this._sensorTimeBuffer, this._bufferIndex));
            packer.write(Arrays.copyOf(normalBuffer, this._bufferIndex));
            packer.write(Arrays.copyOf(this._accuracyBuffer, this._bufferIndex));

            for (int i = 0; i < this._valueBuffer.length && i < this._bufferIndex; i++)
            {
                packer.write(Arrays.copyOf(this._valueBuffer[i], this._bufferIndex));
            }

            packer.close();
            out.close();

            data.putString(Probe.PROBE_MEDIA_URL, Uri.fromFile(accelFile).toString());
            data.putString(Probe.PROBE_MEDIA_CONTENT_TYPE, "application/x-msgpack");
            data.putLong(Probe.PROBE_MEDIA_SIZE, accelFile.length());
        }
        catch (FileNotFoundException e)
        {
            LogManager.getInstance(this._context).logException(e);
        }
        catch (IOException e)
        {
            LogManager.getInstance(this._context).logException(e);
        }
    }

    @Override
    public String name(Context context) {
        return "edu.northwestern.cbits.purple_robot_manager.probes.sensors.LightSensorProbe";
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.title_light_sensor_probe);
    }

    @Override
    public String summary(Context context) {
        return context.getString(R.string.summary_light_sensor_probe_desc);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (this._sensor == null)
            this._sensor = event.sensor;

        final double now = (double) System.currentTimeMillis();

        synchronized (this) {
            if (this._sensorTimeBuffer == null) {
                this.createDataBuffers();
            }

            this._sensorTimeBuffer[this._bufferIndex] = event.timestamp;
            this._timeBuffer[this._bufferIndex] = now / 1000;

            this._accuracyBuffer[this._bufferIndex] = event.accuracy;

            for (int i = 0; i < event.values.length && i < this._valueBuffer.length; i++) {
                this._valueBuffer[i][this._bufferIndex] = event.values[i];
            }

            this._bufferIndex += 1;

            if (this._bufferIndex >= this._timeBuffer.length) {
                this.flushData();
            }
        }
    }

    @Override
    protected void flushData() {
        if (this._bufferIndex > 0) {
            final double now = (double) System.currentTimeMillis();

            this.transmitData(this._sensor, now / 1000);

            this._bufferIndex = 0;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Do nothing...
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double size = bundle.getDouble(Probe.PROBE_MEDIA_SIZE);

        size = size / 1024;

        return context.getString(R.string.summary_light_sensor_probe, size);
    }

}
