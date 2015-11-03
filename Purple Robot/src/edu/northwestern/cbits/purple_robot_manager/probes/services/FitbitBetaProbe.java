package edu.northwestern.cbits.purple_robot_manager.probes.services;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.xsi.oauth.FitbitBetaApi;
import edu.northwestern.cbits.xsi.oauth.Keystore;
import edu.northwestern.cbits.xsi.oauth.OAuthActivity;

public class FitbitBetaProbe extends Probe
{
    public final static String PROBE_NAME = "edu.northwestern.cbits.purple_robot_manager.probes.services.FitbitBetaProbe";

    public static final String COVERED_RANGES = "fitbit-beta_covered_ranges";

    protected static final String STEPS = "STEPS";
    public static final String STEP_TIMESTAMPS = "STEP_TIMESTAMPS";
    protected static final String CALORIES = "CALORIES";
    public static final String CALORIES_TIMESTAMPS = "CALORIES_TIMESTAMPS";
    protected static final String ELEVATION = "ELEVATION";
    public static final String ELEVATION_TIMESTAMPS = "ELEVATION_TIMESTAMPS";
    protected static final String DISTANCE = "DISTANCE";
    public static final String DISTANCE_TIMESTAMPS = "DISTANCE_TIMESTAMPS";
    protected static final String FLOORS = "FLOORS";
    public static final String FLOORS_TIMESTAMPS = "FLOORS_TIMESTAMPS";
    protected static final String HEART = "HEART";
    public static final String HEART_TIMESTAMPS = "HEART_TIMESTAMPS";

    private static final String ENABLED = "config_feature_fitbit_beta_probe_enabled";
    private static final boolean DEFAULT_ENABLED = false;
    private static final String OAUTH_ACCESS_TOKEN = "oauth_fitbit-beta_access_token";
    private static final String OAUTH_REFRESH_TOKEN = "oauth_fitbit-beta_refresh_token";
    private static final String OAUTH_TOKEN_EXPIRES = "oauth_fitbit-beta_expires";

    private static final String ENABLE_STEPS = "config_fitbit-beta_enable_distance";
    private static final boolean DEFAULT_ENABLE_STEPS = true;

    private static final String ENABLE_CALORIES = "config_fitbit-beta_enable_calories";
    private static final boolean DEFAULT_ENABLE_CALORIES = false;

    private static final String ENABLE_DISTANCE = "config_fitbit-beta_enable_distance";
    private static final boolean DEFAULT_ENABLE_DISTANCE = true;

    private static final String ENABLE_FLOORS = "config_fitbit-beta_enable_floors";
    private static final boolean DEFAULT_ENABLE_FLOORS = true;

    private static final String ENABLE_HEART = "config_fitbit-beta_enable_heart";
    private static final boolean DEFAULT_ENABLE_HEART = false;

    private static final String ENABLE_ELEVATION = "config_fitbit-beta_enable_elevation";
    private static final boolean DEFAULT_ENABLE_ELEVATION = false;

    private static final String RETROSPECTIVE_PERIOD = "config_fitbit-beta_retrospective_period";
    private static final String DEFAULT_RETROSPECTIVE_PERIOD = "7";


//    private static final String LAST_STEP_TIMESTAMP = "config_fitbit-beta_last_step_timestamp";
//    private static final String LAST_HEART_TIMESTAMP = "config_fitbit-beta_last_heart_timestamp";
    private static final String LAST_CALORIE_TIMESTAMP = "config_fitbit-beta_last_calorie_timestamp";
//    private static final String LAST_FLOOR_TIMESTAMP = "config_fitbit-beta_last_floor_timestamp";
//    private static final String LAST_DISTANCE_TIMESTAMP = "config_fitbit-beta_last_distance_timestamp";
//    private static final String LAST_ELEVATION_TIMESTAMP = "config_fitbit-beta_last_elevation_timestamp";

    private long _lastUpdate = 0;

    public static class MinuteRange
    {
        public long start = 0;
        public long end = 0;

        public MinuteRange(long minute)
        {
            this.start = minute;
            this.end = minute;
        }

        public MinuteRange(long start, long end)
        {
            this.start = start;
            this.end = end;
        }

        public static MinuteRange rangeFromString(String token)
        {
            if (token.contains("-"))
            {
                String[] tokens = token.split("-");

                return new MinuteRange(Long.parseLong(tokens[0]), Long.parseLong(tokens[1]));
            }

            return new MinuteRange(Long.parseLong(token));
        }

        public String toString()
        {
            if (start == end)
                return "" + start;

            return (start + "-" + end);
        }

        public boolean containsMinute(long minute)
        {
            return ((this.start) <= minute && (this.end >= minute));
        }

        public boolean overlaps(MinuteRange range)
        {
            if (this.start >= range.start && this.start <= range.end)
                return true;
            else if (this.end >= range.start && this.end <= range.end)
                return true;
            else if (range.start >= this.start && range.start <= this.end)
                return true;
            else if (range.end >= this.start && range.end <= this.end)
                return true;

            return false;
        }

