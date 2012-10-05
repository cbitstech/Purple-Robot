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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.WazaBe.HoloEverywhere.app.AlertDialog;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

public class StartActivity extends SherlockActivity
{
	public static String UPDATE_DISPLAY = "UPDATE_LIST_DISPLAY";
	public static String DISPLAY_PROBE_NAME = "DISPLAY_PROBE_NAME";
	public static String DISPLAY_PROBE_VALUE = "DISPLAY_PROBE_VALUE";

	private BroadcastReceiver _receiver = null;

	private static List<String> _probeNames = new ArrayList<String>();
	private static Map<String, String> _probeValues = new HashMap<String, String>();
	private static Map<String, Date> _probeDates = new HashMap<String, Date>();

	private boolean _isPaused = true;
	private long _lastUpdate = 0;

	private void launchPreferences()
	{
		Intent intent = new Intent();
		intent.setClass(this, SettingsActivity.class);

		this.startActivity(intent);
	}

	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.getSupportActionBar().setTitle(R.string.title_probe_readings);
        this.setContentView(R.layout.layout_startup_activity);

        ManagerService.setupPeriodicCheck(this);

        final StartActivity me = this;

        this._receiver = new BroadcastReceiver()
    	{
    		public void onReceive(Context context, Intent intent)
    		{
    			if (StartActivity.UPDATE_DISPLAY.equals(intent.getAction()))
    			{
    				String name = intent.getStringExtra(DISPLAY_PROBE_NAME);
    				String value = intent.getStringExtra(DISPLAY_PROBE_VALUE);

    				if (StartActivity._probeNames.contains(name))
    					StartActivity._probeNames.remove(name);

    				StartActivity._probeNames.add(0, name);
    				StartActivity._probeValues.put(name, value);
    				StartActivity._probeDates.put(name, new Date());

    				me.refreshList();
    			}
    		}
    	};

        ListView listView = (ListView) this.findViewById(R.id.list_probes);

        final SimpleDateFormat sdf = new SimpleDateFormat("MMM d, H:mm");

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
        		String sensorValue = StartActivity._probeValues.get(sensorName);
        		Date sensorDate = StartActivity._probeDates.get(sensorName);

        		Probe probe = ProbeManager.probeForName(sensorName);

        		if (probe != null)
        		{
        			sensorName = probe.title(me);
        			sensorValue = probe.summarizeValue(me, sensorValue);
        		}

        		nameField.setText(sensorName + " (" + sdf.format(sensorDate) + ")");
        		valueField.setText(sensorValue);

        		return convertView;
        	}
        };

        listView.setAdapter(adapter);

    	LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(me);

    	IntentFilter filter = new IntentFilter();

    	filter.addAction(StartActivity.UPDATE_DISPLAY);

    	broadcastManager.registerReceiver(this._receiver, filter);
    }

	public void refreshList()
	{
		long now = System.currentTimeMillis();

		if (this._isPaused == false && now - 1000 > this._lastUpdate)
		{
	        ListView listView = (ListView) this.findViewById(R.id.list_probes);

	        ArrayAdapter<String> adapter = (ArrayAdapter<String>) listView.getAdapter();

	        adapter.notifyDataSetChanged();

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
		final StartActivity me = this;

		if (jsonConfigUri.getScheme().equals("cbits-prm"))
		{
			Uri.Builder b = jsonConfigUri.buildUpon();

			b.scheme("http");

			jsonConfigUri = b.build();
		}

		// TODO: Add support for HTTPS?

		final Uri finalUri = jsonConfigUri;

		JSONConfigFile.validateUri(this, jsonConfigUri, true, new Runnable()
		{
			public void run()
			{
				// Success

				me.runOnUiThread(new Runnable()
				{
					public void run()
					{
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(me);
						Editor editor = prefs.edit();

						editor.putString("config_json_url", finalUri.toString());
						editor.commit();

						Toast.makeText(me, R.string.success_json_set_uri, Toast.LENGTH_LONG).show();
					}
				});
			}
		}, new Runnable()
		{
			public void run()
			{
				me.runOnUiThread(new Runnable()
				{
					public void run()
					{
						// Failure

						Toast.makeText(me, R.string.error_json_set_uri, Toast.LENGTH_LONG).show();
					}
				});
			}
		});
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
    		case R.id.menu_refresh_item:
	        	Intent reloadIntent = new Intent(ManagerService.REFRESH_CONFIGURATION);
	        	this.startService(reloadIntent);

	        	Toast.makeText(this, R.string.toast_refresh_probes, Toast.LENGTH_LONG).show();

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
