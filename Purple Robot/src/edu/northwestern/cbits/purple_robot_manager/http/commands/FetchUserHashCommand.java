package edu.northwestern.cbits.purple_robot_manager.http.commands;

import org.json.JSONException;
import org.json.JSONObject;

import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

import android.content.Context;

public class FetchUserHashCommand extends JSONCommand
{
    public static final String COMMAND_NAME = "fetch_user_hash";

    public FetchUserHashCommand(JSONObject arguments, Context context)
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
                result.put(JSONCommand.PAYLOAD, EncryptionManager.getInstance().getUserHash(this._context));
            }
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return result;
    }
}
