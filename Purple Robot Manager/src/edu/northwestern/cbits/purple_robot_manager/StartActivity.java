package edu.northwestern.cbits.purple_robot_manager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.WazaBe.HoloEverywhere.app.AlertDialog;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

public class StartActivity extends SherlockActivity
{
	public static final String UPDATE_MESSAGE = "UPDATE_LIST_MESSAGE";
	public static final String DISPLAY_MESSAGE = "DISPLAY_MESSAGE";
	public static String UPDATE_DISPLAY = "UPDATE_LIST_DISPLAY";
	public static String DISPLAY_PROBE_NAME = "DISPLAY_PROBE_NAME";
	public static String DISPLAY_PROBE_VALUE = "DISPLAY_PROBE_VALUE";

	private BroadcastReceiver _receiver = null;

	private static List<String> _probeNames = new ArrayList<String>();
	private static Map<String, Bundle> _probeValues = new HashMap<String, Bundle>();
	private static Map<String, Date> _probeDates = new HashMap<String, Date>();

	private boolean _isPaused = true;
	private long _lastUpdate = 0;

	private void launchPreferences()
	{
		Intent intent = new Intent();
		intent.setClass(this, SettingsActivity.class);

		this.startActivity(intent);
	}

	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.getSupportActionBar().setTitle(R.string.title_probe_readings);
        this.setContentView(R.layout.layout_startup_activity);

        ManagerService.setupPeriodicCheck(this);

        final StartActivity me = this;

        ListView listView = (ListView) this.findViewById(R.id.list_probes);

        this._receiver = new BroadcastReceiver()
    	{
    		public void onReceive(Context context, final Intent intent)
    		{
    			me.runOnUiThread(new Runnable()
    			{
    				public void run()
    				{
		    			if (StartActivity.UPDATE_DISPLAY.equals(intent.getAction()))
		    			{
		    				String name = intent.getStringExtra(DISPLAY_PROBE_NAME);
		    				Bundle value = intent.getBundleExtra(DISPLAY_PROBE_VALUE);

		    				if (StartActivity._probeNames.contains(name))
		    					StartActivity._probeNames.remove(name);

		    				StartActivity._probeNames.add(0, name);
		    				StartActivity._probeValues.put(name, value);
		    				StartActivity._probeDates.put(name, new Date());

		    				me.refreshList();
		    			}
		    			else if (StartActivity.UPDATE_MESSAGE.equals(intent.getAction()))
		    			{
		    				final String message = intent.getStringExtra(StartActivity.DISPLAY_MESSAGE);

		    				me.runOnUiThread(new Runnable()
		    				{
								public void run()
								{
									ActionBar bar = me.getSupportActionBar();

									bar.setSubtitle(message);
								}
		    				});

		    				Log.e("PRM", "UPDATE MESSAGE: " + intent.getStringExtra(StartActivity.DISPLAY_MESSAGE));
		    			}
    				}
    			});
    		}
    	};

