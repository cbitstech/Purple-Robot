package edu.northwestern.cbits.purple_robot_manager;

import android.os.Bundle;

import com.WazaBe.HoloEverywhere.preference.Preference;
import com.WazaBe.HoloEverywhere.preference.PreferenceManager;
import com.WazaBe.HoloEverywhere.preference.PreferenceScreen;
import com.WazaBe.HoloEverywhere.sherlock.SPreferenceActivity;

import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

public class ProbeViewerActivity extends SPreferenceActivity
{
	private String _probeName = null;
	private Bundle _probeBundle = null;
	private Probe _probe = null;

	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle bundle = this.getIntent().getExtras();

        this._probeName = bundle.getString("probe_name");
        this._probeBundle = bundle.getBundle("probe_bundle");

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

	@SuppressWarnings("deprecation")
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

	@SuppressWarnings("deprecation")
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

	@SuppressWarnings("deprecation")
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

	@SuppressWarnings("deprecation")
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

	@SuppressWarnings("deprecation")
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

	@SuppressWarnings("deprecation")
	private PreferenceScreen screenForBundle(String title, Bundle bundle)
	{
		PreferenceManager manager = this.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(this);

		screen.setTitle(title);

		for (String key : bundle.keySet())
		{
			Object o = bundle.get(key);

			if (o instanceof Bundle)
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
					subscreen.setSummary(array.length + " vALUES");

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
					subscreen.setSummary(array.length + " vALUES");

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
					subscreen.setSummary(array.length + " vALUES");

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
					subscreen.setSummary(array.length + " vALUES");

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
					subscreen.setSummary(array.length + " vALUES");

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
}
