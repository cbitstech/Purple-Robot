package edu.northwestern.cbits.purple_robot_manager.activities.settings;

import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.UUID;

import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.models.ModelManager;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

/**
 * Created by Administrator on 1/13/15.
 */

@TargetApi(11)
public class SettingsActivity extends ActionBarActivity
{
    private static final String PREFERENCE_SCREEN_KEY = "PREFERENCE_SCREEN_KEY";
    private static HashMap<String, PreferenceScreen> _screens = new HashMap<String, PreferenceScreen>();

    public void onNewIntent(Intent intent)
    {
        Log.e("PR", "NEW INTENT " + intent.getStringExtra(SettingsActivity.PREFERENCE_SCREEN_KEY));
    }

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        final String key = this.getIntent().getStringExtra(SettingsActivity.PREFERENCE_SCREEN_KEY);
        Log.e("PR", "LAUNCHED WITH KEY: " + key);

        this.setContentView(R.layout.layout_settings_activity);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        final ViewGroup view = (ViewGroup) this.findViewById(R.id.content_frame);

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final SettingsActivity me = this;

        FragmentManager fragment = this.getFragmentManager();

        FragmentTransaction transaction = fragment.beginTransaction();

        final PreferenceFragment prefFragment = new PreferenceFragment()
        {
            public void onCreate(Bundle savedInstanceState)
            {
                super.onCreate(savedInstanceState);

                if (key == null)
                {
                    this.addPreferencesFromResource(R.xml.settings);

                    me.setTitle(R.string.title_settings);

                    RobotPreferenceListener listener = new RobotPreferenceListener(me);

                    PreferenceScreen prefs = this.getPreferenceScreen();

                    Preference refresh = prefs.findPreference(BaseSettingsActivity.MANUAL_REFRESH_KEY);
                    refresh.setOnPreferenceClickListener(listener);

                    Preference logRefresh = prefs.findPreference(BaseSettingsActivity.LOG_REFRESH_KEY);
                    logRefresh.setOnPreferenceClickListener(listener);

                    ListPreference haptic = (ListPreference) prefs.findPreference(BaseSettingsActivity.HAPTIC_PATTERN_KEY);
                    haptic.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                    {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue)
                        {
                            String pattern = (String) newValue;

                            Intent intent = new Intent(ManagerService.HAPTIC_PATTERN_INTENT);
                            intent.putExtra(ManagerService.HAPTIC_PATTERN_NAME, pattern);
                            intent.setClass(me, ManagerService.class);

                            me.startService(intent);

                            return true;
                        }
                    });

                    PreferenceScreen probesScreen = ProbeManager.buildPreferenceScreen(me, this.getPreferenceManager());

                    PreferenceCategory category = (PreferenceCategory) prefs.findPreference("config_settings_probe_category");
                    category.addPreference(probesScreen);

                    PreferenceScreen triggersScreen = TriggerManager.getInstance(me).buildPreferenceScreen(me, this.getPreferenceManager());

                    PreferenceCategory triggerCategory = (PreferenceCategory) prefs.findPreference("config_settings_trigger_category");
                    triggerCategory.addPreference(triggersScreen);

                    PreferenceScreen modelsScreen = ModelManager.getInstance(me).buildPreferenceScreen(me, this.getPreferenceManager());

                    PreferenceCategory modelCategory = (PreferenceCategory) prefs.findPreference("config_settings_models_category");
                    modelCategory.addPreference(modelsScreen);

                    Preference archive = prefs.findPreference(BaseSettingsActivity.ZIP_ARCHIVES_KEY);
                    archive.setOnPreferenceClickListener(listener);

                    Preference delete = prefs.findPreference(BaseSettingsActivity.DELETE_ARCHIVES_KEY);
                    delete.setOnPreferenceClickListener(listener);

                    Preference test = prefs.findPreference(BaseSettingsActivity.RUN_TESTS_KEY);
                    test.setOnPreferenceClickListener(listener);

                    CheckBoxPreference update = (CheckBoxPreference) prefs.findPreference(BaseSettingsActivity.CHECK_UPDATES_KEY);
                    update.setOnPreferenceChangeListener(listener);

                    ListPreference listUpdate = (ListPreference) prefs.findPreference(BaseSettingsActivity.RINGTONE_KEY);
                    listUpdate.setOnPreferenceChangeListener(listener);

                    Preference reset = prefs.findPreference(BaseSettingsActivity.RESET_KEY);
                    reset.setOnPreferenceClickListener(listener);

                    Preference logEnabled = prefs.findPreference(LogManager.ENABLED);
                    logEnabled.setOnPreferenceChangeListener(listener);

                    Preference logUri = prefs.findPreference(LogManager.URI);
                    logUri.setOnPreferenceChangeListener(listener);

                    Preference logLocation = prefs.findPreference(LogManager.INCLUDE_LOCATION);
                    logLocation.setOnPreferenceChangeListener(listener);

                    Preference logWifi = prefs.findPreference(LogManager.WIFI_ONLY);
                    logWifi.setOnPreferenceChangeListener(listener);

                    Preference logHeartbeat = prefs.findPreference(LogManager.HEARTBEAT);
                    logHeartbeat.setOnPreferenceChangeListener(listener);

                    Preference logInterval = prefs.findPreference(LogManager.UPLOAD_INTERVAL);
                    logInterval.setOnPreferenceChangeListener(listener);

                    final PreferenceFragment meFragment = this;

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
                                e.printStackTrace();
                            }

