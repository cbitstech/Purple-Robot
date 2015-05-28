package edu.northwestern.cbits.purple_robot_manager.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.snapshots.SnapshotManager;

public class SnapshotRequestHandler implements HttpRequestHandler
{
    private Context _context = null;

    public SnapshotRequestHandler(Context context)
    {
        super();

        this._context = context;
    }

    @SuppressWarnings("resource")
    public void handle(HttpRequest request, HttpResponse response, HttpContext argument) throws HttpException,
            IOException
    {
        if (BasicAuthHelper.isAuthenticated(request) == false)
        {
            BasicAuthHelper.unauthedResponse(response);

            return;
        }

        response.setStatusCode(HttpStatus.SC_OK);

        if (request instanceof BasicHttpRequest)
        {
            Uri u = Uri.parse(request.getRequestLine().getUri());

            try
            {
                long timestamp = Long.parseLong(u.getQueryParameter("timestamp"));

                JSONObject snapshot = SnapshotManager.getInstance(this._context).jsonForTime(timestamp, true);

                AssetManager am = this._context.getAssets();

                InputStream jsStream = am.open("embedded_website/snapshot.html");

                Scanner s = new Scanner(jsStream).useDelimiter("\\A");

                String html = "";

                if (s.hasNext())
                {
                    html = s.next();

                    html = html.replace("SNAPSHOT_PLACEHOLDER", snapshot.toString(2));

                    StringEntity body = new StringEntity(html);
                    body.setContentType("text/html");

                    response.setEntity(body);
                }
            }
            catch (IOException | JSONException e)
            {
                LogManager.getInstance(this._context).logException(e);

                response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

                StringEntity body = new StringEntity(e.toString());
                body.setContentType("text/plain");

                response.setEntity(body);
            }
            catch (NumberFormatException e)
            {
                LogManager.getInstance(this._context).logException(e);

                response.setStatusCode(HttpStatus.SC_NOT_FOUND);

                StringEntity body = new StringEntity(this._context.getString(R.string.message_snapshot_not_found));
                body.setContentType("text/plain");

                response.setEntity(body);
            }
        }
    }
}
