package edu.northwestern.cbits.purple_robot_manager.http;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

public class BasicAuthHelper
{
    private static BasicAuthHelper _sharedInstance = null;
    private Context _context = null;

    public static BasicAuthHelper getInstance(Context context)
    {
        if (BasicAuthHelper._sharedInstance != null)
            return BasicAuthHelper._sharedInstance;

        if (context != null)
            BasicAuthHelper._sharedInstance = new BasicAuthHelper(context.getApplicationContext());

        return BasicAuthHelper._sharedInstance;
    }

    public BasicAuthHelper(Context context)
    {
        this._context = context;
    }

    public static boolean isAuthenticated(HttpRequest request)
    {
        BasicAuthTokenExtractor extractor = new BasicAuthTokenExtractor();

        try
        {
            String creds = extractor.extract(request);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BasicAuthHelper._sharedInstance._context);
            String password = prefs.getString(LocalHttpServer.BUILTIN_HTTP_SERVER_PASSWORD, LocalHttpServer.BUILTIN_HTTP_SERVER_PASSWORD_DEFAULT);

            if (password == null || password.trim().length() == 0 || (creds != null && creds.equals("purple_robot:" + password)))
                return true;
        }
        catch (HttpException e)
        {
            LogManager.getInstance(BasicAuthHelper._sharedInstance._context).logException(e);
        }

        return false;
    }

    public static void unauthedResponse(HttpResponse response)
    {
        response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);

        try
        {
            response.setHeader("WWW-Authenticate", "Basic realm=\"Purple Robot\"");

            StringEntity body = new StringEntity(BasicAuthHelper._sharedInstance._context.getString(R.string.error_unauthed_request));
            body.setContentType("text/plain");
            response.setEntity(body);
        }
        catch (UnsupportedEncodingException e)
        {
            LogManager.getInstance(BasicAuthHelper._sharedInstance._context).logException(e);
        }
    }
}
