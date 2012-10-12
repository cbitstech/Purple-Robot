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

import com.WazaBe.HoloEverywhere.preference.PreferenceManager;
import com.WazaBe.HoloEverywhere.preference.SharedPreferences;
import com.WazaBe.HoloEverywhere.preference.SharedPreferences.Editor;

import edu.northwestern.cbits.purple_robot_manager.R;

public class DateTrigger extends Trigger
{
	public static final String TYPE_NAME = "datetime";

	private static final String DATETIME_START = "datetime_start";
	private static final String DATETIME_END = "datetime_end";
	private static final String DATETIME_REPEATS = "datetime_repeat";
	private static final String DATETIME_RANDOM = "datetime_random";

	private static SecureRandom random = null;

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

	private String _start = null;
	private String _end = null;
	private String _repeats = null;
	private boolean _random = false;
	private Calendar _calendar = null;

	public DateTrigger(Context context, JSONObject object) throws JSONException
	{
		super(context, object);

		this._start = object.getString(DateTrigger.DATETIME_START);
		this._end = object.getString(DateTrigger.DATETIME_END);

		if (object.has(DateTrigger.DATETIME_REPEATS))
			this._repeats = object.getString(DateTrigger.DATETIME_REPEATS);

		if (object.has(DateTrigger.DATETIME_RANDOM))
			this._random = object.getBoolean(DateTrigger.DATETIME_RANDOM);

		if ("null".equals(this._repeats))
			this._repeats = null;

		String repeatString = "";

		if (this._repeats != null)
			repeatString = "\nRRULE:" + this._repeats;

		String icalString = String.format(context.getString(R.string.ical_template), this._start, this._end, this.name(), repeatString);

		StringReader sin = new StringReader(icalString);

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

	public Period getPeriod(long timestamp)
	{
		DateTime from = new DateTime(new Date(timestamp - 5000));
		DateTime to = new DateTime(new Date(timestamp + 5000));

		Date date = new Date(timestamp);

		Period period = new Period(from, to);

		// For each VEVENT in the ICS
		for (Object o : this._calendar.getComponents("VEVENT"))
		{
			Component c = (Component) o;

			PeriodList list = c.calculateRecurrenceSet(period);

			for (Object po : list)
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

		String key = "last_fired_" + this.name();

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
