package edu.northwestern.cbits.purple_robot_manager.http;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.preference.PreferenceManager;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Scanner;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.RobotContentProvider;
import edu.northwestern.cbits.purple_robot_manager.config.JSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.config.SchemeConfigFile;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

public class ProbesHelpRequestHandler implements HttpRequestHandler
{
    private Context _context = null;

    public ProbesHelpRequestHandler(Context context)
    {
        super();

        this._context = context;
    }

    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException
    {
        if (BasicAuthHelper.isAuthenticated(request) == false)
        {
            BasicAuthHelper.unauthedResponse(response);

            return;
        }

        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);

        if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST"))
            throw new MethodNotSupportedException(method + " method not supported");

        String target = request.getRequestLine().getUri();

        String[] components = target.split("/");

        String path = null;

        if (components.length > 3)
            path = components[3];

        response.setStatusCode(HttpStatus.SC_OK);
        response.setHeader("Access-Control-Allow-Origin", "*");

        final byte[] content = this.fetchContent(this._context, path);

        EntityTemplate body = new EntityTemplate(new ContentProducer()
        {
            public void writeTo(OutputStream out) throws IOException
            {
                ByteArrayInputStream in = new ByteArrayInputStream(content);

                byte[] b = new byte[1024];
                int read = 0;

                while ((read = in.read(b, 0, b.length)) != -1)
                    out.write(b, 0, read);

                out.close();
                in.close();
            }
        });

        response.setEntity(body);
    }

    private byte[] fetchContent(Context context, String path)
    {
        if (path == null)
        {
            try
            {
                JSONArray probes = new JSONArray();

                for (Probe probeObj : ProbeManager.allProbes(context))
                {
                    JSONObject probeDef = new JSONObject();
                    probeDef.put("name", probeObj.name(context));
                    probeDef.put("title", probeObj.title(context));
                    probeDef.put("summary", probeObj.summary(context));
                    probeDef.put("category", probeObj.probeCategory(context));

                    String assetPath = probeObj.assetPath(context);

                    if (assetPath != null)
                        probeDef.put("path", assetPath);

                    probes.put(probeDef);
                }

                AssetManager am = this._context.getAssets();

                InputStream in = am.open("embedded_website/docs/probes_template.html");

                // http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
                Scanner s = new Scanner(in).useDelimiter("\\A");

                String content = "";

                if (s.hasNext())
                    content = s.next();

                content = content.replace("{{ PROBES }}", probes.toString(2));

                return content.getBytes(Charset.forName("UTF-8"));
            }
            catch (IOException | JSONException e)
            {
                e.printStackTrace();
            }

            return "404 ERROR".getBytes(Charset.forName("UTF-8"));
        }
        else
        {
            Probe probe = null;

            for (Probe probeObj : ProbeManager.allProbes(context))
            {
                if (path.equals(probeObj.assetPath(context)))
                    probe = probeObj;
            }

            if (probe != null)
            {
                try
                {
                    AssetManager am = this._context.getAssets();

                    InputStream in = am.open("embedded_website/docs/probe_template.html");

                    // http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
                    Scanner s = new Scanner(in).useDelimiter("\\A");

                    String content = "";

                    if (s.hasNext())
                        content = s.next();

                    s.close();

                    in = am.open("embedded_website/docs/probes/" + path);

                    s = new Scanner(in).useDelimiter("\\A");

                    String pageContent = "";

                    if (s.hasNext())
                        pageContent = s.next();

                    s.close();

                    content = content.replace("{{ PROBE_DOCUMENTATION }}", pageContent);
                    content = content.replace("{{ PROBE_NAME }}", probe.title(context));
                    content = content.replace("{{ PROBE_SUMMARY }}", probe.summary(context));
                    content = content.replace("{{ PROBE_PARAMETERS }}", probe.fetchSettings(context).toString(2));
                    content = content.replace("{{ SCHEME_CONFIG }}", SchemeConfigFile.probeConfig(probe.configuration(context)).toString());
                    content = content.replace("{{ JAVASCRIPT_CONFIG }}", JSONConfigFile.jsonFromMap(probe.configuration(context)).toString(2));

                    String where = "source = ?";
                    String[] args = { probe.name(context)};

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

                    boolean isAuthed = prefs.getString(LocalHttpServer.BUILTIN_HTTP_SERVER_PASSWORD, LocalHttpServer.BUILTIN_HTTP_SERVER_PASSWORD_DEFAULT).length() > 0;

                    if (isAuthed)
                    {
                        Cursor c = context.getContentResolver().query(RobotContentProvider.RECENT_PROBE_VALUES, null, where, args, "recorded DESC");

                        if (c.moveToNext())
                        {
                            JSONObject reading = new JSONObject(c.getString(c.getColumnIndex("value")));

                            content = content.replace("{{ LATEST_READING }}", reading.toString(2));
                        }
                        else
                            content = content.replace("{{ LATEST_READING }}", context.getString(R.string.error_no_latest_probe_value));

                        c.close();
                    }
                    else
                        content = content.replace("{{ LATEST_READING }}", context.getString(R.string.error_auth_required));

                    return content.getBytes(Charset.forName("UTF-8"));
                }
                catch (IOException | JSONException e)
                {
                    e.printStackTrace();
                }
            }
        }

        return "404 ERROR".getBytes(Charset.forName("UTF-8"));
    }
}
