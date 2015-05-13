package edu.northwestern.cbits.purple_robot_manager.plugins;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

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
import android.util.JsonWriter;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

// NOTE: This plugin is included for the purposes of comparison between 
// Android's streaming JSON classes and Jackson. The Jackson class is the one
// under continual development and should be used instead of this one.

@SuppressLint("NewApi")
public class StreamingJSONUploadPlugin extends DataUploadPlugin
{
    private final static String FILE_EXTENSION = ".streaming";

    private static final String ENABLED = "config_enable_streaming_json_data_server";

    private JsonWriter _writer = null;

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

        if (now - this._lastAttempt < 300000)
            return;

        this._lastAttempt = now;

        final StreamingJSONUploadPlugin me = this;

        Runnable r = new Runnable()
        {
            public void run()
            {
                try
                {
                    File pendingFolder = me.getPendingFolder();

                    String[] filenames = pendingFolder.list(new FilenameFilter()
                    {
                        public boolean accept(File dir, String filename)
                        {
                            return filename.endsWith(StreamingJSONUploadPlugin.FILE_EXTENSION);
                        }
                    });

                    if (filenames == null)
                        filenames = new String[0];

                    if (filenames.length < 2)
                        return;

                    SecureRandom random = new SecureRandom();

                    int index = random.nextInt(filenames.length - 1);

                    File payloadFile = new File(pendingFolder, filenames[index]);
                    String payload = FileUtils.readFileToString(payloadFile);

                    if (me.transmitPayload(prefs, payload) == DataUploadPlugin.RESULT_SUCCESS)
                    {
                        payloadFile.delete();

                        me._lastAttempt = 0;
                        me.uploadFiles(context, prefs);
                    }
                }
                catch (IOException e)
                {
                    LogManager.getInstance(context).logException(e);
                    me.broadcastMessage(context.getString(R.string.message_general_error), true);
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

        if (prefs.getBoolean(StreamingJSONUploadPlugin.ENABLED, false) == false)
            return;

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
                return;

            long now = System.currentTimeMillis();

            try
            {
                if (this._currentFile != null)
                {
                    File f = this._currentFile.getAbsoluteFile();

                    long length = f.length();
                    long modDelta = now - f.lastModified();

                    // TODO: Make configurable...
                    if (this._writer != null && (length > 262144 || modDelta > 60000))
                    {
                        this._writer.endArray();
                        this._writer.flush();
                        this._writer.close();

                        this._writer = null;

                        this._currentFile = null;
                    }
                }

                this.uploadFiles(context, prefs);

                if (this._writer == null)
                {
                    this._currentFile = new File(this.getPendingFolder(), now + FILE_EXTENSION);

                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(this._currentFile));
                    this._writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));

                    this._writer.beginArray();
                }

                StreamingJSONUploadPlugin.writeBundle(this.getContext(), this._writer, extras);
                this._writer.flush();
            }
            catch (IOException e)
            {
                LogManager.getInstance(this.getContext()).logException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void writeBundle(Context context, JsonWriter writer, Bundle bundle)
    {
        try
        {
            writer.beginObject();

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
                    writer.name(key);

                    if (value instanceof String)
                    {
                        writer.value((String) value);
                    }
                    else if (value instanceof float[])
                    {
                        float[] floats = (float[]) value;

                        writer.beginArray();

                        for (float f : floats)
                            writer.value(f);

                        writer.endArray();
                    }
                    else if (value instanceof int[])
                    {
                        int[] ints = (int[]) value;

                        writer.beginArray();

                        for (int i : ints)
                            writer.value(i);

                        writer.endArray();
                    }
                    else if (value instanceof long[])
                    {
                        long[] longs = (long[]) value;

                        writer.beginArray();

                        for (long l : longs)
                            writer.value(l);

                        writer.endArray();
                    }
                    else if (value instanceof double[])
                    {
                        double[] doubles = (double[]) value;

                        writer.beginArray();

                        for (double d : doubles)
                            writer.value(d);

                        writer.endArray();
                    }
                    else if (value instanceof Float)
                    {
                        Float f = (Float) value;

                        writer.value(f);
                    }
                    else if (value instanceof Integer)
                    {
                        Integer i = (Integer) value;

                        writer.value(i);
                    }
                    else if (value instanceof Long)
                    {
                        Long l = (Long) value;

                        writer.value(l);
                    }
                    else if (value instanceof Boolean)
                    {
                        Boolean b = (Boolean) value;

                        writer.value(b);
                    }
                    else if (value instanceof Short)
                    {
                        Short s = (Short) value;

                        writer.value(s);
                    }
                    else if (value instanceof Double)
                    {
                        Double d = (Double) value;

                        if (d.isInfinite())
                            writer.value(Double.MAX_VALUE);
                        else
                            writer.value(d);
                    }
                    else if (value instanceof List)
                    {
                        List<Object> list = (List<Object>) value;

                        writer.beginArray();

                        for (Object o : list)
                        {
                            if (o instanceof String)
                                writer.value(o.toString());
                            else if (o instanceof Bundle)
                                StreamingJSONUploadPlugin.writeBundle(context, writer, (Bundle) o);
                            else if (o instanceof ScanResult)
                            {
                                ScanResult s = (ScanResult) o;

                                writer.beginObject();

                                if (s.BSSID != null)
                                    writer.name("BSSID").value(s.BSSID);

                                if (s.SSID != null)
                                    writer.name("SSID").value(s.SSID);

                                if (s.capabilities != null)
                                    writer.name("Capabilities").value(s.capabilities);

                                writer.name("Frequency").value(s.frequency);
                                writer.name("Level dBm").value(s.level);

                                writer.endObject();
                            }
                            else if (o instanceof RunningTaskInfo)
                            {
                                RunningTaskInfo r = (RunningTaskInfo) o;

                                writer.beginObject();

                                if (r.baseActivity != null)
                                    writer.name("Base Activity").value(r.baseActivity.getPackageName());

                                if (r.description != null)
                                    writer.name("Description").value(r.description.toString());

                                writer.name("Activity Count").value(r.numActivities);
                                writer.name("Running Activity Count").value(r.numRunning);

                                writer.endObject();
                            }
                            else if (o instanceof ApplicationInfo)
                            {
                                ApplicationInfo a = (ApplicationInfo) o;

                                writer.value(a.packageName);
                            }
                            else if (o instanceof Location)
                            {
                                Location l = (Location) o;

                                writer.beginObject();

                                writer.name("Accuracy").value(l.getAccuracy());
                                writer.name("Altitude").value(l.getAltitude());
                                writer.name("Bearing").value(l.getBearing());
                                writer.name("Latitude").value(l.getLatitude());
                                writer.name("Longitude").value(l.getLongitude());

                                if (l.getProvider() != null)
                                    writer.name("Provider").value(l.getProvider());
                                else
                                    writer.name("Provider").value("Unknown");

                                writer.name("Speed").value(l.getSpeed());
                                writer.name("Timestamp").value(l.getTime());

                                writer.endObject();
                            }
                            else
                                Log.e("PR", "LIST OBJ: " + o.getClass().getCanonicalName() + " IN " + key);
                        }

                        writer.endArray();
                    }
                    else if (value instanceof Location)
                    {
                        Location l = (Location) value;

                        writer.beginObject();

                        writer.name("Accuracy").value(l.getAccuracy());
                        writer.name("Altitude").value(l.getAltitude());
                        writer.name("Bearing").value(l.getBearing());
                        writer.name("Latitude").value(l.getLatitude());
                        writer.name("Longitude").value(l.getLongitude());

                        if (l.getProvider() != null)
                            writer.name("Provider").value(l.getProvider());
                        else
                            writer.name("Provider").value("Unknown");

                        writer.name("Speed").value(l.getSpeed());
                        writer.name("Timestamp").value(l.getTime());

                        writer.endObject();
                    }
                    else if (value instanceof BluetoothClass)
                    {
                        BluetoothClass btClass = (BluetoothClass) value;

                        writer.value(btClass.toString());
                    }
                    else if (value instanceof BluetoothDevice)
                    {
                        BluetoothDevice device = (BluetoothDevice) value;

                        writer.beginObject();

                        if (device.getBondState() == BluetoothDevice.BOND_BONDED)
                            writer.name("Bond State").value("Bonded");
                        else if (device.getBondState() == BluetoothDevice.BOND_BONDING)
                            writer.name("Bond State").value("Bonding");
                        else
                            writer.name("Bond State").value("None");

                        writer.name("Device Address").value(device.getAddress());
                        writer.name("Device Class").value(device.getBluetoothClass().toString());

                        writer.endObject();
                    }
                    else if (value instanceof Bundle)
                        StreamingJSONUploadPlugin.writeBundle(context, writer, (Bundle) value);
                    else
                        Log.e("PR", "GOT TYPE " + value.getClass().getCanonicalName() + " FOR " + key);
                }
            }

            writer.endObject();
        }
        catch (IOException e)
        {
            LogManager.getInstance(context).logException(e);
        }
    }

    public boolean isEnabled(Context context)
    {
        return false;
    }
}
