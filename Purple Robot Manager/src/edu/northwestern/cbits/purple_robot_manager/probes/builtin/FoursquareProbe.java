package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
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
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.OAuthActivity;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class FoursquareProbe extends Probe
{
	public static final String CONSUMER_KEY = "LPF5BLAFJNO1MRGULIOYFKBLWMGQTFAR3WPHZJ1JLDD24S25";
	public static final String CONSUMER_SECRET = "O1N4EAXORJSLT50PSUOSYYIONS0MCQMWAKK1I1ZLSP0TIKGU";
	public static final String CALLBACK = "http://purple.robot.com/oauth/foursquare";

	static final boolean DEFAULT_ENABLED = false;
	private static final boolean DEFAULT_PULL_ACTIVITY = false;

	protected static final String HOUR_COUNT = "HOUR_COUNT";
 	private static final String PULL_ACTIVITY = "config_probe_foursquare_pull_activity";

	private long _lastCheck = 0;
	
	private String _token = null;
	private String _secret = null;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.FoursquareProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_foursquare_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_external_services_category);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_foursquare_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_foursquare_enabled", false);
		
		e.commit();
	}

	public boolean isEnabled(final Context context)
	{
		final SharedPreferences prefs = Probe.getPreferences(context);

		if (super.isEnabled(context))
		{
			final long now = System.currentTimeMillis();

			if (prefs.getBoolean("config_probe_foursquare_enabled", FoursquareProbe.DEFAULT_ENABLED))
			{
				synchronized(this)
				{
					long freq = Long.parseLong(prefs.getString("config_probe_foursquare_frequency", Probe.DEFAULT_FREQUENCY));
					final boolean pullActivity = prefs.getBoolean("config_probe_foursquare_pull_activity", FoursquareProbe.DEFAULT_PULL_ACTIVITY);

					if (now - this._lastCheck  > freq)
					{
						final FoursquareProbe me = this;

						this._token = prefs.getString("oauth_foursquare_token", null);
						this._secret = prefs.getString("oauth_foursquare_secret", null);

	    				final String title = context.getString(R.string.title_foursquare_check);
	    				final SanityManager sanity = SanityManager.getInstance(context);

	        			if (pullActivity && (this._token == null || this._secret == null))
	        			{
	        				String message = context.getString(R.string.message_foursquare_check);
	        				
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

							if (pullActivity)
							{
								Token accessToken = new Token(this._token, this._secret);
								
			                	ServiceBuilder builder = new ServiceBuilder();
			                	builder = builder.provider(TwitterApi.class);
			                	builder = builder.apiKey(FoursquareProbe.CONSUMER_KEY);
			                	builder = builder.apiSecret(FoursquareProbe.CONSUMER_SECRET);
			                	
			                	final OAuthService service = builder.build();
			                	
								final OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.foursquare.com/v2/users/self/checkins?limit=250&sort=newestfirst&v=20130815&oauth_token=" + this._token);
								service.signRequest(accessToken, request);
		
								Runnable r = new Runnable()
								{
									public void run() 
									{
										try 
										{
											SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy", Locale.ENGLISH);
											sdf.setLenient(true);
	
											long mostRecent = prefs.getLong("config_foursquare_most_recent", 0);
											long newRecent = 0;
											
											Response response = request.send();
											JSONObject checkins = new JSONObject(response.getBody());
											
											if (checkins.has("response"))
											{
												JSONArray items = checkins.getJSONObject("response").getJSONObject("checkins").getJSONArray("items");

												for (int i = (items.length() - 1); i >= 0; i--)
												{
													JSONObject item = items.getJSONObject(i);

													long created = item.getLong("createdAt") * 1000;
	
													if (created > mostRecent)
													{
														if (created > newRecent)
															newRecent = created;
														
														Bundle eventBundle = new Bundle();
														eventBundle.putString("PROBE", me.name(context));
														eventBundle.putLong("TIMESTAMP", created / 1000);

														eventBundle.putString("CHECKIN_SOURCE", item.getJSONObject("source").getString("name"));

														if (item.has("venue"))
														{
															JSONObject venue = item.getJSONObject("venue");
															JSONObject location = venue.getJSONObject("location");
															
															eventBundle.putDouble("LATITUDE", location.getDouble("lat"));
															eventBundle.putDouble("LONGITUDE", location.getDouble("lng"));
															
															if (location.has("address"))
																eventBundle.putString("ADDRESS", location.getString("address"));
															
															if (location.has("city"))
																eventBundle.putString("CITY", location.getString("city"));

															if (location.has("state"))
																eventBundle.putString("STATE", location.getString("state"));
															
															if (location.has("postalCode"))
																eventBundle.putString("POSTAL_CODE", location.getString("postalCode"));
															
															eventBundle.putString("COUNTRY", location.getString("cc"));
	
															eventBundle.putString("VENUE_ID", venue.getString("id"));
															
															if (venue.has("url"))
																eventBundle.putString("VENUE_URL", venue.getString("url"));
															
															eventBundle.putString("VENUE_NAME", venue.getString("name"));
															
															JSONArray categories = venue.getJSONArray("categories");
															
															for (int j = 0; j < categories.length(); j++)
															{
																JSONObject category = categories.getJSONObject(j);
																
																if (category.getBoolean("primary"))
																	eventBundle.putString("VENUE_CATEGORY", category.getString("name"));
															}

															me.transmitData(context, eventBundle);
														}
													}
												}

												Editor e = prefs.edit();
												e.putLong("config_foursquare_most_recent", newRecent);
												e.commit();
											}
										} 
										catch (IllegalStateException e) 
										{
											e.printStackTrace();
										} 
										catch (OAuthException e)
										{
											e.printStackTrace();
										} 
										catch (JSONException e) 
										{
											e.printStackTrace();
										} 
									}
								};
								
								Thread t = new Thread(r);
								t.start();
							}
						}
	        			
						me._lastCheck = now;
					}
				}

				return true;
			}
		}

		return false;
	}

	private void fetchAuth(Context context)
	{
        Intent intent = new Intent(context, OAuthActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		intent.putExtra(OAuthActivity.CONSUMER_KEY, CONSUMER_KEY);
		intent.putExtra(OAuthActivity.CONSUMER_SECRET, CONSUMER_SECRET);
		intent.putExtra(OAuthActivity.REQUESTER, "foursquare");
		intent.putExtra(OAuthActivity.CALLBACK_URL, "http://purple.robot.com/oauth/foursquare");
		
		context.startActivity(intent);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		String venue = bundle.getString("VENUE_NAME");
		
		return context.getString(R.string.summary_foursquare_checkin_summary, venue);
	}
	
	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);

		long freq = Long.parseLong(prefs.getString("config_probe_foursquare_frequency", Probe.DEFAULT_FREQUENCY));
		map.put(Probe.PROBE_FREQUENCY, freq);
		
		boolean pullActivity = prefs.getBoolean("config_probe_foursquare_pull_activity", FoursquareProbe.DEFAULT_PULL_ACTIVITY);
		map.put(FoursquareProbe.PULL_ACTIVITY, pullActivity);

		return map;
	}

	public void updateFromMap(Context context, Map<String, Object> params) 
	{
		super.updateFromMap(context, params);
		
		if (params.containsKey(Probe.PROBE_FREQUENCY))
		{
			Object frequency = params.get(Probe.PROBE_FREQUENCY);
			
			if (frequency instanceof Long)
			{
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putString("config_probe_foursquare_frequency", frequency.toString());
				e.commit();
			}
		}
		
		if (params.containsKey(FoursquareProbe.PULL_ACTIVITY))
		{
			Object pull = params.get(FoursquareProbe.PULL_ACTIVITY);
			
			if (pull instanceof Boolean)
			{
				Boolean pullBoolean = (Boolean) pull;
				
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putBoolean(FoursquareProbe.PULL_ACTIVITY, pullBoolean.booleanValue());
				e.commit();
			}
		}
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(final PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		final PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_foursquare_probe_desc);

		final SharedPreferences prefs = Probe.getPreferences(activity);

		String token = prefs.getString("oauth_foursquare_token", null);
		String secret = prefs.getString("oauth_foursquare_secret", null);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_foursquare_enabled");
		enabled.setDefaultValue(FoursquareProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_foursquare_frequency");
		duration.setEntryValues(R.array.probe_low_frequency_values);
		duration.setEntries(R.array.probe_low_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);
		duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

		screen.addPreference(duration);

		CheckBoxPreference pull = new CheckBoxPreference(activity);
		pull.setKey("config_probe_foursquare_pull_activity");
		pull.setDefaultValue(FoursquareProbe.DEFAULT_PULL_ACTIVITY);
		pull.setTitle(R.string.config_probe_foursquare_pull_title);
		pull.setSummary(R.string.config_probe_foursquare_pull_summary);

		screen.addPreference(pull);

		final Preference authPreference = new Preference(activity);
		authPreference.setTitle(R.string.title_authenticate_foursquare_probe);
		authPreference.setSummary(R.string.summary_authenticate_foursquare_probe);

		final FoursquareProbe me = this;
		
		final Preference logoutPreference = new Preference(activity);
		logoutPreference.setTitle(R.string.title_logout_foursquare_probe);
		logoutPreference.setSummary(R.string.summary_logout_foursquare_probe);

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
				e.remove("oauth_foursquare_token");
				e.remove("oauth_foursquare_secret");
				e.commit();
				
				screen.addPreference(authPreference);
				screen.removePreference(logoutPreference);
				
				activity.runOnUiThread(new Runnable()
				{
					public void run() 
					{
						Toast.makeText(activity, activity.getString(R.string.toast_foursquare_logout), Toast.LENGTH_LONG).show();
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

	public static void annotate(Context context, Bundle bundle) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		if (prefs.getBoolean("config_probe_foursquare_enabled", FoursquareProbe.DEFAULT_ENABLED))
		{
			String urlString = "https://api.foursquare.com/v2/venues/search?client_id=" + FoursquareProbe.CONSUMER_KEY + "&client_secret=" + FoursquareProbe.CONSUMER_SECRET + "&v=20130815&ll=";
			
			double latitude = bundle.getDouble(LocationProbe.LATITUDE);
			double longitude = bundle.getDouble(LocationProbe.LONGITUDE);
			
			urlString += latitude + "," + longitude;
			
			try 
			{
				URL u = new URL(urlString);

				InputStream in = u.openStream();

				String jsonString = IOUtils.toString(in);
				
				in.close();
				
				JSONObject respJson = new JSONObject(jsonString);

				JSONArray venues = respJson.getJSONObject("response").getJSONArray("venues");
				
				if (venues.length() > 0)
				{
					JSONObject venue = venues.getJSONObject(0);
					
					bundle.putString("FOURSQUARE_NAME", venue.getString("name"));

					JSONObject location = venue.getJSONObject("location");
					
					if (location.has("address"))
						bundle.putString("FOURSQUARE_ADDRESS", location.getString("address"));
					
					if (location.has("city"))
						bundle.putString("FOURSQUARE_CITY", location.getString("city"));

					if (location.has("state"))
						bundle.putString("FOURSQUARE_STATE", location.getString("state"));
					
					if (location.has("postalCode"))
						bundle.putString("FOURSQUARE_POSTAL_CODE", location.getString("postalCode"));
					
					bundle.putString("FOURSQUARE_COUNTRY", location.getString("cc"));
					bundle.putString("FOURSQUARE_VENUE_ID", venue.getString("id"));
					
					if (venue.has("url"))
						bundle.putString("FOURSQUARE_VENUE_URL", venue.getString("url"));
					
					JSONArray categories = venue.getJSONArray("categories");
					
					for (int j = 0; j < categories.length(); j++)
					{
						JSONObject category = categories.getJSONObject(j);
						
						if (category.getBoolean("primary"))
							bundle.putString("FOURSQUARE_VENUE_CATEGORY", category.getString("name"));
					}
				}
			}
			catch (MalformedURLException e) 
			{
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			} 
			catch (JSONException e) 
			{
				e.printStackTrace();
			}
		}
	}
}
