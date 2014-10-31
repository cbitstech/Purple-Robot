package edu.northwestern.cbits.purple_robot_manager.http.commands;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;

public class FetchStringCommand extends JSONCommand
{
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String ENCRYPTED = "encrypted";
    public static final String COMMAND_NAME = "fetch_string";
    public static final String NOT_FOUND = "not_found";

    public FetchStringCommand(JSONObject arguments, Context context)
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
                String key = this._arguments.getString(FetchStringCommand.KEY);

                result.put(FetchStringCommand.KEY, key);

                JavaScriptEngine engine = new JavaScriptEngine(this._context);

                boolean doEncrypt = false;

                if (this._arguments.has(FetchStringCommand.ENCRYPTED))
                    doEncrypt = this._arguments.getBoolean(FetchStringCommand.ENCRYPTED);

                String resultString = null;

                if (doEncrypt)
                    resultString = engine.fetchEncryptedString(key);
                else
                    resultString = engine.fetchString(key);

                if (resultString != null)
                    result.put(FetchStringCommand.VALUE, resultString);
                else
                    result.put(JSONCommand.STATUS, FetchStringCommand.NOT_FOUND);
            }
        }
        catch (JSONException e)
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
