package edu.northwestern.cbits.purple_robot_manager.scripting;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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

    public Object runScript(String script) throws EvaluatorException, EcmaError
    {
        return this.runScript(script, null, null);
    }

    @SuppressWarnings("unchecked")
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

    public boolean log(String event, NativeObject params)
    {
        if (params != null)
            return LogManager.getInstance(this._context).log(event, JavaScriptEngine.nativeToMap(params));

        return LogManager.getInstance(this._context).log(event, new HashMap<String, Object>());
    }

    @SuppressWarnings("resource")
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

    public void updateWidget(final NativeObject parameters)
    {
        this.updateWidget(JavaScriptEngine.nativeToMap(parameters));
    }

    public boolean broadcastIntent(final String action, final NativeObject extras)
    {
        return this.broadcastIntent(action, JavaScriptEngine.nativeToMap(extras));
    }

    public boolean updateWidget(final String title, final String message, final String applicationName, final NativeObject launchParams, final String script)
    {
        return this.updateWidget(title, message, applicationName, JavaScriptEngine.nativeToMap(launchParams), script);
    }

    public boolean launchApplication(String applicationName, final NativeObject launchParams, final String script)
    {
        return this.launchApplication(applicationName, JavaScriptEngine.nativeToMap(launchParams), script);
    }

    public boolean showApplicationLaunchNotification(String title, String message, String applicationName, long displayWhen, boolean persistent, final NativeObject launchParams, final String script)
    {
        return this.showApplicationLaunchNotification(title, message, applicationName, displayWhen, persistent, JavaScriptEngine.nativeToMap(launchParams), script);
    }

    public boolean showApplicationLaunchNotification(String title, String message, String applicationName, long displayWhen, final NativeObject launchParams, final String script)
    {
        return this.showApplicationLaunchNotification(title, message, applicationName, displayWhen, JavaScriptEngine.nativeToMap(launchParams), script);
    }

    public boolean updateTrigger(String triggerId, NativeObject nativeJson)
    {
        return this.updateTrigger(triggerId, JavaScriptEngine.nativeToMap(nativeJson));
    }

    public void emitReading(String name, Object value)
    {
        double now = System.currentTimeMillis();

        Bundle bundle = new Bundle();
        bundle.putString("PROBE", name);
        bundle.putDouble("TIMESTAMP", now / 1000);

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
            Log.e("PRM", "JS PLUGIN GOT UNKNOWN VALUE " + value);

            if (value != null)
                Log.e("PRM", "JS PLUGIN GOT UNKNOWN CLASS " + value.getClass());
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

            params.put(e.getKey().toString(), value);
        }

        return params;
    }

    @SuppressWarnings("unchecked")
    private static NativeObject mapToNative(Context context, Scriptable scope, Map<String, Object> map)
    {
        NativeObject obj = (NativeObject) context.newObject(scope);

        for (String key : map.keySet())
        {
            Object value = map.get(key);

            if (value instanceof Map)
                value = JavaScriptEngine.mapToNative(context, scope, (Map<String, Object>) value);

            obj.put(key, obj, value);
        }

        return obj;
    }

    public String fetchConfig()
    {
        JSONConfigFile config = new JSONConfigFile(this._context);

        return config.toString();
    }

    public boolean updateConfig(NativeObject nativeObj)
    {
        Map<String, Object> paramsMap = JavaScriptEngine.nativeToMap(nativeObj);

        return super.updateConfig(paramsMap);
    }

    @Override
    public NativeArray fetchNamespaces()
    {
        List<String> namespaces = super.fetchNamespaces();

        String[] values = new String[namespaces.size()];

        for (int i = 0; i < namespaces.size(); i++)
        {
            values[i] = namespaces.get(i);
        }

        return new NativeArray(values);
    }

    @Override
    public NativeArray fetchTriggerIds()
    {
        List<String> triggerIds = super.fetchTriggerIds();

        String[] values = new String[triggerIds.size()];

        for (int i = 0; i < triggerIds.size(); i++)
        {
            values[i] = triggerIds.get(i);
        }

        return new NativeArray(values);
    }

    @Override
    public NativeArray fetchSnapshotIds()
    {
        List<String> snapshotIds = super.fetchSnapshotIds();

        String[] values = new String[snapshotIds.size()];

        for (int i = 0; i < snapshotIds.size(); i++)
        {
            values[i] = snapshotIds.get(i);
        }

        return new NativeArray(values);
    }

    @Override
    public NativeObject fetchSnapshot(String timestamp)
    {
        Map<String, Object> snapshot = super.fetchSnapshot(timestamp);

        if (snapshot != null)
            return JavaScriptEngine.mapToNative(this._jsContext, this._scope, snapshot);

        return null;
    }

    public NativeObject fetchNamespace(String namespace)
    {
        Map<String, Object> map = super.fetchNamespaceMap(namespace);

        return JavaScriptEngine.mapToNative(this._jsContext, this._scope, map);
    }

    @Override
    public NativeObject fetchTrigger(String id)
    {
        Map<String, Object> trigger = super.fetchTrigger(id);

        if (trigger != null)
            return JavaScriptEngine.mapToNative(this._jsContext, this._scope, trigger);

        return null;
    }

    public NativeObject models()
    {
        Map<String, Object> modelMap = ModelManager.getInstance(this._context).models(this._context);

        return JavaScriptEngine.mapToNative(this._jsContext, this._scope, modelMap);
    }

    public NativeObject readings()
    {
        Map<String, Object> readings = ModelManager.getInstance(this._context).readings(this._context);

        return JavaScriptEngine.mapToNative(this._jsContext, this._scope, readings);
    }

    public NativeObject predictions()
    {
        Map<String, Object> predictions = ModelManager.getInstance(this._context).predictions(this._context);

        return JavaScriptEngine.mapToNative(this._jsContext, this._scope, predictions);
    }

    @Override
    public NativeObject fetchWidget(String identifier)
    {
        Map<String, Object> map = super.fetchWidget(identifier);

        return JavaScriptEngine.mapToNative(this._jsContext, this._scope, map);
    }

    @Override
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

    public boolean updateProbe(NativeObject params)
    {
        Map<String, Object> values = JavaScriptEngine.nativeToMap(params);

        return super.updateProbe(values);
    }
}
