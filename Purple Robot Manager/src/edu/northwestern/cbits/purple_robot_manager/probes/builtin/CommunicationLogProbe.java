package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberUtils;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class CommunicationLogProbe extends Probe
{
	private static final String NUMBER_LABEL = "NUMBER_LABEL";
	private static final String CALL_TIMESTAMP = "CALL_TIMESTAMP";
	private static final String NUMBER_TYPE = "NUMBER_TYPE";
	private static final String NUMBER_NAME = "NUMBER_NAME";
	private static final String CALL_DURATION = "CALL_DURATION";
	private static final String NUMBER = "NUMBER";
	private static final String CALL_OUTGOING_COUNT = "CALL_OUTGOING_COUNT";
	private static final String CALL_INCOMING_COUNT = "CALL_INCOMING_COUNT";
	private static final String CALL_MISSED_COUNT = "CALL_MISSED_COUNT";
	private static final String PHONE_CALLS = "PHONE_CALLS";
	private static final String CALL_TOTAL_COUNT = "CALL_TOTAL_COUNT";
	private static final String SMS_OUTGOING_COUNT = "SMS_OUTGOING_COUNT";
	private static final String SMS_INCOMING_COUNT = "SMS_INCOMING_COUNT";

	private long _lastCheck = 0;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.CommunicationLogProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_communication_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_communication_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_communication_enabled", false);
		
		e.commit();
	}

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		if (super.isEnabled(context))
		{
			long now = System.currentTimeMillis();

			if (prefs.getBoolean("config_probe_communication_enabled", false))
			{
				synchronized(this)
				{
					long freq = Long.parseLong(prefs.getString("config_probe_communication_frequency", "300000"));

					if (now - this._lastCheck  > freq)
					{
						Bundle bundle = new Bundle();
						bundle.putString("PROBE", this.name(context));
						bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

						ArrayList<Bundle> calls = new ArrayList<Bundle>();

						int sentCount = 0;
						int receivedCount = 0;
						int missedCount = 0;

						try
						{
							Cursor c = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, null);

							while (c.moveToNext())
							{
								Bundle contactBundle = new Bundle();

								contactBundle.putString(CommunicationLogProbe.NUMBER_NAME, c.getString(c.getColumnIndex(Calls.CACHED_NAME)));
								contactBundle.putString(CommunicationLogProbe.NUMBER_LABEL, c.getString(c.getColumnIndex(Calls.CACHED_NUMBER_LABEL)));
								
								if (c.getColumnIndex(Calls.CACHED_NUMBER_TYPE) != -1)
									contactBundle.putString(CommunicationLogProbe.NUMBER_TYPE, c.getString(c.getColumnIndex(Calls.CACHED_NUMBER_TYPE)));								
								
								contactBundle.putLong(CommunicationLogProbe.CALL_TIMESTAMP, c.getLong(c.getColumnIndex(Calls.DATE)));
								contactBundle.putLong(CommunicationLogProbe.CALL_DURATION, c.getLong(c.getColumnIndex(Calls.DURATION)));
								contactBundle.putString(CommunicationLogProbe.NUMBER, PhoneNumberUtils.formatNumber(c.getString(c.getColumnIndex(Calls.NUMBER))));

								int callType = c.getInt(c.getColumnIndex(Calls.CACHED_NUMBER_TYPE));

								contactBundle.putInt(CommunicationLogProbe.NUMBER_TYPE, callType);

								if (callType == Calls.OUTGOING_TYPE)
									sentCount += 1;
								else if (callType == Calls.INCOMING_TYPE)
									receivedCount += 1;
								if (callType == Calls.MISSED_TYPE)
									missedCount += 1;

								calls.add(contactBundle);
							}

							c.close();

							bundle.putParcelableArrayList(CommunicationLogProbe.PHONE_CALLS, calls);
							bundle.putInt(CommunicationLogProbe.CALL_OUTGOING_COUNT, sentCount);
							bundle.putInt(CommunicationLogProbe.CALL_INCOMING_COUNT, receivedCount);
							bundle.putInt(CommunicationLogProbe.CALL_MISSED_COUNT, missedCount);
							bundle.putInt(CommunicationLogProbe.CALL_TOTAL_COUNT, missedCount + receivedCount + sentCount);

							sentCount = 0;
							receivedCount = 0;

							Uri smsInboxUri = Uri.parse("content://sms/inbox");
							c = context.getContentResolver().query(smsInboxUri, null, null, null, null);
							receivedCount = c.getCount();
							c.close();

							Uri smsOutboxUri = Uri.parse("content://sms/sent");
							c = context.getContentResolver().query(smsOutboxUri, null, null, null, null);
							sentCount = c.getCount();
							c.close();

							bundle.putInt(CommunicationLogProbe.SMS_OUTGOING_COUNT, sentCount);
							bundle.putInt(CommunicationLogProbe.SMS_INCOMING_COUNT, receivedCount);

							this.transmitData(context, bundle);
						}
						catch (Exception e)
						{
							// Broken call & SMS databases on several devices... Ignoring.

							e.printStackTrace();
						}

						this._lastCheck = now;
					}
				}

				return true;
			}
		}

		return false;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		int count = bundle.getInt(CommunicationLogProbe.CALL_TOTAL_COUNT);

		return String.format(context.getResources().getString(R.string.summary_call_log_probe), count);
	}

	/*
	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		@SuppressWarnings("unchecked")
		ArrayList<Bundle> array = (ArrayList<Bundle>) bundle.get(HardwareInformationProbe.DEVICES);
		int count = bundle.getInt(HardwareInformationProbe.DEVICES_COUNT);

		Bundle devicesBundle = this.bundleForDevicesArray(context, array);

		formatted.putBundle(String.format(context.getString(R.string.display_bluetooth_devices_title), count), devicesBundle);

		return formatted;
	};
*/

	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_communication_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_communication_enabled");
		enabled.setDefaultValue(false);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_communication_frequency");
		duration.setDefaultValue("300000");
		duration.setEntryValues(R.array.probe_satellite_frequency_values);
		duration.setEntries(R.array.probe_satellite_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);

		screen.addPreference(duration);

		return screen;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO Auto-generated method stub
	}
}
