package edu.northwestern.cbits.purple_robot_manager.probes.devices;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.calibration.PebbleCalibrationHelper;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class AsyncPebbleProbe extends Probe
{
    private static final String FIRMWARE_VERSION = "FIRMWARE_VERSION";
    private static final byte COMMAND_FLUSH_BUFFER = 0x00;

    private static final String BUNDLE_DURATION = "BUNDLE_DURATION";
    private static final String BUNDLE_NUM_SAMPLES = "BUNDLE_NUM_SAMPLES";
    private static final String BUNDLE_NUM_DUPLICATES = "BUNDLE_NUM_DUPLICATES";
    private static final String BUNDLE_X_DELTA = "BUNDLE_X_DELTA";
    private static final String BUNDLE_Y_DELTA = "BUNDLE_Y_DELTA";
    private static final String BUNDLE_Z_DELTA = "BUNDLE_Z_DELTA";
    private static final String BUNDLE_ALL_DELTA = "BUNDLE_ALL_DELTA";

    private static UUID WATCHAPP_UUID = UUID.fromString("09e5f53c-651e-408a-8b10-3b5b0e1b6b09");

    private static final String ENABLED = "config_probe_pebble_pull_enabled";
    private static final boolean DEFAULT_ENABLED = false;

    private PebbleKit.PebbleDataReceiver _receiver = null;
    private PebbleKit.PebbleNackReceiver _nackReceiver = null;
    private PebbleKit.PebbleAckReceiver _ackReceiver = null;

    private long _lastRefresh = 0;

    private static class ActivityCount
    {
        // Inspired by https://github.com/kramimus/pebble-accel-analyzer

        private long start = 0;
        private long duration = 0;

        private short numSamples = 0;
        private short numDuplicates = 0;

        private int xDelta = 0;
        private int yDelta = 0;
        private int zDelta = 0;

        public ActivityCount(byte[] data)
        {
            for (int i = 0; i < 8; i++)
            {
                this.start |= ((long) (data[i] & 0xff)) << (i * 8);
            }

            for (int i = 0; i < 8; i++)
            {
                this.duration |= ((long) (data[i + 8] & 0xff)) << (i * 8);
            }

            for (int i = 0; i < 2; i++)
            {
                this.numSamples |= ((long) (data[i + 16] & 0xff)) << (i * 8);
            }

            for (int i = 0; i < 2; i++)
            {
                this.numDuplicates |= ((long) (data[i + 18] & 0xff)) << (i * 8);
            }

            for (int i = 0; i < 4; i++)
            {
                this.xDelta |= ((long) (data[i + 20] & 0xff)) << (i * 8);
            }

            for (int i = 0; i < 4; i++)
            {
                this.yDelta |= ((long) (data[i + 24] & 0xff)) << (i * 8);
            }

            for (int i = 0; i < 4; i++)
            {
                this.zDelta |= ((long) (data[i + 28] & 0xff)) << (i * 8);
            }
        }

        public double activityCount()
        {
            return ((double) (this.xDelta + this.yDelta + this.zDelta)) / 3;
        }

        @SuppressWarnings("unused")
        public JSONObject toJson(Context context)
        {
            JSONObject json = new JSONObject();

            try
            {
                json.put("start", this.start);
                json.put("duration", this.duration);
                json.put("num_samples", this.numSamples);
                json.put("num_duplicates", this.numDuplicates);
                json.put("x", xDelta);
                json.put("y", yDelta);
                json.put("z", zDelta);
                json.put("counts", this.activityCount());

                return json;
            }
            catch (JSONException e)
            {
                LogManager.getInstance(context).logException(e);
            }

            return null;
        }

        public long start()
        {
            return this.start;
        }

        public long duration()
        {
            return this.duration;
        }

        public long xDelta()
        {
            return this.xDelta;
        }

        public long yDelta()
        {
            return this.yDelta;
        }

        public long zDelta()
        {
            return this.zDelta;
        }

        public int numSamples()
        {
            return this.numSamples;
        }

        public int numDuplicates()
        {
            return this.numDuplicates;
        }

        public void applyTimezone(TimeZone tz)
        {
            this.start -= tz.getOffset(this.start);
        }

    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.devices.AsyncPebbleProbe";
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_other_devices_category);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_async_pebble_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(AsyncPebbleProbe.ENABLED);
        enabled.setDefaultValue(AsyncPebbleProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        return screen;
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(AsyncPebbleProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(AsyncPebbleProbe.ENABLED, false);

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
            if (prefs.getBoolean(AsyncPebbleProbe.ENABLED, AsyncPebbleProbe.DEFAULT_ENABLED))
            {
                PebbleCalibrationHelper.check(context, true);

                PebbleKit.startAppOnPebble(context, AsyncPebbleProbe.WATCHAPP_UUID);

                if (this._receiver == null)
                {
                    final AsyncPebbleProbe me = this;

                    this._receiver =  new PebbleKit.PebbleDataReceiver(AsyncPebbleProbe.WATCHAPP_UUID)
                    {
                        public void receiveData(final Context context, final int transactionId, final PebbleDictionary dictionary)
                        {
                            PebbleKit.sendAckToPebble(context, transactionId);

                            ActivityCount count = new ActivityCount(dictionary.getBytes(1));

                            try
                            {
                                Log.e("PR", "ACTIVITY-COUNT: " + count.toJson(context).toString(2));
                            }
                            catch (JSONException e)
                            {
                                e.printStackTrace();
                            }

                            TimeZone here = Calendar.getInstance().getTimeZone();
                            count.applyTimezone(here);

                            Bundle data = new Bundle();
                            data.putDouble(Probe.BUNDLE_TIMESTAMP, count.start() / 1000);
                            data.putString(Probe.BUNDLE_PROBE, me.name(context));

                            PebbleKit.FirmwareVersionInfo info = PebbleKit.getWatchFWVersion(context);

                            data.putString(AsyncPebbleProbe.FIRMWARE_VERSION, "" + info.getMajor() + "." + info.getMinor() + "." + info.getPoint() + " " + info.getTag());

                            data.putLong(AsyncPebbleProbe.BUNDLE_DURATION, count.duration());
                            data.putInt(AsyncPebbleProbe.BUNDLE_NUM_SAMPLES, count.numSamples());
                            data.putInt(AsyncPebbleProbe.BUNDLE_NUM_DUPLICATES, count.numDuplicates());
                            data.putLong(AsyncPebbleProbe.BUNDLE_X_DELTA, count.xDelta());
                            data.putLong(AsyncPebbleProbe.BUNDLE_Y_DELTA, count.yDelta());
                            data.putLong(AsyncPebbleProbe.BUNDLE_Z_DELTA, count.zDelta());
                            data.putDouble(AsyncPebbleProbe.BUNDLE_ALL_DELTA, count.activityCount());

                            me.transmitData(context, data);
                        }
                    };

                    PebbleKit.registerReceivedDataHandler(context, this._receiver);
                }

                long now = System.currentTimeMillis();

                if (now - this._lastRefresh > 60000) // Attempt to flush remote buffer every 60 seconds...
                {
                    this._lastRefresh = now;

                    if (this._ackReceiver == null)
                    {
                        this._ackReceiver = new PebbleKit.PebbleAckReceiver(AsyncPebbleProbe.WATCHAPP_UUID)
                        {
                            public void receiveAck(Context context, int i)
                            {
                                Log.e("PR", "ACK: " + i);
                            }
                        };

                        PebbleKit.registerReceivedAckHandler(context, this._ackReceiver);
                    }

                    if (this._nackReceiver == null)
                    {
                        this._nackReceiver = new PebbleKit.PebbleNackReceiver(AsyncPebbleProbe.WATCHAPP_UUID)
                        {
                            public void receiveNack(Context context, int i)
                            {
                                Log.e("PR", "NACK: " + i);
                            }
                        };

                        PebbleKit.registerReceivedNackHandler(context, this._nackReceiver);
                    }

                    PebbleDictionary data = new PebbleDictionary();
                    data.addUint8(0, AsyncPebbleProbe.COMMAND_FLUSH_BUFFER);

                    PebbleKit.sendDataToPebble(context, AsyncPebbleProbe.WATCHAPP_UUID, data);
                }
                else
                    Log.e("PR", "PEBBLE NOT CONNECTED");

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

        return false;
    }

    public String summary(Context context)
    {
        return context.getString(R.string.summary_async_pebble_probe_desc);
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_async_pebble_probe);
    }


    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double count = bundle.getDouble(AsyncPebbleProbe.BUNDLE_ALL_DELTA);
        double numSamples = bundle.getDouble(AsyncPebbleProbe.BUNDLE_NUM_SAMPLES);
        double duration = bundle.getDouble(AsyncPebbleProbe.BUNDLE_DURATION);

        return String.format(context.getResources().getString(R.string.summary_async_pebble_probe), (duration / 1000), numSamples, count);
    }
}
