package edu.northwestern.cbits.purple_robot_manager.http.commands;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class UnknownCommand extends JSONCommand 
{
	public UnknownCommand(JSONObject arguments, Context context) 
	{
		super(arguments, context);
	}

	public JSONObject execute() 
	{
		JSONObject result = super.execute();
		
		try 
		{
			if (JSONCommand.STATUS_OK.equals(result.get(JSONCommand.STATUS)))
			{
				result.put(JSONCommand.STATUS, JSONCommand.STATUS_ERROR);
				result.put(JSONCommand.MESSAGE, "Unknown command '" + this._arguments.getString(JSONCommand.COMMAND) + "'...");
			}		
		}
		catch (JSONException e) 
		{
			e.printStackTrace();
		}
		
		return result;
	}
}
