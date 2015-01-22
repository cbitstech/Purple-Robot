package edu.northwestern.cbits.purple_robot_manager.triggers;

import java.io.IOException;
import java.io.StringReader;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateRange;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;

import org.apache.commons.lang.builder.HashCodeBuilder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

@SuppressLint(
{ "SimpleDateFormat", "TrulyRandom" })
public class DateTrigger extends Trigger
{
    public static final String TYPE_NAME = "datetime";

    public static final String DATETIME_START = "datetime_start";
    public static final String DATETIME_END = "datetime_end";
    public static final String DATETIME_REPEATS = "datetime_repeat";
    public static final String DATETIME_RANDOM = "datetime_random";

    private static final String RANDOM = "random";
    private static final String START = "start";
    private static final String END = "end";
    private static final String ORIGINAL_END = "original_end";
    private static final String CALENDAR_STRING = "calendar_rule";
    private static final String ORIGINAL_START = "original_start";
    private static final String REPEATS = "repeats";
    private static final String FIRE_ON_BOOT = "fire_on_boot";

    private static SecureRandom random = null;

    private boolean _random = false;
    private String _start = null;
    private String _end = null;
    private String _originalStart = null;
    private String _originalEnd = null;
    private String _repeats = null;
    private boolean _fireOnBoot = false;

    private Calendar _calendar = null;

    public String _icalString = null;

    private long _lastFireCalcDate = 0;
    private final List<Date> _upcomingFireDates = new ArrayList<Date>();

    private static List<Runnable> pendingRefreshes = new ArrayList<Runnable>();
    private static boolean isRefreshing = false;

    private abstract class RefreshRunnable implements Runnable
    {
        private String _identifier = null;

        public RefreshRunnable(String identifier)
        {
            this._identifier = identifier;
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder(17, 31).append(this._identifier).toHashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
                return false;

            return obj.hashCode() == this.hashCode();
        }
    }

    static
    {
        try
        {
            DateTrigger.random = SecureRandom.getInstance("SHA1PRNG");
        }
        catch (NoSuchAlgorithmException e)
        {
            LogManager.getInstance(null).logException(e);
        }
    }

    @Override
    public void refresh(final Context context)
    {
        final DateTrigger me = this;

        RefreshRunnable r = new RefreshRunnable(this.identifier())
        {
            @Override
            public void run()
            {
                me.refreshTrigger(context);
            }
        };

        if (DateTrigger.pendingRefreshes.contains(r) == false)
            DateTrigger.pendingRefreshes.add(r);

        if (DateTrigger.isRefreshing == false)
        {
            DateTrigger.isRefreshing = true;

            Runnable s = new Runnable()
            {
                @Override
                public void run()
                {
                    if (DateTrigger.pendingRefreshes.size() > 0)
                    {
                        Runnable r = DateTrigger.pendingRefreshes.remove(0);

                        if (r != null)
                            r.run();

                        this.run();
                    }
                    else
                        DateTrigger.isRefreshing = false;
                }
            };

            Thread t = new Thread(s, "Trigger Refresh");
            t.start();
            t.setName("Trigger Refresh");
        }
    }

    public void refreshTrigger(final Context context)
    {
        long now = System.currentTimeMillis();

        if (now - this._lastFireCalcDate > (3600 * 1000))
        {
            this._lastFireCalcDate = now;

            this.refreshCalendar(context);

            if (this._calendar == null)
                return;

            ArrayList<Date> upcoming = new ArrayList<Date>();

            long current = now;

            long maxCount = 64;

            long hour = 1000 * 60 * 60;

            Date currentDate = new Date(now);

            while (current < now + (hour * 48) && upcoming.size() < maxCount)
            {
                DateTime from = new DateTime(new Date(current));
                DateTime to = new DateTime(new Date(current + hour));

                try
                {
                    for (Object o : this._calendar.getComponents("VEVENT"))
                    {
                        Component c = (Component) o;

                        Period period = new Period(from, to);

                        PeriodList l = c.calculateRecurrenceSet(period);

                        for (Object po : l)
                        {
                            if (po instanceof Period)
                            {
                                Period p = (Period) po;

                                Date start = p.getRangeStart();

                                if (start.after(currentDate))
                                {
                                    if (upcoming.contains(start) == false)
                                        upcoming.add(start);
                                }
                            }
                        }
                    }
                }
                catch (NullPointerException e)
                {

                }
                catch (IllegalArgumentException e)
                {
                    LogManager.getInstance(context).logException(e);
                }

                current += hour;
            }

            synchronized (this._upcomingFireDates)
            {
                this._upcomingFireDates.clear();
                this._upcomingFireDates.addAll(upcoming);
            }
        }
    }

