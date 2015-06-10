package edu.northwestern.cbits.purple_robot_manager.activities.settings;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.http.LocalHttpServer;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.models.ModelManager;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class LegacySettingsActivity extends PreferenceActivity
{
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        PurpleRobotApplication.fixPreferences(this, true);

        this.addPreferencesFromResource(R.xml.settings);

        RobotPreferenceListener listener = new RobotPreferenceListener(this);

        PreferenceScreen prefs = this.getPreferenceScreen();

        Preference refresh = prefs.findPreference(SettingsKeys.MANUAL_REFRESH_KEY);
        refresh.setOnPreferenceClickListener(listener);

        Preference logRefresh = prefs.findPreference(SettingsKeys.LOG_REFRESH_KEY);
        logRefresh.setOnPreferenceClickListener(listener);

        final LegacySettingsActivity me = this;

        ListPreference haptic = (ListPreference) prefs.findPreference(SettingsKeys.HAPTIC_PATTERN_KEY);
        haptic.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
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

        PreferenceScreen probesScreen = ProbeManager.buildPreferenceScreen(this, this.getPreferenceManager());

        PreferenceCategory category = (PreferenceCategory) prefs.findPreference("config_settings_probe_category");
        category.addPreference(probesScreen);

        PreferenceScreen triggersScreen = TriggerManager.getInstance(this).buildPreferenceScreen(this, this.getPreferenceManager());

        PreferenceCategory triggerCategory = (PreferenceCategory) prefs
                .findPreference("config_settings_trigger_category");
        triggerCategory.addPreference(triggersScreen);

        PreferenceScreen modelsScreen = ModelManager.getInstance(this).buildPreferenceScreen(this, this.getPreferenceManager());

        PreferenceCategory modelCategory = (PreferenceCategory) prefs.findPreference("config_settings_models_category");
        modelCategory.addPreference(modelsScreen);

        Preference archive = prefs.findPreference(SettingsKeys.ZIP_ARCHIVES_KEY);
        archive.setOnPreferenceClickListener(listener);

        Preference delete = prefs.findPreference(SettingsKeys.DELETE_ARCHIVES_KEY);
        delete.setOnPreferenceClickListener(listener);

        Preference test = prefs.findPreference(SettingsKeys.RUN_TESTS_KEY);
        test.setOnPreferenceClickListener(listener);

//        CheckBoxPreference update = (CheckBoxPreference) prefs.findPreference(SettingsKeys.CHECK_UPDATES_KEY);
//        update.setOnPreferenceChangeListener(listener);

        ListPreference listUpdate = (ListPreference) prefs.findPreference(SettingsKeys.RINGTONE_KEY);
        listUpdate.setOnPreferenceChangeListener(listener);

        Preference reset = prefs.findPreference(SettingsKeys.RESET_KEY);
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

        Preference configUrl = prefs.findPreference(SettingsKeys.CONFIG_URL);
        configUrl.setOnPreferenceChangeListener(listener);

        Preference enableHttpServer = prefs.findPreference(LocalHttpServer.BUILTIN_HTTP_SERVER_ENABLED);
        enableHttpServer.setOnPreferenceChangeListener(listener);

        Preference enableZeroconf = prefs.findPreference(LocalHttpServer.BUILTIN_ZEROCONF_ENABLED);
        enableZeroconf.setOnPreferenceChangeListener(listener);

        Preference enableZeroconfName = prefs.findPreference(LocalHttpServer.BUILTIN_ZEROCONF_NAME);
        enableZeroconfName.setOnPreferenceChangeListener(listener);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
        {
            PreferenceScreen screen = (PreferenceScreen) this.findPreference("screen_builtin_http_server");

            screen.removePreference(enableZeroconf);
            screen.removePreference(enableZeroconfName);
        }

        Preference exportBootstrap = prefs.findPreference("config_export_bootstrap");
        exportBootstrap.setOnPreferenceClickListener(listener);

        LogManager.getInstance(me).log("pr_settings_visited", null);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        LogManager.getInstance(this).log("pr_settings_exited", null);
    }
}
