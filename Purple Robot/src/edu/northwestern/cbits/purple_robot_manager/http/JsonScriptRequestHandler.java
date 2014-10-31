package edu.northwestern.cbits.purple_robot_manager.http;

import java.io.IOException;
import java.net.URLDecoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;

import edu.northwestern.cbits.purple_robot_manager.http.commands.ExecuteSchemeCommand;
import edu.northwestern.cbits.purple_robot_manager.http.commands.ExecuteJavaScriptCommand;
import edu.northwestern.cbits.purple_robot_manager.http.commands.FetchStringCommand;
import edu.northwestern.cbits.purple_robot_manager.http.commands.FetchUserHashCommand;
import edu.northwestern.cbits.purple_robot_manager.http.commands.JSONCommand;
import edu.northwestern.cbits.purple_robot_manager.http.commands.PersistStringCommand;
import edu.northwestern.cbits.purple_robot_manager.http.commands.PingCommand;
import edu.northwestern.cbits.purple_robot_manager.http.commands.UnknownCommand;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

public class JsonScriptRequestHandler implements HttpRequestHandler
{
    private Context _context = null;

    public JsonScriptRequestHandler(Context context)
    {
        super();

        this._context = context;
    }

    @SuppressWarnings("deprecation")
    public void handle(HttpRequest request, HttpResponse response, HttpContext argument) throws HttpException,
            IOException
    {
        response.setStatusCode(HttpStatus.SC_OK);

        if (request instanceof HttpEntityEnclosingRequest)
        {
            HttpEntityEnclosingRequest enclosingRequest = (HttpEntityEnclosingRequest) request;

            HttpEntity entity = enclosingRequest.getEntity();

            String entityString = EntityUtils.toString(entity);

            Uri u = Uri.parse("http://localhost/?" + entityString);

            JSONObject arguments = null;

            String jsonArg = URLDecoder.decode(entityString.substring(5)); // u.getQueryParameter("json");

            try
            {
                try
                {
                    arguments = new JSONObject(jsonArg);
                }
                catch (JSONException e)
                {
                    try
                    {
                        jsonArg = URLDecoder.decode(u.getQueryParameter("json"), "UTF-16");
                        arguments = new JSONObject(jsonArg);
                    }
                    catch (JSONException ex)
                    {
                        jsonArg = URLDecoder.decode(u.getQueryParameter("json"), "UTF-8");
                        arguments = new JSONObject(jsonArg);
                    }
                }
                catch (IllegalArgumentException e)
                {
                    try
                    {
                        jsonArg = URLDecoder.decode(u.getQueryParameter("json"), "UTF-16");
                        arguments = new JSONObject(jsonArg);
                    }
                    catch (JSONException ex)
                    {
                        jsonArg = URLDecoder.decode(u.getQueryParameter("json"), "UTF-8");
                        arguments = new JSONObject(jsonArg);
                    }
                }
            }
            catch (JSONException e)
            {
                LogManager.getInstance(this._context).logException(e);

                response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

                StringEntity body = new StringEntity(e.toString());
                body.setContentType("text/plain");

                response.setEntity(body);

                return;
            }
            catch (NullPointerException e)
            {
                LogManager.getInstance(this._context).logException(e);

                response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

                StringEntity body = new StringEntity(e.toString());
                body.setContentType("text/plain");

                response.setEntity(body);

                return;
            }

            if (arguments != null)
            {
                try
                {
                    JSONCommand command = JsonScriptRequestHandler.commandForJson(arguments, this._context);

                    JSONObject result = command.execute(this._context);

                    StringEntity body = new StringEntity(result.toString(2));
                    body.setContentType("application/json");

                    response.setEntity(body);
                }
                catch (JSONException e)
                {
                    response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

                    StringEntity body = new StringEntity(e.toString());
                    body.setContentType("text/plain");

                    response.setEntity(body);
                }
            }
        }
    }

    public static JSONCommand commandForJson(JSONObject arguments, Context context)
    {
        try
        {
            if (PingCommand.COMMAND_NAME.equals(arguments.get(JSONCommand.COMMAND)))
                return new PingCommand(arguments, context);
            else if (ExecuteJavaScriptCommand.COMMAND_NAME.equals(arguments.get(JSONCommand.COMMAND)))
                return new ExecuteJavaScriptCommand(arguments, context);
            else if (ExecuteSchemeCommand.COMMAND_NAME.equals(arguments.get(JSONCommand.COMMAND)))
                return new ExecuteSchemeCommand(arguments, context);
            else if (FetchUserHashCommand.COMMAND_NAME.equals(arguments.get(JSONCommand.COMMAND)))
                return new FetchUserHashCommand(arguments, context);
            else if (PersistStringCommand.COMMAND_NAME.equals(arguments.get(JSONCommand.COMMAND)))
                return new PersistStringCommand(arguments, context);
            else if (FetchStringCommand.COMMAND_NAME.equals(arguments.get(JSONCommand.COMMAND)))
                return new FetchStringCommand(arguments, context);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return new UnknownCommand(arguments, context);
    }
}
