package edu.northwestern.cbits.purple_robot_manager.plugins;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager.RunningTaskInfo;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

public abstract class OutputPlugin
{
    private final ArrayList<Intent> _pendingIntents = new ArrayList<>();

    private boolean _isProcessing = false;

    public static final String PAYLOAD = "edu.northwestern.cbits.purple_robot.OUTPUT_EVENT_PLUGIN";
    public static final String OUTPUT_EVENT = "edu.northwestern.cbits.purple_robot.OUTPUT_EVENT";
    public static final String LOG_EVENT = "edu.northwestern.cbits.purple_robot.LOG_EVENT";
    public static final String FORCE_UPLOAD = "edu.northwestern.cbits.purple_robot.FORCE_UPLOAD";
    public static final String DISPLAY_MESSAGE = "edu.northwestern.cbits.purple_robot.DISPLAY_MESSAGE";

    public static final String USE_EXTERNAL_STORAGE = "config_external_storage";

    private static List<Class<OutputPlugin>> _pluginClasses = new ArrayList<>();

    public abstract String[] respondsTo();

    public abstract void processIntent(Intent intent);

    private Context _context = null;

    public void setContext(Context context)
    {
        this._context = context;
    }

    public Context getContext()
    {
        return this._context;
    }

    protected void broadcastMessage(String message, boolean log)
    {
        Intent displayIntent = new Intent(OutputPlugin.DISPLAY_MESSAGE);
        displayIntent.putExtra("MESSAGE", message);

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this.getContext());
        manager.sendBroadcast(displayIntent);

