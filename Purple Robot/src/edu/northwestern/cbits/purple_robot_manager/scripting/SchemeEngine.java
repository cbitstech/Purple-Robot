package edu.northwestern.cbits.purple_robot_manager.scripting;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.annotation.ScriptingEngineMethod;
import edu.northwestern.cbits.purple_robot_manager.models.ModelManager;
import jscheme.JScheme;
import jscheme.SchemeException;
import jsint.Evaluator;
import jsint.Pair;
import jsint.Symbol;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.ScheduleManager;
import edu.northwestern.cbits.purple_robot_manager.config.SchemeConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.features.Feature;

public class SchemeEngine extends BaseScriptEngine
{
    public SchemeEngine(Context context, Map<String, Object> objects)
    {
        super(context);
    }

    @SuppressLint("DefaultLocale")
    public static boolean canRun(String script)
    {
        script = script.trim();

        if (script == null || script.length() < 1)
            return false;
        else if (script.charAt(0) == '(')
        {
            if (script.charAt(script.length() - 1) == ';')
                return false;
            else
            {
                JScheme scheme = new JScheme();

                Object pair = scheme.read(script);

                if ((pair instanceof Pair) == false)
                    return false;
            }

            return true;
        }

        return false;
    }

    @SuppressLint("DefaultLocale")
    @ScriptingEngineMethod(language = "Scheme")
    public Object evaluateSource(String source)
    {
        if (source.trim().toLowerCase().equals("(begin)"))
            return null;

        Evaluator eval = new Evaluator();
        eval.getInteractionEnvironment().setValue(Symbol.intern("PurpleRobot"), this);
        eval.getInteractionEnvironment().setValue(Symbol.intern("JSONHelper"), new JSONHelper());
        JScheme scheme = new JScheme(eval);

        try
        {
            scheme.load(new InputStreamReader(this._context.getAssets().open("scheme/pregexp.scm")));
        }
        catch (IOException e)
        {
            LogManager.getInstance(this._context).logException(e);
        }
        catch (StackOverflowError e)
        {
            LogManager.getInstance(this._context).logException(e);
        }

        try
        {
            scheme.load(new InputStreamReader(this._context.getAssets().open("scheme/json.scm")));
        }
        catch (IOException e)
        {
            LogManager.getInstance(this._context).logException(e);
        }
        catch (StackOverflowError e)
        {
            LogManager.getInstance(this._context).logException(e);
        }

        try
        {
            scheme.load(new InputStreamReader(this._context.getAssets().open("scheme/purple-robot.scm")));
        }
        catch (IOException e)
        {
            LogManager.getInstance(this._context).logException(e);
        }
        catch (StackOverflowError e)
        {
            LogManager.getInstance(this._context).logException(e);
        }

        try
        {
            return scheme.eval(source);
        }
        catch (StackOverflowError e)
        {
            LogManager.getInstance(this._context).logException(e);
        }

        return false;
    }

