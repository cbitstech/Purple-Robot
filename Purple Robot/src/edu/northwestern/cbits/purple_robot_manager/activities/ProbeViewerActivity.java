package edu.northwestern.cbits.purple_robot_manager.activities;

import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ProbeViewerActivity extends AppCompatActivity
{
    private String _probeName = null;
    private Bundle _probeBundle = null;
    private Probe _probe = null;

    private static final String PREFERENCE_SCREEN_KEY = "PREFERENCE_SCREEN_KEY";
    private static HashMap<String, PreferenceScreen> _screens = new HashMap<>();

    public static class ProbeViewerFragment extends PreferenceFragment
    {
        public ProbeViewerFragment()
        {
            super();
        }

        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            final ProbeViewerActivity activity = (ProbeViewerActivity) this.getActivity();

            Bundle arguments = this.getArguments();

            String key = arguments.getString(ProbeViewerActivity.PREFERENCE_SCREEN_KEY);

            // If launched with no key, build out settings from the top...

            if (key == null)
            {
                if (arguments.getBoolean("is_model", false))
                {
                    PreferenceScreen screen = ProbeViewerActivity.screenForBundle(activity, this.getPreferenceManager(), activity._probeName, activity._probeBundle);

                    this.setPreferenceScreen(screen);
                }
                else
                {
                    activity._probe = ProbeManager.probeForName(activity._probeName, activity);

                    if (activity._probe != null)
                    {
                        activity.setTitle(activity._probe.title(activity));

                        Bundle formattedBundle = activity._probe.formattedBundle(activity, activity._probeBundle);

                        if (formattedBundle != null)
                        {
                            PreferenceScreen screen = ProbeViewerActivity.screenForBundle(activity, this.getPreferenceManager(), activity._probe.title(activity), formattedBundle);

                            screen.addPreference(ProbeViewerActivity.screenForBundle(activity, this.getPreferenceManager(), activity.getString(R.string.display_raw_data), activity._probeBundle));

                            this.setPreferenceScreen(screen);
                        }
                        else
                        {
                            PreferenceScreen screen = ProbeViewerActivity.screenForBundle(activity, this.getPreferenceManager(), activity._probe.title(activity), activity._probeBundle);

                            this.setPreferenceScreen(screen);
                        }
                    }
                }

                final PreferenceFragment meFragment = this;

                // Delay for half a second so preferences can be completely constructed...

                Runnable r = new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            Thread.sleep(500);
                        }
                        catch (InterruptedException e)
                        {

                        }

                        activity.runOnUiThread(new Runnable()
                        {
                            public void run()
                            {
                                // After delay, build preference screen map...

                                activity.mapScreens(meFragment.getPreferenceScreen());
                            }
                        });
                    }
                };

                Thread t = new Thread(r);
                t.start();

                PurpleRobotApplication.fixPreferences(activity, true);
            }
            else
            {
                // If launched with a key, lookup the preference screen and go from there...

                this.setPreferenceScreen(ProbeViewerActivity._screens.get(key));
                activity.setTitle(ProbeViewerActivity._screens.get(key).getTitle());
            }
        }

    }

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle bundle = this.getIntent().getExtras();

        this._probeName = bundle.getString("probe_name");
        this._probeBundle = bundle.getBundle("probe_bundle");

        this.setContentView(R.layout.layout_settings_activity);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final ProbeViewerActivity me = this;

        FragmentManager fragment = this.getFragmentManager();

        FragmentTransaction transaction = fragment.beginTransaction();

        PreferenceFragment prefFragment = new ProbeViewerFragment();
        prefFragment.setArguments(bundle);

        transaction.replace(R.id.content_frame, prefFragment);
        transaction.commit();
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            this.finish();
        }

        return true;
    }

    private void mapScreens(PreferenceGroup screen)
    {
        if (screen == null)
            return;

        // If the screen does not have a key, generate one.

        if (screen.getKey() == null)
        {
            String key = UUID.randomUUID().toString();

            screen.setKey(key);
        }

        // If what we're looking at is a screen, add it to the shared map.

        if (screen instanceof PreferenceScreen)
            ProbeViewerActivity._screens.put(screen.getKey(), (PreferenceScreen) screen);

        // Iterate and recurse...

        for (int i = 0; i < screen.getPreferenceCount(); i++)
        {
            Preference pref = screen.getPreference(i);

            // If this is a preference group, recursively map it.

            if (pref instanceof PreferenceGroup)
                this.mapScreens((PreferenceGroup) pref);

            if (pref instanceof PreferenceScreen)
            {
                // Add activity intent to launch new SettingActivity instances to override
                // dialog-based behavior.

                Intent intent = new Intent(this, ProbeViewerActivity.class);
                intent.putExtra(ProbeViewerActivity.PREFERENCE_SCREEN_KEY, pref.getKey());

                pref.setIntent(intent);
            }
        }
    }
    public static PreferenceScreen screenForFloatArray(Context context, PreferenceManager manager, String title, float[] values)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);

        screen.setTitle(title);

        for (float value : values)
        {
            Preference pref = new Preference(context);
            pref.setTitle("" + value);

            screen.addPreference(pref);
        }

        return screen;
    }

    public static PreferenceScreen screenForIntArray(Context context, PreferenceManager manager, String title, int[] values)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);

        screen.setTitle(title);

        for (int value : values)
        {
            Preference pref = new Preference(context);
            pref.setTitle("" + value);

            screen.addPreference(pref);
        }

        return screen;
    }

    public static PreferenceScreen screenForDoubleArray(Context context, PreferenceManager manager, String title, double[] values)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);

        screen.setTitle(title);

        for (double value : values)
        {
            Preference pref = new Preference(context);
            pref.setTitle("" + value);

            screen.addPreference(pref);
        }

        return screen;
    }

    public static PreferenceScreen screenForLongArray(Context context, PreferenceManager manager, String title, long[] values)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);

        screen.setTitle(title);

        for (long value : values)
        {
            Preference pref = new Preference(context);
            pref.setTitle("" + value);

            screen.addPreference(pref);
        }

        return screen;
    }

    public static PreferenceScreen screenForStringArray(Context context, PreferenceManager manager, String title, String[] values)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);

        screen.setTitle(title);

        for (String value : values)
        {
            Preference pref = new Preference(context);
            pref.setTitle(value);

            screen.addPreference(pref);
        }

        return screen;
    }

    @SuppressWarnings(
    { "rawtypes", "unchecked" })
    public static PreferenceScreen screenForBundle(Context context, PreferenceManager manager, String title, Bundle bundle)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);

        screen.setTitle(title);

        ArrayList<String> keys = new ArrayList<>();

        if (bundle.containsKey("KEY_ORDER"))
            keys.addAll(bundle.getStringArrayList("KEY_ORDER"));
        else
        {
            keys.addAll(bundle.keySet());
            Collections.sort(keys);
        }

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

                PreferenceScreen subscreen = ProbeViewerActivity.screenForBundle(context, manager, key, b);

                screen.addPreference(subscreen);
            }
            else if (o instanceof float[])
            {
                float[] array = (float[]) o;

                if (array.length > 1)
                {
                    PreferenceScreen subscreen = ProbeViewerActivity.screenForFloatArray(context, manager, key, array);
                    subscreen.setSummary(String.format(context.getString(R.string.display_probe_values), array.length));

                    screen.addPreference(subscreen);
                }
                else
                {
                    Preference pref = new Preference(context);
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
                    PreferenceScreen subscreen = ProbeViewerActivity.screenForDoubleArray(context, manager, key, array);
                    subscreen.setSummary(String.format(context.getString(R.string.display_probe_values), array.length));

                    screen.addPreference(subscreen);
                }
                else
                {
                    Preference pref = new Preference(context);
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
                    PreferenceScreen subscreen = ProbeViewerActivity.screenForLocationArray(context, manager, key, array);
                    subscreen.setSummary(String.format(context.getString(R.string.display_probe_values), array.length));

                    screen.addPreference(subscreen);
                }
                else
                {
                    Preference pref = new Preference(context);
                    pref.setTitle(array[0].getProvider() + ": " + array[0].getLatitude() + ","
                            + array[0].getLongitude());
                    pref.setSummary(key);

                    screen.addPreference(pref);
                }
            }
            else if (o instanceof Location)
            {
                Location location = (Location) o;

                Preference pref = new Preference(context);
                pref.setTitle(location.getProvider() + ": " + location.getLatitude() + "," + location.getLongitude());
                pref.setSummary(key);

                screen.addPreference(pref);
            }
            else if (o instanceof Bundle[])
            {
                Bundle[] array = (Bundle[]) o;

                PreferenceScreen subscreen = ProbeViewerActivity.screenForBundleArray(context, manager, key, array);
                subscreen.setSummary(String.format(context.getString(R.string.display_probe_values), array.length));

                screen.addPreference(subscreen);
            }
            else if (o instanceof int[])
            {
                int[] array = (int[]) o;

                if (array.length > 1)
                {
                    PreferenceScreen subscreen = ProbeViewerActivity.screenForIntArray(context, manager, key, array);
                    subscreen.setSummary(String.format(context.getString(R.string.display_probe_values), array.length));

                    screen.addPreference(subscreen);
                }
                else
                {
                    Preference pref = new Preference(context);
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
                    PreferenceScreen subscreen = ProbeViewerActivity.screenForLongArray(context, manager, key, array);
                    subscreen.setSummary(String.format(context.getString(R.string.display_probe_values), array.length));

                    screen.addPreference(subscreen);
                }
                else
                {
                    Preference pref = new Preference(context);
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
                    PreferenceScreen subscreen = ProbeViewerActivity.screenForStringArray(context, manager, key, array);
                    subscreen.setSummary(String.format(context.getString(R.string.display_probe_values), array.length));

                    screen.addPreference(subscreen);
                }
                else
                {
                    Preference pref = new Preference(context);
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
                        PreferenceScreen subscreen = ProbeViewerActivity.screenForBundleArray(context, manager, key, (Bundle[]) array.toArray(new Bundle[0]));
                        subscreen.setSummary(String.format(context.getString(R.string.display_probe_values), array.size()));

                        screen.addPreference(subscreen);
                    }
                }
            }
            else
            {
                String desc = o.toString();

                Preference pref = new Preference(context);
                pref.setTitle(desc);
                pref.setSummary(key);

                screen.addPreference(pref);
            }
        }

        return screen;
    }

    public static PreferenceScreen screenForLocationArray(Context context, PreferenceManager manager, String title, Location[] values)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);

        screen.setTitle(title);

        for (Location value : values)
        {
            Preference pref = new Preference(context);

            pref.setTitle(value.getProvider() + ": " + value.getLatitude() + "," + value.getLongitude());

            screen.addPreference(pref);
        }

        return screen;
    }

    public static PreferenceScreen screenForBundleArray(Context context, PreferenceManager manager, String title, Bundle[] values)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);

        screen.setTitle(title);

        for (Bundle value : values)
        {
            Preference pref = ProbeViewerActivity.screenForBundle(context, manager, title, value);

            pref.setTitle(context.getString(R.string.display_data_bundle));

            screen.addPreference(pref);
        }

        return screen;
    }
}
