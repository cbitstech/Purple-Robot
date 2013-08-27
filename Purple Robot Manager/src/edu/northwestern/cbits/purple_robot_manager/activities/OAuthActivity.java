package edu.northwestern.cbits.purple_robot_manager.activities;

import java.util.List;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import edu.northwestern.cbits.purple_robot_manager.oauth.FitBitApi;
import edu.northwestern.cbits.purple_robot_manager.probes.features.FitBitApiFeature;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class OAuthActivity extends Activity
{
	public static final String CONSUMER_KEY = "CONSUMER_KEY";
	public static final String CONSUMER_SECRET = "CONSUMER_SECRET";
	public static final String REQUEST_TOKEN_URL = "REQUEST_TOKEN_URL";
	public static final String ACCESS_TOKEN_URL = "ACCESS_TOKEN_URL";
	public static final String AUTHORIZE_URL = "AUTHORIZE_URL";
	public static final String CALLBACK_URL = "CALLBACK_URL";
	public static final String REQUESTER = "REQUESTER";

	public void onResume()
	{
		super.onResume();
		
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    	Bundle extras = this.getIntent().getExtras();
    	
    	if (extras.containsKey(OAuthActivity.CONSUMER_KEY))
    	{
        	final String consumerKey = extras.getString(OAuthActivity.CONSUMER_KEY);
        	final String consumerSecret = extras.getString(OAuthActivity.CONSUMER_SECRET);
        	final String callbackUrl = extras.getString(OAuthActivity.CALLBACK_URL);
        	final String requester = extras.getString(OAuthActivity.REQUESTER);

        	ServiceBuilder builder = new ServiceBuilder();
        	builder = builder.provider(FitBitApi.class);
        	builder = builder.apiKey(consumerKey);
        	builder = builder.apiSecret(consumerSecret);
        	builder = builder.callback(callbackUrl);
        	
        	final OAuthService service = builder.build();
    		
    		Log.e("PR", "AUTHING");

        	final OAuthActivity me = this;
        	
        	Runnable r = new Runnable()
        	{
				public void run() 
				{
					Token token = service.getRequestToken();
					
					Editor e = prefs.edit();
					e.putString("request_token_" + requester, token.getToken());
					e.putString("request_secret_" + requester, token.getSecret());
					e.commit();

					String url = service.getAuthorizationUrl(token);
	    	        
	    	        Intent intent = new Intent(Intent.ACTION_VIEW);
	    	        intent.setData(Uri.parse(url));
	    	        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

	    	        me.startActivity(intent);
				}
        	};
        	
        	Thread t = new Thread(r);
        	t.start();
    	}
    	else
    	{
    		Log.e("PR", "URLING");

    		Uri incomingUri = this.getIntent().getData();

        	if ("cbits-oauth".equals(incomingUri.getScheme()))
        	{
        		Log.e("PR", "GOT OAUTH URL: " + incomingUri);
        		
        		List<String> segments = incomingUri.getPathSegments();
        		
        		if (segments.get(0).equals("oauth"))
        		{
        			final String requester = segments.get(1);
        					
        			String token = incomingUri.getQueryParameter("oauth_token");
        			String verifier = incomingUri.getQueryParameter("oauth_verifier");
        			
        			final Token requestToken = new Token(prefs.getString("request_token_" + requester, ""), prefs.getString("request_secret_" + requester, ""));
        			
        			final Verifier v = new Verifier(verifier);

                	ServiceBuilder builder = new ServiceBuilder();
                	builder = builder.provider(FitBitApi.class);
                	builder = builder.apiKey(FitBitApiFeature.CONSUMER_KEY);
                	builder = builder.apiSecret(FitBitApiFeature.CONSUMER_SECRET);
                	
                	final OAuthService service = builder.build();
                	
                	Runnable r = new Runnable()
                	{
						public void run() 
						{
		                	Token accessToken = service.getAccessToken(requestToken, v);
		                	
		                	Editor e = prefs.edit();
		                	e.putString("oauth_" + requester + "_secret", accessToken.getSecret());
		                	e.putString("oauth_" + requester + "_token", accessToken.getToken());
		                	
		                	e.commit();
						}
                	};
                	
                	Thread t = new Thread(r);
                	t.start();
        		}
        	}
    	}

    	this.finish();
	}
}