    protected String language()
    {
        return "Scheme";
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_update_trigger.html", category = R.string.docs_script_category_triggers_automation, arguments = { "identifier", "definition" })
    public boolean updateTrigger(String triggerId, Pair parameters)
    {
        Map<String, Object> paramsMap = SchemeEngine.parsePairList(parameters);

        paramsMap.put("identifier", triggerId);

        return super.updateTrigger(triggerId, paramsMap);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_update_trigger.html", category = R.string.docs_script_category_triggers_automation, arguments = { "identifier", "definition" })
    public boolean updateTrigger(Pair parameters)
    {
        Map<String, Object> paramsMap = SchemeEngine.parsePairList(parameters);

        if (paramsMap.containsKey("identifier"))
        {
            String triggerId = paramsMap.get("identifier").toString();

            return this.updateTrigger(triggerId, parameters);
        }

        return false;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_schedule_script.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "identifier", "dateString", "script" })
    public void scheduleScript(String identifier, String dateString, Pair action)
    {
        super.scheduleScript(identifier, dateString, action.toString());
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_update_probe.html", category = R.string.docs_script_category_data_collection, arguments = { "parameters" })
    public boolean updateProbe(Pair params)
    {
        Map<String, Object> map = SchemeEngine.parsePairList(params);

        return this.updateProbeConfig(map);
    }

    public static Map<String, Object> parsePairList(Pair pair)
    {
        HashMap<String, Object> map = new HashMap<>();

        if (pair.isEmpty() == false)
        {
            Object first = pair.getFirst();

            if (first instanceof Pair)
            {
                Pair firstPair = (Pair) first;

                String key = firstPair.first.toString();

                Object value = firstPair.rest();

                if (value instanceof Symbol)
                {
                    Symbol symbol = (Symbol) value;

                    value = symbol.getGlobalValue();
                }
                if (value instanceof Pair)
                {
                    Pair valuePair = (Pair) value;

                    // value = valuePair.toString();

                    if (valuePair.first() instanceof String && valuePair.rest() instanceof Pair)
                    {
                        ArrayList<Object> list = new ArrayList<>();

                        list.add(valuePair.first());

                        Pair restPair = (Pair) valuePair.rest();

                        while (restPair.isEmpty() == false)
                        {
                            list.add(restPair.first());

                            if (restPair.rest() instanceof Pair)
                                restPair = (Pair) restPair.rest();
                            else
                                restPair = Pair.EMPTY;
                        }

                        value = list;
                    }
                    else
                        value = SchemeEngine.parsePairList(valuePair);
                }

                map.put(key, value);
            }

            Object rest = pair.getRest();

            if (rest instanceof Pair)
            {
                Pair restPair = (Pair) rest;

                Map<String, Object> restMap = SchemeEngine.parsePairList(restPair);

                map.putAll(restMap);
            }
        }

        return map;
    }

    private static Bundle bundleForPair(Pair pair)
    {
        Bundle b = new Bundle();

        if (pair.isEmpty() == false)
        {
            Object first = pair.first();

            if (first instanceof Pair)
            {
                Pair firstPair = (Pair) first;

                Object firstFirst = firstPair.first();

                if (firstFirst instanceof String)
                {
                    String key = (String) firstFirst;

                    Object second = firstPair.rest();

                    if (second instanceof String)
                        b.putString(key, second.toString());
                    else if (second instanceof Pair)
                    {
                        Pair secondPair = (Pair) second;

                        if (secondPair.first().toString().equalsIgnoreCase("begin"))
                            b.putString(key, secondPair.toString());
                        else
                            b.putBundle(key, SchemeEngine.bundleForPair(secondPair));
                    }
                }
            }

            Object rest = pair.rest();

            if (rest instanceof Pair && !((Pair) rest).isEmpty())
            {
                Pair restPair = (Pair) rest;

                Bundle restBundle = SchemeEngine.bundleForPair(restPair);

                b.putAll(restBundle);

            }
        }

        return b;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_show_native_dialog.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "title", "message", "confirmLabel", "cancelLabel", "confirmScript", "cancelScript", "tag", "priority" })
    public void showNativeDialog(String title, String message, String confirmTitle, String cancelTitle,
            Pair confirmAction, Pair cancelAction)
    {
        this.showNativeDialog(title, message, confirmTitle, cancelTitle, confirmAction.toString(),
                cancelAction.toString());
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_show_native_dialog.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "title", "message", "confirmLabel", "cancelLabel", "confirmScript", "cancelScript", "tag", "priority" })
    public void showNativeDialog(String title, String message, String confirmTitle, String cancelTitle,
                                 Pair confirmAction, Pair cancelAction, String tag, int priority)
    {
        this.showNativeDialog(title, message, confirmTitle, cancelTitle, confirmAction.toString(),
                cancelAction.toString(), tag, priority);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_update_config_map.html", category = R.string.docs_script_category_configuration, arguments = { "configMap" })
    public boolean updateConfig(Pair parameters)
    {
        Map<String, Object> paramsMap = SchemeEngine.parsePairList(parameters);

        return super.updateConfig(paramsMap);
    }

    @ScriptingEngineMethod(language = "Scheme")
    public void updateWidget(Pair parameters)
    {
        Map<String, Object> paramsMap = SchemeEngine.parsePairList(parameters);

        Intent intent = new Intent(ManagerService.UPDATE_WIDGETS);
        intent.setClass(this._context, ManagerService.class);

        for (String key : paramsMap.keySet())
        {
            intent.putExtra(key, paramsMap.get(key).toString());
        }

        this._context.startService(intent);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_readings.html", category = R.string.docs_script_category_data_collection, arguments = { })
    public Pair readings()
    {
        Map<String, Object> readings = ModelManager.getInstance(this._context).readings(this._context);

        return this.pairForMap(readings);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_emit_reading.html", category = R.string.docs_script_category_data_collection, arguments = { "name", "value", "priority" })
    public void emitReading(String name, Object value)
    {
        this.emitReading(name, value, false);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_emit_reading.html", category = R.string.docs_script_category_data_collection, arguments = { "name", "value", "priority" })
    public void emitReading(String name, Object value, boolean priority)
    {
        Bundle bundle = new Bundle();
        bundle.putString("PROBE", name);
        bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

        if (priority)
            bundle.putBoolean("PRIORITY", true);

        if (value instanceof String)
            bundle.putString(Feature.FEATURE_VALUE, value.toString());
        else if (value instanceof Double)
        {
            Double d = (Double) value;

            bundle.putDouble(Feature.FEATURE_VALUE, d);
        }
        else if (value instanceof Integer)
        {
            Integer i = (Integer) value;

            bundle.putDouble(Feature.FEATURE_VALUE, i.doubleValue());
        }
        else if (value instanceof Pair)
        {
            Pair pair = (Pair) value;

            try
            {
                Bundle b = SchemeEngine.bundleForPair(pair);

                bundle.putParcelable(Feature.FEATURE_VALUE, b);
            }
            catch (SchemeException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            Log.e("PR", "SCHEME ENGINE GOT UNKNOWN VALUE " + value);

            if (value != null)
                Log.e("PR", "SCHEME ENGINE GOT UNKNOWN CLASS " + value.getClass());
        }

        this.transmitData(bundle);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_broadcast_intent.html", category = R.string.docs_script_category_system_integration, arguments = { "action", "extras" })
    public boolean broadcastIntent(final String action, Pair extras)
    {
        return this.broadcastIntent(action, SchemeEngine.parsePairList(extras));
    }

    @ScriptingEngineMethod(language = "Scheme")
    public boolean updateWidget(final String title, final String message, final String applicationName,
            final Pair launchParams, final String script)
    {
        return this.updateWidget(title, message, applicationName, SchemeEngine.parsePairList(launchParams), script);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_labels.html", category = R.string.docs_script_category_data_collection, arguments = { "instructions", "labels" })
    public void fetchLabels(String appContext, String instructions, final Pair labels)
    {
        super.fetchLabelsInterface(appContext, instructions, SchemeEngine.parsePairList(labels));
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_launch_application.html", category = R.string.docs_script_category_system_integration, arguments = { "applicationName", "options", "script" })
    public boolean launchApplication(String applicationName, final Pair launchParams, final String script)
    {
        return this.launchApplication(applicationName, SchemeEngine.parsePairList(launchParams), script);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_show_app_launch_note.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "title", "message", "packageName" })
    public boolean showApplicationLaunchNotification(String title, String message, String applicationName,
            boolean persistent, final Pair launchParams, final String script)
    {
        return this.showApplicationLaunchNotification(title, message, applicationName, persistent,
                SchemeEngine.parsePairList(launchParams), script);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_show_app_launch_note.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "title", "message", "packageName" })
    public boolean showApplicationLaunchNotification(String title, String message, String applicationName,
            final Pair launchParams, final String script)
    {
        return this.showApplicationLaunchNotification(title, message, applicationName,
                SchemeEngine.parsePairList(launchParams), script);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_config.html", category = R.string.docs_script_category_configuration, arguments = { })
    public String fetchConfig()
    {
        SchemeConfigFile config = new SchemeConfigFile(this._context);

        return config.toString();
    }

    public Pair dateToComponents(Date date)
    {
        String stringRep = this.formatDate(date);

        String year = stringRep.substring(0, 4);
        String month = stringRep.substring(4, 6);
        String day = stringRep.substring(6, 8);
        String hour = stringRep.substring(9, 11);
        String minute = stringRep.substring(11, 13);
        String second = stringRep.substring(13, 15);

        return new Pair(year, new Pair(month, new Pair(day, new Pair(hour, new Pair(minute, new Pair(second, null))))));
    }

    public Date dateFromComponents(Pair components)
    {

        String year = components.nth(0).toString();
        String month = components.nth(1).toString();
        String day = components.nth(2).toString();
        String hour = components.nth(3).toString();
        String minute = components.nth(4).toString();
        String second = components.nth(5).toString();

        String dateString = year + month + day + "T" + hour + minute + second;

        return ScheduleManager.parseString(dateString);
    }

    @ScriptingEngineMethod(language = "Scheme", assetPath = "scheme_nth.html", category = R.string.docs_script_category_language_enhancements, arguments = { "index", "list" })
    public Object nth(int index, Object obj)
    {
        if (obj == null)
            return null;
        else if (index < 0)
            return null;
        else if ((obj instanceof Pair) == false)
            return null;

        Pair list = (Pair) obj;

        if (index == 0)
            return list.first();

        return this.nth(index - 1, list.rest());
    }

    public Object valueFromString(String key, String string)
    {
        Object value = super.valueFromString(key, string);

        if (value instanceof Map<?, ?>)
            value = this.pairForMap((Map<?, ?>) value);
        else if (value instanceof List<?>)
            value = this.pairForList((List<?>) value);

        return value;
    }

    private Pair pairForMap(Map<?, ?> map)
    {
        Pair list = Pair.EMPTY;

        for (Object key : map.keySet())
        {
            Object value = map.get(key.toString());

            if (value instanceof Map<?, ?>)
                value = this.pairForMap((Map<?, ?>) value);
            else if (value instanceof List<?>)
                value = this.pairForList((List<?>) value);

            list = new Pair(new Pair(key, value), list);
        }

        return new Pair(new Pair(Symbol.QUOTE, new Pair(list, Pair.EMPTY)), Pair.EMPTY);
    }

    private Pair pairForList(List<?> list)
    {
        Pair pairs = Pair.EMPTY;

        for (Object value : list)
        {
            if (value instanceof Map<?, ?>)
                value = this.pairForMap((Map<?, ?>) value);
            else if (value instanceof List<?>)
                value = this.pairForList((List<?>) value);

            pairs = new Pair(value, pairs);
        }

        return new Pair(Symbol.QUOTE, new Pair(pairs, Pair.EMPTY));
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_scheduled_scripts.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { })
    public Pair scheduledScripts()
    {
        List<Map<String, String>> scripts = ScheduleManager.allScripts(this._context);

        return this.pairForList(scripts);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_namespaces.html", category = R.string.docs_script_category_persistence, arguments = { })
    public Pair namespaces()
    {
        List<String> namespaces = super.namespaceList();

        return this.pairForList(namespaces);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_namespace.html", category = R.string.docs_script_category_persistence, arguments = { "namespaceId" })
    public Pair fetchNamespace(String namespace)
    {
        Map<String, Object> map = super.fetchNamespaceMap(namespace);

        return this.pairForMap(map);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_trigger.html", category = R.string.docs_script_category_triggers_automation, arguments = { "triggerId" })
    public Pair fetchTrigger(String id)
    {
        Map<String, Object> trigger = super.fetchTriggerMap(id);

        if (trigger != null)
            return this.pairForMap(trigger);

        return null;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_trigger_ids.html", category = R.string.docs_script_category_triggers_automation, arguments = { })
    public Pair fetchTriggerIds()
    {
        List<String> triggerIds = super.fetchTriggerIdList();

        return this.pairForList(triggerIds);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_list_tones.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { })
    public Pair listTones()
    {
        List<String> tones = super.fetchToneList();

        return (Pair) (((Pair) this.pairForList(tones).getRest()).getFirst());
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_snapshot_ids.html", category = R.string.docs_script_category_data_collection, arguments = { })
    public Pair fetchSnapshotIds()
    {
        List<String> snapshotIds = super.fetchSnapshotIdList();

        return (Pair) this.pairForList(snapshotIds).second();
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_snapshot.html", category = R.string.docs_script_category_data_collection, arguments = { "snapshotId" })
    public Pair fetchSnapshot(String timestamp)
    {
        Map<String, Object> snapshot = super.fetchSnapshotMap(timestamp);

        if (snapshot != null)
            return this.pairForMap(snapshot);

        return null;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_probe_config.html", category = R.string.docs_script_category_data_collection, arguments = { "probeName" })
    public Pair probeConfig(String name)
    {
        Map<String, Object> map = this.probeConfigMap(name);

        if (map != null)
            return this.pairForMap(map);

        return null;
    }
}
