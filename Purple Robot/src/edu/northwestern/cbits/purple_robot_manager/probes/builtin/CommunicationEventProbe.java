package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.preference.Preference.OnPreferenceChangeListener;
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

public class CommunicationEventProbe extends Probe
{
    private static final String TYPE = "COMMUNICATION_TYPE";
    private static final String TYPE_PHONE = "PHONE";
    private static final String TYPE_SMS = "SMS";
    private static final String DIRECTION = "COMMUNICATION_DIRECTION";
    private static final String INCOMING = "INCOMING";
    private static final String OUTGOING = "OUTGOING";
    private static final String MISSED = "MISSED";
    private static final String UNKNOWN = "UNKNOWN";
    private static final String GROUP = "GROUP";
    private static final String NUMBER = "NUMBER";
    private static final String NAME = "NAME";
    private static final String TIMESTAMP = "COMM_TIMESTAMP";
    private static final String DURATION = "DURATION";
    private static final String MESSAGE_BODY = "MESSAGE_BODY";

    private static final boolean DEFAULT_ENABLED = true;
    private static final boolean DEFAULT_RETRIEVE = false;
    private static final boolean DEFAULT_ENCRYPT = true;
    private static final String ENABLED = "config_probe_communication_event_enabled";
    private static final String FREQUENCY = "config_probe_communication_event_frequency";
    private static final String HASH_DATA = "config_probe_communication_event_hash_data";
    private static final String RECENT_EVENT = "config_probe_communication_event_recent";
    private static final String RETRIEVE_DATA = "config_probe_communication_event_retrieve_data";
    private static final String ENCRYPT_DATA = "config_probe_communication_event_encrypt_data";

