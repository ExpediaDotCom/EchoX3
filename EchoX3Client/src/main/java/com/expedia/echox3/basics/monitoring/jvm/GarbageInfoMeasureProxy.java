/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.jvm;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.tools.time.TimeUnits;
import com.expedia.echox3.basics.tools.time.WallClock;

/**
 * This class runs on a remote machine and is responsible for gathering the data measured by GarbageInfo
 * and exposed via JMX. The class uses JMX to read the data at the remote end.
 */
@SuppressWarnings("PMD.TooManyFields")
public class GarbageInfoMeasureProxy extends AbstractMeasureProxy implements GarbageInfoMBean
{
	private static final String[]		ATTRIBUTE_NAME_LIST		=
			{
				"CollectorName",			// 0
				"PrimaryPoolName",			// 1

				"TotalCount",				// 2
				"TotalDurationMS",			// 3
				"TotalElapsedMS",			// 4
//				"TotalElapsed",
				"TotalAverageMS",			// 5
				"TotalPeriodSec",			// 6
				"TotalDutyCyclePercent",	// 7

				"LatchTimeMS",				// 8
//				"LatchDate",

				"CurrentCount",				// 9
				"CurrentDurationMS",		// 10
				"CurrentElapsedMS",			// 11
//				"CurrentElapsed",
				"PauseCurrentMinimumMS",	// 12
				"PauseCurrentAverageMS",	// 13
				"PauseCurrentMaximumMS",	// 14

				"ClockCurrentMinimumMS",	// 15
				"ClockCurrentAverageMS",	// 16
				"ClockCurrentMaximumMS",	// 17

				"CurrentPeriodSec",			// 18
				"CurrentDutyCyclePercent",	// 19
			};

	private String		m_collectorName;
	private String		m_primaryPoolName;
	private long		m_totalCount;
	private long		m_totalDurationMS;
	private long		m_totalElapsedMS;
//	private String		m_totalElapsed;
	private double		m_totalAverageMS;
	private double		m_totalPeriodSec;
	private double		m_totalDutyCyclePercent;
	private long		m_latchTimeMS;
//	private String		m_latchDate;
	private long		m_currentCount;
	private double		m_currentDurationMS;
	private long		m_currentElapsedMS;
//	private String		m_currentElapsed;

	private double		m_pauseCurrentMinimumMS;
	private double		m_pauseCurrentAverageMS;
	private double		m_pauseCurrentMaximumMS;

	private double		m_clockCurrentMinimumMS;
	private double		m_clockCurrentAverageMS;
	private double		m_clockCurrentMaximumMS;

	private double		m_currentPeriodSec;
	private double		m_currentDutyCyclePercent;
//	private String[]	m_histogramData;

	public GarbageInfoMeasureProxy(String collectorGeneration)
	{
		super(null, GarbageInfo.generateMbeanNameList(GarbageInfo.BEAN_TYPE_COLLECTOR, collectorGeneration));
	}

	public boolean measure(String serverName)
	{
		return measure(serverName, BasicMBeanManager.DEFAULT_JMX_PORT);
	}
	public boolean measure(String serverName, String port)
	{
		BasicMBeanProxy proxy		= BasicMBeanManager.getMbeanProxy(serverName, port);

		// Don't let someone else change the content of the list of attributes
		synchronized (proxy)
		{
			if (!load(proxy, ATTRIBUTE_NAME_LIST))
			{
				return false;
			}

			try
			{
				m_collectorName				= proxy.getAttributeFromListAsString(ATTRIBUTE_NAME_LIST[0]);
				m_primaryPoolName			= proxy.getAttributeFromListAsString(ATTRIBUTE_NAME_LIST[1]);

				m_totalCount				= proxy.getAttributeFromListAsLong(ATTRIBUTE_NAME_LIST[4]);
				m_totalDurationMS			= proxy.getAttributeFromListAsLong(ATTRIBUTE_NAME_LIST[5]);
				m_totalElapsedMS			= proxy.getAttributeFromListAsLong(ATTRIBUTE_NAME_LIST[6]);
//				m_totalElapsed				= proxy.getAttributeFromList(ATTRIBUTE_NAME_LIST[0]);
				m_totalAverageMS			= proxy.getAttributeFromListAsDouble(ATTRIBUTE_NAME_LIST[7]);
				m_totalPeriodSec			= proxy.getAttributeFromListAsDouble(ATTRIBUTE_NAME_LIST[8]);
				m_totalDutyCyclePercent		= proxy.getAttributeFromListAsDouble(ATTRIBUTE_NAME_LIST[9]);

				m_latchTimeMS				= proxy.getAttributeFromListAsLong(ATTRIBUTE_NAME_LIST[10]);

				m_currentCount				= proxy.getAttributeFromListAsLong(ATTRIBUTE_NAME_LIST[11]);
				m_currentDurationMS			= proxy.getAttributeFromListAsDouble(ATTRIBUTE_NAME_LIST[12]);
				m_currentElapsedMS			= proxy.getAttributeFromListAsLong(ATTRIBUTE_NAME_LIST[13]);
//				m_currentElapsed			= proxy.getAttributeFromList(ATTRIBUTE_NAME_LIST[0]);

				m_pauseCurrentMinimumMS		= proxy.getAttributeFromListAsDouble(ATTRIBUTE_NAME_LIST[14]);
				m_pauseCurrentAverageMS		= proxy.getAttributeFromListAsDouble(ATTRIBUTE_NAME_LIST[15]);
				m_pauseCurrentMaximumMS		= proxy.getAttributeFromListAsDouble(ATTRIBUTE_NAME_LIST[16]);

				m_clockCurrentMinimumMS		= proxy.getAttributeFromListAsDouble(ATTRIBUTE_NAME_LIST[17]);
				m_clockCurrentAverageMS		= proxy.getAttributeFromListAsDouble(ATTRIBUTE_NAME_LIST[18]);
				m_clockCurrentMaximumMS		= proxy.getAttributeFromListAsDouble(ATTRIBUTE_NAME_LIST[19]);

				m_currentPeriodSec			= proxy.getAttributeFromListAsDouble(ATTRIBUTE_NAME_LIST[20]);
				m_currentDutyCyclePercent	= proxy.getAttributeFromListAsDouble(ATTRIBUTE_NAME_LIST[21]);
			}
			catch (Exception exception)
			{
				getLogger().debug(BasicEvent.EVENT_JMX_MEMORY_PARSING_EXCEPTION, exception,
						"Failed to parse data for %s from %s:%s",
						getClass().getSimpleName(), proxy.getServerName(), proxy.getPort());
				return false;
			}
		}

		return true;
	}


