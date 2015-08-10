package edu.northwestern.cbits.purple_robot_manager.output;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import edu.northwestern.cbits.purple_robot_manager.annotation.ScriptingEngineMethod;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.scripting.SchemeEngine;

public class BootstrapSiteExporter
{
    private static final String LANGUAGE_ALL = "All";
    private static final String METHOD_NAME = "name";
    private static final String METHOD_LANGUAGE = "language";
    private static final String METHOD_CATEGORY = "category";
    private static final String METHOD_ASSET_PATH = "path";
    private static final String METHOD_FULL_NAME = "full_name";
    public static final int TEMPLATE_TYPE_BOOTSTRAP = 0;
    private static final int TEMPLATE_TYPE_JEKYLL = 1;

    public static Uri exportSite(Context context)
    {
        File exportDirectory = context.getExternalFilesDir(null);

        String filename = System.currentTimeMillis() + "_bootstrap.zip";

        File exportedFile = new File(exportDirectory, filename);

        AssetManager assets = context.getAssets();

        try {
            FileOutputStream out = new FileOutputStream(exportedFile);

            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(out));

            try
            {
                BootstrapSiteExporter.writeStaticAssets(assets, zos, "embedded_website/css");
                BootstrapSiteExporter.writeStaticAssets(assets, zos, "embedded_website/fonts");
                BootstrapSiteExporter.writeStaticAssets(assets, zos, "embedded_website/images");
                BootstrapSiteExporter.writeStaticAssets(assets, zos, "embedded_website/js");

                BootstrapSiteExporter.writeScriptDocumentation(context, zos, "embedded_website/docs/all", BootstrapSiteExporter.TEMPLATE_TYPE_BOOTSTRAP);
                BootstrapSiteExporter.writeScriptDocumentation(context, zos, "embedded_website/docs/scheme", BootstrapSiteExporter.TEMPLATE_TYPE_BOOTSTRAP);
                BootstrapSiteExporter.writeScriptDocumentation(context, zos, "embedded_website/docs/javascript", BootstrapSiteExporter.TEMPLATE_TYPE_BOOTSTRAP);
            } finally {
                zos.close();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        return Uri.fromFile(exportedFile);
    }

    private static void writeScriptDocumentation(Context context, ZipOutputStream out, String path, int templateType)
    {
        String[] components = path.split("/");

        String language = components[components.length - 1];

        byte[] indexPage = BootstrapSiteExporter.fetchContent(context, language, null, true, templateType);

        try {
            ZipEntry entry = new ZipEntry(path + "/index.html");
            out.putNextEntry(entry);
            out.write(indexPage);
            out.closeEntry();

            JSONArray methods = BootstrapSiteExporter.methodsForLanguage(context, language);

            HashSet<String> loaded = new HashSet<>();

            for (int i = 0; i < methods.length(); i++)
            {
                try
                {
                    JSONObject methodDef = methods.getJSONObject(i);

                    if (methodDef.has(BootstrapSiteExporter.METHOD_ASSET_PATH))
                    {
                        String[] methodComponents = methodDef.getString(BootstrapSiteExporter.METHOD_ASSET_PATH).split("/");

                        String page = methodComponents[methodComponents.length - 1];

                        if (loaded.contains(page) == false) {
                            String pageLanguage = language;

                            if (page.startsWith("all_"))
                                pageLanguage = "all";

                            byte[] methodContent = BootstrapSiteExporter.fetchContent(context, pageLanguage, page, true, templateType);

                            try {
                                ZipEntry methodEntry = new ZipEntry(path + "/" + page);
                                out.putNextEntry(methodEntry);
                                out.write(methodContent);
                                out.closeEntry();
                            } catch (ZipException e) {
                                e.printStackTrace();
                            }

                            loaded.add(page);
                        }
                    }
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void writeStaticAssets(AssetManager assets, ZipOutputStream out, String path)
    {
        try
        {
            for (String item : assets.list(path))
            {
                try {
                    InputStream fileIn = assets.open(path + "/" + item);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    BufferedInputStream bin = new BufferedInputStream(fileIn);

                    byte[] buffer = new byte[8192];
                    int read = 0;

                    while ((read = bin.read(buffer, 0, buffer.length)) != -1)
                    {
                        baos.write(buffer, 0, read);
                    }

                    bin.close();
                    baos.close();

                    ZipEntry entry = new ZipEntry(path + "/" + item);
                    out.putNextEntry(entry);
                    out.write(baos.toByteArray());
                    out.closeEntry();
                }
                catch (IOException e)
                {
                    BootstrapSiteExporter.writeStaticAssets(assets, out, path + "/" + item);
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JSONArray methodsForLanguage(Context context, String language)
    {
        Class[] scriptingClasses = { JavaScriptEngine.class, SchemeEngine.class };

        ArrayList<String> languages = new ArrayList<>();
        languages.add(BootstrapSiteExporter.LANGUAGE_ALL.toLowerCase());

        if (languages.contains(language) == false)
            languages.add(language.toLowerCase());

        JSONArray declaredMethods = new JSONArray();
        HashSet<String> included = new HashSet<>();

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
                            StringBuilder args = new StringBuilder();

                            for (String argument : ((ScriptingEngineMethod) annotation).arguments())
                            {
                                if (args.length() > 0)
                                    args.append(", ");

                                args.append(argument);
                            }

                            JSONObject methodDef = new JSONObject();

                            try {
                                methodDef.put(BootstrapSiteExporter.METHOD_NAME, methodName + "(" + args.toString() + ")");
                                methodDef.put(BootstrapSiteExporter.METHOD_FULL_NAME, method.toGenericString());
                                methodDef.put(BootstrapSiteExporter.METHOD_LANGUAGE, scriptLanguage);
                                methodDef.put(BootstrapSiteExporter.METHOD_CATEGORY, category);

                                if (assetPath.trim().length() > 0)
                                    methodDef.put(BootstrapSiteExporter.METHOD_ASSET_PATH, assetPath);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            declaredMethods.put(methodDef);

                            included.add(method.toGenericString());
                        }
                    }
                }
            }
        }

        return declaredMethods;
    }

    public static byte[] fetchContent(Context context, String language, String page, boolean readOnly, int templateType)
    {
        Class[] scriptingClasses = { JavaScriptEngine.class, SchemeEngine.class };

        language = language.toLowerCase();

        ArrayList<String> languages = new ArrayList<>();
        languages.add(BootstrapSiteExporter.LANGUAGE_ALL.toLowerCase());

        if (languages.contains(language) == false)
            languages.add(language.toLowerCase());

        if (page == null || page.trim().length() == 0)
        {
            try
            {
                JSONArray declaredMethods = BootstrapSiteExporter.methodsForLanguage(context, language);

                AssetManager am = context.getAssets();

                String template = "embedded_website/docs/scripting_template.html";

                if (readOnly && templateType == BootstrapSiteExporter.TEMPLATE_TYPE_BOOTSTRAP)
                    template = "embedded_website/docs/scripting_template_readonly.html";
                else if (templateType == BootstrapSiteExporter.TEMPLATE_TYPE_JEKYLL)
                    template = "embedded_website/docs/scripting_template_jekyll.html";

                InputStream in = am.open(template);

                // http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
                Scanner s = new Scanner(in).useDelimiter("\\A");

                String content = "";

                if (s.hasNext())
                    content = s.next();

                content = content.replace("{{ METHOD_DEFINITIONS }}", declaredMethods.toString(2));
                content = content.replace("{{ LANGUAGE }}", language.toLowerCase());

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
            if (page.startsWith("all_"))
                language = "all";

            try
            {
                AssetManager am = context.getAssets();

                String template = "embedded_website/docs/method_template.html";

                if (readOnly && templateType == BootstrapSiteExporter.TEMPLATE_TYPE_BOOTSTRAP)
                    template = "embedded_website/docs/method_template_readonly.html";
                else if (templateType == BootstrapSiteExporter.TEMPLATE_TYPE_JEKYLL)
                    template = "embedded_website/docs/method_template_jekyll.html";

                InputStream in = am.open(template);

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
                                    StringBuilder args = new StringBuilder();

                                    for (String argument : ((ScriptingEngineMethod) annotation).arguments())
                                    {
                                        if (args.length() > 0)
                                            args.append(", ");

                                        args.append(argument);
                                    }

                                    content = content.replace("{{ METHOD_NAME }}", method.getName() +  "(" + args.toString() + ")");
                                    content = content.replace("{{ LANGUAGE }}", ((ScriptingEngineMethod) annotation).language().toLowerCase());
                                    content = content.replace("{{ PAGE }}", page);
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

    public static Uri exportJekyllPages(Context context) {
        File exportDirectory = context.getExternalFilesDir(null);

        String filename = System.currentTimeMillis() + "_jekyll.zip";

        File exportedFile = new File(exportDirectory, filename);

        try {
            FileOutputStream out = new FileOutputStream(exportedFile);

            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(out));

            try
            {
                BootstrapSiteExporter.writeScriptDocumentation(context, zos, "embedded_website/docs/all", BootstrapSiteExporter.TEMPLATE_TYPE_JEKYLL);
                BootstrapSiteExporter.writeScriptDocumentation(context, zos, "embedded_website/docs/scheme", BootstrapSiteExporter.TEMPLATE_TYPE_JEKYLL);
                BootstrapSiteExporter.writeScriptDocumentation(context, zos, "embedded_website/docs/javascript", BootstrapSiteExporter.TEMPLATE_TYPE_JEKYLL);
            } finally {
                zos.close();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        return Uri.fromFile(exportedFile);
    }
}
