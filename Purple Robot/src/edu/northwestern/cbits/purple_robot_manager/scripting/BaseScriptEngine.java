package edu.northwestern.cbits.purple_robot_manager.scripting;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.ScheduleManager;
import edu.northwestern.cbits.purple_robot_manager.activities.DialogActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.LabelActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.NfcActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.TestActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.WebActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.SettingsKeys;
import edu.northwestern.cbits.purple_robot_manager.annotation.ScriptingEngineMethod;
import edu.northwestern.cbits.purple_robot_manager.config.LegacyJSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.models.ModelManager;
import edu.northwestern.cbits.purple_robot_manager.plugins.DataUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPluginManager;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.snapshots.EmptySnapshotException;
import edu.northwestern.cbits.purple_robot_manager.snapshots.SnapshotManager;
import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;
import edu.northwestern.cbits.purple_robot_manager.util.ImageUtils;
import edu.northwestern.cbits.purple_robot_manager.widget.PurpleRobotAppWideWidgetProvider;
import edu.northwestern.cbits.purple_robot_manager.widget.PurpleRobotAppWidgetProvider;

public abstract class BaseScriptEngine
{
    public static String SCRIPT_ENGINE_PERSISTENCE_PREFIX = "purple_robot_script_persist_prefix_";
    protected static String SCRIPT_ENGINE_NAMESPACES = "purple_robot_script_namespaces";

    public static final int NOTIFICATION_ID = (int) System.currentTimeMillis();
    public static final String STICKY_NOTIFICATION_PARAMS = "STICKY_NOTIFICATION_PARAMS";

    private static String LOG_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    protected Context _context = null;
    private static Map<String, String> packageMap = null;

    private final Handler _handler = new Handler(Looper.getMainLooper());

    protected abstract String language();

    public BaseScriptEngine(Context context)
    {
        this._context = context;
    }

    public Date dateFromTimestamp(long epoch)
    {
        return new Date(epoch);
    }

    public String formatDate(Date date)
    {
        return ScheduleManager.formatString(date);
    }

    public Date parseDate(String dateString)
    {
        return ScheduleManager.parseString(dateString);
    }

    public Date now()
    {
        return ScheduleManager.clearMillis(new Date());
    }

    @SuppressLint("SimpleDateFormat")
    @ScriptingEngineMethod(language = "All", assetPath = "all_log.html", category = R.string.docs_script_category_diagnostic, arguments = { "message" })
    public void log(Object message)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(BaseScriptEngine.LOG_DATE_FORMAT);

