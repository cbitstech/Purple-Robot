package edu.northwestern.cbits.purple_robot_manager.probes.features;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.OAuthActivity;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.oauth.FitbitApi;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class FitbitApiFeature extends Feature 
{
	public static final String CONSUMER_KEY = "7bc8998f319c43eb99c72f3e3d63dbd9";
	public static final String CONSUMER_SECRET = "85c9ef8be7c14d668b4b9bf782f1c0b5";

	protected static final String VERY_ACTIVE_MINUTES = "VERY_ACTIVE_MINUTES";
	protected static final String LIGHTLY_ACTIVE_MINUTES = "LIGHTLY_ACTIVE_MINUTES";
	protected static final String STEPS = "STEPS";
	protected static final String MARGINAL_CALORIES = "MARGINAL_CALORIES";
	protected static final String CALORIES_OUT = "CALORIES_OUT";
	protected static final String ACTIVE_SCORE = "ACTIVE_SCORE";
	protected static final String CALORIES_BMR = "CALORIES_BMR";
	protected static final String FAIRLY_ACTIVE_MINUTES = "FAIRLY_ACTIVE_MINUTES";
	protected static final String SEDENTARY_MINUTES = "SEDENTARY_MINUTES";
	protected static final String ACTIVITY_CALORIES = "ACTIVITY_CALORIES";

	protected static final String VERY_ACTIVE_RATIO = "VERY_ACTIVE_RATIO";
	protected static final String LIGHTLY_ACTIVE_RATIO = "LIGHTLY_ACTIVE_RATIO";
	protected static final String SEDENTARY_RATIO = "SEDENTARY_RATIO";
	protected static final String FAIRLY_ACTIVE_RATIO = "FAIRLY_ACTIVE_RATIO";

	protected static final String TOTAL_DISTANCE = "TOTAL_DISTANCE";
	protected static final String GOAL_STEPS = "GOAL_STEPS";
	protected static final String GOAL_SCORE = "GOAL_SCORE";
	protected static final String GOAL_DISTANCE = "GOAL_DISTANCE";
	protected static final String GOAL_CALORIES = "GOAL_CALORIES";
	protected static final String GOAL_DISTANCE_RATIO = "GOAL_DISTANCE_RATIO";
	protected static final String GOAL_STEPS_RATIO = "GOAL_STEPS_RATIO";
	protected static final String GOAL_CALORIES_RATIO = "GOAL_CALORIES_RATIO";
	protected static final String GOAL_SCORE_RATIO = "GOAL_SCORE_RATIO";

	private long _lastUpdate = 0;
	private long _lastFetch = 0;

	private String _token = null;
	private String _secret = null;

	protected String featureKey() 
	{
		return "fitbit_api";
	}

	protected String summary(Context context) 
	{
		return context.getString(R.string.summary_fitbit_api_feature_desc);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_external_services_category);
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
		e.putBoolean("config_feature_fitbit_api_enabled", true);
		e.commit();
	}

	public void disable(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_feature_fitbit_api_enabled", false);
		e.commit();
	}
	
	public boolean isEnabled(final Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		if (super.isEnabled(context))
		{
			if (prefs.getBoolean("config_feature_fitbit_api_enabled", false))
			{
				long now = System.currentTimeMillis();
				
				if (now - this._lastUpdate > 1000 * 60)
				{
					this._lastUpdate = now;

					final FitbitApiFeature me = this;

					this._token = prefs.getString("oauth_fitbit_token", null);
					this._secret = prefs.getString("oauth_fitbit_secret", null);

    				final String title = context.getString(R.string.title_fitbit_check);
    				final SanityManager sanity = SanityManager.getInstance(context);

        			if (this._token == null || this._secret == null)
        			{
        				String message = context.getString(R.string.message_fitbit_check);
        				
        				Runnable action = new Runnable()
        				{
							public void run() 
							{
								me.fetchAuth(context);
							}
        				};
        				
        				sanity.addAlert(SanityCheck.WARNING, title, message, action);
        			}
					else
					{
						sanity.clearAlert(title);

						if (now - this._lastFetch > 1000 * 60 * 5)
						{
							this._lastFetch = now;
							
							Token accessToken = new Token(this._token, this._secret);
	
		                	ServiceBuilder builder = new ServiceBuilder();
		                	builder = builder.provider(FitbitApi.class);
		                	builder = builder.apiKey(FitbitApiFeature.CONSUMER_KEY);
		                	builder = builder.apiSecret(FitbitApiFeature.CONSUMER_SECRET);
		                	
		                	final OAuthService service = builder.build();
		                	
		                	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		                	
		                	String dateString = sdf.format(new Date());
	
							final OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/activities/date/" + dateString + ".json");
							service.signRequest(accessToken, request);
	
							Runnable r = new Runnable()
							{
								public void run() 
								{
									try 
									{
										Response response = request.send();

										JSONObject body = new JSONObject(response.getBody());

										JSONObject summary = body.getJSONObject("summary");
										
										Bundle bundle = new Bundle();
										bundle.putString("PROBE", me.name(context));
										bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
										
										long veryActive = summary.getLong("veryActiveMinutes");
										long fairlyActive = summary.getLong("fairlyActiveMinutes");
										long lightlyActive = summary.getLong("lightlyActiveMinutes");
										long sedentary = summary.getLong("sedentaryMinutes");
										
										long total = veryActive + fairlyActive + lightlyActive + sedentary;
										
										bundle.putLong(FitbitApiFeature.VERY_ACTIVE_MINUTES, veryActive);
										bundle.putLong(FitbitApiFeature.FAIRLY_ACTIVE_MINUTES, fairlyActive);
										bundle.putLong(FitbitApiFeature.LIGHTLY_ACTIVE_MINUTES, lightlyActive);
										bundle.putLong(FitbitApiFeature.SEDENTARY_MINUTES, sedentary);
	
										bundle.putDouble(FitbitApiFeature.VERY_ACTIVE_RATIO, (double) veryActive / (double) total);
										bundle.putDouble(FitbitApiFeature.FAIRLY_ACTIVE_RATIO, (double) fairlyActive / (double) total);
										bundle.putDouble(FitbitApiFeature.LIGHTLY_ACTIVE_RATIO, (double) lightlyActive / (double) total);
										bundle.putDouble(FitbitApiFeature.SEDENTARY_RATIO, (double) sedentary / (double) total);
										
										long steps = summary.getLong("steps");
										bundle.putLong(FitbitApiFeature.STEPS, steps);
	
										long caloriesOut = summary.getLong("caloriesOut");
										bundle.putLong(FitbitApiFeature.CALORIES_OUT, caloriesOut);
										bundle.putLong(FitbitApiFeature.CALORIES_BMR, summary.getLong("caloriesBMR"));
										bundle.putLong(FitbitApiFeature.MARGINAL_CALORIES, summary.getLong("marginalCalories"));
										bundle.putLong(FitbitApiFeature.ACTIVITY_CALORIES, summary.getLong("activityCalories"));
										
										long score = summary.getLong("activeScore");
										bundle.putLong(FitbitApiFeature.ACTIVE_SCORE, score);
										
										JSONArray activities = summary.getJSONArray("distances");
										
										long distance = 0;
										
										for (int i = 0; i < activities.length(); i++)
										{
											JSONObject activity = activities.getJSONObject(i);
											
											if ("total".equals(activity.getString("activity")))
												distance = activity.getLong("distance");
										}
										
										bundle.putLong(FitbitApiFeature.TOTAL_DISTANCE, distance);
	
										JSONObject goals = body.getJSONObject("goals");
										
										long goalDistance = goals.getLong("distance");
										long goalSteps = goals.getLong("steps");
										long goalCalories = goals.getLong("caloriesOut");
	
										bundle.putLong(FitbitApiFeature.GOAL_DISTANCE, goalDistance);
										bundle.putLong(FitbitApiFeature.GOAL_STEPS, goalSteps);
										bundle.putLong(FitbitApiFeature.GOAL_CALORIES, goalCalories);
	
										bundle.putDouble(FitbitApiFeature.GOAL_DISTANCE_RATIO, (double) distance / (double) goalDistance);
										bundle.putDouble(FitbitApiFeature.GOAL_STEPS_RATIO, (double) steps / (double) goalSteps);
										bundle.putDouble(FitbitApiFeature.GOAL_CALORIES_RATIO, (double) caloriesOut / (double) goalCalories);
										
										me.transmitData(context, bundle);
									} 
									catch (JSONException e) 
									{
					         			LogManager.getInstance(context).logException(e);
									}
									catch (OAuthException e)
									{
					         			LogManager.getInstance(context).logException(e);
									}
								}
							};
							
							Thread t = new Thread(r);
							t.start();
						}
					}
				}
				
				return true;
			}
		}

		return false;
	}

	protected boolean defaultEnabled() 
	{
		return false;
	}
	
	private void fetchAuth(Context context)
	{
        Intent intent = new Intent(context, OAuthActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		intent.putExtra(OAuthActivity.CONSUMER_KEY, CONSUMER_KEY);
		intent.putExtra(OAuthActivity.CONSUMER_SECRET, CONSUMER_SECRET);
		intent.putExtra(OAuthActivity.REQUESTER, "fitbit");
		intent.putExtra(OAuthActivity.CALLBACK_URL, "http://pr-oauth/oauth/fitbit");
		
		context.startActivity(intent);
	}

	public PreferenceScreen preferenceScreen(final PreferenceActivity activity)
	{
		final PreferenceScreen screen = super.preferenceScreen(activity);

		final SharedPreferences prefs = Probe.getPreferences(activity);

		String token = prefs.getString("oauth_fitbit_token", null);
		String secret = prefs.getString("oauth_fitbit_secret", null);

		final Preference authPreference = new Preference(activity);
		authPreference.setTitle(R.string.title_authenticate_fitbit_probe);
		authPreference.setSummary(R.string.summary_authenticate_fitbit_probe);

		final Preference logoutPreference = new Preference(activity);
		logoutPreference.setTitle(R.string.title_logout_fitbit_probe);
		logoutPreference.setSummary(R.string.summary_logout_fitbit_probe);

		final FitbitApiFeature me = this;

		authPreference.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			public boolean onPreferenceClick(Preference preference) 
			{
				me.fetchAuth(activity);
				
				screen.addPreference(logoutPreference);
				screen.removePreference(authPreference);

				return true;
			}
		});
		
		logoutPreference.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			public boolean onPreferenceClick(Preference preference) 
			{
				Editor e = prefs.edit();
				e.remove("oauth_fitbit_token");
				e.remove("oauth_fitbit_secret");
				e.commit();
				
				me._lastUpdate = 0;

				screen.addPreference(authPreference);
				screen.removePreference(logoutPreference);
				
				activity.runOnUiThread(new Runnable()
				{
					public void run() 
					{
						Toast.makeText(activity, activity.getString(R.string.toast_fitbit_logout), Toast.LENGTH_LONG).show();
					}
				});

				return true;
			}
		});
		
		if (token == null || secret == null)
			screen.addPreference(authPreference);
		else
			screen.addPreference(logoutPreference);
		
		return screen;
	}
	
	
	public String summarizeValue(Context context, Bundle bundle)
	{
		double steps = bundle.getDouble(FitbitApiFeature.STEPS);
		double progress = bundle.getDouble(FitbitApiFeature.GOAL_STEPS_RATIO) * 100;

		return String.format(context.getResources().getString(R.string.summary_fitbit), steps, progress);
	}
}
