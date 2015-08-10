package edu.northwestern.cbits.purple_robot_manager.triggers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.util.LongSparseArray;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.SettingsKeys;
import edu.northwestern.cbits.purple_robot_manager.config.SchemeConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.util.WakeLockManager;

public class TriggerManager
{
    private static TriggerManager _instance = null;

    private final List<Trigger> _triggers = new ArrayList<>();
    private Timer _timer = null;

    private boolean _triggersInited = false;

    private TriggerManager(Context context)
    {
        if (TriggerManager._instance != null)
            throw new IllegalStateException("Already instantiated");
    }

    public static TriggerManager getInstance(Context context)
    {
        if (TriggerManager._instance == null)
        {
            TriggerManager._instance = new TriggerManager(context.getApplicationContext());
            TriggerManager._instance.restoreTriggers(context);
        }

        return TriggerManager._instance;
    }

    @SuppressLint("Wakelock")
    public void nudgeTriggers(Context context)
    {
        WakeLock wakeLock = WakeLockManager.getInstance(context).requestWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "trigger_manager_wakelock");

        Date now = new Date();

        synchronized (this._triggers)
        {
            for (Trigger trigger : this._triggers)
            {
                boolean execute = false;

                if (trigger instanceof DateTrigger)
                {
                    if (trigger.matches(context, now))
                        execute = true;
                }

                if (execute)
                {
                    trigger.execute(context, false);
                }
            }
        }