                            me.runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    me.mapScreens(meFragment.getPreferenceScreen());
                                }
                            });
                        }
                    };

                    Thread t = new Thread(r);
                    t.start();

                    PurpleRobotApplication.fixPreferences(me, true);
                }
                else
                {
                    this.setPreferenceScreen(SettingsActivity._screens.get(key));
                    me.setTitle(SettingsActivity._screens.get(key).getTitle());
                }
            }

            public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference)
            {
                Log.e("PR", "PREF TREE CLICKED: " + preference.getTitle() + " -- " + (screen == null) + " -- " + TextUtils.isEmpty(screen.getTitle()));
                Log.e("PR", "TOOLBAR VIZ(1): " + toolbar.getVisibility() + " -- " + me.getSupportActionBar());
                Log.e("PR", "VIEW: " + view);

                return super.onPreferenceTreeClick(screen, preference);
            }
        };

        transaction.replace(R.id.content_frame, prefFragment);
        transaction.commit();

        LogManager.getInstance(this).log("pr_settings_visited", null);
    }

    private void mapScreens(PreferenceGroup screen)
    {
        Log.e("PR", "MAP: " + screen.getClass().getCanonicalName());

        if (screen.getKey() == null)
        {
            String key = UUID.randomUUID().toString();

            screen.setKey(key);
        }

        if (screen instanceof PreferenceScreen)
            SettingsActivity._screens.put(screen.getKey(), (PreferenceScreen) screen);

        for (int i = 0; i < screen.getPreferenceCount(); i++)
        {
            Preference pref = screen.getPreference(i);

            Log.e("PR", "MAP PREF: " + pref.getClass().getCanonicalName());

            if (pref instanceof PreferenceGroup)
                this.mapScreens((PreferenceGroup) pref);

            if (pref instanceof PreferenceScreen)
            {
                Log.e("PR", "PREF: " + pref.getTitle() + " -- " + pref.getClass().getCanonicalName());

                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra(SettingsActivity.PREFERENCE_SCREEN_KEY, pref.getKey());

                Log.e("PR", "SWAPPING PREF");

//                Preference newScreen = new Preference(this);
//                newScreen.setTitle(pref.getTitle());
//                newScreen.setOrder(pref.getOrder());
                pref.setIntent(intent);

//                screen.removePreference(pref);
//                screen.addPreference(newScreen);
            }
        }
    }

    protected void onDestroy()
    {
        super.onDestroy();

        LogManager.getInstance(this).log("pr_settings_exited", null);
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            this.finish();
        }

        return true;
    }
}