	@Override
	public String getCollectorName()
	{
		return m_collectorName;
	}

	@Override
	public String getPrimaryPoolName()
	{
		return m_primaryPoolName;
	}

	@Override
	public long getTotalCount()
	{
		return m_totalCount;
	}

	@Override
	public long getTotalDurationMS()
	{
		return m_totalDurationMS;
	}

	@Override
	public long getTotalElapsedMS()
	{
		return m_totalElapsedMS;
	}

	@Override
	public String getTotalElapsed()
	{
		return TimeUnits.formatMS(getTotalElapsedMS());
	}

	@Override
	public double getTotalAverageMS()
	{
		return m_totalAverageMS;
	}

	@Override
	public double getTotalPeriodSec()
	{
		return m_totalPeriodSec;
	}

	@Override
	public double getTotalDutyCyclePercent()
	{
		return m_totalDutyCyclePercent;
	}

	@Override
	public long getLatchTimeMS()
	{
		return m_latchTimeMS;
	}

	@Override
	public String getLatchDate()
	{
		return WallClock.formatTime(WallClock.FormatType.DateTime, WallClock.FormatSize.Large, m_latchTimeMS);
	}

	@Override
	public long getCurrentCount()
	{
		return m_currentCount;
	}

	@Override
	public double getCurrentDurationMS()
	{
		return m_currentDurationMS;
	}

	@Override
	public long getCurrentElapsedMS()
	{
		return m_currentElapsedMS;
	}

	@Override
	public String getCurrentElapsed()
	{
		return TimeUnits.formatMS(getCurrentElapsedMS());
	}

	@Override
	public double getPauseCurrentMinimumMS()
	{
		return m_pauseCurrentMinimumMS;
	}

	@Override
	public double getPauseCurrentAverageMS()
	{
		return m_pauseCurrentAverageMS;
	}

	@Override
	public double getPauseCurrentMaximumMS()
	{
		return m_pauseCurrentMaximumMS;
	}

	@Override
	public double getClockCurrentMinimumMS()
	{
		return m_clockCurrentMinimumMS;
	}

	@Override
	public double getClockCurrentAverageMS()
	{
		return m_clockCurrentAverageMS;
	}

	@Override
	public double getClockCurrentMaximumMS()
	{
		return m_clockCurrentMaximumMS;
	}

	@Override
	public double getCurrentPeriodSec()
	{
		return m_currentPeriodSec;
	}

	@Override
	public double getCurrentDutyCyclePercent()
	{
		return m_currentDutyCyclePercent;
	}

	@Override
	public String[] getHistogramData()
	{
		return new String[0];
	}

	/**
	 * Returns a string representation of the object. In general, the
	 * {@code toString} method returns a string that
	 * "textually represents" this object. The result should
	 * be a concise but informative representation that is easy for a
	 * person to read.
	 * It is recommended that all subclasses override this method.
	 * <p>
	 * The {@code toString} method for class {@code Object}
	 * returns a string consisting of the name of the class of which the
	 * object is an instance, the at-sign character `{@code @}', and
	 * the unsigned hexadecimal representation of the hash code of the
	 * object. In other words, this method returns a string equal to the
	 * value of:
	 * <blockquote>
	 * <pre>
	 * getClass().getName() + '@' + Integer.toHexString(hashCode())
	 * </pre></blockquote>
	 *
	 * @return a string representation of the object.
	 */
	@Override
	public String toString()
	{
		return String.format("%s: %.1fms / %.1Sec", getPrimaryPoolName(),
				getPauseCurrentAverageMS(), getCurrentPeriodSec());
	}
}
