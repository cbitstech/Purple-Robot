package edu.northwestern.cbits.purple_robot_manager.triggers;

import java.io.IOException;
import java.io.StringReader;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateRange;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import edu.northwestern.cbits.purple_robot_manager.R;

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

	static
	{
		try
		{
			DateTrigger.random = SecureRandom.getInstance("SHA1PRNG");
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
	}

	private boolean _random = false;
	private Calendar _calendar = null;
	
	private String _icalString = null;

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

	public void merge(Trigger trigger) 
	{
		if (trigger instanceof DateTrigger)
		{
			super.merge(trigger);
			
			DateTrigger dateTrigger = (DateTrigger) trigger;
		
			this._icalString = dateTrigger._icalString;
			this._random = dateTrigger._random;
			
			this.refreshCalendar();
		}
	}

	private void refreshCalendar()
	{
		StringReader sin = new StringReader(this._icalString);

		CalendarBuilder builder = new CalendarBuilder();

		try
		{
			this._calendar = builder.build(sin);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (ParserException e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean updateFromJson(Context context, JSONObject json) 
	{
		if (super.updateFromJson(context, json))
		{
			try
			{
				String start = null;
				
				if (json.has(DateTrigger.DATETIME_START))
					start = json.getString(DateTrigger.DATETIME_START);
				
				String end = null;
	
				if (json.has(DateTrigger.DATETIME_END))
					end = json.getString(DateTrigger.DATETIME_END);
	
				String repeats = "null";
	
				if (json.has(DateTrigger.DATETIME_REPEATS))
					repeats = json.getString(DateTrigger.DATETIME_REPEATS);
	
				if (json.has(DateTrigger.DATETIME_RANDOM))
					this._random = json.getBoolean(DateTrigger.DATETIME_RANDOM);
	
				if ("null".equals(repeats))
					repeats = null;
				
				String repeatString = "";

				if (repeats != null)
					repeatString = "\nRRULE:" + repeats;

				this._icalString = String.format(context.getString(R.string.ical_template), start, end, this.name(), repeatString);
				
				this.refreshCalendar();
	
				return true;
			} 
			catch (JSONException e) 
			{
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
	public DateTrigger(Context context, JSONObject object) throws JSONException
	{
		super(context, object);

		String start = object.getString(DateTrigger.DATETIME_START);
		String end = object.getString(DateTrigger.DATETIME_END);
		
		String repeats = "null";

		if (object.has(DateTrigger.DATETIME_REPEATS))
			repeats = object.getString(DateTrigger.DATETIME_REPEATS);

		if (object.has(DateTrigger.DATETIME_RANDOM))
			this._random = object.getBoolean(DateTrigger.DATETIME_RANDOM);

		if ("null".equals(repeats))
			repeats = null;

		String repeatString = "";

		if (repeats != null)
			repeatString = "\nRRULE:" + repeats;

		this._icalString = String.format(context.getString(R.string.ical_template), start, end, this.name(), repeatString);
		
		this.refreshCalendar();
	}

	public Period getPeriod(long timestamp)
	{
		if (timestamp - this.lastUpdate > 300000)
		{
			periodList = null;
			this.lastUpdate = timestamp;
		}

		Date date = new Date(timestamp);

		if (periodList == null)
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

		if (periodList != null)
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

		return null;
	}

	public void execute(Context context)
	{
		long now = System.currentTimeMillis();

		Period p = this.getPeriod(now);

		SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(context);

		String key = "last_fired_" + this.identifier();

		if (p != null)
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

		super.execute(context);
	}

	public boolean matches(Context context, Object obj)
	{
		if (obj instanceof Date)
		{
			if (this._calendar != null)
			{
				Date date = (Date) obj;

				Period p = this.getPeriod(date.getTime());

				return (p != null);
			}
		}

		return false;
	}
}
