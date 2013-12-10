package edu.northwestern.cbits.purplescope;

import java.io.IOException;
import java.io.RandomAccessFile;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;

public class HomeActivity extends ActionBarActivity 
{
	private Menu _menu = null;
	
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.activity_home);
		
		String cpu = this.getString(R.string.resource_cpu);
		String battery = this.getString(R.string.resource_battery);
		String robotMemory = this.getString(R.string.resource_memory_robot);
		String funfMemory = this.getString(R.string.resource_memory_funf);
		String robotDisk = this.getString(R.string.resource_disk_robot);
		String funfDisk = this.getString(R.string.resource_disk_funf);

		String[] resources = { cpu, battery, robotMemory, funfMemory, robotDisk, funfDisk };
		
		final HomeActivity me = this;
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.row_resource, resources)
		{
			public View getView (int position, View convertView, ViewGroup parent)
			{
        		if (convertView == null)
        		{
        			LayoutInflater inflater = (LayoutInflater) me.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        			convertView = inflater.inflate(R.layout.row_resource, null);
        		}
        		
        		final String item = this.getItem(position);
        		
        		CheckBox checkbox = (CheckBox) convertView.findViewById(R.id.resource_check);
        		checkbox.setText(item);
        		
    			checkbox.setChecked(prefs.contains(item));
    			
    			checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener()
    			{
					public void onCheckedChanged(CompoundButton view, boolean checked) 
					{
						Editor e = prefs.edit();
						
						if (checked)
							e.putBoolean(item, true);
						else
							e.remove(item);
						
						e.commit();
					}
    				
    			});
				
				return convertView;
			}
		};
		
		ListView resourceList = (ListView) this.findViewById(R.id.capture_list);
		resourceList.setAdapter(adapter);
		
		TextView minute = (TextView) this.findViewById(R.id.minute_field);
		TextView second = (TextView) this.findViewById(R.id.second_field);
		
		OnClickListener listener = new OnClickListener()
		{
			public void onClick(View view) 
			{
				Log.e("PS", "Set Timer");
			}
		};
		
		minute.setOnClickListener(listener);
		second.setOnClickListener(listener);
	}

	public boolean onCreateOptionsMenu(Menu menu) 
	{
		this.getMenuInflater().inflate(R.menu.home, menu);
		
		this._menu = menu;
		
		return true;
	}
	
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	if (item.getItemId() == R.id.play_button)
    	{
			String cpu = this.getString(R.string.resource_cpu);
			String battery = this.getString(R.string.resource_battery);
			String robotMemory = this.getString(R.string.resource_memory_robot);
			String funfMemory = this.getString(R.string.resource_memory_funf);
			String robotDisk = this.getString(R.string.resource_disk_robot);
			String funfDisk = this.getString(R.string.resource_disk_funf);
	
			String[] resources = { cpu, battery, robotMemory, funfMemory, robotDisk, funfDisk };
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			
			for (String resource : resources)
			{
				if (prefs.contains(resource))
					Log.e("PS", "CAPTURE " + resource);
			}
			
			MenuItem pause = this._menu.findItem(R.id.pause_button);
			pause.setVisible(true);
			
			item.setVisible(false);
    	}
    	else if (item.getItemId() == R.id.pause_button)
    	{
			MenuItem pause = this._menu.findItem(R.id.play_button);
			pause.setVisible(true);
			
			item.setVisible(false);
    	}

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

