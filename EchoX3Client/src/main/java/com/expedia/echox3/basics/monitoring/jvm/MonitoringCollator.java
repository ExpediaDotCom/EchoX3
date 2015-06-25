/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.jvm;

import java.util.Comparator;

import com.expedia.echox3.basics.tools.time.WallClock;
import com.expedia.echox3.basics.tools.time.WallClock.FormatSize;
import com.expedia.echox3.basics.tools.time.WallClock.FormatType;

public class MonitoringCollator
{
	private static final Comparator<MonitoringCollator> COMPARATOR_BY_NAME	= new ComparatorByName();

	private String m_serverName;
	private long					m_lastMeasuredTimeMS	= 0;

	// CHECKSTYLE:OFF
	private JavaMeasureProxy			m_javaMeasureProxy		= new JavaMeasureProxy();
	private GarbageInfoMeasureProxy		m_garbageMeasureYoung	= new GarbageInfoMeasureProxy(GarbageInfo.PRIMARY_POOL_CONTAINS_YOUNG);
	private GarbageInfoMeasureProxy		m_garbageMeasureOld		= new GarbageInfoMeasureProxy(GarbageInfo.PRIMARY_POOL_CONTAINS_OLD);
	private WallClockMeasureProxy		m_wallClockMeasure		= new WallClockMeasureProxy();
	// CHECKSTYLE:ON

	public MonitoringCollator(String serverName)
	{
		m_serverName= serverName;
	}

	public static Comparator<MonitoringCollator> getComparatorByName()
	{
		return COMPARATOR_BY_NAME;
	}

	public String getServerName()
	{
		return m_serverName;
	}

	public long getLastMeasuredTimeMS()
	{
		return m_lastMeasuredTimeMS;
	}

	public JavaMeasureProxy getJavaMeasureProxy()
	{
		return m_javaMeasureProxy;
	}

	public GarbageInfoMeasureProxy getGarbageInfoYoung()
	{
		return m_garbageMeasureYoung;
	}

	public GarbageInfoMeasureProxy getGarbageInfoOld()
	{
		return m_garbageMeasureOld;
	}

	public WallClockMeasureProxy getWallClockMeasure()
	{
		return m_wallClockMeasure;
	}

	/**
	 * Performs all necessary measurements.
	 */
	public void measure()
	{
		m_lastMeasuredTimeMS = WallClock.getCurrentTimeMS();

		m_javaMeasureProxy.measure(m_serverName);
		m_garbageMeasureYoung.measure(m_serverName);
		m_garbageMeasureOld.measure(m_serverName);
		m_wallClockMeasure.measure(m_serverName);
	}

	@Override
	public int hashCode()
	{
		return m_serverName.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof MonitoringCollator))
		{
			return false;
		}
		return m_serverName.equals(((MonitoringCollator)obj).getServerName());
	}

	@Override
	public String toString()
	{
		return String.format("%s(%s, %s) (Java = %s; GC = %s; Clock = %s)",
				getClass().getSimpleName(), getServerName(),
				WallClock.formatTime(FormatType.DateTime, FormatSize.Medium, getLastMeasuredTimeMS()),
				getJavaMeasureProxy().isConnected(),
				getGarbageInfoYoung().isConnected() && getGarbageInfoOld().isConnected(),
				getWallClockMeasure().isConnected());
	}

	private static class ComparatorByName implements java.util.Comparator<MonitoringCollator>
	{
		/**
		 * Compares its two arguments for order.  Returns a negative integer,
		 * zero, or a positive integer as the first argument is less than, equal
		 * to, or greater than the second.<p>
		 * <p/>
		 * In the foregoing description, the notation
		 * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
		 * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
		 * <tt>0</tt>, or <tt>1</tt> according to whether the value of
		 * <i>expression</i> is negative, zero or positive.<p>
		 * <p/>
		 * The implementor must ensure that <tt>sgn(compare(x, y)) ==
		 * -sgn(compare(y, x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
		 * implies that <tt>compare(x, y)</tt> must throw an exception if and only
		 * if <tt>compare(y, x)</tt> throws an exception.)<p>
		 * <p/>
		 * The implementor must also ensure that the relation is transitive:
		 * <tt>((compare(x, y)&gt;0) &amp;&amp; (compare(y, z)&gt;0))</tt> implies
		 * <tt>compare(x, z)&gt;0</tt>.<p>
		 * <p/>
		 * Finally, the implementor must ensure that <tt>compare(x, y)==0</tt>
		 * implies that <tt>sgn(compare(x, z))==sgn(compare(y, z))</tt> for all
		 * <tt>z</tt>.<p>
		 * <p/>
		 * It is generally the case, but <i>not</i> strictly required that
		 * <tt>(compare(x, y)==0) == (x.equals(y))</tt>.  Generally speaking,
		 * any comparator that violates this condition should clearly indicate
		 * this fact.  The recommended language is "Note: this comparator
		 * imposes orderings that are inconsistent with equals."
		 *
		 * @param o1 the first object to be compared.
		 * @param o2 the second object to be compared.
		 * @return a negative integer, zero, or a positive integer as the
		 * first argument is less than, equal to, or greater than the
		 * second.
		 * @throws NullPointerException if an argument is null and this
		 *                              comparator does not permit null arguments
		 * @throws ClassCastException   if the arguments' types prevent them from
		 *                              being compared by this comparator.
		 */
		@Override
		public int compare(MonitoringCollator o1, MonitoringCollator o2)
		{
			return o1.getServerName().compareTo(o2.getServerName());
		}
	}
}
