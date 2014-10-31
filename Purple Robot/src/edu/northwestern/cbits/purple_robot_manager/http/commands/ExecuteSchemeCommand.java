package edu.northwestern.cbits.purple_robot_manager.http.commands;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.SchemeEngine;

public class ExecuteSchemeCommand extends JSONCommand
{
    public static final String COMMAND_NAME = "execute_scheme";

    public static final String SOURCE = "source";

    public ExecuteSchemeCommand(JSONObject arguments, Context context)
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
                String source = this._arguments.getString(ExecuteSchemeCommand.SOURCE);

                SchemeEngine engine = new SchemeEngine(this._context, null);

                Object o = engine.evaluateSource(source);

                if (o != null)
                    result.put(JSONCommand.PAYLOAD, o);
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
