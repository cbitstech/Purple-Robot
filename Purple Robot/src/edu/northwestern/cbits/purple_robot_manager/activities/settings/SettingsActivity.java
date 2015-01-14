package edu.northwestern.cbits.purple_robot_manager.activities.settings;

import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.Window;

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
public class SettingsActivity extends PreferenceActivity
{
    public void onCreate(Bundle savedInstanceState)
    {
        this.requestWindowFeature(Window.FEATURE_ACTION_BAR);

        super.onCreate(savedInstanceState);

        this.setTitle(R.string.title_settings);
//        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final SettingsActivity me = this;

        FragmentManager fragment = this.getFragmentManager();

        FragmentTransaction transaction = fragment.beginTransaction();

        transaction.replace(android.R.id.content, new PreferenceFragment()
        {
            public void onCreate(Bundle savedInstanceState)
            {
                super.onCreate(savedInstanceState);

                this.addPreferencesFromResource(R.xml.settings);

                PurpleRobotApplication.fixPreferences(me, true);

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

                PreferenceCategory triggerCategory = (PreferenceCategory) prefs
                        .findPreference("config_settings_trigger_category");
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
            }
        });

        transaction.commit();

        LogManager.getInstance(this).log("pr_settings_visited", null);
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
