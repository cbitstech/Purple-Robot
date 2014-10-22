package edu.northwestern.cbits.purple_robot_manager.triggers;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;

public class ProbeTrigger extends Trigger {
    public static final String TYPE_NAME = "probe";
    private static final String TRIGGER_TEST = "test";
    private static final String TRIGGER_PROBE = "probe";
    private static final String PROBE = null;
    private static final String TEST = null;

    private String _probe = null;
    private String _test = null;

    private long _lastUpdate = 0;

    public ProbeTrigger(Context context, Map<String, Object> map) {
        super(context, map);

        this.updateFromMap(context, map);
    }

    public void merge(Trigger trigger) {
        if (trigger instanceof ProbeTrigger) {
            super.merge(trigger);

            ProbeTrigger probeTrigger = (ProbeTrigger) trigger;

            this._test = probeTrigger._test;
        }
    }

    public boolean matches(Context context, Object object) {
        long now = System.currentTimeMillis();

        if (this._test == null || now - this._lastUpdate < 5000)
            return false;

        this._lastUpdate = now;

        HashMap<String, Object> objects = new HashMap<String, Object>();

        if (object instanceof JSONObject) {
            JSONObject json = (JSONObject) object;

            JSONArray names = json.names();

            for (int i = 0; i < names.length(); i++) {
                try {
                    String name = names.getString(i);
                    objects.put(name, json.get(name));
                } catch (JSONException e) {
                    LogManager.getInstance(context).logException(e);
                }
            }
        }

        try {
            Object result = BaseScriptEngine.runScript(context, this._test,
                    objects);

            if (result instanceof Boolean) {
                Boolean boolResult = (Boolean) result;

                return boolResult.booleanValue();
            }
        } catch (Throwable e) {
            LogManager.getInstance(context).logException(e);
        }

        return false;
    }

    public Map<String, Object> configuration(Context context) {
        Map<String, Object> config = super.configuration(context);

        config.put(ProbeTrigger.TRIGGER_TEST, this._test);
        config.put(ProbeTrigger.TRIGGER_PROBE, this._probe);
        config.put("type", ProbeTrigger.TYPE_NAME);

        return config;
    }

    public boolean updateFromMap(Context context, Map<String, Object> map) {
        if (super.updateFromMap(context, map)) {
            if (map.containsKey(ProbeTrigger.TRIGGER_TEST))
                this._test = map.get(ProbeTrigger.TRIGGER_TEST).toString();

            if (map.containsKey(ProbeTrigger.TRIGGER_PROBE))
                this._probe = map.get(ProbeTrigger.TRIGGER_PROBE).toString();

            return true;
        }

        return false;
    }

    public void refresh(Context context) {
        // Nothing to do for this trigger type...
    }

    public boolean matchesProbe(String probeName) {
        if (this._probe != null && this._probe.equals(probeName))
            return true;

        return false;
    }

    public String getDiagnosticString(Context context) {
        String name = this.name();
        String identifier = this.identifier();

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        String key = "last_fired_" + identifier;

        long lastFired = prefs.getLong(key, 0);

        String lastFiredString = context
                .getString(R.string.trigger_fired_never);

        if (lastFired != 0) {
            DateFormat formatter = android.text.format.DateFormat
                    .getMediumDateFormat(context);
            DateFormat timeFormatter = android.text.format.DateFormat
                    .getTimeFormat(context);

            lastFiredString = formatter.format(new Date(lastFired)) + " "
                    + timeFormatter.format(new Date(lastFired));
        }

        String enabled = context.getString(R.string.trigger_disabled);

        if (this.enabled(context))
            enabled = context.getString(R.string.trigger_enabled);

        return context.getString(R.string.trigger_diagnostic_string, name,
                identifier, enabled, lastFiredString);
    }

    public Bundle bundle(Context context) {
        Bundle bundle = super.bundle(context);

        bundle.putString(Trigger.TYPE, ProbeTrigger.TYPE_NAME);

        if (this._probe != null)
            bundle.putString(ProbeTrigger.PROBE, this._probe);

        if (this._test != null)
            bundle.putString(ProbeTrigger.TEST, this._probe);

        return bundle;
    }
}
