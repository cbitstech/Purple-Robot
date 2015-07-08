package edu.northwestern.cbits.purple_robot_manager.plugins;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.json.JSONArray;
import org.json.JSONException;

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
    private static final String PRIORITY_FILE_EXTENSION = ".priority";

    public static final String ENABLED = "config_enable_streaming_jackson_data_server";
    private static final String UPLOAD_SIZE = "config_streaming_jackson_upload_size";
    private static final String UPLOAD_INTERVAL = "config_streaming_jackson_upload_interval";

    private static final String UPLOAD_SIZE_DEFAULT = "262114";
    private static final String UPLOAD_INTERVAL_DEFAULT = "300";
    public static final boolean ENABLED_DEFAULT = false;
    public static final String LAST_UPLOAD_TIME = "streaming_json_last_upload";
    public static final String LAST_UPLOAD_SIZE = "streaming_json_last_upload_size";

    private JsonGenerator _generator = null;
    private boolean _priorityPayload = false;

    private long _lastAttempt = 0;
    private File _currentFile = null;
    private boolean _hasPriorityPayloads = false;

    private ArrayList<String> _regularFilenames = new ArrayList<>();
    private ArrayList<String> _priorityFilenames = new ArrayList<>();

    public String[] respondsTo()
    {
        String[] activeActions = { Probe.PROBE_READING, OutputPlugin.FORCE_UPLOAD };

        return activeActions;
    }

    private void uploadFiles(final Context context, final SharedPreferences prefs, final int callLevel)
    {
        long now = System.currentTimeMillis();

        long duration = Long.parseLong(prefs.getString(StreamingJacksonUploadPlugin.UPLOAD_INTERVAL, StreamingJacksonUploadPlugin.UPLOAD_INTERVAL_DEFAULT)) * 1000;

        if (now - this._lastAttempt < duration || this.shouldAttemptUpload(context) == false)
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

                        String[] filenames = {};

                        final MutableInt found = new MutableInt(0);

                        if (me._hasPriorityPayloads) {
                            if (me._priorityFilenames.size() == 0) {
                                String[] priorityFilenames = pendingFolder.list(new FilenameFilter() {
                                    public boolean accept(File dir, String filename) {
                                        // Only return first 256 for performance reasons...
                                        if (found.intValue() >= 256)
                                            return false;

                                        if (filename.endsWith(StreamingJacksonUploadPlugin.PRIORITY_FILE_EXTENSION)) {
                                            found.add(1);

                                            return true;
                                        }

                                        return false;
                                    }
                                });

                                for (String filename : priorityFilenames)
                                    me._priorityFilenames.add(filename);
                            }

                            filenames = me._priorityFilenames.toArray(new String[0]);
                        }

                        if (filenames == null || found.intValue() == 0)
                        {
                            me._hasPriorityPayloads = false;

                            if (me._regularFilenames.size() == 0) {
                                String[] regularFilenames = pendingFolder.list(new FilenameFilter() {
                                    public boolean accept(File dir, String filename) {
                                        // Only return first 1024 for performance reasons...
                                        if (found.intValue() >= 1024)
                                            return false;

                                        if (filename.endsWith(StreamingJacksonUploadPlugin.FILE_EXTENSION)) {
                                            found.add(1);

                                            return true;
                                        }

                                        return false;
                                    }
                                });


                                for (String filename : regularFilenames)
                                    me._regularFilenames.add(filename);
                            }

                            filenames = me._regularFilenames.toArray(new String[0]);
                        }

                        if (filenames.length < 1)
                            return;

                        SecureRandom random = new SecureRandom();

                        int index = 0;

                        if (filenames.length > 1)
                            index = random.nextInt(filenames.length);

                        String filename = filenames[index];
                        me._priorityFilenames.remove(filename);
                        me._regularFilenames.remove(filename);

                        File payloadFile = new File(pendingFolder, filename);

                        String payload = FileUtils.readFileToString(payloadFile, "UTF-8");

                        int result = me.transmitPayload(prefs, payload);

                        if (result == DataUploadPlugin.RESULT_SUCCESS)
                        {
                            SharedPreferences.Editor e = prefs.edit();
                            e.putLong(StreamingJacksonUploadPlugin.LAST_UPLOAD_TIME, System.currentTimeMillis());
                            e.putLong(StreamingJacksonUploadPlugin.LAST_UPLOAD_SIZE, payloadFile.length());
                            e.commit();

                            payloadFile.delete();

                            me._lastAttempt = 0;
                            me.uploadFiles(context, prefs, callLevel + 1);
                        }
                        else if (result == DataUploadPlugin.RESULT_NO_CONNECTION)
                        {
                            // WiFi only or no connection...
                        }
                        else if (result == DataUploadPlugin.RESULT_NO_POWER)
                        {
                            // Device isn't charging...
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

                                HashMap<String, Object> details = new HashMap<>();
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

        if (prefs.getBoolean(StreamingJacksonUploadPlugin.ENABLED, StreamingJacksonUploadPlugin.ENABLED_DEFAULT) == false)
            return;

        synchronized (this)
        {
            String action = intent.getAction();

            if (OutputPlugin.FORCE_UPLOAD.equals(action))
            {
                this._lastAttempt = 0;

                this.uploadFiles(context, prefs, 0);
            }
            else if (Probe.PROBE_READING.equals(action))
            {
                Bundle extras = intent.getExtras();

                if (extras.containsKey(DataUploadPlugin.TRANSMIT_KEY) && extras.getBoolean(DataUploadPlugin.TRANSMIT_KEY) == false)
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

                        this.uploadFiles(context, prefs, 0);

                        if (this._generator == null)
                        {
                            this._currentFile = new File(pendingFolder, now + TEMP_FILE_EXTENSION);

                            JsonFactory factory = new JsonFactory();

                            this._generator = factory.createGenerator(this._currentFile, JsonEncoding.UTF8);

                            this._generator.writeStartArray();
                        }

                        if (extras.containsKey("PRIORITY") && extras.getBoolean("PRIORITY"))
                            this._priorityPayload = true;

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

    private void closeOpenSession() throws IOException
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

        if (this._priorityPayload)
        {
            this._hasPriorityPayloads = true;

            finalFile = finalFile.replace(FILE_EXTENSION, PRIORITY_FILE_EXTENSION);
        }

        this._currentFile = null;
        this._priorityPayload = false;

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
                                Log.e("PR", "LIST OBJ: " + o.getClass().getCanonicalName() + " IN " + key);
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
                        Log.e("PR", "GOT TYPE " + value.getClass().getCanonicalName() + " FOR " + key);
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

        return 2L * 1024 * 1024 * 1024;
    }

    public boolean isEnabled(Context context)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return prefs.getBoolean(StreamingJacksonUploadPlugin.ENABLED, StreamingJacksonUploadPlugin.ENABLED_DEFAULT);
    }
}
