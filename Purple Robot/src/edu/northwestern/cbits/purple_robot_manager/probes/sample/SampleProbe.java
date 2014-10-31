package edu.northwestern.cbits.purple_robot_manager.probes.sample;

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
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

/**
 * SampleProbe: This is a documented example of how to create new probes for
 * Purple Robot. It accesses the device's built-in microphone to obtain audio
 * samples and compute features from the samples.
 * 
 * In addition to implementing this class, to register the new Probe with Purple
 * Robot, the following steps must be completed:
 * 
 * 1. In `res/values/arrays.xml`, add the following line to the `probe_classes`
 * array:
 * 
 * <item>sample.SampleProbe</item>
 * 
 * This instructs Purple Robot to load the probe with the full classname
 * 
 * edu.northwestern.cbits.purple_robot_manager.probes.sample.SampleProbe
 * 
 * into the set of supported probes. The loader automatically provides the
 * 
 * edu.northwestern.cbits.purple_robot_manager.probes
 * 
 * package prefix.
 * 
 * 2. In the ProbeManager.probeForName, add the following condition to the
 * sequence of if-else statements:
 * 
 * else if (probe instanceof SampleProbe) { SampleProbe sample = (SampleProbe)
 * probe;
 * 
 * if (sample.name(context).equalsIgnoreCase(name)) found = true; }
 * 
 * This allows the system to determine if a probe is available that matches the
 * given name. (This is an old & suboptimal mechanism that is due for
 * replacement as time becomes available.)
 * 
 * Once these steps are complete, the rest of the work involved is contained
 * within this class file.
 */

public class SampleProbe extends Probe
{
    public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.sample.SampleProbe";

    private static final boolean DEFAULT_ENABLED = false;
    private static final String ENABLE = "config_probe_sample_enabled";
    private static final String FREQUENCY = "config_probe_sample_frequency";

    private boolean _recording = false;
    private double[] _samples = new double[32768];

    private long _lastCheck = 0;

    /**
     * Returns the machine-readable name of the probe used throughout the
     * system. Typically is the full class name of the probe and defined as a
     * class constant for use elsewhere in the system where a Context object may
     * be unavailable.
     */
    @Override
    public String name(Context context)
    {
        return SampleProbe.NAME;
    }

