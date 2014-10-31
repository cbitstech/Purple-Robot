package edu.northwestern.cbits.purple_robot_manager.http.commands;

import org.json.JSONException;
import org.json.JSONObject;

import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

import android.content.Context;

public abstract class JSONCommand
{
    public static final String STATUS = "status";
    public static final String STATUS_OK = "ok";
    public static final String STATUS_ERROR = "error";

    public static final String MESSAGE = "message";
    public static final String COMMAND = "command";
    public static final String PAYLOAD = "payload";

    protected Context _context = null;
    protected JSONObject _arguments = null;

    public JSONCommand(JSONObject arguments, Context context)
    {
        this._context = context;
        this._arguments = arguments;
    }

    public JSONObject execute(Context context)
    {
        JSONObject json = new JSONObject();

        try
        {
            json.put(JSONCommand.COMMAND, this._arguments.getString(JSONCommand.COMMAND));
            json.put(JSONCommand.STATUS, JSONCommand.STATUS_OK);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return json;
    }
}
