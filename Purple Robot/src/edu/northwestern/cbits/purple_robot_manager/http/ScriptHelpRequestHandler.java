package edu.northwestern.cbits.purple_robot_manager.http;

import android.content.Context;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import edu.northwestern.cbits.purple_robot_manager.output.BootstrapSiteExporter;

public class ScriptHelpRequestHandler implements HttpRequestHandler
{
    private Context _context = null;

    public ScriptHelpRequestHandler(Context context)
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

        String language = components[3];

        String page = null;

        if (language.length() == 0)
            language = "All";

        if (components.length > 4)
            page = components[4];

        if (page == null && target.endsWith("/") == false)
        {
            response.setStatusCode(HttpStatus.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", target + "/");
        }
        else {
            response.setStatusCode(HttpStatus.SC_OK);
            response.setHeader("Access-Control-Allow-Origin", "*");

            final byte[] content = BootstrapSiteExporter.fetchContent(this._context, language, page, false, BootstrapSiteExporter.TEMPLATE_TYPE_BOOTSTRAP);

            EntityTemplate body = new EntityTemplate(new ContentProducer() {
                public void writeTo(OutputStream out) throws IOException {
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
    }
}
