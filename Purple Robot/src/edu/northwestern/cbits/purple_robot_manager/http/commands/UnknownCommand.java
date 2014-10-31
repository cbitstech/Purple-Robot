package edu.northwestern.cbits.purple_robot_manager.http.commands;

import org.json.JSONException;
import org.json.JSONObject;

import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

import android.content.Context;

public class UnknownCommand extends JSONCommand
{
    public UnknownCommand(JSONObject arguments, Context context)
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
                result.put(JSONCommand.STATUS, JSONCommand.STATUS_ERROR);
                result.put(JSONCommand.MESSAGE, "Unknown command '" + this._arguments.getString(JSONCommand.COMMAND)
                        + "'...");
            }
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return result;
    }
}
