/**
 * Copyright 2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.expedia.echox3.basics.monitoring.jvm;

import java.util.Date;

import com.expedia.echox3.basics.monitoring.event.BasicEvent;
import com.expedia.echox3.basics.tools.time.WallClock;
import com.expedia.echox3.basics.tools.time.WallClockMBean;

/**
 * This class runs on a remote machine and is responsible for gathering the data measured by GarbageInfo
 * and exposed via JMX. The class uses JMX to read the data at the remote end.
 */
public class WallClockMeasureProxy extends AbstractMeasureProxy implements WallClockMBean
{
	private static final String[] ATTRIBUTE_NAME_LIST =
			{
					"MasterName",			// 0
					"CorrectedTime",		// 1
					"LocalTime",			// 2
					"MeasuredTime",			// 3
					"OffsetMS",				// 4
			};
	private static final String TO_STRING_FORMAT =
			  ATTRIBUTE_NAME_LIST[0] + "=%s; "
			+ ATTRIBUTE_NAME_LIST[1] + "=%d; "
			+ ATTRIBUTE_NAME_LIST[2] + "=%d; "
			+ ATTRIBUTE_NAME_LIST[3] + "=%d; "
			+ ATTRIBUTE_NAME_LIST[4] + "=%d";

	private String		m_masterName;
	private long		m_correctedTimeMS;
	private long		m_localTimeMS;
	private long		m_measuredTimeMS;
	private long		m_offsetMS;

	public WallClockMeasureProxy()
	{
		super(null, WallClock.getNameList());
	}

	public boolean measure(String serverName)
	{
		return measure(serverName, BasicMBeanManager.DEFAULT_JMX_PORT);
	}

	public boolean measure(String serverName, String port)
	{
		BasicMBeanProxy proxy = BasicMBeanManager.getMbeanProxy(serverName, port);

		// Don't let someone else change the content of the list of attributes
		synchronized (proxy)
		{
			if (!load(proxy, ATTRIBUTE_NAME_LIST))
			{
				return false;
			}

			try
			{
				m_masterName		= proxy.getAttributeFromListAsString(ATTRIBUTE_NAME_LIST[0]);
				m_correctedTimeMS	= proxy.getAttributeFromListAsLong(ATTRIBUTE_NAME_LIST[1]);
				m_localTimeMS		= proxy.getAttributeFromListAsLong(ATTRIBUTE_NAME_LIST[2]);
				m_measuredTimeMS	= proxy.getAttributeFromListAsLong(ATTRIBUTE_NAME_LIST[3]);
				m_offsetMS			= proxy.getAttributeFromListAsLong(ATTRIBUTE_NAME_LIST[4]);
			}
			catch (Exception exception)
			{
				getLogger().error(BasicEvent.EVENT_JMX_MEMORY_PARSING_EXCEPTION, exception,
						"Failed to parse data for %s", getClass().getSimpleName());
				return false;
			}
		}

		return true;
	}


	@Override
	public String getMasterName()
	{
		return m_masterName;
	}

	@Override
	public long getCorrectedTime()
	{
		return m_correctedTimeMS;
	}

	@Override
	public String getCorrectedDate()
	{
		return WallClock.formatTime(WallClock.FormatType.DateTime, WallClock.FormatSize.Large, getCorrectedTime());
	}

	@Override
	public long getLocalTime()
	{
		return m_localTimeMS;
	}

	@Override
	public String getLocalDate()
	{
		return WallClock.formatTime(WallClock.FormatType.DateTime, WallClock.FormatSize.Large, getLocalTime());
	}

	@Override
	public long getMeasuredTime()
	{
		return m_measuredTimeMS;
	}

	@Override
	public Date getMeasuredDate()
	{
		return new Date(m_measuredTimeMS);
	}

	@Override
	public long getOffsetMS()
	{
		return m_offsetMS;
	}

	@Override
	public java.lang.String toString()
	{
		return String.format(TO_STRING_FORMAT,
				m_masterName, m_correctedTimeMS, m_localTimeMS, m_measuredTimeMS, m_offsetMS);
	}
}
