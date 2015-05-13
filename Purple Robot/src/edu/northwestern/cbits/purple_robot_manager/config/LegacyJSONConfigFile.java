package edu.northwestern.cbits.purple_robot_manager.config;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.RhinoException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.scripting.SchemeEngine;
import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class LegacyJSONConfigFile
{
    public static final String FIRST_RUN = "json_config_first_run";

    public static final String USER_ID = "user_id";
    public static final String JSON_CONFIGURATION = "json_configuration_contents";
    public static final String JSON_LAST_UPDATE = "json_configuration_last_update";
    public static final String JSON_LAST_HASH = "json_configuration_last_update_hash";
    public static final String FEATURES = "features";
    private static final String JSON_INIT_SCRIPT = "init_script";

    private JSONObject parameters = null;

    private static LegacyJSONConfigFile _sharedFile = null;

    private static SharedPreferences prefs;

    @SuppressLint("DefaultLocale")
    public static void updateFromOnline(final Context context)
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                Runnable next = null;

                final EncryptionManager encryption = EncryptionManager.getInstance();

                Uri uri = encryption.getConfigUri(context);

                if (uri != null && uri.toString().trim().length() > 0)
                {
                    try
                    {
                        URL u = new URL(uri.toString());

                        final SharedPreferences prefs = LegacyJSONConfigFile.getPreferences(context);

                        Editor edit = prefs.edit();

                        X509TrustManager trust = new X509TrustManager()
                        {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers()
                            {
                                return new java.security.cert.X509Certificate[]{};
                            }

                            public void checkClientTrusted(X509Certificate[] chain, String authType)
                                    throws CertificateException
                            {

                            }

                            public void checkServerTrusted(X509Certificate[] chain, String authType)
                                    throws CertificateException
                            {

                            }
                        };

                        TrustManager[] trustAllCerts = { trust };

                        SSLContext sc = null;

                        try
                        {
                            sc = SSLContext.getInstance("TLS");
                            sc.init(null, trustAllCerts, new java.security.SecureRandom());
                        }
                        catch (Exception e)
                        {
                            LogManager.getInstance(context).logException(e);
                        }

                        HttpURLConnection conn = (HttpURLConnection) u.openConnection();

                        if (conn instanceof HttpsURLConnection)
                        {
                            HttpsURLConnection conns = (HttpsURLConnection) conn;

                            if (prefs.getBoolean("config_http_liberal_ssl", true))
                            {
                                conns.setSSLSocketFactory(sc.getSocketFactory());

                                conns.setHostnameVerifier(new HostnameVerifier()
                                {
                                    public boolean verify(String hostname, SSLSession session)
                                    {
                                        return true;
                                    }
                                });
                            }
                        }

                        for (int z = 0; z < 16 && conn.getHeaderField("Location") != null; z++)
                        {
                            URL newUrl = new URL(conn.getHeaderField("Location"));

                            conn.disconnect();

                            conn = (HttpURLConnection) newUrl.openConnection();

                            if (conn instanceof HttpsURLConnection)
                            {
                                HttpsURLConnection conns = (HttpsURLConnection) conn;

                                if (prefs.getBoolean("config_http_liberal_ssl", true))
                                {
                                    conns.setSSLSocketFactory(sc.getSocketFactory());

                                    conns.setHostnameVerifier(new HostnameVerifier()
                                    {
                                        public boolean verify(String hostname, SSLSession session)
                                        {
                                            return true;
                                        }
                                    });
                                }
                            }
                        }

                        BufferedInputStream bin = new BufferedInputStream(conn.getInputStream());
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();

                        byte[] buffer = new byte[4096];
                        int read = 0;

                        while ((read = bin.read(buffer, 0, buffer.length)) != -1)
                        {
                            bout.write(buffer, 0, read);
                        }

                        bin.close();

                        String scriptString = new String(bout.toByteArray(), "UTF-8");

                        String oldHash = prefs.getString(LegacyJSONConfigFile.JSON_LAST_HASH, "");
                        final String newHash = encryption.createHash(context, scriptString);

                        PurpleRobotApplication.fixPreferences(context, true);

                        if (scriptString.toLowerCase().startsWith("(begin ")
                                || (conn.getContentType() != null && conn.getContentType().toLowerCase()
                                        .startsWith("text/x-scheme")))
                        {
                            // TODO: Temp code until we get a more flexible
                            // parsing system in place...

                            if ("".equals(scriptString.trim()))
                                scriptString = "(begin)";

                            edit.putString("scheme_config_contents", scriptString);

                            if (oldHash.equals(newHash) == false)
                            {
                                try
                                {
                                    if (Looper.myLooper() == null)
                                        Looper.prepare();

                                    SchemeEngine scheme = new SchemeEngine(context, null);
                                    scheme.evaluateSource(scriptString);
                                }
                                catch (final Exception e)
                                {
                                    LogManager.getInstance(context).logException(e);

                                    e.printStackTrace();

                                    if (context instanceof Activity)
                                    {
                                        final Activity activity = (Activity) context;

                                        activity.runOnUiThread(new Runnable()
                                        {
                                            public void run()
                                            {
                                                Toast.makeText(activity, "100: " + e.getMessage(), Toast.LENGTH_LONG)
                                                        .show();
                                            }
                                        });
                                    }
                                }

                                edit.putString(LegacyJSONConfigFile.JSON_LAST_HASH, newHash);
                                edit.commit();

                                if (context instanceof Activity)
                                {
                                    final Activity activity = (Activity) context;

                                    activity.runOnUiThread(new Runnable()
                                    {
                                        public void run()
                                        {
                                            Toast.makeText(activity, R.string.success_json_set_uri, Toast.LENGTH_LONG)
                                                    .show();
                                        }
                                    });
                                }
                            }

                            encryption.setConfigurationReady(true);
                        }
                        else
                        {
                            if ("".equals(scriptString.trim()))
                                scriptString = "{}";

                            final JSONObject json = new JSONObject(scriptString);

                            if (oldHash.equals(newHash) == false)
                            {
                                edit.putString(LegacyJSONConfigFile.JSON_CONFIGURATION, scriptString);
                                edit.commit();

                                // TriggerManager.getInstance(context).removeAllTriggers();

                                next = new Runnable()
                                {
                                    public void run()
                                    {
                                        try
                                        {
                                            if (json.has(LegacyJSONConfigFile.JSON_INIT_SCRIPT))
                                            {
                                                String script = json.getString(LegacyJSONConfigFile.JSON_INIT_SCRIPT);

                                                JavaScriptEngine engine = new JavaScriptEngine(context);

                                                engine.runScript(script);
                                            }

                                            Editor edit = prefs.edit();
                                            edit.putString(LegacyJSONConfigFile.JSON_LAST_HASH, newHash);
                                            edit.commit();
                                        }
                                        catch (JSONException e)
                                        {
                                            LogManager.getInstance(context).logException(e);
                                        }
                                        catch (final EcmaError e)
                                        {
                                            LogManager.getInstance(context).logException(e);

                                            if (context instanceof Activity)
                                            {
                                                final Activity activity = (Activity) context;

                                                activity.runOnUiThread(new Runnable()
                                                {
                                                    public void run()
                                                    {
                                                        Toast.makeText(activity, "200: " + e.getMessage(),
                                                                Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                            }
                                        }
                                        catch (final EvaluatorException e)
                                        {
                                            LogManager.getInstance(context).logException(e);

                                            if (context instanceof Activity)
                                            {
                                                final Activity activity = (Activity) context;

                                                activity.runOnUiThread(new Runnable()
                                                {
                                                    public void run()
                                                    {
                                                        Toast.makeText(activity, "300: " + e.getMessage(),
                                                                Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                            }
                                        }
                                    }
                                };
                            }

                            encryption.setConfigurationReady(true);

                            if (context instanceof Activity)
                            {
                                final Activity activity = (Activity) context;

                                activity.runOnUiThread(new Runnable()
                                {
                                    public void run()
                                    {
                                        Toast.makeText(activity, R.string.success_json_set_uri, Toast.LENGTH_LONG)
                                                .show();
                                    }
                                });
                            }
                        }
                    }
                    catch (UnknownHostException e)
                    {
                        LogManager.getInstance(context).logException(e);
                    }
                    catch (MalformedURLException e)
                    {
                        LogManager.getInstance(context).logException(e);
                    }
                    catch (ConnectException e)
                    {
                        LogManager.getInstance(context).logException(e);
                    }
                    catch (IOException e)
                    {
                        LogManager.getInstance(context).logException(e);
                    }
                    catch (JSONException e)
                    {
                        LogManager.getInstance(context).logException(e);
                    }
                    catch (NullPointerException e)
                    {
                        LogManager.getInstance(context).logException(e);
                    }
                    catch (Exception e)
                    {
                        LogManager.getInstance(context).logException(e);
                    }

                    LegacyJSONConfigFile._sharedFile = new LegacyJSONConfigFile(context, next);
                }
                else
                {
                    final SharedPreferences prefs = LegacyJSONConfigFile.getPreferences(context);

                    if (prefs.getString(LegacyJSONConfigFile.JSON_CONFIGURATION, "{}").length() > 4)
                    {
                        Editor edit = prefs.edit();

                        edit.putString(LegacyJSONConfigFile.JSON_CONFIGURATION, "{}");
                        edit.commit();
                    }
                }
            }
        };

        Thread t = new Thread(r);

        try
        {
            t.start();
        }
        catch (OutOfMemoryError e)
        {
            System.gc();

            LogManager.getInstance(context).logException(e);
        }
    }

    protected void updateTriggers(Context context)
    {
        List<Trigger> triggerList = new ArrayList<Trigger>();

        try
        {
            JSONArray triggers = this.parameters.getJSONArray("triggers");

            for (int i = 0; triggers != null && i < triggers.length(); i++)
            {
                JSONObject json = triggers.getJSONObject(i);

                Map<String, Object> map = new HashMap<String, Object>();

                Iterator<String> keys = json.keys();

                while (keys.hasNext())
                {
                    String key = keys.next();

                    map.put(key, json.get(key));
                }

                Trigger t = Trigger.parse(context, map);

                if (t != null)
                    triggerList.add(t);
            }
        }
        catch (JSONException e)
        {

        }

        TriggerManager.getInstance(context).updateTriggers(context, triggerList);
    }

    public static LegacyJSONConfigFile getSharedFile(Context context)
    {
        SharedPreferences prefs = LegacyJSONConfigFile.getPreferences(context);

        if (prefs.getBoolean(LegacyJSONConfigFile.FIRST_RUN, true))
        {
            EncryptionManager.getInstance().setConfigUri(context,
                    Uri.parse(context.getString(R.string.json_config_url)));

            Editor e = prefs.edit();
            e.putBoolean(LegacyJSONConfigFile.FIRST_RUN, false);
            e.commit();
        }

        ProbeManager.allProbes(context);

        if (LegacyJSONConfigFile._sharedFile == null)
        {
            EncryptionManager.getInstance().setConfigurationReady(false);

            LegacyJSONConfigFile._sharedFile = new LegacyJSONConfigFile(context, null);
            LegacyJSONConfigFile.update(context, false);
        }

        return LegacyJSONConfigFile._sharedFile;
    }

    public String getStringParameter(String key)
    {
        try
        {
            if (this.parameters != null)
                return this.parameters.getString(key);
        }
        catch (JSONException e)
        {

        }

        return null;
    }

    private static SharedPreferences getPreferences(Context context)
    {
        if (LegacyJSONConfigFile.prefs == null)
            LegacyJSONConfigFile.prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        return LegacyJSONConfigFile.prefs;
    }

    private LegacyJSONConfigFile(Context context, Runnable next)
    {
        SharedPreferences prefs = LegacyJSONConfigFile.getPreferences(context);

        try
        {
            this.parameters = new JSONObject(prefs.getString(LegacyJSONConfigFile.JSON_CONFIGURATION, "{}"));

            this.updateSharedPreferences(context);
            this.updateTriggers(context);

            if (this.parameters.has(LegacyJSONConfigFile.JSON_INIT_SCRIPT))
            {
                String script = this.parameters.getString(LegacyJSONConfigFile.JSON_INIT_SCRIPT);

                JavaScriptEngine engine = new JavaScriptEngine(context);
                engine.runScript(script);
            }

            if (next != null)
                next.run();
        }
        catch (JSONException e)
        {
            e.printStackTrace();
            this.parameters = new JSONObject();
        }
        catch (RhinoException e)
        {
            e.printStackTrace();
            LogManager.getInstance(context).logException(e);

            this.parameters = new JSONObject();
        }
    }

    private void updateSharedPreferences(Context context)
    {
        String userId = null;

        try
        {
            if (this.parameters.has(LegacyJSONConfigFile.USER_ID))
                userId = this.parameters.getString(LegacyJSONConfigFile.USER_ID);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        SharedPreferences prefs = LegacyJSONConfigFile.getPreferences(context);
        Editor editor = prefs.edit();

        if (userId == null)
            userId = EncryptionManager.getInstance().getUserId(context);

        if (userId == null)
        {
            AccountManager manager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
            Account[] list = manager.getAccountsByType("com.google");

            if (list.length == 0)
                list = manager.getAccounts();

            if (list.length > 0)
            {
                userId = list[0].name;
            }
        }

        if (userId != null)
            EncryptionManager.getInstance().setUserId(context, userId);

        if (this.parameters.has(LegacyJSONConfigFile.FEATURES))
        {
            ProbeManager.clearFeatures();

            try
            {
                JSONArray features = this.parameters.getJSONArray(LegacyJSONConfigFile.FEATURES);

                for (int i = 0; i < features.length(); i++)
                {
                    JSONObject feature = features.getJSONObject(i);

                    String name = context.getString(R.string.label_unknown_feature);
                    String script = "";
                    String formatter = "";

                    if (feature.has("name"))
                        name = feature.getString("name");

                    if (feature.has("feature"))
                        script = feature.getString("feature");

                    if (feature.has("formatter"))
                        formatter = feature.getString("formatter");

                    ArrayList<String> sources = new ArrayList<String>();

                    if (feature.has("sources"))
                    {
                        JSONArray sourceArray = feature.getJSONArray("sources");

                        for (int j = 0; j < sourceArray.length(); j++)
                        {
                            String source = sourceArray.getString(j);

                            sources.add(source);
                        }
                    }

                    ProbeManager.addFeature(name, LegacyJSONConfigFile.toSlug(name), script, formatter, sources, false);
                }
            }
            catch (JSONException e)
            {
                LogManager.getInstance(context).logException(e);
            }
        }

        editor.commit();
    }

    public static String toSlug(String input)
    {
        Pattern NONLATIN = Pattern.compile("[^\\w-]");
        Pattern WHITESPACE = Pattern.compile("[\\s]");

        String nowhitespace = WHITESPACE.matcher(input).replaceAll("_");

        String slug = NONLATIN.matcher(nowhitespace).replaceAll("");

        return slug.toLowerCase(Locale.ENGLISH);
    }

    public static void update(final Context context, final boolean force)
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                SharedPreferences prefs = LegacyJSONConfigFile.getPreferences(context);

                long lastUpdate = prefs.getLong(LegacyJSONConfigFile.JSON_LAST_UPDATE, 0);

                long now = System.currentTimeMillis();

                int interval = Integer.parseInt(prefs.getString("config_json_refresh_interval", "3600"));

                if (force || (interval > 0 && now - lastUpdate > 1000 * interval))
                {
                    Editor edit = prefs.edit();

                    EncryptionManager.getInstance().setConfigurationReady(false);

                    LegacyJSONConfigFile.updateFromOnline(context);

                    edit.putLong(LegacyJSONConfigFile.JSON_LAST_UPDATE, now);
                    edit.commit();

                    if (LegacyJSONConfigFile._sharedFile != null)
                        LegacyJSONConfigFile._sharedFile.updateTriggers(context);
                }
                else if (lastUpdate != 0)
                {
                    String lastHash = prefs.getString(LegacyJSONConfigFile.JSON_LAST_HASH, null);

                    if (lastHash != null && lastHash.trim().length() > 0)
                        EncryptionManager.getInstance().setConfigurationReady(true);
                }
            }
        };

        try
        {
            Thread t = new Thread(r);
            t.start();
        }
        catch (OutOfMemoryError e)
        {
            LogManager.getInstance(context).logException(e);
        }
    }

    protected String content()
    {
        return this.parameters.toString();
    }

    protected boolean isValid()
    {
        if (this.content().length() > Preferences.MAX_VALUE_LENGTH)
            return false;

        return true;
    }
}