        if (log)
        {
            HashMap<String, Object> payload = new HashMap<>();
            payload.put("message", message);

            LogManager.getInstance(this.getContext()).log("broadcast_message", payload);
        }
    }

    public boolean shouldRespond(String intentAction)
    {
        String[] actions = this.respondsTo();

        for (String action : actions)
        {
            if (action.equalsIgnoreCase(intentAction))
                return true;
        }

        return false;
    }


    @SuppressWarnings("unchecked")
    public static void loadPluginClasses(Context context)
    {
        String packageName = OutputPlugin.class.getPackage().getName();

        String[] probeClasses = context.getResources().getStringArray(R.array.output_plugin_classes);

        IntentFilter intentFilter = new IntentFilter();

        for (String className : probeClasses)
        {
            try
            {
                @SuppressWarnings("rawtypes")
                Class pluginClass = Class.forName(packageName + "." + className);

                OutputPlugin.registerPluginClass(pluginClass);

                OutputPlugin plugin = (OutputPlugin) pluginClass.newInstance();

                Method method = pluginClass.getDeclaredMethod("respondsTo");

                String[] actions = (String[]) method.invoke(plugin);

                for (String action : actions)
                {
                    if (intentFilter.hasAction(action) == false)
                        intentFilter.addAction(action);
                }
            }
            catch (ClassNotFoundException | InstantiationException | InvocationTargetException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException e)
            {
                LogManager.getInstance(context).logException(e);
            }
        }

        intentFilter.addAction(OutputPlugin.OUTPUT_EVENT);

        LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
        localManager.registerReceiver(OutputPluginManager.sharedInstance, intentFilter);
    }

    @SuppressWarnings("rawtypes")
    public static void registerPluginClass(Class<OutputPlugin> pluginClass)
    {
        if (!OutputPlugin._pluginClasses.contains(pluginClass))
            OutputPlugin._pluginClasses.add(pluginClass);
    }

    @SuppressWarnings("rawtypes")
    public static List<Class<OutputPlugin>> availablePluginClasses()
    {
        return OutputPlugin._pluginClasses;
    }

    public void process(final Intent intent)
    {
        if (this.shouldRespond(intent.getAction()))
        {
            synchronized (this._pendingIntents)
            {
                this._pendingIntents.add(intent);
            }

            if (this._isProcessing == false)
            {
                final OutputPlugin me = this;

                Runnable r = new Runnable()
                {
                    public void run()
                    {
                        me._isProcessing = true;

                        while (me._pendingIntents.size() > 0)
                        {
                            Intent nextIntent = null;

                            synchronized (me._pendingIntents)
                            {
                                if (me._pendingIntents.size() > 0)
                                    nextIntent = me._pendingIntents.remove(0);
                            }

                            if (nextIntent != null)
                                me.processIntent(nextIntent);
                        }

                        me._isProcessing = false;
                    }
                };

                try
                {
                    Thread t = new Thread(r);
                    t.start();
                }
                catch (OutOfMemoryError e)
                {
                    LogManager.getInstance(this._context).logException(e);
                }
            }
        }
    }

    public static Map<String, Object> getValues(final Bundle bundle)
    {
        HashMap<String, Object> values = new HashMap<>();

        if (bundle == null)
            return values;

        try
        {
            for (String key : bundle.keySet())
            {
                values.put(key, bundle.get(key));
            }
        }
        catch (BadParcelableException e)
        {
            e.printStackTrace();
        }

        return values;
    }

    public static JSONObject jsonForBundle(Bundle bundle) throws JSONException
    {
        JSONObject json = new JSONObject();

        Map<String, Object> values = OutputPlugin.getValues(bundle);

        for (String key : values.keySet())
        {
            Object value = values.get(key);

            if (value == null || key == null)
            {
                // Skip
            }
            else if (value instanceof String)
                json.put(key, value);
            else if (value instanceof Bundle)
            {
                value = OutputPlugin.jsonForBundle((Bundle) value);

                json.put(key, value);
            }
            else if (value instanceof float[])
            {
                float[] floats = (float[]) value;

                JSONArray floatArray = new JSONArray();

                for (float f : floats)
                {
                    floatArray.put((double) f);
                }

                json.put(key, floatArray);
            }
            else if (value instanceof int[])
            {
                int[] ints = (int[]) value;

                JSONArray intArray = new JSONArray();

                for (int i : ints)
                {
                    intArray.put(i);
                }

                json.put(key, intArray);
            }
            else if (value instanceof long[])
            {
                long[] longs = (long[]) value;

                JSONArray longArray = new JSONArray();

                for (long l : longs)
                {
                    longArray.put(l);
                }

                json.put(key, longArray);
            }
            else if (value instanceof double[])
            {
                double[] doubles = (double[]) value;

                JSONArray doubleArray = new JSONArray();

                for (double d : doubles)
                {
                    doubleArray.put(d);
                }

                json.put(key, doubleArray);
            }
            else if (value instanceof Float)
            {
                Float f = (Float) value;

                json.put(key, f.doubleValue());
            }
            else if (value instanceof Integer)
            {
                Integer i = (Integer) value;

                json.put(key, i.intValue());
            }
            else if (value instanceof Long)
            {
                Long l = (Long) value;

                json.put(key, l.longValue());
            }
            else if (value instanceof Boolean)
            {
                Boolean b = (Boolean) value;

                json.put(key, b.booleanValue());
            }
            else if (value instanceof Short)
            {
                Short s = (Short) value;

                json.put(key, s.intValue());
            }
            else if (value instanceof Double)
            {
                Double d = (Double) value;

                if (d.isInfinite())
                    json.put(key, Double.MAX_VALUE);
                else
                    json.put(key, d.doubleValue());
            }
            else if (value instanceof List)
            {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;

                JSONArray objectArray = new JSONArray();

                for (Object o : list)
                {
                    if (o instanceof Bundle)
                        objectArray.put(OutputPlugin.jsonForBundle((Bundle) o));
                    else if (o instanceof ScanResult)
                    {
                        ScanResult s = (ScanResult) o;

                        JSONObject scanObject = new JSONObject();

                        if (s.BSSID != null)
                            scanObject.put("BSSID", s.BSSID);

                        if (s.SSID != null)
                            scanObject.put("SSID", s.SSID);

                        if (s.capabilities != null)
                            scanObject.put("Capabilities", s.capabilities);

                        scanObject.put("Frequency", s.frequency);
                        scanObject.put("Level dBm", s.level);

                        objectArray.put(scanObject);
                    }
                    else if (o instanceof RunningTaskInfo)
                    {
                        RunningTaskInfo r = (RunningTaskInfo) o;

                        JSONObject taskObject = new JSONObject();

                        if (r.baseActivity != null)
                            taskObject.put("Base Activity", r.baseActivity.getPackageName());

                        if (r.description != null)
                            taskObject.put("Description", r.description.toString());

                        taskObject.put("Activity Count", r.numActivities);
                        taskObject.put("Running Activity Count", r.numRunning);

                        objectArray.put(taskObject);
                    }
                    else if (o instanceof ApplicationInfo)
                    {
                        ApplicationInfo a = (ApplicationInfo) o;

                        objectArray.put(a.packageName);
                    }
                    else if (o instanceof Location)
                    {
                        Location l = (Location) o;

                        JSONObject locObject = new JSONObject();

                        locObject.put("Accuracy", l.getAccuracy());
                        locObject.put("Altitude", l.getAltitude());
                        locObject.put("Bearing", l.getBearing());
                        locObject.put("Latitude", l.getLatitude());
                        locObject.put("Longitude", l.getLongitude());

                        if (l.getProvider() != null)
                            locObject.put("Provider", l.getProvider());
                        else
                            locObject.put("Provider", "Unknown");

                        locObject.put("Speed", l.getSpeed());
                        locObject.put("Timestamp", l.getTime());

                        objectArray.put(locObject);
                    }
                    else if (o instanceof String)
                        objectArray.put(o.toString());
                    else
                        Log.e("PR", "LIST OBJ: " + o.getClass().getCanonicalName() + " IN " + key);
                }

                json.put(key, objectArray);
            }
            else if (value instanceof Location)
            {
                Location l = (Location) value;

                JSONObject locObject = new JSONObject();

                locObject.put("Accuracy", l.getAccuracy());
                locObject.put("Altitude", l.getAltitude());
                locObject.put("Bearing", l.getBearing());
                locObject.put("Latitude", l.getLatitude());
                locObject.put("Longitude", l.getLongitude());

                if (l.getProvider() != null)
                    locObject.put("Provider", l.getProvider());
                else
                    locObject.put("Provider", "Unknown");

                locObject.put("Speed", l.getSpeed());
                locObject.put("Timestamp", l.getTime());

                json.put(key, locObject);
            }
            else if (value instanceof BluetoothClass)
            {
                BluetoothClass btClass = (BluetoothClass) value;

                json.put(key, btClass.toString());
            }
            else if (value instanceof BluetoothDevice)
            {
                BluetoothDevice device = (BluetoothDevice) value;

                JSONObject deviceObject = new JSONObject();

                if (device.getBondState() == BluetoothDevice.BOND_BONDED)
                    deviceObject.put("Bond State", "Bonded");
                if (device.getBondState() == BluetoothDevice.BOND_BONDING)
                    deviceObject.put("Bond State", "Bonding");
                else
                    deviceObject.put("Bond State", "None");

                deviceObject.put("Device Address", device.getAddress());
                deviceObject.put("Device Class", device.getBluetoothClass().toString());

                json.put(key, deviceObject);
            }
            else
                Log.e("PRM", "GOT TYPE " + value.getClass().getCanonicalName() + " FOR " + key);
        }

        // String jsonString = JsonUtils.getGson().toJson(values);

        return json;
    }

    @SuppressWarnings("rawtypes")
    public static Bundle bundleForJson(final Context context, String jsonString)
    {
        Bundle bundle = new Bundle();

        try
        {
            JSONObject json = new JSONObject(jsonString);

            Iterator keys = json.keys();

            while (keys.hasNext())
            {
                String key = keys.next().toString();

                Object o = json.get(key);

                if (o instanceof Double)
                    bundle.putDouble(key, (Double) o);
                else if (o instanceof Float)
                    bundle.putDouble(key, ((Float) o).doubleValue());
                else if (o instanceof Long)
                    bundle.putDouble(key, ((Long) o).doubleValue());
                else if (o instanceof Integer)
                    bundle.putDouble(key, ((Integer) o).doubleValue());
                else if (o instanceof Boolean)
                    bundle.putBoolean(key, (Boolean) o);
                else if (o instanceof String)
                    bundle.putString(key, o.toString());
                else if (o instanceof JSONArray)
                {
                    JSONArray child = (JSONArray) o;

                    if (child.length() > 0)
                    {
                        Object nextChild = child.get(0);

                        if (nextChild instanceof String)
                        {
                            String[] strings = new String[child.length()];

                            for (int i = 0; i < strings.length; i++)
                                strings[i] = child.getString(i);

                            bundle.putStringArray(key, strings);
                        }
                        else if (nextChild instanceof Double)
                        {
                            double[] doubles = new double[child.length()];

                            for (int i = 0; i < doubles.length; i++)
                                doubles[i] = child.getDouble(i);

                            bundle.putDoubleArray(key, doubles);
                        }
                        else if (nextChild instanceof Integer)
                        {
                            double[] doubles = new double[child.length()];

                            for (int i = 0; i < doubles.length; i++)
                                doubles[i] = child.getDouble(i);

                            bundle.putDoubleArray(key, doubles);
                        }
                        else if (nextChild instanceof Long)
                        {
                            double[] doubles = new double[child.length()];

                            for (int i = 0; i < doubles.length; i++)
                                doubles[i] = child.getDouble(i);

                            bundle.putDoubleArray(key, doubles);
                        }

                        else if (nextChild instanceof JSONObject)
                        {
                            ArrayList<Bundle> bundles = new ArrayList<>();

                            for (int i = 0; i < child.length(); i++)
                                bundles.add(OutputPlugin.bundleForJson(context, child.getJSONObject(i).toString()));

                            bundle.putParcelableArrayList(key, bundles);
                        }
                    }
                }
                else if (o instanceof JSONObject)
                {
                    JSONObject child = (JSONObject) o;
                    bundle.putBundle(key, OutputPlugin.bundleForJson(context, child.toString()));
                }
            }
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return bundle;
    }
}
