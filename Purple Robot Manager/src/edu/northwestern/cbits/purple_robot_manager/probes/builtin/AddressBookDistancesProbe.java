package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.db.DistancesProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class AddressBookDistancesProbe extends Probe
{
	private static final boolean DEFAULT_ENABLED = false;
	private static final String DEFAULT_FREQUENCY = "3600000";

	private static final String NOW = "NOW";

	private long _lastCheck = 0;
	private double _lastLatitude = 100;
	private double _lastLongitude = 200;
	
	private LocationListener _listener = null;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.AddressBookDistancesProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_distances_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_personal_info_category);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_distances_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_distances_enabled", false);
		
		e.commit();
	}

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		if (super.isEnabled(context))
		{
			long now = System.currentTimeMillis();

			if (prefs.getBoolean("config_probe_distances_enabled", AddressBookDistancesProbe.DEFAULT_ENABLED))
			{
				
				HashMap<String, String> addresses = new HashMap<String, String>();

				long freq = Long.parseLong(prefs.getString("config_probe_distances_frequency", AddressBookDistancesProbe.DEFAULT_FREQUENCY));
				boolean doHash = prefs.getBoolean("config_probe_distances_hash_data", Probe.DEFAULT_HASH_DATA);

				synchronized(this)
				{
					final AddressBookDistancesProbe me = this;
					
					if (this._listener == null)
					{
						this._listener = new LocationListener()
						{
							public void onLocationChanged(Location location) 
							{
								me._lastLatitude = location.getLatitude();
								me._lastLongitude = location.getLongitude();
							}

							public void onProviderDisabled(String provider) 
							{

							}

							public void onProviderEnabled(String provider) 
							{

							}

							public void onStatusChanged(String provider, int status, Bundle extras) 
							{

							}
						};
						
						locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, this._listener);
					}
					else if (now - this._lastCheck  > freq && this._lastLatitude < 90 && this._lastLongitude < 180)
					{
						Bundle bundle = new Bundle();
						bundle.putString("PROBE", this.name(context));
						bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

						this._lastCheck = now;

						String selection = ContactsContract.Groups.TITLE + " LIKE ?";
						String[] args = { "Purple Robot%" };
	
						Cursor cursor = context.getContentResolver().query(ContactsContract.Groups.CONTENT_URI, null, selection, args, null);
	
						while (cursor.moveToNext()) 
						{
							String membersSelection = ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + " = ?";
							String[] membersArgs = { cursor.getString(cursor.getColumnIndex(ContactsContract.Groups._ID)) };
							String[] membersProjection = { ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID, ContactsContract.Contacts.DISPLAY_NAME };
							
							Cursor membersCursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, membersProjection, membersSelection, membersArgs, null);
							
							while (membersCursor.moveToNext())
							{
								int contactId = membersCursor.getInt(membersCursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID));
								String contactName = membersCursor.getString(membersCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
								
								String addressSelection = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
								String[] addressArgs = { "" + contactId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE };
	
					            String[] projection = new String[] { StructuredPostal.FORMATTED_ADDRESS, StructuredPostal.TYPE }; //,

								Cursor addressCursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, projection, addressSelection, addressArgs, null);
	
								while (addressCursor.moveToNext())
								{
									String address = addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.FORMATTED_ADDRESS));
									int type = addressCursor.getInt(addressCursor.getColumnIndex(StructuredPostal.TYPE));
									
									String label = context.getString(R.string.config_probe_distances_label_other);
									
									if (type == StructuredPostal.TYPE_HOME)
										label = context.getString(R.string.config_probe_distances_label_home);
									else if (type == StructuredPostal.TYPE_WORK)
										label = context.getString(R.string.config_probe_distances_label_work);
									
									label = contactName + ": " + label;
									
									if (doHash)
										label = EncryptionManager.getInstance().createHash(context, label);
									
									addresses.put(label, address);
								}
								
								addressCursor.close();
							}
							
							membersCursor.close();
						}
	
						cursor.close();
						
						Location here = new Location("Purple Robot");
						here.setLatitude(this._lastLatitude);
						here.setLongitude(this._lastLongitude);

						Bundle nowDistances = this.distancesForDays(context, here, addresses, 0, now, false);

						if (nowDistances != null)
							bundle.putBundle(AddressBookDistancesProbe.NOW, nowDistances);

						Bundle todayDistances = this.distancesForDays(context, here, addresses, 1, now, false);

						if (todayDistances != null)
							bundle.putBundle("TODAY_AVERAGE", todayDistances);

						Bundle weekDistances = this.distancesForDays(context, here, addresses, 7, now, false);

						if (weekDistances != null)
							bundle.putBundle("WEEK_AVERAGE", weekDistances);

						Bundle monthDistances = this.distancesForDays(context, here, addresses, 28, now, true);

						if (monthDistances != null)
							bundle.putBundle("MONTH_AVERAGE", monthDistances);

						this.transmitData(context, bundle);
					}
				}

				return true;
			}
		}
		
		if (this._listener != null)
		{
			locationManager.removeUpdates(this._listener);
			this._listener = null;
		}

		return false;
	}

	@SuppressLint("DefaultLocale")
	private Bundle distancesForDays(Context context, Location here, HashMap<String, String> addresses, long days, long now, boolean clear) 
	{
		long start = now - (days * 24 * 60 * 60 * 1000);
		
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Bundle bundle = new Bundle();

		if (days == 0)
		{
			for (String label : addresses.keySet())
			{
				String address = addresses.get(label).trim().toLowerCase().replace("\n", " ").replace("\r", " ");
				
				while (address.indexOf("  ") != -1)
					address = address.replace("  ", " ");
					
				String key = "Geocoded Location: " + address;
					
				String locationJson = prefs.getString(key, null);
				Location there = null;
	
				try 
				{
					if (locationJson == null)
					{
						Geocoder geo = new Geocoder(context);
						
						List<Address> matches = geo.getFromLocationName(address, 1);
						
						if (matches.size() > 0)
						{
							Address match = matches.get(0);
							
							JSONObject json = new JSONObject();
							json.put("latitude", match.getLatitude());
							json.put("longitude", match.getLongitude());
							
							locationJson = json.toString();
							
							Editor e = prefs.edit();
							e.putString(key, locationJson);
							e.commit();
							
							there = new Location("Purple Robot");
							there.setLatitude(match.getLatitude());
							there.setLongitude(match.getLongitude());
						}
						else
						{
							throw new Exception("Unable to find location for '"  + address + "'.");
						}
					} 
					else
					{
						JSONObject json = new JSONObject(locationJson);
	
						there = new Location("Purple Robot");
						there.setLatitude(json.getDouble("latitude"));
						there.setLongitude(json.getDouble("longitude"));
					}
				}
				catch (Throwable e) 
				{
					LogManager.getInstance(context).logException(e);
				}
				
				if (there != null)
				{
					float distance = here.distanceTo(there);
					
					bundle.putFloat(label, distance);

					ContentValues value = new ContentValues();
					value.put(DistancesProvider.NAME, label);
					value.put(DistancesProvider.DISTANCE, distance);
					value.put(DistancesProvider.TIMESTAMP, now);
						
					context.getContentResolver().insert(DistancesProvider.CONTENT_URI, value);
				}
			}
		}
		else
		{
			for (String label : addresses.keySet())
			{
				String testSelection = DistancesProvider.NAME + " = ? AND " + DistancesProvider.TIMESTAMP + " < ?";
				String[] testSelectionArgs = { label, "" + start };
				String[] testProjection = { DistancesProvider.DISTANCE, DistancesProvider.TIMESTAMP };
			
				Cursor testCursor = context.getContentResolver().query(DistancesProvider.CONTENT_URI, testProjection, testSelection, testSelectionArgs, null);
				
				boolean goOn = (testCursor.getCount() > 0);

				testCursor.close();
				
				if (goOn)
				{
					String selection = DistancesProvider.NAME + " = ? AND " + DistancesProvider.TIMESTAMP + " >= ? AND " + DistancesProvider.TIMESTAMP + " <= ?";
					String[] selectionArgs = { label, "" + start, "" + now };
					String[] projection = { DistancesProvider.DISTANCE, DistancesProvider.TIMESTAMP };
				
					Cursor cursor = context.getContentResolver().query(DistancesProvider.CONTENT_URI, projection, selection, selectionArgs, null);
					
					DescriptiveStatistics stats = new DescriptiveStatistics();
					
					while (cursor.moveToNext())
					{
						double distance = cursor.getDouble(cursor.getColumnIndex(DistancesProvider.DISTANCE));
						
						stats.addValue(distance);
					}
					
					cursor.close();
					
					if (stats.getN() > 0)
					{
						Bundle placeStats = new Bundle();
						
						placeStats.putDouble("MEAN", stats.getMean());
						placeStats.putDouble("MIN", stats.getMin());
						placeStats.putDouble("MAX", stats.getMax());
						placeStats.putDouble("STD_DEV", stats.getStandardDeviation());
						placeStats.putDouble("COUNT", stats.getN());
						
						bundle.putBundle(label, placeStats);
					}
				}
			}
			
			if (clear)
			{
				String deleteSelection = DistancesProvider.TIMESTAMP + " < ?";
				String[] deleteArgs = { "" + start };
				
				context.getContentResolver().delete(DistancesProvider.CONTENT_URI, deleteSelection, deleteArgs);
			}
		}
		
		if (bundle.keySet().size() == 0)
			return null;

		return bundle;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		float minDistance = Float.MAX_VALUE;
		String minLabel = null;
		
		Bundle now = bundle.getBundle(AddressBookDistancesProbe.NOW);
		
		for (String key : now.keySet())
		{
			float distance = now.getFloat(key);
			
			if (distance < minDistance)
			{
				minDistance = distance;
				minLabel = key;
			}
		}

		return context.getResources().getString(R.string.summary_distances_probe, minLabel, minDistance);
	}

	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);

		long freq = Long.parseLong(prefs.getString("config_probe_distance_frequency", AddressBookDistancesProbe.DEFAULT_FREQUENCY));
		map.put(Probe.PROBE_FREQUENCY, freq);
		
		boolean hash = prefs.getBoolean("config_probe_distances_hash_data", Probe.DEFAULT_HASH_DATA);
		map.put(Probe.HASH_DATA, hash);

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
				
				e.putString("config_probe_distances_frequency", frequency.toString());
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
				
				e.putBoolean("config_probe_distances_hash_data", ((Boolean) hash).booleanValue());
				e.commit();
			}
		}
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_distances_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_distances_enabled");
		enabled.setDefaultValue(AddressBookDistancesProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_distances_frequency");
		duration.setEntryValues(R.array.probe_distance_frequency_values);
		duration.setEntries(R.array.probe_distance_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);
		duration.setDefaultValue(AddressBookDistancesProbe.DEFAULT_FREQUENCY);

		screen.addPreference(duration);

		CheckBoxPreference hash = new CheckBoxPreference(activity);
		hash.setKey("config_probe_distances_hash_data");
		hash.setDefaultValue(Probe.DEFAULT_HASH_DATA);
		hash.setTitle(R.string.config_probe_distances_hash_title);
		hash.setSummary(R.string.config_probe_distances_hash_summary);

		screen.addPreference(hash);

		return screen;
	}

