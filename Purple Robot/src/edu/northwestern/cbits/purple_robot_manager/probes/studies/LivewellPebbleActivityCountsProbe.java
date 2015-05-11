package edu.northwestern.cbits.purple_robot_manager.probes.studies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.calibration.PebbleCalibrationHelper;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class LivewellPebbleActivityCountsProbe extends Probe
{
    private static final String FIRMWARE_VERSION = "FIRMWARE_VERSION";
    private static final byte COMMAND_FLUSH_BUFFER = 0x00;

    private static final String BUNDLE_NUM_SAMPLES = "BUNDLE_NUM_SAMPLES";
    private static final String BUNDLE_BATTERY_LEVEL = "BUNDLE_BATTERY_LEVEL";
    private static final String BUNDLE_IS_CHARGING = "BUNDLE_IS_CHARGING";
    private static final String FREQUENCY = "config_probe_livewell_pebble_frequency";
    private static final String BUNDLE_DIFF_MEANS = "BUNDLE_DIFF_MEANS";
    private static final int CURRENT_VERSION = 0x03;

    private static UUID WATCHAPP_UUID = UUID.fromString("09e5f53c-651e-408a-8b10-3b5b0e1b6b09");

    public static final String ENABLED = "config_probe_livewell_pebble_enabled";
    public static final boolean DEFAULT_ENABLED = false;

    private PebbleKit.PebbleDataReceiver _receiver = null;
    private PebbleKit.PebbleNackReceiver _nackReceiver = null;
    private PebbleKit.PebbleAckReceiver _ackReceiver = null;

    private long _lastRefresh = 0;
    private ArrayList<Bundle> _pendingReadings = new ArrayList<Bundle>();
    private boolean _isTransmitting = false;

    private static class ActivityCount
    {
        // Inspired by https://github.com/kramimus/pebble-accel-analyzer

        private long start = 0;
        private short numSamples = 0;
        private int mean = 0;
        private byte battery = 0;

        public ActivityCount(byte[] data)
        {
            for (int i = 0; i < 8; i++)
            {
                this.start |= ((long) (data[i] & 0xff)) << (i * 8);
            }

            for (int i = 0; i < 2; i++)
            {
                this.numSamples |= ((short) (data[i + 8] & 0xff)) << (i * 8);
            }

            for (int i = 0; i < 4; i++)
            {
                this.mean |= (data[i + 10] & 0xff) << (i * 8);
            }

            this.battery = data[14];
        }

        public int mean()
        {
            return this.mean;
        }

        @SuppressWarnings("unused")
        public JSONObject toJson(Context context)
        {
            JSONObject json = new JSONObject();

            try
            {
                json.put("start", this.start);
                json.put("num_samples", this.numSamples);
                json.put("mean", this.mean);
                json.put("battery_level", this.batteryLevel());
                json.put("is_charging", this.isCharging());

                return json;
            }
            catch (JSONException e)
            {
                LogManager.getInstance(context).logException(e);
            }

            return null;
        }

        public boolean isCharging()
        {
            return (this.battery & 0x80) == 0x80;
        }

        public int batteryLevel()
        {
            return (int) this.battery & 0x7f;
        }

        public long start()
        {
            return this.start;
        }

        public int numSamples()
        {
            return this.numSamples;
        }

        public void applyTimezone(TimeZone tz)
        {
            this.start -= tz.getOffset(this.start);
        }
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.studies.LivewellPebbleActivityCountsProbe";
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_studies_category);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(final Context context, PreferenceManager manager)
    {
        final LivewellPebbleActivityCountsProbe me = this;

        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_livewell_pebble_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(LivewellPebbleActivityCountsProbe.ENABLED);
        enabled.setDefaultValue(LivewellPebbleActivityCountsProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleListPreference duration = new FlexibleListPreference(context);
        duration.setKey(LivewellPebbleActivityCountsProbe.FREQUENCY);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);
        duration.setEntryValues(R.array.probe_satellite_frequency_values);
        duration.setEntries(R.array.probe_satellite_frequency_labels);
        duration.setTitle(R.string.probe_fetch_rate_label);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.contains(LivewellPebbleActivityCountsProbe.FREQUENCY))
        {
            try
            {
                prefs.getString(LivewellPebbleActivityCountsProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY);
            }
            catch (ClassCastException e)
            {
                long freq = prefs.getLong(LivewellPebbleActivityCountsProbe.FREQUENCY, 0);

                Editor ed = prefs.edit();
                ed.putString(LivewellPebbleActivityCountsProbe.FREQUENCY, "" + freq);
                ed.commit();
            }
        }

        screen.addPreference(duration);

        Preference fetchNow = new Preference(context);
        fetchNow.setTitle(R.string.action_request_data);
        fetchNow.setSummary(R.string.action_desc_request_data);

        fetchNow.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                me._lastRefresh = 0;

                me.isEnabled(context);

                return true;
            }
        });

        screen.addPreference(fetchNow);

        Preference installWatchApp = new Preference(context);
        installWatchApp.setTitle(R.string.probe_livewell_pebble_install_label);
        installWatchApp.setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.probe_livewell_pebble_install_url))));

        screen.addPreference(installWatchApp);

        return screen;
    }

    public String getMainScreenAction(Context context)
    {
        return context.getString(R.string.action_request_data_no_now);
    }

    public void runMainScreenAction(Context context)
    {
        this._lastRefresh = 0;

        this.isEnabled(context);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(LivewellPebbleActivityCountsProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(LivewellPebbleActivityCountsProbe.ENABLED, false);

        e.commit();
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

            String[] options = context.getResources().getStringArray(R.array.probe_satellite_frequency_values);

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

    @Override
    public boolean isEnabled(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(LivewellPebbleActivityCountsProbe.ENABLED, LivewellPebbleActivityCountsProbe.DEFAULT_ENABLED))
            {
                PebbleCalibrationHelper.check(context, true);

                PebbleKit.startAppOnPebble(context, LivewellPebbleActivityCountsProbe.WATCHAPP_UUID);

                if (this._receiver == null)
                {
                    final LivewellPebbleActivityCountsProbe me = this;

                    this._receiver =  new PebbleKit.PebbleDataReceiver(LivewellPebbleActivityCountsProbe.WATCHAPP_UUID)
                    {
                        public void receiveData(final Context context, final int transactionId, final PebbleDictionary dictionary)
                        {
                            PebbleKit.sendAckToPebble(context, transactionId);

                            SanityManager sanity = SanityManager.getInstance(context);
                            String obsoleteTitle = me.title(context);

                            byte[] payload = dictionary.getBytes(LivewellPebbleActivityCountsProbe.CURRENT_VERSION);

                            if (payload != null && payload.length <= 15)
                            {
                                ActivityCount count = new ActivityCount(payload);

                                TimeZone here = Calendar.getInstance().getTimeZone();
                                count.applyTimezone(here);

                                Bundle data = new Bundle();
                                data.putDouble(Probe.BUNDLE_TIMESTAMP, count.start() / 1000);
                                data.putString(Probe.BUNDLE_PROBE, me.name(context));

                                PebbleKit.FirmwareVersionInfo info = PebbleKit.getWatchFWVersion(context);

                                if (info != null)
                                    data.putString(LivewellPebbleActivityCountsProbe.FIRMWARE_VERSION, "" + info.getMajor() + "." + info.getMinor() + "." + info.getPoint());

                                data.putInt(LivewellPebbleActivityCountsProbe.BUNDLE_NUM_SAMPLES, count.numSamples());
                                data.putInt(LivewellPebbleActivityCountsProbe.BUNDLE_DIFF_MEANS, count.mean());
                                data.putInt(LivewellPebbleActivityCountsProbe.BUNDLE_BATTERY_LEVEL, count.batteryLevel());
                                data.putBoolean(LivewellPebbleActivityCountsProbe.BUNDLE_IS_CHARGING, count.isCharging());

                                synchronized (me._pendingReadings) {
                                    me._pendingReadings.add(data);
                                }

                                Runnable r = new Runnable() {
                                    // Delay immediate transmission so display system can keep up - one reading per second.

                                    public void run() {
                                        if (me._isTransmitting)
                                            return;

                                        me._isTransmitting = true;

                                        while (me._pendingReadings.size() > 0) {
                                            synchronized (me._pendingReadings) {
                                                if (me._pendingReadings.size() > 0)
                                                    me.transmitData(context, me._pendingReadings.get(0));

                                                me._pendingReadings.remove(0);
                                            }

                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {
                                                LogManager.getInstance(context).logException(e);
                                            }
                                        }

                                        me._isTransmitting = false;
                                    }
                                };

                                Thread t = new Thread(r);
                                t.start();

                                sanity.clearAlert(obsoleteTitle);
                            }
                            else
                            {
                                String obsoleteWarning = context.getString(R.string.message_warning_obsolete_livewell_watchface);
                                final Context appContext = context.getApplicationContext();

                                Runnable r = new Runnable()
                                {
                                    public void run()
                                    {
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(appContext.getString(R.string.probe_livewell_pebble_install_url)));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                        appContext.startActivity(intent);
                                    }
                                };

                                sanity.addAlert(SanityCheck.WARNING, obsoleteTitle, obsoleteWarning, r);
                            }
                        }
                    };

                    PebbleKit.registerReceivedDataHandler(context, this._receiver);
                }

                long now = System.currentTimeMillis();

                long freq = 0;

                try
                {
                    freq = Long.parseLong(prefs.getString(LivewellPebbleActivityCountsProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
                }
                catch (ClassCastException e)
                {
                    freq = prefs.getLong(LivewellPebbleActivityCountsProbe.FREQUENCY, Long.parseLong(Probe.DEFAULT_FREQUENCY));
                }

                if (now - this._lastRefresh > freq)
                {
                    this._lastRefresh = now;

                    if (this._ackReceiver == null)
                    {
                        this._ackReceiver = new PebbleKit.PebbleAckReceiver(LivewellPebbleActivityCountsProbe.WATCHAPP_UUID)
                        {
                            public void receiveAck(Context context, int i)
                            {
//                                Log.e("PR", "RECV ACK: " + i);
                            }
                        };

                        PebbleKit.registerReceivedAckHandler(context, this._ackReceiver);
                    }

                    if (this._nackReceiver == null)
                    {
                        this._nackReceiver = new PebbleKit.PebbleNackReceiver(LivewellPebbleActivityCountsProbe.WATCHAPP_UUID)
                        {
                            public void receiveNack(Context context, int i)
                            {
//                                Log.e("PR", "RECV NAK: " + i);
                            }
                        };

                        PebbleKit.registerReceivedNackHandler(context, this._nackReceiver);
                    }

                    PebbleDictionary data = new PebbleDictionary();
                    data.addUint8(0x00, LivewellPebbleActivityCountsProbe.COMMAND_FLUSH_BUFFER);

                    PebbleKit.sendDataToPebble(context, LivewellPebbleActivityCountsProbe.WATCHAPP_UUID, data);
                }

                return true;
            }
        }

        PebbleCalibrationHelper.check(context, false);

        if (this._receiver != null)
        {
            try
            {
                context.unregisterReceiver(this._receiver);
            }
            catch (IllegalArgumentException e)
            {
                // Do nothing - receiver not registered...
            }

            this._receiver = null;
        }

        if (this._nackReceiver != null)
        {
            try
            {
                context.unregisterReceiver(this._nackReceiver);
            }
            catch (IllegalArgumentException e)
            {
                // Do nothing - receiver not registered...
            }

            this._nackReceiver = null;
        }

        if (this._ackReceiver != null)
        {
            try
            {
                context.unregisterReceiver(this._ackReceiver);
            }
            catch (IllegalArgumentException e)
            {
                // Do nothing - receiver not registered...
            }

            this._ackReceiver = null;
        }

        PebbleCalibrationHelper.check(context, false);

        return false;
    }

    public String summary(Context context)
    {
        return context.getString(R.string.summary_livewell_pebble_probe_desc);
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_livewell_pebble_probe);
    }


    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double count = bundle.getDouble(LivewellPebbleActivityCountsProbe.BUNDLE_DIFF_MEANS);
        double numSamples = bundle.getDouble(LivewellPebbleActivityCountsProbe.BUNDLE_NUM_SAMPLES);

        return String.format(context.getResources().getString(R.string.summary_livewell_pebble_probe), numSamples, count);
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(Probe.PROBE_FREQUENCY))
        {
            Object frequency = params.get(Probe.PROBE_FREQUENCY);

            if (frequency instanceof Double)
            {
                frequency = Long.valueOf(((Double) frequency).longValue());
            }

            if (frequency instanceof Long)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(LivewellPebbleActivityCountsProbe.FREQUENCY, frequency.toString());
                e.commit();
            }
        }
    }
}
