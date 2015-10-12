package edu.northwestern.cbits.purple_robot_manager.probes.media;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.StartActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.SettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class AudioCaptureProbe extends Probe
{
    private static final boolean DEFAULT_ENABLED = false;
    private static final String ENABLED = "config_probe_audio_capture_enabled";
    private static final String FREQUENCY = "config_probe_audio_capture_frequency";
    private static final String DURATION = "config_probe_audio_capture_duration";
    private static final String DEFAULT_DURATION = "15000";
    private static final int NOTE_ID = 684756384;
    private static final String RECORDING_DURATION = "RECORDING_DURATION";

    private boolean _recording = false;
    private long _lastCheck = System.currentTimeMillis();

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.media.AudioCaptureProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_audio_capture_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_media_capture_category);
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
        enabled.setKey(AudioCaptureProbe.ENABLED);
        enabled.setDefaultValue(AudioCaptureProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleListPreference frequency = new FlexibleListPreference(context);
        frequency.setKey(AudioCaptureProbe.FREQUENCY);
        frequency.setEntryValues(R.array.probe_satellite_frequency_values);
        frequency.setEntries(R.array.probe_satellite_frequency_labels);
        frequency.setTitle(R.string.probe_frequency_label);
        frequency.setDefaultValue(Probe.DEFAULT_FREQUENCY);

        screen.addPreference(frequency);

        FlexibleListPreference duration = new FlexibleListPreference(context);
        duration.setKey(AudioCaptureProbe.DURATION);
        duration.setEntryValues(R.array.probe_audio_feature_duration_values);
        duration.setEntries(R.array.probe_audio_feature_duration_labels);
        duration.setTitle(R.string.probe_audio_feature_duration_label);
        duration.setDefaultValue(AudioCaptureProbe.DEFAULT_DURATION);

        screen.addPreference(duration);

        return screen;
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        if (super.isEnabled(context))
        {
            SharedPreferences prefs = Probe.getPreferences(context);

            final AudioCaptureProbe me = this;

            boolean enabled = prefs.getBoolean(AudioCaptureProbe.ENABLED, AudioCaptureProbe.DEFAULT_ENABLED);

            if (this._recording == false && enabled)
            {
                final long now = System.currentTimeMillis();

                long freq = Long.parseLong(prefs.getString(AudioCaptureProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
                final int duration = Integer.parseInt(prefs.getString(AudioCaptureProbe.DURATION, AudioCaptureProbe.DEFAULT_DURATION));

                if (now - this._lastCheck > freq) {
                    this._recording = true;

                    final NotificationManager noteManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                    PendingIntent contentIntent = PendingIntent.getActivity(context, AudioCaptureProbe.NOTE_ID, new Intent(context, StartActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                    builder = builder.setContentIntent(contentIntent);
                    builder = builder.setContentTitle(context.getString(R.string.notify_audio_recording_active));
                    builder.setOngoing(true);

                    builder = builder.setSmallIcon(R.drawable.ic_note_mic);
                    builder = builder.setContentText(context.getString(R.string.notify_audio_recording_active_details));
                    builder = builder.setTicker(context.getString(R.string.notify_audio_recording_active));

                    Notification note = builder.build();

                    noteManager.notify(AudioCaptureProbe.NOTE_ID, note);

                    Runnable r = new Runnable() {
                        @Override
                        @SuppressWarnings("deprecation")
                        public void run() {
                            final Bundle bundle = new Bundle();
                            bundle.putString("PROBE", me.name(context));
                            bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

                            File internalStorage = context.getCacheDir();

                            if (SettingsActivity.useExternalStorage(context))
                                internalStorage = context.getExternalCacheDir();

                            if (internalStorage != null && !internalStorage.exists())
                                internalStorage.mkdirs();

                            File audioFolder = new File(internalStorage, "Audio Capture Probe Audio");

                            if (audioFolder != null && !audioFolder.exists())
                                audioFolder.mkdirs();

                            final File audioFile = new File(audioFolder, now + ".mp4");

                            final String filename = audioFile.getAbsolutePath();

                            try {
                                final MediaRecorder recorder = new MediaRecorder();

                                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                                recorder.setOutputFile(filename);
                                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

                                recorder.prepare();

                                final long start = System.currentTimeMillis();
                                recorder.start();

                                Runnable stop = new Runnable()
                                {
                                    public void run()
                                    {
                                        try
                                        {
                                            Thread.sleep(duration);
                                        }
                                        catch (InterruptedException e)
                                        {

                                        }

                                        recorder.stop();
                                        long end = System.currentTimeMillis();
                                        recorder.release();

                                        bundle.putLong(AudioCaptureProbe.RECORDING_DURATION, end - start);
                                        bundle.putString(Probe.PROBE_MEDIA_URL, Uri.fromFile(audioFile).toString());
                                        bundle.putString(Probe.PROBE_MEDIA_CONTENT_TYPE, "audio/mp4");
                                        me.transmitData(context, bundle);

                                        noteManager.cancel(AudioCaptureProbe.NOTE_ID);
                                        me._recording = false;
                                    }
                                };

                                Thread stopThread = new Thread(stop);
                                stopThread.start();
                            }
                            catch (IOException e)
                            {
                                LogManager.getInstance(context).logException(e);

                                me._recording = false;
                                noteManager.cancel(AudioCaptureProbe.NOTE_ID);
                            }
                            catch (IllegalStateException e)
                            {
                                LogManager.getInstance(context).logException(e);

                                me._recording = false;
                                noteManager.cancel(AudioCaptureProbe.NOTE_ID);
                            }
                        }
                    };

                    Thread t = new Thread(r);
                    t.start();

                    this._lastCheck = now;
                }
            }

            return enabled;
        }

        return false;
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_audio_capture_probe_desc);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(AudioCaptureProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(AudioCaptureProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        long duration = (long) bundle.getDouble(AudioCaptureProbe.RECORDING_DURATION);

        return String.format(context.getResources().getString(R.string.summary_audio_capture_probe), duration);
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
            long freq = Long.parseLong(prefs.getString(AudioCaptureProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
            map.put(Probe.PROBE_FREQUENCY, freq);

            long duration = Long.parseLong(prefs.getString(AudioCaptureProbe.DURATION, AudioCaptureProbe.DEFAULT_DURATION));
            map.put(Probe.PROBE_DURATION, duration);
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

            e.putString(AudioCaptureProbe.FREQUENCY, frequency.toString());
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

            e.putString(AudioCaptureProbe.DURATION, duration.toString());
            e.commit();
        }
    }
}
