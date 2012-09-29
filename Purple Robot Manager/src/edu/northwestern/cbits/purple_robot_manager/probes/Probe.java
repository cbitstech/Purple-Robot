package edu.northwestern.cbits.purple_robot_manager.probes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northwestern.cbits.purple_robot_manager.R;

import android.content.Context;
import android.os.Bundle;

public abstract class Probe
{
	public static final String START_DATE = "START";
	public static final String END_DATE = "END";
	public static final String DURATION = "DURATION";
	public static final String PERIOD = "PERIOD";

	private static String DATE_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss Z";

	public enum ValueType
	{
		STRING,
		REAL,
		LONG,
		DATE,
		OBJECT
	}

	public static class ProbePropertyValue
	{
		private ValueType _type;
		private String _name;
		private String _value;

		public ProbePropertyValue(String name, ValueType type)
		{
			this._name = name;
			this._type = type;
		}

		public static Date dateForString(String dateString)
		{
			SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STRING);

			try
			{
				return sdf.parse(dateString);
			}
			catch (ParseException e)
			{
				e.printStackTrace();
			}

			return null;
		}

		public static ProbePropertyValue dateProperty(String name, Date date)
		{
			ProbePropertyValue property = new ProbePropertyValue(name, ValueType.DATE);

			property.setDate(date);

			return property;
		}

		private void setDate(Date date)
		{
			SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STRING);

			this._value = sdf.format(date);
		}

		public static ProbePropertyValue longProperty(String name, long value)
		{
			ProbePropertyValue property = new ProbePropertyValue(name, ValueType.LONG);

			property.setLong(value);

			return property;
		}

		private void setLong(long value)
		{
			this._value = "" + value;
		}

		public String name()
		{
			return this._name;
		}

		public ValueType type()
		{
			return this._type;
		}

		public Date dateValue()
		{
			return ProbePropertyValue.dateForString(this._value);
		}

		public long longValue()
		{
			return ProbePropertyValue.longForString(this._value);
		}

		private static long longForString(String value)
		{
			return Long.parseLong(value);
		}
	}

	protected Map<String, ProbePropertyValue> properties = new HashMap<String, ProbePropertyValue>();
	private String _name = null;
	private String _title = null;

	public Probe(String name, String title, long period, long duration, Date start, Date end)
	{
		this._name = name;
		this._title = title;

		properties.put(Probe.START_DATE, ProbePropertyValue.dateProperty(Probe.START_DATE, start));
		properties.put(Probe.END_DATE, ProbePropertyValue.dateProperty(Probe.END_DATE, end));
		properties.put(Probe.DURATION, ProbePropertyValue.longProperty(Probe.DURATION, duration));
		properties.put(Probe.PERIOD, ProbePropertyValue.longProperty(Probe.PERIOD, period));
	}

	public String name()
	{
		return this._name;
	}

	public String title()
	{
		return this._title;
	}

	@SuppressWarnings("rawtypes")
	private static List<Class> _probeClasses = new ArrayList<Class>();

	@SuppressWarnings("rawtypes")
	public static void registerProbeClass(Class probeClass)
	{
		if (!Probe._probeClasses.contains(probeClass))
			Probe._probeClasses.add(probeClass);
	}

	@SuppressWarnings("rawtypes")
	public static List<Class> availableProbeClasses()
	{
		return Probe._probeClasses;
	}

	public static void loadProbeClasses(Context context)
	{
		String packageName = Probe.class.getPackage().getName();

		String[] probeClasses = context.getResources().getStringArray(R.array.probe_classes);

		for (String className : probeClasses)
		{
			try
			{
				Probe.registerProbeClass(Class.forName(packageName + "." + className));
			}
			catch (ClassNotFoundException e)
			{
				e.printStackTrace();
			}
		}
	}
}
