package edu.northwestern.cbits.purple_robot_manager;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import edu.northwestern.cbits.purple_robot_manager.activities.settings.SettingsKeys;
import edu.northwestern.cbits.purple_robot_manager.config.LegacyJSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ActivityDetectionProbe;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class ManagerService extends IntentService
{
    public static String RUN_SCRIPT_INTENT = "purple_robot_manager_run_script";
    public static String RUN_SCRIPT = "run_script";

    public static String APPLICATION_LAUNCH_INTENT = "purple_robot_manager_application_launch";
    public static String APPLICATION_LAUNCH_INTENT_PACKAGE = "purple_robot_manager_widget_launch_package";
    public static String APPLICATION_LAUNCH_INTENT_URL = "purple_robot_manager_widget_launch_url";
    public static String APPLICATION_LAUNCH_INTENT_PARAMETERS = "purple_robot_manager_widget_launch_parameters";
    public static String APPLICATION_LAUNCH_INTENT_POSTSCRIPT = "purple_robot_manager_widget_launch_postscript";

    public static String UPDATE_WIDGETS = "purple_robot_manager_update_widgets";
    public static String FIRE_TRIGGERS_INTENT = "purple_robot_manager_fire_triggers";
    public static String INCOMING_DATA_INTENT = "purple_robot_manager_incoming_data";
    public static String UPLOAD_LOGS_INTENT = "purple_robot_manager_upload_logs";
    public static String REFRESH_ERROR_STATE_INTENT = "purple_robot_manager_refresh_errors";

    public static String GOOGLE_PLAY_ACTIVITY_DETECTED = "purple_robot_manager_google_play_activity_detected";

    public static String HAPTIC_PATTERN_INTENT = "purple_robot_manager_haptic_pattern";
    public static String HAPTIC_PATTERN_NAME = "purple_robot_manager_haptic_pattern_name";
    public static String HAPTIC_PATTERN_REPEATS = "purple_robot_manager_haptic_pattern_repeats";

    public static String RINGTONE_STOP_INTENT = "purple_robot_manager_stop_ringtone";
    public static String RINGTONE_INTENT = "purple_robot_manager_ringtone";
    public static String RINGTONE_NAME = "purple_robot_manager_ringtone_name";
    public static String RINGTONE_LOOPS = "purple_robot_manager_ringtone_loops";

    public static String REFRESH_CONFIGURATION = "purple_robot_manager_refresh_configuration";

    public static final String UPDATE_TRIGGER_SCHEDULE_INTENT = "purple_robot_manager_update_trigger_schedule";

    public static long startTimestamp = System.currentTimeMillis();

    private static boolean _checkSetup = false;
    private static long _lastTriggerNudge = 0;
    private static boolean _updatingTriggerSchedule = false;
    protected static boolean _needsTriggerUpdate = false;

    private static boolean _looping = false;
    protected static MediaPlayer _player = null;
    private static boolean _vibrationRepeats = false;

    public ManagerService()
    {
        super("ManagerService");
    }

    public ManagerService(String name)
    {
        super(name);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onHandleIntent(final Intent intent)
    {
        String action = intent.getAction();

        final ManagerService me = this;

        if (GOOGLE_PLAY_ACTIVITY_DETECTED.equalsIgnoreCase(action))
            ActivityDetectionProbe.activityDetected(this, intent);
        else if (REFRESH_ERROR_STATE_INTENT.equalsIgnoreCase(action))
        {
            Runnable r = new Runnable()
            {
                @Override
                public void run()
                {
                    SanityManager.getInstance(me).refreshState();
                }
            };

            Thread t = new Thread(r);
            t.start();
        }
        else if (UPDATE_WIDGETS.equalsIgnoreCase(action))
        {
            Intent broadcast = new Intent("edu.northwestern.cbits.purple.UPDATE_WIDGET");
            broadcast.putExtras(intent.getExtras());

            this.startService(broadcast);
        }
        else if (HAPTIC_PATTERN_INTENT.equalsIgnoreCase(action))
        {
            String pattern = intent.getStringExtra(HAPTIC_PATTERN_NAME);
            ManagerService._vibrationRepeats = intent.getBooleanExtra(ManagerService.HAPTIC_PATTERN_REPEATS, false);

            if (!pattern.startsWith("vibrator_"))
                pattern = "vibrator_" + pattern;

            int[] patternSpec = this.getResources().getIntArray(R.array.vibrator_buzz);

            if ("vibrator_blip".equalsIgnoreCase(pattern))
                patternSpec = this.getResources().getIntArray(R.array.vibrator_blip);
            if ("vibrator_sos".equalsIgnoreCase(pattern))
                patternSpec = this.getResources().getIntArray(R.array.vibrator_sos);

            final long[] longSpec = new long[patternSpec.length];

            for (int i = 0; i < patternSpec.length; i++)
            {
                longSpec[i] = (long) patternSpec[i];
            }

            final Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

            Runnable r = new Runnable()
            {
                public void run()
                {
                    do
                    {
                        long duration = 0;

                        for (long length : longSpec)
                            duration += length;

                        v.cancel();
                        v.vibrate(longSpec, -1);

                        try
                        {
                            Thread.sleep(duration + 250);
                        }
                        catch (InterruptedException e)
                        {

                        }
                    }
                    while(ManagerService._vibrationRepeats);
                }
            };

            Thread t = new Thread(r);
            t.start();
        }
        else if (RINGTONE_STOP_INTENT.equalsIgnoreCase(action))
        {
            ManagerService._looping = false;

            if (ManagerService._player != null)
                ManagerService._player.stop();

            ManagerService._player = null;
        }
        else if (RINGTONE_INTENT.equalsIgnoreCase(action))
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            Uri toneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            String toneString = prefs.getString(SettingsKeys.RINGTONE_KEY, null);

            String name = null;

            if (intent.hasExtra(ManagerService.RINGTONE_NAME)) {
                name = intent.getStringExtra(ManagerService.RINGTONE_NAME);
                toneString = null;
                toneUri = null;
            }

            try
            {
                if (toneString != null)
                {
                    if (toneString.equals("0"))
                        toneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION);
                    else if (toneString.startsWith("sounds/") == false)
                        toneUri = Uri.parse(toneString);
                    else
                        toneUri = null;
                }
                else
                {
                    if (name != null)
                    {
                        RingtoneManager rm = new RingtoneManager(this);
                        rm.setType(RingtoneManager.TYPE_NOTIFICATION);

                        Cursor cursor = rm.getCursor();

                        do
                        {
                            String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);

                            if (name.equalsIgnoreCase(title))
                                toneUri = rm.getRingtoneUri(cursor.getPosition());
                        }
                        while (cursor.moveToNext());

                        cursor.deactivate();
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                LogManager.getInstance(this).logException(e);
            }

            if (toneUri != null)
            {
                final Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), toneUri);

                ManagerService._looping = intent.getBooleanExtra(ManagerService.RINGTONE_LOOPS, false);

                if (r != null)
                {
                    Thread t = new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            r.play();

                            try
                            {
                                while (r.isPlaying())
                                    Thread.sleep(100);

                                if (ManagerService._looping)
                                {
                                    Thread t = new Thread(this);

                                    t.start();
                                }
                            }
                            catch (InterruptedException e)
                            {
                                LogManager.getInstance(me).logException(e);
                            }
                        }
                    });

                    t.start();
                }
            }
            else
            {
                try
                {
                    String path = ManagerService.pathForSound(this, name);

                    final AssetFileDescriptor afd = this.getAssets().openFd(path);

                    Runnable r = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            ManagerService._player = new MediaPlayer();

                            try
                            {
                                ManagerService._player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                                ManagerService._player.setLooping(intent.getBooleanExtra(ManagerService.RINGTONE_LOOPS, false));

                                ManagerService._player.prepare();

                                ManagerService._player.setOnCompletionListener(new OnCompletionListener()
                                {
                                    @Override
                                    public void onCompletion(MediaPlayer player)
                                    {
                                        ManagerService._player = null;
                                    }
                                });

                                ManagerService._player.start();
                            }
                            catch (IllegalArgumentException | IOException | IllegalStateException e)
                            {
                                LogManager.getInstance(me).logException(e);
                            }
                        }
                    };

                    Thread t = new Thread(r);
                    t.start();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    LogManager.getInstance(this).logException(e);

                    Intent defaultIntent = new Intent(ManagerService.RINGTONE_INTENT);
                    this.startService(defaultIntent);
                }
            }
        }
        else if (APPLICATION_LAUNCH_INTENT.equalsIgnoreCase(action))
        {
            if (intent.hasExtra(APPLICATION_LAUNCH_INTENT_PACKAGE))
            {
                String packageName = intent.getStringExtra(APPLICATION_LAUNCH_INTENT_PACKAGE);

                if (packageName != null)
                {
                    Intent launchIntent = this.getPackageManager().getLaunchIntentForPackage(packageName);

                    if (launchIntent != null)
                    {
                        String launchParams = intent.getStringExtra(APPLICATION_LAUNCH_INTENT_PARAMETERS);

                        if (launchParams != null)
                        {
                            try
                            {
                                JSONObject paramsObj = new JSONObject(launchParams);

                                Iterator<String> keys = paramsObj.keys();

                                while (keys.hasNext())
                                {
                                    String key = keys.next();

                                    launchIntent.putExtra(key, paramsObj.getString(key));
                                }
                            }
                            catch (JSONException e)
                            {
                                LogManager.getInstance(this).logException(e);
                            }
                        }

                        this.startActivity(launchIntent);
                    }

                    String script = intent.getStringExtra(APPLICATION_LAUNCH_INTENT_POSTSCRIPT);

                    if (script != null)
                        BaseScriptEngine.runScript(this, script);
                }
            }
            else if (intent.hasExtra(APPLICATION_LAUNCH_INTENT_URL))
            {
                String url = intent.getStringExtra(APPLICATION_LAUNCH_INTENT_URL);

                if (url != null)
                {
                    Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    this.startActivity(launchIntent);

                    String script = intent.getStringExtra(APPLICATION_LAUNCH_INTENT_POSTSCRIPT);

                    if (script != null)
                        BaseScriptEngine.runScript(this, script);
                }
            }
        }
        else if (RUN_SCRIPT_INTENT.equalsIgnoreCase(action))
        {
            if (intent.hasExtra(RUN_SCRIPT))
            {
                String script = intent.getStringExtra(RUN_SCRIPT);

                if (script != null)
                    BaseScriptEngine.runScript(this, script);
            }
        }
        else if (FIRE_TRIGGERS_INTENT.equals(action))
        {
            TriggerManager.getInstance(this).nudgeTriggers(this);

            Intent updateIntent = new Intent(ManagerService.UPDATE_TRIGGER_SCHEDULE_INTENT);
            updateIntent.setClass(this, ManagerService.class);

            this.startService(updateIntent);
        }
        else if (UPDATE_TRIGGER_SCHEDULE_INTENT.equals(action))
        {
            Runnable r = new Runnable()
            {
                @Override
                public void run()
                {
                    if (ManagerService._updatingTriggerSchedule)
                    {
                        ManagerService._needsTriggerUpdate = true;

                        return;
                    }

                    ManagerService._updatingTriggerSchedule = true;
                    ManagerService._needsTriggerUpdate = false;

                    List<Long> times = TriggerManager.getInstance(me).upcomingFireTimes(me);

                    long now = System.currentTimeMillis();

                    for (int index = 0; now > ManagerService._lastTriggerNudge && index < times.size(); index++)
                    {
                        Long fire = times.get(index);

                        if (Math.abs(ManagerService._lastTriggerNudge - fire) >= 60000)
                        {
                            AlarmManager alarmManager = (AlarmManager) me.getSystemService(Context.ALARM_SERVICE);

                            PendingIntent pi = PendingIntent.getService(me, 0, new Intent(ManagerService.FIRE_TRIGGERS_INTENT), PendingIntent.FLAG_UPDATE_CURRENT);

                            alarmManager.cancel(pi);

                            ManagerService._lastTriggerNudge = fire;

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, fire, pi);
                            else
                                alarmManager.set(AlarmManager.RTC_WAKEUP, fire, pi);

                            break;
                        }
                    }

                    ManagerService._updatingTriggerSchedule = false;

                    if (ManagerService._needsTriggerUpdate)
                        this.run();
                }
            };

            Thread t = new Thread(r);
            t.start();
        }
        else if (REFRESH_CONFIGURATION.equals(action))
            LegacyJSONConfigFile.update(this, false);
    }

    public static void setupPeriodicCheck(final Context context)
    {
        if (ManagerService._checkSetup)
            return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent pi = PendingIntent.getService(context, 0, new Intent(ManagerService.REFRESH_CONFIGURATION), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 60000, pi);

        pi = PendingIntent.getService(context, 0, new Intent(ManagerService.UPDATE_TRIGGER_SCHEDULE_INTENT), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), (15 * 60 * 1000), pi);

        prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
            {
                Intent reloadIntent = new Intent(ManagerService.REFRESH_CONFIGURATION);
                reloadIntent.setClass(context, ManagerService.class);

                context.startService(reloadIntent);
            }
        });

        Intent nudgeIntent = new Intent(PersistentService.NUDGE_PROBES);
        nudgeIntent.setClass(context, PersistentService.class);

        context.startService(nudgeIntent);

        SanityManager.getInstance(context);

        ManagerService._checkSetup = true;
    }

    public static String soundNameForPath(Context context, String path)
    {
        String[] values = context.getResources().getStringArray(R.array.sound_effect_values);
        String[] labels = context.getResources().getStringArray(R.array.sound_effect_labels);

        for (int i = 0; i < labels.length && i < values.length; i++)
        {
            if (values[i].toLowerCase(Locale.ENGLISH).equals(path.toLowerCase(Locale.getDefault())))
                return labels[i];
        }

        return path;
    }

    private static String pathForSound(Context context, String name)
    {
        String[] values = context.getResources().getStringArray(R.array.sound_effect_values);
        String[] labels = context.getResources().getStringArray(R.array.sound_effect_labels);

        for (int i = 0; i < labels.length && i < values.length; i++)
        {
            if (labels[i].toLowerCase(Locale.getDefault()).equals(name.toLowerCase(Locale.getDefault())))
                return values[i];
        }

        for (int i = 0; i < labels.length && i < values.length; i++)
        {
            if (labels[i].toLowerCase(Locale.getDefault()).endsWith(name.toLowerCase(Locale.getDefault())))
                return values[i];
        }

        return name;
    }

    public static void resetTriggerNudgeDate()
    {
        ManagerService._lastTriggerNudge = 0;
    }

    public static void stopAllVibrations()
    {
        ManagerService._vibrationRepeats = false;

    }
}
