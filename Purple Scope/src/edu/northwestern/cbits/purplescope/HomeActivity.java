package edu.northwestern.cbits.purplescope;

import java.io.IOException;
import java.io.RandomAccessFile;

import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;

public class HomeActivity extends ActionBarActivity 
{
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
	}

	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}

	// Source: http://stackoverflow.com/questions/3118234/how-to-get-memory-usage-and-cpu-usage-in-android
	
	private float readUsage() 
	{
	    try 
	    {
	        RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
	        String load = reader.readLine();

	        String[] toks = load.split(" ");

	        long idle1 = Long.parseLong(toks[5]);
	        long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
	              + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

	        try 
	        {
	            Thread.sleep(360);
	        } 
	        catch (Exception e) 
	        {
	        	
	        }

	        reader.seek(0);
	        load = reader.readLine();
	        reader.close();

	        toks = load.split(" ");

	        long idle2 = Long.parseLong(toks[5]);
	        long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
	            + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

	        return (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

	    }
	    catch (IOException e) 
	    {
	        e.printStackTrace();
	    }

	    return 0;
	} 
	
	// public MemoryInfo[] getProcessMemoryInfo (int[] pids)

}

