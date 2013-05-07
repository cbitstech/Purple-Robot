package edu.northwestern.cbits.purple_robot_manager.triggers;

import java.io.IOException;
import java.io.StringReader;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

@SuppressLint("SimpleDateFormat")
public class DateTrigger extends Trigger
{
	public static final String TYPE_NAME = "datetime";

	private static final String DATETIME_START = "datetime_start";
	private static final String DATETIME_END = "datetime_end";
	private static final String DATETIME_REPEATS = "datetime_repeat";
	private static final String DATETIME_RANDOM = "datetime_random";

	private static SecureRandom random = null;

	private PeriodList periodList = null;
	private long lastUpdate = 0;

	private boolean _random = false;
	private String _start = null;
	private String _end = null;
	private String _repeats = null;
	
	private Calendar _calendar = null;
	
	private String _icalString = null;

	private long _lastFireCalcDate = 0;
	private List<Date> _upcomingFireDates = new ArrayList<Date>();
	
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
	
	public void refresh(final Context context) 
	{
		long now = System.currentTimeMillis();
		
		if (now - this._lastFireCalcDate > (3600 * 1000))
		{
			this._lastFireCalcDate = now;
			
			final DateTrigger me = this;
			
			Runnable r = new Runnable()
			{
				public void run() 
				{
					me.refreshCalendar(context);
					
					if (me._calendar == null)
						return;
					
					ArrayList<Date> upcoming = new ArrayList<Date>();
					
					long now = System.currentTimeMillis();
					long current = now;
					
					long maxCount = 64;

					long hour = 1000 * 60 * 60;
					
					Date currentDate = new Date(now);

					while (current - now < (hour * 48) && upcoming.size() < maxCount)
					{
						DateTime from = new DateTime(new Date(current));
						DateTime to = new DateTime(new Date(current + (hour)));

						try
						{
							Period period = new Period(from, to);
			
							for (Object o : me._calendar.getComponents("VEVENT"))
							{
								Component c = (Component) o;
			
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
							
							current += hour;
						}
						catch (NullPointerException e)
						{
							
						}
						catch (IllegalArgumentException e)
						{
							LogManager.getInstance(context).logException(e);
						}
					}

					synchronized(me._upcomingFireDates)
					{
						me._upcomingFireDates.clear();
						me._upcomingFireDates.addAll(upcoming);
					}
				}
			};
			
			Thread t = new Thread(r);
			t.start();
		}
	}


	public void reset(Context context) 
	{
		super.reset(context);
		
		this.lastUpdate = 0;

		String key = "last_fired_" + this.identifier();

		SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(context);

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
			StringReader sin = new StringReader(this._icalString);
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
	
	public boolean updateFromMap(Context context, Map<String, Object> map) 
	{
		if (super.updateFromMap(context, map))
		{
			if (map.containsKey(DateTrigger.DATETIME_START))
				this._start = map.get(DateTrigger.DATETIME_START).toString();
			
			if (map.containsKey(DateTrigger.DATETIME_END))
				this._end = map.get(DateTrigger.DATETIME_END).toString();

			if (map.containsKey(DateTrigger.DATETIME_REPEATS))
				this._repeats = map.get(DateTrigger.DATETIME_REPEATS).toString();

			if (map.containsKey(DateTrigger.DATETIME_RANDOM))
				this._random = ((Boolean) map.get(DateTrigger.DATETIME_RANDOM)).booleanValue();

			if ("null".equals(this._repeats))
				this._repeats = null;
			
			String repeatString = "";

			if (this._repeats != null)
				repeatString = "\nRRULE:" + this._repeats;

			this._icalString = String.format(context.getString(R.string.ical_template), this._start, this._end, this.name(), repeatString);
			
			this._lastFireCalcDate = 0;
			this.refresh(context);

			return true;
		}
		
		return false;
	}

	public Map<String, Object> configuration(Context context) 
	{
		Map<String, Object> config = super.configuration(context);
		
		config.put(DateTrigger.DATETIME_START, this._start);
		config.put(DateTrigger.DATETIME_END, this._end);
		config.put(DateTrigger.DATETIME_REPEATS, this._repeats);
		config.put(DateTrigger.DATETIME_RANDOM, this._random);
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
		if (timestamp - this.lastUpdate > 300000)
		{
			periodList = null;
			this.lastUpdate = timestamp;
		}

		Date date = new Date(timestamp);

		if (periodList == null)
		{
			try
			{
				DateTime from = new DateTime(new Date(timestamp - 5000));
				DateTime to = new DateTime(new Date(timestamp + 600000));
	
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

					if (range.includes(date, DateRange.INCLUSIVE_END | DateRange.INCLUSIVE_END))
						return p;
				}
			}
		}
		catch (NullPointerException e)
		{
			
		}

		return null;
	}

	public void execute(Context context, boolean force)
	{
		long now = System.currentTimeMillis();

		Period p = this.getPeriod(context, now);

		SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(context);

		String key = "last_fired_" + this.identifier();

		if (p != null && force == false)
		{
			long lastFired = prefs.getLong(key, 0);

			Date lastFireDate = new Date(lastFired);

			DateTime end = p.getEnd();
			DateTime start = p.getStart();

			DateRange range = new DateRange(start, end);

			if (range.includes(lastFireDate, DateRange.INCLUSIVE_END | DateRange.INCLUSIVE_END))
				return; // Already fired.

			if (this._random && DateTrigger.random != null)
			{
				long timeLeft = System.currentTimeMillis();
				long periodEnd = end.getTime();

				long delta = periodEnd - timeLeft;

				delta = (delta - 1) / (60 * 1000); // Normalize to minutes, drop last minute

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

	public boolean matches(Context context, Object obj)
	{
		if (obj instanceof Date)
		{
			if (this._calendar != null)
			{
				Date date = (Date) obj;

				Period p = this.getPeriod(context, date.getTime());

				return (p != null);
			}
		}

		return false;
	}
	
	public PreferenceScreen preferenceScreen(PreferenceActivity activity) 
	{
		PreferenceScreen screen = super.preferenceScreen(activity);

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss, yyyy-MM-dd");

		SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(activity);

		Preference lastFire = new Preference(activity);
		lastFire.setSummary(R.string.label_trigger_last_fire);
		lastFire.setOrder(0);
		
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
		
		synchronized(this._upcomingFireDates)
		{
			if (this._upcomingFireDates.size() > 0)
			{
				PreferenceManager manager = activity.getPreferenceManager();
	
				PreferenceScreen upcomingScreen = manager.createPreferenceScreen(activity);
				upcomingScreen.setSummary(R.string.label_trigger_upcoming_fires);
				upcomingScreen.setOrder(0);
	
				if (this._upcomingFireDates.size() == 1)
					upcomingScreen.setTitle(R.string.label_trigger_upcoming_fire_summary);
				else
					upcomingScreen.setTitle(String.format(activity.getString(R.string.label_trigger_upcoming_fires_summary), this._upcomingFireDates.size()));
	
				for (Date d : this._upcomingFireDates)
				{
					Preference upcomingFire = new Preference(activity);
					upcomingFire.setTitle(sdf.format(d));
					
					upcomingScreen.addPreference(upcomingFire);
				}
	
				screen.addPreference(upcomingScreen);
			}
			else
			{
				Preference upcomingFires = new Preference(activity);
				upcomingFires.setOrder(0);
				
				upcomingFires.setSummary(R.string.label_trigger_upcoming_fires);
				upcomingFires.setTitle(R.string.label_trigger_upcoming_fires_none);
				
				screen.addPreference(upcomingFires);
			}
		}	
		
		return screen;
	}
}
