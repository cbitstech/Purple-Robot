package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberUtils;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.probes.AddressBookLabelActivity;
import edu.northwestern.cbits.purple_robot_manager.calibration.ContactCalibrationHelper;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
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
    private static final String RECENT_CALLER = "RECENT_CALLER";
    private static final String RECENT_TIME = "RECENT_TIME";
    private static final String RECENT_NUMBER = "RECENT_NUMBER";
    private static final String NUMBER_GROUP = "NUMBER_GROUP";
    private static final String RECENT_GROUP = "RECENT_GROUP";

    private static final boolean DEFAULT_ENABLED = true;
    private static final String ENABLED = "config_probe_communication_enabled";
    private static final String FREQUENCY = "config_probe_communication_frequency";
    private static final String HASH_DATA = "config_probe_communication_hash_data";

    private long _lastCheck = 0;

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.CommunicationLogProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_communication_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_personal_info_category);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(CommunicationLogProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(CommunicationLogProbe.ENABLED, false);

        e.commit();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isEnabled(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            long now = System.currentTimeMillis();

            if (prefs.getBoolean(CommunicationLogProbe.ENABLED, CommunicationLogProbe.DEFAULT_ENABLED))
            {
                synchronized (this)
                {
                    long freq = Long.parseLong(prefs.getString(CommunicationLogProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
                    boolean doHash = prefs.getBoolean(CommunicationLogProbe.HASH_DATA, Probe.DEFAULT_HASH_DATA);

                    if (now - this._lastCheck > freq)
                    {
                        ContactCalibrationHelper.check(context);

                        Bundle bundle = new Bundle();
                        bundle.putString("PROBE", this.name(context));
                        bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                        ArrayList<Bundle> calls = new ArrayList<Bundle>();

                        int sentCount = 0;
                        int receivedCount = 0;
                        int missedCount = 0;

                        String recentName = null;
                        String recentNumber = null;
                        long recentTimestamp = 0;

                        try
                        {
                            EncryptionManager em = EncryptionManager.getInstance();

                            Cursor c = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, null);

                            while (c.moveToNext())
                            {
                                Bundle contactBundle = new Bundle();

                                String numberName = c.getString(c.getColumnIndex(Calls.CACHED_NAME));
                                String phoneNumber = PhoneNumberUtils.formatNumber(c.getString(c.getColumnIndex(Calls.NUMBER)));

                                if (numberName == null)
                                    numberName = phoneNumber;

                                String group = ContactCalibrationHelper.getGroup(context, numberName, false);

                                if (group == null)
                                    group = ContactCalibrationHelper.getGroup(context, phoneNumber, true);

                                if (group != null)
                                    contactBundle.putString(CommunicationLogProbe.NUMBER_GROUP, group);

                                if (doHash)
                                {
                                    numberName = em.createHash(context, numberName);
                                    phoneNumber = em.createHash(context, phoneNumber);
                                }

                                contactBundle.putString(CommunicationLogProbe.NUMBER_NAME, numberName);
                                contactBundle.putString(CommunicationLogProbe.NUMBER_LABEL, phoneNumber);

                                if (c.getColumnIndex(Calls.CACHED_NUMBER_TYPE) != -1)
                                    contactBundle.putString(CommunicationLogProbe.NUMBER_TYPE, c.getString(c.getColumnIndex(Calls.CACHED_NUMBER_TYPE)));

                                long callTime = c.getLong(c.getColumnIndex(Calls.DATE));

                                contactBundle.putLong(CommunicationLogProbe.CALL_TIMESTAMP, callTime);
                                contactBundle.putLong(CommunicationLogProbe.CALL_DURATION, c.getLong(c.getColumnIndex(Calls.DURATION)));
                                contactBundle.putString(CommunicationLogProbe.NUMBER, phoneNumber);

                                int callType = c.getInt(c.getColumnIndex(Calls.CACHED_NUMBER_TYPE));

                                contactBundle.putInt(CommunicationLogProbe.NUMBER_TYPE, callType);

                                if (callType == Calls.OUTGOING_TYPE)
                                    sentCount += 1;
                                else if (callType == Calls.INCOMING_TYPE)
                                    receivedCount += 1;
                                else if (callType == Calls.MISSED_TYPE)
                                    missedCount += 1;

                                if (callType > 0)
                                {
                                    calls.add(contactBundle);

                                    if (callTime > recentTimestamp)
                                    {
                                        recentName = numberName;
                                        recentNumber = phoneNumber;

                                        recentTimestamp = callTime;
                                    }
                                }
                            }

                            c.close();

                            bundle.putParcelableArrayList(CommunicationLogProbe.PHONE_CALLS, calls);
                            bundle.putInt(CommunicationLogProbe.CALL_OUTGOING_COUNT, sentCount);
                            bundle.putInt(CommunicationLogProbe.CALL_INCOMING_COUNT, receivedCount);
                            bundle.putInt(CommunicationLogProbe.CALL_MISSED_COUNT, missedCount);
                            bundle.putInt(CommunicationLogProbe.CALL_TOTAL_COUNT, missedCount + receivedCount + sentCount);

                            if (recentName != null)
                                bundle.putString(CommunicationLogProbe.RECENT_CALLER, recentName);

                            if (recentNumber != null)
                                bundle.putString(CommunicationLogProbe.RECENT_NUMBER, recentNumber);

                            String group = ContactCalibrationHelper.getGroup(context, recentName, false);

                            if (group == null)
                                group = ContactCalibrationHelper.getGroup(context, recentNumber, true);

                            if (group != null)
                                bundle.putString(CommunicationLogProbe.RECENT_GROUP, group);

                            if (recentTimestamp > 0)
                                bundle.putLong(CommunicationLogProbe.RECENT_TIME, recentTimestamp);

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
                            // Broken call & SMS databases on several devices...
                            // Ignoring.

                            LogManager.getInstance(context).logException(e);
                        }

                        this._lastCheck = now;
                    }
                }

                return true;
            }
        }

        ContactCalibrationHelper.clear(context);

        return false;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        int count = (int) bundle.getDouble(CommunicationLogProbe.CALL_TOTAL_COUNT);

        return String.format(context.getResources().getString(R.string.summary_call_log_probe), count);
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString(CommunicationLogProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
        map.put(Probe.PROBE_FREQUENCY, freq);

        boolean hash = prefs.getBoolean(CommunicationLogProbe.HASH_DATA, Probe.DEFAULT_HASH_DATA);
        map.put(Probe.HASH_DATA, hash);

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(Probe.PROBE_FREQUENCY))
        {
            Object frequency = params.get(Probe.PROBE_FREQUENCY);

            if (frequency instanceof Double)
            {
                frequency = Long.valueOf(((Double) frequency).longValue());
            }

            if (frequency instanceof Long)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(CommunicationLogProbe.FREQUENCY, frequency.toString());
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

                e.putBoolean(CommunicationLogProbe.HASH_DATA, ((Boolean) hash).booleanValue());
                e.commit();
            }
        }
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_communication_probe_desc);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(final PreferenceActivity activity)
    {
        PreferenceManager manager = activity.getPreferenceManager();

        PreferenceScreen screen = manager.createPreferenceScreen(activity);
        screen.setTitle(this.title(activity));
        screen.setSummary(R.string.summary_communication_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(CommunicationLogProbe.ENABLED);
        enabled.setDefaultValue(CommunicationLogProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        ListPreference duration = new ListPreference(activity);
        duration.setKey(CommunicationLogProbe.FREQUENCY);
        duration.setEntryValues(R.array.probe_low_frequency_values);
        duration.setEntries(R.array.probe_low_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

        screen.addPreference(duration);

        CheckBoxPreference hash = new CheckBoxPreference(activity);
        hash.setKey(CommunicationLogProbe.HASH_DATA);
        hash.setDefaultValue(Probe.DEFAULT_HASH_DATA);
        hash.setTitle(R.string.config_probe_communication_hash_title);
        hash.setSummary(R.string.config_probe_communication_hash_summary);

        screen.addPreference(hash);

        Preference calibrate = new Preference(activity);
        calibrate.setTitle(R.string.config_probe_calibrate_title);
        calibrate.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference pref)
            {
                Intent intent = new Intent(activity, AddressBookLabelActivity.class);
                activity.startActivity(intent);

                return true;
            }
        });

        screen.addPreference(calibrate);

        return screen;
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = new JSONObject();

        try
        {
            JSONArray values = new JSONArray();
            values.put(true);
            values.put(false);

            JSONObject enabled = new JSONObject();
            enabled.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            enabled.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_ENABLED, enabled);

            JSONObject hash = new JSONObject();
            hash.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            hash.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.HASH_DATA, hash);

            JSONObject frequency = new JSONObject();
            frequency.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
            values = new JSONArray();

            String[] options = context.getResources().getStringArray(R.array.probe_low_frequency_values);

            for (String option : options)
            {
                values.put(Long.parseLong(option));
            }

            frequency.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_FREQUENCY, frequency);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    private Bundle bundleForCallArray(Context context, ArrayList<Bundle> objects)
    {
        Bundle bundle = new Bundle();

        ArrayList<String> keys = new ArrayList<String>();

        for (int i = 0; i < objects.size(); i++)
        {
            Bundle value = objects.get(i);
            String name = value.getString(CommunicationLogProbe.NUMBER);
            String number = value.getString(CommunicationLogProbe.NUMBER_NAME);

            keys.add(name);
            bundle.putString(name, number);
        }

        bundle.putStringArrayList("KEY_ORDER", keys);

        return bundle;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Bundle formattedBundle(Context context, Bundle bundle)
    {
        Bundle formatted = super.formattedBundle(context, bundle);

        ArrayList<Bundle> array = (ArrayList<Bundle>) bundle.get(CommunicationLogProbe.PHONE_CALLS);

        if (array == null)
            array = new ArrayList<Bundle>();

        int count = array.size();

        Bundle callsBundle = this.bundleForCallArray(context, array);

        formatted.putBundle(String.format(context.getString(R.string.display_calls_list_title), count), callsBundle);

        formatted.putString(context.getString(R.string.display_calls_recent_caller_title), bundle.getString(CommunicationLogProbe.RECENT_CALLER));
        formatted.putString(context.getString(R.string.display_calls_recent_number_title), bundle.getString(CommunicationLogProbe.RECENT_NUMBER));

        Date d = new Date(bundle.getLong(CommunicationLogProbe.RECENT_TIME));

        formatted.putString(context.getString(R.string.display_calls_recent_time_title), d.toString());

        formatted.putInt(context.getString(R.string.display_calls_incoming_count_title), (int) bundle.getDouble(CommunicationLogProbe.CALL_INCOMING_COUNT));
        formatted.putInt(context.getString(R.string.display_calls_missed_count_title), (int) bundle.getDouble(CommunicationLogProbe.CALL_MISSED_COUNT));
        formatted.putInt(context.getString(R.string.display_calls_outgoing_count_title), (int) bundle.getDouble(CommunicationLogProbe.CALL_OUTGOING_COUNT));
        formatted.putInt(context.getString(R.string.display_sms_incoming_count_title), (int) bundle.getDouble(CommunicationLogProbe.SMS_INCOMING_COUNT));
        formatted.putInt(context.getString(R.string.display_sms_outgoing_count_title), (int) bundle.getDouble(CommunicationLogProbe.SMS_OUTGOING_COUNT));

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
}
