package edu.northwestern.cbits.purple_robot_manager.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.SchemeEngine;

public class HttpStoreRequestHandler implements HttpRequestHandler
{
    private Context _context = null;

    public HttpStoreRequestHandler(Context context)
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
        response.setHeader("Access-Control-Allow-Origin", "*");

        if (request instanceof HttpEntityEnclosingRequest)
        {
            SchemeEngine engine = new SchemeEngine(this._context, null);

            HttpEntityEnclosingRequest enclosingRequest = (HttpEntityEnclosingRequest) request;

            HttpEntity entity = enclosingRequest.getEntity();

            String entityString = EntityUtils.toString(entity);

            Uri u = Uri.parse("http://localhost/?" + entityString);

            HashMap<String, String> arguments = new HashMap<>();

            try
            {
                for (NameValuePair pair : URLEncodedUtils.parse(new URI(u.toString()), "UTF-8"))
                {
                    arguments.put(pair.getName(), pair.getValue());
                }
            }
            catch (URISyntaxException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }

            if (arguments.size() > 0)
            {
                boolean encrypt = false;

                if (arguments.containsKey("encrypt") && "true".equals(arguments.get("encrypt")))
                    encrypt = true;

                String namespace = null;

                if (arguments.containsKey("namespace"))
                {
                    namespace = arguments.get("namespace").trim();

                    if (namespace.length() < 1)
                        namespace = null;
                }

                for (String key : arguments.keySet())
                {
                    if ("encrypt".equalsIgnoreCase(key) || "namespace".equalsIgnoreCase(key))
                    {
                        // Ignore these values...
                    }
                    else
                    {
                        String value = arguments.get(key).trim();

                        if (value.length() > 0)
                        {
                            if (encrypt)
                            {
                                if (namespace != null)
                                    engine.persistEncryptedString(namespace, key, value);
                                else
                                    engine.persistEncryptedString(key, value);
                            }
                            else
                            {
                                if (namespace != null)
                                    engine.persistString(namespace, key, value);
                                else
                                    engine.persistString(key, value);
                            }
                        }
                        else
                        {
                            if (encrypt)
                            {
                                if (namespace != null)
                                    engine.persistEncryptedString(namespace, key, null);
                                else
                                    engine.persistEncryptedString(key, null);
                            }
                            else
                            {
                                if (namespace != null)
                                    engine.persistString(namespace, key, null);
                                else
                                    engine.persistString(key, null);
                            }
                        }
                    }
                }
            }

            JSONObject obj = JsonVariablesRequestHandler.fetchStoredValues(this._context);

            try
            {
                StringEntity body = new StringEntity(obj.toString(2));
                body.setContentType("application/json");

                response.setEntity(body);

                return;
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

        response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

        StringEntity body = new StringEntity(this._context.getString(R.string.error_malformed_request));
        body.setContentType("text/plain");

        response.setEntity(body);
    }
}