    @Override
    public void reset(Context context)
    {
        super.reset(context);

        String key = "last_fired_" + this.identifier();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Editor edit = prefs.edit();
        edit.remove(key);
        edit.commit();
    }

    public void merge(Context context, Trigger trigger)
    {
        if (trigger instanceof DateTrigger)
        {
            super.merge(trigger);

            DateTrigger dateTrigger = (DateTrigger) trigger;

            this._icalString = dateTrigger._icalString;
            this._random = dateTrigger._random;

            this.refreshCalendar(context);
        }
    }

    private void refreshCalendar(Context context)
    {
        if (this._icalString == null)
            return;

        try
        {
            StringReader sin = new StringReader(this.adjustCalendar(context, this._icalString));
            CalendarBuilder builder = new CalendarBuilder();

            this._calendar = builder.build(sin);
        }
        catch (NullPointerException e)
        {
            LogManager.getInstance(context).logException(e);
        }
        catch (IOException e)
        {
            LogManager.getInstance(context).logException(e);
        }
        catch (ParserException e)
        {
            LogManager.getInstance(context).logException(e);
        }
    }

    private String adjustCalendar(Context context, String original)
    {
        if (original.contains("FREQ=MINUTELY") && original.contains("COUNT=") == false)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

            StringBuffer newCalendar = new StringBuffer();

            long day = (24 * 60 * 60 * 1000);

            for (String line : original.split("\n"))
            {
                if (line.startsWith("DTSTART"))
                {
                    try
                    {
                        Date originalDate = sdf.parse(line.substring(8));

                        long now = System.currentTimeMillis();

                        if (originalDate.getTime() < now - day)
                        {
                            long newTime = now - day - (now % day) + (originalDate.getTime() % day);

                            Date newDate = new Date(newTime);

                            newCalendar.append("DTSTART:" + sdf.format(newDate) + "\n");
                        }
                        else
                            newCalendar.append("DTSTART:" + sdf.format(originalDate) + "\n");
                    }
                    catch (ParseException e)
                    {
                        LogManager.getInstance(context).logException(e);

                        return original;
                    }
                }
                else if (line.startsWith("DTEND"))
                {
                    try
                    {
                        Date originalDate = sdf.parse(line.substring(6));

                        long now = System.currentTimeMillis();

                        if (originalDate.getTime() < now - day) {
                            long newTime = now - day - (now % day) + (originalDate.getTime() % day);

                            Date newDate = new Date(newTime);

                            newCalendar.append("DTEND:" + sdf.format(newDate) + "\n");
                        }
                        else
                            newCalendar.append("DTEND:" + sdf.format(originalDate) + "\n");
                    }
                    catch (ParseException e)
                    {
                        LogManager.getInstance(context).logException(e);

                        return original;
                    }
                }
                else
                    newCalendar.append(line + "\n");
            }

            return newCalendar.toString();
        }

