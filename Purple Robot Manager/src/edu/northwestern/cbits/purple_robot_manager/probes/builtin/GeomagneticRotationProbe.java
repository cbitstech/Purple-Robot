package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

@SuppressLint("SimpleDateFormat")
public class GeomagneticRotationProbe extends RotationProbe
{
	public static final String DB_TABLE = "geomagnetic_rotation_probe";

	public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.GeomagneticRotationProbe";

	public String contentSubtitle(Context context)
	{
		Cursor c = ProbeValuesProvider.getProvider(context).retrieveValues(context, GeomagneticRotationProbe.DB_TABLE, this.databaseSchema());

		int count = -1;

		if (c != null)
		{
			count = c.getCount();
			c.close();
		}

		return String.format(context.getString(R.string.display_item_count), count);
	}

	public long getFrequency()
	{
		SharedPreferences prefs = ContinuousProbe.getPreferences(this._context);

		return Long.parseLong(prefs.getString("config_probe_geomagnetic_rotation_built_in_frequency", ContinuousProbe.DEFAULT_FREQUENCY));
	}

	public String name(Context context)
	{
		return GeomagneticRotationProbe.NAME;
	}

	public int getTitleResource()
	{
		return R.string.title_geomagnetic_rotation_probe;
	}

	protected String dbTable() 
	{
		return GeomagneticRotationProbe.DB_TABLE;
	}

	
	@SuppressLint("InlinedApi")
	public boolean isEnabled(Context context)
	{
		if (Build.VERSION.SDK_INT < 19)
			return false;
		
    	SharedPreferences prefs = ContinuousProbe.getPreferences(context);

    	this._context = context.getApplicationContext();

    	SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		Sensor sensor = sensors.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);

        if (super.isSuperEnabled(context))
        {
        	if (prefs.getBoolean("config_probe_geomagnetic_rotation_built_in_enabled", ContinuousProbe.DEFAULT_ENABLED))
        	{
				int frequency = Integer.parseInt(prefs.getString("config_probe_geomagnetic_rotation_built_in_frequency", ContinuousProbe.DEFAULT_FREQUENCY));
				
				if (this._lastFrequency != frequency)
				{
					sensors.unregisterListener(this, sensor);
	                
	                switch (frequency)
	                {
	                	case SensorManager.SENSOR_DELAY_FASTEST:
		                	sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST, null);
	                		break;
	                	case SensorManager.SENSOR_DELAY_GAME:
		                	sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME, null);
	                		break;
	                	case SensorManager.SENSOR_DELAY_UI:
		                	sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, null);
	                		break;
	                	case SensorManager.SENSOR_DELAY_NORMAL:
		                	sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL, null);
	                		break;
	                	default:
		                	sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME, null);
	                		break;
	                }
	                
	                this._lastFrequency = frequency;
				}
				
				return true;
        	}
        	else
        	{
                sensors.unregisterListener(this, sensor);
                this._lastFrequency = -1;
        	}
        }
    	else
    	{
            sensors.unregisterListener(this, sensor);
            this._lastFrequency = -1;
    	}

        return false;
	}
	
	public void updateFromMap(Context context, Map<String, Object> params) 
	{
		super.updateFromMap(context, params);
		
		if (params.containsKey(ContinuousProbe.PROBE_THRESHOLD))
		{
			Object threshold = params.get(ContinuousProbe.PROBE_THRESHOLD);
			
			if (threshold instanceof Double)
			{
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putString("config_probe_geomagnetic_rotation_threshold", threshold.toString());
				e.commit();
			}
		}
	}

	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceScreen screen = super.preferenceScreen(activity);
		
		screen.removePreference(screen.findPreference("config_probe_rotation_threshold"));

		ListPreference threshold = new ListPreference(activity);
		threshold.setKey("config_probe_geomagnetic_rotation_threshold");
		threshold.setDefaultValue(RotationProbe.DEFAULT_THRESHOLD);
		threshold.setEntryValues(R.array.probe_rotation_threshold);
		threshold.setEntries(R.array.probe_rotation_threshold_labels);
		threshold.setTitle(R.string.probe_noise_threshold_label);
		threshold.setSummary(R.string.probe_noise_threshold_summary);

		screen.addPreference(threshold);

		return screen;
	}

	public String getPreferenceKey()
	{
		return "geomagnetic_rotation_built_in";
	}

	public int getSummaryResource()
	{
		return R.string.summary_geomagnetic_rotation_probe_desc;
	}
}
