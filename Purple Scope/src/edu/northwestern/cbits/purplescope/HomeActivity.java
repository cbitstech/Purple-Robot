package edu.northwestern.cbits.purplescope;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends ActionBarActivity 
{
	public static final String UPDATE_INTERFACE = "update_interface";

	private Menu _menu = null;
	private BroadcastReceiver _receiver = null;
	private ArrayAdapter<String> _adapter = null;
	
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.activity_home);
		
		String[] resources = this.deviceResources();
		
		final HomeActivity me = this;
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		this._adapter = new ArrayAdapter<String>(this, R.layout.row_resource, resources)
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
        		
        		checkbox.setEnabled(ScopeService.isSampling() == false);
        		
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
		resourceList.setAdapter(this._adapter);
	}
	
	public void onResume()
	{
		super.onResume();
		
		if (this._receiver == null)
		{
			final HomeActivity me = this;
			
			this._receiver = new BroadcastReceiver()
			{
				public void onReceive(Context context, Intent intent) 
				{
					TextView minute = (TextView) me.findViewById(R.id.minute_field);
					TextView second = (TextView) me.findViewById(R.id.second_field);
					
					long duration = intent.getLongExtra(ScopeService.DURATION, 0);
					
					long seconds = duration / 1000;
					
					long minutes = seconds / 60;
					seconds = seconds % 60;
					
					if (minutes < 10)
						minute.setText("0" + minutes);
					else
						minute.setText("" + minutes);
				
					if (seconds < 10)
						second.setText(":0" + seconds);
					else
						second.setText(":" + seconds);

					MenuItem pause = me._menu.findItem(R.id.pause_button);
					MenuItem play = me._menu.findItem(R.id.play_button);

					if (ScopeService.isSampling())
					{
						pause.setVisible(true);
						play.setVisible(false);
					}
					else
					{
						play.setVisible(true);
						pause.setVisible(false);
					}

					me._adapter.notifyDataSetChanged();
				}
			};
			
			IntentFilter filter = new IntentFilter(HomeActivity.UPDATE_INTERFACE);
			
			LocalBroadcastManager broadcasts = LocalBroadcastManager.getInstance(this);
			broadcasts.registerReceiver(this._receiver, filter);
		}
	}

	public void onPause()
	{
		super.onPause();
		
		if (this._receiver != null)
		{
			LocalBroadcastManager broadcasts = LocalBroadcastManager.getInstance(this);
			
			broadcasts.unregisterReceiver(this._receiver);
			
			this._receiver = null;
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		this.getMenuInflater().inflate(R.menu.menu_home, menu);
		
		this._menu = menu;
		
		return true;
	}
	
	private String[] deviceResources()
	{
		String cpu = this.getString(R.string.resource_cpu);
		String battery = this.getString(R.string.resource_battery);
		String robotMemory = this.getString(R.string.resource_memory_robot);
		String funfMemory = this.getString(R.string.resource_memory_funf);
		// String robotDisk = this.getString(R.string.resource_disk_robot);
		// String funfDisk = this.getString(R.string.resource_disk_funf);

		String[] resources = { cpu, battery, robotMemory, funfMemory }; // , robotDisk, funfDisk };
		
		return resources;
	}
	
	public boolean onOptionsItemSelected(final MenuItem item)
    {
		final HomeActivity me = this;
		
    	if (item.getItemId() == R.id.play_button)
    	{
			LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View sessionView = inflater.inflate(R.layout.layout_session, null);
			
			final SeekBar duration = (SeekBar) sessionView.findViewById(R.id.duration_size);
			duration.setMax(59);
			
			duration.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
			{
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
				{
					TextView durationLabel = (TextView) sessionView.findViewById(R.id.duration_text);
					
					int seconds = (progress + 1) * 30;
					
					int minutes = seconds / 60;
					seconds = seconds % 60;
					
					String minutesText = "" + minutes;
					
					if (minutes < 10)
						minutesText = "0" + minutes;
					
					String secondsText = "" + seconds;
					
					if (seconds < 10)
						secondsText = "0" + seconds;
					
					durationLabel.setText(minutesText + ":" + secondsText);
				}

				public void onStartTrackingTouch(SeekBar seekBar) 
				{

				}

				public void onStopTrackingTouch(SeekBar seekBar) 
				{

				}
			});
			
			duration.setProgress(29);
			
			final EditText name = (EditText) sessionView.findViewById(R.id.session_name);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder = builder.setTitle("Begin Session");
			builder = builder.setView(sessionView);
			builder = builder.setPositiveButton("Begin Session", new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int which) 
				{
					MenuItem pause = me._menu.findItem(R.id.pause_button);
					pause.setVisible(true);
					
					item.setVisible(false);

					int halfMinutes = duration.getProgress() + 1;

					Intent intent = new Intent(ScopeService.BEGIN_SAMPLING);
					intent.putExtra(ScopeService.DURATION, 1000L * 30 * halfMinutes);
					intent.putExtra(ScopeService.SESSION_NAME, name.getText().toString());
					
					me.startService(intent);
				}
			});
			
			builder.create().show();
    	}
    	else if (item.getItemId() == R.id.pause_button)
    	{
			MenuItem pause = this._menu.findItem(R.id.play_button);
			pause.setVisible(true);
			
			item.setVisible(false);

			Intent intent = new Intent(ScopeService.END_SAMPLING);
			this.startService(intent);
    	}
    	else if (item.getItemId() == R.id.save_button)
    	{
    		File externalDir = this.getExternalFilesDir(null);

    		if (externalDir.exists() == false)
    			externalDir.mkdirs();
    		
    		try 
    		{
    			File root = this.getFilesDir().getParentFile();
    			File databases = new File(root, "databases");
    			
				FileUtils.copyFileToDirectory(new File(databases, PerformanceContentProvider.DATABASE), externalDir);

				Toast.makeText(this, "The database is now available on accessible storage.", Toast.LENGTH_LONG).show();
			}
    		catch (IOException e) 
    		{
				e.printStackTrace();

				Toast.makeText(this, "Error encountered copying the database to accessible storage.", Toast.LENGTH_LONG).show();
			}
    	}
    	else if (item.getItemId() == R.id.reset_button)
    	{
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder = builder.setTitle("Clear Database?");
    		builder = builder.setMessage("Are you sure you want to clear existing performance readings from the database?");

    		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() 
    		{
				public void onClick(DialogInterface arg0, int arg1) 
				{
					int deleted = me.getContentResolver().delete(PerformanceContentProvider.PERFORMANCE_VALUES, "_id != -1", null);
					
					Toast.makeText(me, deleted + " records cleared.", Toast.LENGTH_LONG).show();
				}
			});
    		
    		builder.setNegativeButton("No", new DialogInterface.OnClickListener() 
    		{
				public void onClick(DialogInterface arg0, int arg1) 
				{

				}
			});
    		
    		builder.create().show();
    	}

		return true;
    }
}