        return original;
    }

    @Override
    public boolean updateFromMap(Context context, Map<String, Object> map)
    {
        if (super.updateFromMap(context, map))
        {
            if (map.containsKey(DateTrigger.DATETIME_START))
                this._start = map.get(DateTrigger.DATETIME_START).toString();
            if (map.containsKey(DateTrigger.DATETIME_END))
                this._end = map.get(DateTrigger.DATETIME_END).toString();

            this._originalStart = this._start;
            this._originalEnd = this._end;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

            long now = System.currentTimeMillis();

            try
            {
                if (this._end == null)
                    this._end = this._start;

                Date startDate = sdf.parse(this._start);
                Date endDate = sdf.parse(this._end);

                long startTime = startDate.getTime();
                long endTime = endDate.getTime();

                while ((now - startTime) > (180 * 24 * 60 * 60 * 1000L))
                {
                    startTime += (24 * 60 * 60 * 1000);
                    endTime += (24 * 60 * 60 * 1000);
                }

                while ((endTime - now) > (180 * 24 * 60 * 60 * 1000L))
                {
                    endTime -= (24 * 60 * 60 * 1000);
                }

                if (startTime > endTime)
                {
                    long holder = startTime;
                    startTime = endTime;
                    endTime = holder;
                }

                this._start = sdf.format(new Date(startTime));
                this._end = sdf.format(new Date(endTime));
            }
            catch (ParseException ee)
            {
                LogManager.getInstance(context).logException(ee);
            }

            if (this._start != null && this._start.equals(this._end))
            {
                try
                {
                    Date start = sdf.parse(this._start);

                    long time = start.getTime();

                    time += 60 * 1000;

                    Date end = new Date(time);

                    this._end = sdf.format(end);
                }
                catch (ParseException e)
                {
                    LogManager.getInstance(context).logException(e);
                }
            }

            if (map.containsKey(DateTrigger.DATETIME_REPEATS))
                this._repeats = map.get(DateTrigger.DATETIME_REPEATS).toString();

            if (map.containsKey(DateTrigger.DATETIME_RANDOM))
                this._random = ((Boolean) map.get(DateTrigger.DATETIME_RANDOM)).booleanValue();

            if (map.containsKey(DateTrigger.FIRE_ON_BOOT))
                this._fireOnBoot = ((Boolean) map.get(DateTrigger.FIRE_ON_BOOT)).booleanValue();

            if ("null".equals(this._repeats))
                this._repeats = null;

            String repeatString = "";

            if (this._repeats != null)
                repeatString = "\nRRULE:" + this._repeats;

            this._icalString = String.format(context.getString(R.string.ical_template), this._start, this._end, this.name(), repeatString);

            this._lastFireCalcDate = 0;
            this.refresh(context);

            this.refreshCalendar(context);

            ManagerService.resetTriggerNudgeDate();

            TriggerManager.getInstance(context).persistTriggers(context);

            return true;
        }

        return false;
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> config = super.configuration(context);

        config.put(DateTrigger.DATETIME_START, this._start);
        config.put(DateTrigger.DATETIME_END, this._end);
        config.put(DateTrigger.DATETIME_REPEATS, this._repeats);
        config.put(DateTrigger.DATETIME_RANDOM, this._random);
        config.put(DateTrigger.FIRE_ON_BOOT, this._fireOnBoot);
        config.put("type", DateTrigger.TYPE_NAME);

        return config;
    }

    public DateTrigger(Context context, Map<String, Object> map)
    {
        super(context, map);

        this.updateFromMap(context, map);
    }

    public Period getPeriod(Context context, long timestamp)
    {
        Date date = new Date(timestamp);

        PeriodList periodList = null;

        if (this._calendar != null && periodList == null)
        {
            try
            {
                Date fromDate = new Date(timestamp - 5000);
                Date toDate = new Date(timestamp + (15 * 60 * 1000));

                DateTime from = new DateTime(fromDate);
                DateTime to = new DateTime(toDate);

                Period period = new Period(from, to);

                for (Object o : this._calendar.getComponents("VEVENT"))
                {
                    Component c = (Component) o;

                    PeriodList l = c.calculateRecurrenceSet(period);

                    if (l != null && l.size() > 0)
                        periodList = l;
                }
            }
            catch (IllegalArgumentException e)
            {
                LogManager.getInstance(context).logException(e);
            }
        }

        try
        {
            for (Object po : periodList)
            {
                if (po instanceof Period)
                {
                    Period p = (Period) po;

                    DateRange range = new DateRange(p.getStart(), p.getEnd());

                    if (range.includes(date, DateRange.INCLUSIVE_START | DateRange.INCLUSIVE_END))
                        return p;
                }
            }
        }
        catch (NullPointerException e)
        {

        }

        return null;
    }

    @Override
    public void execute(Context context, boolean force)
    {
        long now = System.currentTimeMillis();

        Period p = this.getPeriod(context, now);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String key = "last_fired_" + this.identifier();

        if (p != null && force == false)
        {
            long lastFired = prefs.getLong(key, 0);

            Date lastFireDate = new Date(lastFired);

            DateTime end = p.getEnd();
            DateTime start = p.getStart();

            DateRange range = new DateRange(start, end);

            if (range.includes(lastFireDate, DateRange.INCLUSIVE_START | DateRange.INCLUSIVE_END))
                return; // Already fired.

            if (this._random && DateTrigger.random != null)
            {
                long timeLeft = System.currentTimeMillis();
                long periodEnd = end.getTime();

                long delta = periodEnd - timeLeft;

                delta = (delta / (60 * 1000)) - 1; // Normalize to minutes, drop
                                                   // last minute

                if (delta > 1)
                {
                    double fireThreshold = 1.0 / (double) delta;

                    double randomDouble = random.nextDouble();

                    if (randomDouble > fireThreshold)
                        return; // Not your time, please try again.
                }
            }
        }

        Editor edit = prefs.edit();
        edit.putLong(key, now);
        edit.commit();

        super.execute(context, force);
    }

    @Override
    public boolean matches(Context context, Object obj)
    {
        if (obj instanceof Date)
        {
            if (this._calendar == null)
                this.refreshCalendar(context);

            Date date = (Date) obj;

            Period p = this.getPeriod(context, date.getTime());

            return (p != null);
        }

        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = super.preferenceScreen(context, manager);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss, yyyy-MM-dd");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Preference lastFire = new Preference(context);
        lastFire.setSummary(R.string.label_trigger_last_fire);

        String key = "last_fired_" + this.identifier();
        long lastFireTime = prefs.getLong(key, 0);

        if (lastFireTime == 0)
            lastFire.setTitle(R.string.label_trigger_last_fire_never);
        else
        {
            Date d = new Date(lastFireTime);

            lastFire.setTitle(sdf.format(d));
        }

        screen.addPreference(lastFire);

        synchronized (this._upcomingFireDates)
        {
            if (this._upcomingFireDates.size() > 0)
            {
                PreferenceScreen upcomingScreen = manager.createPreferenceScreen(context);
                upcomingScreen.setSummary(R.string.label_trigger_upcoming_fires);

                if (this._upcomingFireDates.size() == 1)
                    upcomingScreen.setTitle(R.string.label_trigger_upcoming_fire_summary);
                else
                    upcomingScreen.setTitle(String.format(context.getString(R.string.label_trigger_upcoming_fires_summary), this._upcomingFireDates.size()));

                for (Date d : this._upcomingFireDates)
                {
                    Preference upcomingFire = new Preference(context);
                    upcomingFire.setTitle(sdf.format(d));

                    upcomingScreen.addPreference(upcomingFire);
                }

                screen.addPreference(upcomingScreen);
            }
            else
            {
                Preference upcomingFires = new Preference(context);

                upcomingFires.setSummary(R.string.label_trigger_upcoming_fires);
                upcomingFires.setTitle(R.string.label_trigger_upcoming_fires_none);

                screen.addPreference(upcomingFires);
            }

            Preference originalStartString = new Preference(context);
            originalStartString.setSummary(R.string.label_trigger_original_start);
            originalStartString.setTitle(this._originalStart);
            screen.addPreference(originalStartString);

            Preference originalEndString = new Preference(context);
            originalEndString.setSummary(R.string.label_trigger_original_end);
            originalEndString.setTitle(this._originalEnd);
            screen.addPreference(originalEndString);

            Preference startString = new Preference(context);
            startString.setSummary(R.string.label_trigger_start);
            startString.setTitle(this._start);
            screen.addPreference(startString);

            Preference endString = new Preference(context);
            endString.setSummary(R.string.label_trigger_end);
            endString.setTitle(this._end);
            screen.addPreference(endString);

            Preference repeatString = new Preference(context);
            repeatString.setSummary(R.string.label_trigger_repeat);
            repeatString.setTitle(this._repeats);
            screen.addPreference(repeatString);

            Preference randomString = new Preference(context);
            randomString.setSummary(R.string.label_trigger_random);

            if (this._random)
                randomString.setTitle(R.string.label_trigger_is_random);
            else
                randomString.setTitle(R.string.label_trigger_not_random);

            screen.addPreference(randomString);

            Preference bootString = new Preference(context);
            bootString.setSummary(R.string.label_trigger_boot);

            if (this._fireOnBoot)
                bootString.setTitle(R.string.label_trigger_is_boot);
            else
                bootString.setTitle(R.string.label_trigger_not_boot);

            screen.addPreference(bootString);
        }

        return screen;
    }

    @Override
    public String getDiagnosticString(Context context)
    {
        String name = this.name();
        String identifier = this.identifier();

        long lastFired = this.lastFireTime(context);

        String lastFiredString = context.getString(R.string.trigger_fired_never);

        if (lastFired != 0)
        {
            DateFormat formatter = android.text.format.DateFormat.getMediumDateFormat(context);
            DateFormat timeFormatter = android.text.format.DateFormat.getTimeFormat(context);

            lastFiredString = formatter.format(new Date(lastFired)) + " " + timeFormatter.format(new Date(lastFired));
        }

        String enabled = context.getString(R.string.trigger_disabled);

        if (this.enabled(context))
            enabled = context.getString(R.string.trigger_enabled);

        return context.getString(R.string.trigger_diagnostic_string, name, identifier, enabled, lastFiredString);
    }

    @Override
    public Bundle bundle(Context context)
    {
        Bundle bundle = super.bundle(context);

        bundle.putBoolean(DateTrigger.RANDOM, this._random);
        bundle.putBoolean(DateTrigger.FIRE_ON_BOOT, this._fireOnBoot);
        bundle.putString(Trigger.TYPE, DateTrigger.TYPE_NAME);
        bundle.putString(DateTrigger.REPEATS, this._repeats);

        if (this._start != null)
            bundle.putString(DateTrigger.START, this._start);

        if (this._end != null)
            bundle.putString(DateTrigger.END, this._end);

        if (this._originalStart != null)
            bundle.putString(DateTrigger.ORIGINAL_START, this._originalStart);

        if (this._originalEnd != null)
            bundle.putString(DateTrigger.ORIGINAL_END, this._originalEnd);

        if (this._icalString != null)
            bundle.putString(DateTrigger.CALENDAR_STRING, this._icalString);

        return bundle;
    }

    public boolean missedFire(Context context, long end)
    {
        if (this._fireOnBoot)
        {
            this.refreshCalendar(context);

            long start = this.lastFireTime(context);

            try
            {
                DateTime from = new DateTime(new Date(start));
                DateTime to = new DateTime(new Date(end));

                Period period = new Period(from, to);

                for (Object o : this._calendar.getComponents("VEVENT"))
                {
                    Component c = (Component) o;

                    PeriodList l = c.calculateRecurrenceSet(period);

                    if (l != null && l.size() > 0)
                        return true;
                }
            }
            catch (IllegalArgumentException e)
            {
                LogManager.getInstance(context).logException(e);
            }
        }

        return false;
    }

    public List<Long> fireTimes(Context context, long start, long end)
    {
        ArrayList<Long> times = new ArrayList<Long>();

        long offset = 0;

        while (start + offset <= end)
        {
            Period p = this.getPeriod(context, start + offset);

            if (p != null)
            {
                long fireTime = Long.valueOf(start + offset + 5000);

                times.add(fireTime);
            }

            offset += 60000;
        }

        return times;
    }

    public long lastMissedFireTime(Context context)
    {
        long now = System.currentTimeMillis();

        long lastFired = this.lastFireTime(context);

        // Assumes that the device has been on and fired at least once in
        // the last two weeks. To go back indefinitely is computationally
        // infeasible. (See method above why.)

        if (lastFired == 0)
            lastFired = now - (14 * 24 * 60 * 60 * 1000);

        List<Long> missedFires = this.fireTimes(context, lastFired, now);

        if (missedFires.size() > 0)
        {
            Collections.sort(missedFires);
            Collections.reverse(missedFires);

            return missedFires.get(0).longValue();
        }

        return 0;
    }
}
