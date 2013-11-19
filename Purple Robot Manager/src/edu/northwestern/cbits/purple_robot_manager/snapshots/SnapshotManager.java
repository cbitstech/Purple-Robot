package edu.northwestern.cbits.purple_robot_manager.snapshots;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import edu.northwestern.cbits.purple_robot_manager.RobotContentProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

public class SnapshotManager 
{
	private static final String PROBE_NAME = "probe";
	private static final String RECORDED = "recorded";
	private static final String VALUE = "value";

	private static SnapshotManager _instance = null;

	private Context _context = null;
	
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
	
	public void takeSnapshot(String source, Runnable r)
	{
		long now = System.currentTimeMillis();
		
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
		
		ContentValues values = new ContentValues();
		
		values.put("_id", now);
		values.put("source", source);
		values.put("recorded", now);
		values.put("value", snapshot.toString());
		
		this._context.getContentResolver().insert(RobotContentProvider.SNAPSHOTS, values);
		
		if (r != null)
			r.run();
	}
	
	public long[] snapshotTimes()
	{
		return null;
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