    private long _lastCheck = 0;

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.CommunicationEventProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_communication_event_probe);
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
        e.putBoolean(CommunicationEventProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(CommunicationEventProbe.ENABLED, false);

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

            if (prefs.getBoolean(CommunicationEventProbe.ENABLED, CommunicationEventProbe.DEFAULT_ENABLED))
            {
                synchronized (this)
                {
                    long freq = Long.parseLong(prefs.getString(CommunicationEventProbe.FREQUENCY,
                            Probe.DEFAULT_FREQUENCY));
                    boolean doHash = prefs.getBoolean(CommunicationEventProbe.HASH_DATA, Probe.DEFAULT_HASH_DATA);

                    if (now - this._lastCheck > freq)
                    {
                        long mostRecent = prefs.getLong(CommunicationEventProbe.RECENT_EVENT, 0);
                        long newRecent = mostRecent;

                        try
                        {
                            EncryptionManager em = EncryptionManager.getInstance();

                            String selection = "date > ?";
                            String[] args =
                            { "" + mostRecent };

                            Cursor c = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, selection,
                                    args, "date");

                            while (c.moveToNext())
                            {
                                Bundle bundle = new Bundle();
                                bundle.putString("PROBE", this.name(context));
                                bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                                bundle.putString(CommunicationEventProbe.TYPE, CommunicationEventProbe.TYPE_PHONE);

                                String numberName = c.getString(c.getColumnIndex(Calls.CACHED_NAME));
                                String phoneNumber = PhoneNumberUtils.formatNumber(c.getString(c
                                        .getColumnIndex(Calls.NUMBER)));

                                if (numberName == null)
                                    numberName = phoneNumber;

                                String group = ContactCalibrationHelper.getGroup(context, numberName, false);

                                if (group == null)
                                    group = ContactCalibrationHelper.getGroup(context, phoneNumber, true);

                                if (group != null)
                                    bundle.putString(CommunicationEventProbe.GROUP, group);

                                if (doHash)
                                {
                                    numberName = em.createHash(context, numberName);
                                    phoneNumber = em.createHash(context, phoneNumber);
                                }

                                bundle.putString(CommunicationEventProbe.NAME, numberName);
                                bundle.putString(CommunicationEventProbe.NUMBER, phoneNumber);

                                long callTime = c.getLong(c.getColumnIndex(Calls.DATE));

                                bundle.putLong(CommunicationEventProbe.TIMESTAMP, callTime);

                                if (callTime > newRecent)
                                    newRecent = callTime;

                                bundle.putLong(CommunicationEventProbe.DURATION,
                                        c.getLong(c.getColumnIndex(Calls.DURATION)));

                                int callType = c.getInt(c.getColumnIndex(Calls.TYPE));

                                if (callType == Calls.OUTGOING_TYPE)
                                    bundle.putString(CommunicationEventProbe.DIRECTION,
                                            CommunicationEventProbe.OUTGOING);
                                else if (callType == Calls.INCOMING_TYPE)
                                    bundle.putString(CommunicationEventProbe.DIRECTION,
                                            CommunicationEventProbe.INCOMING);
                                else if (callType == Calls.MISSED_TYPE)
                                    bundle.putString(CommunicationEventProbe.DIRECTION, CommunicationEventProbe.MISSED);
                                else
                                    bundle.putString(CommunicationEventProbe.DIRECTION, CommunicationEventProbe.UNKNOWN);

                                this.transmitData(context, bundle);
                            }

                            c.close();

                            c = context.getContentResolver().query(Uri.parse("content://sms/inbox"), null, selection,
                                    args, "date");

                            while (c.moveToNext())
                            {
                                Bundle bundle = new Bundle();
                                bundle.putString("PROBE", this.name(context));
                                bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                                bundle.putString(CommunicationEventProbe.TYPE, CommunicationEventProbe.TYPE_SMS);

                                String numberName = c.getString(c.getColumnIndex("person"));
                                String phoneNumber = PhoneNumberUtils.formatNumber(c.getString(c
                                        .getColumnIndex("address")));

                                if (numberName == null)
                                    numberName = phoneNumber;

                                String group = ContactCalibrationHelper.getGroup(context, numberName, false);

                                if (group == null)
                                    group = ContactCalibrationHelper.getGroup(context, phoneNumber, true);

                                if (group != null)
                                    bundle.putString(CommunicationEventProbe.GROUP, group);

                                if (doHash)
                                {
                                    numberName = em.createHash(context, numberName);
                                    phoneNumber = em.createHash(context, phoneNumber);
                                }

                                bundle.putString(CommunicationEventProbe.NAME, numberName);
                                bundle.putString(CommunicationEventProbe.NUMBER, phoneNumber);

                                long callTime = c.getLong(c.getColumnIndex("date"));

                                bundle.putLong(CommunicationEventProbe.TIMESTAMP, callTime);

                                if (callTime > newRecent)
                                    newRecent = callTime;

                                bundle.putString(CommunicationEventProbe.DIRECTION, CommunicationEventProbe.INCOMING);

                                this.transmitData(context, bundle);
                            }

                            c.close();

                            c = context.getContentResolver().query(Uri.parse("content://sms/sent"), null, selection,
                                    args, "date");

                            while (c.moveToNext())
                            {
                                Bundle bundle = new Bundle();
                                bundle.putString("PROBE", this.name(context));
                                bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                                bundle.putString(CommunicationEventProbe.TYPE, CommunicationEventProbe.TYPE_SMS);

                                String numberName = c.getString(c.getColumnIndex("person"));
                                String phoneNumber = PhoneNumberUtils.formatNumber(c.getString(c
                                        .getColumnIndex("address")));

                                if (numberName == null)
                                    numberName = phoneNumber;

                                String group = ContactCalibrationHelper.getGroup(context, numberName, false);

                                if (group == null)
                                    group = ContactCalibrationHelper.getGroup(context, phoneNumber, true);

                                if (group != null)
                                    bundle.putString(CommunicationEventProbe.GROUP, group);

                                if (doHash)
                                {
                                    numberName = em.createHash(context, numberName);
                                    phoneNumber = em.createHash(context, phoneNumber);
                                }

                                bundle.putString(CommunicationEventProbe.NAME, numberName);
                                bundle.putString(CommunicationEventProbe.NUMBER, phoneNumber);

                                long callTime = c.getLong(c.getColumnIndex("date"));

                                bundle.putLong(CommunicationEventProbe.TIMESTAMP, callTime);

                                if (callTime > newRecent)
                                    newRecent = callTime;

                                bundle.putString(CommunicationEventProbe.DIRECTION, CommunicationEventProbe.OUTGOING);

                                boolean retrieve = prefs.getBoolean(CommunicationEventProbe.RETRIEVE_DATA,
                                        CommunicationEventProbe.DEFAULT_RETRIEVE);
                                boolean encrypt = prefs.getBoolean(CommunicationEventProbe.ENCRYPT_DATA,
                                        CommunicationEventProbe.DEFAULT_ENCRYPT);

                                if (retrieve)
                                {
                                    String body = c.getString(c.getColumnIndex("body"));

                                    if (encrypt)
                                        body = em.encryptString(context, body);

                                    bundle.putString(CommunicationEventProbe.MESSAGE_BODY, body);
                                }

                                this.transmitData(context, bundle);
                            }

                            c.close();
                        }
                        catch (Exception e)
                        {
                            // Broken call & SMS databases on several devices...
                            // Ignoring.

                            LogManager.getInstance(context).logException(e);
                        }

                        this._lastCheck = now;

                        Editor e = prefs.edit();
                        e.putLong(CommunicationEventProbe.RECENT_EVENT, newRecent);
                        e.commit();
                    }
                }

                return true;
            }
        }

        return false;
    }

    @Override
    @SuppressLint("SimpleDateFormat")
    public String summarizeValue(Context context, Bundle bundle)
    {
        String name = bundle.getString(CommunicationEventProbe.NAME);
        String type = bundle.getString(CommunicationEventProbe.TYPE);

        if (CommunicationEventProbe.TYPE_PHONE.equals(type))
            type = context.getResources().getString(R.string.summary_communication_events_phone_type);
        else
            type = context.getResources().getString(R.string.summary_communication_events_sms_type);

        long timestamp = (long) bundle.getDouble(CommunicationEventProbe.TIMESTAMP);

        SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm");

        return String.format(context.getResources().getString(R.string.summary_communication_events_probe), type, name,
                format.format(new Date(timestamp)));
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = Probe.getPreferences(context);

        long freq = Long.parseLong(prefs.getString(CommunicationEventProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
        map.put(Probe.PROBE_FREQUENCY, freq);

        boolean hash = prefs.getBoolean(CommunicationEventProbe.HASH_DATA, Probe.DEFAULT_HASH_DATA);
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

            if (frequency instanceof Long)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(CommunicationEventProbe.FREQUENCY, frequency.toString());
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

                e.putBoolean(CommunicationEventProbe.HASH_DATA, ((Boolean) hash).booleanValue());
                e.commit();
            }
        }
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_communication_event_probe_desc);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(final PreferenceActivity activity)
    {
        PreferenceManager manager = activity.getPreferenceManager();

        PreferenceScreen screen = manager.createPreferenceScreen(activity);
        screen.setTitle(this.title(activity));
        screen.setSummary(R.string.summary_communication_event_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(CommunicationEventProbe.ENABLED);
        enabled.setDefaultValue(CommunicationEventProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        ListPreference duration = new ListPreference(activity);
        duration.setKey(CommunicationEventProbe.FREQUENCY);
        duration.setEntryValues(R.array.probe_low_frequency_values);
        duration.setEntries(R.array.probe_low_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

        screen.addPreference(duration);

        CheckBoxPreference hash = new CheckBoxPreference(activity);
        hash.setKey(CommunicationEventProbe.HASH_DATA);
        hash.setDefaultValue(Probe.DEFAULT_HASH_DATA);
        hash.setTitle(R.string.config_probe_communication_hash_title);
        hash.setSummary(R.string.config_probe_communication_hash_summary);

        screen.addPreference(hash);

        CheckBoxPreference retrieve = new CheckBoxPreference(activity);
        retrieve.setKey(CommunicationEventProbe.RETRIEVE_DATA);
        retrieve.setDefaultValue(CommunicationEventProbe.DEFAULT_RETRIEVE);
        retrieve.setTitle(R.string.config_probe_communication_retrieve_title);
        retrieve.setSummary(R.string.config_probe_communication_retrieve_summary);

        retrieve.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference arg0, Object newValue)
            {
                Boolean b = (Boolean) newValue;

                if (b)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder = builder.setTitle(R.string.config_probe_communication_retrieve_warning_title);
                    builder = builder.setMessage(R.string.config_probe_communication_retrieve_warning);
                    builder = builder.setPositiveButton(R.string.button_continue, null);

                    builder.create().show();
                }

                return true;
            }
        });

        screen.addPreference(retrieve);

        CheckBoxPreference encrypt = new CheckBoxPreference(activity);
        encrypt.setKey(CommunicationEventProbe.ENCRYPT_DATA);
        encrypt.setDefaultValue(CommunicationEventProbe.DEFAULT_ENCRYPT);
        encrypt.setTitle(R.string.config_probe_communication_encrypt_title);
        encrypt.setSummary(R.string.config_probe_communication_encrypt_summary);

        screen.addPreference(encrypt);

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
}
