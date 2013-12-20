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
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

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
	
	public void takeSnapshot(Context context, String source, final Runnable r)
	{
		final long now = System.currentTimeMillis();
		
        Cursor cursor = this._context.getContentResolver().query(RobotContentProvider.RECENT_PROBE_VALUES, null, null, null, "recorded DESC");
        
        JSONArray snapshot = new JSONArray();
        
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
		
		if (this._isRecording == false && prefs.getBoolean(SnapshotManager.RECORD_AUDIO, SnapshotManager.DEFAULT_RECORD))
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

					if (prefs.getBoolean("config_external_storage", false))
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
									Thread.sleep(5000); // TODO: Make configurable...
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
			        	e.printStackTrace();
			        	
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
	}
	
	public long[] snapshotTimes()
	{
		Cursor c = this._context.getContentResolver().query(RobotContentProvider.SNAPSHOTS, null, null, null, "recorded");
		
		long[] times = new long[c.getCount()];
		
		for (int i = 0; c.moveToNext(); i++)
		{
			times[i] = c.getLong(c.getColumnIndex("recorded"));
		}
		
		c.close();

		return times;
	}
	
	public JSONObject jsonForTime(long time)
	{
		return null;
	}
	
	public void clearTime(long time)
	{
		
	}
	
	public void showSnapshot(long time, Map<String, Object> options)
	{
		
	}
}
