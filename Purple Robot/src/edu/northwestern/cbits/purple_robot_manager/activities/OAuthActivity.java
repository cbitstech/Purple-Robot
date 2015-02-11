package edu.northwestern.cbits.purple_robot_manager.activities;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.DefaultApi10a;
import org.scribe.builder.api.DefaultApi20;
import org.scribe.builder.api.Foursquare2Api;
import org.scribe.builder.api.LinkedInApi;
import org.scribe.exceptions.OAuthConnectionException;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.FoursquareProbe;

public class OAuthActivity extends Activity
{
    public static final String CONSUMER_KEY = "CONSUMER_KEY";
    public static final String CONSUMER_SECRET = "CONSUMER_SECRET";
    public static final String CALLBACK_URL = "CALLBACK_URL";
    public static final String REQUESTER = "REQUESTER";

    @SuppressWarnings(
    { "unchecked", "rawtypes" })
    public void onResume()
    {
        super.onResume();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        final OAuthActivity me = this;

        Bundle extras = this.getIntent().getExtras();

        if (extras.containsKey(OAuthActivity.CONSUMER_KEY))
        {
            final String consumerKey = extras.getString(OAuthActivity.CONSUMER_KEY);
            final String consumerSecret = extras.getString(OAuthActivity.CONSUMER_SECRET);
            final String callbackUrl = extras.getString(OAuthActivity.CALLBACK_URL);
            final String requester = extras.getString(OAuthActivity.REQUESTER);

            Class api = null;

            if ("linkedin".equalsIgnoreCase(requester))
                api = LinkedInApi.class;
            else if ("foursquare".equalsIgnoreCase(requester))
                api = Foursquare2Api.class;

            final Class apiClass = api;

            if (apiClass != null)
            {
                ServiceBuilder builder = new ServiceBuilder();
                builder = builder.provider(apiClass);
                builder = builder.apiKey(consumerKey);
                builder = builder.apiSecret(consumerSecret);
                builder = builder.callback(callbackUrl);

                final OAuthService service = builder.build();

                final OAuthConfig config = new OAuthConfig(consumerKey, consumerSecret, callbackUrl, null, null, null);

                Runnable r = new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            if (DefaultApi20.class.isAssignableFrom(apiClass))
                            {
                                Constructor constructor = apiClass.getConstructors()[0];

                                try
                                {
                                    DefaultApi20 api = (DefaultApi20) constructor.newInstance();

                                    String url = api.getAuthorizationUrl(config);

                                    Intent intent = new Intent(me, OAuthWebActivity.class);
                                    intent.setData(Uri.parse(url));

                                    me.startActivity(intent);
                                }
                                catch (InstantiationException e)
                                {
                                    LogManager.getInstance(me).logException(e);
                                }
                                catch (IllegalAccessException e)
                                {
                                    LogManager.getInstance(me).logException(e);
                                }
                                catch (IllegalArgumentException e)
                                {
                                    LogManager.getInstance(me).logException(e);
                                }
                                catch (InvocationTargetException e)
                                {
                                    LogManager.getInstance(me).logException(e);
                                }
                            }
                            else if (DefaultApi10a.class.isAssignableFrom(apiClass))
                            {
                                Token token = service.getRequestToken();

                                Editor e = prefs.edit();
                                e.putString("request_token_" + requester, token.getToken());
                                e.putString("request_secret_" + requester, token.getSecret());
                                e.commit();

                                String url = service.getAuthorizationUrl(token);

                                Intent intent = new Intent(me, OAuthWebActivity.class);
                                intent.setData(Uri.parse(url));
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                me.startActivity(intent);
                            }
                        }
                        catch (OAuthException e)
                        {
                            LogManager.getInstance(me).logException(e);
                        }
                    }
                };

                Thread t = new Thread(r);
                t.start();
            }

            this.finish();
        }
        else
        {
            Uri incomingUri = this.getIntent().getData();

            if ("http".equals(incomingUri.getScheme()))
            {
                List<String> segments = incomingUri.getPathSegments();

                if (segments.get(0).equals("oauth"))
                {
                    final String requester = segments.get(1);

                    String verifier = incomingUri.getQueryParameter("oauth_verifier");

                    if (verifier == null)
                        verifier = incomingUri.getQueryParameter("code");

                    if (verifier != null)
                    {
                        final Token requestToken = new Token(prefs.getString("request_token_" + requester, ""),
                                prefs.getString("request_secret_" + requester, ""));

                        final Verifier v = new Verifier(verifier);

                        Class apiClass = null;
                        String consumerKey = null;
                        String consumerSecret = null;
                        String callback = null;

                        if ("foursquare".equalsIgnoreCase(requester))
                        {
                            apiClass = Foursquare2Api.class;
                            consumerKey = this.getString(R.string.foursquare_consumer_key);
                            consumerSecret = this.getString(R.string.foursquare_consumer_secret);
                            callback = FoursquareProbe.CALLBACK;
                        }

                        if (apiClass != null && consumerKey != null && consumerSecret != null)
                        {
                            ServiceBuilder builder = new ServiceBuilder();
                            builder = builder.provider(apiClass);
                            builder = builder.apiKey(consumerKey);
                            builder = builder.apiSecret(consumerSecret);

                            if (callback != null)
                                builder = builder.callback(callback);

                            final OAuthService service = builder.build();

                            Runnable r = null;

                            if (DefaultApi20.class.isAssignableFrom(apiClass))
                            {
                                r = new Runnable()
                                {
                                    public void run()
                                    {
                                        Token accessToken = service.getAccessToken(null, v);

                                        Editor e = prefs.edit();
                                        e.putString("oauth_" + requester + "_secret", accessToken.getSecret());
                                        e.putString("oauth_" + requester + "_token", accessToken.getToken());

                                        e.commit();

                                        SanityManager.getInstance(me).refreshState();

                                        me.runOnUiThread(new Runnable()
                                        {
                                            public void run()
                                            {
                                                me.authSuccess();
                                            }
                                        });
                                    }
                                };
                            }
                            else if (DefaultApi10a.class.isAssignableFrom(apiClass))
                            {
                                r = new Runnable()
                                {
                                    public void run()
                                    {
                                        try
                                        {
                                            Token accessToken = service.getAccessToken(requestToken, v);

                                            Editor e = prefs.edit();
                                            e.putString("oauth_" + requester + "_secret", accessToken.getSecret());
                                            e.putString("oauth_" + requester + "_token", accessToken.getToken());

                                            e.commit();

                                            SanityManager.getInstance(me).refreshState();

                                            me.runOnUiThread(new Runnable()
                                            {
                                                public void run()
                                                {
                                                    me.authSuccess();
                                                }
                                            });
                                        }
                                        catch (OAuthConnectionException e)
                                        {
                                            LogManager.getInstance(me).logException(e);
                                        }
                                    }
                                };
                            }

                            Thread t = new Thread(r);
                            t.start();
                        }
                    }
                }
            }
        }
    }

    protected void authSuccess()
    {
        final OAuthActivity me = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder = builder.setTitle(R.string.auth_success_title);
        builder = builder.setMessage(R.string.auth_success_message);

        builder = builder.setCancelable(false);
        builder = builder.setPositiveButton(R.string.auth_success_close, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                me.finish();
            }
        });

        builder.create().show();
    }
}
