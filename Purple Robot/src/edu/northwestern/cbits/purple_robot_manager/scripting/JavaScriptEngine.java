package edu.northwestern.cbits.purple_robot_manager.scripting;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.ScheduleManager;
import edu.northwestern.cbits.purple_robot_manager.annotation.ScriptingEngineMethod;
import edu.northwestern.cbits.purple_robot_manager.config.JSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.models.ModelManager;
import edu.northwestern.cbits.purple_robot_manager.probes.features.Feature;
import edu.northwestern.cbits.purple_robot_manager.probes.features.JavascriptFeature;

public class JavaScriptEngine extends BaseScriptEngine
{
    private Context _jsContext = null;
    private Scriptable _scope = null;

    public JavaScriptEngine(android.content.Context context)
    {
        super(context);
    }

    @ScriptingEngineMethod(language = "JavaScript", assetPath = "js_run_script.html", category = R.string.docs_script_category_system_integration, arguments = { "script", "extras", "extraValues" })
    public Object runScript(String script) throws EvaluatorException, EcmaError
    {
        return this.runScript(script, null, null);
    }

    @ScriptingEngineMethod(language = "JavaScript", assetPath = "js_run_script.html", category = R.string.docs_script_category_system_integration, arguments = { "script", "extras", "extraValues" })
    public Object runScript(String script, String extrasName, Object extras) throws EvaluatorException, EcmaError
    {
        this._jsContext = Context.enter();
        this._jsContext.setOptimizationLevel(-1);

        this._scope = _jsContext.initStandardObjects();

        Object thisWrapper = Context.javaToJS(this, this._scope);
        ScriptableObject.putProperty(this._scope, "PurpleRobot", thisWrapper);

        if (extras instanceof Map<?, ?>)
        {
            extras = JavaScriptEngine.mapToNative(this._jsContext, this._scope, (Map<String, Object>) extras);

            extras = NativeJSON.stringify(this._jsContext, this._scope, extras, null, null);
        }

        if (extras != null && extrasName != null)
            script = "var " + extrasName + " = " + extras.toString() + "; " + script;

        return this._jsContext.evaluateString(this._scope, script, "<engine>", 0, null);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_log.html", category = R.string.docs_script_category_diagnostic, arguments = { "message" })
    public boolean log(String event, NativeObject params)
    {
        if (params != null)
            return LogManager.getInstance(this._context).log(event, JavaScriptEngine.nativeToMap(params));

        return LogManager.getInstance(this._context).log(event, new HashMap<String, Object>());
    }

    @SuppressWarnings("resource")
    @ScriptingEngineMethod(language = "JavaScript", assetPath = "js_load_library.html", category = R.string.docs_script_category_system_integration, arguments = { "libraryName" })
    public boolean loadLibrary(String libraryName)
    {
        if (this._jsContext != null && this._scope != null)
        {
            try
            {
                if (libraryName.endsWith(".js") == false)
                    libraryName += ".js";

                AssetManager am = this._context.getAssets();

                InputStream jsStream = am.open("js/" + libraryName);

                // http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
                Scanner s = new Scanner(jsStream).useDelimiter("\\A");

                String script = "";

                if (s.hasNext())
                    script = s.next();
                else
                    return false;

                this._jsContext.evaluateString(this._scope, script, "<engine>", 1, null);

                return true;
            }
            catch (IOException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }
        }

        return false;
    }

    @ScriptingEngineMethod(language = "JavaScript")
    public void updateWidget(final NativeObject parameters)
    {
        this.updateWidget(JavaScriptEngine.nativeToMap(parameters));
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_namespaces.html", category = R.string.docs_script_category_persistence, arguments = { })
    public NativeArray namespaces()
    {
        List<String> namespaces = super.namespaceList();

        NativeArray allNamespaces = new NativeArray(namespaces.size());

        for (int i = 0; i < namespaces.size(); i++)
        {
            allNamespaces.put(i, allNamespaces, namespaces.get(i));
        }

        return allNamespaces;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_scheduled_scripts.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { })
    public NativeArray scheduledScripts()
    {
        List<Map<String, String>> scripts = ScheduleManager.allScripts(this._context);

        NativeArray allScripts = new NativeArray(scripts.size());

        for (int i = 0; i < scripts.size(); i++)
        {
            Map<String, String> script = scripts.get(i);

            Map<String, Object> scriptObj = new HashMap<String, Object>();

            for (String key : script.keySet())
                scriptObj.put(key, script.get(key));

            NativeObject nativeObj = JavaScriptEngine.mapToNative(this._jsContext, this._scope, scriptObj);

            try
            {
                Log.e("PR", "NATIVE OBJ: " + JavaScriptEngine.nativeToJson(nativeObj).toString(2));
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

            allScripts.put(i, allScripts, nativeObj);
        }

        return allScripts;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_broadcast_intent.html", category = R.string.docs_script_category_system_integration, arguments = { "action", "extras" })
    public boolean broadcastIntent(final String action, final NativeObject extras)
    {
        return this.broadcastIntent(action, JavaScriptEngine.nativeToMap(extras));
    }

    @ScriptingEngineMethod(language = "JavaScript")
    public boolean updateWidget(final String title, final String message, final String applicationName, final NativeObject launchParams, final String script)
    {
        return this.updateWidget(title, message, applicationName, JavaScriptEngine.nativeToMap(launchParams), script);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_launch_application.html", category = R.string.docs_script_category_system_integration, arguments = { "applicationName", "options", "script" })
    public boolean launchApplication(String applicationName, final NativeObject launchParams, final String script)
    {
        return this.launchApplication(applicationName, JavaScriptEngine.nativeToMap(launchParams), script);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_show_app_launch_note.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "title", "message", "packageName" })
    public boolean showApplicationLaunchNotification(String title, String message, String applicationName, boolean persistent, final NativeObject launchParams, final String script)
    {
        return this.showApplicationLaunchNotification(title, message, applicationName, persistent, JavaScriptEngine.nativeToMap(launchParams), script);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_show_app_launch_note.html", category = R.string.docs_script_category_dialogs_notifications, arguments = { "title", "message", "packageName" })
    public boolean showApplicationLaunchNotification(String title, String message, String applicationName, final NativeObject launchParams, final String script)
    {
        return this.showApplicationLaunchNotification(title, message, applicationName, JavaScriptEngine.nativeToMap(launchParams), script);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_update_trigger.html", category = R.string.docs_script_category_triggers_automation, arguments = { "identifier", "definition" })
    public boolean updateTrigger(String triggerId, NativeObject nativeJson)
    {
        return this.updateTrigger(triggerId, JavaScriptEngine.nativeToMap(nativeJson));
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_emit_reading.html", category = R.string.docs_script_category_data_collection, arguments = { "name", "value", "priority" })
    public void emitReading(String name, Object value)
    {
        this.emitReading(name, value, false);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_emit_reading.html", category = R.string.docs_script_category_data_collection, arguments = { "name", "value", "priority" })
    public void emitReading(String name, Object value, boolean priority)
    {
        double now = System.currentTimeMillis();

        Bundle bundle = new Bundle();
        bundle.putString("PROBE", name);
        bundle.putDouble("TIMESTAMP", now / 1000);

        if (priority)
            bundle.putBoolean("PRIORITY", true);

        if (value instanceof String)
            bundle.putString(Feature.FEATURE_VALUE, value.toString());
        else if (value instanceof Double)
        {
            Double d = (Double) value;

            bundle.putDouble(Feature.FEATURE_VALUE, d.doubleValue());
        }
        else if (value instanceof NativeObject)
        {
            NativeObject nativeObj = (NativeObject) value;

            Bundle b = JavascriptFeature.bundleForNativeObject(nativeObj);

            bundle.putParcelable(Feature.FEATURE_VALUE, b);
        }
        else
        {
            Log.e("PR", "JS PLUGIN GOT UNKNOWN VALUE " + value);

            if (value != null)
                Log.e("PR", "JS PLUGIN GOT UNKNOWN CLASS " + value.getClass());
        }

        this.transmitData(bundle);
    }

    public static JSONArray nativeToJson(NativeArray nativeArray) throws JSONException
    {
        JSONArray array = new JSONArray();

        for (int i = 0; i < nativeArray.getLength(); i++)
        {
            Object value = nativeArray.get(i);

            if (value instanceof String)
                array.put(value);
            else if (value instanceof Double)
                array.put(value);
            else if (value instanceof NativeObject)
            {
                NativeObject obj = (NativeObject) value;

                array.put(JavaScriptEngine.nativeToJson(obj));
            }
            else if (value instanceof NativeArray)
            {
                NativeArray arr = (NativeArray) value;

                array.put(JavaScriptEngine.nativeToJson(arr));
            }
        }

        return array;
    }

    public static JSONObject nativeToJson(NativeObject nativeObj) throws JSONException
    {
        if (nativeObj == null)
            return null;

        JSONObject json = new JSONObject();

        for (Entry<Object, Object> e : nativeObj.entrySet())
        {
            String key = e.getKey().toString();
            Object value = e.getValue();

            if (value instanceof String)
                json.put(key, value);
            else if (value instanceof Double)
                json.put(key, value);
            else if (value instanceof NativeObject)
            {
                NativeObject obj = (NativeObject) value;

                json.put(key, JavaScriptEngine.nativeToJson(obj));
            }
            else if (value instanceof NativeArray)
            {
                NativeArray arr = (NativeArray) value;

                json.put(key, JavaScriptEngine.nativeToJson(arr));
            }
        }

        return json;
    }

    @Override
    protected String language()
    {
        return "JavaScript";
    }

    public static boolean canRun(String script)
    {
        // TODO: Validate if actually JavaScript...

        return true;
    }

    public static Map<String, Object> nativeToMap(NativeObject nativeObj)
    {
        HashMap<String, Object> params = new HashMap<String, Object>();

        for (Entry<Object, Object> e : nativeObj.entrySet())
        {
            Object value = e.getValue();

            if (value instanceof NativeObject)
                value = JavaScriptEngine.nativeToMap((NativeObject) value);
            else if (value instanceof NativeArray)
                value = JavaScriptEngine.nativeToArrayList((NativeArray) value);

            params.put(e.getKey().toString(), value);
        }

        return params;
    }

    private static ArrayList<Object> nativeToArrayList(NativeArray array)
    {
        ArrayList<Object> list = new ArrayList<Object>();

        for (int i = 0; i < array.size(); i++)
        {
            Object value = array.get(i);

            if (value instanceof NativeObject)
                value = JavaScriptEngine.nativeToMap((NativeObject) value);
            else if (value instanceof NativeArray)
                value = JavaScriptEngine.nativeToArrayList((NativeArray) value);

            list.add(value);
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    private static NativeObject mapToNative(Context context, Scriptable scope, Map<String, Object> map)
    {
        NativeObject obj = (NativeObject) context.newObject(scope);

        for (String key : map.keySet())
        {
            Object value = map.get(key);

            Log.e("PR", "MAP KEY " + key + " -- " + value.toString() + " -- " + value.getClass().getCanonicalName());

            if (value instanceof Map)
                value = JavaScriptEngine.mapToNative(context, scope, (Map<String, Object>) value);
            else if (value instanceof Long)
            {
                Long l = (Long) value;

                value = l.doubleValue();
            }
            else if (value instanceof List)
            {
                List list = (List) value;

                NativeArray array = new NativeArray(list.size());

                for (int i = 0; i < list.size(); i++)
                {
                    Object item = list.get(i);

                    if (item instanceof Map)
                        item = JavaScriptEngine.mapToNative(context, scope, (Map<String, Object>) item);

                    array.put(i, array, item);
                }

                value = array;
            }

            NativeObject.putProperty(obj, key, value);
        }

        return obj;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_config.html", category = R.string.docs_script_category_configuration, arguments = { })
    public String fetchConfig()
    {
        JSONConfigFile config = new JSONConfigFile(this._context);

        return config.toString();
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_update_config_map.html", category = R.string.docs_script_category_configuration, arguments = { "configMap" })
    public boolean updateConfig(NativeObject nativeObj)
    {
        Map<String, Object> paramsMap = JavaScriptEngine.nativeToMap(nativeObj);

        return super.updateConfig(paramsMap);
    }

    public NativeArray fetchNamespaces()
    {
        return this.namespaces();
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_trigger_ids.html", category = R.string.docs_script_category_triggers_automation, arguments = { })
    public NativeArray fetchTriggerIds()
    {
        List<String> triggerIds = super.fetchTriggerIdList();

        String[] values = new String[triggerIds.size()];

        for (int i = 0; i < triggerIds.size(); i++)
        {
            values[i] = triggerIds.get(i);
        }

        return new NativeArray(values);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_snapshot_ids.html", category = R.string.docs_script_category_data_collection, arguments = { })
    public NativeArray fetchSnapshotIds()
    {
        List<String> snapshotIds = super.fetchSnapshotIdList();

        String[] values = new String[snapshotIds.size()];

        for (int i = 0; i < snapshotIds.size(); i++)
        {
            values[i] = snapshotIds.get(i);
        }

        return new NativeArray(values);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_snapshot.html", category = R.string.docs_script_category_data_collection, arguments = { "snapshotId" })
    public NativeObject fetchSnapshot(String timestamp)
    {
        Map<String, Object> snapshot = super.fetchSnapshotMap(timestamp);

        if (snapshot != null)
            return JavaScriptEngine.mapToNative(this._jsContext, this._scope, snapshot);

        return null;
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_namespace.html", category = R.string.docs_script_category_persistence, arguments = { "namespaceId" })
    public NativeObject fetchNamespace(String namespace)
    {
        Map<String, Object> map = super.fetchNamespaceMap(namespace);

        return JavaScriptEngine.mapToNative(this._jsContext, this._scope, map);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_trigger.html", category = R.string.docs_script_category_triggers_automation, arguments = { "triggerId" })
    public NativeObject fetchTrigger(String id)
    {
        Map<String, Object> trigger = super.fetchTriggerMap(id);

        if (trigger != null)
            return JavaScriptEngine.mapToNative(this._jsContext, this._scope, trigger);

        return null;
    }

    // TODO: Document API...
    public NativeObject models()
    {
        Map<String, Object> modelMap = ModelManager.getInstance(this._context).models(this._context);

        return JavaScriptEngine.mapToNative(this._jsContext, this._scope, modelMap);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_readings.html", category = R.string.docs_script_category_data_collection, arguments = { })
    public NativeObject readings()
    {
        Map<String, Object> readings = ModelManager.getInstance(this._context).readings(this._context);

        return JavaScriptEngine.mapToNative(this._jsContext, this._scope, readings);
    }

    // TODO: Document API...
    public NativeObject predictions()
    {
        Map<String, Object> predictions = ModelManager.getInstance(this._context).predictions(this._context);

        return JavaScriptEngine.mapToNative(this._jsContext, this._scope, predictions);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_fetch_labels.html", category = R.string.docs_script_category_data_collection, arguments = { "instructions", "labels" })
    public void fetchLabels(String appContext, String instructions, final NativeObject labels)
    {
        this.fetchLabelsInterface(appContext, instructions, JavaScriptEngine.nativeToMap(labels));
    }

    @Override
    @ScriptingEngineMethod(language = "JavaScript")
    public NativeObject fetchWidget(String identifier)
    {
        Map<String, Object> map = super.fetchWidget(identifier);

        return JavaScriptEngine.mapToNative(this._jsContext, this._scope, map);
    }

    @Override
    @ScriptingEngineMethod(language = "JavaScript")
    public NativeArray widgets()
    {
        List<String> widgets = super.widgets();

        String[] values = new String[widgets.size()];

        for (int i = 0; i < widgets.size(); i++)
        {
            values[i] = widgets.get(i);
        }

        return new NativeArray(values);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_update_probe.html", category = R.string.docs_script_category_data_collection, arguments = { "parameters" })
    public boolean updateProbe(NativeObject params)
    {
        Map<String, Object> values = JavaScriptEngine.nativeToMap(params);

        return super.updateProbeConfig(values);
    }

    @ScriptingEngineMethod(language = "All", assetPath = "all_probe_config.html", category = R.string.docs_script_category_data_collection, arguments = { "probeName" })
    public NativeObject probeConfig(String name)
    {
        Map<String, Object> map = this.probeConfigMap(name);

        return JavaScriptEngine.mapToNative(this._jsContext, this._scope, map);
    }
}
