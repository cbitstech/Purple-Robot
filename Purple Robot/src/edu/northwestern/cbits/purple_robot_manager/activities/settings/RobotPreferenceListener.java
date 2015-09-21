package edu.northwestern.cbits.purple_robot_manager.activities.settings;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import edu.northwestern.cbits.anthracite.LogService;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.PersistentService;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.RobotContentProvider;
import edu.northwestern.cbits.purple_robot_manager.activities.TestActivity;
import edu.northwestern.cbits.purple_robot_manager.config.LegacyJSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.db.DistancesProvider;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.http.LocalHttpServer;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.output.BootstrapSiteExporter;
import edu.northwestern.cbits.purple_robot_manager.plugins.HttpUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPluginManager;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class RobotPreferenceListener implements Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener
{
    private Context _context = null;

    public RobotPreferenceListener(Context context)
    {
        this._context = context;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onPreferenceClick(Preference preference)
    {
        if (SettingsKeys.HAPTIC_PATTERN_KEY.equals(preference.getKey()))
        {
            ListPreference listPref = (ListPreference) preference;

            String pattern = listPref.getValue();

            Intent intent = new Intent(ManagerService.HAPTIC_PATTERN_INTENT);
            intent.putExtra(ManagerService.HAPTIC_PATTERN_NAME, pattern);
            intent.setClass(this._context, ManagerService.class);

            this._context.startService(intent);

            return true;
        }
        else if (SettingsKeys.PROBES_SCREEN_KEY.equals(preference.getKey()))
            return true;
        else if (SettingsKeys.MANUAL_REFRESH_KEY.equals(preference.getKey()))
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context.getApplicationContext());
            SharedPreferences.Editor editor = prefs.edit();

            editor.putLong(LegacyJSONConfigFile.JSON_LAST_UPDATE, 0);
            editor.remove(LegacyJSONConfigFile.JSON_LAST_HASH);

            editor.commit();
            LegacyJSONConfigFile.update(this._context, true);

            ProbeManager.nudgeProbes(this._context);
            TriggerManager.getInstance(this._context).refreshTriggers(this._context);

            return true;
        }
        else if (SettingsKeys.LOG_REFRESH_KEY.equals(preference.getKey()))
        {
            try
            {
                PackageInfo info = this._context.getPackageManager().getPackageInfo(this._context.getPackageName(), 0);

                Intent refreshIntent = new Intent(info.packageName + ".UPLOAD_LOGS_INTENT");
                refreshIntent.putExtra(LogService.LOG_FORCE_UPLOAD, true);
                refreshIntent.setClass(this._context, LogService.class);

                int eventCount = LogManager.getInstance(this._context).pendingEventsCount();

                String message = this._context.getString(R.string.toast_transmitting_events, eventCount);

                Toast.makeText(this._context, message, Toast.LENGTH_SHORT).show();

                this._context.startService(refreshIntent);
            }
            catch (PackageManager.NameNotFoundException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }

            return true;
        }
        else if (SettingsKeys.ZIP_ARCHIVES_KEY.equals(preference.getKey()))
        {
            HttpUploadPlugin plugin = (HttpUploadPlugin) OutputPluginManager.sharedInstance.pluginForClass(this._context,
                    HttpUploadPlugin.class);

            if (plugin != null)
            {
                plugin.mailArchiveFiles(this._context);

                return true;
            }
        }
        else if (SettingsKeys.DELETE_ARCHIVES_KEY.equals(preference.getKey()))
        {
            HttpUploadPlugin plugin = (HttpUploadPlugin) OutputPluginManager.sharedInstance.pluginForClass(this._context,
                    HttpUploadPlugin.class);

            if (plugin != null)
            {
                plugin.deleteArchiveFiles(this._context);

                return true;
            }
        }
        else if (SettingsKeys.RUN_TESTS_KEY.equals(preference.getKey()))
        {
            Intent intent = new Intent(this._context, TestActivity.class);
            this._context.startActivity(intent);
        }
        else if (SettingsKeys.RESET_KEY.equals(preference.getKey()))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this._context);

            builder = builder.setTitle(R.string.title_clear_configuration);
            builder = builder.setMessage(R.string.message_clear_configuration);

            final RobotPreferenceListener me = this;

            builder = builder.setPositiveButton(R.string.button_clear_yes, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(me._context);
                    SharedPreferences.Editor e = prefs.edit();

                    Map<String, ?> prefMap = prefs.getAll();

                    for (String key : prefMap.keySet())
                    {
                        e.remove(key);
                    }

                    e.commit();

                    Intent intent = new Intent(PersistentService.NUDGE_PROBES);
                    intent.setClass(me._context, PersistentService.class);

                    me._context.startService(intent);

                    TriggerManager.getInstance(me._context).removeAllTriggers();
                    TriggerManager.getInstance(me._context).refreshTriggers(me._context);
                    HttpUploadPlugin.clearFiles(me._context);

                    String where = "_id != -1";

                    me._context.getContentResolver().delete(RobotContentProvider.RECENT_PROBE_VALUES, where, null);
                    me._context.getContentResolver().delete(RobotContentProvider.SNAPSHOTS, where, null);
                    me._context.getContentResolver().delete(DistancesProvider.CONTENT_URI, where, null);

                    ProbeValuesProvider.getProvider(me._context).clear(me._context);

                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            });

            builder = builder.setNegativeButton(R.string.button_clear_no, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    // TODO Auto-generated method stub
                }
            });

            builder.create().show();
        }
        else if ("config_export_bootstrap".equals(preference.getKey()))
        {
            final RobotPreferenceListener me = this;

            Toast.makeText(this._context, R.string.toast_compiling_export, Toast.LENGTH_LONG).show();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Uri path = BootstrapSiteExporter.exportSite(me._context);

                    try
                    {
                        Intent intent = new Intent(Intent.ACTION_SEND);

                        intent.setType("message/rfc822");
                        intent.putExtra(Intent.EXTRA_SUBJECT, me._context.getString(R.string.email_export_bootstrap_subject));
                        intent.putExtra(Intent.EXTRA_TEXT, me._context.getString(R.string.message_export_bootstrap_subject));
                        intent.putExtra(Intent.EXTRA_STREAM, path);

                        me._context.startActivity(intent);
                    }
                    catch (ActivityNotFoundException e)
                    {
                        Toast.makeText(me._context, R.string.toast_mail_not_found, Toast.LENGTH_LONG).show();
                    }
                }
            };

            Thread t = new Thread(r);
            t.start();
        }
        else if ("config_export_jekyll".equals(preference.getKey()))
        {
            final RobotPreferenceListener me = this;

            Toast.makeText(this._context, R.string.toast_compiling_export, Toast.LENGTH_LONG).show();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Uri path = BootstrapSiteExporter.exportJekyllPages(me._context);

                    try
                    {
                        Intent intent = new Intent(Intent.ACTION_SEND);

                        intent.setType("message/rfc822");
                        intent.putExtra(Intent.EXTRA_SUBJECT, me._context.getString(R.string.email_export_jekyll_subject));
                        intent.putExtra(Intent.EXTRA_TEXT, me._context.getString(R.string.message_export_jekyll_subject));
                        intent.putExtra(Intent.EXTRA_STREAM, path);

                        me._context.startActivity(intent);
                    }
                    catch (ActivityNotFoundException e)
                    {
                        Toast.makeText(me._context, R.string.toast_mail_not_found, Toast.LENGTH_LONG).show();
                    }
                }
            };

            Thread t = new Thread(r);
            t.start();
        }
        else if (PersistentService.PROBE_NUDGE_INTERVAL.equals(preference.getKey()))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this._context);

            int selected = -1;

            final String[] values = this._context.getResources().getStringArray(R.array.probe_nudge_interval_values);

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
            String selectedInterval = prefs.getString(PersistentService.PROBE_NUDGE_INTERVAL, PersistentService.PROBE_NUDGE_INTERVAL_DEFAULT);

            for (int i = 0; i < values.length; i++)
            {
                if (values[i].equals(selectedInterval))
                    selected = i;
            }

            builder.setSingleChoiceItems(R.array.probe_nudge_interval_labels, selected, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    String newInterval = values[i];

                    SharedPreferences.Editor e = prefs.edit();
                    e.putString(PersistentService.PROBE_NUDGE_INTERVAL, newInterval);
                    e.commit();
                }
            });

            builder.create().show();
        }

        return false;
    }

    private JSONObject dumpJson(Preference preference) throws JSONException
    {
        JSONObject prefJson = new JSONObject();

        if (preference.getKey() != null)
            prefJson.put("key", preference.getKey());

        if (preference.getTitle() != null)
            prefJson.put("title", preference.getTitle());

        try {
            if (preference.getSummary() != null)
                prefJson.put("summary", preference.getSummary());
        }
        catch (ArrayIndexOutOfBoundsException e) // Lollipop bug?
        {
            LogManager.getInstance(preference.getContext()).logException(e);
        }

        if (preference instanceof PreferenceGroup)
        {
            if ("config_settings_trigger_category".equals(preference.getKey()))
                return null;
            else
            {
                PreferenceGroup group = (PreferenceGroup) preference;

                if (group.getPreferenceCount() == 0)
                    return null;

                prefJson.put("type", "group");

                JSONArray children = new JSONArray();

                for (int i = 0; i < group.getPreferenceCount(); i++)
                {
                    JSONObject child = this.dumpJson(group.getPreference(i));

                    if (child != null)
                        children.put(child);
                }

                prefJson.put("children", children);
            }
        }
        else if (preference instanceof CheckBoxPreference)
            prefJson.put("type", "boolean");
        else if (preference instanceof EditTextPreference)
            prefJson.put("type", "string");
        else if (preference instanceof ListPreference)
        {
            ListPreference list = (ListPreference) preference;
            prefJson.put("type", "list");

            JSONArray entries = new JSONArray();

            for (CharSequence cs : list.getEntries())
                entries.put(cs);

            prefJson.put("labels", entries);

            JSONArray values = new JSONArray();

            for (CharSequence cs : list.getEntryValues())
                values.put(cs);

            prefJson.put("values", values);
        }
        else
            prefJson = null;

        return prefJson;
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object value)
    {
        final RobotPreferenceListener me = this;

        if (SettingsKeys.CHECK_UPDATES_KEY.equals(pref.getKey()))
        {
            Toast.makeText(this._context, R.string.message_update_check, Toast.LENGTH_LONG).show();

            return true;
        }
        else if (SettingsKeys.CONFIG_URL.equals(pref.getKey()))
        {
            EncryptionManager.getInstance().setConfigurationReady(false);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(pref.getContext());
            SharedPreferences.Editor e = prefs.edit();
            e.remove(LegacyJSONConfigFile.JSON_LAST_UPDATE);
            e.remove(LegacyJSONConfigFile.JSON_LAST_HASH);

            e.commit();

            return true;
        }
        else if (SettingsKeys.RINGTONE_KEY.equals(pref.getKey()))
        {
            String name = ManagerService.soundNameForPath(this._context, value.toString());

            Intent playIntent = new Intent(ManagerService.RINGTONE_INTENT);

            if (name != null)
                playIntent.putExtra(SettingsKeys.RINGTONE_KEY, name);

            playIntent.setClass(this._context, ManagerService.class);

            this._context.startService(playIntent);

            return true;
        }
        else if (LogManager.ENABLED.equals(pref.getKey()))
        {
            LogManager.getInstance(this._context).setEnabled(((Boolean) value));

            return true;
        }
        else if (LogManager.URI.equals(pref.getKey()))
        {
            LogManager.getInstance(this._context).setEndpoint(value.toString());

            return true;
        }
        else if (LogManager.INCLUDE_LOCATION.equals(pref.getKey()))
        {
            LogManager.getInstance(this._context).setIncludeLocation(((Boolean) value));

            return true;
        }
        else if (LogManager.UPLOAD_INTERVAL.equals(pref.getKey()))
        {
            LogManager.getInstance(this._context).setUploadInterval(Long.parseLong(value.toString()));

            return true;
        }
        else if (LogManager.WIFI_ONLY.equals(pref.getKey()))
        {
            LogManager.getInstance(this._context).setWifiOnly(((Boolean) value));

            return true;
        }
        else if (LogManager.LIBERAL_SSL.equals(pref.getKey()))
        {
            LogManager.getInstance(this._context).setLiberalSsl(((Boolean) value));

            return true;
        }
        else if (LogManager.HEARTBEAT.equals(pref.getKey()))
        {
            LogManager.getInstance(this._context).setHeartbeat(((Boolean) value));

            return true;
        }
        else if (LocalHttpServer.BUILTIN_HTTP_SERVER_ENABLED.equals(pref.getKey()))
        {
            Boolean enable = (Boolean) value;

            if (enable)
            {
                Intent intent = new Intent(PersistentService.START_HTTP_SERVICE);
                intent.setClass(this._context, PersistentService.class);

                this._context.startService(intent);
            }
            else
            {
                Intent intent = new Intent(PersistentService.STOP_HTTP_SERVICE);
                intent.setClass(this._context, PersistentService.class);

                this._context.startService(intent);
            }

            return true;
        }
        else if (LocalHttpServer.BUILTIN_ZEROCONF_ENABLED.equals(pref.getKey()) ||  LocalHttpServer.BUILTIN_ZEROCONF_NAME.equals(pref.getKey()))
        {
            Runnable r = new Runnable()
            {
                @Override
                public void run() {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(me._context);

                    if (prefs.getBoolean(LocalHttpServer.BUILTIN_HTTP_SERVER_ENABLED, LocalHttpServer.BUILTIN_HTTP_SERVER_ENABLED_DEFAULT)) {
                        Intent stopIntent = new Intent(PersistentService.STOP_HTTP_SERVICE);
                        stopIntent.setClass(me._context, PersistentService.class);

                        me._context.startService(stopIntent);

                        try {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        Intent startIntent = new Intent(PersistentService.START_HTTP_SERVICE);
                        startIntent.setClass(me._context, PersistentService.class);

                        me._context.startService(startIntent);
                    }
                }
            };

            Thread t = new Thread(r);
            t.start();

            return true;
        }
        else if (PersistentService.PROBE_NUDGE_INTERVAL.equals(pref.getKey()))
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(me._context);

            PendingIntent pi = PendingIntent.getService(this._context, 0, new Intent(PersistentService.NUDGE_PROBES), PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarmManager = (AlarmManager) this._context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pi);

            long now = System.currentTimeMillis();
            long interval = Long.parseLong(prefs.getString(PersistentService.PROBE_NUDGE_INTERVAL, PersistentService.PROBE_NUDGE_INTERVAL_DEFAULT));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, now + interval, pi);
            else
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pi);

            return true;
        }

        return false;
    }
}
