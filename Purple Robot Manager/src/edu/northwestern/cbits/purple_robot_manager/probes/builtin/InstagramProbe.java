package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

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
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.OAuthActivity;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.oauth.InstagramApi;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class InstagramProbe extends Probe
{
	public static final String CONSUMER_KEY = "47f0718396bc4bafbfad940c7e15ab3b";
	public static final String CONSUMER_SECRET = "f39d3b9a33b347a2bda462ef5f373935";
	public static final String CALLBACK = "http://purple.robot.com/oauth/instagram";

	private static final boolean DEFAULT_ENABLED = false;
	private static final boolean DEFAULT_ENCRYPT = false;

	protected static final String HOUR_COUNT = "HOUR_COUNT";

	private long _lastCheck = 0;
	
	private String _token = null;
	private String _secret = null;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.InstagramProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_instagram_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_external_services_category);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_instagram_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_instagram_enabled", false);
		
		e.commit();
	}

	public boolean isEnabled(final Context context)
	{
		final SharedPreferences prefs = Probe.getPreferences(context);

		if (super.isEnabled(context))
		{
			final long now = System.currentTimeMillis();

			if (prefs.getBoolean("config_probe_instagram_enabled", InstagramProbe.DEFAULT_ENABLED))
			{
				synchronized(this)
				{
					long freq = Long.parseLong(prefs.getString("config_probe_instagram_frequency", Probe.DEFAULT_FREQUENCY));
					final boolean doEncrypt = prefs.getBoolean("config_probe_instagram_encrypt_data", InstagramProbe.DEFAULT_ENCRYPT);
					
					final EncryptionManager em = EncryptionManager.getInstance();
					
					if (now - this._lastCheck  > freq)
					{
						final InstagramProbe me = this;

						this._token = prefs.getString("oauth_instagram_token", null);
						this._secret = prefs.getString("oauth_instagram_secret", null);

	    				final String title = context.getString(R.string.title_instagram_check);
	    				final SanityManager sanity = SanityManager.getInstance(context);

	        			if (this._token == null || this._secret == null)
	        			{
	        				String message = context.getString(R.string.message_instagram_check);
	        				
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
							
							Token accessToken = new Token(this._token, this._secret);
							
		                	ServiceBuilder builder = new ServiceBuilder();
		                	builder = builder.provider(InstagramApi.class);
		                	builder = builder.apiKey(InstagramProbe.CONSUMER_KEY);
		                	builder = builder.apiSecret(InstagramProbe.CONSUMER_SECRET);
		                	
		                	final OAuthService service = builder.build();
		                	
							final OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.instagram.com/v1/users/self/feed");
							service.signRequest(accessToken, request);
	
							Runnable r = new Runnable()
							{
								public void run() 
								{
									try 
									{
										long mostRecent = prefs.getLong("config_instagram_most_recent", 0);
										
										Response response = request.send();

										JSONObject root = new JSONObject(response.getBody());
										
										JSONArray data = root.getJSONArray("data");
										
										for (int i = (data.length() - 1); i >= 0; i--)
										{
											JSONObject post = data.getJSONObject(i);
											
											long postTime = post.getLong("created_time") * 1000;

											if (postTime > mostRecent)
											{
												Bundle eventBundle = new Bundle();
												eventBundle.putString("PROBE", me.name(context));
												eventBundle.putLong("TIMESTAMP", postTime / 1000);
												
												JSONObject location = post.getJSONObject("location");
												
												if (location != null)
												{
													eventBundle.putDouble("LATITUDE", location.getDouble("latitude"));
													eventBundle.putDouble("LONGITUDE", location.getDouble("longitude"));
													
													if (location.has("name"))
													{
														String name = location.getString("name");
														
														if (name != null)
															eventBundle.putString("LOCATION_NAME", name);
													}
												}
												
												eventBundle.putString("LINK", post.getString("link"));
												eventBundle.putString("TYPE", post.getString("type"));
												eventBundle.putString("FILTER", post.getString("filter"));
												
												if (post.has("caption"))
												{
													String caption = post.getJSONObject("caption").getString("text");
													
													if (caption != null)
													{
														if (doEncrypt)
															caption = em.encryptString(context, caption);
	
														eventBundle.putString("CAPTION", caption);
													}
												}
												
												eventBundle.putBoolean("IS_OBFUSCATED", doEncrypt);
												me.transmitData(context, eventBundle);
											}
										}
										
										Editor e = prefs.edit();
										e.putLong("config_instagram_most_recent", System.currentTimeMillis());
										e.commit();
									} 
									catch (JSONException e) 
									{
						     			LogManager.getInstance(context).logException(e);
									}
									catch (OAuthException e)
									{
						     			LogManager.getInstance(context).logException(e);
									} 
									catch (IllegalBlockSizeException e) 
									{
						     			LogManager.getInstance(context).logException(e);
									} 
									catch (BadPaddingException e) 
									{
						     			LogManager.getInstance(context).logException(e);
									} 
									catch (UnsupportedEncodingException e) 
									{
						     			LogManager.getInstance(context).logException(e);
									} 
								}
							};
							
							Thread t = new Thread(r);
							t.start();
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
		intent.putExtra(OAuthActivity.REQUESTER, "instagram");
		intent.putExtra(OAuthActivity.CALLBACK_URL, CALLBACK);
		
		context.startActivity(intent);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		if (bundle.containsKey("CAPTION"))
			return bundle.getString("CAPTION") + " (" + bundle.getString("TYPE") + ")";

		return bundle.getString("TYPE");
	}
	
	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);

		long freq = Long.parseLong(prefs.getString("config_probe_instagram_frequency", Probe.DEFAULT_FREQUENCY));
		map.put(Probe.PROBE_FREQUENCY, freq);
		
		boolean hash = prefs.getBoolean("config_probe_instagram_encrypt_data", InstagramProbe.DEFAULT_ENCRYPT);
		map.put(Probe.ENCRYPT_DATA, hash);

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
				
				e.putString("config_probe_instagram_frequency", frequency.toString());
				e.commit();
			}
		}
		
		if (params.containsKey(Probe.ENCRYPT_DATA))
		{
			Object encrypt = params.get(Probe.ENCRYPT_DATA);
			
			if ( encrypt instanceof Boolean)
			{
				Boolean encryptBoolean = (Boolean)  encrypt;
				
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putBoolean("config_probe_instagram_encrypt_data", encryptBoolean.booleanValue());
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
		screen.setSummary(R.string.summary_instagram_probe_desc);

		final SharedPreferences prefs = Probe.getPreferences(activity);

		String token = prefs.getString("oauth_instagram_token", null);
		String secret = prefs.getString("oauth_instagram_secret", null);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_instagram_enabled");
		enabled.setDefaultValue(InstagramProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_instagram_frequency");
		duration.setEntryValues(R.array.probe_low_frequency_values);
		duration.setEntries(R.array.probe_low_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);
		duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

		screen.addPreference(duration);

		CheckBoxPreference encrypt = new CheckBoxPreference(activity);
		encrypt.setKey("config_probe_instagram_encrypt_data");
		encrypt.setDefaultValue(InstagramProbe.DEFAULT_ENCRYPT);
		encrypt.setTitle(R.string.config_probe_instagram_encrypt_title);
		encrypt.setSummary(R.string.config_probe_instagram_encrypt_summary);

		screen.addPreference(encrypt);

		final Preference authPreference = new Preference(activity);
		authPreference.setTitle(R.string.title_authenticate_instagram_probe);
		authPreference.setSummary(R.string.summary_authenticate_instagram_probe);

		final InstagramProbe me = this;
		
		final Preference logoutPreference = new Preference(activity);
		logoutPreference.setTitle(R.string.title_logout_instagram_probe);
		logoutPreference.setSummary(R.string.summary_logout_instagram_probe);

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
				e.remove("oauth_instagram_token");
				e.remove("oauth_instagram_secret");
				e.commit();
				
				screen.addPreference(authPreference);
				screen.removePreference(logoutPreference);
				
				activity.runOnUiThread(new Runnable()
				{
					public void run() 
					{
						Toast.makeText(activity, activity.getString(R.string.toast_instagram_logout), Toast.LENGTH_LONG).show();
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
}
