package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.ContextCompat;

import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.Manifest;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class AudioFeaturesProbe extends Probe
{
    private static final boolean DEFAULT_ENABLED = false;
    private static final String ENABLED = "config_probe_audio_feature_enabled";
    private static final String FREQUENCY = "config_probe_audio_features_frequency";
    private static final String DURATION = "config_probe_audio_features_duration";
    private static final String DEFAULT_DURATION = "15000";
    private static final String MAX_SAMPLE_RATE = "config_probe_audio_features_max_sample_rate";
    private static final String DEFAULT_MAX_SAMPLE_RATE = "11025";

    private boolean _recording = false;
    private long _lastCheck = 0;

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.AudioFeaturesProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_audio_features_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_sensor_category);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(this.summary(context));

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(AudioFeaturesProbe.ENABLED);
        enabled.setDefaultValue(AudioFeaturesProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleListPreference frequency = new FlexibleListPreference(context);
        frequency.setKey(AudioFeaturesProbe.FREQUENCY);
        frequency.setEntryValues(R.array.probe_satellite_frequency_values);
        frequency.setEntries(R.array.probe_satellite_frequency_labels);
        frequency.setTitle(R.string.probe_frequency_label);
        frequency.setDefaultValue(Probe.DEFAULT_FREQUENCY);

        screen.addPreference(frequency);

        FlexibleListPreference duration = new FlexibleListPreference(context);
        duration.setKey(AudioFeaturesProbe.DURATION);
        duration.setEntryValues(R.array.probe_audio_feature_duration_values);
        duration.setEntries(R.array.probe_audio_feature_duration_labels);
        duration.setTitle(R.string.probe_audio_feature_duration_label);
        duration.setDefaultValue(AudioFeaturesProbe.DEFAULT_DURATION);

        screen.addPreference(duration);

        FlexibleListPreference sampleRate = new FlexibleListPreference(context);
        sampleRate.setKey(AudioFeaturesProbe.MAX_SAMPLE_RATE);
        sampleRate.setEntryValues(R.array.probe_audio_feature_sample_rate_values);
        sampleRate.setEntries(R.array.probe_audio_feature_sample_rate_labels);
        sampleRate.setTitle(R.string.probe_audio_feature_sample_rate_label);
        sampleRate.setDefaultValue(AudioFeaturesProbe.DEFAULT_MAX_SAMPLE_RATE);

        screen.addPreference(sampleRate);

        return screen;
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        if (super.isEnabled(context))
        {
            SharedPreferences prefs = Probe.getPreferences(context);

            boolean enabled = prefs.getBoolean(AudioFeaturesProbe.ENABLED, AudioFeaturesProbe.DEFAULT_ENABLED);

            if (this._recording == false && enabled)
            {
                if (ContextCompat.checkSelfPermission(context, "android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_GRANTED) {
                    long now = System.currentTimeMillis();

                    long freq = Long.parseLong(prefs.getString(AudioFeaturesProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
                    final int duration = Integer.parseInt(prefs.getString(AudioFeaturesProbe.DURATION, AudioFeaturesProbe.DEFAULT_DURATION));
                    final int sampleRate = Integer.parseInt(prefs.getString(AudioFeaturesProbe.MAX_SAMPLE_RATE, AudioFeaturesProbe.DEFAULT_MAX_SAMPLE_RATE));

                    if (now - this._lastCheck > freq) {
                        this._recording = true;

                        final AudioFeaturesProbe me = this;

                        Runnable r = new Runnable() {
                            @Override
                            @SuppressWarnings("deprecation")
                            public void run() {
                                int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

                                AudioRecord recorder = null;

                                int[] rates = new int[]{44100, 22050, 11025, 8000};

                                for (int rate : rates) {
                                    if (rate <= sampleRate && recorder == null) {
                                        AudioRecord newRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

                                        if (newRecorder.getState() == AudioRecord.STATE_INITIALIZED)
                                            recorder = newRecorder;
                                        else
                                            newRecorder.release();
                                    }
                                }

                                if (recorder == null)
                                {
                                    me._recording = false;

                                    SanityManager.getInstance(context).addAlert(SanityCheck.WARNING, context.getString(R.string.name_sanity_audio_features_title), context.getString(R.string.name_sanity_audio_features_warning), null);

                                    return;
                                }

                                SanityManager.getInstance(context).clearAlert(context.getString(R.string.name_sanity_audio_features_title));

                                int sampleCount = recorder.getSampleRate() * (duration / 1000);

                                int twoPower = 2;

                                while (sampleCount > twoPower)
                                    twoPower *= 2;

                                sampleCount = twoPower / 2;

                                if (recorder != null) {
                                    double[] samples = new double[sampleCount];
                                    recorder.startRecording();

                                    short[] buffer = new short[bufferSize];

                                    int index = 0;

                                    int read = 0;

                                    double sampleSum = 0;
                                    double samplePower = 0;

                                    while (index < samples.length && 0 <= (read = recorder.read(buffer, 0, bufferSize))) {
                                        for (int i = 0; i < read; i++) {
                                            if (index < samples.length) {
                                                sampleSum += Math.abs(buffer[i]);
                                                samplePower += Math.pow(((double) buffer[i]) / Short.MAX_VALUE, 2);

                                                samples[index] = (double) buffer[i];
                                                index += 1;
                                            }
                                        }
                                    }

                                    recorder.stop();

                                    Bundle bundle = new Bundle();
                                    bundle.putString("PROBE", me.name(context));
                                    bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
                                    bundle.putInt("SAMPLE_RATE", recorder.getSampleRate());
                                    bundle.putInt("SAMPLE_BUFFER_SIZE", samples.length);
                                    bundle.putInt("SAMPLES_RECORDED", index);

                                    recorder.release();

                                    FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

                                    Complex[] values = fft.transform(samples, TransformType.FORWARD);

                                    double maxFrequency = 0;
                                    double maxMagnitude = Double.NEGATIVE_INFINITY;
                                    double minMagnitude = Double.POSITIVE_INFINITY;

                                    for (int i = 0; i < values.length / 2; i++) {
                                        Complex value = values[i];

                                        double magnitude = value.abs();

                                        if (magnitude > maxMagnitude) {
                                            maxMagnitude = magnitude;
                                            maxFrequency = (i * recorder.getSampleRate()) / (double) samples.length;
                                        }

                                        if (magnitude < minMagnitude)
                                            minMagnitude = magnitude;
                                    }

                                    bundle.putDouble("FREQUENCY", maxFrequency);
                                    bundle.putDouble("NORMALIZED_AVG_MAGNITUDE", (sampleSum / Short.MAX_VALUE) / samples.length);
                                    bundle.putDouble("POWER", samplePower / samples.length);

                                    me.transmitData(context, bundle);
                                }

                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                me._recording = false;
                            }
                        };

                        Thread t = new Thread(r);
                        t.start();

                        this._lastCheck = now;
                    }
                }
                else
                    SanityManager.getInstance(context).addPermissionAlert(this.name(context), "android.permission.RECORD_AUDIO", context.getString(R.string.rationale_audio_features_probe), null);
            }

            return enabled;
        }

        return false;
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_audio_features_probe_desc);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(AudioFeaturesProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(AudioFeaturesProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double freq = bundle.getDouble("FREQUENCY");

        return String.format(context.getResources().getString(R.string.summary_audio_features_probe), freq);
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

            JSONObject duration = new JSONObject();
            duration.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
            values = new JSONArray();

            String[] durationOptions = context.getResources().getStringArray(R.array.probe_audio_feature_duration_values);

            for (String option : durationOptions)
            {
                values.put(Long.parseLong(option));
            }

            duration.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_DURATION, duration);

            JSONObject sample = new JSONObject();
            sample.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
            values = new JSONArray();

            String[] sampleOptions = context.getResources().getStringArray(R.array.probe_audio_feature_sample_rate_values);

            for (String option : sampleOptions)
            {
                values.put(Long.parseLong(option));
            }

            sample.put(Probe.PROBE_VALUES, values);
            settings.put(AudioFeaturesProbe.MAX_SAMPLE_RATE, sample);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        try
        {
            long freq = Long.parseLong(prefs.getString(AudioFeaturesProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
            map.put(Probe.PROBE_FREQUENCY, freq);

            long duration = Long.parseLong(prefs.getString(AudioFeaturesProbe.DURATION, AudioFeaturesProbe.DEFAULT_DURATION));
            map.put(Probe.PROBE_DURATION, duration);

            long sampleRate = Long.parseLong(prefs.getString(AudioFeaturesProbe.MAX_SAMPLE_RATE, AudioFeaturesProbe.DEFAULT_MAX_SAMPLE_RATE));
            map.put(AudioFeaturesProbe.MAX_SAMPLE_RATE, sampleRate);
        }
        catch (NumberFormatException e)
        {
            LogManager.getInstance(context).logException(e);
        }

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

            e.putString(AudioFeaturesProbe.FREQUENCY, frequency.toString());
            e.commit();
        }

        if (params.containsKey(Probe.PROBE_DURATION))
        {
            Object duration = params.get(Probe.PROBE_DURATION);

            if ((duration instanceof Double) == false)
                duration = Double.valueOf(duration.toString()).longValue();
            else
                duration = ((Double) duration).longValue();

            SharedPreferences prefs = Probe.getPreferences(context);
            Editor e = prefs.edit();

            e.putString(AudioFeaturesProbe.DURATION, duration.toString());
            e.commit();
        }

        if (params.containsKey(AudioFeaturesProbe.MAX_SAMPLE_RATE))
        {
            Object rate = params.get(AudioFeaturesProbe.MAX_SAMPLE_RATE);

            if ((rate instanceof Double) == false)
                rate = Double.valueOf(rate.toString()).longValue();
            else
                rate = ((Double) rate).longValue();

            SharedPreferences prefs = Probe.getPreferences(context);
            Editor e = prefs.edit();

            e.putString(AudioFeaturesProbe.MAX_SAMPLE_RATE, rate.toString());
            e.commit();
        }
    }
}
