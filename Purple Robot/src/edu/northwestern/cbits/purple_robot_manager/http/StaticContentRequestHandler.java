package edu.northwestern.cbits.purple_robot_manager.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

import android.content.Context;

public class StaticContentRequestHandler implements HttpRequestHandler
{
    private static String WEB_PREFIX = "embedded_website";

    private Context _context = null;

    public StaticContentRequestHandler(Context context)
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

        if (target.trim().length() == 1)
            target = "/index.html";

        if (target.contains("?"))
            target = target.substring(0, target.indexOf("?"));

        if (request instanceof HttpEntityEnclosingRequest)
        {
            try
            {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                byte[] entityContent = EntityUtils.toByteArray(entity);
            }
            catch (IOException e)
            {
                LogManager.getInstance(this._context).logException(e);
            }
        }

        final String path = WEB_PREFIX + target;

        try
        {
            final InputStream in = this._context.getAssets().open(path);

            response.setStatusCode(HttpStatus.SC_OK);
            response.setHeader("Access-Control-Allow-Origin", "*");

            EntityTemplate body = new EntityTemplate(new ContentProducer()
            {
                public void writeTo(OutputStream out) throws IOException
                {
                    byte[] b = new byte[1024];
                    int read = 0;

                    while ((read = in.read(b, 0, b.length)) != -1)
                        out.write(b, 0, read);

                    out.close();
                    in.close();
                }
            });

            if (path.endsWith(".js"))
                body.setContentType("application/javascript");
            else if (path.endsWith(".html"))
                body.setContentType("text/html");
            else if (path.endsWith(".css"))
                body.setContentType("text/css");
            else
                body.setContentType("text/plain");

            response.setEntity(body);
        }
        catch (IOException e)
        {
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);

            try
            {
                final InputStream in = this._context.getAssets().open("embedded_website/404.html");

                EntityTemplate body = new EntityTemplate(new ContentProducer()
                {
                    public void writeTo(OutputStream out) throws IOException
                    {
                        byte[] b = new byte[1024];
                        int read = 0;

                        while ((read = in.read(b, 0, b.length)) != -1)
                            out.write(b, 0, read);

                        out.close();
                        in.close();
                    }
                });

                response.setEntity(body);

                body.setContentType("text/html; charset=UTF-8");
                response.setEntity(body);
            }
            catch (IOException e1)
            {
                LogManager.getInstance(this._context).logException(e1);
            }
        }
    }
}
