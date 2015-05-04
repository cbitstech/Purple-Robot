package edu.northwestern.cbits.purple_robot_manager.http;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.scripting.SchemeEngine;

public class JsonVariablesRequestHandler implements HttpRequestHandler
{
    private Context _context = null;

    public JsonVariablesRequestHandler(Context context)
    {
        super();

        this._context = context;
    }

    public static JSONObject fetchStoredValues(Context context)
    {
        JSONObject obj = new JSONObject();

        SchemeEngine engine = new SchemeEngine(context, null);

        for (String ns : engine.fetchNamespaceList())
        {
            JSONObject nsObject = new JSONObject();

            Map<String, Object> nsMap = engine.fetchNamespaceMap(ns);

            for (String key : nsMap.keySet())
            {
                try
                {
                    nsObject.put(key, nsMap.get(key));
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }

            try
            {
                obj.put(ns, nsObject);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }

        return obj;
    }

    public void handle(HttpRequest request, HttpResponse response, HttpContext argument) throws HttpException,
            IOException
    {
        if (BasicAuthHelper.isAuthenticated(request) == false)
        {
            BasicAuthHelper.unauthedResponse(response);

            return;
        }

        response.setStatusCode(HttpStatus.SC_OK);

        JSONObject obj = JsonVariablesRequestHandler.fetchStoredValues(this._context);

        try
        {
            StringEntity body = new StringEntity(obj.toString(2));
            body.setContentType("application/json");

            response.setEntity(body);
        }
        catch (JSONException e)
        {
            e.printStackTrace();

            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

            StringEntity body = new StringEntity(this._context.getString(R.string.error_malformed_request));
            body.setContentType("text/plain");

            response.setEntity(body);

        }
    }
}
