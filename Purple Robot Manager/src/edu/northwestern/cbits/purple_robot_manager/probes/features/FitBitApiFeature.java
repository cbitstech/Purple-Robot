package edu.northwestern.cbits.purple_robot_manager.probes.features;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.OAuthActivity;
import edu.northwestern.cbits.purple_robot_manager.oauth.FitBitApi;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class FitBitApiFeature extends Feature 
{
	public static final String CONSUMER_KEY = "7bc8998f319c43eb99c72f3e3d63dbd9";
	public static final String CONSUMER_SECRET = "85c9ef8be7c14d668b4b9bf782f1c0b5";
	private static final String REQUEST_TOKEN_URL = "https://api.fitbit.com/oauth/request_token";
	private static final String ACCESS_TOKEN_URL = "https://api.fitbit.com/oauth/access_token";
	private static final String AUTHORIZE_URL = "http://www.fitbit.com/oauth/authorize";

	private long _lastUpdate = 0;
	private boolean _authed = false;

	private String _token = null;
	private String _secret = null;

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
					this._token = prefs.getString("oauth_fitbit_token", null);
					this._secret = prefs.getString("oauth_fitbit_secret", null);

					Log.e("PR", "TOKEN:  " + this._token);
					Log.e("PR", "SECRET: " + this._secret);
					
        			if (this._authed == false)
        			{
				        Log.e("PR", "FETCH FITBIT DATA...");

				        Intent intent = new Intent(context, OAuthActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						
						intent.putExtra(OAuthActivity.CONSUMER_KEY, CONSUMER_KEY);
						intent.putExtra(OAuthActivity.CONSUMER_SECRET, CONSUMER_SECRET);
						intent.putExtra(OAuthActivity.REQUEST_TOKEN_URL, REQUEST_TOKEN_URL);
						intent.putExtra(OAuthActivity.ACCESS_TOKEN_URL, ACCESS_TOKEN_URL);
						intent.putExtra(OAuthActivity.AUTHORIZE_URL, AUTHORIZE_URL);
						intent.putExtra(OAuthActivity.REQUESTER, "fitbit");
						intent.putExtra(OAuthActivity.CALLBACK_URL, "cbits-oauth://purplerobot/oauth/fitbit");
						
						context.startActivity(intent);
						
						this._authed = true;
        			}
					else
					{
						Token accessToken = new Token(this._token, this._secret);

	                	ServiceBuilder builder = new ServiceBuilder();
	                	builder = builder.provider(FitBitApi.class);
	                	builder = builder.apiKey(FitBitApiFeature.CONSUMER_KEY);
	                	builder = builder.apiSecret(FitBitApiFeature.CONSUMER_SECRET);
	                	
	                	final OAuthService service = builder.build();

						final OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/profile.json");
						service.signRequest(accessToken, request);
						
						Runnable r = new Runnable()
						{
							public void run() 
							{
								Response response = request.send();
								Log.e("PR", "GOT RESP: " + response.getBody());
							}
						};
						
						Thread t = new Thread(r);
						t.start();
						

						this._lastUpdate = now;
					}
				}
				
				return true;
			}
		}

		return false;
	}
}
