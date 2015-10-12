package edu.northwestern.cbits.purple_robot_manager.snapshots;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.preference.PreferenceManager;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.RobotContentProvider;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.SettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

public class SnapshotManager
{
    private static final String PROBE_NAME = "probe";
    private static final String RECORDED = "recorded";
    private static final String VALUE = "value";
    private static final String RECORD_AUDIO = "config_record_snapshot_audio";
    private static final boolean DEFAULT_RECORD = false;

    private static SnapshotManager _instance = null;

    private Context _context = null;
    private boolean _isRecording = false;

    private SnapshotManager(Context context)
    {
        if (SnapshotManager._instance != null)
            throw new IllegalStateException("Already instantiated");

        this._context = context;
    }

    public static SnapshotManager getInstance(Context context)
    {
        if (SnapshotManager._instance == null)
        {
            SnapshotManager._instance = new SnapshotManager(context.getApplicationContext());
        }

        return SnapshotManager._instance;
    }

    public long takeSnapshot(final Context context, String source, final Runnable r) throws EmptySnapshotException
    {
        final long now = System.currentTimeMillis();

        Cursor cursor = this._context.getContentResolver().query(RobotContentProvider.RECENT_PROBE_VALUES, null, null,
                null, "recorded DESC");

        JSONArray snapshot = new JSONArray();

        if (cursor.getCount() == 0)
            throw new EmptySnapshotException();

        while (cursor.moveToNext())
        {
            String name = cursor.getString(cursor.getColumnIndex("source"));
            long recorded = cursor.getLong(cursor.getColumnIndex("recorded"));

            String value = cursor.getString(cursor.getColumnIndex("value"));

            try
            {
                JSONObject sensorValue = new JSONObject(value);

                JSONObject sensorObj = new JSONObject();
                sensorObj.put(SnapshotManager.PROBE_NAME, name);
                sensorObj.put(SnapshotManager.RECORDED, recorded);
                sensorObj.put(SnapshotManager.VALUE, sensorValue);

                snapshot.put(sensorObj);
            }
            catch (JSONException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }
        }

        cursor.close();

        final ContentValues values = new ContentValues();

        values.put("_id", now);
        values.put("source", source);
        values.put("recorded", now);
        values.put("value", snapshot.toString());

        final SnapshotManager me = this;

        final Runnable save = new Runnable()
        {
            public void run()
            {
                me._context.getContentResolver().insert(RobotContentProvider.SNAPSHOTS, values);

                if (r != null)
                    r.run();
            }
        };

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        if (this._isRecording == false
                && prefs.getBoolean(SnapshotManager.RECORD_AUDIO, SnapshotManager.DEFAULT_RECORD))
        {
            this._isRecording = true;

            if (context instanceof Activity)
            {
                final Activity a = (Activity) context;

                a.runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        Toast.makeText(a, R.string.toast_recording_audio, Toast.LENGTH_LONG).show();
                    }
                });
            }

            Runnable record = new Runnable()
            {
                public void run()
                {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(me._context);

                    File internalStorage = me._context.getCacheDir();

                    if (SettingsActivity.useExternalStorage(context))
                        internalStorage = me._context.getExternalCacheDir();

                    if (internalStorage != null && !internalStorage.exists())
                        internalStorage.mkdirs();

                    File audioFolder = new File(internalStorage, "Snapshot Audio");

                    if (audioFolder != null && !audioFolder.exists())
                        audioFolder.mkdirs();

                    File audioFile = new File(audioFolder, now + ".3gp");

                    final String filename = audioFile.getAbsolutePath();

                    final MediaRecorder recorder = new MediaRecorder();
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    recorder.setOutputFile(filename);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

                    try
                    {
                        recorder.prepare();
                        recorder.start();

                        Runnable stop = new Runnable()
                        {
                            public void run()
                            {
                                try
                                {
                                    Thread.sleep(5000); // TODO: Make
                                                        // configurable...
                                }
                                catch (InterruptedException e)
                                {

                                }

                                recorder.stop();
                                recorder.release();

                                values.put("audio_file", filename);

                                me._isRecording = false;
                                save.run();
                            }
                        };

                        Thread stopThread = new Thread(stop);
                        stopThread.start();
                    }
                    catch (IOException e)
                    {
                        LogManager.getInstance(me._context).logException(e);

                        me._isRecording = false;
                        save.run();
                    }
                }
            };

            Thread t = new Thread(record);
            t.start();
        }
        else
            save.run();

        return now;
    }

    public long[] snapshotTimes()
    {
        Cursor c = this._context.getContentResolver().query(RobotContentProvider.SNAPSHOTS, null, null, null,
                "recorded");

        long[] times = new long[c.getCount()];

        for (int i = 0; c.moveToNext(); i++)
        {
            times[i] = c.getLong(c.getColumnIndex("recorded"));
        }

        c.close();

        return times;
    }

    public JSONObject jsonForTime(long time, boolean includeValues)
    {
        JSONObject object = new JSONObject();

        String selection = "recorded = ?";
        String[] args =
        { "" + time };

        Cursor c = this._context.getContentResolver().query(RobotContentProvider.SNAPSHOTS, null, selection, args, null);

        if (c.moveToNext())
        {
            String source = c.getString(c.getColumnIndex("source"));
            long recorded = c.getLong(c.getColumnIndex("recorded"));
            String jsonString = c.getString(c.getColumnIndex("value"));

            String audio = c.getString(c.getColumnIndex("audio_file"));

            if (audio != null)
            {
                File f = new File(audio);

                if (f.exists() == false)
                    audio = null;
            }

            try
            {
                object.put("recorded", recorded);
                object.put("source", source);

                if (audio != null)
                    object.put("audio", audio.replace("\\/", "/"));

                if (includeValues)
                {
                    JSONArray values = new JSONArray(jsonString);

                    for (int i = 0; i < values.length(); i++)
                    {
                        JSONObject value = values.getJSONObject(i);

                        String name = value.getString("probe");

                        Probe p = ProbeManager.probeForName(name, this._context);

                        if (p != null)
                            value.put("name", p.title(this._context));
                    }

                    object.put("values", values);
                }
            }
            catch (JSONException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }
        }
        else
            object = null;

        c.close();

        return object;
    }

    public void clearTime(long time)
    {

    }

    public void showSnapshot(long time, Map<String, Object> options)
    {

    }

    public void deleteSnapshot(long timestamp)
    {
        String selection = "recorded = ?";
        String[] args =
        { "" + timestamp };

        this._context.getContentResolver().delete(RobotContentProvider.SNAPSHOTS, selection, args);
    }
}
