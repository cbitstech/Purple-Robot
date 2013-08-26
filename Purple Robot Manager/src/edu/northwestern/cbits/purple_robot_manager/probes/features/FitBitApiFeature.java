package edu.northwestern.cbits.purple_robot_manager.probes.features;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class FitBitApiFeature extends Feature
{
	private static final String CONSUMER_KEY = "7bc8998f319c43eb99c72f3e3d63dbd9";
	private static final String CONSUMER_SECRET = "85c9ef8be7c14d668b4b9bf782f1c0b5";
	private static final String REQUEST_TOKEN_URL = "https://api.fitbit.com/oauth/request_token";
	private static final String ACCESS_TOKEN_URL = "https://api.fitbit.com/oauth/access_token";
	private static final String AUTHORIZE_URL = "http://www.fitbit.com/oauth/authorize";
	

	private long _lastUpdate = 0;
	private String _token = null;
	
	protected String featureKey() 
	{
		return "fitbit_api";
	}

	protected String summary(Context context) 
	{
		return "tOdO";
	}

	public String name(Context context) 
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.features.FitBitApiFeature";
	}

	public String title(Context context) 
	{
		return context.getString(R.string.title_fitbit_api_feature);
	}

	public void enable(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_fitbit_api_enabled", true);
		e.commit();
	}

	public void disable(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_fitbit_api_enabled", false);
		e.commit();
	}
	
	public boolean isEnabled(Context context)
	{
		if (super.isEnabled(context))
		{
			SharedPreferences prefs = Probe.getPreferences(context);

			if (prefs.getBoolean("config_feature_fitbit_api_enabled", false))
			{
				long now = System.currentTimeMillis();
				
				if (now - this._lastUpdate > 1000 * 60 * 30)
				{
					if (this._token == null)
					{
				        try 
				        {
							OAuthConsumer consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
					        OAuthProvider provider = new CommonsHttpOAuthProvider(REQUEST_TOKEN_URL, ACCESS_TOKEN_URL, AUTHORIZE_URL);

					        String url = provider.retrieveRequestToken(consumer, "http://purplerobot/oauth/fitbit");
					        
					        Intent intent = new Intent(Intent.ACTION_VIEW);
					        intent.setData(Uri.parse(url));
					        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					        
					        context.startActivity(intent);
					        
					        Log.e("PR", "TOKEN: " + this._token);
						}
				        catch (OAuthMessageSignerException e) 
				        {
							e.printStackTrace();
						} 
				        catch (OAuthNotAuthorizedException e) 
				        {
							e.printStackTrace();
						} 
				        catch (OAuthExpectationFailedException e) 
				        {
							e.printStackTrace();
						} 
				        catch (OAuthCommunicationException e) 
				        {
							e.printStackTrace();
						}
					}
					
			        this._token = "foobar";

			        Log.e("PR", "FETCH FITBIT DATA...");
					// Fetch API data...
					
					this._lastUpdate = now;
				}
				
				return true;
			}
		}

		return false;
	}
}
