package edu.northwestern.cbits.purple_robot_manager.http.commands;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.scripting.SchemeEngine;

public class ExecuteSchemeCommand extends JSONCommand 
{
	public static final String COMMAND_NAME = "execute_scheme";
	
	public static final String SOURCE = "source";
	
	public ExecuteSchemeCommand(JSONObject arguments, Context context) 
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
				String source = this._arguments.getString(ExecuteSchemeCommand.SOURCE);
				
				SchemeEngine engine = new SchemeEngine(this._context);
				
				Object o = engine.evaluateSource(source);

				if (o != null)
					result.put(JSONCommand.PAYLOAD, o);
			}
		}
		catch (Exception e) 
		{
			try 
			{
				result.put(JSONCommand.STATUS, JSONCommand.STATUS_ERROR);
				result.put(JSONCommand.MESSAGE, e.toString());
			}
			catch (JSONException ee) 
			{
				ee.printStackTrace();
			}
		}

		return result;
	}
}
