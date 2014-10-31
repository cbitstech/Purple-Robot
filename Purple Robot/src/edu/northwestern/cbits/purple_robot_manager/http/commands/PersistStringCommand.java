package edu.northwestern.cbits.purple_robot_manager.http.commands;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;

public class PersistStringCommand extends JSONCommand
{
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String ENCRYPTED = "encrypted";
    public static final String COMMAND_NAME = "persist_string";

    public PersistStringCommand(JSONObject arguments, Context context)
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
                String key = this._arguments.getString(PersistStringCommand.KEY);
                String value = this._arguments.getString(PersistStringCommand.VALUE);

                JavaScriptEngine engine = new JavaScriptEngine(this._context);

                boolean doEncrypt = false;

                if (this._arguments.has(PersistStringCommand.ENCRYPTED))
                    doEncrypt = this._arguments.getBoolean(PersistStringCommand.ENCRYPTED);

                if (doEncrypt)
                    engine.persistEncryptedString(key, value);
                else
                    engine.persistString(key, value);
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
