package edu.northwestern.cbits.purple_robot_manager;

import java.util.ArrayList;

import android.location.Location;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

@SuppressWarnings("deprecation")
public class ProbeViewerActivity extends PreferenceActivity
{
	private String _probeName = null;
	private Bundle _probeBundle = null;
	private Probe _probe = null;

	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle bundle = this.getIntent().getExtras();

        this._probeName = bundle.getString("probe_name");
        this._probeBundle = bundle.getBundle("probe_bundle");

        if (bundle.getBoolean("is_model", false))
        {
            PreferenceScreen screen = this.screenForBundle(this._probeName, this._probeBundle);

            this.setPreferenceScreen(screen);
        }
        else
        {
	        this._probe = ProbeManager.probeForName(this._probeName, this);
	
	        if (this._probe != null)
	        {
		        Bundle formattedBundle = this._probe.formattedBundle(this, this._probeBundle);
	
		        if (formattedBundle != null)
		        {
		            PreferenceScreen screen = this.screenForBundle(this._probe.title(this), formattedBundle);
	
		            screen.addPreference(this.screenForBundle(this.getString(R.string.display_raw_data), this._probeBundle));
	
		    		this.setPreferenceScreen(screen);
		        }
		        else
		        {
		            PreferenceScreen screen = this.screenForBundle(this._probe.title(this), this._probeBundle);
	
		            this.setPreferenceScreen(screen);
		        }
	        }
        }
    }

	private PreferenceScreen screenForFloatArray(String title, float[] values)
	{
		PreferenceManager manager = this.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(this);

		screen.setTitle(title);

		for (float value : values)
		{
			Preference pref = new Preference(this);
			pref.setTitle("" + value);

			screen.addPreference(pref);
		}

		return screen;
	}

	private PreferenceScreen screenForIntArray(String title, int[] values)
	{
		PreferenceManager manager = this.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(this);

		screen.setTitle(title);

		for (int value : values)
		{
			Preference pref = new Preference(this);
			pref.setTitle("" + value);

			screen.addPreference(pref);
		}

		return screen;
	}

	private PreferenceScreen screenForDoubleArray(String title, double[] values)
	{
		PreferenceManager manager = this.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(this);

		screen.setTitle(title);

		for (double value : values)
		{
			Preference pref = new Preference(this);
			pref.setTitle("" + value);

			screen.addPreference(pref);
		}

		return screen;
	}

	private PreferenceScreen screenForLongArray(String title, long[] values)
	{
		PreferenceManager manager = this.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(this);

		screen.setTitle(title);

		for (long value : values)
		{
			Preference pref = new Preference(this);
			pref.setTitle("" + value);

			screen.addPreference(pref);
		}

		return screen;
	}

	private PreferenceScreen screenForStringArray(String title, String[] values)
	{
		PreferenceManager manager = this.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(this);

		screen.setTitle(title);

		for (String value : values)
		{
			Preference pref = new Preference(this);
			pref.setTitle(value);

			screen.addPreference(pref);
		}

		return screen;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private PreferenceScreen screenForBundle(String title, Bundle bundle)
	{
		PreferenceManager manager = this.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(this);

		screen.setTitle(title);

		ArrayList<String> keys = new ArrayList<String>();

		if (bundle.containsKey("KEY_ORDER"))
			keys.addAll(bundle.getStringArrayList("KEY_ORDER"));
		else
			keys.addAll(bundle.keySet());

		for (String key : keys)
		{
			Object o = bundle.get(key);

			if (o == null)
			{
				Log.e("PRM", "NULL KEY (" + title + "): " + key);
			}
			else if (o instanceof Bundle)
			{
				Bundle b = (Bundle) o;

				PreferenceScreen subscreen = this.screenForBundle(key, b);

				screen.addPreference(subscreen);
			}
			else if (o instanceof float[])
			{
				float[] array = (float[]) o;

				if (array.length > 1)
				{
					PreferenceScreen subscreen = this.screenForFloatArray(key, array);
					subscreen.setSummary(String.format(this.getString(R.string.display_probe_values), array.length));

					screen.addPreference(subscreen);
				}
				else
				{
					Preference pref = new Preference(this);
					pref.setTitle("" + array[0]);
					pref.setSummary(key);

					screen.addPreference(pref);
				}
			}
			else if (o instanceof double[])
			{
				double[] array = (double[]) o;

				if (array.length > 1)
				{
					PreferenceScreen subscreen = this.screenForDoubleArray(key, array);
					subscreen.setSummary(String.format(this.getString(R.string.display_probe_values), array.length));

					screen.addPreference(subscreen);
				}
				else
				{
					Preference pref = new Preference(this);
					pref.setTitle("" + array[0]);
					pref.setSummary(key);

					screen.addPreference(pref);
				}
			}
			else if (o instanceof Location[])
			{
				Location[] array = (Location[]) o;

				if (array.length > 1)
				{
					PreferenceScreen subscreen = this.screenForLocationArray(key, array);
					subscreen.setSummary(String.format(this.getString(R.string.display_probe_values), array.length));

					screen.addPreference(subscreen);
				}
				else
				{
					Preference pref = new Preference(this);
					pref.setTitle(array[0].getProvider() + ": " + array[0].getLatitude() + "," + array[0].getLongitude());
					pref.setSummary(key);

					screen.addPreference(pref);
				}
			}
			else if (o instanceof Location)
			{
				Location location = (Location) o;

				Preference pref = new Preference(this);
				pref.setTitle(location.getProvider() + ": " + location.getLatitude() + "," + location.getLongitude());
				pref.setSummary(key);

				screen.addPreference(pref);
			}
			else if (o instanceof Bundle[])
			{
				Bundle[] array = (Bundle[]) o;

				if (array.length > 1)
				{
					PreferenceScreen subscreen = this.screenForBundleArray(key, array);
					subscreen.setSummary(String.format(this.getString(R.string.display_probe_values), array.length));

					screen.addPreference(subscreen);
				}
				else
				{
					Preference pref = new Preference(this);
					pref.setTitle("" + array[0]);
					pref.setSummary(key);

					screen.addPreference(pref);
				}
			}
			else if (o instanceof int[])
			{
				int[] array = (int[]) o;

				if (array.length > 1)
				{
					PreferenceScreen subscreen = this.screenForIntArray(key, array);
					subscreen.setSummary(String.format(this.getString(R.string.display_probe_values), array.length));

					screen.addPreference(subscreen);
				}
				else
				{
					Preference pref = new Preference(this);
					pref.setTitle("" + array[0]);
					pref.setSummary(key);

					screen.addPreference(pref);
				}
			}
			else if (o instanceof long[])
			{
				long[] array = (long[]) o;

				if (array.length > 1)
				{
					PreferenceScreen subscreen = this.screenForLongArray(key, array);
					subscreen.setSummary(String.format(this.getString(R.string.display_probe_values), array.length));

					screen.addPreference(subscreen);
				}
				else
				{
					Preference pref = new Preference(this);
					pref.setTitle("" + array[0]);
					pref.setSummary(key);

					screen.addPreference(pref);
				}
			}
			else if (o instanceof String[])
			{
				String[] array = (String[]) o;

				if (array.length > 1)
				{
					PreferenceScreen subscreen = this.screenForStringArray(key, array);
					subscreen.setSummary(String.format(this.getString(R.string.display_probe_values), array.length));

					screen.addPreference(subscreen);
				}
				else
				{
					Preference pref = new Preference(this);
					pref.setTitle("" + array[0]);
					pref.setSummary(key);

					screen.addPreference(pref);
				}
			}
			else if (o instanceof ArrayList)
			{
				ArrayList array = (ArrayList) o;

				if (array.size() > 0)
				{
					Object oo = array.get(0);

					if (oo instanceof Bundle)
					{
						if (array.size() > 1)
						{
							PreferenceScreen subscreen = this.screenForBundleArray(key, (Bundle[]) array.toArray(new Bundle[0]));
							subscreen.setSummary(String.format(this.getString(R.string.display_probe_values), array.size()));

							screen.addPreference(subscreen);
						}
						else
						{
							Preference pref = new Preference(this);
							pref.setTitle("" + oo);
							pref.setSummary(key);

							screen.addPreference(pref);
						}
					}
				}
			}
			else
			{
				String desc = o.toString();

				Preference pref = new Preference(this);
				pref.setTitle(desc);
				pref.setSummary(key);

				screen.addPreference(pref);
			}
		}

		return screen;
	}

	private PreferenceScreen screenForLocationArray(String title, Location[] values)
	{
		PreferenceManager manager = this.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(this);

		screen.setTitle(title);

		for (Location value : values)
		{
			Preference pref = new Preference(this);

			pref.setTitle(value.getProvider() + ": " + value.getLatitude() + "," + value.getLongitude());

			screen.addPreference(pref);
		}

		return screen;
	}

	private PreferenceScreen screenForBundleArray(String title, Bundle[] values)
	{
		PreferenceManager manager = this.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(this);

		screen.setTitle(title);

		for (Bundle value : values)
		{
			Preference pref = this.screenForBundle(title, value);

			pref.setTitle(this.getString(R.string.display_data_bundle));

			screen.addPreference(pref);
		}

		return screen;
	}
}
