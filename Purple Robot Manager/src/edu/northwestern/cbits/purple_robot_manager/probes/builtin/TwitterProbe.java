package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class TwitterProbe extends Probe
{
	private static final boolean DEFAULT_ENABLED = false;
	private static final boolean DEFAULT_ENCRYPT = true;

	protected static final String HOUR_COUNT = "HOUR_COUNT";

	private long _lastCheck = 0;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.TwitterProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_twitter_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_external_services_category);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_twitter_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_twitter_enabled", false);
		
		e.commit();
	}

	public boolean isEnabled(final Context context)
	{
		final SharedPreferences prefs = Probe.getPreferences(context);

		if (super.isEnabled(context))
		{
			final long now = System.currentTimeMillis();

			if (prefs.getBoolean("config_probe_twitter_enabled", TwitterProbe.DEFAULT_ENABLED))
			{
				Log.e("PR", "GRABBING TWEETS");
				
				synchronized(this)
				{
					long freq = Long.parseLong(prefs.getString("config_probe_twitter_frequency", Probe.DEFAULT_FREQUENCY));
					final boolean doEncrypt = prefs.getBoolean("config_probe_twitter_encrypt_data", TwitterProbe.DEFAULT_ENCRYPT);
					
					if (false && now - this._lastCheck  > freq)
					{
						final TwitterProbe me = this;
						
						try
						{
							Twitter twitter = TwitterFactory.getSingleton();
							
						    Query query = new Query("source:twitter4j yusukey");
						    QueryResult result = twitter.search(query);
						    
						    for (Status status : result.getTweets()) 
						    {
						        Log.e("PR", "TWITTER @" + status.getUser().getScreenName() + ":" + status.getText());
						    }
							
						}
						catch (TwitterException e)
						{
							LogManager.getInstance(context).logException(e);
						}
						catch (IllegalStateException e)
						{
							LogManager.getInstance(context).logException(e);
						}

						me._lastCheck = now;
					}
				}

				return true;
			}
		}

		return false;
	}

/*	public String summarizeValue(Context context, Bundle bundle)
	{
		double count = bundle.getDouble("HOUR_COUNT", 0);
		double friends = bundle.getDouble("FRIEND_COUNT", 0);
		
		return String.format(context.getResources().getString(R.string.facebook_count_desc), (int) count, (int) friends);
	}
*/
	
	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);

		long freq = Long.parseLong(prefs.getString("config_probe_twitter_frequency", Probe.DEFAULT_FREQUENCY));
		map.put(Probe.PROBE_FREQUENCY, freq);
		
		boolean hash = prefs.getBoolean("config_probe_twitter_encrypt_data", TwitterProbe.DEFAULT_ENCRYPT);
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
		screen.setSummary(R.string.summary_twitter_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_twitter_enabled");
		enabled.setDefaultValue(TwitterProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_twitter_frequency");
		duration.setEntryValues(R.array.probe_low_frequency_values);
		duration.setEntries(R.array.probe_low_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);
		duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

		screen.addPreference(duration);

		CheckBoxPreference encrypt = new CheckBoxPreference(activity);
		encrypt.setKey("config_probe_facebook_encrypt_data");
		encrypt.setDefaultValue(TwitterProbe.DEFAULT_ENCRYPT);
		encrypt.setTitle(R.string.config_probe_twitter_encrypt_title);
		encrypt.setSummary(R.string.config_probe_twitter_encrypt_summary);

		screen.addPreference(encrypt);

/* TODO: Enable w/ optional OAuth...
 * 
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
*/
		return screen;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO Auto-generated method stub
	}
}