    /**
     * Returns a human-readable title for the probe that is used in various
     * display contexts. Titles may be translated using the `strings.xml`
     * mechanism.
     */
    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_sample_probe);
    }

    /**
     * Returns a category name used to group probes in the app settings. Names
     * should be defined in `strings.xml`, and not the source code for
     * translations.
     */
    @Override
    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_misc_category);
    }

    /**
     * Generates and returns a preferences screen for setting the options for
     * the probe. The title and summary are shown in the probe lists. All probes
     * should include an "Enable" checkbox to allow the user to deactivate the
     * probe as needed. Additional optins such as frequencies and durations may
     * also be defined. In the case below, a frequency parameter is defined that
     * dictates how often the probe should emit a reading.
     */
    @SuppressWarnings("deprecation")
    @Override
    public PreferenceScreen preferenceScreen(PreferenceActivity activity)
    {
        PreferenceManager manager = activity.getPreferenceManager();

        PreferenceScreen screen = manager.createPreferenceScreen(activity);
        screen.setTitle(this.title(activity));
        screen.setSummary(this.summary(activity));

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(SampleProbe.ENABLE);
        enabled.setDefaultValue(SampleProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        // Adding a frequency parameter that limits how frequently the probe can
        // sample data...

        ListPreference duration = new ListPreference(activity);
        duration.setKey(SampleProbe.FREQUENCY);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);
        duration.setEntryValues(R.array.probe_builtin_frequency_values);
        duration.setEntries(R.array.probe_builtin_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);

        screen.addPreference(duration);

        return screen;
    }

    /**
     * Returns a human-readable summary that explains what the probe is and what
     * it does.
     */
    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_sample_probe_desc);
    }

    /**
     * Standard method called by various elements of the system to enable this
     * probe.
     */
    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(SampleProbe.ENABLE, true);
        e.commit();
    }

    /**
     * Standard method called by various elements of the system to disable this
     * probe.
     */
    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(SampleProbe.ENABLE, false);
        e.commit();
    }

    /**
     * Called by the system every fifteen seconds to verify if the probe is
     * enabled and if there's work to do. Note that if a larger sample rate is
     * required, probes must set up and manage their own threads that implement
     * the desired sample rate.
     * 
     * (The naming of this method needs to be revisited and revised.)
     * 
     * This example will illustrate how to communicate with the device's
     * built-in audio capture devices to calculate audio features from
     * regularly-recorded audio samples.
     */

    public boolean isEnabled(final Context context)
    {
        // First, check if probes are enabled across the app
        // (Settings->Probes->Enable Probes)

        if (super.isEnabled(context))
        {
            // Probes are enabled system-wide, continue...

            // Fetch the SharedPreferences object of obtaining configuration
            // parameters...

            SharedPreferences prefs = Probe.getPreferences(context);

            if (prefs.getBoolean(SampleProbe.ENABLE, SampleProbe.DEFAULT_ENABLED) == false)
            {
                // We're not enabled. Bailing out.

                return false;
            }

            // Fetch the frequency settings and date to determine if enough time
            // has elapsed
            // since the last check...

            long freq = Long.parseLong(prefs.getString(SampleProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
            long now = System.currentTimeMillis();

            if (now - this._lastCheck < freq)
            {
                // Not enough time has passed. Bailing out...

                return true;
            }

            this._lastCheck = now;

            // If we're not recording, go ahead and get started...

            if (this._recording == false)
            {
                this._recording = true;

                // Since we are going to collect and analyze data in separate
                // threads, create
                // a final pointer to this instance of the probe so that we can
                // still access
                // functions like emitReading to send the data to the rest of
                // the system after
                // analysis is complete.

                final SampleProbe me = this;

                // Define what the probe should do in its own thread...

                Runnable r = new Runnable()
                {
                    @SuppressWarnings("deprecation")
                    public void run()
                    {
                        // Determine the size of the smallest buffer the probe
                        // will need
                        // to collect and store audio samples.

                        int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                AudioFormat.ENCODING_PCM_16BIT);

                        // Find the highest sample rate the device supports and
                        // create an
                        // AudioRecord instance for that rate...

                        AudioRecord recorder = null;

                        int[] rates = new int[]
                        { 44100, 22050, 11025, 8000 };

                        for (int rate : rates)
                        {
                            if (recorder == null)
                            {
                                AudioRecord newRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate,
                                        AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,
                                        bufferSize);

                                if (newRecorder.getState() == AudioRecord.STATE_INITIALIZED)
                                    recorder = newRecorder;
                                else
                                    newRecorder.release();
                            }
                        }

                        // If we were able to find a sample rate and create an
                        // AudioRecorder
                        // instance...

                        if (recorder != null)
                        {
                            // Create a buffer for samples and start recording.
                            // Note that the recording duration
                            // is a function of the sample rate and master
                            // buffer size (in seconds):
                            //
                            // _samples.length / recorder.getSampleRate
                            //

                            short[] buffer = new short[bufferSize];

                            recorder.startRecording();

                            int index = 0;

                            int read = 0;

                            // Designate a place to store a running tally for
                            // calculating
                            // the signal power...

                            double sampleSum = 0;
                            double samplePower = 0;

                            // While we have room in the master buffer and audio
                            // is available...

                            while (index < me._samples.length && 0 <= (read = recorder.read(buffer, 0, bufferSize)))
                            {
                                // Calculate the power as we collect samples...

                                for (int i = 0; i < read; i++)
                                {
                                    if (index < me._samples.length)
                                    {
                                        sampleSum += Math.abs(buffer[i]);
                                        samplePower += Math.pow(((double) buffer[i]) / Short.MAX_VALUE, 2);

                                        me._samples[index] = (double) buffer[i];
                                        index += 1;
                                    }
                                }
                            }

                            // Master buffer is full, stop recording.

                            recorder.stop();

                            // Start building the data payload that the probe
                            // will emit. Note that the timestamp
                            // is in seconds at this level.

                            Bundle bundle = new Bundle();
                            bundle.putString("PROBE", me.name(context));
                            bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
                            bundle.putInt("SAMPLE_RATE", recorder.getSampleRate());
                            bundle.putDouble("POWER", samplePower / me._samples.length);
                            bundle.putDouble("NORMALIZED_AVG_MAGNITUDE", (sampleSum / Short.MAX_VALUE)
                                    / me._samples.length);

                            // The AudioRecord instance is no longer needed at
                            // the moment, free it...

                            recorder.release();

                            // Let's calculate the magnitude and frequency of
                            // the audio signal using a
                            // Fast Fourier Transform. (Implementation provided
                            // by the Apache Commons Math
                            // library.)
                            //
                            // If you wanted to calculate your own features, you
                            // would replace the code below
                            // with your own code for analyzing the collected
                            // samples.

                            FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

                            Complex[] values = fft.transform(me._samples, TransformType.FORWARD);

                            double maxFrequency = 0;
                            double maxMagnitude = 0;

                            // Look at the possible wavelengths and determine
                            // which one has the largest magnitude...

                            for (int i = 0; i < values.length / 2; i++)
                            {
                                Complex value = values[i];

                                double magnitude = value.abs();

                                if (magnitude > maxMagnitude)
                                {
                                    maxMagnitude = magnitude;
                                    maxFrequency = (i * recorder.getSampleRate()) / (double) me._samples.length;
                                }
                            }

                            // Add the largest found frequency to the payload...

                            bundle.putDouble("FREQUENCY", maxFrequency);

                            // Transmit. Note that this is a thread-safe method,
                            // so probe data can be transmitted
                            // on a separate thread than the original one
                            // containing the isEnabled call.

                            me.transmitData(context, bundle);
                        }

                        // Wait a moment while data is flushed out of the system
                        // before we allow recording to
                        // resume...

                        try
                        {
                            Thread.sleep(10000);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }

                        // Reset the flag to allow recording to start in the
                        // next cycle.

                        me._recording = false;
                    }
                };

                // Start the thread.

                Thread t = new Thread(r);
                t.start();

                return true;
            }

            return true;
        }

        return false;
    }

    /**
     * Provides a human-readable display of the data in places like the main
     * screen. A copy of the bundle transmitted is provided, and this method
     * returns a string formatting the most relevant details.
     */

    public String summarizeValue(Context context, Bundle bundle)
    {
        double freq = bundle.getDouble("FREQUENCY");

        return String.format(context.getResources().getString(R.string.summary_audio_features_probe), freq);
    }

}
