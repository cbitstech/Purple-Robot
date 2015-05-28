package edu.northwestern.cbits.purple_robot_manager.http;

import java.io.IOException;

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
import android.media.MediaPlayer;
import android.net.Uri;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.snapshots.SnapshotManager;

public class SnapshotAudioRequestHandler implements HttpRequestHandler
{
    private Context _context = null;

    public SnapshotAudioRequestHandler(Context context)
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

        if (request instanceof BasicHttpRequest)
        {
            Uri u = Uri.parse(request.getRequestLine().getUri());

            long timestamp = Long.parseLong(u.getQueryParameter("timestamp"));

            JSONObject snapshot = SnapshotManager.getInstance(this._context).jsonForTime(timestamp, true);

            if (snapshot.has("audio"))
            {

                try
                {
                    MediaPlayer mp = new MediaPlayer();

                    mp.setDataSource(snapshot.getString("audio"));
                    mp.prepare();
                    mp.start();

                    Thread.sleep(500);

                    while (mp.isPlaying())
                        Thread.sleep(250);

                    JSONObject status = new JSONObject();
                    status.put("status", "success");

                    StringEntity body = new StringEntity(status.toString(2));
                    body.setContentType("application/json");

                    response.setEntity(body);

                    return;
                }
                catch (IllegalArgumentException | InterruptedException | JSONException | IllegalStateException | SecurityException e)
                {
                    LogManager.getInstance(this._context).logException(e);
                }

                try
                {
                    JSONObject status = new JSONObject();
                    status.put("status", "error");

                    StringEntity body = new StringEntity(status.toString(2));
                    body.setContentType("application/json");

                    response.setEntity(body);

                    return;
                }
                catch (JSONException e)
                {
                    LogManager.getInstance(this._context).logException(e);
                }
            }

            try
            {
                JSONObject status = new JSONObject();
                status.put("status", "no-audio-found");

                StringEntity body = new StringEntity(status.toString(2));
                body.setContentType("application/json");

                response.setEntity(body);
            }
            catch (JSONException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }
        }
    }
}
