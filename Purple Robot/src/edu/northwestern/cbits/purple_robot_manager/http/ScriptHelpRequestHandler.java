package edu.northwestern.cbits.purple_robot_manager.http;

import android.content.Context;
import android.content.res.AssetManager;

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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;

import edu.northwestern.cbits.purple_robot_manager.annotation.ScriptingEngineMethod;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.scripting.SchemeEngine;

public class ScriptHelpRequestHandler implements HttpRequestHandler
{
    private static final String LANGUAGE_ALL = "All";
    private static final String METHOD_NAME = "name";
    private static final String METHOD_LANGUAGE = "language";
    private static final String METHOD_CATEGORY = "category";
    private static final String METHOD_ASSET_PATH = "path";
    private static final String METHOD_FULL_NAME = "full_name";

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

        response.setStatusCode(HttpStatus.SC_OK);
        response.setHeader("Access-Control-Allow-Origin", "*");

        final byte[] content = this.fetchContent(this._context, language, page);

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

    /* TODO: Move some of this logic into the BaseScriptEngine class? */
    private byte[] fetchContent(Context context, String language, String page)
    {
        Class[] scriptingClasses = { JavaScriptEngine.class, SchemeEngine.class };

        ArrayList<String> languages = new ArrayList<String>();
        languages.add(ScriptHelpRequestHandler.LANGUAGE_ALL.toLowerCase());

        if (languages.contains(language) == false)
            languages.add(language.toLowerCase());

        if (page == null || page.trim().length() == 0)
        {
            HashSet<String> included = new HashSet<String>();

            try
            {
                JSONArray declaredMethods = new JSONArray();

                for (Class classObj : scriptingClasses)
                {
                    Method[] methods = classObj.getMethods();

                    for (Method method : methods)
                    {
                        Annotation[] annotations = method.getDeclaredAnnotations();

                        for (Annotation annotation : annotations)
                        {
                            if (annotation instanceof ScriptingEngineMethod)
                            {
                                ScriptingEngineMethod scriptAnnotation = (ScriptingEngineMethod) annotation;

                                String category = context.getString(scriptAnnotation.category());
                                String assetPath = scriptAnnotation.assetPath();
                                String scriptLanguage = scriptAnnotation.language();
                                String methodName = method.getName();

                                if (languages.contains(scriptLanguage.toLowerCase()) && included.contains(method.toGenericString()) == false)
                                {
                                    StringBuffer args = new StringBuffer();

                                    for (String argument : ((ScriptingEngineMethod) annotation).arguments())
                                    {
                                        if (args.length() > 0)
                                            args.append(", ");

                                        args.append(argument);
                                    }

                                    JSONObject methodDef = new JSONObject();
                                    methodDef.put(ScriptHelpRequestHandler.METHOD_NAME, methodName + "(" + args.toString() + ")");
                                    methodDef.put(ScriptHelpRequestHandler.METHOD_FULL_NAME, method.toGenericString());
                                    methodDef.put(ScriptHelpRequestHandler.METHOD_LANGUAGE, scriptLanguage);
                                    methodDef.put(ScriptHelpRequestHandler.METHOD_CATEGORY, category);

                                    if (assetPath.trim().length() > 0)
                                        methodDef.put(ScriptHelpRequestHandler.METHOD_ASSET_PATH, assetPath);

                                    declaredMethods.put(methodDef);

                                    included.add(method.toGenericString());
                                }
                            }
                        }
                    }
                }

                AssetManager am = this._context.getAssets();

                InputStream in = am.open("embedded_website/docs/scripting_template.html");

                // http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
                Scanner s = new Scanner(in).useDelimiter("\\A");

                String content = "";

                if (s.hasNext())
                    content = s.next();

                content = content.replace("{{ METHOD_DEFINITIONS }}", declaredMethods.toString(2));
                content = content.replace("{{ LANGUAGE }}", language);

                return content.getBytes(Charset.forName("UTF-8"));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

            return "404 ERROR".getBytes(Charset.forName("UTF-8"));
        }
        else
        {
            try
            {
                AssetManager am = this._context.getAssets();

                InputStream in = am.open("embedded_website/docs/method_template.html");

                // http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
                Scanner s = new Scanner(in).useDelimiter("\\A");

                String content = "";

                if (s.hasNext())
                    content = s.next();

                s.close();

                in = am.open("embedded_website/docs/" + language.toLowerCase() + "/" + page);

                s = new Scanner(in).useDelimiter("\\A");

                String pageContent = "";

                if (s.hasNext())
                    pageContent = s.next();

                s.close();

                for (Class classObj : scriptingClasses)
                {
                    Method[] methods = classObj.getMethods();

                    for (Method method : methods)
                    {
                        Annotation[] annotations = method.getDeclaredAnnotations();

                        for (Annotation annotation : annotations)
                        {
                            if (annotation instanceof ScriptingEngineMethod)
                            {
                                ScriptingEngineMethod scriptAnnotation = (ScriptingEngineMethod) annotation;

                                if (page.equals(scriptAnnotation.assetPath()))
                                {
                                    StringBuffer args = new StringBuffer();

                                    for (String argument : ((ScriptingEngineMethod) annotation).arguments())
                                    {
                                        if (args.length() > 0)
                                            args.append(", ");

                                        args.append(argument);
                                    }

                                    content = content.replace("{{ METHOD_NAME }}", method.getName() +  "(" + args.toString() + ")");
                                    content = content.replace("{{ LANGUAGE }}", ((ScriptingEngineMethod) annotation).language());
                                }
                            }
                        }
                    }
                }

                content = content.replace("{{ METHOD_DOCUMENTATION }}", pageContent);

                return content.getBytes(Charset.forName("UTF-8"));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return "404 ERROR".getBytes(Charset.forName("UTF-8"));
    }
}
