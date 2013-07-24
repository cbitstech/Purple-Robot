package edu.northwestern.cbits.purple_robot_manager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import net.hockeyapp.android.UpdateManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import edu.northwestern.cbits.purple_robot_manager.activities.DiagnosticActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.LabelActivity;
import edu.northwestern.cbits.purple_robot_manager.config.LegacyJSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.probes.features.Feature;

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

	private static String _statusMessage = null;

	private boolean _isPaused = true;
	private long _lastUpdate = 0;

	private SharedPreferences prefs = null;
	protected String _lastProbe = "";
	
	private Menu _menu = null;

	private static OnSharedPreferenceChangeListener _prefListener = new OnSharedPreferenceChangeListener()
    {
		public void onSharedPreferenceChanged(SharedPreferences prefs, String key) 
		{
			if (SettingsActivity.USER_ID_KEY.equals(key))
			{
				Editor e = prefs.edit();

				e.remove(SettingsActivity.USER_HASH_KEY);
				e.commit();
			}
		}
    };

	private SharedPreferences getPreferences(Context context)
	{
		if (this.prefs == null)
			this.prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

		return this.prefs;
	}

	private void launchPreferences()
	{
		Intent intent = new Intent();
		intent.setClass(this, SettingsActivity.class);

		this.startActivity(intent);
	}

	@SuppressLint("SimpleDateFormat")
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

		LogManager.getInstance(this);
		
		SharedPreferences sharedPrefs = this.getPreferences(this);

		if (this.getPackageManager().getInstallerPackageName(this.getPackageName()) == null) 
		{
			if (sharedPrefs.getBoolean(SettingsActivity.CHECK_UPDATES_KEY, true))
				UpdateManager.register(this, "7550093e020b1a4a6df90f1e9dde68b6");
		}

        this.getSupportActionBar().setTitle(R.string.title_probe_readings);
        this.setContentView(R.layout.layout_startup_activity);

        ManagerService.setupPeriodicCheck(this);

        sharedPrefs.registerOnSharedPreferenceChangeListener(StartActivity._prefListener);
        
        final StartActivity me = this;

        ListView listView = (ListView) this.findViewById(R.id.list_probes);

        this._receiver = new BroadcastReceiver()
    	{
    		public void onReceive(Context context, final Intent intent)
    		{
    			final long now = System.currentTimeMillis();

    			me.runOnUiThread(new Runnable()
    			{
    				public void run()
    				{
		    			if (StartActivity.UPDATE_DISPLAY.equals(intent.getAction()))
		    			{
			    			String name = intent.getStringExtra(DISPLAY_PROBE_NAME);

			    			if (name != null && !me._lastProbe.equals(name))
				    		{
				    			Bundle value = intent.getBundleExtra(DISPLAY_PROBE_VALUE);

				    			if (StartActivity._probeNames.contains(name))
				    				StartActivity._probeNames.remove(name);

				    			StartActivity._probeNames.add(0, name);
				    			StartActivity._probeValues.put(name, value);
				    			StartActivity._probeDates.put(name, new Date());

				    			if (me._isPaused == false && now - 5000 > me._lastUpdate)
				    			{
					    			me._lastUpdate = now;
			    					me.refreshList();
				    			}

				    			me._lastProbe = name;
				    		}
		    			}
		    			else if (StartActivity.UPDATE_MESSAGE.equals(intent.getAction()))
		    			{
		    				final String message = intent.getStringExtra(StartActivity.DISPLAY_MESSAGE);

		    				me.runOnUiThread(new Runnable()
		    				{
								public void run()
								{
									ActionBar bar = me.getSupportActionBar();

									SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

									StartActivity._statusMessage = sdf.format(new Date())+ ": " + message;
									bar.setSubtitle(StartActivity._statusMessage);
								}
		    				});
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

        		final String sensorName = StartActivity._probeNames.get(position);

        		final Probe probe = ProbeManager.probeForName(sensorName, me);

        		final Bundle value = StartActivity._probeValues.get(sensorName);
        		Date sensorDate = StartActivity._probeDates.get(sensorName);

        		convertView.setOnClickListener(new OnClickListener()
    			{
					public void onClick(View v)
					{
						Intent intent = null;

						if (probe != null)
							intent = probe.viewIntent(me);

						if (intent == null)
						{
							Intent dataIntent = new Intent(me, ProbeViewerActivity.class);

							dataIntent.putExtra("probe_name", sensorName);
							dataIntent.putExtra("probe_bundle", value);

							me.startActivity(dataIntent);
						}
						else
						{
							intent.putExtra("probe_name", sensorName);
							intent.putExtra("probe_bundle", value);

							me.startActivity(intent);
						}
					}
    			});

        		TextView nameField = (TextView) convertView.findViewById(R.id.text_sensor_name);
        		TextView valueField = (TextView) convertView.findViewById(R.id.text_sensor_value);

        		String formattedValue = sensorName;

        		String displayName = sensorName;

        		if (probe != null && value != null)
        		{
        			try
        			{
        				displayName = probe.title(me);
        				formattedValue = probe.summarizeValue(me, value);
        			}
        			catch (Exception e)
        			{
        				LogManager.getInstance(me).logException(e);
        			}

        			Bundle sensor = value.getBundle("SENSOR");

        			if (sensor != null && sensor.containsKey("POWER"))
        				formattedValue += " (" + sensor.getFloat("POWER") + " mA)";
        		}
        		else if (value.containsKey(Feature.FEATURE_VALUE))
        			formattedValue = value.get(Feature.FEATURE_VALUE).toString();
        		

        		nameField.setText(displayName + " (" + sdf.format(sensorDate) + ")");
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
    	
    	LegacyJSONConfigFile.getSharedFile(this.getApplicationContext());
    }

	public void refreshList()
	{
		final StartActivity me = this;

		Runnable r = new Runnable()
		{
			public void run()
			{
				SharedPreferences prefs = me.getPreferences(me);

		        boolean probesEnabled = prefs.getBoolean("config_probes_enabled", false);

		        if (probesEnabled)
		        {
	    			Runnable rr = new Runnable()
	    			{
						public void run()
						{
			    	        ListView listView = (ListView) me.findViewById(R.id.list_probes);

			    	        ListAdapter adapter = listView.getAdapter();

			    	        if (adapter instanceof BaseAdapter)
			    			{
			    	        	BaseAdapter baseAdapter = (BaseAdapter) adapter;

			    	        	baseAdapter.notifyDataSetChanged();
			    	        }

			    	        listView.invalidateViews();

			    	        me.getSupportActionBar().setTitle(String.format(me.getResources().getString(R.string.title_probes_count), StartActivity._probeNames.size()));
			    	        
			    	        me.updateAlertIcon();
						}
	    			};

	    			me.runOnUiThread(rr);
		        }
			}
		};

		Thread t = new Thread(r);
		t.start();
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

        ListView listView = (ListView) this.findViewById(R.id.list_probes);
		
		boolean probesEnabled = (listView.getVisibility() == View.VISIBLE);

		HashMap <String, Object> payload = new HashMap<String, Object>();
		payload.put("probes_enabled", probesEnabled);
		LogManager.getInstance(this).log("main_ui_dismissed", payload);
	}

	private void setJsonUri(Uri jsonConfigUri)
	{
		if (jsonConfigUri.getScheme().equals("cbits-prm") || jsonConfigUri.getScheme().equals("cbits-pr"))
		{
			Uri.Builder b = jsonConfigUri.buildUpon();

			b.scheme("http");

			jsonConfigUri = b.build();
		}

		EncryptionManager.getInstance().setConfigUri(this, jsonConfigUri);

		LegacyJSONConfigFile.updateFromOnline(this);
	}

	protected void onResume()
	{
		super.onResume();

		CrashManager.register(this, "7550093e020b1a4a6df90f1e9dde68b6", new CrashManagerListener()
		{
			  public Boolean onCrashesFound()
			  {
				    return true;
			  }
		});

		if (StartActivity._statusMessage != null)
			this.getSupportActionBar().setSubtitle(StartActivity._statusMessage);

		Uri jsonConfigUri = this.getIntent().getData();

		SharedPreferences prefs = this.getPreferences(this);

        final String savedPassword = prefs.getString("config_password", null);

        if (jsonConfigUri != null)
        {
        	if (savedPassword == null || savedPassword.equals(""))
        		this.setJsonUri(jsonConfigUri);
        	else
        		Toast.makeText(this, R.string.error_json_set_uri_password, Toast.LENGTH_LONG).show();
        }

        this._isPaused = false;

        ListView listView = (ListView) this.findViewById(R.id.list_probes);
        ImageView logoView = (ImageView) this.findViewById(R.id.logo_view);
        logoView.setBackgroundColor(Color.WHITE);
        
        boolean probesEnabled = prefs.getBoolean("config_probes_enabled", false);

        this.getSupportActionBar().setTitle(R.string.app_name);

        if (probesEnabled)
        {
        	listView.setVisibility(View.VISIBLE);
        	logoView.setVisibility(View.GONE);
        }
        else
        {
        	logoView.setImageResource(R.drawable.laptop);

        	logoView.setScaleType(ScaleType.CENTER);

        	listView.setVisibility(View.GONE);
        	logoView.setVisibility(View.VISIBLE);
        }
        
        boolean showBackground = prefs.getBoolean("config_show_background", true);
        
        if (showBackground == false)
        	logoView.setVisibility(View.GONE);

        this.refreshList();
        
		HashMap <String, Object> payload = new HashMap<String, Object>();
		payload.put("probes_enabled", probesEnabled);
		LogManager.getInstance(this).log("main_ui_shown", payload);
	}
	
	private void updateAlertIcon()
	{
        if (this._menu != null)
        {
        	MenuItem diagIcon = this._menu.findItem(R.id.menu_diagnostic_item);

        	diagIcon.setIcon(SanityManager.getInstance(this).getErrorIconResource());
        }
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
        MenuInflater inflater = this.getSupportMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        this._menu = menu;
        
        this.updateAlertIcon();

        return true;
	}

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
    	{
    		case R.id.menu_label_item:
    			Intent labelIntent = new Intent();
    			labelIntent.setClass(this, LabelActivity.class);

    			labelIntent.putExtra(LabelActivity.LABEL_CONTEXT, "Home Screen");
    			labelIntent.putExtra(LabelActivity.TIMESTAMP, ((double) System.currentTimeMillis()));
    			
    			this.startActivity(labelIntent);

    			break;
    		case R.id.menu_upload_item:
    			LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this);
    			Intent intent = new Intent(OutputPlugin.FORCE_UPLOAD);

    			localManager.sendBroadcast(intent);

    			break;
    		case R.id.menu_diagnostic_item:
    			Intent diagIntent = new Intent();
    			diagIntent.setClass(this, DiagnosticActivity.class);

    			this.startActivity(diagIntent);

    			break;
    		case R.id.menu_settings_item:
    	        final String savedPassword = this.getPreferences(this).getString("config_password", null);

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