        WakeLockManager.getInstance(context).releaseWakeLock(wakeLock);
    }

    public void updateTriggers(Context context, List<Trigger> triggerList)
    {
        ArrayList<Trigger> toAdd = new ArrayList<>();

        synchronized (this._triggers)
        {
            for (Trigger newTrigger : triggerList)
            {
                boolean found = false;

                for (Trigger trigger : this._triggers)
                {
                    if (trigger.equals(newTrigger))
                    {
                        trigger.merge(newTrigger);

                        found = true;
                    }
                }

                if (!found)
                    toAdd.add(newTrigger);
            }

            if (toAdd.size() > 0)
                ManagerService.resetTriggerNudgeDate();

            this._triggers.addAll(toAdd);
        }

        this.persistTriggers(context);
    }

    private void restoreTriggers(Context context)
    {
        if (this._triggersInited)
            return;

        this._triggersInited = true;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.contains("triggers_scheme_script"))
        {
            String script = prefs.getString("triggers_scheme_script", "(begin)");

            try
            {
                BaseScriptEngine.runScript(context, script);
            }
            catch (Exception e)
            {
                LogManager.getInstance(context).logException(e);
            }
        }
    }

    protected void persistTriggers(final Context context)
    {
        if (this._timer == null)
        {
            this._timer = new Timer();

            final TriggerManager me = this;

            this._timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    Editor e = prefs.edit();

                    SchemeConfigFile config = new SchemeConfigFile(context);
                    String script = config.triggersScript(context);

                    e.putString("triggers_scheme_script", script);

                    e.commit();

                    me._timer = null;
                }

            }, 5000);
        }

        Intent intent = new Intent(ManagerService.UPDATE_TRIGGER_SCHEDULE_INTENT);
        intent.setClass(context, ManagerService.class);

        context.startService(intent);
    }

    public List<Trigger> triggersForId(String triggerId)
    {
        ArrayList<Trigger> matches = new ArrayList<>();

        synchronized (this._triggers)
        {
            for (Trigger trigger : this._triggers)
            {
                if (trigger.identifier() != null && trigger.identifier().equals(triggerId))
                    matches.add(trigger);
            }
        }
        return matches;
    }

    public List<Trigger> allTriggers()
    {
        return this._triggers;
    }

    public void addTrigger(Context context, Trigger t)
    {
        ArrayList<Trigger> ts = new ArrayList<>();

        ts.add(t);

        this.updateTriggers(context, ts);
    }

    public void removeAllTriggers()
    {
        synchronized (this._triggers)
        {
            this._triggers.clear();
        }
    }

    @SuppressWarnings("deprecation")
    public PreferenceScreen buildPreferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setOrder(0);
        screen.setTitle(R.string.title_preference_triggers_screen);
        screen.setKey(SettingsKeys.TRIGGERS_SCREEN_KEY);

        PreferenceCategory triggersCategory = new PreferenceCategory(context);
        triggersCategory.setTitle(R.string.title_preference_triggers_category);
        triggersCategory.setKey("key_available_triggers");

        screen.addPreference(triggersCategory);

        synchronized (this._triggers)
        {
            for (Trigger trigger : this._triggers)
            {
                PreferenceScreen triggerScreen = trigger.preferenceScreen(context, manager);

                if (triggerScreen != null)
                    screen.addPreference(triggerScreen);
            }
        }

        return screen;
    }

    public List<Map<String, Object>> triggerConfigurations(Context context)
    {
        List<Map<String, Object>> configs = new ArrayList<>();

        synchronized (this._triggers)
        {
            for (Trigger t : this._triggers)
            {
                Map<String, Object> config = t.configuration(context);

                configs.add(config);
            }
        }

        return configs;
    }

    public void refreshTriggers(Context context)
    {
        synchronized (this._triggers)
        {
            for (Trigger t : this._triggers)
                t.refresh(context);
        }
    }

    public List<String> triggerIds()
    {
        ArrayList<String> triggerIds = new ArrayList<>();

        synchronized (this._triggers)
        {
            for (Trigger t : this._triggers)
            {
                String id = t.identifier();

                if (id != null && triggerIds.contains(id) == false)
                    triggerIds.add(id);
            }
        }

        return triggerIds;
    }

    public Map<String, Object> fetchTrigger(Context context, String id)
    {
        List<Trigger> triggers = this.triggersForId(id);

        if (triggers.size() > 0)
            return triggers.get(0).configuration(context);

        return null;
    }

    public boolean deleteTrigger(String id)
    {
        List<Trigger> triggers = this.triggersForId(id);

        synchronized (this._triggers)
        {
            this._triggers.removeAll(triggers);
        }

        return triggers.size() > 0;
    }

    public ArrayList<Bundle> allTriggersBundles(Context context)
    {
        ArrayList<Bundle> triggers = new ArrayList<>();

        synchronized (this._triggers)
        {
            for (Trigger trigger : this._triggers)
            {
                triggers.add(trigger.bundle(context));
            }
        }

        return triggers;
    }

    public void fireMissedTriggers(final Context context, long now)
    {
        final LongSparseArray<String> fireDates = new LongSparseArray<>();

        for (Trigger trigger : this._triggers)
        {
            if (trigger instanceof DateTrigger)
            {
                DateTrigger dateTrig = (DateTrigger) trigger;

                if (dateTrig.missedFire(context, now))
                {
                    long lastFired = dateTrig.lastMissedFireTime(context);

                    fireDates.put(lastFired, dateTrig.identifier());
                }
            }
        }

        final TriggerManager me = this;

        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                for (int i = 0; i < fireDates.size(); i++)
                {
                    long time = fireDates.keyAt(i);

                    for (Trigger t : me.triggersForId(fireDates.get(time)))
                    {
                        t.execute(context, true);

                        // Wait 5 seconds for this trigger to complete so things
                        // run in some semblance of "order" if each script runs
                        // in 5 sec. or less...

                        try
                        {
                            Thread.sleep(5000);
                        }
                        catch (InterruptedException e)
                        {
                            LogManager.getInstance(context).logException(e);
                        }
                    }
                }
            }
        };

        Thread t = new Thread(r);
        t.start();
    }

    @SuppressLint("Wakelock")
    public List<Long> upcomingFireTimes(Context context)
    {
        ArrayList<Long> upcoming = new ArrayList<>();

        WakeLock wakeLock = WakeLockManager.getInstance(context).requestWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "trigger_wakelock");

        long now = System.currentTimeMillis();

        synchronized (this._triggers)
        {
            for (Trigger trigger : this._triggers)
            {
                if (trigger instanceof DateTrigger)
                {
                    DateTrigger dateTrigger = (DateTrigger) trigger;

                    upcoming.addAll(dateTrigger.fireTimes(context, now, now + (60 * 60 * 1000)));
                }
            }
        }

        Collections.sort(upcoming);

        WakeLockManager.getInstance(context).releaseWakeLock(wakeLock);

        return upcoming;
    }
}
