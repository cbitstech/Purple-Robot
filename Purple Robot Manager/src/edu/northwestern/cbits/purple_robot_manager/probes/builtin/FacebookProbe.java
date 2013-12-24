package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
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

import com.facebook.AccessToken;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphObjectList;
import com.facebook.model.GraphUser;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.probes.FacebookLoginActivity;
import edu.northwestern.cbits.purple_robot_manager.calibration.FacebookCalibrationHelper;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class FacebookProbe extends Probe
{
	private static final boolean DEFAULT_ENABLED = false;
	private static final boolean DEFAULT_ENCRYPT = true;

	public static final String TOKEN = "facebook_auth_token";

	protected static final String HOUR_COUNT = "HOUR_COUNT";

	private long _lastCheck = 0;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.FacebookProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_facebook_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_external_services_category);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_facebook_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_facebook_enabled", false);
		
		e.commit();
	}

	public boolean isEnabled(final Context context)
	{
		final SharedPreferences prefs = Probe.getPreferences(context);

		if (super.isEnabled(context))
		{
			final long now = System.currentTimeMillis();

			if (prefs.getBoolean("config_probe_facebook_enabled", FacebookProbe.DEFAULT_ENABLED))
			{
				synchronized(this)
				{
					long freq = Long.parseLong(prefs.getString("config_probe_facebook_frequency", Probe.DEFAULT_FREQUENCY));
					final boolean doHash = prefs.getBoolean("config_probe_facebook_encrypt_data", FacebookProbe.DEFAULT_ENCRYPT);
					
					if (now - this._lastCheck  > freq)
					{
						FacebookCalibrationHelper.check(context);

						String token = prefs.getString(FacebookProbe.TOKEN, "");

						if (token != null && token.trim().length() > 0)
						{
							final FacebookProbe me = this;
		
							AccessToken accessToken = AccessToken.createFromExistingAccessToken(token, null, null, null, null);
							
							me._lastCheck = now;

							Session.openActiveSessionWithAccessToken(context, accessToken, new Session.StatusCallback()
							{
								public void call(final Session session, SessionState state, Exception exception) 
								{
									Request friendRequest = Request.newMyFriendsRequest(session, new Request.GraphUserListCallback() 
									{
										public void onCompleted(List<GraphUser> users, Response response) 
										{
											if (users == null)
												return;
												
											final long mostRecent = prefs.getLong("config_probe_facebook_recent", 0);
				
											try
											{
												final EncryptionManager em = EncryptionManager.getInstance();
					
												final Bundle bundle = new Bundle();
												bundle.putString("PROBE", me.name(context));
												bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
												bundle.putInt("FRIEND_COUNT", users.size());

												final ArrayList<GraphObject> items = new ArrayList<GraphObject>();

												Request statuses = new Request(session, "/me/statuses", null, HttpMethod.GET, new Request.Callback() 
												{
													@SuppressLint("SimpleDateFormat")
													public void onCompleted(Response response) 
													{
														try
														{
															final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

															GraphObject obj = response.getGraphObject();

															GraphObjectList<GraphObject> rawPosts = obj.getPropertyAsList("data", GraphObject.class);
															
															for (GraphObject object : rawPosts)
															{
																items.add(object);
															}

															Request posts = new Request(session, "/me/posts", null, HttpMethod.GET, new Request.Callback() 
															{
																public void onCompleted(Response response) 
																{
																	try
																	{
																		GraphObject obj = response.getGraphObject();
	
																		GraphObjectList<GraphObject> rawPosts = obj.getPropertyAsList("data", GraphObject.class);
																		
																		for (GraphObject object : rawPosts)
																		{
																			items.add(object);
																		}
	
																		Collections.sort(items, new Comparator<GraphObject>()
																		{
																			public int compare(GraphObject one, GraphObject two) 
																			{
																				String oneTime = one.getProperty("updated_time").toString();
																				String twoTime = two.getProperty("updated_time").toString();
																				
																				return oneTime.compareTo(twoTime);
																			}
																		});
																		
																		long newRecent = mostRecent;
																		int hour = 0;
	
																		for (GraphObject object : items)
																		{
																			try 
																			{
																				Object createdTime = object.getProperty("created_time");
																				
																				if (createdTime != null)
																				{
																					Date created = sdf.parse(createdTime.toString());
																					
																					long postTime = created.getTime();
																					
																					if (now - postTime < 60 * 60 * 1000)
																						hour += 1;
																					
																					if (postTime > mostRecent)
																					{
																						Object o = object.getProperty("message");
	
																						if (o != null)
																						{
																							Bundle eventBundle = new Bundle();
																							eventBundle.putString("PROBE", FacebookEventsProbe.PROBE_NAME);
																							eventBundle.putLong("TIMESTAMP", postTime / 1000);
																							eventBundle.putString("TYPE", object.getProperty("type").toString());
					
																							String message = o.toString();
																							
																							try 
																							{	
																								if (doHash)
																									message = em.encryptString(context, message);
						
																								eventBundle.putString("MESSAGE", message);
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
						
																							eventBundle.putBoolean("IS_OBFUSCATED", doHash);
																							me.transmitData(context, eventBundle);
																							
																							if (postTime > newRecent)
																								newRecent = postTime;
																						}
																					}
																				}
																			} 
																			catch (ParseException e) 
																			{
																				LogManager.getInstance(context).logException(e);
																			}
																			catch (NullPointerException e) 
																			{
																				LogManager.getInstance(context).logException(e);
																			}
																		}
																		
																		Editor e = prefs.edit();
																		e.putLong("config_probe_facebook_recent", newRecent);
																		e.commit();
				
																		bundle.putInt(FacebookProbe.HOUR_COUNT, hour);
																		
																		me.transmitData(context, bundle);
																	}
																	catch (NullPointerException e)
																	{
																		LogManager.getInstance(context).logException(e);
																	}
																}
															});
															
															posts.executeAsync();
														}
														catch (NullPointerException e)
														{
															LogManager.getInstance(context).logException(e);
														}
													}
												});
												
												statuses.executeAsync();
											}
											catch (Exception e)
											{
												LogManager.getInstance(context).logException(e);
											}
										}
									});
									
							        Bundle params = new Bundle();
							        params.putString("fields", "id");
							        friendRequest.setParameters(params);

    								friendRequest.executeAsync();
								}
							});
						}
					}
				}

				return true;
			}
		}

		return false;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		double count = bundle.getDouble("HOUR_COUNT", 0);
		double friends = bundle.getDouble("FRIEND_COUNT", 0);
		
		return String.format(context.getResources().getString(R.string.facebook_count_desc), (int) count, (int) friends);
	}

	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);

		long freq = Long.parseLong(prefs.getString("config_probe_facebook_frequency", Probe.DEFAULT_FREQUENCY));
		map.put(Probe.PROBE_FREQUENCY, freq);
		
		boolean hash = prefs.getBoolean("config_probe_facebook_encrypt_data", FacebookProbe.DEFAULT_ENCRYPT);
		map.put(Probe.ENCRYPT_DATA, hash);

		return map;
	}

	public void updateFromMap(Context context, Map<String, Object> params) 
	{
/*		super.updateFromMap(context, params);
		
		if (params.containsKey(Probe.PROBE_FREQUENCY))
		{
			Object frequency = params.get(Probe.PROBE_FREQUENCY);
			
			if (frequency instanceof Long)
			{
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putString("config_probe_communication_event_frequency", frequency.toString());
				e.commit();
			}
		}

		if (params.containsKey(Probe.HASH_DATA))
		{
			Object hash = params.get(Probe.HASH_DATA);
			
			if (hash instanceof Boolean)
			{
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putBoolean("config_probe_communication_event_hash_data", ((Boolean) hash).booleanValue());
				e.commit();
			}
		} */
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(final PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_facebook_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_facebook_enabled");
		enabled.setDefaultValue(FacebookProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_facebook_frequency");
		duration.setEntryValues(R.array.probe_low_frequency_values);
		duration.setEntries(R.array.probe_low_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);
		duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

		screen.addPreference(duration);

		CheckBoxPreference encrypt = new CheckBoxPreference(activity);
		encrypt.setKey("config_probe_facebook_encrypt_data");
		encrypt.setDefaultValue(FacebookProbe.DEFAULT_ENCRYPT);
		encrypt.setTitle(R.string.config_probe_facebook_encrypt_title);
		encrypt.setSummary(R.string.config_probe_facebook_encrypt_summary);

		screen.addPreference(encrypt);

        Preference calibrate = new Preference(activity);
        calibrate.setTitle(R.string.config_probe_calibrate_title);
        calibrate.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference pref) 
            {
        		Intent intent = new Intent(activity, FacebookLoginActivity.class);
                activity.startActivity(intent);

                return true;
            }
        });
        
        screen.addPreference(calibrate);

		return screen;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO Auto-generated method stub
	}
}