        final SimpleDateFormat sdf = new SimpleDateFormat("MMM d, H:mm:ss");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.layout_probe_row, StartActivity._probeNames)
        {
        	public View getView(final int position, View convertView, ViewGroup parent)
        	{
        		if (convertView == null)
        		{
        			LayoutInflater inflater = (LayoutInflater) me.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        			convertView = inflater.inflate(R.layout.layout_probe_row, null);
        		}

        		TextView nameField = (TextView) convertView.findViewById(R.id.text_sensor_name);
        		TextView valueField = (TextView) convertView.findViewById(R.id.text_sensor_value);

        		String sensorName = StartActivity._probeNames.get(position);
        		Bundle value = StartActivity._probeValues.get(sensorName);
        		Date sensorDate = StartActivity._probeDates.get(sensorName);

        		Probe probe = ProbeManager.probeForName(sensorName);

        		String formattedValue = sensorName;

        		if (probe != null && value != null)
        		{
        			try
        			{
        				sensorName = probe.title(me);
        				formattedValue = probe.summarizeValue(me, value);
        			}
        			catch (Exception e)
        			{
        				e.printStackTrace();
        			}

        			Bundle sensor = value.getBundle("SENSOR");

        			if (sensor != null && sensor.containsKey("POWER"))
        				formattedValue += " (" + sensor.getFloat("POWER") + " mA)";
        		}

        		nameField.setText(sensorName + " (" + sdf.format(sensorDate) + ")");
        		valueField.setText(formattedValue);

        		return convertView;
        	}
        };

        listView.setAdapter(adapter);

    	LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(me);

    	IntentFilter filter = new IntentFilter();

    	filter.addAction(StartActivity.UPDATE_DISPLAY);
    	filter.addAction(StartActivity.UPDATE_MESSAGE);

    	broadcastManager.registerReceiver(this._receiver, filter);
    }

	public void refreshList()
	{
		long now = System.currentTimeMillis();

		if (this._isPaused == false && now - 1000 > this._lastUpdate)
		{
	        ListView listView = (ListView) this.findViewById(R.id.list_probes);

	        ListAdapter adapter = listView.getAdapter();

	        if (adapter instanceof BaseAdapter)
			{
	        	BaseAdapter baseAdapter = (BaseAdapter) adapter;

	        	baseAdapter.notifyDataSetChanged();
	        }

	        listView.invalidateViews();

	        this.getSupportActionBar().setTitle(String.format(this.getResources().getString(R.string.title_probes_count), StartActivity._probeNames.size()));

	        this._lastUpdate = now;
		}
	}

	protected void onDestroy()
	{
    	LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
    	broadcastManager.unregisterReceiver(this._receiver);

		super.onDestroy();
	}

	protected void onPause()
	{
		super.onPause();

		this._isPaused = true;
	}

	private void setJsonUri(Uri jsonConfigUri)
	{
		if (jsonConfigUri.getScheme().equals("cbits-prm"))
		{
			Uri.Builder b = jsonConfigUri.buildUpon();

			b.scheme("http");

			jsonConfigUri = b.build();
		}

		JSONConfigFile.updateFromOnline(this, jsonConfigUri);
	}

	protected void onResume()
	{
		super.onResume();

		Uri jsonConfigUri = this.getIntent().getData();

        final String savedPassword = PreferenceManager.getDefaultSharedPreferences(this).getString("config_password", null);

        if (jsonConfigUri != null)
        {
        	if (savedPassword == null || savedPassword.equals(""))
        		this.setJsonUri(jsonConfigUri);
        	else
        		Toast.makeText(this, R.string.error_json_set_uri_password, Toast.LENGTH_LONG).show();
        }

        this.refreshList();

        this._isPaused = false;
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
        MenuInflater inflater = this.getSupportMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        return true;
	}

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
    	{
    		case R.id.menu_upload_item:
    			LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this);
    			Intent intent = new Intent(OutputPlugin.FORCE_UPLOAD);

    			localManager.sendBroadcast(intent);

    			break;
    		case R.id.menu_play_toggle_item:
    			boolean doPause = (this._isPaused == false);

    			if (doPause)
    			{
    				item.setIcon(R.drawable.action_play);
    				item.setTitle(R.string.menu_resume_toggle_label);
    			}
    			else
    			{
    				item.setIcon(R.drawable.action_pause);
    				item.setTitle(R.string.menu_pause_toggle_label);
    			}

    			this._isPaused = doPause;

    			break;
    		case R.id.menu_settings_item:
    	        final String savedPassword = PreferenceManager.getDefaultSharedPreferences(this).getString("config_password", null);

    	        final StartActivity me = this;

				if (savedPassword == null || savedPassword.equals(""))
					this.launchPreferences();
				else
				{
	    	        AlertDialog.Builder builder = new AlertDialog.Builder(this);

	    	        builder.setMessage(R.string.dialog_password_prompt);
	    	        builder.setPositiveButton(R.string.dialog_password_submit, new DialogInterface.OnClickListener()
	    	        {
						public void onClick(DialogInterface dialog, int which)
						{
							AlertDialog alertDialog = (AlertDialog) dialog;

			    	        final EditText passwordField = (EditText) alertDialog.findViewById(R.id.text_dialog_password);

							Editable password = passwordField.getText();

							if (password.toString().equals(savedPassword))
								me.launchPreferences();
							else
								Toast.makeText(me, R.string.toast_incorrect_password, Toast.LENGTH_LONG).show();

							alertDialog.dismiss();
						}
					});
	    	        builder.setView(me.getLayoutInflater().inflate(R.layout.dialog_password, null));

	    	        AlertDialog alert = builder.create();

	    	        alert.show();
				}

    	        break;
		}

    	return true;
    }
}