        Log.e("PR." + this.language(), sdf.format(new Date()) + ": " + message.toString());

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        LogManager.getInstance(this._context).log("pr_script_log_message", payload);
    }

    public void testLog(Object message)
    {
        this.log(message);

        LocalBroadcastManager bcast = LocalBroadcastManager.getInstance(this._context);

        Intent intent = new Intent(TestActivity.INTENT_PROGRESS_MESSAGE);
        intent.putExtra(TestActivity.PROGRESS_MESSAGE, message.toString());

        bcast.sendBroadcastSync(intent);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_stop_playback.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { })
    public void stopPlayback()
    {
        Intent intent = new Intent(ManagerService.RINGTONE_STOP_INTENT);
        intent.setClass(this._context, ManagerService.class);

        HashMap<String, Object> payload = new HashMap<>();
        LogManager.getInstance(this._context).log("pr_tone_stopped", payload);

        this._context.startService(intent);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_play_default_tone.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "loops" })
    public void playDefaultTone(boolean loops)
    {
        LogManager.getInstance(this._context).log("pr_default_tone_played", null);

        this.playTone(null, loops);

    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_play_default_tone.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "loops" })
    public void playDefaultTone()
    {
        this.playDefaultTone(false);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_play_tone.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "tone", "loops" })
    public void playTone(String tone)
    {
        this.playTone(tone, false);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_play_tone.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "tone", "loops" })
    public void playTone(String tone, boolean loops)
    {
        Intent intent = new Intent(ManagerService.RINGTONE_INTENT);
        intent.setClass(this._context, ManagerService.class);

        if (tone != null)
            intent.putExtra(ManagerService.RINGTONE_NAME, tone);

        if (loops)
            intent.putExtra(ManagerService.RINGTONE_LOOPS, loops);

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("tone", tone);
        LogManager.getInstance(this._context).log("pr_tone_played", payload);

        this._context.startService(intent);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_persist_encrypted_string.html", category = R.string.docs_script_category_persistence, arguments = { "key", "value" })
    public boolean persistEncryptedString(String key, String value)
    {
        key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;

        return EncryptionManager.getInstance().persistEncryptedString(this._context, key, value);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_persist_encrypted_string.html", category = R.string.docs_script_category_persistence, arguments = { "key", "value" })
    public boolean persistEncryptedString(String namespace, String key, String value)
    {
        key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;
        key = namespace + " - " + key;

        return EncryptionManager.getInstance().persistEncryptedString(this._context, key, value);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_encrypted_string.html", category = R.string.docs_script_category_persistence, arguments = { "key" })
    public String fetchEncryptedString(String key)
    {
        key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;

        return EncryptionManager.getInstance().fetchEncryptedString(this._context, key);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_encrypted_string.html", category = R.string.docs_script_category_persistence, arguments = { "key" })
    public String fetchEncryptedString(String namespace, String key)
    {
        key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;
        key = namespace + " - " + key;

        return EncryptionManager.getInstance().fetchEncryptedString(this._context, key);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_vibrate.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "pattern", "repeats" })
    public void vibrate(String pattern)
    {
        this.vibrate(pattern, false);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_vibrate.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "pattern", "repeats" })
    public void vibrate(String pattern, boolean repeats)
    {
        Intent intent = new Intent(ManagerService.HAPTIC_PATTERN_INTENT);
        intent.putExtra(ManagerService.HAPTIC_PATTERN_NAME, pattern);
        intent.putExtra(ManagerService.HAPTIC_PATTERN_REPEATS, repeats);
        intent.setClass(this._context, ManagerService.class);

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("pattern", pattern);
        LogManager.getInstance(this._context).log("pr_vibrate_device", payload);

        this._context.startService(intent);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_stop_vibrate.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { })
    public void stopVibrate()
    {
        ManagerService.stopAllVibrations();
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_read_url.html", category = R.string.docs_script_category_data_collection, arguments = { "url", "lenient" })
    public String readUrl(String urlString)
    {
        return this.readUrl(urlString, false);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_read_url.html", category = R.string.docs_script_category_data_collection, arguments = { "url", "lenient" })
    public String readUrl(String urlString, boolean lenient)
    {
        try
        {
            if (lenient)
            {
                TrustManager[] trustAllCerts = new TrustManager[]
                { new X509TrustManager()
                {
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers()
                    {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType)
                    {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType)
                    {
                    }
                } };

                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                // Create all-trusting host name verifier
                HostnameVerifier allHostsValid = new HostnameVerifier()
                {
                    @Override
                    public boolean verify(String hostname, SSLSession session)
                    {
                        // TODO Auto-generated method stub
                        return true;
                    }
                };

                // Install the all-trusting host verifier
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            }

            URL url = new URL(urlString);

            URLConnection connection = url.openConnection();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            StringBuilder response = new StringBuilder();

            String inputLine;

            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);

            in.close();

            return response.toString();
        }
        catch (Exception e)
        {
            LogManager.getInstance(this._context).logException(e);
        }

        return null;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_emit_toast.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "message", "useLongDuration" })
    public boolean emitToast(final String message)
    {
        return this.emitToast(message, false);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_emit_toast.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "message", "useLongDuration" })
    public boolean emitToast(final String message, final boolean useLongDuration)
    {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("has_activity", (this._context instanceof Activity));
        payload.put("message", message);
        LogManager.getInstance(this._context).log("pr_toast_message", payload);

        final BaseScriptEngine me = this;

        this._handler.post(new Runnable() {
            @Override
            public void run() {
                if (useLongDuration)
                    Toast.makeText(me._context, message, Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(me._context, message, Toast.LENGTH_SHORT).show();
            }
        });

        return true;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_launch_url.html", category = R.string.docs_script_category_system_integration, arguments = { "url" })
    public boolean launchUrl(String urlString)
    {
        try
        {
            Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.putExtra(Browser.EXTRA_APPLICATION_ID, this._context.getPackageName());

            this._context.startActivity(launchIntent);

            HashMap<String, Object> payload = new HashMap<>();
            payload.put("url", urlString);
            LogManager.getInstance(this._context).log("pr_launch_url", payload);

            return true;
        }
        catch (Exception e)
        {
            LogManager.getInstance(this._context).logException(e);
        }

        return false;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_launch_internal_url.html", category = R.string.docs_script_category_system_integration, arguments = { "url" })
    public boolean launchInternalUrl(String urlString)
    {
        if (urlString.startsWith("file:///android_asset/") || urlString.startsWith("https://") || urlString.startsWith("http://"))
        {
            Intent launchIntent = new Intent(this._context, WebActivity.class);
            launchIntent.setData(Uri.parse(urlString));
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            this._context.startActivity(launchIntent);

            HashMap<String, Object> payload = new HashMap<>();
            payload.put("url", urlString);
            LogManager.getInstance(this._context).log("pr_launch_internal_url", payload);

            return true;
        }

        return false;
    }

    @SuppressLint("DefaultLocale")
    @ScriptingEngineMethod(language = "All", assetPath = "all_package_for_application_name.html", category = R.string.docs_script_category_system_integration, arguments = { "applicationName" })
    public String packageForApplicationName(String applicationName)
    {
        if (applicationName == null)
            return null;

        if (BaseScriptEngine.packageMap == null)
        {
            BaseScriptEngine.packageMap = new HashMap<>();

            String[] keyArray = this._context.getResources().getStringArray(R.array.app_package_keys);
            String[] valueArray = this._context.getResources().getStringArray(R.array.app_package_values);

            if (keyArray.length == valueArray.length)
            {
                for (int i = 0; i < keyArray.length; i++)
                {
                    BaseScriptEngine.packageMap.put(keyArray[i].toLowerCase(), valueArray[i]);
                }
            }
        }

        String packageName = BaseScriptEngine.packageMap.get(applicationName.toLowerCase());

        if (packageName == null)
            packageName = applicationName; // Allows us to launch by package
                                           // name as well.

        PackageManager pkgManager = this._context.getPackageManager();

        Intent launchIntent = pkgManager.getLaunchIntentForPackage(packageName);

        if (launchIntent == null) // No matching package found on system...

            packageName = null;

        if (packageName == null)
        {
            List<ApplicationInfo> apps = this._context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo app : apps)
            {
                String thisAppName = app.loadLabel(this._context.getPackageManager()).toString();

                if (applicationName.toLowerCase().equals(thisAppName.toLowerCase()))
                    return app.packageName;
            }
        }

        try
        {
            if (this._context.getPackageManager().getPackageInfo(applicationName, 0) != null)
                return applicationName;
        }
        catch (NameNotFoundException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_version.html", category = R.string.docs_script_category_diagnostic, arguments = { })
    public String version()
    {
        try
        {
            PackageInfo info = this._context.getPackageManager().getPackageInfo(this._context.getPackageName(), 0);

            return info.versionName;
        }
        catch (NameNotFoundException e)
        {
            LogManager.getInstance(this._context).logException(e);
        }

        return null;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_version_code.html", category = R.string.docs_script_category_diagnostic, arguments = { })
    public int versionCode()
    {
        try
        {
            PackageInfo info = this._context.getPackageManager().getPackageInfo(this._context.getPackageName(), 0);

            return info.versionCode;
        }
        catch (NameNotFoundException e)
        {
            LogManager.getInstance(this._context).logException(e);
        }

        return -1;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_persist_string.html", category = R.string.docs_script_category_persistence, arguments = { "key", "value" })
    public boolean persistString(String key, String value)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
        Editor editor = prefs.edit();

        key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;

        if (value != null)
            editor.putString(key, value.toString());
        else
            editor.remove(key);

        return editor.commit();
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_add_namespace.html", category = R.string.docs_script_category_persistence, arguments = { "namespace" })
    public void addNamespace(String namespace)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        try
        {
            JSONArray namespaces = new JSONArray(prefs.getString(BaseScriptEngine.SCRIPT_ENGINE_NAMESPACES, "[]"));

            for (int i = 0; i < namespaces.length(); i++)
            {
                String item = namespaces.getString(i);

                if (item.equals(namespace))
                    return;
            }

            namespaces.put(namespace);

            Editor e = prefs.edit();
            e.putString(BaseScriptEngine.SCRIPT_ENGINE_NAMESPACES, namespaces.toString());
            e.commit();
        }
        catch (JSONException e)
        {
            LogManager.getInstance(this._context).logException(e);
        }
    }

    public List<String> namespaceList()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        ArrayList<String> namespaceList = new ArrayList<>();

        try
        {
            JSONArray namespaces = new JSONArray(prefs.getString(BaseScriptEngine.SCRIPT_ENGINE_NAMESPACES, "[]"));

            for (int i = 0; i < namespaces.length(); i++)
            {
                namespaceList.add(namespaces.getString(i));
            }
        }
        catch (JSONException e)
        {
            LogManager.getInstance(this._context).logException(e);
        }

        return namespaceList;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_persist_string.html", category = R.string.docs_script_category_persistence, arguments = { "key", "value" })
    public boolean persistString(String namespace, String key, String value)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
        Editor editor = prefs.edit();

        this.addNamespace(namespace);

        key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;
        key = namespace + " - " + key;

        if (value != null)
            editor.putString(key, value.toString());
        else
            editor.remove(key);

        return editor.commit();
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_string.html", category = R.string.docs_script_category_persistence, arguments = { "key" })
    public String fetchString(String namespace, String key)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;
        key = namespace + " - " + key;

        return prefs.getString(key, null);
    }

    // Not documented in favor of fetchLabels...
    public void fetchLabel(String context, String key)
    {
        Intent labelIntent = new Intent(this._context, LabelActivity.class);
        labelIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (context == null || context.trim().length() < 1)
            context = this._context.getString(R.string.label_unknown_context);

        labelIntent.putExtra(LabelActivity.LABEL_CONTEXT, context);

        if (key != null && key.trim().length() > 1)
            labelIntent.putExtra(LabelActivity.LABEL_KEY, key);

        this._context.getApplicationContext().startActivity(labelIntent);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_string.html", category = R.string.docs_script_category_persistence, arguments = { "key" })
    public String fetchString(String key)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;

        return prefs.getString(key, null);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_setting.html", category = R.string.docs_script_category_configuration, arguments = { "key" })
    public String fetchSetting(String key)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        Object value = prefs.getAll().get(key);

        if (value != null)
            return value.toString();

        return null;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_reset_trigger.html", category = R.string.docs_script_category_triggers_automation, arguments = { "triggerId" })
    public void resetTrigger(String triggerId)
    {
        for (Trigger trigger : TriggerManager.getInstance(this._context).triggersForId(triggerId))
        {
            trigger.reset(this._context);
        }
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_enable_trigger.html", category = R.string.docs_script_category_triggers_automation, arguments = { "triggerId" })
    public void enableTrigger(String triggerId)
    {
        for (Trigger trigger : TriggerManager.getInstance(this._context).triggersForId(triggerId))
        {
            trigger.setEnabled(this._context, true);
        }
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fire_trigger.html", category = R.string.docs_script_category_triggers_automation, arguments = { "triggerId" })
    public void fireTrigger(String triggerId)
    {
        for (Trigger trigger : TriggerManager.getInstance(this._context).triggersForId(triggerId))
        {
            trigger.execute(this._context, true);
        }
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_disable_trigger.html", category = R.string.docs_script_category_triggers_automation, arguments = { "triggerId" })
    public void disableTrigger(String triggerId)
    {
        for (Trigger trigger : TriggerManager.getInstance(this._context).triggersForId(triggerId))
        {
            trigger.setEnabled(this._context, false);
        }
    }

    public void disableAutoConfigUpdates(String triggerId)
    {
        // TODO
    }

    public void enableAutoConfigUpdates(String triggerId)
    {
        // TODO
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_enable_probes.html", category = R.string.docs_script_category_data_collection, arguments = { })
    public void enableProbes()
    {
        ProbeManager.enableProbes(this._context);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_disable_probes.html", category = R.string.docs_script_category_data_collection, arguments = { })
    public void disableProbes()
    {
        ProbeManager.disableProbes(this._context);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_disable_each_probe.html", category = R.string.docs_script_category_data_collection, arguments = { })
    public void disableEachProbe()
    {
        ProbeManager.disableEachProbe(this._context);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_probes_enabled.html", category = R.string.docs_script_category_data_collection, arguments = { })
    public boolean probesEnabled()
    {
        return ProbeManager.probesState(this._context);
    }

    public boolean probesState()
    {
        // Left for script compatibility
        return this.probesEnabled();
    }

    protected void transmitData(Bundle data)
    {
        UUID uuid = UUID.randomUUID();
        data.putString("GUID", uuid.toString());

        LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this._context);
        Intent intent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
        intent.putExtras(data);

        localManager.sendBroadcast(intent);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_disable_probe.html", category = R.string.docs_script_category_data_collection, arguments = {"probeName" })
    public void disableProbe(String probeName)
    {
        ProbeManager.disableProbe(this._context, probeName);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_update_config_url.html", category = R.string.docs_script_category_configuration, arguments = { "newConfigUrl" })
    public void updateConfigUrl(String newConfigUrl)
    {
        if (newConfigUrl != null && newConfigUrl.trim().length() == 0)
            newConfigUrl = null;

        EncryptionManager.getInstance().setConfigurationReady(false);

        EncryptionManager.getInstance().setConfigUri(this._context, Uri.parse(newConfigUrl));

        LegacyJSONConfigFile.update(this._context, true);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_set_password.html", category = R.string.docs_script_category_configuration, arguments = { "password" })
    public void setPassword(String password)
    {
        if (password == null || password.trim().length() == 0)
            this.clearPassword();
        else
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

            Editor e = prefs.edit();
            e.putString("config_password", password);
            e.commit();
        }
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_clear_password.html", category = R.string.docs_script_category_configuration, arguments = { })
    public void clearPassword()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        Editor e = prefs.edit();
        e.remove("config_password");
        e.commit();
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_enable_background_image.html", category = R.string.docs_script_category_configuration, arguments = { })
    public void enableBackgroundImage()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        Editor e = prefs.edit();
        e.putBoolean("config_show_background", true);
        e.commit();
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_is_configuration_ready.html", category = R.string.docs_script_category_diagnostic, arguments = { })
    public boolean isConfigurationReady()
    {
        return EncryptionManager.getInstance().getConfigurationReady();
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_uploaders_enabled.html", category = R.string.docs_script_category_diagnostic, arguments = { })
    public boolean uploadersEnabled()
    {
        OutputPluginManager plugins = OutputPluginManager.getSharedInstance();

        for (OutputPlugin plugin : plugins.getPlugins())
        {
            if (plugin instanceof DataUploadPlugin)
            {
                DataUploadPlugin uploadPlugin = (DataUploadPlugin) plugin;

                if (uploadPlugin.isEnabled(this._context))
                    return true;
            }
        }

        return false;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_disable_background_image.html", category = R.string.docs_script_category_configuration, arguments = { })
    public void disableBackgroundImage()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        Editor e = prefs.edit();
        e.putBoolean("config_show_background", false);
        e.commit();
    }

    @ScriptingEngineMethod(language = "All")
    private void refreshConfigUrl()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
        Editor editor = prefs.edit();

        editor.putLong(LegacyJSONConfigFile.JSON_LAST_UPDATE, 0);
        editor.commit();

        LegacyJSONConfigFile.update(this._context, true);

        ProbeManager.nudgeProbes(this._context);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_set_user_id.html", category = R.string.docs_script_category_configuration, arguments = { "userId", "refresh" })
    public void setUserId(String userId)
    {
        this.setUserId(userId, true);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_set_user_id.html", category = R.string.docs_script_category_configuration, arguments = { "userId", "refresh" })
    public void setUserId(String userId, boolean refreshConfig)
    {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("source", "BaseScriptEngine");
        payload.put("old_id", EncryptionManager.getInstance().getUserId(this._context));
        payload.put("new_id", userId);
        payload.put("refresh_config", refreshConfig);

        LogManager.getInstance(this._context).log("set_user_id", payload);

        EncryptionManager.getInstance().setUserId(this._context, userId);

        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {

        }

        if (refreshConfig)
            this.refreshConfigUrl();
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_user_id.html", category = R.string.docs_script_category_configuration, arguments = { })
    public String fetchUserId()
    {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("source", "BaseScriptEngine");

        LogManager.getInstance(this._context).log("fetch_user_id", payload);

        return EncryptionManager.getInstance().getUserId(this._context);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_user_hash.html", category = R.string.docs_script_category_configuration, arguments = { })
    public String fetchUserHash()
    {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("source", "BaseScriptEngine");

        LogManager.getInstance(this._context).log("fetch_user_hash", payload);

        return EncryptionManager.getInstance().getUserHash(this._context);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_restore_default_id.html", category = R.string.docs_script_category_configuration, arguments = { })
    public void restoreDefaultId()
    {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("source", "BaseScriptEngine");

        LogManager.getInstance(this._context).log("restore_default_id", payload);

        EncryptionManager.getInstance().restoreDefaultId(this._context);

        this.refreshConfigUrl();
    }

    // Left for legacy compatibility purposes...
    public void enableUpdateChecks()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        Editor e = prefs.edit();
        e.putBoolean(SettingsKeys.CHECK_UPDATES_KEY, true);
        e.commit();
    }

    // Left for legacy compatibility purposes...
    public void disableUpdateChecks()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        Editor e = prefs.edit();
        e.putBoolean(SettingsKeys.CHECK_UPDATES_KEY, false);
        e.commit();
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_enable_probe.html", category = R.string.docs_script_category_data_collection, arguments = {"probeName" })
    public void enableProbe(String probeName)
    {
        ProbeManager.enableProbe(this._context, probeName);
    }

    protected boolean updateWidget(final String title, final String message, final String applicationName, final Map<String, Object> launchParams, final String script)
    {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this._context);

        ComponentName provider = new ComponentName(this._context.getPackageName(), PurpleRobotAppWidgetProvider.class.getName());

        int[] widgetIds = widgetManager.getAppWidgetIds(provider);

        ComponentName wideProvider = new ComponentName(this._context.getPackageName(), PurpleRobotAppWideWidgetProvider.class.getName());

        int[] wideWidgetIds = widgetManager.getAppWidgetIds(wideProvider);

        RemoteViews views = new RemoteViews(this._context.getPackageName(), R.layout.layout_widget);

        views.setCharSequence(R.id.widget_title_text, "setText", title);
        views.setCharSequence(R.id.widget_message_text, "setText", message);

        RemoteViews wideViews = new RemoteViews(this._context.getPackageName(), R.layout.layout_wide_widget);

        wideViews.setCharSequence(R.id.widget_wide_title_text, "setText", title);
        wideViews.setCharSequence(R.id.widget_wide_message_text, "setText", message);

        Intent intent = this.constructLaunchIntent(applicationName, launchParams, script);

        if (intent != null)
        {
            if (intent.getAction().equals(ManagerService.APPLICATION_LAUNCH_INTENT))
            {
                PendingIntent pi = PendingIntent.getService(this._context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                views.setOnClickPendingIntent(R.id.widget_root_layout, pi);
                wideViews.setOnClickPendingIntent(R.id.widget_root_layout, pi);
            }
            else
            {
                PendingIntent pi = PendingIntent.getActivity(this._context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                views.setOnClickPendingIntent(R.id.widget_root_layout, pi);
                wideViews.setOnClickPendingIntent(R.id.widget_root_layout, pi);
            }
        }

        widgetManager.updateAppWidget(widgetIds, views);
        widgetManager.updateAppWidget(wideWidgetIds, wideViews);

        return true;
    }

    @SuppressLint("DefaultLocale")
    protected Intent constructLaunchIntent(String applicationName, Map<String, Object> launchParams, String script)
    {
        if (applicationName == null)
            return null;

        String packageName = this.packageForApplicationName(applicationName);

        if (packageName != null)
        {
            Intent intent = new Intent(ManagerService.APPLICATION_LAUNCH_INTENT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            intent.putExtra(ManagerService.APPLICATION_LAUNCH_INTENT_PACKAGE, packageName);

            if (script != null)
                intent.putExtra(ManagerService.APPLICATION_LAUNCH_INTENT_POSTSCRIPT, script);

            if (launchParams != null)
            {
                HashMap<String, String> launchMap = new HashMap<>();

                for (Entry<String, Object> e : launchParams.entrySet())
                {
                    launchMap.put(e.getKey(), e.getValue().toString());
                }

                JSONObject jsonMap = new JSONObject(launchMap);

                intent.putExtra(ManagerService.APPLICATION_LAUNCH_INTENT_PARAMETERS, jsonMap.toString());
            }

            return intent;
        }

        if (applicationName.toLowerCase().startsWith("http://") || applicationName.toLowerCase().startsWith("https://"))
        {
            Intent intent = new Intent(ManagerService.APPLICATION_LAUNCH_INTENT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            intent.putExtra(ManagerService.APPLICATION_LAUNCH_INTENT_URL, applicationName);

            if (script != null)
                intent.putExtra(ManagerService.APPLICATION_LAUNCH_INTENT_POSTSCRIPT, script);

            if (launchParams != null)
            {
                HashMap<String, String> launchMap = new HashMap<>();

                for (Entry<String, Object> e : launchParams.entrySet())
                {
                    launchMap.put(e.getKey().toString(), e.getValue().toString());
                }

                JSONObject jsonMap = new JSONObject(launchMap);

                intent.putExtra(ManagerService.APPLICATION_LAUNCH_INTENT_PARAMETERS, jsonMap.toString());
            }

            return intent;
        }

        return null;
    }

    @SuppressLint("DefaultLocale")
    protected Intent constructDirectLaunchIntent(final String applicationName, Map<String, Object> launchParams)
    {
        if (applicationName.toLowerCase().startsWith("http://") || applicationName.toLowerCase().startsWith("https://"))
            return new Intent(Intent.ACTION_VIEW, Uri.parse(applicationName));
        else
        {
            String packageName = this.packageForApplicationName(applicationName);

            if (packageName != null)
            {
                Intent intent = this._context.getPackageManager().getLaunchIntentForPackage(packageName);

                if (launchParams != null)
                {
                    for (Entry<String, Object> e : launchParams.entrySet())
                    {
                        intent.putExtra(e.getKey().toString(), e.getValue().toString());
                    }
                }

                return intent;
            }
        }

        return null;
    }

    protected boolean updateTrigger(String triggerId, Map<String, Object> params)
    {
        boolean found = false;

        params.put("identifier", triggerId);

        for (Trigger trigger : TriggerManager.getInstance(this._context).triggersForId(triggerId))
        {
            trigger.updateFromMap(this._context, params);

            found = true;
        }

        if (!found)
        {
            Trigger t = Trigger.parse(this._context, params);

            TriggerManager.getInstance(this._context).addTrigger(this._context, t);

            found = true;
        }

        return found;
    }

    protected boolean updateProbeConfig(Map<String, Object> params)
    {
        if (params.containsKey("name"))
        {
            String probeName = params.get("name").toString();

            return ProbeManager.updateProbe(this._context, probeName, params);
        }

        return false;
    }

    protected Map<String, Object> probeConfigMap(String name)
    {
        return ProbeManager.probeConfiguration(this._context, name);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_launch_application.html", category = R.string.docs_script_category_system_integration, arguments = { "applicationName", "options", "script" })
    public boolean launchApplication(String applicationName)
    {
        return this.launchApplication(applicationName, new HashMap<String, Object>(), null);
    }

    protected boolean launchApplication(String applicationName, Map<String, Object> launchParams, final String script)
    {
        Intent intent = this.constructLaunchIntent(applicationName, launchParams, script);

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("application_present", (intent != null));
        payload.put("application_name", applicationName);
        LogManager.getInstance(this._context).log("pr_application_launch", payload);

        if (intent != null)
        {
            intent.setClass(this._context, ManagerService.class);

            this._context.startService(intent);

            return true;
        }

        return false;
    }

    protected boolean showApplicationLaunchNotification(String title, String message, String applicationName, Map<String, Object> launchParams, final String script)
    {
        return this.showApplicationLaunchNotification(title, message, applicationName, false, launchParams, script);
    }

    protected boolean showApplicationLaunchNotification(String title, String message, String applicationName, boolean persistent, Map<String, Object> launchParams, final String script)
    {
        try
        {
            long now = System.currentTimeMillis();

            Intent intent = this.constructDirectLaunchIntent(applicationName, launchParams);

            HashMap<String, Object> payload = new HashMap<>();
            payload.put("application_present", (intent != null));
            payload.put("application_name", applicationName);
            LogManager.getInstance(this._context).log("pr_application_launch_notification", payload);

            if (intent != null)
            {
                PendingIntent pendingIntent = PendingIntent.getActivity(this._context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                if (script != null)
                {
                    Intent serviceIntent = this.constructLaunchIntent(applicationName, launchParams, script);
                    pendingIntent = PendingIntent.getService(this._context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                }

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this._context);
                builder.setContentIntent(pendingIntent);
                builder.setAutoCancel(true);
                builder.setContentTitle(title);
                builder.setContentText(message);
                builder.setTicker(message);
                builder.setSmallIcon(R.drawable.ic_note_icon);

                try
                {
                    Notification note = builder.build();

                    if (persistent)
                        note.flags = note.flags | Notification.FLAG_NO_CLEAR;

                    NotificationManager noteManager = (NotificationManager) this._context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
                    noteManager.notify(BaseScriptEngine.NOTIFICATION_ID, note);
                }
                catch (UnsupportedOperationException e)
                {
                    // Added so that the mock test cases could still execute.
                }

                return true;
            }

            return false;
        }
        catch (Exception e)
        {
            LogManager.getInstance(this._context).logException(e);
        }

        return false;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_show_script_note.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "title", "message", "persistent", "script", "sticky", "imageUrl" })
    public boolean showScriptNotification(String title, String message, boolean persistent, Object... args)
    {
        boolean sticky = false;
        String script = null;
        String iconUrl = null;

        for (Object item : args)
        {
            if (item instanceof Boolean)
                sticky = (Boolean) item;
            else if (item instanceof String)
            {
                String value = (String) item;

                String lowerValue = value.toLowerCase();

                if (lowerValue.startsWith("https://") || lowerValue.startsWith("http://") || lowerValue.startsWith("file://")) {
                    iconUrl = value;
                }
                else
                {
                    // Two scripts provided - bail...

                    if (script != null)
                    {
                        HashMap<String, Object> payload = new HashMap<>();
                        payload.put("message", "Tried to define two script parameters in showScriptNotification.");

                        LogManager.getInstance(this._context).log("script_error", payload);

                        return false;
                    }

                    script = value;
                }
            }
        }

        return this.showScriptNotification(title, message, persistent, sticky, script, iconUrl);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_show_script_note.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "title", "message", "persistent", "script", "sticky", "imageUrl" })
    public boolean showScriptNotification(final String title, final String message, final boolean persistent, final boolean sticky, final String script, final String iconUrl)
    {
        try
        {
            HashMap<String, Object> payload = new HashMap<>();
            LogManager.getInstance(this._context).log("pr_script_run_notification", payload);

            Intent serviceIntent = this.constructScriptIntent(script);

            PendingIntent pendingIntent = PendingIntent.getService(this._context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this._context);
            builder = builder.setContentIntent(pendingIntent);

            if (!sticky)
                builder = builder.setAutoCancel(true);

            builder = builder.setContentTitle(title);
            builder = builder.setContentText(message);
            builder = builder.setTicker(message);
            builder = builder.setSmallIcon(R.drawable.ic_note_icon);

            if (iconUrl != null)
            {
                final int side = (int) (40 * this._context.getResources().getDisplayMetrics().density);

                Uri resized = ImageUtils.fetchResizedImageSync(this._context, Uri.parse(iconUrl), side, side, true);

                if (resized == null)
                    return false;

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this._context.getContentResolver(), resized);

                float multiplier = 1.0f;

                while (bitmap.getWidth() * multiplier < side && bitmap.getHeight() * multiplier < side)
                {
                    multiplier += 1;
                }

                Matrix matrix = new Matrix();
                matrix.postScale(multiplier, multiplier);

                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                builder = builder.setLargeIcon(bitmap);
            }

            try
            {
                Notification note = builder.build();

                if (persistent)
                    note.flags = note.flags | Notification.FLAG_NO_CLEAR;

                if (sticky)
                {
                    note.flags = note.flags | Notification.FLAG_ONGOING_EVENT;

                    JSONObject json = new JSONObject();

                    json.put("title", title);
                    json.put("message", message);
                    json.put("script", script);
                    json.put("persistent", persistent);
                    json.put("sticky", sticky);

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

                    Editor e = prefs.edit();
                    e.putString(BaseScriptEngine.STICKY_NOTIFICATION_PARAMS, json.toString());
                    e.commit();
                }

                NotificationManager noteManager = (NotificationManager) this._context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
                noteManager.notify(BaseScriptEngine.NOTIFICATION_ID, note);
            }
            catch (UnsupportedOperationException e)
            {
                // Added so that the mock test cases could still execute.
            }

            return true;
        }
        catch (Exception e)
        {
            LogManager.getInstance(this._context).logException(e);
        }

        return false;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_cancel_script_note.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { })
    public void cancelScriptNotification()
    {
        NotificationManager noteManager = (NotificationManager) this._context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        noteManager.cancel(BaseScriptEngine.NOTIFICATION_ID);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        Editor e = prefs.edit();
        e.remove(BaseScriptEngine.STICKY_NOTIFICATION_PARAMS);
        e.commit();
    }

    private Intent constructScriptIntent(String script)
    {
        Intent intent = new Intent(ManagerService.RUN_SCRIPT_INTENT);
        intent.putExtra(ManagerService.RUN_SCRIPT, script);

        return intent;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_show_native_dialog.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "title", "message", "confirmLabel", "cancelLabel", "confirmScript", "cancelScript", "tag", "priority" })
    public void showNativeDialog(final String title, final String message, final String confirmTitle, final String cancelTitle, final String confirmScript, final String cancelScript)
    {
        DialogActivity.showNativeDialog(this._context, title, message, confirmTitle, cancelTitle, confirmScript, cancelScript, null, 0);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_show_native_dialog.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "title", "message", "confirmLabel", "cancelLabel", "confirmScript", "cancelScript", "tag", "priority" })
    public void showNativeDialog(final String title, final String message, final String confirmLabel, final String cancelLabel, final String confirmScript, final String cancelScript, String tag, int priority)
    {
        DialogActivity.showNativeDialog(this._context, title, message, confirmLabel, cancelLabel, confirmScript, cancelScript, tag, priority);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_clear_native_dialogs.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "tag" })
    public void clearNativeDialogs()
    {
        DialogActivity.clearNativeDialogs(this._context);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_clear_native_dialogs.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "tag" })
    public void clearNativeDialogs(String tag)
    {
        DialogActivity.clearNativeDialogs(this._context, tag, null);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_show_app_launch_note.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "title", "message", "packageName" })
    public boolean showApplicationLaunchNotification(String title, String message, String applicationName, long displayWhen)
    {
        return this.showApplicationLaunchNotification(title, message, applicationName, new HashMap<String, Object>(), null);
    }

    @ScriptingEngineMethod(language = "All")
    public boolean updateWidget(final String title, final String message, final String applicationName)
    {
        return this.updateWidget(title, message, applicationName, new HashMap<String, Object>(), null);
    }

    protected void updateWidget(Map<String, Object> parameters)
    {
        Intent intent = new Intent(ManagerService.UPDATE_WIDGETS);
        intent.setClass(this._context, ManagerService.class);

        for (Object keyObj : parameters.keySet())
        {
            String key = keyObj.toString();

            intent.putExtra(key, parameters.get(key).toString());
        }

        this._context.startService(intent);

        if (parameters.containsKey("identifier"))
        {
            String identifier = parameters.get("identifier").toString();

            JSONObject params = new JSONObject();

            for (String key : parameters.keySet())
            {
                try
                {
                    params.put(key, parameters.get(key).toString());
                }
                catch (JSONException e)
                {
                    LogManager.getInstance(this._context).logException(e);
                }
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

            Editor e = prefs.edit();
            e.putString("WIDGET_UPDATE_" + identifier, params.toString());
            e.commit();
        }
    }

    protected Map<String, Object> fetchWidgetMap(String identifier)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        String key = "WIDGET_UPDATE_" + identifier;

        Map<String, Object> params = new HashMap<>();

        if (prefs.contains(key))
        {
            try
            {
                JSONObject json = new JSONObject(prefs.getString(key, "{}"));

                JSONArray names = json.names();

                for (int i = 0; i < names.length(); i++)
                {
                    String name = names.getString(i);

                    params.put(name, json.get(name));
                }
            }
            catch (JSONException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }
        }

        return params;
    }

    protected List<String> widgetsList()
    {
        String prefix = "WIDGET_UPDATE_";

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        ArrayList<String> widgets = new ArrayList<>();

        for (String key : prefs.getAll().keySet())
        {
            if (key.startsWith(prefix))
                widgets.add(key.substring(prefix.length()));
        }

        return widgets;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_schedule_script.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "identifier", "dateString", "script" })
    public void scheduleScript(String identifier, String dateString, String script)
    {
        ScheduleManager.updateScript(this._context, identifier, dateString, script);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_cancel_scheduled_script.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "identifier" })
    public void cancelScheduledScript(String identifier)
    {
        ScheduleManager.updateScript(this._context, identifier, null, null);
    }

    protected boolean broadcastIntent(final String action, final Map<String, Object> extras)
    {
        Intent intent = new Intent(action);

        if (extras != null)
        {
            for (Entry<String, Object> e : extras.entrySet())
            {
                intent.putExtra(e.getKey(), e.getValue().toString());
            }
        }

        this._context.sendBroadcast(intent);

        return true;
    }

    public static Object runScript(Context context, String script)
    {
        return BaseScriptEngine.runScript(context, script, null);
    }

    public static Object runScript(Context context, String script, Map<String, Object> objects)
    {
        context = context.getApplicationContext();

        try
        {
            if (SchemeEngine.canRun(script))
            {
                SchemeEngine engine = new SchemeEngine(context, objects);

                return engine.evaluateSource(script);
            }
            else if (JavaScriptEngine.canRun(script))
            {
                JavaScriptEngine engine = new JavaScriptEngine(context);

                return engine.runScript(script, "extras", objects);
            }
        }
        catch (RuntimeException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return null;
    }

    protected boolean updateConfig(Map<String, Object> config)
    {
        return PurpleRobotApplication.updateFromMap(this._context, config);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_update_config.html", category = R.string.docs_script_category_configuration, arguments = { "key", "value" })
    public boolean updateConfig(String key, Object value)
    {
        Map<String, Object> values = new HashMap<>();

        values.put(key, value);

        return PurpleRobotApplication.updateFromMap(this._context, values);
    }

    public Object valueFromString(String key, String string)
    {
        try
        {
            JSONObject json = new JSONObject(string);

            if (json.has(key))
            {
                Object value = json.get(key);

                if (value instanceof JSONObject)
                    value = this.jsonToMap((JSONObject) value);
                else if (value instanceof JSONArray)
                    value = this.jsonToList((JSONArray) value);

                return value;
            }
        }
        catch (JSONException e)
        {
            LogManager.getInstance(this._context).logException(e);
        }

        return null;
    }

    private Map<String, Object> jsonToMap(JSONObject object)
    {
        Map<String, Object> map = new HashMap<>();

        Iterator<String> keys = object.keys();

        while (keys.hasNext())
        {
            String key = keys.next();

            try
            {
                Object value = object.get(key);

                if (value instanceof JSONObject)
                    value = this.jsonToMap((JSONObject) value);
                else if (value instanceof JSONArray)
                    value = this.jsonToList((JSONArray) value);

                map.put(key, value);
            }
            catch (JSONException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }
        }

        return map;
    }

    private List<Object> jsonToList(JSONArray array)
    {
        List<Object> list = new ArrayList<>();

        for (int i = 0; i < array.length(); i++)
        {
            try
            {
                Object value = array.get(i);

                if (value instanceof JSONObject)
                    value = this.jsonToMap((JSONObject) value);
                else if (value instanceof JSONArray)
                    value = this.jsonToList((JSONArray) value);

                list.add(value);
            }
            catch (JSONException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }
        }

        return list;
    }

    public List<String> fetchNamespaceList()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        ArrayList<String> list = new ArrayList<>();
        list.add("");

        try
        {
            JSONArray namespaces = new JSONArray(prefs.getString(BaseScriptEngine.SCRIPT_ENGINE_NAMESPACES, "[]"));

            for (int i = 0; i < namespaces.length(); i++)
            {
                list.add(namespaces.getString(i));
            }
        }
        catch (JSONException e)
        {
            LogManager.getInstance(this._context).logException(e);
        }

        return list;
    }

    public Map<String, Object> fetchNamespaceMap(String namespace)
    {
        Map<String, Object> map = new HashMap<>();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        Map<String, ?> all = prefs.getAll();

        String prefix = namespace + " - " + BaseScriptEngine.SCRIPT_ENGINE_PERSISTENCE_PREFIX;

        if (namespace.length() == 0)
            prefix = BaseScriptEngine.SCRIPT_ENGINE_PERSISTENCE_PREFIX;

        for (String key : all.keySet())
        {
            if (key.indexOf(prefix) == 0)
                map.put(key.substring(prefix.length()), all.get(key));
        }

        return map;
    }

    public List<String> fetchTriggerIdList()
    {
        return TriggerManager.getInstance(this._context).triggerIds();
    }

    public List<String> fetchSnapshotIdList()
    {
        ArrayList<String> times = new ArrayList<>();

        for (long time : SnapshotManager.getInstance(this._context).snapshotTimes())
            times.add("" + time);

        return times;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_take_snapshot.html", category = R.string.docs_script_category_data_collection, arguments = {"source" })
    public String takeSnapshot(String source)
    {
        try
        {
            return "" + SnapshotManager.getInstance(this._context).takeSnapshot(this._context, source, null);
        }
        catch (EmptySnapshotException e)
        {

        }

        return null;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_delete_snapshot.html", category = R.string.docs_script_category_data_collection, arguments = { "snapshotId" })
    public void deleteSnapshot(String snapshotId)
    {
        SnapshotManager.getInstance(this._context).deleteSnapshot(Long.parseLong(snapshotId));
    }

    public Map<String, Object> fetchSnapshotMap(String timestamp)
    {
        JSONObject json = SnapshotManager.getInstance(this._context).jsonForTime(Long.parseLong(timestamp), true);

        return this.jsonToMap(json);
    }

    public Map<String, Object> fetchTriggerMap(String id)
    {
        return TriggerManager.getInstance(this._context).fetchTrigger(this._context, id);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_delete_trigger.html", category = R.string.docs_script_category_triggers_automation, arguments = { "triggerId" })
    public boolean deleteTrigger(String id)
    {
        return TriggerManager.getInstance(this._context).deleteTrigger(id);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_clear_triggers.html", category = R.string.docs_script_category_triggers_automation, arguments = { })
    public void clearTriggers()
    {
        LogManager.getInstance(this._context).log("script_clear_triggers", null);

        for (String id : this.fetchTriggerIdList())
        {
            this.deleteTrigger(id);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void fetchLabelsInterface(String appContext, String instructions, Map<String, Object> labels)
    {
        Intent labelIntent = new Intent();
        labelIntent.setClass(this._context, LabelActivity.class);
        labelIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        labelIntent.putExtra(LabelActivity.LABEL_CONTEXT, appContext);

        if (instructions != null)
            labelIntent.putExtra(LabelActivity.INSTRUCTIONS, instructions);

        labelIntent.putExtra(LabelActivity.TIMESTAMP, ((double) System.currentTimeMillis()));

        Bundle labelsBundle = new Bundle();

        for (String key : labels.keySet())
        {
            Map<String, Object> labelMap = (Map<String, Object>) labels.get(key);

            Bundle labelBundle = new Bundle();

            for (String labelKey : labelMap.keySet())
            {
                Object o = labelMap.get(labelKey);

                if (o instanceof String)
                    labelBundle.putString(labelKey, o.toString());
                else if (o instanceof Double)
                    labelBundle.putDouble(labelKey, (Double) o);
                else if (o instanceof ArrayList)
                {
                    ArrayList<String> listItems = new ArrayList<>();

                    for (Object item : ((ArrayList) o))
                    {
                        listItems.add(item.toString());
                    }

                    labelBundle.putStringArrayList(labelKey, listItems);
                }
                else
                    Log.e("PR", "UNKNOWN OBJECT: " + o.getClass().getCanonicalName() + " -- " + labelKey);
            }

            labelsBundle.putParcelable(key, labelBundle);
        }

        labelIntent.putExtra(LabelActivity.LABEL_DEFINITIONS, labelsBundle);

        this._context.startActivity(labelIntent);
    }

    // TODO: Document API call
    public void addModel(String jsonUrl)
    {
        ModelManager.getInstance(this._context).addModel(jsonUrl);
    }

    // TODO: Document API call
    public void deleteModel(String jsonUrl)
    {
        ModelManager.getInstance(this._context).deleteModel(jsonUrl);
    }

    // TODO: Document API call
    public void enableModel(String jsonUrl)
    {
        ModelManager.getInstance(this._context).enableModel(jsonUrl);
    }

    // TODO: Document API call
    public void disableModel(String jsonUrl)
    {
        ModelManager.getInstance(this._context).disableModel(jsonUrl);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_set_upload_url.html", category = R.string.docs_script_category_configuration, arguments = { "url" })
    public void setUploadUrl(String uploadUrl)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        Editor e = prefs.edit();

        if (uploadUrl != null)
        {
            if (Uri.parse(uploadUrl) != null)
                e.putString(DataUploadPlugin.UPLOAD_URI, uploadUrl);
        }
        else
            e.remove(DataUploadPlugin.UPLOAD_URI);

        e.commit();
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_get_upload_url.html", category = R.string.docs_script_category_configuration, arguments = { })
    public String getUploadUrl()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

        return prefs.getString(DataUploadPlugin.UPLOAD_URI, null);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_scan_nfc.html", category = R.string.docs_script_category_data_collection, arguments = { })
    public boolean scanNFC()
    {
        return NfcActivity.startScan(this._context);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_can_scan_nfc.html", category = R.string.docs_script_category_data_collection, arguments = { })
    public boolean canScanNFC()
    {
        return NfcActivity.canScan(this._context);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_cancel_nfc_scan.html", category = R.string.docs_script_category_data_collection, arguments = { })
    public void cancelNFCScan()
    {
        NfcActivity.cancelScan();
    }
}
