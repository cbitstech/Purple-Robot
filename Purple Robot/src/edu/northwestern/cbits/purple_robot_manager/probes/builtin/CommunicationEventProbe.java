package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneNumberUtils;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.probes.AddressBookLabelActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.calibration.ContactCalibrationHelper;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
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
    private static final String NORMALIZED_HASH = "NORMALIZED_HASH";

    public static final boolean DEFAULT_ENABLED = true;
    private static final boolean DEFAULT_RETRIEVE = false;
    private static final boolean DEFAULT_ENCRYPT = true;
    public static final String ENABLED = "config_probe_communication_event_enabled";
    private static final String FREQUENCY = "config_probe_communication_event_frequency";
    private static final String HASH_DATA = "config_probe_communication_event_hash_data";
    private static final String RECENT_EVENT = "config_probe_communication_event_recent";
    private static final String RETRIEVE_DATA = "config_probe_communication_event_retrieve_data";
    private static final String ENCRYPT_DATA = "config_probe_communication_event_encrypt_data";

    public static final boolean DEFAULT_ENABLE_CALIBRATION_NOTIFICATIONS = true;
    public static final String ENABLE_CALIBRATION_NOTIFICATIONS = "config_probe_communication_event_calibration_notifications";

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
                boolean ready = true;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(context, "android.permission.READ_CALL_LOG") != PackageManager.PERMISSION_GRANTED) {
                        SanityManager.getInstance(context).addPermissionAlert(this.name(context), "android.permission.READ_CALL_LOG", context.getString(R.string.rationale_event_call_log_probe), null);
                        ready = false;
                    }

                    if (ContextCompat.checkSelfPermission(context, "android.permission.READ_SMS") != PackageManager.PERMISSION_GRANTED) {
                        SanityManager.getInstance(context).addPermissionAlert(this.name(context), "android.permission.READ_SMS", context.getString(R.string.rationale_event_sms_log_probe), null);
                        ready = false;
                    }
                }

                if (ready)
                {
                    SanityManager.getInstance(context).clearPermissionAlert("android.permission.READ_CALL_LOG");
                    SanityManager.getInstance(context).clearPermissionAlert("android.permission.READ_SMS");

                    synchronized (this) {
                        long freq = Long.parseLong(prefs.getString(CommunicationEventProbe.FREQUENCY, Probe.DEFAULT_FREQUENCY));
                        boolean doHash = prefs.getBoolean(CommunicationEventProbe.HASH_DATA, Probe.DEFAULT_HASH_DATA);

                        if (now - this._lastCheck > freq) {
                            long mostRecent = prefs.getLong(CommunicationEventProbe.RECENT_EVENT, 0);
                            long newRecent = mostRecent;

                            try {
                                EncryptionManager em = EncryptionManager.getInstance();

                                String selection = "date > ?";
                                String[] args =
                                        {"" + mostRecent};

                                Cursor c = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, selection, args, "date");

                                while (c.moveToNext()) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("PROBE", this.name(context));
                                    bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                                    bundle.putString(CommunicationEventProbe.TYPE, CommunicationEventProbe.TYPE_PHONE);

                                    String numberName = c.getString(c.getColumnIndex(Calls.CACHED_NAME));
                                    String phoneNumber = PhoneNumberUtils.formatNumber(c.getString(c.getColumnIndex(Calls.NUMBER)));

                                    if (numberName == null)
                                        numberName = phoneNumber;

                                    String group = ContactCalibrationHelper.getGroup(context, numberName, false);

                                    if (group == null)
                                        group = ContactCalibrationHelper.getGroup(context, phoneNumber, true);

                                    if (group != null)
                                        bundle.putString(CommunicationEventProbe.GROUP, group);

                                    bundle.putString(CommunicationEventProbe.NORMALIZED_HASH, EncryptionManager.normalizedPhoneHash(context, phoneNumber));

                                    if (doHash) {
                                        numberName = em.createHash(context, numberName);
                                        phoneNumber = em.createHash(context, phoneNumber);
                                    }

                                    bundle.putString(CommunicationEventProbe.NAME, numberName);
                                    bundle.putString(CommunicationEventProbe.NUMBER, phoneNumber);

                                    long callTime = c.getLong(c.getColumnIndex(Calls.DATE));

                                    bundle.putLong(CommunicationEventProbe.TIMESTAMP, callTime);

                                    if (callTime > newRecent)
                                        newRecent = callTime;

                                    bundle.putLong(CommunicationEventProbe.DURATION, c.getLong(c.getColumnIndex(Calls.DURATION)));

                                    int callType = c.getInt(c.getColumnIndex(Calls.TYPE));

                                    if (callType == Calls.OUTGOING_TYPE)
                                        bundle.putString(CommunicationEventProbe.DIRECTION, CommunicationEventProbe.OUTGOING);
                                    else if (callType == Calls.INCOMING_TYPE)
                                        bundle.putString(CommunicationEventProbe.DIRECTION, CommunicationEventProbe.INCOMING);
                                    else if (callType == Calls.MISSED_TYPE)
                                        bundle.putString(CommunicationEventProbe.DIRECTION, CommunicationEventProbe.MISSED);
                                    else
                                        bundle.putString(CommunicationEventProbe.DIRECTION, CommunicationEventProbe.UNKNOWN);

                                    this.transmitData(context, bundle);
                                }

                                c.close();

                                c = context.getContentResolver().query(Uri.parse("content://sms/inbox"), null, selection, args, "date");

                                while (c.moveToNext()) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("PROBE", this.name(context));
                                    bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                                    bundle.putString(CommunicationEventProbe.TYPE, CommunicationEventProbe.TYPE_SMS);

                                    String numberName = c.getString(c.getColumnIndex("person"));
                                    String phoneNumber = PhoneNumberUtils.formatNumber(c.getString(c.getColumnIndex("address")));

                                    if (numberName == null)
                                        numberName = phoneNumber;

                                    String group = ContactCalibrationHelper.getGroup(context, numberName, false);

                                    if (group == null)
                                        group = ContactCalibrationHelper.getGroup(context, phoneNumber, true);

                                    if (group != null)
                                        bundle.putString(CommunicationEventProbe.GROUP, group);

                                    if (doHash) {
                                        numberName = em.createHash(context, numberName);
                                        phoneNumber = em.createHash(context, phoneNumber);
                                    }

                                    bundle.putString(CommunicationEventProbe.NAME, numberName);
                                    bundle.putString(CommunicationEventProbe.NUMBER, phoneNumber);
                                    bundle.putString(CommunicationEventProbe.NORMALIZED_HASH, EncryptionManager.normalizedPhoneHash(context, phoneNumber));

                                    long callTime = c.getLong(c.getColumnIndex("date"));

                                    bundle.putLong(CommunicationEventProbe.TIMESTAMP, callTime);

                                    if (callTime > newRecent)
                                        newRecent = callTime;

                                    bundle.putString(CommunicationEventProbe.DIRECTION, CommunicationEventProbe.INCOMING);

                                    boolean retrieve = prefs.getBoolean(CommunicationEventProbe.RETRIEVE_DATA, CommunicationEventProbe.DEFAULT_RETRIEVE);
                                    boolean encrypt = prefs.getBoolean(CommunicationEventProbe.ENCRYPT_DATA, CommunicationEventProbe.DEFAULT_ENCRYPT);

                                    if (retrieve) {
                                        String body = c.getString(c.getColumnIndex("body"));

                                        if (encrypt)
                                            body = em.encryptString(context, body);

                                        bundle.putString(CommunicationEventProbe.MESSAGE_BODY, body);
                                    }

                                    this.transmitData(context, bundle);
                                }

                                c.close();

                                c = context.getContentResolver().query(Uri.parse("content://sms/sent"), null, selection, args, "date");

                                while (c.moveToNext()) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("PROBE", this.name(context));
                                    bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                                    bundle.putString(CommunicationEventProbe.TYPE, CommunicationEventProbe.TYPE_SMS);

                                    String numberName = c.getString(c.getColumnIndex("person"));
                                    String phoneNumber = PhoneNumberUtils.formatNumber(c.getString(c.getColumnIndex("address")));
                                    bundle.putString(CommunicationEventProbe.NORMALIZED_HASH, EncryptionManager.normalizedPhoneHash(context, phoneNumber));

                                    if (numberName == null)
                                        numberName = phoneNumber;

                                    String group = ContactCalibrationHelper.getGroup(context, numberName, false);

                                    if (group == null)
                                        group = ContactCalibrationHelper.getGroup(context, phoneNumber, true);

                                    if (group != null)
                                        bundle.putString(CommunicationEventProbe.GROUP, group);

                                    if (doHash) {
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

                                    boolean retrieve = prefs.getBoolean(CommunicationEventProbe.RETRIEVE_DATA, CommunicationEventProbe.DEFAULT_RETRIEVE);
                                    boolean encrypt = prefs.getBoolean(CommunicationEventProbe.ENCRYPT_DATA, CommunicationEventProbe.DEFAULT_ENCRYPT);

                                    if (retrieve) {
                                        String body = c.getString(c.getColumnIndex("body"));

                                        if (encrypt)
                                            body = em.encryptString(context, body);

                                        bundle.putString(CommunicationEventProbe.MESSAGE_BODY, body);
                                    }

                                    this.transmitData(context, bundle);
                                }

                                c.close();
                            } catch (Exception e) {
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

        return String.format(context.getResources().getString(R.string.summary_communication_events_probe), type, name, format.format(new Date(timestamp)));
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

        boolean encrypt = prefs.getBoolean(CommunicationEventProbe.ENCRYPT_DATA, CommunicationEventProbe.DEFAULT_ENCRYPT);
        map.put(CommunicationEventProbe.ENCRYPT_DATA, encrypt);

        boolean retrieve = prefs.getBoolean(CommunicationEventProbe.RETRIEVE_DATA, CommunicationEventProbe.DEFAULT_RETRIEVE);
        map.put(CommunicationEventProbe.RETRIEVE_DATA, retrieve);

        boolean calibrateNotes = prefs.getBoolean(CommunicationEventProbe.ENABLE_CALIBRATION_NOTIFICATIONS, CommunicationEventProbe.DEFAULT_ENABLE_CALIBRATION_NOTIFICATIONS);
        map.put(Probe.PROBE_CALIBRATION_NOTIFICATIONS, calibrateNotes);

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
                frequency = ((Double) frequency).longValue();
            }

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

                e.putBoolean(CommunicationEventProbe.HASH_DATA, (Boolean) hash);
                e.commit();
            }
        }

        if (params.containsKey(CommunicationEventProbe.RETRIEVE_DATA))
        {
            Object retrieve = params.get(CommunicationEventProbe.RETRIEVE_DATA);

            if (retrieve instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(CommunicationEventProbe.RETRIEVE_DATA, (Boolean) retrieve);
                e.commit();
            }
        }

        if (params.containsKey(CommunicationEventProbe.ENCRYPT_DATA))
        {
            Object encrypt = params.get(CommunicationEventProbe.ENCRYPT_DATA);

            if (encrypt instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(CommunicationEventProbe.ENCRYPT_DATA, (Boolean) encrypt);
                e.commit();
            }
        }

        if (params.containsKey(Probe.PROBE_CALIBRATION_NOTIFICATIONS))
        {
            Object enable = params.get(Probe.PROBE_CALIBRATION_NOTIFICATIONS);

            if (enable instanceof Boolean)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putBoolean(CommunicationEventProbe.ENABLE_CALIBRATION_NOTIFICATIONS, ((Boolean) enable));
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
    public PreferenceScreen preferenceScreen(final Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_communication_event_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(CommunicationEventProbe.ENABLED);
        enabled.setDefaultValue(CommunicationEventProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleListPreference duration = new FlexibleListPreference(context);
        duration.setKey(CommunicationEventProbe.FREQUENCY);
        duration.setEntryValues(R.array.probe_low_frequency_values);
        duration.setEntries(R.array.probe_low_frequency_labels);
        duration.setTitle(R.string.probe_frequency_label);
        duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

        screen.addPreference(duration);

        CheckBoxPreference hash = new CheckBoxPreference(context);
        hash.setKey(CommunicationEventProbe.HASH_DATA);
        hash.setDefaultValue(Probe.DEFAULT_HASH_DATA);
        hash.setTitle(R.string.config_probe_communication_hash_title);
        hash.setSummary(R.string.config_probe_communication_hash_summary);

        screen.addPreference(hash);

        CheckBoxPreference retrieve = new CheckBoxPreference(context);
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder = builder.setTitle(R.string.config_probe_communication_retrieve_warning_title);
                    builder = builder.setMessage(R.string.config_probe_communication_retrieve_warning);
                    builder = builder.setPositiveButton(R.string.button_continue, null);

                    builder.create().show();
                }

                return true;
            }
        });

        screen.addPreference(retrieve);

        CheckBoxPreference encrypt = new CheckBoxPreference(context);
        encrypt.setKey(CommunicationEventProbe.ENCRYPT_DATA);
        encrypt.setDefaultValue(CommunicationEventProbe.DEFAULT_ENCRYPT);
        encrypt.setTitle(R.string.config_probe_communication_encrypt_title);
        encrypt.setSummary(R.string.config_probe_communication_encrypt_summary);

        screen.addPreference(encrypt);

        Preference calibrate = new Preference(context);
        calibrate.setTitle(R.string.config_probe_calibrate_title);
        calibrate.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference pref)
            {
                Intent intent = new Intent(context, AddressBookLabelActivity.class);
                context.startActivity(intent);

                return true;
            }
        });

        screen.addPreference(calibrate);

        CheckBoxPreference enableCalibrationNotifications = new CheckBoxPreference(context);
        enableCalibrationNotifications.setTitle(R.string.title_enable_calibration_notifications);
        enableCalibrationNotifications.setSummary(R.string.summary_enable_calibration_notifications);
        enableCalibrationNotifications.setKey(CommunicationEventProbe.ENABLE_CALIBRATION_NOTIFICATIONS);
        enableCalibrationNotifications.setDefaultValue(CommunicationEventProbe.DEFAULT_ENABLE_CALIBRATION_NOTIFICATIONS);

        screen.addPreference(enableCalibrationNotifications);

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

            settings.put(Probe.PROBE_CALIBRATION_NOTIFICATIONS, enabled);

            JSONObject encrypt = new JSONObject();
            encrypt.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            encrypt.put(Probe.PROBE_VALUES, values);
            settings.put(CommunicationEventProbe.ENCRYPT_DATA, encrypt);

            JSONObject retrieve = new JSONObject();
            retrieve.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            retrieve.put(Probe.PROBE_VALUES, values);
            settings.put(CommunicationEventProbe.RETRIEVE_DATA, retrieve);

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
}
