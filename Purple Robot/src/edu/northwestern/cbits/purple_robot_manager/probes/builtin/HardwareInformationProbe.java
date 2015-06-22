package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class HardwareInformationProbe extends Probe
{
    private static final String BOARD = "BOARD";
    private static final String BOOTLOADER = "BOOTLOADER";
    private static final String BRAND = "BRAND";
    private static final String DEVICE = "DEVICE";
    private static final String DISPLAY = "DISPLAY";
    private static final String FINGERPRINT = "FINGERPRINT";
    private static final String HARDWARE = "HARDWARE";
    private static final String HOST = "HOST";
    private static final String ID = "ID";
    private static final String MANUFACTURER = "MANUFACTURER";
    private static final String MODEL = "MODEL";
    private static final String PRODUCT = "PRODUCT";
    private static final String WIFI_MAC = "WIFI_MAC";
    private static final String BLUETOOTH_MAC = "BLUETOOTH_MAC";
    private static final String MOBILE_ID = "MOBILE_ID";

    private static final boolean DEFAULT_ENABLED = true;
    private static final String ENABLED = "config_probe_hardware_enabled";
    private static final String FREQUENCY = "config_probe_hardware_frequency";

    private long _lastCheck = 0;

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.HardwareInformationProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_hardware_info_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_device_info_category);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(HardwareInformationProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(HardwareInformationProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public boolean isEnabled(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        long now = System.currentTimeMillis();

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(HardwareInformationProbe.ENABLED, HardwareInformationProbe.DEFAULT_ENABLED))
            {
                synchronized (this)
                {
                    long freq = Long.parseLong(prefs.getString(HardwareInformationProbe.FREQUENCY,
                            Probe.DEFAULT_FREQUENCY));

                    if (now - this._lastCheck > freq)
                    {
                        Bundle bundle = new Bundle();
                        bundle.putString("PROBE", this.name(context));
                        bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                        bundle.putString(HardwareInformationProbe.BOARD, Build.BOARD);
                        bundle.putString(HardwareInformationProbe.BOOTLOADER, Build.BOOTLOADER);
                        bundle.putString(HardwareInformationProbe.BRAND, Build.BRAND);
                        bundle.putString(HardwareInformationProbe.DEVICE, Build.DEVICE);
                        bundle.putString(HardwareInformationProbe.DISPLAY, Build.DISPLAY);
                        bundle.putString(HardwareInformationProbe.FINGERPRINT, Build.FINGERPRINT);
                        bundle.putString(HardwareInformationProbe.HARDWARE, Build.HARDWARE);
                        bundle.putString(HardwareInformationProbe.HOST, Build.HOST);
                        bundle.putString(HardwareInformationProbe.ID, Build.ID);
                        bundle.putString(HardwareInformationProbe.MANUFACTURER, Build.MANUFACTURER);
                        bundle.putString(HardwareInformationProbe.MODEL, Build.MODEL);
                        bundle.putString(HardwareInformationProbe.PRODUCT, Build.PRODUCT);

                        try
                        {
                            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                            bundle.putString(HardwareInformationProbe.WIFI_MAC, wifi.getConnectionInfo()
                                    .getMacAddress());
                        }
                        catch (NullPointerException e)
                        {
                            LogManager.getInstance(context).logException(e);
                        }

                        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                        bundle.putString(HardwareInformationProbe.MOBILE_ID, tm.getDeviceId());

                        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

                        if (adapter != null)
                            bundle.putString(HardwareInformationProbe.BLUETOOTH_MAC, adapter.getAddress());
                        else
                            bundle.putString(HardwareInformationProbe.BLUETOOTH_MAC, "");

                        this.transmitData(context, bundle);

                        this._lastCheck = now;
                    }
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public Bundle formattedBundle(Context context, Bundle bundle)
    {
        Bundle formatted = super.formattedBundle(context, bundle);

        formatted.putString(context.getString(R.string.hardware_model_label),
                bundle.getString(HardwareInformationProbe.MODEL));
        formatted.putString(context.getString(R.string.hardware_mfr_label),
                bundle.getString(HardwareInformationProbe.MANUFACTURER));
        formatted.putString(context.getString(R.string.hardware_bluetooth_label),
                bundle.getString(HardwareInformationProbe.BLUETOOTH_MAC));
        formatted.putString(context.getString(R.string.hardware_wifi_label),
                bundle.getString(HardwareInformationProbe.WIFI_MAC));

        return formatted;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        String model = bundle.getString(HardwareInformationProbe.MODEL);
        String wifi = bundle.getString(HardwareInformationProbe.WIFI_MAC);

        return String.format(context.getResources().getString(R.string.summary_hardware_info_probe), model, wifi);
    }

    /*
     * public Bundle formattedBundle(Context context, Bundle bundle) { Bundle
     * formatted = super.formattedBundle(context, bundle);
     * 
     * @SuppressWarnings("unchecked") ArrayList<Bundle> array =
     * (ArrayList<Bundle>) bundle.get(HardwareInformationProbe.DEVICES); int
     * count = (int) bundle.getDouble(HardwareInformationProbe.DEVICES_COUNT);
     * 
     * Bundle devicesBundle = this.bundleForDevicesArray(context, array);
     * 
     * formatted.putBundle(String.format(context.getString(R.string.
     * display_bluetooth_devices_title), count), devicesBundle);
     * 
     * return formatted; };
     */

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString(HardwareInformationProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));

        map.put(Probe.PROBE_FREQUENCY, freq);

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(Probe.PROBE_FREQUENCY))
        {
            Object frequency = params.get(Probe.PROBE_FREQUENCY);

            if ((frequency instanceof Double) == false)
                frequency = Double.valueOf(frequency.toString()).longValue();
            else
                frequency = ((Double) frequency).longValue();

            SharedPreferences prefs = Probe.getPreferences(context);
            Editor e = prefs.edit();

            e.putString(HardwareInformationProbe.FREQUENCY, frequency.toString());
            e.commit();
        }
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_hardware_info_probe_desc);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_hardware_info_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(HardwareInformationProbe.ENABLED);
        enabled.setDefaultValue(HardwareInformationProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleListPreference duration = new FlexibleListPreference(context);
        duration.setKey(HardwareInformationProbe.FREQUENCY);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);
        duration.setEntryValues(R.array.probe_low_frequency_values);
        duration.setEntries(R.array.probe_low_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);

        screen.addPreference(duration);

        return screen;
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = new JSONObject();

        try
        {
            JSONObject enabled = new JSONObject();
            enabled.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            JSONArray values = new JSONArray();
            values.put(true);
            values.put(false);
            enabled.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_ENABLED, enabled);

            JSONObject frequency = new JSONObject();
            frequency.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
            values = new JSONArray();

            String[] options = context.getResources().getStringArray(R.array.probe_low_frequency_values);

            for (String option : options)
            {
                values.put(Long.parseLong(option));
            }

            frequency.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_FREQUENCY, frequency);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    public String assetPath(Context context)
    {
        return "hardware-info-probe.html";
    }
}
