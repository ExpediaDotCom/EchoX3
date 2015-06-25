/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.tools.time;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Pierre on 5/2/14.
 */
public enum TimeUnits
{
	ns		(1),
	us		(ns.getNS() * 1000),
	ms		(us.getNS() * 1000),
	sec		(ms.getNS() * 1000),
	min		(sec.getNS() * 60),
	hour	(min.getNS() * 60),
	day		(hour.getNS()  * 24),
	week	(day.getNS() * 7),
	year	(day.getNS() * 365);

	private static final TimeUnits[]				TIME_UNITS_LIST		= TimeUnits.values();
	private static final Map<String, TimeUnits> TEXT_TO_UNITS_MAP	= new HashMap<>();

	private long		m_ns;

	static
	{
		TEXT_TO_UNITS_MAP.put("nanosecond", TimeUnits.ns);
		TEXT_TO_UNITS_MAP.put("nano", TimeUnits.ns);
		TEXT_TO_UNITS_MAP.put("ns", TimeUnits.ns);
		TEXT_TO_UNITS_MAP.put("microsecond", TimeUnits.us);
		TEXT_TO_UNITS_MAP.put("micro", TimeUnits.us);
		TEXT_TO_UNITS_MAP.put("us", TimeUnits.us);
		TEXT_TO_UNITS_MAP.put("millisecond", TimeUnits.ms);
		TEXT_TO_UNITS_MAP.put("milli", TimeUnits.ms);
		TEXT_TO_UNITS_MAP.put("ms", TimeUnits.ms);
		TEXT_TO_UNITS_MAP.put("second", TimeUnits.sec);
		TEXT_TO_UNITS_MAP.put("sec", TimeUnits.sec);
		TEXT_TO_UNITS_MAP.put("minute", TimeUnits.min);
		TEXT_TO_UNITS_MAP.put("min", TimeUnits.min);
		TEXT_TO_UNITS_MAP.put("hour", TimeUnits.hour);
		TEXT_TO_UNITS_MAP.put("hr", TimeUnits.hour);
		TEXT_TO_UNITS_MAP.put("day", TimeUnits.day);
		TEXT_TO_UNITS_MAP.put("week", TimeUnits.week);
		TEXT_TO_UNITS_MAP.put("year", TimeUnits.year);
	}

	private TimeUnits(long ns)
	{
		m_ns = ns;
	}

	public long getNS()
	{
		return m_ns;
	}

	public static long getTimeMS(long number, String unitsText)
	{
		return getTimeNS(number, unitsText) / TimeUnits.ms.getNS();
	}
	public static long getTimeMS(long number, TimeUnits units)
	{
		return getTimeNS(number, units) / TimeUnits.ms.getNS();
	}

	public static long getTimeUS(long number, TimeUnits units)
	{
		return getTimeNS(number, units) / TimeUnits.us.getNS();
	}
	public static long getTimeUS(long number, String unitsText)
	{
		return getTimeNS(number, unitsText) / TimeUnits.us.getNS();
	}

	public static long getTimeNS(long number, String unitsText)
	{
		TimeUnits		units		= TEXT_TO_UNITS_MAP.get(unitsText.toLowerCase(Locale.US));
		if (null == units)
		{
			throw new IllegalArgumentException("Unknown units " + unitsText);
		}

		return getTimeNS(number , units);
	}
	public static long getTimeNS(long number, TimeUnits units)
	{
		long		multiplier		= units.getNS();
		long		ns				= number * multiplier;

		return ns;
	}

	public static String formatMS(long ms)
	{
		return formatNS(ms * TimeUnits.ms.getNS());
	}

	public static String formatUS(long us)
	{
		return formatNS(us * TimeUnits.us.getNS());
	}

	public static String formatNS(long ns)
	{
		StringBuilder sb				= new StringBuilder();
		if (ns < 0)
		{
			ns = -ns;
			sb.append('-');
		}

		boolean			highUnitFound	= false;
		for (int i = TIME_UNITS_LIST.length - 1; i >= 0; i--)
		{
			TimeUnits	units			= TIME_UNITS_LIST[i];
			long		multiplier		= units.getNS();
			long		value			= ns / multiplier;
			boolean		done			= highUnitFound;
			if (0 < value)
			{
				if (highUnitFound)
				{
					sb.append(' ');
				}
				sb.append(String.format("%,d %s", value, units.name()));
				ns -= (value * multiplier);

				highUnitFound = true;
			}
			if (done)
			{
				break;
			}
		}
		String result		= sb.toString();
		// If it is empty, it means the input time  was 0
		// The most likely scenario for this is when System.currentTimeMillis is used, so report 0 mS, not 0 nS or 0 uS
		if (result.isEmpty())
		{
			result = "0 mS";
		}
		return result;
	}
}
