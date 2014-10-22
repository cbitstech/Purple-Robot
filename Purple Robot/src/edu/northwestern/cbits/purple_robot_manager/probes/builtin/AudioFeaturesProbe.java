package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class AudioFeaturesProbe extends Probe {
    private static final boolean DEFAULT_ENABLED = false;
    private static final String ENABLED_KEY = "config_probe_audio_feature_enabled";

    private double[] samples = new double[32768];

    private boolean _recording = false;

    public String name(Context context) {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.AudioFeaturesProbe";
    }

    public String title(Context context) {
        return context.getString(R.string.title_audio_features_probe);
    }

    public String probeCategory(Context context) {
        return context.getString(R.string.probe_sensor_category);
    }

    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(PreferenceActivity activity) {
        PreferenceManager manager = activity.getPreferenceManager();

        PreferenceScreen screen = manager.createPreferenceScreen(activity);
        screen.setTitle(this.title(activity));
        screen.setSummary(this.summary(activity));

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(AudioFeaturesProbe.ENABLED_KEY);
        enabled.setDefaultValue(AudioFeaturesProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        return screen;
    }

    public boolean isEnabled(final Context context) {
        if (super.isEnabled(context)) {
            SharedPreferences prefs = Probe.getPreferences(context);

            boolean enabled = prefs.getBoolean(AudioFeaturesProbe.ENABLED_KEY,
                    AudioFeaturesProbe.DEFAULT_ENABLED);

            if (this._recording == false && enabled) {
                this._recording = true;

                final AudioFeaturesProbe me = this;

                Runnable r = new Runnable() {
                    @SuppressWarnings("deprecation")
                    public void run() {
                        int bufferSize = AudioRecord.getMinBufferSize(44100,
                                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                AudioFormat.ENCODING_PCM_16BIT);

                        AudioRecord recorder = null;

                        int[] rates = new int[] { 44100, 22050, 11025, 8000 };

                        for (int rate : rates) {
                            if (recorder == null) {
                                AudioRecord newRecorder = new AudioRecord(
                                        MediaRecorder.AudioSource.MIC, rate,
                                        AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                        AudioFormat.ENCODING_PCM_16BIT,
                                        bufferSize);

                                if (newRecorder.getState() == AudioRecord.STATE_INITIALIZED)
                                    recorder = newRecorder;
                                else
                                    newRecorder.release();
                            }
                        }

                        if (recorder != null) {
                            recorder.startRecording();

                            short[] buffer = new short[bufferSize];

                            int index = 0;

                            int read = 0;

                            double sampleSum = 0;
                            double samplePower = 0;

                            while (index < me.samples.length
                                    && 0 <= (read = recorder.read(buffer, 0,
                                            bufferSize))) {
                                for (int i = 0; i < read; i++) {
                                    if (index < me.samples.length) {
                                        sampleSum += Math.abs(buffer[i]);
                                        samplePower += Math.pow(
                                                ((double) buffer[i])
                                                        / Short.MAX_VALUE, 2);

                                        me.samples[index] = (double) buffer[i];
                                        index += 1;
                                    }
                                }
                            }

                            recorder.stop();

                            Bundle bundle = new Bundle();
                            bundle.putString("PROBE", me.name(context));
                            bundle.putLong("TIMESTAMP",
                                    System.currentTimeMillis() / 1000);
                            bundle.putInt("SAMPLE_RATE",
                                    recorder.getSampleRate());

                            recorder.release();

                            FastFourierTransformer fft = new FastFourierTransformer(
                                    DftNormalization.STANDARD);

                            Complex[] values = fft.transform(me.samples,
                                    TransformType.FORWARD);

                            double maxFrequency = 0;
                            double maxMagnitude = 0;

                            double minMagnitude = Double.MAX_VALUE;

                            for (int i = 0; i < values.length / 2; i++) {
                                Complex value = values[i];

                                double magnitude = value.abs();

                                if (magnitude > maxMagnitude) {
                                    maxMagnitude = magnitude;
                                    maxFrequency = (i * recorder
                                            .getSampleRate())
                                            / (double) me.samples.length;
                                }

                                if (magnitude < minMagnitude)
                                    minMagnitude = magnitude;
                            }

                            bundle.putDouble("FREQUENCY", maxFrequency);
                            bundle.putDouble("NORMALIZED_AVG_MAGNITUDE",
                                    (sampleSum / Short.MAX_VALUE)
                                            / me.samples.length);
                            bundle.putDouble("POWER", samplePower
                                    / me.samples.length);

                            me.transmitData(context, bundle);
                        }

                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        me._recording = false;
                    }
                };

                Thread t = new Thread(r);
                t.start();
            }

            return enabled;
        }

        return false;
    }

    public String summary(Context context) {
        return context.getString(R.string.summary_audio_features_probe_desc);
    }

    public void enable(Context context) {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(AudioFeaturesProbe.ENABLED_KEY, true);

        e.commit();
    }

    public void disable(Context context) {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(AudioFeaturesProbe.ENABLED_KEY, false);

        e.commit();
    }

    public String summarizeValue(Context context, Bundle bundle) {
        double freq = bundle.getDouble("FREQUENCY");

        return String.format(
                context.getResources().getString(
                        R.string.summary_audio_features_probe), freq);
    }
}
