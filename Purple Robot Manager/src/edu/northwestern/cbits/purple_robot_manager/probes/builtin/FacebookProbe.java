package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphObjectList;
import com.facebook.model.GraphUser;

import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.calibration.FacebookCalibrationHelper;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class FacebookProbe extends Probe
{
	private static final boolean DEFAULT_ENABLED = false;
	private static final boolean DEFAULT_RETRIEVE = false;
	private static final boolean DEFAULT_ENCRYPT = true;

	public static final String TOKEN = "facebook_auth_token";

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
		return context.getResources().getString(R.string.probe_personal_info_category);
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
					boolean doHash = prefs.getBoolean("config_probe_facebook_hash_data", Probe.DEFAULT_HASH_DATA);
					
					if (now - this._lastCheck  > freq)
					{
						FacebookCalibrationHelper.check(context);
		
						if (prefs.contains(FacebookProbe.TOKEN))
						{
							String token = prefs.getString(FacebookProbe.TOKEN, "");
							
							final FacebookProbe me = this;
		
							AccessToken accessToken = AccessToken.createFromExistingAccessToken(token, null, null, null, null);
							
							Session.openActiveSessionWithAccessToken(context, accessToken, new Session.StatusCallback()
							{
								public void call(final Session session, SessionState state, Exception exception) 
								{
									Request.newMyFriendsRequest(session, new Request.GraphUserListCallback() 
									{
										public void onCompleted(List<GraphUser> users, Response response) 
										{
											long mostRecent = prefs.getLong("config_probe_facebook_recent", 0);
											long newRecent = mostRecent;
				
											try
											{
												EncryptionManager em = EncryptionManager.getInstance();
					
												final Bundle bundle = new Bundle();
												bundle.putString("PROBE", me.name(context));
												bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
												bundle.putInt("FRIEND_COUNT", users.size());
			
												Request statuses = new Request(session, "/me/posts", null, HttpMethod.GET, new Request.Callback() 
												{
													public void onCompleted(Response response) 
													{
														GraphObject obj = response.getGraphObject();
														
														GraphObjectList<GraphObject> posts = obj.getPropertyAsList("data", GraphObject.class);
														
														for (GraphObject object : posts)
														{
															Log.e("PR", "GOT: " + object.getProperty("message"));
														}
														
														me.transmitData(context, bundle);
													}
												});
												
												statuses.executeAsync();
											}
											catch (Exception e)
											{
												LogManager.getInstance(context).logException(e);
											}
				
											me._lastCheck = now;
				
											Editor e = prefs.edit();
											e.putLong("config_probe_facebook_recent", newRecent);
											e.commit();
										}
									}).executeAsync();
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

	/* public String summarizeValue(Context context, Bundle bundle)
	{
		String name = bundle.getString(FacebookProbe.NAME);
		String type = bundle.getString(FacebookProbe.TYPE);
		
		if (FacebookProbe.TYPE_PHONE.equals(type))
			type = context.getResources().getString(R.string.summary_communication_events_phone_type);
		else
			type = context.getResources().getString(R.string.summary_communication_events_sms_type);

		long timestamp = (long) bundle.getDouble(FacebookProbe.TIMESTAMP);
		
		SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm");
		
		return String.format(context.getResources().getString(R.string.summary_communication_events_probe), type, name, format.format(new Date(timestamp)));
	} */

	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);

		long freq = Long.parseLong(prefs.getString("config_probe_facebook_frequency", Probe.DEFAULT_FREQUENCY));
		map.put(Probe.PROBE_FREQUENCY, freq);
		
		boolean hash = prefs.getBoolean("config_probe_facebook_hash_data", Probe.DEFAULT_HASH_DATA);
		map.put(Probe.HASH_DATA, hash);

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

		CheckBoxPreference hash = new CheckBoxPreference(activity);
		hash.setKey("config_probe_facebook_hash_data");
		hash.setDefaultValue(Probe.DEFAULT_HASH_DATA);
		hash.setTitle(R.string.config_probe_facebook_hash_title);
		hash.setSummary(R.string.config_probe_facebook_hash_summary);

		screen.addPreference(hash);

/*		
 		CheckBoxPreference retrieve = new CheckBoxPreference(activity);
		retrieve.setKey("config_probe_facebook_retrieve_data");
		retrieve.setDefaultValue(FacebookProbe.DEFAULT_RETRIEVE);
		retrieve.setTitle(R.string.config_probe_facebook_retrieve_title);
		retrieve.setSummary(R.string.config_probe_facebook_retrieve_summary);
		
		retrieve.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			public boolean onPreferenceChange(Preference arg0, Object newValue) 
			{
				Boolean b = (Boolean) newValue;
				
				if (b)
				{
					AlertDialog.Builder builder = new AlertDialog.Builder(activity);
					builder = builder.setTitle(R.string.config_probe_facebook_retrieve_warning_title);
					builder = builder.setMessage(R.string.config_probe_facebook_retrieve_warning);
					builder = builder.setPositiveButton(R.string.button_continue, null);
					
					builder.create().show();
				}
					
				return true;
			}
		});

		screen.addPreference(retrieve);

		*/

		CheckBoxPreference encrypt = new CheckBoxPreference(activity);
		encrypt.setKey("config_probe_facebook_encrypt_data");
		encrypt.setDefaultValue(FacebookProbe.DEFAULT_ENCRYPT);
		encrypt.setTitle(R.string.config_probe_facebook_encrypt_title);
		encrypt.setSummary(R.string.config_probe_facebook_encrypt_summary);

		screen.addPreference(encrypt);

		return screen;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO Auto-generated method stub
	}
}
