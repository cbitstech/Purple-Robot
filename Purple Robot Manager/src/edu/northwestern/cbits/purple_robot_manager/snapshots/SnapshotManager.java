package edu.northwestern.cbits.purple_robot_manager.snapshots;

import java.util.Map;

import org.json.JSONObject;

import android.content.Context;

public class SnapshotManager 
{
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
	
	public void takeSnapshot(Runnable r)
	{
		
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