/*	@SuppressWarnings("unchecked")
	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		ArrayList<Bundle> array = (ArrayList<Bundle>) bundle.get(AddressBookDistancesProbe.PHONE_CALLS);

		int count = array.size();

		Bundle callsBundle = this.bundleForCallArray(context, array);

		formatted.putBundle(String.format(context.getString(R.string.display_calls_list_title), count), callsBundle);

		formatted.putString(context.getString(R.string.display_calls_recent_caller_title), bundle.getString(AddressBookDistancesProbe.RECENT_CALLER));
		formatted.putString(context.getString(R.string.display_calls_recent_number_title), bundle.getString(AddressBookDistancesProbe.RECENT_NUMBER));

		Date d = new Date(bundle.getLong(AddressBookDistancesProbe.RECENT_TIME));

		formatted.putString(context.getString(R.string.display_calls_recent_time_title), d.toString());

		formatted.putInt(context.getString(R.string.display_calls_incoming_count_title), bundle.getInt(AddressBookDistancesProbe.CALL_INCOMING_COUNT));
		formatted.putInt(context.getString(R.string.display_calls_missed_count_title), bundle.getInt(AddressBookDistancesProbe.CALL_MISSED_COUNT));
		formatted.putInt(context.getString(R.string.display_calls_outgoing_count_title), bundle.getInt(AddressBookDistancesProbe.CALL_OUTGOING_COUNT));
		formatted.putInt(context.getString(R.string.display_sms_incoming_count_title), bundle.getInt(AddressBookDistancesProbe.SMS_INCOMING_COUNT));
		formatted.putInt(context.getString(R.string.display_sms_outgoing_count_title), bundle.getInt(AddressBookDistancesProbe.SMS_OUTGOING_COUNT));


		ArrayList<String> keys = new ArrayList<String>();
		keys.add(String.format(context.getString(R.string.display_calls_list_title), count));
		keys.add(context.getString(R.string.display_calls_recent_caller_title));
		keys.add(context.getString(R.string.display_calls_recent_number_title));
		keys.add(context.getString(R.string.display_calls_recent_time_title));
		keys.add(context.getString(R.string.display_calls_incoming_count_title));
		keys.add(context.getString(R.string.display_calls_missed_count_title));
		keys.add(context.getString(R.string.display_calls_outgoing_count_title));
		keys.add(context.getString(R.string.display_sms_incoming_count_title));
		keys.add(context.getString(R.string.display_sms_outgoing_count_title));

		formatted.putStringArrayList("KEY_ORDER", keys);
		
		return formatted;
	}
	*/

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO Auto-generated method stub
	}
}
