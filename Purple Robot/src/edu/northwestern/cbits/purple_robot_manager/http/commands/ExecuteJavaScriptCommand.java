package edu.northwestern.cbits.purple_robot_manager.http.commands;

import java.net.URLDecoder;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Undefined;

import android.content.Context;

import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;

public class ExecuteJavaScriptCommand extends JSONCommand
{
    public static final String COMMAND_NAME = "execute_script";

    public static final String SCRIPT = "script";

    public ExecuteJavaScriptCommand(JSONObject arguments, Context context)
    {
        super(arguments, context);
    }

    public JSONObject execute(Context context)
    {
        JSONObject result = super.execute(context);

        try
        {
            if (JSONCommand.STATUS_OK.equals(result.get(JSONCommand.STATUS)))
            {
                JavaScriptEngine engine = new JavaScriptEngine(this._context);

                Object o = null;

                String script = this._arguments.getString(ExecuteJavaScriptCommand.SCRIPT);

                try
                {
                    o = engine.runScript(script);
                }
                catch (EvaluatorException ee)
                {
                    script = URLDecoder.decode(script, "UTF-8");
                    o = engine.runScript(script);
                }

                if (!(o instanceof Undefined))
                {
                    if (o instanceof NativeJavaObject)
                    {
                        NativeJavaObject nativeObj = (NativeJavaObject) o;

                        o = nativeObj.unwrap();
                    }
                    else if (o instanceof NativeArray)
                    {
                        o = JavaScriptEngine.nativeToJson((NativeArray) o);
                    }
                    else if (o instanceof NativeObject)
                    {
                        o = JavaScriptEngine.nativeToJson((NativeObject) o);
                    }

                    result.put(JSONCommand.PAYLOAD, o);
                }
            }
        }
        catch (Exception e)
        {
            LogManager.getInstance(context).logException(e);

            try
            {
                result.put(JSONCommand.STATUS, JSONCommand.STATUS_ERROR);
                result.put(JSONCommand.MESSAGE, e.toString());
            }
            catch (JSONException ee)
            {
                LogManager.getInstance(context).logException(ee);
            }
        }

        return result;
    }
}
