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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;

public class JsonStoreRequestHandler implements HttpRequestHandler
{
    private Context _context = null;

    public JsonStoreRequestHandler(Context context)
    {
        super();

        this._context = context;
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

        if (request instanceof HttpEntityEnclosingRequest)
        {
            HttpEntityEnclosingRequest enclosingRequest = (HttpEntityEnclosingRequest) request;

            HttpEntity entity = enclosingRequest.getEntity();

            String entityString = EntityUtils.toString(entity);

            Uri u = Uri.parse("http://localhost/?" + entityString);

            JSONObject arguments = null;

            try
            {
                arguments = new JSONObject(URLDecoder.decode(u.getQueryParameter("json"), "UTF-8"));
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

            if (arguments != null)
            {
                try
                {
                    JavaScriptEngine engine = new JavaScriptEngine(this._context);

                    String action = arguments.getString("action");

                    if ("fetch".equals(action))
                    {
                        if (arguments.has("keys"))
                        {
                            JSONObject result = new JSONObject();
                            result.put("status", "success");

                            JSONArray keys = arguments.getJSONArray("keys");

                            JSONObject values = new JSONObject();

                            for (int i = 0; i < keys.length(); i++)
                            {
                                String key = keys.getString(i);

                                String value = engine.fetchString(key);

                                values.put(key, value);
                            }

                            result.put("values", values);

                            StringEntity body = new StringEntity(result.toString(2));
                            body.setContentType("application/json");

                            response.setEntity(body);

                            return;
                        }
                    }
                    else if ("put".equals(action))
                    {
                        JSONArray names = arguments.names();

                        for (int i = 0; i < names.length(); i++)
                        {
                            String name = names.getString(i);

                            if ("action".equals(name) == false)
                            {
                                String value = arguments.getString(name);

                                if (value != null)
                                    engine.persistString(name, value);
                            }
                        }

                        JSONObject result = new JSONObject();
                        result.put("status", "success");

                        StringEntity body = new StringEntity(result.toString(2));
                        body.setContentType("application/json");

                        response.setEntity(body);

                        return;
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
            }
        }

        response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

        StringEntity body = new StringEntity(this._context.getString(R.string.error_malformed_request));
        body.setContentType("text/plain");

        response.setEntity(body);
    }
}
