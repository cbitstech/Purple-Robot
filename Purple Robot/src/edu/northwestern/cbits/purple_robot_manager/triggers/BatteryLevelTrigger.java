package edu.northwestern.cbits.purple_robot_manager.triggers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.R;

public class BatteryLevelTrigger extends Trigger
{
    public static final String TYPE_NAME = "battery_level";
    private static final String TRIGGER_THRESHOLD = "threshold";
    private static final String TRIGGER_DECREASING = "is_decreasing";
    private BroadcastReceiver _receiver = null;

    private int _threshold = 0;
    private int _lastLevel = 100;

    private boolean _isDecreasing = true;

    public BatteryLevelTrigger(final Context context, Map<String, Object> map)
    {
        super(context, map);

        this.updateFromMap(context, map);

        final BatteryLevelTrigger me = this;

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        this._receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                boolean enabled = me.enabled(context);

                Bundle extras = intent.getExtras();

                int level = extras.getInt(BatteryManager.EXTRA_LEVEL);

                if (enabled && me._isDecreasing && (me._lastLevel > me._threshold && level <= me._threshold))
                {
                    me.execute(context, true);
                }
                else if (enabled && me._isDecreasing == false && (me._lastLevel < me._threshold && level >= me._threshold))
                {
                    me.execute(context, true);
                }

                me._lastLevel = level;
            }
        };

        context.registerReceiver(this._receiver, filter);
    }

    public void merge(Trigger trigger)
    {
        if (trigger instanceof BatteryLevelTrigger)
        {
            super.merge(trigger);

            BatteryLevelTrigger batteryTrigger = (BatteryLevelTrigger) trigger;

            this._threshold = batteryTrigger._threshold;
        }
    }

    public boolean matches(Context context, Object object)
    {
        // Fire in response to broadcast, not TriggerManager polling...

        return false;
    }

    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> config = super.configuration(context);

        config.put(BatteryLevelTrigger.TRIGGER_THRESHOLD, this._threshold);
        config.put(BatteryLevelTrigger.TRIGGER_DECREASING, this._isDecreasing);
        config.put("type", BatteryLevelTrigger.TYPE_NAME);

        return config;
    }

    public boolean updateFromMap(Context context, Map<String, Object> map)
    {
        if (super.updateFromMap(context, map))
        {
            if (map.containsKey(BatteryLevelTrigger.TRIGGER_THRESHOLD)) {
                this._threshold = (int) map.get(BatteryLevelTrigger.TRIGGER_THRESHOLD);
            }

            if (map.containsKey(BatteryLevelTrigger.TRIGGER_DECREASING)) {
                this._isDecreasing = (boolean) map.get(BatteryLevelTrigger.TRIGGER_DECREASING);
            }

            return true;
        }

        return false;
    }

    public void refresh(Context context)
    {
        // Nothing to do for this trigger type...
    }

    public String getDiagnosticString(Context context)
    {
        String name = this.name();
        String identifier = this.identifier();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String key = "last_fired_" + identifier;

        long lastFired = prefs.getLong(key, 0);

        String lastFiredString = context.getString(R.string.trigger_fired_never);

        if (lastFired != 0)
        {
            DateFormat formatter = android.text.format.DateFormat.getMediumDateFormat(context);
            DateFormat timeFormatter = android.text.format.DateFormat.getTimeFormat(context);

            lastFiredString = formatter.format(new Date(lastFired)) + " " + timeFormatter.format(new Date(lastFired));
        }

        String enabled = context.getString(R.string.trigger_disabled);

        if (this.enabled(context))
            enabled = context.getString(R.string.trigger_enabled);

        return context.getString(R.string.trigger_diagnostic_string, name, identifier, enabled, lastFiredString);
    }

    public Bundle bundle(Context context)
    {
        Bundle bundle = super.bundle(context);

        bundle.putString(Trigger.TYPE, BatteryLevelTrigger.TYPE_NAME);

        bundle.putInt(BatteryLevelTrigger.TRIGGER_THRESHOLD, this._threshold);
        bundle.putBoolean(BatteryLevelTrigger.TRIGGER_DECREASING, this._isDecreasing);

        return bundle;
    }

    public void addCustomPreferences(Context context, PreferenceScreen screen) {
        Preference threshold = new Preference(context);
        threshold.setTitle("" + this._threshold);
        threshold.setSummary(R.string.label_trigger_battery_threshold);
        screen.addPreference(threshold);

        Preference direction = new Preference(context);
        direction.setSummary(R.string.label_trigger_battery_direction);

        if (this._isDecreasing)
            direction.setTitle(R.string.label_trigger_battery_decreasing);
        else
            direction.setTitle(R.string.label_trigger_battery_increasing);

        screen.addPreference(direction);
    }
}
