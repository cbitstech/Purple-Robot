package edu.northwestern.cbits.purple_robot_manager.http;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.json.JSONArray;
import org.json.JSONException;

import edu.northwestern.cbits.purple_robot_manager.snapshots.SnapshotManager;

import android.content.Context;

public class SnapshotJsonRequestHandler implements HttpRequestHandler
{
    private Context _context = null;

    public SnapshotJsonRequestHandler(Context context)
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

        JSONArray snapshots = new JSONArray();

        SnapshotManager manager = SnapshotManager.getInstance(this._context);

        long[] timestamps = manager.snapshotTimes();

        for (long timestamp : timestamps)
        {
            snapshots.put(manager.jsonForTime(timestamp, false));
        }

        try
        {
            StringEntity body = new StringEntity(snapshots.toString(2));
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
