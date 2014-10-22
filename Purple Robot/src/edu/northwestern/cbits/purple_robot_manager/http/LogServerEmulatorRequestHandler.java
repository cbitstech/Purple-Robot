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

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;

public class LogServerEmulatorRequestHandler implements HttpRequestHandler {
    public static final String LOG_COUNT = "edu.northwestern.cbits.purple_robot_manager.logging.LogManager.LOG_COUNT";

    private Context _context = null;

    public LogServerEmulatorRequestHandler(Context context) {
        super();

        this._context = context;
    }

    @SuppressWarnings("deprecation")
    public void handle(HttpRequest request, HttpResponse response,
            HttpContext argument) throws HttpException, IOException {
        response.setStatusCode(HttpStatus.SC_OK);

        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest enclosingRequest = (HttpEntityEnclosingRequest) request;

            HttpEntity entity = enclosingRequest.getEntity();

            String entityString = EntityUtils.toString(entity);

            Uri u = Uri.parse("http://localhost/?" + entityString);

            JSONObject arguments = null;

            String jsonArg = URLDecoder.decode(entityString.substring(5));

            try {
                try {
                    jsonArg = URLDecoder.decode(u.getQueryParameter("json"),
                            "UTF-16");

                    arguments = new JSONObject(jsonArg);
                } catch (JSONException e) {
                    jsonArg = URLDecoder.decode(u.getQueryParameter("json"),
                            "UTF-16");

                    arguments = new JSONObject(jsonArg);
                }
            } catch (JSONException e) {
                LogManager.getInstance(this._context).logException(e);

                response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

                StringEntity body = new StringEntity(e.toString());
                body.setContentType("text/plain");

                response.setEntity(body);

                return;
            } catch (NullPointerException e) {
                LogManager.getInstance(this._context).logException(e);

                response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

                StringEntity body = new StringEntity(e.toString());
                body.setContentType("text/plain");

                response.setEntity(body);

                return;
            }

            if (arguments != null) {
                try {
                    if ("test_event".equals(arguments.get("event_type"))) {
                        SharedPreferences prefs = PreferenceManager
                                .getDefaultSharedPreferences(this._context);

                        int count = prefs.getInt(
                                LogServerEmulatorRequestHandler.LOG_COUNT, 0);

                        Editor e = prefs.edit();
                        e.putInt(LogServerEmulatorRequestHandler.LOG_COUNT,
                                count + 1);
                        e.commit();

                        StringEntity body = new StringEntity(
                                arguments.toString(2));
                        body.setContentType("application/json");

                        response.setEntity(body);
                    } else {
                        response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

                        StringEntity body = new StringEntity(
                                this._context
                                        .getString(R.string.error_only_logs_test));
                        body.setContentType("text/plain");

                        response.setEntity(body);
                    }
                } catch (JSONException e) {
                    response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

                    StringEntity body = new StringEntity(e.toString());
                    body.setContentType("text/plain");

                    response.setEntity(body);
                }
            }
        }
    }
}