        public void merge(MinuteRange range) {
            if (range.start < this.start)
                this.start = range.start;

            if (range.end > this.end)
                this.end = range.end;
        }
    }

    public static class MinuteRanges
    {
        ArrayList<MinuteRange> ranges = new ArrayList<>();

        public void addMinute(long minute)
        {
            for (MinuteRange range : this.ranges)
            {
                if (range.start - 1 == minute)
                {
                    range.start = minute;

                    return;
                }

                if (range.end + 1 == minute)
                {
                    range.end = minute;

                    return;
                }
            }

            this.ranges.add(new MinuteRange(minute));
        }

        public void addRange(MinuteRange range)
        {
            this.ranges.add(range);
        }

        public boolean containsMinute(long minute)
        {
            for (MinuteRange range : this.ranges)
            {
                if (range.containsMinute(minute))
                    return true;
            }

            return false;
        }

        public String coalescedJSON()
        {
            Collections.sort(this.ranges, new Comparator<MinuteRange>() {
                @Override
                public int compare(MinuteRange one, MinuteRange two) {
                    if (one.start < two.start)
                        return -1;
                    else if (one.start > two.start)
                        return 1;

                    return 0;
                }
            });

            ArrayList<MinuteRange> newRanges = new ArrayList<>();

            for (MinuteRange range : this.ranges)
            {
                boolean add = true;

                for (MinuteRange newRange : newRanges)
                {
                    if (newRange.overlaps(range)) {
                        newRange.merge(range);
                        add = false;
                    }
                }

                if (add)
                    newRanges.add(range);
            }

            JSONArray jsonArray = new JSONArray();

            for (MinuteRange newRange : newRanges)
            {
                jsonArray.put(newRange.toString());
            }

            int oldCount = this.ranges.size();
            int newCount = newRanges.size();

            this.ranges = newRanges;

            if (oldCount != newCount)
                return this.coalescedJSON();

            return jsonArray.toString();
        }
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_fitbit_beta_probe_desc);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_beta_category);
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.services.FitbitBetaProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_fitbit_beta_probe);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(FitbitBetaProbe.ENABLED, true);
        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(FitbitBetaProbe.ENABLED, false);
        e.commit();
    }

    private void initKeystore(Context context)
    {
        Keystore.put(FitbitBetaApi.OAUTH2_CLIENT_ID, context.getString(R.string.fitbit_client_id));
        Keystore.put(FitbitBetaApi.CONSUMER_KEY, context.getString(R.string.fitbit_consumer_key));
        Keystore.put(FitbitBetaApi.CONSUMER_SECRET, context.getString(R.string.fitbit_consumer_secret));
        Keystore.put(FitbitBetaApi.CALLBACK_URL, "http://purple.robot.com/oauth/fitbit-beta");
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        final SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(FitbitBetaProbe.ENABLED, false))
            {
                this.initKeystore(context);

                String token = prefs.getString(FitbitBetaProbe.OAUTH_ACCESS_TOKEN, "");
                String refresh = prefs.getString(FitbitBetaProbe.OAUTH_REFRESH_TOKEN, "");

                final String title = context.getString(R.string.title_fitbit_check);
                final SanityManager sanity = SanityManager.getInstance(context);

                final FitbitBetaProbe me = this;
                final long now = System.currentTimeMillis();

                if (token == null || refresh == null || token.trim().length() == 0 || refresh.trim().length() == 0)
                {
                    String message = context.getString(R.string.message_fitbit_check);

                    Runnable action = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            me.fetchAuth(context);
                        }
                    };

                    sanity.addAlert(SanityCheck.WARNING, title, message, action);
                }
                else
                {
                    final long expires =  prefs.getLong(FitbitBetaProbe.OAUTH_TOKEN_EXPIRES, 0);
                    Keystore.put(FitbitBetaApi.USER_ACCESS_TOKEN, prefs.getString(FitbitBetaProbe.OAUTH_ACCESS_TOKEN, null));
                    Keystore.put(FitbitBetaApi.USER_REFRESH_TOKEN, prefs.getString(FitbitBetaProbe.OAUTH_REFRESH_TOKEN, null));
                    Keystore.put(FitbitBetaApi.USER_TOKEN_EXPIRES, "" + expires);

                    sanity.clearAlert(title);

                    final String warningTitle = context.getString(R.string.config_probe_fitbit_rate_limit_warning_title);
                    final String warningMessage = context.getString(R.string.config_probe_fitbit_rate_limit_warning);

                    if (now - this._lastUpdate > 1000 * 60 * 15)
                    {
                        sanity.clearAlert(warningTitle);

                        this._lastUpdate = now;

                        Runnable r = new Runnable()
                        {
                            public void run()
                            {
                                try
                                {
                                    if (System.currentTimeMillis() > expires) {
                                        FitbitBetaApi.refreshTokens(context, FitbitBetaProbe.OAUTH_ACCESS_TOKEN, FitbitBetaProbe.OAUTH_REFRESH_TOKEN, FitbitBetaProbe.OAUTH_TOKEN_EXPIRES);

                                        long expires = prefs.getLong(FitbitBetaProbe.OAUTH_TOKEN_EXPIRES, 0);
                                        Keystore.put(FitbitBetaApi.USER_ACCESS_TOKEN, prefs.getString(FitbitBetaProbe.OAUTH_ACCESS_TOKEN, null));
                                        Keystore.put(FitbitBetaApi.USER_REFRESH_TOKEN, prefs.getString(FitbitBetaProbe.OAUTH_REFRESH_TOKEN, null));
                                        Keystore.put(FitbitBetaApi.USER_TOKEN_EXPIRES, "" + expires);
                                    }

                                    int retrospectivePeriod = Integer.parseInt(prefs.getString(FitbitBetaProbe.RETROSPECTIVE_PERIOD, FitbitBetaProbe.DEFAULT_RETROSPECTIVE_PERIOD));

                                    ArrayList<Long> minutesToEval = new ArrayList<>();

                                    for(int i = 0; i < (retrospectivePeriod * 24 * 60); i++)
                                        minutesToEval.add((now / (60 * 1000)) - i);

                                    ArrayList<Long> minutesToSkip = new ArrayList<>();

                                    JSONArray coveredRanges = new JSONArray(prefs.getString(FitbitBetaProbe.COVERED_RANGES, "[]"));

                                    MinuteRanges coveredMinutes = new MinuteRanges();

                                    for (int i = 0; i < coveredRanges.length(); i++)
                                    {
                                        coveredMinutes.addRange(MinuteRange.rangeFromString(coveredRanges.getString(i)));
                                    }

                                    String coveredMinutesString = coveredMinutes.coalescedJSON();

                                    for (long minute : minutesToEval)
                                    {
                                        if (coveredMinutes.containsMinute(minute))
                                            minutesToSkip.add(minute);
                                    }

                                    minutesToEval.removeAll(minutesToSkip);

                                    HashSet<Long> evaledDays = new HashSet<>();

                                    Calendar cal = Calendar.getInstance();

                                    long lastMinute = -1;

                                    for (long minute : minutesToEval)
                                    {
                                        lastMinute = minute;

                                        if (evaledDays.contains(minute / (24 * 60)))
                                        {

                                        }
                                        else
                                        {
                                            cal.setTimeInMillis(minute * 60 * 1000);

                                            String month = "" + (cal.get(Calendar.MONTH) + 1);

                                            if (month.length() < 2)
                                                month = "0" + month;


                                            String monthDay = "" + cal.get(Calendar.DAY_OF_MONTH);

                                            if (monthDay.length() < 2)
                                                monthDay = "0" + monthDay;


                                            String dayString = cal.get(Calendar.YEAR) + "-" + month + "-" + monthDay;

                                            Bundle bundle = new Bundle();

                                            bundle.putString(Probe.BUNDLE_PROBE, me.name(context));
                                            bundle.putLong(Probe.BUNDLE_TIMESTAMP, System.currentTimeMillis() / 1000);

                                            boolean transmit = false;

                                            HashSet<Long> minutesToday = new HashSet<>();

                                            if (prefs.getBoolean(FitbitBetaProbe.ENABLE_STEPS, FitbitBetaProbe.DEFAULT_ENABLE_STEPS)) {
                                                try {
                                                    JSONObject stepsObj = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/activities/steps/date/" + dayString + "/1d/1min.json"));

                                                    JSONObject stepsIntraday = stepsObj.getJSONObject("activities-steps-intraday");
                                                    JSONArray stepsValues = stepsIntraday.getJSONArray("dataset");

                                                    ArrayList<Long> stepTimestamps = new ArrayList<>();
                                                    ArrayList<Long> valueList = new ArrayList<>();

                                                    Calendar c = Calendar.getInstance();

                                                    for (int i = 0; i < stepsValues.length(); i++) {
                                                        JSONObject value = stepsValues.getJSONObject(i);

                                                        String time = value.getString("time");

                                                        String[] tokens = time.split(":");

                                                        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tokens[0]));
                                                        c.set(Calendar.MINUTE, Integer.parseInt(tokens[1]));
                                                        c.set(Calendar.SECOND, Integer.parseInt(tokens[2]));
                                                        c.set(Calendar.MILLISECOND, 0);

                                                        long timestamp = c.getTimeInMillis();
                                                        long valueQty = value.getLong("value");

                                                        if (coveredMinutes.containsMinute(minute) == false && valueQty > 0) {
                                                            stepTimestamps.add(timestamp);

                                                            valueList.add(valueQty);

                                                            minutesToday.add(timestamp / (60 * 1000));
                                                        }
                                                    }

                                                    long[] timestamps = new long[stepTimestamps.size()];
                                                    long[] steps = new long[stepTimestamps.size()];

                                                    for (int i = 0; i < stepTimestamps.size(); i++) {
                                                        timestamps[i] = stepTimestamps.get(i);
                                                        steps[i] = valueList.get(i);
                                                    }

                                                    bundle.putLongArray(FitbitBetaProbe.STEP_TIMESTAMPS, timestamps);
                                                    bundle.putLongArray(FitbitBetaProbe.STEPS, steps);

                                                    transmit = true;
                                                } catch (Exception e) {
                                                    e.printStackTrace();

                                                    me._lastUpdate = System.currentTimeMillis() + (1000 * 60 * 15);

                                                    SanityManager.getInstance(context).addAlert(SanityCheck.WARNING, warningTitle, warningMessage, null);

                                                    break;
                                                }
                                            }

                                            if (prefs.getBoolean(FitbitBetaProbe.ENABLE_CALORIES, FitbitBetaProbe.DEFAULT_ENABLE_CALORIES)) {
                                                long lastUpdate = prefs.getLong(FitbitBetaProbe.LAST_CALORIE_TIMESTAMP, 0);

                                                try {
                                                    JSONObject stepsObj = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/activities/calories/date/" + dayString + "/1d/1min.json"));

                                                    JSONObject intraday = stepsObj.getJSONObject("activities-calories-intraday");
                                                    JSONArray valuesArray = intraday.getJSONArray("dataset");

                                                    ArrayList<Long> valueTimestamps = new ArrayList<>();
                                                    ArrayList<Long> valueList = new ArrayList<>();

                                                    Calendar c = Calendar.getInstance();

                                                    for (int i = 0; i < valuesArray.length(); i++) {
                                                        JSONObject value = valuesArray.getJSONObject(i);

                                                        String time = value.getString("time");

                                                        String[] tokens = time.split(":");

                                                        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tokens[0]));
                                                        c.set(Calendar.MINUTE, Integer.parseInt(tokens[1]));
                                                        c.set(Calendar.SECOND, Integer.parseInt(tokens[2]));
                                                        c.set(Calendar.MILLISECOND, 0);

                                                        long timestamp = c.getTimeInMillis();
                                                        long valueQty = value.getLong("value");

                                                        if (timestamp > lastUpdate && valueQty > 0) {
                                                            valueTimestamps.add(timestamp);
                                                            lastUpdate = timestamp;

                                                            valueList.add(valueQty);

                                                            // Don't add to evaled since automatically calculated...
                                                        }
                                                    }

                                                    long[] timestamps = new long[valueTimestamps.size()];
                                                    long[] values = new long[valueList.size()];

                                                    for (int i = 0; i < valueTimestamps.size(); i++) {
                                                        timestamps[i] = valueTimestamps.get(i);
                                                        values[i] = valueList.get(i);
                                                    }

                                                    bundle.putLongArray(FitbitBetaProbe.CALORIES_TIMESTAMPS, timestamps);
                                                    bundle.putLongArray(FitbitBetaProbe.CALORIES, values);

                                                    Editor e = prefs.edit();
                                                    e.putLong(FitbitBetaProbe.LAST_CALORIE_TIMESTAMP, lastUpdate);
                                                    e.commit();

                                                    transmit = true;
                                                } catch (Exception e) {
                                                    e.printStackTrace();

                                                    me._lastUpdate = System.currentTimeMillis() + (1000 * 60 * 15);

                                                    SanityManager.getInstance(context).addAlert(SanityCheck.WARNING, warningTitle, warningMessage, null);

                                                    break;
                                                }
                                            }

                                            if (prefs.getBoolean(FitbitBetaProbe.ENABLE_DISTANCE, FitbitBetaProbe.DEFAULT_ENABLE_DISTANCE)) {
                                                try {
                                                    JSONObject stepsObj = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/activities/distance/date/" + dayString + "/1d/1min.json"));

                                                    JSONObject intraday = stepsObj.getJSONObject("activities-distance-intraday");
                                                    JSONArray valuesArray = intraday.getJSONArray("dataset");

                                                    ArrayList<Long> valueTimestamps = new ArrayList<>();
                                                    ArrayList<Long> valueList = new ArrayList<>();

                                                    Calendar c = Calendar.getInstance();

                                                    for (int i = 0; i < valuesArray.length(); i++) {
                                                        JSONObject value = valuesArray.getJSONObject(i);

                                                        String time = value.getString("time");

                                                        String[] tokens = time.split(":");

                                                        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tokens[0]));
                                                        c.set(Calendar.MINUTE, Integer.parseInt(tokens[1]));
                                                        c.set(Calendar.SECOND, Integer.parseInt(tokens[2]));
                                                        c.set(Calendar.MILLISECOND, 0);

                                                        long timestamp = c.getTimeInMillis();
                                                        long valueQty = value.getLong("value");

                                                        if (coveredMinutes.containsMinute(minute) == false && valueQty > 0) {
                                                            valueTimestamps.add(timestamp);

                                                            valueList.add(valueQty);

                                                            minutesToday.add(timestamp / (60 * 1000));
                                                        }
                                                    }

                                                    long[] timestamps = new long[valueTimestamps.size()];
                                                    long[] values = new long[valueList.size()];

                                                    for (int i = 0; i < valueTimestamps.size(); i++) {
                                                        timestamps[i] = valueTimestamps.get(i);
                                                        values[i] = valueList.get(i);
                                                    }

                                                    bundle.putLongArray(FitbitBetaProbe.DISTANCE_TIMESTAMPS, timestamps);
                                                    bundle.putLongArray(FitbitBetaProbe.DISTANCE, values);

                                                    transmit = true;
                                                } catch (Exception e) {
                                                    e.printStackTrace();

                                                    me._lastUpdate = System.currentTimeMillis() + (1000 * 60 * 15);

                                                    SanityManager.getInstance(context).addAlert(SanityCheck.WARNING, warningTitle, warningMessage, null);

                                                    break;
                                                }
                                            }

                                            if (prefs.getBoolean(FitbitBetaProbe.ENABLE_FLOORS, FitbitBetaProbe.DEFAULT_ENABLE_FLOORS)) {
                                                try {
                                                    JSONObject stepsObj = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/activities/floors/date/" + dayString + "/1d/1min.json"));

                                                    JSONObject intraday = stepsObj.getJSONObject("activities-floors-intraday");
                                                    JSONArray valuesArray = intraday.getJSONArray("dataset");

                                                    ArrayList<Long> valueTimestamps = new ArrayList<>();
                                                    ArrayList<Long> valueList = new ArrayList<>();

                                                    Calendar c = Calendar.getInstance();

                                                    for (int i = 0; i < valuesArray.length(); i++) {
                                                        JSONObject value = valuesArray.getJSONObject(i);

                                                        String time = value.getString("time");

                                                        String[] tokens = time.split(":");

                                                        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tokens[0]));
                                                        c.set(Calendar.MINUTE, Integer.parseInt(tokens[1]));
                                                        c.set(Calendar.SECOND, Integer.parseInt(tokens[2]));
                                                        c.set(Calendar.MILLISECOND, 0);

                                                        long timestamp = c.getTimeInMillis();
                                                        long valueQty = value.getLong("value");

                                                        if (coveredMinutes.containsMinute(minute) == false && valueQty > 0) {
                                                            valueTimestamps.add(timestamp);

                                                            valueList.add(valueQty);

                                                            minutesToday.add(timestamp / (60 * 1000));
                                                        }
                                                    }

                                                    long[] timestamps = new long[valueTimestamps.size()];
                                                    long[] values = new long[valueList.size()];

                                                    for (int i = 0; i < valueTimestamps.size(); i++) {
                                                        timestamps[i] = valueTimestamps.get(i);
                                                        values[i] = valueList.get(i);
                                                    }

                                                    bundle.putLongArray(FitbitBetaProbe.FLOORS_TIMESTAMPS, timestamps);
                                                    bundle.putLongArray(FitbitBetaProbe.FLOORS, values);

                                                    transmit = true;
                                                } catch (JSONException e) {
                                                    e.printStackTrace();

                                                    me._lastUpdate = System.currentTimeMillis() + (1000 * 60 * 15);

                                                    SanityManager.getInstance(context).addAlert(SanityCheck.WARNING, warningTitle, warningMessage, null);

                                                    break;
                                                }
                                            }

                                            if (prefs.getBoolean(FitbitBetaProbe.ENABLE_HEART, FitbitBetaProbe.DEFAULT_ENABLE_HEART)) {
                                                try {
                                                    JSONObject stepsObj = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/activities/heart/date/" + dayString + "/1d/1min.json"));

                                                    JSONObject intraday = stepsObj.getJSONObject("activities-heart-intraday");
                                                    JSONArray valuesArray = intraday.getJSONArray("dataset");

                                                    ArrayList<Long> valueTimestamps = new ArrayList<>();
                                                    ArrayList<Long> valueList = new ArrayList<>();

                                                    Calendar c = Calendar.getInstance();

                                                    for (int i = 0; i < valuesArray.length(); i++) {
                                                        JSONObject value = valuesArray.getJSONObject(i);

                                                        String time = value.getString("time");

                                                        String[] tokens = time.split(":");

                                                        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tokens[0]));
                                                        c.set(Calendar.MINUTE, Integer.parseInt(tokens[1]));
                                                        c.set(Calendar.SECOND, Integer.parseInt(tokens[2]));
                                                        c.set(Calendar.MILLISECOND, 0);

                                                        long timestamp = c.getTimeInMillis();
                                                        long valueQty = value.getLong("value");

                                                        if (coveredMinutes.containsMinute(minute) == false && valueQty > 0) {
                                                            valueTimestamps.add(timestamp);

                                                            valueList.add(valueQty);

                                                            minutesToday.add(timestamp / (60 * 1000));
                                                        }
                                                    }

                                                    long[] timestamps = new long[valueTimestamps.size()];
                                                    long[] values = new long[valueList.size()];

                                                    for (int i = 0; i < valueTimestamps.size(); i++) {
                                                        timestamps[i] = valueTimestamps.get(i);
                                                        values[i] = valueList.get(i);
                                                    }

                                                    bundle.putLongArray(FitbitBetaProbe.HEART_TIMESTAMPS, timestamps);
                                                    bundle.putLongArray(FitbitBetaProbe.HEART, values);

                                                    transmit = true;
                                                } catch (JSONException e) {
                                                    e.printStackTrace();

                                                    me._lastUpdate = System.currentTimeMillis() + (1000 * 60 * 15);

                                                    SanityManager.getInstance(context).addAlert(SanityCheck.WARNING, warningTitle, warningMessage, null);

                                                    break;
                                                }
                                            }

                                            if (prefs.getBoolean(FitbitBetaProbe.ENABLE_ELEVATION, FitbitBetaProbe.DEFAULT_ENABLE_ELEVATION)) {
                                                try {
                                                    JSONObject stepsObj = FitbitBetaApi.fetch(Uri.parse("https://api.fitbit.com/1/user/-/activities/elevation/date/" + dayString + "/1d/1min.json"));

                                                    JSONObject intraday = stepsObj.getJSONObject("activities-elevation-intraday");
                                                    JSONArray valuesArray = intraday.getJSONArray("dataset");

                                                    ArrayList<Long> valueTimestamps = new ArrayList<>();
                                                    ArrayList<Long> valueList = new ArrayList<>();

                                                    Calendar c = Calendar.getInstance();

                                                    for (int i = 0; i < valuesArray.length(); i++) {
                                                        JSONObject value = valuesArray.getJSONObject(i);

                                                        String time = value.getString("time");

                                                        String[] tokens = time.split(":");

                                                        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tokens[0]));
                                                        c.set(Calendar.MINUTE, Integer.parseInt(tokens[1]));
                                                        c.set(Calendar.SECOND, Integer.parseInt(tokens[2]));
                                                        c.set(Calendar.MILLISECOND, 0);

                                                        long timestamp = c.getTimeInMillis();
                                                        long valueQty = value.getLong("value");

                                                        if (coveredMinutes.containsMinute(minute) == false && valueQty > 0) {
                                                            valueTimestamps.add(timestamp);

                                                            valueList.add(valueQty);

                                                            minutesToday.add(timestamp / (60 * 1000));
                                                        }
                                                    }

                                                    long[] timestamps = new long[valueTimestamps.size()];
                                                    long[] values = new long[valueList.size()];

                                                    for (int i = 0; i < valueTimestamps.size(); i++) {
                                                        timestamps[i] = valueTimestamps.get(i);
                                                        values[i] = valueList.get(i);
                                                    }

                                                    bundle.putLongArray(FitbitBetaProbe.ELEVATION_TIMESTAMPS, timestamps);
                                                    bundle.putLongArray(FitbitBetaProbe.ELEVATION, values);

                                                    transmit = true;
                                                } catch (JSONException e) {
                                                    e.printStackTrace();

                                                    me._lastUpdate = System.currentTimeMillis() + (1000 * 60 * 15);

                                                    SanityManager.getInstance(context).addAlert(SanityCheck.WARNING, warningTitle, warningMessage, null);

                                                    break;
                                                }
                                            }

                                            if (transmit) {
                                                me.transmitData(context, bundle);
                                            }

                                            evaledDays.add(minute / (24 * 60));

                                            for (Long todayMinute : minutesToday)
                                            {
                                                coveredMinutes.addMinute(todayMinute);
                                            }
                                        }
                                    }

                                    long firstMinute = (now / (60 * 1000)) - (5 * 24 * 60);

                                    if (lastMinute < firstMinute) {
                                        coveredMinutes.addRange(new MinuteRange(lastMinute, firstMinute));
                                    }

                                    String coalescedRanges = coveredMinutes.coalescedJSON();

                                    Editor e = prefs.edit();
                                    e.putString(FitbitBetaProbe.COVERED_RANGES, coalescedRanges);
                                    e.commit();
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        };

                        Thread t = new Thread(r);
                        t.start();
                    }
                }

                return true;
            }
        }

        return false;
    }

    private void fetchAuth(Context context)
    {
        this.initKeystore(context);

        String userId = EncryptionManager.getInstance().getUserHash(context);

        Intent intent = new Intent(context, OAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra(OAuthActivity.CONSUMER_KEY, context.getString(R.string.fitbit_consumer_key));
        intent.putExtra(OAuthActivity.CONSUMER_SECRET, context.getString(R.string.fitbit_consumer_secret));
        intent.putExtra(OAuthActivity.REQUESTER, "fitbit-beta");
        intent.putExtra(OAuthActivity.CALLBACK_URL, "http://purple.robot.com/oauth/fitbit-beta");
        intent.putExtra(OAuthActivity.LOG_URL, LogManager.getInstance(context).getLogUrl(context));
        intent.putExtra(OAuthActivity.HASH_SECRET, userId);

        context.startActivity(intent);
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = new JSONObject();

        try
        {
            JSONObject enabled = new JSONObject();
            enabled.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            JSONArray values = new JSONArray();
            values.put(true);
            values.put(false);
            enabled.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_ENABLED, enabled);

            settings.put(FitbitBetaProbe.ENABLE_STEPS, enabled);
            settings.put(FitbitBetaProbe.ENABLE_DISTANCE, enabled);
            settings.put(FitbitBetaProbe.ENABLE_CALORIES, enabled);
            settings.put(FitbitBetaProbe.ENABLE_ELEVATION, enabled);
            settings.put(FitbitBetaProbe.ENABLE_HEART, enabled);
            settings.put(FitbitBetaProbe.ENABLE_FLOORS, enabled);

            JSONObject retro = new JSONObject();
            retro.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_STRING);

            JSONArray retroValues = new JSONArray();
            retroValues.put("1");
            retroValues.put("3");
            retroValues.put("5");
            retroValues.put("7");
            retroValues.put("14");
            retroValues.put("30");
            retroValues.put("60");
            retroValues.put("90");
            retro.put(Probe.PROBE_VALUES, retroValues);

            settings.put(FitbitBetaProbe.RETROSPECTIVE_PERIOD, retro);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        map.put(FitbitBetaProbe.ENABLE_CALORIES, prefs.getBoolean(FitbitBetaProbe.ENABLE_CALORIES, FitbitBetaProbe.DEFAULT_ENABLE_CALORIES));
        map.put(FitbitBetaProbe.ENABLE_STEPS, prefs.getBoolean(FitbitBetaProbe.ENABLE_STEPS, FitbitBetaProbe.DEFAULT_ENABLE_STEPS));
        map.put(FitbitBetaProbe.ENABLE_HEART, prefs.getBoolean(FitbitBetaProbe.ENABLE_HEART, FitbitBetaProbe.DEFAULT_ENABLE_HEART));
        map.put(FitbitBetaProbe.ENABLE_DISTANCE, prefs.getBoolean(FitbitBetaProbe.ENABLE_DISTANCE, FitbitBetaProbe.DEFAULT_ENABLE_DISTANCE));
        map.put(FitbitBetaProbe.ENABLE_ELEVATION, prefs.getBoolean(FitbitBetaProbe.ENABLE_ELEVATION, FitbitBetaProbe.DEFAULT_ENABLE_ELEVATION));
        map.put(FitbitBetaProbe.FLOORS, prefs.getBoolean(FitbitBetaProbe.FLOORS, FitbitBetaProbe.DEFAULT_ENABLE_FLOORS));
        map.put(FitbitBetaProbe.RETROSPECTIVE_PERIOD, prefs.getString(FitbitBetaProbe.RETROSPECTIVE_PERIOD, FitbitBetaProbe.DEFAULT_RETROSPECTIVE_PERIOD));

        return map;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        String[] keys = { FitbitBetaProbe.ENABLE_CALORIES, FitbitBetaProbe.ENABLE_STEPS, FitbitBetaProbe.ENABLE_DISTANCE, FitbitBetaProbe.ENABLE_HEART };

        for (String key: keys) {
            if (params.containsKey(key)) {
                Object value = params.get(key);

                if (value instanceof Boolean) {
                    SharedPreferences prefs = Probe.getPreferences(context);
                    Editor e = prefs.edit();

                    e.putBoolean(key, ((Boolean) value));
                    e.commit();
                }
            }
        }

        if (params.containsKey(FitbitBetaProbe.RETROSPECTIVE_PERIOD))
        {
            Object value = params.get(FitbitBetaProbe.RETROSPECTIVE_PERIOD);

            if (value instanceof String) {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(FitbitBetaProbe.RETROSPECTIVE_PERIOD, value.toString());
                e.commit();
            }
        }
    }

    @Override
    public PreferenceScreen preferenceScreen(final Context context, PreferenceManager manager)
    {
        final PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(this.summary(context));

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(FitbitBetaProbe.ENABLED);
        enabled.setDefaultValue(FitbitBetaProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        CheckBoxPreference stepsPref = new CheckBoxPreference(context);
        stepsPref.setKey(FitbitBetaProbe.ENABLE_STEPS);
        stepsPref.setDefaultValue(FitbitBetaProbe.DEFAULT_ENABLE_STEPS);
        stepsPref.setTitle(R.string.config_fitbit_steps_title);
        screen.addPreference(stepsPref);

        CheckBoxPreference distancePref = new CheckBoxPreference(context);
        distancePref.setKey(FitbitBetaProbe.ENABLE_DISTANCE);
        distancePref.setDefaultValue(FitbitBetaProbe.DEFAULT_ENABLE_DISTANCE);
        distancePref.setTitle(R.string.config_fitbit_distance_title);
        screen.addPreference(distancePref);

        CheckBoxPreference floorsPref = new CheckBoxPreference(context);
        floorsPref.setKey(FitbitBetaProbe.FLOORS);
        floorsPref.setDefaultValue(FitbitBetaProbe.DEFAULT_ENABLE_FLOORS);
        floorsPref.setTitle(R.string.config_fitbit_floors_title);
        screen.addPreference(floorsPref);

        CheckBoxPreference elevationPref = new CheckBoxPreference(context);
        elevationPref.setKey(FitbitBetaProbe.ENABLE_ELEVATION);
        elevationPref.setDefaultValue(FitbitBetaProbe.DEFAULT_ENABLE_ELEVATION);
        elevationPref.setTitle(R.string.config_fitbit_elevation_title);
        screen.addPreference(elevationPref);

        CheckBoxPreference caloriesPref = new CheckBoxPreference(context);
        caloriesPref.setKey(FitbitBetaProbe.ENABLE_CALORIES);
        caloriesPref.setDefaultValue(FitbitBetaProbe.DEFAULT_ENABLE_CALORIES);
        caloriesPref.setTitle(R.string.config_fitbit_calories_title);
        screen.addPreference(caloriesPref);

        CheckBoxPreference heartPref = new CheckBoxPreference(context);
        heartPref.setKey(FitbitBetaProbe.ENABLE_HEART);
        heartPref.setDefaultValue(FitbitBetaProbe.DEFAULT_ENABLE_HEART);
        heartPref.setTitle(R.string.config_fitbit_heart_title);
        screen.addPreference(heartPref);

        FlexibleListPreference retrospective = new FlexibleListPreference(context);
        retrospective.setKey(FitbitBetaProbe.RETROSPECTIVE_PERIOD);
        retrospective.setDefaultValue(FitbitBetaProbe.DEFAULT_RETROSPECTIVE_PERIOD);
        retrospective.setEntryValues(R.array.probe_fitbit_retrospective_values);
        retrospective.setEntries(R.array.probe_fitbit_retrospective_labels);
        retrospective.setTitle(R.string.probe_fitbit_retrospective_label);

        screen.addPreference(retrospective);

        final SharedPreferences prefs = Probe.getPreferences(context);

        String token = prefs.getString(FitbitBetaProbe.OAUTH_ACCESS_TOKEN, null);
        String refresh = prefs.getString(FitbitBetaProbe.OAUTH_REFRESH_TOKEN, null);

        final Preference authPreference = new Preference(context);
        authPreference.setTitle(R.string.title_authenticate_fitbit_probe);
        authPreference.setSummary(R.string.summary_authenticate_fitbit_probe);

        final Preference logoutPreference = new Preference(context);
        logoutPreference.setTitle(R.string.title_logout_fitbit_probe);
        logoutPreference.setSummary(R.string.summary_logout_fitbit_probe);

        final FitbitBetaProbe me = this;

        authPreference.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                me.fetchAuth(context);

                screen.addPreference(logoutPreference);
                screen.removePreference(authPreference);

                return true;
            }
        });

        logoutPreference.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                Editor e = prefs.edit();
                e.remove(FitbitBetaProbe.OAUTH_ACCESS_TOKEN);
                e.remove(FitbitBetaProbe.OAUTH_REFRESH_TOKEN);
                e.remove(FitbitBetaProbe.OAUTH_TOKEN_EXPIRES);
                e.commit();

                me._lastUpdate = 0;

                screen.addPreference(authPreference);
                screen.removePreference(logoutPreference);

                if (context instanceof Activity)
                {
                    Activity activity = (Activity) context;
                    activity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Toast.makeText(context, context.getString(R.string.toast_fitbit_logout), Toast.LENGTH_LONG).show();
                        }
                    });
                }

                return true;
            }
        });

        if (token == null || refresh == null)
            screen.addPreference(authPreference);
        else
            screen.addPreference(logoutPreference);

        return screen;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        if (bundle.containsKey(FitbitBetaProbe.CALORIES_TIMESTAMPS)) {
            double[] timestamps = bundle.getDoubleArray(FitbitBetaProbe.CALORIES_TIMESTAMPS);

            return String.format(context.getResources().getString(R.string.summary_fitbit_beta), timestamps.length);
        }
        else if (bundle.containsKey(FitbitBetaProbe.DISTANCE_TIMESTAMPS)) {
            double[] timestamps = bundle.getDoubleArray(FitbitBetaProbe.DISTANCE_TIMESTAMPS);

            return String.format(context.getResources().getString(R.string.summary_fitbit_beta), timestamps.length);
        }
        else if (bundle.containsKey(FitbitBetaProbe.ELEVATION_TIMESTAMPS)) {
            double[] timestamps = bundle.getDoubleArray(FitbitBetaProbe.ELEVATION_TIMESTAMPS);

            return String.format(context.getResources().getString(R.string.summary_fitbit_beta), timestamps.length);
        }
        else if (bundle.containsKey(FitbitBetaProbe.FLOORS_TIMESTAMPS)) {
            double[] timestamps = bundle.getDoubleArray(FitbitBetaProbe.FLOORS_TIMESTAMPS);

            return String.format(context.getResources().getString(R.string.summary_fitbit_beta), timestamps.length);
        }
        else if (bundle.containsKey(FitbitBetaProbe.HEART_TIMESTAMPS)) {
            double[] timestamps = bundle.getDoubleArray(FitbitBetaProbe.HEART_TIMESTAMPS);

            return String.format(context.getResources().getString(R.string.summary_fitbit_beta), timestamps.length);
        }
        else if (bundle.containsKey(FitbitBetaProbe.STEP_TIMESTAMPS)) {
            double[] timestamps = bundle.getDoubleArray(FitbitBetaProbe.STEP_TIMESTAMPS);

            return String.format(context.getResources().getString(R.string.summary_fitbit_beta), timestamps.length);
        }

        return context.getString(R.string.summary_fitbit_beta_no_datapoints);
    }
}
