package edu.northwestern.cbits.purple_robot_manager.plugins;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActivityManager.RunningTaskInfo;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

@SuppressLint("NewApi")
public class StreamingJacksonUploadPlugin extends DataUploadPlugin
{
    private final static String ERROR_EXTENSION = ".error";
    private final static String FILE_EXTENSION = ".jackson";
    private static final String TEMP_FILE_EXTENSION = ".jackson-temp";

    private static final String ENABLED = "config_enable_streaming_jackson_data_server";
    private static final String UPLOAD_SIZE = "config_streaming_jackson_upload_size";
    private static final String UPLOAD_INTERVAL = "config_streaming_jackson_upload_interval";

    private static final String UPLOAD_SIZE_DEFAULT = "262114";
    private static final String UPLOAD_INTERVAL_DEFAULT = "300";

    private JsonGenerator _generator = null;

    private long _lastAttempt = 0;
    private File _currentFile = null;

    public String[] respondsTo()
    {
        String[] activeActions =
        { Probe.PROBE_READING, OutputPlugin.FORCE_UPLOAD };
        return activeActions;
    }

    private void uploadFiles(final Context context, final SharedPreferences prefs)
    {
        long now = System.currentTimeMillis();

        long duration = Long.parseLong(prefs.getString(StreamingJacksonUploadPlugin.UPLOAD_INTERVAL,
                StreamingJacksonUploadPlugin.UPLOAD_INTERVAL_DEFAULT)) * 1000;

        if (now - this._lastAttempt < duration)
            return;

        this._lastAttempt = now;

        final StreamingJacksonUploadPlugin me = this;

        Runnable r = new Runnable()
        {
            @SuppressLint("TrulyRandom")
            public void run()
            {
                synchronized (me)
                {
                    try
                    {
                        me.closeOpenSession();

                        File pendingFolder = me.getPendingFolder();

                        String[] filenames = pendingFolder.list(new FilenameFilter()
                        {
                            public boolean accept(File dir, String filename)
                            {
                                return filename.endsWith(StreamingJacksonUploadPlugin.FILE_EXTENSION);
                            }
                        });

                        if (filenames == null)
                            filenames = new String[0];

                        if (filenames.length < 1)
                            return;

                        SecureRandom random = new SecureRandom();

                        int index = 0;

                        if (filenames.length > 1)
                            index = random.nextInt(filenames.length);

                        File payloadFile = new File(pendingFolder, filenames[index]);

                        String payload = FileUtils.readFileToString(payloadFile, "UTF-8");

                        if (me.transmitPayload(prefs, payload) == DataUploadPlugin.RESULT_SUCCESS)
                        {
                            payloadFile.delete();

                            me._lastAttempt = 0;
                            me.uploadFiles(context, prefs);
                        }
                        else
                        {
                            try
                            {
                                JSONArray jsonPayload = new JSONArray(payload);

                                // JSON is valid
                            }
                            catch (JSONException e)
                            {
                                // Invalid JSON, log results.

                                LogManager.getInstance(context).logException(e);

                                HashMap<String, Object> details = new HashMap<String, Object>();
                                payloadFile.renameTo(new File(payloadFile.getAbsolutePath() + StreamingJacksonUploadPlugin.ERROR_EXTENSION));

                                details.put("name", payloadFile.getAbsolutePath());
                                details.put("size", payloadFile.length());

                                LogManager.getInstance(context).log("corrupted_file", details);
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        LogManager.getInstance(context).logException(e);
                        me.broadcastMessage(context.getString(R.string.message_general_error), true);
                    }
                }
            }
        };

        Thread t = new Thread(r);
        t.start();
    }

    public void processIntent(final Intent intent)
    {
        final Context context = this.getContext().getApplicationContext();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean(StreamingJacksonUploadPlugin.ENABLED, false) == false)
            return;

        synchronized (this)
        {
            String action = intent.getAction();

            if (OutputPlugin.FORCE_UPLOAD.equals(action))
            {
                this._lastAttempt = 0;

                this.uploadFiles(context, prefs);
            }
            else if (Probe.PROBE_READING.equals(action))
            {
                Bundle extras = intent.getExtras();

                if (extras.containsKey(DataUploadPlugin.TRANSMIT_KEY)
                        && extras.getBoolean(DataUploadPlugin.TRANSMIT_KEY) == false)
                {

                }
                else
                {
                    long now = System.currentTimeMillis();

                    try
                    {
                        File pendingFolder = this.getPendingFolder();

                        if (this._currentFile != null)
                        {
                            File f = this._currentFile.getAbsoluteFile();

                            long length = f.length();
                            long modDelta = now - f.lastModified();

                            long size = Long.parseLong(prefs.getString(StreamingJacksonUploadPlugin.UPLOAD_SIZE,
                                    StreamingJacksonUploadPlugin.UPLOAD_SIZE_DEFAULT));

                            if (this._generator != null && (length > size || modDelta > 60000))
                                this.closeOpenSession();
                        }

                        this.uploadFiles(context, prefs);

                        if (this._generator == null)
                        {
                            this._currentFile = new File(pendingFolder, now + TEMP_FILE_EXTENSION);

                            JsonFactory factory = new JsonFactory();

                            this._generator = factory.createGenerator(this._currentFile, JsonEncoding.UTF8);

                            this._generator.writeStartArray();
                        }

                        StreamingJacksonUploadPlugin.writeBundle(this.getContext(), this._generator, extras);
                        this._generator.flush();
                    }
                    catch (IOException e)
                    {
                        LogManager.getInstance(this.getContext()).logException(e);
                    }
                }
            }
        }
    }

    private void closeOpenSession() throws JsonGenerationException, IOException
    {
        if (this._generator == null || this._currentFile == null)
            return;

        File pendingFolder = this.getPendingFolder();

        this._generator.writeEndArray();
        this._generator.flush();
        this._generator.close();

        this._generator = null;

        String tempFile = this._currentFile.getAbsolutePath();
        String finalFile = tempFile.replace(TEMP_FILE_EXTENSION, FILE_EXTENSION);

        this._currentFile = null;

        FileUtils.moveFile(new File(tempFile), new File(finalFile));

        String[] filenames = pendingFolder.list(new FilenameFilter()
        {
            public boolean accept(File dir, String filename)
            {
                return filename.endsWith(StreamingJacksonUploadPlugin.TEMP_FILE_EXTENSION);
            }
        });

        if (filenames == null)
            filenames = new String[0];

        for (String filename : filenames)
        {
            File toDelete = new File(pendingFolder, filename);

            toDelete.delete();
        }
    }

    @SuppressWarnings("unchecked")
    public static void writeBundle(Context context, JsonGenerator generator, Bundle bundle)
    {
        try
        {
            generator.writeStartObject();

            Map<String, Object> values = OutputPlugin.getValues(bundle);

            for (String key : values.keySet())
            {
                Object value = values.get(key);

                if (value == null || key == null)
                {
                    // Skip
                }
                else
                {
                    if (value instanceof String)
                        generator.writeStringField(key, (String) value);
                    else if (value instanceof float[])
                    {
                        float[] floats = (float[]) value;

                        generator.writeArrayFieldStart(key);

                        for (float f : floats)
                            generator.writeNumber(f);

                        generator.writeEndArray();
                    }
                    else if (value instanceof int[])
                    {
                        int[] ints = (int[]) value;

                        generator.writeArrayFieldStart(key);

                        for (int i : ints)
                            generator.writeNumber(i);

                        generator.writeEndArray();
                    }
                    else if (value instanceof long[])
                    {
                        long[] longs = (long[]) value;

                        generator.writeArrayFieldStart(key);

                        for (long l : longs)
                            generator.writeNumber(l);

                        generator.writeEndArray();
                    }
                    else if (value instanceof double[])
                    {
                        double[] doubles = (double[]) value;

                        generator.writeArrayFieldStart(key);

                        for (double d : doubles)
                            generator.writeNumber(d);

                        generator.writeEndArray();
                    }
                    else if (value instanceof Float)
                    {
                        Float f = (Float) value;

                        generator.writeNumberField(key, f);
                    }
                    else if (value instanceof Integer)
                    {
                        Integer i = (Integer) value;

                        generator.writeNumberField(key, i);
                    }
                    else if (value instanceof Long)
                    {
                        Long l = (Long) value;

                        generator.writeNumberField(key, l);
                    }
                    else if (value instanceof Boolean)
                    {
                        Boolean b = (Boolean) value;

                        generator.writeBooleanField(key, b);
                    }
                    else if (value instanceof Short)
                    {
                        Short s = (Short) value;

                        generator.writeNumberField(key, s);
                    }
                    else if (value instanceof Double)
                    {
                        Double d = (Double) value;

                        if (d.isInfinite())
                            generator.writeNumberField(key, Double.MAX_VALUE);
                        else
                            generator.writeNumberField(key, d);
                    }
                    else if (value instanceof List)
                    {
                        List<Object> list = (List<Object>) value;

                        generator.writeArrayFieldStart(key);

                        for (Object o : list)
                        {
                            if (o instanceof String)
                                generator.writeString(o.toString());
                            else if (o instanceof Bundle)
                                StreamingJacksonUploadPlugin.writeBundle(context, generator, (Bundle) o);
                            else if (o instanceof ScanResult)
                            {
                                ScanResult s = (ScanResult) o;

                                generator.writeStartObject();

                                if (s.BSSID != null)
                                    generator.writeStringField("BSSID", s.BSSID);

                                if (s.SSID != null)
                                    generator.writeStringField("SSID", s.SSID);

                                if (s.capabilities != null)
                                    generator.writeStringField("Capabilities", s.capabilities);

                                generator.writeNumberField("Frequency", s.frequency);
                                generator.writeNumberField("Level dBm", s.level);

                                generator.writeEndObject();
                            }
                            else if (o instanceof RunningTaskInfo)
                            {
                                RunningTaskInfo r = (RunningTaskInfo) o;

                                generator.writeStartObject();

                                if (r.baseActivity != null)
                                    generator.writeStringField("Base Activity", r.baseActivity.getPackageName());

                                if (r.description != null)
                                    generator.writeStringField("Description", r.description.toString());

                                generator.writeNumberField("Activity Count", r.numActivities);
                                generator.writeNumberField("Running Activity Count", r.numRunning);

                                generator.writeEndObject();
                            }
                            else if (o instanceof ApplicationInfo)
                            {
                                ApplicationInfo a = (ApplicationInfo) o;

                                generator.writeString(a.packageName);
                            }
                            else if (o instanceof Location)
                            {
                                Location l = (Location) o;

                                generator.writeStartObject();

                                generator.writeNumberField("Accuracy", l.getAccuracy());
                                generator.writeNumberField("Altitude", l.getAltitude());
                                generator.writeNumberField("Bearing", l.getBearing());
                                generator.writeNumberField("Latitude", l.getLatitude());
                                generator.writeNumberField("Longitude", l.getLongitude());
                                generator.writeNumberField("Speed", l.getSpeed());
                                generator.writeNumberField("Timestamp", l.getTime());

                                if (l.getProvider() != null)
                                    generator.writeStringField("Provider", l.getProvider());
                                else
                                    generator.writeStringField("Provider", "Unknown");

                                generator.writeEndObject();
                            }
                            else
                                Log.e("PRM", "LIST OBJ: " + o.getClass().getCanonicalName() + " IN " + key);
                        }

                        generator.writeEndArray();
                    }
                    else if (value instanceof Location)
                    {
                        Location l = (Location) value;

                        generator.writeStartObject();

                        generator.writeNumberField("Accuracy", l.getAccuracy());
                        generator.writeNumberField("Altitude", l.getAltitude());
                        generator.writeNumberField("Bearing", l.getBearing());
                        generator.writeNumberField("Latitude", l.getLatitude());
                        generator.writeNumberField("Longitude", l.getLongitude());
                        generator.writeNumberField("Speed", l.getSpeed());
                        generator.writeNumberField("Timestamp", l.getTime());

                        if (l.getProvider() != null)
                            generator.writeStringField("Provider", l.getProvider());
                        else
                            generator.writeStringField("Provider", "Unknown");

                        generator.writeEndObject();
                    }
                    else if (value instanceof BluetoothClass)
                    {
                        BluetoothClass btClass = (BluetoothClass) value;

                        generator.writeStringField(key, btClass.toString());
                    }
                    else if (value instanceof BluetoothDevice)
                    {
                        BluetoothDevice device = (BluetoothDevice) value;

                        generator.writeStartObject();

                        if (device.getBondState() == BluetoothDevice.BOND_BONDED)
                            generator.writeStringField("Bond State", "Bonded");
                        else if (device.getBondState() == BluetoothDevice.BOND_BONDING)
                            generator.writeStringField("Bond State", "Bonding");
                        else
                            generator.writeStringField("Bond State", "None");

                        generator.writeStringField("Device Address", device.getAddress());
                        generator.writeStringField("Device Class", device.getBluetoothClass().toString());

                        generator.writeEndObject();
                    }
                    else if (value instanceof Bundle)
                    {
                        generator.writeFieldName(key);
                        StreamingJacksonUploadPlugin.writeBundle(context, generator, (Bundle) value);
                    }
                    else
                        Log.e("PRM", "GOT TYPE " + value.getClass().getCanonicalName() + " FOR " + key);
                }
            }

            generator.writeEndObject();
        }
        catch (IOException e)
        {
            LogManager.getInstance(context).logException(e);
        }
    }

    public int pendingFilesCount()
    {
        File pendingFolder = this.getPendingFolder();

        String[] filenames = pendingFolder.list(new FilenameFilter()
        {
            public boolean accept(File dir, String filename)
            {
                return (filename.endsWith(StreamingJacksonUploadPlugin.FILE_EXTENSION) || filename
                        .endsWith(StreamingJacksonUploadPlugin.TEMP_FILE_EXTENSION));
            }
        });

        if (filenames == null)
            filenames = new String[0];

        return filenames.length;
    }

    public long pendingFilesSize()
    {
        File pendingFolder = this.getPendingFolder();

        String[] filenames = pendingFolder.list(new FilenameFilter()
        {
            public boolean accept(File dir, String filename)
            {
                return (filename.endsWith(StreamingJacksonUploadPlugin.FILE_EXTENSION) || filename
                        .endsWith(StreamingJacksonUploadPlugin.TEMP_FILE_EXTENSION));
            }
        });

        if (filenames == null)
            filenames = new String[0];

        if (filenames.length < 1024)
        {
            try
            {
                return FileUtils.sizeOf(pendingFolder);
            }
            catch (IllegalArgumentException e)
            {
                // File went away - try again...

                return this.pendingFilesSize();
            }
        }

        return 2 * 1024 * 1024 * 1024;
    }
}
